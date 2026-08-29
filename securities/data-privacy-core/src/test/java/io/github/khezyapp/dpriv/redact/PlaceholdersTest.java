package io.github.khezyapp.dpriv.redact;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@code <ENTITY>} placeholder mapping table (design §7.1).
 */
class PlaceholdersTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "pii_email_address, <EMAIL_ADDRESS>",
            "pii_credit_card, <CREDIT_CARD>",
            "pii_us_ssn, <US_SSN>",
            "pii_generic_pii, <GENERIC_PII>"
    })
    @DisplayName("should strip the pii_ family prefix and uppercase the rule name")
    void stripsPiiFamilyPrefix(final String entityType, final String expected) {
        assertThat(Placeholders.forEntityType(entityType)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "secret, <SECRET>",
            "link, <LINK>",
            "keyword, <KEYWORD>",
            "jailbreak, <JAILBREAK>"
    })
    @DisplayName("should map known non-PII families to their uppercased name")
    void mapsKnownNonPiiFamilies(final String entityType,
                                 final String expected) {
        assertThat(Placeholders.forEntityType(entityType)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "custom-regex-name!, <CUSTOMREGEXNAME>",
            "SECRET_KEY, <SECRET_KEY>",
            "a b c, <ABC>"
    })
    @DisplayName("should fall back to a sanitized, uppercased placeholder for unknown types")
    void fallsBackForUnknownTypes(final String entityType,
                                  final String expected) {
        assertThat(Placeholders.forEntityType(entityType)).isEqualTo(expected);
    }

    @Test
    @DisplayName("should expose a token pattern matching emitted placeholders")
    void tokenPatternMatchesPlaceholders() {
        assertThat(Placeholders.TOKEN.matcher("<EMAIL_ADDRESS>").matches()).isTrue();
        assertThat(Placeholders.TOKEN.matcher("[REDACTED]").matches()).isFalse();
    }
}
