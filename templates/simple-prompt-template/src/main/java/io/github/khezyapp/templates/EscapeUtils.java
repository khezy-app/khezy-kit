package io.github.khezyapp.templates;

/**
 * Utility for escaping and un-escaping special template characters.
 * <p>
 * Before template resolution, {@code \$} and {@code \!} are replaced with
 * Unicode private-use-area codepoints so they survive resolver passes without
 * being interpreted as placeholders. After resolution the codepoints are
 * restored to their literal character equivalents.
 */
public final class EscapeUtils {

    private static final String ESCAPED_DOLLAR = "\uE000";
    private static final String ESCAPED_BANG = "\uE001";

    private EscapeUtils() {
    }

    /**
     * Replaces {@code \$} and {@code \!} with private-use Unicode codepoints.
     *
     * @param template the input template
     * @return the template with escaped sequences replaced
     */
    public static String escape(final String template) {
        var result = template;
        result = result.replace("\\$", ESCAPED_DOLLAR);
        result = result.replace("\\!", ESCAPED_BANG);
        return result;
    }

    /**
     * Restores private-use Unicode codepoints back to {@code $} and {@code !}.
     *
     * @param template the escaped template
     * @return the template with original characters restored
     */
    public static String unescape(final String template) {
        var result = template;
        result = result.replace(ESCAPED_DOLLAR, "$");
        result = result.replace(ESCAPED_BANG, "!");
        return result;
    }
}
