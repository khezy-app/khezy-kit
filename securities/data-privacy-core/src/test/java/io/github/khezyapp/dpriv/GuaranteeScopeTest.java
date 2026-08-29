package io.github.khezyapp.dpriv;

import io.github.khezyapp.dpriv.api.AuditRecord;
import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.api.Guardrails;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.KeywordsConfig;
import io.github.khezyapp.dpriv.api.LlmCheckConfig;
import io.github.khezyapp.dpriv.api.LlmClassifier;
import io.github.khezyapp.dpriv.api.Operation;
import io.github.khezyapp.dpriv.api.SecretConfig;
import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.api.UrlsConfig;
import io.github.khezyapp.dpriv.pipeline.GuardrailPipeline;
import io.github.khezyapp.dpriv.pipeline.StreamPipeline;
import io.github.khezyapp.dpriv.policy.SecretPreset;
import io.github.khezyapp.dpriv.redact.StreamRedactor;
import io.github.khezyapp.dpriv.stream.TextChunker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 13: the design's §3 guarantee scope, locked as a named test per guarantee/non-guarantee.
 * G1–G7 are pinned as behavior; N1–N5 assert the documented non-behavior so the claim boundary is
 * itself regression-tested. The {@link #endToEndAllFamiliesMatchApiSurface()} test exercises the
 * full API surface — in-memory + streaming + CLASSIFY — on one multi-family fixture.
 */
class GuaranteeScopeTest {

    private static final String FIXTURE =
            "Email visal@example.com token wJalrXUtnFEMIK7p2x1qK visit https://example.com/page confidential";

    private static final String RICH =
            "Email visal@example.com and sok@example.com card 4111111111111111 "
                    + "secret wJalrXUtnFEMIK7p2x1qK link https://example.com/login wall confidential urgent";

    private static final String NEUTRAL = "this is a perfectly safe sentence about the weather";

    private static final int REPETITIONS = 25;

    private static GuardrailsConfig config() {
        return GuardrailsConfig.builder()
                .urls(new UrlsConfig(List.of(), List.of()))
                .keywords(new KeywordsConfig(true, List.of("confidential", "urgent")))
                .build();
    }

    private static String big(final String base) {
        return base.repeat(600);
    }

    @Test
    @DisplayName("G1 — deterministic exactness: same input always yields byte-identical outcomes")
    void g1DeterministicExactness() {
        final var guardrails = Guardrails.builder()
                .config(config())
                .withClassifier(new FlaggingClassifier("jailbreak", true, 0.9))
                .withClassifier(new FlaggingClassifier("nsfw", false, 0.9))
                .build();
        final var text = big(RICH);

        final var baselineScan = guardrails.scan(RICH);
        final var baselineRedact = guardrails.redact(RICH);
        final var baselineSanitize = guardrails.run(RICH, Operation.SANITIZE);
        final var baselineClassify = guardrails.run(NEUTRAL, Operation.CLASSIFY);
        final var baselineStreamScan = guardrails.scan(new StringReader(text));
        final var baselineStreamWriter = new StringWriter();
        guardrails.redact(new StringReader(text), baselineStreamWriter);

        for (var i = 0; i < REPETITIONS; i++) {
            assertThat(guardrails.scan(RICH)).isEqualTo(baselineScan);
            assertThat(guardrails.redact(RICH)).isEqualTo(baselineRedact);
            assertThat(guardrails.run(RICH, Operation.SANITIZE)).isEqualTo(baselineSanitize);
            assertThat(guardrails.run(NEUTRAL, Operation.CLASSIFY)).isEqualTo(baselineClassify);
            assertThat(guardrails.scan(new StringReader(text))).isEqualTo(baselineStreamScan);
            final var streamWriter = new StringWriter();
            guardrails.redact(new StringReader(text), streamWriter);
            assertThat(streamWriter.toString()).isEqualTo(baselineStreamWriter.toString());
        }
    }

    @Test
    @DisplayName("G2 — complete redaction: every reported token is un-findable in the redacted text")
    void g2CompleteRedactionOfDetectedTokens() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var outcome = guardrails.scan(RICH);
        final var redacted = guardrails.redact(RICH);

        for (final var entry : outcome.maskEntities().entrySet()) {
            for (final var token : entry.getValue()) {
                assertThat(redacted).as("no %s token %s may survive", entry.getKey(), token)
                        .doesNotContain(token);
            }
        }

        final var streamed = new StringWriter();
        guardrails.redact(new StringReader(RICH), streamed);
        for (final var entry : outcome.maskEntities().entrySet()) {
            for (final var token : entry.getValue()) {
                assertThat(streamed.toString()).doesNotContain(token);
            }
        }

        assertThat(guardrails.redact(redacted)).isEqualTo(redacted);
        assertThat(guardrails.scan(redacted).detected()).isFalse();
    }

    @Test
    @DisplayName("G3 — typed placeholders: every replacement exactly names the policy rule")
    void g3TypedPlaceholdersIdentifyThePolicyRule() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var redacted = guardrails.redact(FIXTURE);

        assertThat(redacted).isEqualTo(
                "Email <EMAIL_ADDRESS> token <SECRET> visit <LINK> <KEYWORD>");

        final var cardRedacted = guardrails.redact("paid 4111111111111111 now");
        assertThat(cardRedacted).contains("<CREDIT_CARD>").doesNotContain("4111111111111111");
    }

    @Test
    @DisplayName("G4 — fail-safe: an errored check is never a pass and redaction aborts (fail-closed)")
    void g4ErroredClassifierNeverPasses() {
        final var guardrails = Guardrails.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .withClassifier(new ThrowingClassifier("jailbreak"))
                .build();
        final var outcome = guardrails.run(NEUTRAL, Operation.CLASSIFY);

        assertThat(outcome.detected()).isTrue();
        assertThat(outcome.isPassed()).isFalse();
        assertThat(outcome.messages()).isNotEmpty();
    }

    @Test
    @DisplayName("G4 — fail-safe: in-memory and streaming redaction abort before emitting text")
    void g4RedactionFailsClosedOnCheckError() {
        final var pipeline = new GuardrailPipeline(
                GuardrailsConfig.DEFAULTS,
                List.of(new ThrowingCheck()),
                List.of(),
                true);
        assertThatThrownBy(() -> pipeline.redact(RICH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed");

        final var stream = new StreamPipeline(
                GuardrailsConfig.DEFAULTS,
                List.of(new ThrowingStreamCheck()),
                List.of());
        assertThat(stream.scan(new StringReader(RICH)).errorMessages()).isNotEmpty();

        final var writer = new StringWriter();
        assertThatThrownBy(() -> stream.redact(new StringReader(RICH), writer))
                .isInstanceOf(IllegalStateException.class);
        assertThat(writer.toString()).isEmpty();
    }

    @Test
    @DisplayName("G5 — zero side effects: no output to any sink and no classifier call outside run(CLASSIFY)")
    void g5ZeroSideEffectsOnAnyPath() {
        final var capturing = new CapturingClassifier("jailbreak", true, 0.9);
        final var guardrails = Guardrails.builder()
                .config(config())
                .withClassifier(capturing)
                .build();

        guardrails.scan(RICH);
        guardrails.redact(RICH);
        guardrails.run(RICH, Operation.SANITIZE);
        final var streamWriter = new StringWriter();
        guardrails.redact(new StringReader(big(RICH)), streamWriter);
        guardrails.scan(new StringReader(big(RICH)));
        assertThat(capturing.saw()).isNull();

        final var out = new ByteArrayOutputStream();
        final var err = new ByteArrayOutputStream();
        final var originalOut = System.out;
        final var originalErr = System.err;
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
        try {
            guardrails.run(NEUTRAL, Operation.CLASSIFY);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        assertThat(capturing.saw()).isNotNull();
        assertThat(out.toString(StandardCharsets.UTF_8)).isEmpty();
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    @DisplayName("G6 — bounded-window streaming: large inputs stream correctly with small windows")
    void g6StreamingUsesBoundedWindowsOnLargeInput() throws IOException {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var text = RICH.repeat(1000);

        final var fromString = guardrails.redact(text);
        final var facadeWriter = new StringWriter();
        guardrails.redact(new StringReader(text), facadeWriter);
        assertThat(facadeWriter.toString()).isEqualTo(fromString);
        assertThat(guardrails.scan(new StringReader(text)).maskEntities())
                .isEqualTo(guardrails.scan(text).maskEntities());

        final var masks = guardrails.scan(text).maskEntities();
        final var tinyWindow = new StreamRedactor(
                new TextChunker(new StringReader(text), 512, 64), masks);
        final var tinyWriter = new StringWriter();
        tinyWindow.redact(tinyWriter);
        assertThat(tinyWriter.toString()).isEqualTo(fromString);
    }

    @Test
    @DisplayName("G7 — reproducible audit data: records are returned as data, deterministically")
    void g7AuditRecordsReturnedAsReproducibleData() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var baseline = guardrails.scan(FIXTURE);
        assertThat(baseline.auditRecords()).isNotEmpty();
        assertThat(baseline.auditRecords().stream().map(AuditRecord::entityType))
                .contains("pii", "secret", "link", "keyword");

        for (var i = 0; i < 5; i++) {
            assertThat(guardrails.scan(FIXTURE).auditRecords()).isEqualTo(baseline.auditRecords());
        }

        final var withLlm = Guardrails.builder()
                .config(config())
                .withClassifier(new FlaggingClassifier("jailbreak", true, 0.9))
                .build();
        final var outcome = withLlm.run(NEUTRAL, Operation.CLASSIFY);
        assertThat(outcome.auditRecords().stream().map(AuditRecord::entityType))
                .contains("jailbreak");
    }

    @Test
    @DisplayName("N1 — LLM confidence is the model's opinion; only the configured threshold gates it")
    void n1ConfidenceIsModelOpinionScaledByThreshold() {
        assertThat(guardrailsWith("jailbreak", true, 0.9).run(NEUTRAL, Operation.CLASSIFY).detected())
                .isTrue();
        assertThat(guardrailsWith("jailbreak", true, 0.1).run(NEUTRAL, Operation.CLASSIFY).detected())
                .isFalse();
        assertThat(guardrailsWith("jailbreak", false, 0.9).run(NEUTRAL, Operation.CLASSIFY).detected())
                .isFalse();

        final var tuned = Guardrails.builder()
                .config(GuardrailsConfig.builder()
                        .jailbreak(new LlmCheckConfig(true, 0.5))
                        .build())
                .withClassifier(new FlaggingClassifier("jailbreak", true, 0.6))
                .build();
        assertThat(tuned.run(NEUTRAL, Operation.CLASSIFY).detected()).isTrue();
    }

    @Test
    @DisplayName("N2 — detection is catalog-bounded: obfuscation defeats the regexes")
    void n2ObfuscationDefeatsExhaustiveDetection() {
        final var guardrails = Guardrails.builder().config(GuardrailsConfig.DEFAULTS).build();
        assertThat(guardrails.scan("contact visal@example.com please").detected()).isTrue();
        assertThat(guardrails.scan("contact visal AT example DOT com please").detected()).isFalse();
    }

    @Test
    @DisplayName("N3 — logging is the caller's decision; the library returns raw data and a log-safe form")
    void n3LoggingIsACallerDecisionTheLibraryReturnsRawData() {
        final var guardrails = Guardrails.builder().config(config()).build();
        final var scan = guardrails.scan(RICH);
        assertThat(scan.text()).isEqualTo(RICH);
        assertThat(guardrails.run(RICH, Operation.CLASSIFY).text()).isEqualTo(RICH);
        for (final var record : scan.auditRecords()) {
            assertThat(record.rawText()).isEqualTo(RICH);
        }
        final var redacted = guardrails.redact(RICH);
        assertThat(redacted).doesNotContain(
                "visal@example.com", "4111111111111111", "wJalrXUtnFEMIK7p2x1qK");
    }

    @Test
    @DisplayName("N4 — downstream behavior is out of scope: the library returns data and stops")
    void n4DownstreamRoutingIsOutOfScope() {
        final var guardrails = Guardrails.builder()
                .config(config())
                .withClassifier(new FlaggingClassifier("jailbreak", true, 0.9))
                .build();
        final var outcome = guardrails.run(NEUTRAL, Operation.CLASSIFY);
        assertThat(outcome.detected()).isTrue();
        assertThat(outcome.isPassed()).isEqualTo(!outcome.detected());
        assertThat(outcome.messages()).isEmpty();
        assertThat(guardrails.redact(RICH)).contains("EMAIL_ADDRESS");
    }

    @Test
    @DisplayName("N5 — entropy scanning trades precision for recall; the preset is the caller's dial")
    void n5EntropyScanningTradesPrecisionForRecall() {
        final var code = "confirmation code K7Qm2Xp9Rt4Zn8Wv3 sent by email";
        final var balanced = Guardrails.builder().config(GuardrailsConfig.DEFAULTS).build();
        assertThat(balanced.scan(code).maskEntities()).containsKeys("secret");

        final var permissive = Guardrails.builder()
                .config(GuardrailsConfig.builder()
                        .secrets(new SecretConfig(SecretPreset.PERMISSIVE, Map.of()))
                        .build())
                .build();
        assertThat(permissive.scan(code).maskEntities()).doesNotContainKey("secret");
    }

    @Test
    @DisplayName("end-to-end — all families across the full API surface align with the §12.3 shapes")
    void endToEndAllFamiliesMatchApiSurface() {
        final var guardrails = Guardrails.builder()
                .config(config())
                .withClassifier(new FlaggingClassifier("jailbreak", true, 0.9))
                .build();

        final var scan = guardrails.scan(RICH);
        assertThat(scan.detected()).isTrue();
        assertThat(scan.entityTypes())
                .contains("pii_email_address", "pii_credit_card", "secret", "link", "keyword");
        assertThat(scan.maskEntities()).isNotEmpty();
        assertThat(scan.auditRecords()).isNotEmpty();

        final var redacted = guardrails.redact(RICH);
        assertThat(redacted).contains(
                "<EMAIL_ADDRESS>", "<CREDIT_CARD>", "<SECRET>", "<LINK>", "<KEYWORD>");
        assertThat(redacted).doesNotContain(
                "visal@example.com", "sok@example.com", "4111111111111111",
                "wJalrXUtnFEMIK7p2x1qK", "https://example.com/login", "confidential", "urgent");

        final var streamWriter = new StringWriter();
        guardrails.redact(new StringReader(big(RICH)), streamWriter);
        assertThat(streamWriter.toString()).isEqualTo(guardrails.redact(big(RICH)));
        assertThat(guardrails.scan(new StringReader(big(RICH))).maskEntities())
                .isEqualTo(guardrails.scan(big(RICH)).maskEntities());

        final var sanitized = guardrails.run(RICH, Operation.SANITIZE);
        assertThat(sanitized.text()).isEqualTo(redacted);
        assertThat(sanitized.detected()).isFalse();

        final var classified = guardrails.run(NEUTRAL, Operation.CLASSIFY);
        assertThat(classified.detected()).isTrue();
        assertThat(classified.entityType()).isEqualTo("jailbreak");
        assertThat(classified.validations()).isNotEmpty();
        assertThat(classified.messages()).isEmpty();
    }

    private static Guardrails guardrailsWith(final String bean,
                                             final boolean flagged,
                                             final double confidence) {
        return Guardrails.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .withClassifier(new FlaggingClassifier(bean, flagged, confidence))
                .build();
    }

    private static class CapturingClassifier implements LlmClassifier {

        private final String bean;
        private final boolean flagged;
        private final double confidence;
        private String seen;

        private CapturingClassifier(final String bean,
                                    final boolean flagged,
                                    final double confidence) {
            this.bean = bean;
            this.flagged = flagged;
            this.confidence = confidence;
        }

        @Override
        public Verdict classify(final String input) {
            this.seen = input;
            return new Verdict(flagged, confidence);
        }

        @Override
        public String beanName() {
            return bean;
        }

        private String saw() {
            return seen;
        }
    }

    private static final class FlaggingClassifier extends CapturingClassifier {

        private FlaggingClassifier(final String bean,
                                   final boolean flagged,
                                   final double confidence) {
            super(bean, flagged, confidence);
        }
    }

    private static final class ThrowingClassifier implements LlmClassifier {

        private final String bean;

        private ThrowingClassifier(final String bean) {
            this.bean = bean;
        }

        @Override
        public Verdict classify(final String input) {
            throw new IllegalStateException(bean + " model unavailable");
        }

        @Override
        public String beanName() {
            return bean;
        }
    }

    private static final class ThrowingCheck implements GuardrailCheck {

        @Override
        public GuardrailResult run(final String input) {
            throw new IllegalStateException("preflight check exploded");
        }
    }

    private static final class ThrowingStreamCheck implements GuardrailCheck {

        @Override
        public GuardrailResult run(final String input) {
            return GuardrailResult.pass("throwing", input);
        }

        @Override
        public StreamCheck toStream() {
            return (final var input, final var sink) -> {
                throw new IllegalStateException("stream check exploded");
            };
        }
    }
}