package io.github.khezyapp.dpriv.api;

import io.github.khezyapp.dpriv.checks.LlmCheck;
import io.github.khezyapp.dpriv.pipeline.GuardrailPipeline;
import io.github.khezyapp.dpriv.pipeline.StreamPipeline;
import io.github.khezyapp.dpriv.pipeline.StageResult;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Public facade of {@code data-privacy-core} (design §12.1). The in-memory pipeline
 * ({@link #scan(String)}, {@link #redact(String)}, {@link #run(String, Operation)}) is implemented
 * here over a {@link GuardrailPipeline}; the streaming variant ({@link #scan(Reader)},
 * {@link #redact(Reader, Writer)}) runs over a {@link StreamPipeline}.
 */
public final class Guardrails {

    private final GuardrailPipeline pipeline;
    private final StreamPipeline streamPipeline;

    private Guardrails(final GuardrailPipeline pipeline,
                       final StreamPipeline streamPipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        this.streamPipeline = Objects.requireNonNull(streamPipeline, "streamPipeline");
    }

    /**
     * Creates a fluent builder for a {@link Guardrails} instance.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs the configured pipeline over {@code text} under the given {@link Operation}.
     *
     * @param text the input text
     * @param op   the operation to perform (classify or sanitize)
     * @return the outcome
     */
    public GuardrailsOutcome run(final String text,
                                 final Operation op) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(op, "op");
        return op == Operation.SANITIZE ? sanitize(text) : classify(text);
    }

    /**
     * Scans {@code text} for sensitive content.
     *
     * @param text the input text
     * @return the scan outcome
     */
    public ScanOutcome scan(final String text) {
        Objects.requireNonNull(text, "text");
        final var preflight = pipeline.preflight(text);
        return new ScanOutcome(
                text,
                primaryEntityType(preflight.validations(), preflight.maskEntities()),
                preflight.detected(),
                preflight.errors(),
                preflight.maskEntities(),
                auditRecords(preflight, text)
        );
    }

    /**
     * Redacts sensitive content from {@code text}.
     *
     * @param text the input text
     * @return the redacted text
     */
    public String redact(final String text) {
        Objects.requireNonNull(text, "text");
        return pipeline.redact(text);
    }

    /**
     * Scans a streaming input.
     *
     * @param input the streaming input
     * @return the scan outcome
     */
    public ScanOutcome scan(final Reader input) {
        Objects.requireNonNull(input, "input");
        return streamPipeline.scan(input);
    }

    /**
     * Redacts a streaming input into {@code out}.
     *
     * @param input the streaming input
     * @param out   the destination writer
     */
    public void redact(final Reader input,
                       final Writer out) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(out, "out");
        streamPipeline.redact(input, out);
    }

    private GuardrailsOutcome sanitize(final String text) {
        final var preflight = pipeline.preflight(text);
        if (!preflight.errors().isEmpty()) {
            throw new IllegalStateException("sanitize aborted on check error: " + preflight.errors());
        }
        return new GuardrailsOutcome(
                preflight.cleanedValue(),
                primaryEntityType(preflight.validations(), preflight.maskEntities()),
                false,
                preflight.validations(),
                preflight.maskEntities(),
                auditRecords(preflight, text),
                List.of()
        );
    }

    private GuardrailsOutcome classify(final String text) {
        final var preflight = pipeline.preflight(text);
        final var preflightFailed = pipeline.failOnlyOnErrors() && !preflight.errors().isEmpty();
        if (preflightFailed || preflight.detected()) {
            return new GuardrailsOutcome(
                    text,
                    primaryEntityType(preflight.validations(), preflight.maskEntities()),
                    true,
                    preflight.validations(),
                    preflight.maskEntities(),
                    auditRecords(preflight, text),
                    pipeline.failOnlyOnErrors() ? preflight.errors() : List.of()
            );
        }
        final var masked = preflight.cleanedValue();
        final var classify = pipeline.classify(masked);
        final var errored = pipeline.failOnlyOnErrors()
                && (!preflight.errors().isEmpty() || !classify.errors().isEmpty());
        final var detected = classify.detected() || errored;
        final var validations = concat(preflight.validations(), classify.validations());
        final var audit = concat(auditRecords(preflight, text), auditRecords(classify, masked));
        final List<String> messages = pipeline.failOnlyOnErrors()
                ? concat(preflight.errors(), classify.errors())
                : List.of();
        return new GuardrailsOutcome(
                text,
                primaryEntityType(validations, preflight.maskEntities()),
                detected,
                validations,
                preflight.maskEntities(),
                audit,
                messages
        );
    }

    private static String primaryEntityType(final List<GuardrailResult> validations,
                                            final Map<String, List<String>> masks) {
        for (final var result : validations) {
            if (result.detected()) {
                return result.entityType();
            }
        }
        return masks.keySet().stream().findFirst().orElse(null);
    }

    private static List<AuditRecord> auditRecords(final StageResult stage,
                                                  final String text) {
        final var records = new ArrayList<AuditRecord>();
        for (final var result : stage.validations()) {
            records.add(new AuditRecord(result.entityType(), List.of(result), text));
        }
        return List.copyOf(records);
    }

    private static <T> List<T> concat(final List<T> first,
                                      final List<T> second) {
        final var combined = new ArrayList<T>(first.size() + second.size());
        combined.addAll(first);
        combined.addAll(second);
        return List.copyOf(combined);
    }

    /**
     * Fluent builder for {@link Guardrails} (design §12.4).
     */
    public static final class Builder {

        private GuardrailsConfig config = GuardrailsConfig.DEFAULTS;
        private final List<LlmClassifier> classifiers = new ArrayList<>();
        private boolean failOnlyOnErrors = true;

        private Builder() {
        }

        /**
         * Sets the policy used to build the deterministic pipeline.
         *
         * @param value the configuration; never null
         * @return this builder
         */
        public Builder config(final GuardrailsConfig value) {
            this.config = Objects.requireNonNull(value, "config");
            return this;
        }

        /**
         * Configures the error policy for classifier failures.
         *
         * @param value true to fail the input on any classifier error
         * @return this builder
         */
        public Builder failOnlyOnErrors(final boolean value) {
            this.failOnlyOnErrors = value;
            return this;
        }

        /**
         * Registers a classifier, appended to the classificatory stage as an {@link LlmCheck}.
         *
         * @param classifier the classifier to register; never null
         * @return this builder
         */
        public Builder withClassifier(final LlmClassifier classifier) {
            this.classifiers.add(Objects.requireNonNull(classifier, "classifier"));
            return this;
        }

        /**
         * Builds the {@link Guardrails} instance.
         *
         * @return the built facade
         */
        public Guardrails build() {
            final var preflight = GuardrailPipeline.defaultPreflight(config);
            final var classificatory = classifiers.stream()
                    .map(classifier -> (GuardrailCheck) new LlmCheck(classifier, configFor(classifier)))
                    .toList();
            final var built = new GuardrailPipeline(config, preflight, classificatory, failOnlyOnErrors);
            final var stream = new StreamPipeline(config, preflight, classificatory);
            return new Guardrails(built, stream);
        }

        private LlmCheckConfig configFor(final LlmClassifier classifier) {
            return switch (classifier.beanName()) {
                case "llm" -> config.llm();
                case "jailbreak" -> config.jailbreak();
                case "nsfw" -> config.nsfw();
                case "topical" -> config.topical();
                default -> LlmCheckConfig.DEFAULTS;
            };
        }
    }
}
