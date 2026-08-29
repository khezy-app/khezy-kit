package io.github.khezyapp.dpriv.policy;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Checksum validators behind the opt-in {@code strict} PII gate (design §8.2). Three entities are
 * checksum-validatable: {@code CREDIT_CARD} (Luhn mod-10), {@code IBAN_CODE} (ISO 7064 mod-97-10),
 * and {@code IN_AADHAAR} (Verhoeff). Every validator is fail-safe: malformed or scrambled input
 * returns {@code false} instead of throwing, so a candidate that fails its checksum is rejected,
 * never crashes a check.
 */
public final class ChecksumValidators {

    /** Smallest card length Luhn accepts. */
    private static final int MIN_CARD_LENGTH = 13;

    /** Largest card length Luhn accepts. */
    private static final int MAX_CARD_LENGTH = 19;

    /** Shortest ISO 13616 IBAN, per country BBAN bounds. */
    private static final int MIN_IBAN_LENGTH = 15;

    /** Longest ISO 13616 IBAN (34 chars). */
    private static final int MAX_IBAN_LENGTH = 34;

    /** Printed IBANs may carry a literal {@code IBAN} prefix before the country code. */
    private static final String IBAN_LITERAL_PREFIX = "IBAN";

    /** Length of the {@link #IBAN_LITERAL_PREFIX} literal prefix. */
    private static final int IBAN_LITERAL_PREFIX_LENGTH = 4;

    /** Country code + check digits rotated to the end of the mod-97 computation. */
    private static final int IBAN_CHECK_PREFIX_LENGTH = 4;

    /** ISO 13616 shape: country code, check digits, then an alphanumeric BBAN. */
    private static final Pattern IBAN_FORMAT = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}");

    /** ISO 7064 modulus. */
    private static final BigInteger MOD_97 = BigInteger.valueOf(97);

    /** Aadhaar numbers are exactly 12 digits; any other length was not issued by UIDAI. */
    private static final int AADHAAR_DIGIT_LENGTH = 12;

    /** Verhoeff dihedral (D5) table. */
    private static final int[][] D_TABLE = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    /** Verhoeff permutation table, indexed by digit position modulo its row count. */
    private static final int[][] P_TABLE = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    /** Verhoeff inverse table (check-digit generation). */
    private static final int[] INV_TABLE = {0, 4, 3, 2, 1, 5, 6, 7, 8, 9};

    private ChecksumValidators() {
    }

    /**
     * Luhn mod-10 check for card numbers. Accepts classic 13-19 digit card forms and tolerates
     * common separators (spaces, dashes) which are stripped before summing.
     *
     * @param digits the candidate card number (may contain separators)
     * @return {@code true} when the number is 13-19 digits and passes Luhn; {@code false} otherwise
     */
    public static boolean luhn(final String digits) {
        final var cleaned = stripNonDigits(digits);
        if (cleaned.length() < MIN_CARD_LENGTH || cleaned.length() > MAX_CARD_LENGTH) {
            return false;
        }
        var sum = 0;
        var doubleDigit = false;
        for (var i = cleaned.length() - 1; i >= 0; i--) {
            final var digit = cleaned.charAt(i) - '0';
            if (doubleDigit) {
                var doubled = digit * 2;
                if (doubled > 9) {
                    doubled -= 9;
                }
                sum += doubled;
            } else {
                sum += digit;
            }
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    /**
     * ISO 7064 mod-97-10 check for IBANs. Normalizes case and whitespace, optionally strips a
     * literal {@code IBAN} prefix, validates the ISO 13616 shape (country code + check digits +
     * BBAN, 15-34 chars), then confirms the rotated alphanumeric number is congruent to 1 mod 97.
     *
     * @param iban the candidate IBAN (spacing and case are ignored)
     * @return {@code true} when the full IBAN is well-formed and passes mod-97; {@code false}
     *     otherwise
     */
    public static boolean mod97(final String iban) {
        if (Objects.isNull(iban)) {
            return false;
        }
        final var compact = iban.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        final var normalized = compact.startsWith(IBAN_LITERAL_PREFIX)
                ? compact.substring(IBAN_LITERAL_PREFIX_LENGTH)
                : compact;
        if (normalized.length() < MIN_IBAN_LENGTH
                || normalized.length() > MAX_IBAN_LENGTH
                || !IBAN_FORMAT.matcher(normalized).matches()) {
            return false;
        }
        final var rotated = normalized.substring(IBAN_CHECK_PREFIX_LENGTH)
                + normalized.substring(0, IBAN_CHECK_PREFIX_LENGTH);
        final var digits = new StringBuilder(rotated.length() * 2);
        for (final var c : rotated.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                digits.append(10 + (c - 'A'));
            } else {
                digits.append(c);
            }
        }
        return new BigInteger(digits.toString()).mod(MOD_97).intValue() == 1;
    }

    /**
     * Verhoeff check for 12-digit Aadhaar numbers. Aadhaar is exactly 12 digits, so any other
     * length (including scrambled snippets) fails immediately; an altered digit changes the check
     * and fails.
     *
     * @param digits the candidate Aadhaar number (spaces are stripped)
     * @return {@code true} when exactly 12 digits pass Verhoeff; {@code false} otherwise
     */
    public static boolean verhoeff(final String digits) {
        final var cleaned = stripNonDigits(digits);
        if (cleaned.length() != AADHAAR_DIGIT_LENGTH) {
            return false;
        }
        var check = 0;
        for (var i = 0; i < cleaned.length(); i++) {
            final var digit = cleaned.charAt(cleaned.length() - 1 - i) - '0';
            final var permuted = P_TABLE[i % P_TABLE.length][digit];
            check = D_TABLE[check][permuted];
        }
        return check == 0;
    }

    private static String stripNonDigits(final String input) {
        if (Objects.isNull(input)) {
            return "";
        }
        final var sb = new StringBuilder(input.length());
        for (final var c : input.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
