package io.github.khezyapp.dpriv.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the checksum validators (design §8.2): Luhn for {@code CREDIT_CARD}, ISO 7064 mod-97
 * for {@code IBAN_CODE}, and Verhoeff for {@code IN_AADHAAR}. Each validator fails soft on
 * malformed/scrambled input instead of throwing.
 */
class ChecksumValidatorsTest {

    @ParameterizedTest(name = "luhn accepts {0}")
    @ValueSource(strings = {
            "4111111111111111",
            "5555555555554444",
            "4111-1111-1111-1111",
            "4111 1111 1111 1111"
    })
    @DisplayName("luhn accepts known-good card numbers and separator forms")
    void luhnAcceptsKnownGood(final String card) {
        assertThat(ChecksumValidators.luhn(card)).isTrue();
    }

    @ParameterizedTest(name = "luhn rejects {0}")
    @ValueSource(strings = {
            "4111111111111112",
            "5555555555554445",
            "1234",
            "41111111111111111111",
            "abcdef",
            "credit-card"
    })
    @DisplayName("luhn rejects toggled, too-short, too-long, and scrambled input")
    void luhnRejectsInvalid(final String card) {
        assertThat(ChecksumValidators.luhn(card)).isFalse();
    }

    @Test
    @DisplayName("luhn returns false for null")
    void luhnRejectsNull() {
        assertThat(ChecksumValidators.luhn(null)).isFalse();
    }

    @ParameterizedTest(name = "mod97 accepts {0}")
    @ValueSource(strings = {
            "GB82 WEST 1234 5698 7654 32",
            "GB82WEST12345698765432",
            "IBAN GB82 WEST 1234 5698 7654 32",
            "gb82 west 1234 5698 7654 32"
    })
    @DisplayName("mod97 accepts the canonical GB82 IBAN, spaced, compact, prefixed, and lowercase")
    void mod97AcceptsKnownGood(final String iban) {
        assertThat(ChecksumValidators.mod97(iban)).isTrue();
    }

    @ParameterizedTest(name = "mod97 rejects {0}")
    @ValueSource(strings = {
            "GB82 WEST 1234 5698 7654 33",
            "GB82WEST1234569876543",
            "hello world",
            "GB8 2WEST 1234 5698 7654 3x"
    })
    @DisplayName("mod97 rejects corrupted, truncated, and malformed IBANs")
    void mod97RejectsInvalid(final String iban) {
        assertThat(ChecksumValidators.mod97(iban)).isFalse();
    }

    @Test
    @DisplayName("mod97 returns false for null")
    void mod97RejectsNull() {
        assertThat(ChecksumValidators.mod97(null)).isFalse();
    }

    @ParameterizedTest(name = "verhoeff accepts {0}")
    @ValueSource(strings = {
            "4852 7504 5745",
            "485275045745"
    })
    @DisplayName("verhoeff accepts a valid 12-digit Aadhaar")
    void verhoeffAcceptsKnownGood(final String aadhaar) {
        assertThat(ChecksumValidators.verhoeff(aadhaar)).isTrue();
    }

    @ParameterizedTest(name = "verhoeff rejects {0}")
    @ValueSource(strings = {
            "485275045746",
            "4852750457451",
            "2363",
            "48 52 ab cd 45"
    })
    @DisplayName("verhoeff rejects altered, wrong-length, and scrambled input")
    void verhoeffRejectsInvalid(final String value) {
        assertThat(ChecksumValidators.verhoeff(value)).isFalse();
    }

    @Test
    @DisplayName("verhoeff returns false for null")
    void verhoeffRejectsNull() {
        assertThat(ChecksumValidators.verhoeff(null)).isFalse();
    }
}
