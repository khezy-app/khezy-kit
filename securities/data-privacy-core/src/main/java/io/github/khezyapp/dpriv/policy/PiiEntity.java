package io.github.khezyapp.dpriv.policy;

import java.util.Locale;

/**
 * The canonical 33-entity PII catalog (design §8). Constant names and the {@link #type()} contract
 * string family ({@code pii_}-prefixed) are pinned; pattern/checksum logic lives in Task 04.
 */
public enum PiiEntity {

    CREDIT_CARD,
    CRYPTO,
    EMAIL_ADDRESS,
    IP_ADDRESS,
    PHONE_NUMBER,
    IBAN_CODE,
    LOCATION,
    DATE_TIME,
    MEDICAL_LICENSE,

    US_BANK_NUMBER,
    US_DRIVER_LICENSE,
    US_ITIN,
    US_PASSPORT,
    US_SSN,

    UK_NHS,
    UK_NINO,

    ES_NIF,
    ES_NIE,

    IT_FISCAL_CODE,
    IT_VAT_CODE,

    PL_PESEL,

    SG_NRIC_FIN,
    SG_UEN,

    AU_ABN,
    AU_ACN,
    AU_TFN,
    AU_MEDICARE,

    IN_PAN,
    IN_AADHAAR,
    IN_VEHICLE_REGISTRATION,
    IN_VOTER,
    IN_PASSPORT,

    FI_PERSONAL_IDENTITY_CODE;

    private final String typeString;

    PiiEntity() {
        this.typeString = "pii_" + name().toLowerCase(Locale.ROOT);
    }

    /**
     * The {@code pii_}-prefixed contract string (e.g. {@code EMAIL_ADDRESS.type() == "pii_email"}).
     *
     * @return the type string
     */
    public String type() {
        return typeString;
    }
}
