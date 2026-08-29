package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.CustomRegexConfig;
import io.github.khezyapp.dpriv.api.PiiConfig;
import io.github.khezyapp.dpriv.api.PiiCoverage;
import io.github.khezyapp.dpriv.policy.PiiEntity;
import io.github.khezyapp.dpriv.redact.Redactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the aggregated PII check (design §9.1): coverage resolution, strict/non-strict
 * checksum gating, per-entity {@code maskEntities} keys, custom-group folding, redaction to
 * {@code <ENTITY>} families, and the no-match pass.
 */
class PiiCheckTest {

    private static PiiCheck check(final PiiConfig config) {
        return new PiiCheck(config, new Redactor());
    }

    private static PiiConfig selected(final Set<PiiEntity> entities,
                                      final List<CustomRegexConfig> custom,
                                      final boolean strict) {
        return new PiiConfig(PiiCoverage.SELECTED, entities, custom, strict);
    }

    private static PiiConfig customOnly(final CustomRegexConfig... configs) {
        return selected(Set.of(PiiEntity.EMAIL_ADDRESS), List.of(configs), true);
    }

    @Test
    @DisplayName("should detect email and geo location under ALL coverage and redact the families")
    void allCoverageDetectsEmailAndLocation() {
        final var input = "Contact visal@example.com, 123 Monivong Boulevard, Phnom Penh";
        final var result = check(PiiConfig.DEFAULTS).run(input);

        assertThat(result.entityType()).isEqualTo("pii");
        assertThat(result.detected()).isTrue();
        assertThat(result.isPassed()).isFalse();
        assertThat(result.maskEntities().get("pii_email_address")).containsExactly("visal@example.com");
        assertThat(result.maskEntities().get("pii_location")).containsExactly(" Monivong Boulevard");
        assertThat(result.cleanedValue())
                .contains("<EMAIL_ADDRESS>")
                .contains("<LOCATION>")
                .doesNotContain("visal@example.com")
                .doesNotContain("Monivong Boulevard");
    }

    @Test
    @DisplayName("should scan only the selected entity, ignoring every other PII type")
    void selectedEmailOnlyLeavesOtherTypesAlone() {
        final var config = selected(Set.of(PiiEntity.EMAIL_ADDRESS), List.of(), true);
        final var result = check(config).run("visal@example.com 123 Monivong Boulevard");

        assertThat(result.maskEntities()).containsOnlyKeys("pii_email_address");
        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities()).doesNotContainKey("pii_location");
    }

    @Test
    @DisplayName("should pass when a non-email fixture is scanned for EMAIL_ADDRESS only")
    void selectedEmailOnlyPassesWithoutEmail() {
        final var config = selected(Set.of(PiiEntity.EMAIL_ADDRESS), List.of(), true);
        final var input = "123 Monivong Boulevard, Phnom Penh";
        final var result = check(config).run(input);

        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEmpty();
        assertThat(result.cleanedValue()).isSameAs(input);
    }

    @Test
    @DisplayName("should reject a checksum-failing credit card under strict and accept it non-strict")
    void strictGatesChecksumBackedEntities() {
        final var input = "card 4111111111111112";
        final var strictResult = check(selected(Set.of(PiiEntity.CREDIT_CARD), List.of(), true))
                .run(input);
        final var nonStrictResult = check(selected(Set.of(PiiEntity.CREDIT_CARD), List.of(), false))
                .run(input);

        assertThat(strictResult.detected()).isFalse();
        assertThat(strictResult.isPassed()).isTrue();
        assertThat(strictResult.maskEntities()).isEmpty();

        assertThat(nonStrictResult.detected()).isTrue();
        assertThat(nonStrictResult.maskEntities().get("pii_credit_card"))
                .containsExactly("4111111111111112");
        assertThat(nonStrictResult.cleanedValue()).isEqualTo("card <CREDIT_CARD>");
    }

    @Test
    @DisplayName("should keep a valid credit card detected under strict too")
    void strictAcceptsChecksumValidCard() {
        final var config = selected(Set.of(PiiEntity.CREDIT_CARD), List.of(), true);
        final var result = check(config).run("card 4111111111111111");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("pii_credit_card")).containsExactly("4111111111111111");
    }

    @Test
    @DisplayName("should emit a custom group as its own maskEntities key alongside pii keys")
    void customGroupAppearsAlongsidePiiKeys() {
        final var orderRef = new CustomRegexConfig("order_ref", List.of(Pattern.compile("\\bOR-\\d{5}\\b")));
        final var config = selected(Set.of(PiiEntity.EMAIL_ADDRESS), List.of(orderRef), true);
        final var result = check(config).run("Order OR-12345, email visal@example.com");

        assertThat(result.maskEntities()).containsOnlyKeys("pii_email_address", "order_ref");
        assertThat(result.maskEntities().get("pii_email_address")).containsExactly("visal@example.com");
        assertThat(result.maskEntities().get("order_ref")).containsExactly("OR-12345");
        assertThat(result.detected()).isTrue();
    }

    @Test
    @DisplayName("should detect when only a custom group matches (no PII)")
    void detectedWhenOnlyCustomMatches() {
        final var orderRef = new CustomRegexConfig("order_ref", List.of(Pattern.compile("\\bOR-\\d{5}\\b")));
        final var result = check(customOnly(orderRef)).run("order OR-12345");

        assertThat(result.entityType()).isEqualTo("pii");
        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities()).containsOnlyKeys("order_ref");
        assertThat(result.maskEntities()).doesNotContainKey("pii_email_address");
    }

    @Test
    @DisplayName("should redact both families and leave no raw token surviving")
    void cleanedValueShowsBothFamiliesAndHidesRawTokens() {
        final var orderRef = new CustomRegexConfig("order_ref", List.of(Pattern.compile("\\bOR-\\d{5}\\b")));
        final var config = selected(Set.of(PiiEntity.EMAIL_ADDRESS), List.of(orderRef), true);
        final var input = "Order OR-12345 from visal@example.com";
        final var result = check(config).run(input);

        assertThat(result.cleanedValue()).isEqualTo("Order <ORDER_REF> from <EMAIL_ADDRESS>");
        for (final var tokens : result.maskEntities().values()) {
            for (final var token : tokens) {
                assertThat(result.cleanedValue()).doesNotContain(token);
            }
        }
    }

    @Test
    @DisplayName("should pass with unchanged cleanedValue on a no-match prose input")
    void noMatchIsPass() {
        final var config = selected(Set.of(PiiEntity.EMAIL_ADDRESS), List.of(), true);
        final var input = "no sensitive data in this report";
        final var result = check(config).run(input);

        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEmpty();
        assertThat(result.cleanedValue()).isSameAs(input);
    }
}
