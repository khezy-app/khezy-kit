package io.github.khezyapp.dpriv.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the PII catalog (design §8): the 33 entity patterns match their canonical examples and
 * reject boundary-padded near-misses, the {@code strict} checksum gate only tightens the three
 * checksum-validatable entities, and the catalog stays 1:1 with the {@link PiiEntity} enum.
 */
class PiiPatternsTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("entityExamples")
    @DisplayName("matches each entity's canonical example and rejects a near-miss negative")
    void matchesCanonicalExampleAndRejectsPadded(final PiiEntity entity,
                                                 final String positive,
                                                 final String negative) {
        assertThat(PiiPatterns.isNonStrictMatch(entity, positive)).isTrue();
        assertThat(PiiPatterns.isNonStrictMatch(entity, negative)).isFalse();
    }

    @Test
    @DisplayName("strict mode accepts valid checksum tokens for the three validatable entities")
    void strictAcceptsChecksumValidTokens() {
        assertThat(PiiPatterns.isStrictMatch(PiiEntity.CREDIT_CARD, "4111-1111-1111-1111")).isTrue();
        assertThat(PiiPatterns.isStrictMatch(PiiEntity.IBAN_CODE, "GB82WEST12345698765432"))
                .isTrue();
        assertThat(PiiPatterns.isStrictMatch(PiiEntity.IN_AADHAAR, "4852 7504 5745")).isTrue();
    }

    @Test
    @DisplayName("a pattern match that fails its checksum still passes non-strict, never strict")
    void failedChecksumBreaksStrictMatchOnly() {
        final var invalidCard = "4111111111111112";
        assertThat(invalidCard).hasSize(16);
        assertThat(PiiPatterns.isNonStrictMatch(PiiEntity.CREDIT_CARD, invalidCard)).isTrue();
        assertThat(PiiPatterns.isStrictMatch(PiiEntity.CREDIT_CARD, invalidCard)).isFalse();
    }

    @Test
    @DisplayName("non-checksum entities are not gated by strict mode")
    void nonChecksumEntitiesAreNotGatedByStrict() {
        assertThat(PiiPatterns.isStrictMatch(PiiEntity.US_SSN, "123-45-6789")).isTrue();
        assertThat(PiiPatterns.isStrictMatch(PiiEntity.US_SSN, "a123-45-6789b")).isFalse();
        assertThat(PiiPatterns.isNonStrictMatch(PiiEntity.US_SSN, "a123-45-6789b")).isFalse();
    }

    @Test
    @DisplayName("all() holds exactly the 33 design §8 entities, matching PiiEntity 1:1")
    void catalogMatchesEntityEnumExactly() {
        final var all = PiiPatterns.all();
        assertThat(all).hasSize(33);
        assertThat(all.keySet()).isEqualTo(EnumSet.allOf(PiiEntity.class));
        assertThat(all).containsOnlyKeys(PiiEntity.values());
    }

    @Test
    @DisplayName("every PiiEntity constant and type() pin the design §8 names and pii_ contract")
    void entityConstantsMatchDesignSection8Exactly() {
        final var expectedNames = List.of(
                "CREDIT_CARD", "CRYPTO", "EMAIL_ADDRESS", "IP_ADDRESS", "PHONE_NUMBER",
                "IBAN_CODE", "LOCATION", "DATE_TIME", "MEDICAL_LICENSE",
                "US_BANK_NUMBER", "US_DRIVER_LICENSE", "US_ITIN", "US_PASSPORT", "US_SSN",
                "UK_NHS", "UK_NINO",
                "ES_NIF", "ES_NIE",
                "IT_FISCAL_CODE", "IT_VAT_CODE",
                "PL_PESEL",
                "SG_NRIC_FIN", "SG_UEN",
                "AU_ABN", "AU_ACN", "AU_TFN", "AU_MEDICARE",
                "IN_PAN", "IN_AADHAAR", "IN_VEHICLE_REGISTRATION", "IN_VOTER", "IN_PASSPORT",
                "FI_PERSONAL_IDENTITY_CODE");
        assertThat(PiiEntity.values())
                .extracting(Enum::name)
                .containsExactlyElementsOf(expectedNames);
        for (final var entity : PiiEntity.values()) {
            assertThat(entity.type())
                    .isEqualTo("pii_" + entity.name().toLowerCase(Locale.ROOT));
        }
    }

    @Test
    @DisplayName("all() is unmodifiable")
    void allIsUnmodifiable() {
        assertThatThrownBy(() -> PiiPatterns.all().put(PiiEntity.CRYPTO, Pattern.compile("x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("forEntity returns the same compiled singleton every call")
    void forEntityIsCachedSingleton() {
        assertThat(PiiPatterns.forEntity(PiiEntity.US_SSN))
                .isSameAs(PiiPatterns.forEntity(PiiEntity.US_SSN));
        assertThat(PiiPatterns.all().get(PiiEntity.US_SSN))
                .isSameAs(PiiPatterns.forEntity(PiiEntity.US_SSN));
    }

    static Stream<Arguments> entityExamples() {
        return Stream.of(
                Arguments.of(PiiEntity.CREDIT_CARD, "4111-1111-1111-1111", "a4111-1111-1111-1111b"),
                Arguments.of(PiiEntity.CRYPTO,
                        "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2",
                        "a1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2b"),
                Arguments.of(PiiEntity.EMAIL_ADDRESS, "visal@example.com", "visal@example"),
                Arguments.of(PiiEntity.IP_ADDRESS, "192.168.1.1", "x192.168.1.1y"),
                Arguments.of(PiiEntity.PHONE_NUMBER, "+855 123 4567", "x+855 123 4567y"),
                Arguments.of(PiiEntity.IBAN_CODE, "GB82WEST12345698765432",
                        "xGB82WEST12345698765432y"),
                Arguments.of(PiiEntity.LOCATION, "Main Street", "xMain Streety"),
                Arguments.of(PiiEntity.DATE_TIME, "12/31/2020", "a12/31/2020b"),
                Arguments.of(PiiEntity.MEDICAL_LICENSE, "AB123456", "xAB123456y"),
                Arguments.of(PiiEntity.US_BANK_NUMBER, "12341234", "a12341234b"),
                Arguments.of(PiiEntity.US_DRIVER_LICENSE, "D1234567", "xD1234567y"),
                Arguments.of(PiiEntity.US_ITIN, "912-34-5678", "a912-34-5678b"),
                Arguments.of(PiiEntity.US_PASSPORT, "E12345678", "xE12345678y"),
                Arguments.of(PiiEntity.US_SSN, "123-45-6789", "a123-45-6789b"),
                Arguments.of(PiiEntity.UK_NHS, "123 456 7890", "a123 456 7890b"),
                Arguments.of(PiiEntity.UK_NINO, "AB123456C", "xAB123456Cy"),
                Arguments.of(PiiEntity.ES_NIF, "A12345678", "xA12345678y"),
                Arguments.of(PiiEntity.ES_NIE, "X12345678", "xX12345678y"),
                Arguments.of(PiiEntity.IT_FISCAL_CODE, "RSSMRA85T10A562S", "xRSSMRA85T10A562Sy"),
                Arguments.of(PiiEntity.IT_VAT_CODE, "IT12345678901", "aIT12345678901b"),
                Arguments.of(PiiEntity.PL_PESEL, "12345678901", "a12345678901b"),
                Arguments.of(PiiEntity.SG_NRIC_FIN, "S1234567A", "xS1234567Ay"),
                Arguments.of(PiiEntity.SG_UEN, "12345678A", "x12345678Ay"),
                Arguments.of(PiiEntity.AU_ABN, "12 345 678 901", "x12 345 678 901y"),
                Arguments.of(PiiEntity.AU_ACN, "123 456 789", "x123 456 789y"),
                Arguments.of(PiiEntity.AU_TFN, "123456789", "a123456789b"),
                Arguments.of(PiiEntity.AU_MEDICARE, "1234 12345 1", "a1234 12345 1b"),
                Arguments.of(PiiEntity.IN_PAN, "ABCDE1234F", "xABCDE1234Fy"),
                Arguments.of(PiiEntity.IN_AADHAAR, "4852 7504 5745", "a4852 7504 5745b"),
                Arguments.of(PiiEntity.IN_VEHICLE_REGISTRATION, "KA01AB1234", "xKA01AB1234y"),
                Arguments.of(PiiEntity.IN_VOTER, "ABC1234567", "xABC1234567y"),
                Arguments.of(PiiEntity.IN_PASSPORT, "A1234567", "xA1234567y"),
                Arguments.of(PiiEntity.FI_PERSONAL_IDENTITY_CODE, "010101-123T", "a010101-123Tb"));
    }
}
