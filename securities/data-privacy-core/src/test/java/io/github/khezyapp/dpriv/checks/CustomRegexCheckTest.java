package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.CustomRegexConfig;
import io.github.khezyapp.dpriv.redact.Redactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the standalone custom-regex check (design §9.5): one {@code maskEntities} group per
 * rule name, full-text tokens unique-first-seen, own (non-{@code pii_*}) group keys, redaction to
 * the {@code <NAME>} family, and the empty/malformed no-op behavior.
 */
class CustomRegexCheckTest {

    private static CustomRegexCheck check(final CustomRegexConfig... configs) {
        return new CustomRegexCheck(List.of(configs), new Redactor());
    }

    private static CustomRegexConfig group(final String name, final String regex) {
        return new CustomRegexConfig(name, List.of(Pattern.compile(regex)));
    }

    @Test
    @DisplayName("should be an identity pass for an empty config list")
    void emptyConfigIsPass() {
        final var input = "plain text here";
        final var result = new CustomRegexCheck(List.of(), new Redactor()).run(input);

        assertThat(result.entityType()).isEqualTo("custom");
        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEmpty();
        assertThat(result.cleanedValue()).isSameAs(input);
    }

    @Test
    @DisplayName("should skip malformed or blank names without throwing")
    void skipsBlankName() {
        final var blank = new CustomRegexConfig("  ", List.of(Pattern.compile("\\bOR-\\d{5}\\b")));
        final var input = "order OR-12345";
        final var result = check(blank).run(input);

        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEmpty();
        assertThat(result.cleanedValue()).isSameAs(input);
    }

    @Test
    @DisplayName("should put each matched token under its own rule-name key")
    void ownGroupKeyPerName() {
        final var result = check(group("order_ref", "\\bOR-\\d{5}\\b"))
                .run("order OR-12345");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities()).containsOnlyKeys("order_ref");
        assertThat(result.maskEntities().get("order_ref")).containsExactly("OR-12345");
        assertThat(result.cleanedValue()).isEqualTo("order <ORDER_REF>");
    }

    @Test
    @DisplayName("should keep groups distinct and not fold into a pii family")
    void groupsStayDistinct() {
        final var result = check(
                        group("order_ref", "\\bOR-\\d{5}\\b"),
                        group("license_ref", "\\bLC-\\d{6}\\b"))
                .run("OR-12345 and LC-987654");

        assertThat(result.maskEntities()).containsOnlyKeys("order_ref", "license_ref");
        assertThat(result.maskEntities()).doesNotContainKey("pii");
        assertThat(result.cleanedValue()).isEqualTo("<ORDER_REF> and <LICENSE_REF>");
    }

    @Test
    @DisplayName("should dedupe tokens unique-first-seen per group")
    void tokensUniqueFirstSeen() {
        final var result = check(group("order_ref", "\\bOR-\\d{5}\\b"))
                .run("OR-12345 then OR-12345 again");

        assertThat(result.maskEntities().get("order_ref")).containsExactly("OR-12345");
        assertThat(result.cleanedValue()).isEqualTo("<ORDER_REF> then <ORDER_REF> again");
    }

    @Test
    @DisplayName("should not throw when a config or its patterns are null")
    void nullConfigOrPatternsNeverThrow() {
        final var nullPatterns = new CustomRegexConfig("order_ref", null);
        final var input = "order OR-12345";
        final var withNullPatterns = new CustomRegexCheck(Arrays.asList(nullPatterns, null), new Redactor()).run(input);
        final var withNullEntry = new CustomRegexCheck(Arrays.asList(null, nullPatterns), new Redactor()).run(input);

        assertThat(withNullPatterns.isPassed()).isTrue();
        assertThat(withNullEntry.isPassed()).isTrue();
    }
}
