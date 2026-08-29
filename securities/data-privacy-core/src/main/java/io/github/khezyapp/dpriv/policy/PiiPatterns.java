package io.github.khezyapp.dpriv.policy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Precompiled PII detection catalog (design §8). Ports the upstream {@code DEFAULT_PII_PATTERNS}
 * pattern table (OpenAI Guardrails JS / n8n — MIT, attributed in {@code CREDITS.md}) one regex per
 * {@link PiiEntity}; each pattern is compiled exactly once and reused. {@link #isNonStrictMatch} is
 * pattern-only; {@link #isStrictMatch} additionally requires a valid checksum for the three
 * checksum-validatable entities (design §8.2: {@code CREDIT_CARD}/{@code IBAN_CODE}/
 * {@code IN_AADHAAR}).
 */
public final class PiiPatterns {

    private static final String CREDIT_CARD =
            "\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b";
    private static final String CRYPTO =
            "\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b";
    private static final String EMAIL_ADDRESS =
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b";
    private static final String IP_ADDRESS =
            "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b";
    private static final String PHONE_NUMBER =
            "\\b[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}\\b";
    private static final String IBAN_CODE =
            "\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}([A-Z0-9]?){0,16}\\b";
    private static final String LOCATION =
            "\\b[A-Za-z\\s]+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Drive|Dr|Lane|Ln|Place|Pl"
                    + "|Court|Ct|Way|Highway|Hwy)\\b";
    private static final String DATE_TIME =
            "\\b(0[1-9]|1[0-2])[/\\-](0[1-9]|[12]\\d|3[01])[/\\-](19|20)\\d{2}\\b";
    private static final String MEDICAL_LICENSE =
            "\\b[A-Z]{2}\\d{6}\\b";

    private static final String US_BANK_NUMBER =
            "\\b\\d{8,17}\\b";
    private static final String US_DRIVER_LICENSE =
            "\\b[A-Z]\\d{7}\\b";
    private static final String US_ITIN =
            "\\b9\\d{2}-\\d{2}-\\d{4}\\b";
    private static final String US_PASSPORT =
            "\\b[A-Z]\\d{8}\\b";
    private static final String US_SSN =
            "\\b\\d{3}-\\d{2}-\\d{4}\\b|\\b\\d{9}\\b";

    private static final String UK_NHS =
            "\\b\\d{3} \\d{3} \\d{4}\\b";
    private static final String UK_NINO =
            "\\b[A-Z]{2}\\d{6}[A-Z]\\b";

    private static final String ES_NIF =
            "\\b[A-Z]\\d{8}\\b";
    private static final String ES_NIE =
            "\\b[A-Z]\\d{8}\\b";

    private static final String IT_FISCAL_CODE =
            "\\b[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]\\b";
    private static final String IT_VAT_CODE =
            "\\bIT\\d{11}\\b";

    private static final String PL_PESEL =
            "\\b\\d{11}\\b";

    private static final String SG_NRIC_FIN =
            "\\b[A-Z]\\d{7}[A-Z]\\b";
    private static final String SG_UEN =
            "\\b\\d{8}[A-Z]\\b|\\b\\d{9}[A-Z]\\b";

    private static final String AU_ABN =
            "\\b\\d{2} \\d{3} \\d{3} \\d{3}\\b";
    private static final String AU_ACN =
            "\\b\\d{3} \\d{3} \\d{3}\\b";
    private static final String AU_TFN =
            "\\b\\d{9}\\b";
    private static final String AU_MEDICARE =
            "\\b\\d{4} \\d{5} \\d{1}\\b";

    private static final String IN_PAN =
            "\\b[A-Z]{5}\\d{4}[A-Z]\\b";
    private static final String IN_AADHAAR =
            "\\b\\d{4} \\d{4} \\d{4}\\b";
    private static final String IN_VEHICLE_REGISTRATION =
            "\\b[A-Z]{2}\\d{2}[A-Z]{2}\\d{4}\\b";
    private static final String IN_VOTER =
            "\\b[A-Z]{3}\\d{7}\\b";
    private static final String IN_PASSPORT =
            "\\b[A-Z]\\d{7}\\b";

    private static final String FI_PERSONAL_IDENTITY_CODE =
            "\\b\\d{6}[+-A]\\d{3}[A-Z0-9]\\b";

    private PiiPatterns() {
    }

    /**
     * The precompiled, lazily-built singleton pattern for an entity.
     *
     * @param entity the catalog entity
     * @return the cached compiled pattern
     */
    public static Pattern forEntity(final PiiEntity entity) {
        return Holder.PATTERNS.get(Objects.requireNonNull(entity, "entity"));
    }

    /**
     * The full catalog as an unmodifiable map, ordered by enum constant declaration.
     *
     * @return every entity to its compiled pattern
     */
    public static Map<PiiEntity, Pattern> all() {
        return Holder.PATTERNS;
    }

    /**
     * Pattern-only detection, independent of the {@code strict} checksum layer (design §8.2).
     *
     * @param entity the catalog entity
     * @param token  the candidate text
     * @return {@code true} when the entity's pattern finds a match inside {@code token}
     */
    public static boolean isNonStrictMatch(final PiiEntity entity,
                                           final String token) {
        return forEntity(Objects.requireNonNull(entity, "entity"))
                .matcher(Objects.requireNonNull(token, "token"))
                .find();
    }

    /**
     * Strict detection: the pattern must match and, for the three checksum-validatable entities
     * ({@code CREDIT_CARD}/Luhn, {@code IBAN_CODE}/mod-97, {@code IN_AADHAAR}/Verhoeff), the token
     * must also pass its checksum. All other entities are pattern-only either way.
     *
     * @param entity the catalog entity
     * @param token  the candidate text
     * @return {@code true} when pattern (and, where applicable, checksum) accepts {@code token}
     */
    public static boolean isStrictMatch(final PiiEntity entity,
                                        final String token) {
        if (!isNonStrictMatch(entity, token)) {
            return false;
        }
        return switch (entity) {
            case CREDIT_CARD -> ChecksumValidators.luhn(token);
            case IBAN_CODE -> ChecksumValidators.mod97(token);
            case IN_AADHAAR -> ChecksumValidators.verhoeff(token);
            default -> true;
        };
    }

    private static final class Holder {

        private static final Map<PiiEntity, Pattern> PATTERNS = createPatterns();
    }

    private static Map<PiiEntity, Pattern> createPatterns() {
        final var patterns = new EnumMap<PiiEntity, Pattern>(PiiEntity.class);
        patterns.put(PiiEntity.CREDIT_CARD, Pattern.compile(CREDIT_CARD));
        patterns.put(PiiEntity.CRYPTO, Pattern.compile(CRYPTO));
        patterns.put(PiiEntity.EMAIL_ADDRESS, Pattern.compile(EMAIL_ADDRESS));
        patterns.put(PiiEntity.IP_ADDRESS, Pattern.compile(IP_ADDRESS));
        patterns.put(PiiEntity.PHONE_NUMBER, Pattern.compile(PHONE_NUMBER));
        patterns.put(PiiEntity.IBAN_CODE, Pattern.compile(IBAN_CODE));
        patterns.put(PiiEntity.LOCATION, Pattern.compile(LOCATION));
        patterns.put(PiiEntity.DATE_TIME, Pattern.compile(DATE_TIME));
        patterns.put(PiiEntity.MEDICAL_LICENSE, Pattern.compile(MEDICAL_LICENSE));

        patterns.put(PiiEntity.US_BANK_NUMBER, Pattern.compile(US_BANK_NUMBER));
        patterns.put(PiiEntity.US_DRIVER_LICENSE, Pattern.compile(US_DRIVER_LICENSE));
        patterns.put(PiiEntity.US_ITIN, Pattern.compile(US_ITIN));
        patterns.put(PiiEntity.US_PASSPORT, Pattern.compile(US_PASSPORT));
        patterns.put(PiiEntity.US_SSN, Pattern.compile(US_SSN));

        patterns.put(PiiEntity.UK_NHS, Pattern.compile(UK_NHS));
        patterns.put(PiiEntity.UK_NINO, Pattern.compile(UK_NINO));

        patterns.put(PiiEntity.ES_NIF, Pattern.compile(ES_NIF));
        patterns.put(PiiEntity.ES_NIE, Pattern.compile(ES_NIE));

        patterns.put(PiiEntity.IT_FISCAL_CODE, Pattern.compile(IT_FISCAL_CODE));
        patterns.put(PiiEntity.IT_VAT_CODE, Pattern.compile(IT_VAT_CODE));

        patterns.put(PiiEntity.PL_PESEL, Pattern.compile(PL_PESEL));

        patterns.put(PiiEntity.SG_NRIC_FIN, Pattern.compile(SG_NRIC_FIN));
        patterns.put(PiiEntity.SG_UEN, Pattern.compile(SG_UEN));

        patterns.put(PiiEntity.AU_ABN, Pattern.compile(AU_ABN));
        patterns.put(PiiEntity.AU_ACN, Pattern.compile(AU_ACN));
        patterns.put(PiiEntity.AU_TFN, Pattern.compile(AU_TFN));
        patterns.put(PiiEntity.AU_MEDICARE, Pattern.compile(AU_MEDICARE));

        patterns.put(PiiEntity.IN_PAN, Pattern.compile(IN_PAN));
        patterns.put(PiiEntity.IN_AADHAAR, Pattern.compile(IN_AADHAAR));
        patterns.put(PiiEntity.IN_VEHICLE_REGISTRATION, Pattern.compile(IN_VEHICLE_REGISTRATION));
        patterns.put(PiiEntity.IN_VOTER, Pattern.compile(IN_VOTER));
        patterns.put(PiiEntity.IN_PASSPORT, Pattern.compile(IN_PASSPORT));

        patterns.put(PiiEntity.FI_PERSONAL_IDENTITY_CODE, Pattern.compile(FI_PERSONAL_IDENTITY_CODE));
        return Collections.unmodifiableMap(patterns);
    }
}
