package io.github.khezyapp.dpriv.redact;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The {@code <ENTITY>} placeholder contract (design §7.1). A placeholder is exactly
 * {@code <ENTITY>} where {@code ENTITY} is the uppercased, sanitized policy rule name. This is the
 * single source of truth for the family-prefix mapping so that {@code PiiEntity} and every check
 * produces the same placeholder string. The format is fixed and not configurable.
 */
public final class Placeholders {

    /**
     * Matches an emitted placeholder such as {@code <EMAIL_ADDRESS>}; usable to skip protected
     * regions when a consumer re-scans already-redacted text.
     */
    public static final Pattern TOKEN = Pattern.compile("<[A-Z0-9_]+>");

    /**
     * Leading prefix stripped from PII rule names ("pii_") before uppercasing the rule name.
     */
    private static final String PII_FAMILY_PREFIX = "pii_";

    private Placeholders() {
    }

    /**
     * Maps a policy rule name / {@code entityType} string to its {@code <ENTITY>} placeholder.
     *
     * <p>Mapping rules (documented for reuse by {@code PiiEntity}, checks, and Task 09):
     * <ul>
     *   <li>A leading {@code pii_} family prefix is stripped and the remainder uppercased:
     *       {@code "pii_credit_card" → <CREDIT_CARD>}, {@code "pii_email_address" → <EMAIL_ADDRESS>}.</li>
     *   <li>Non-PII families (already prefix-free) uppercase as-is:
     *       {@code "secret" → <SECRET>}, {@code "link" → <LINK>}, {@code "keyword" → <KEYWORD>},
     *       {@code "jailbreak" → <JAILBREAK>}.</li>
     *   <li>Unknown types fall back to the uppercased name with anything that is not
     *       alphanumeric or {@code _} removed, e.g. {@code "custom-regex-name!" → <CUSTOMREGEXNAME>}.</li>
     * </ul>
     *
     * @param entityType the policy rule name / entityType string (e.g. {@code "pii_credit_card"})
     * @return the placeholder, exactly {@code <ENTITY>}
     */
    public static String forEntityType(final String entityType) {
        final var name = stripFamilyPrefix(entityType);
        final var sanitized = sanitize(name);
        return "<" + sanitized.toUpperCase(Locale.ROOT) + ">";
    }

    private static String stripFamilyPrefix(final String entityType) {
        if (entityType.startsWith(PII_FAMILY_PREFIX)) {
            return entityType.substring(PII_FAMILY_PREFIX.length());
        }
        return entityType;
    }

    private static String sanitize(final String name) {
        final var sb = new StringBuilder(name.length());
        for (var i = 0; i < name.length(); i++) {
            final var c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
