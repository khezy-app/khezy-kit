package io.github.khezy.utils;

import java.util.*;

/**
 * Utility class providing common String-related helper methods.
 * <p>
 * This class is final and cannot be instantiated.
 * All methods are static and null-safe unless otherwise specified.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *     <li>Null-safe blank and empty checks</li>
 *     <li>Null-safe equality checks</li>
 *     <li>String reversing</li>
 *     <li>Whitespace stripping</li>
 *     <li>Prefix and suffix stripping (optionally repeated)</li>
 * </ul>
 *
 * <h2>Null Handling</h2>
 * <ul>
 *     <li>Most methods return {@code false} for null input when returning boolean.</li>
 *     <li>Some methods optionally return empty string instead of null.</li>
 *     <li>Strip methods return the original value if inputs are invalid.</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <pre>
 * StringUtil.isBlank("   ");              // true
 * StringUtil.isEmpty("");                 // true
 * StringUtil.equals("a", "a");            // true
 * StringUtil.reverse("abc");              // "cba"
 * StringUtil.strip("  text  ");           // "text"
 * StringUtil.stripLeft("foobar", "foo");  // "bar"
 * StringUtil.stripRight("foobar", "bar"); // "foo"
 * </pre>
 */
public final class StringUtil {

    private StringUtil() {
    }

    /**
     * Returns {@code true} if the given text is not null and contains only
     * whitespace characters, otherwise {@code false}.
     *
     * @param text the input text
     * @return {@code true} if text is blank, otherwise {@code false}
     */
    public static boolean isBlank(final String text) {
        return Optional.ofNullable(text)
                .map(String::isBlank)
                .orElse(false);
    }

    /**
     * Returns {@code true} if the given text is not blank.
     *
     * @param text the input text
     * @return {@code true} if text is not blank, otherwise {@code false}
     */
    public static boolean isNotBlank(final String text) {
        return !isBlank(text);
    }

    /**
     * Returns {@code true} if the given text is not null and has zero length.
     *
     * @param text the input text
     * @return {@code true} if text is empty, otherwise {@code false}
     */
    public static boolean isEmpty(final String text) {
        return Optional.ofNullable(text)
                .map(String::isEmpty)
                .orElse(false);
    }

    /**
     * Returns {@code true} if the given text is not empty.
     *
     * @param text the input text
     * @return {@code true} if text is not empty, otherwise {@code false}
     */
    public static boolean isNotEmpty(final String text) {
        return !isEmpty(text);
    }

    /**
     * Compares two strings for equality in a null-safe manner.
     *
     * @param text1 the first text
     * @param text2 the second text
     * @return {@code true} if both texts are equal or both are null
     */
    public static boolean equals(final String text1,
                                 final String text2) {
        return Objects.equals(text1, text2);
    }

    /**
     * Compares two strings for equality ignoring case differences.
     * Null values are treated as empty strings.
     *
     * @param text1 the first text
     * @param text2 the second text
     * @return {@code true} if texts are equal ignoring case
     */
    public static boolean equalsIgnoreCase(final String text1,
                                           final String text2) {
        return emptyIfNull(text1)
                .equalsIgnoreCase(emptyIfNull(text2));
    }

    /**
     * Reverses the given text.
     * If the input is null, an empty string is returned.
     *
     * @param text the input text
     * @return reversed text or empty string if null
     */
    public static String reverse(final String text) {
        final var textToRevers = emptyIfNull(text);
        final var sb = new StringBuilder();
        final var chars = textToRevers.toCharArray();
        for (var index = textToRevers.length() - 1; index >= 0; index--) {
            sb.append(chars[index]);
        }
        return sb.toString();
    }

    /**
     * Trims leading and trailing whitespace from the given text.
     * Returns empty string if input is null.
     *
     * @param text the input text
     * @return stripped text or empty string if null
     */
    public static String strip(final String text) {
        return strip(text, true);
    }

    /**
     * Trims leading and trailing whitespace from the given text.
     *
     * @param text              the input text
     * @param returnEmptyIfNull if true returns empty string when text is null,
     *                          otherwise returns null
     * @return stripped text, empty string, or null depending on configuration
     */
    public static String strip(final String text,
                               final boolean returnEmptyIfNull) {
        return isNull(text) ? returnEmptyIfNull(returnEmptyIfNull) : text.strip();
    }

    /**
     * Removes the specified prefix from the given text.
     * Repeats removal by default while the prefix matches.
     *
     * @param text      the input text
     * @param stripText the prefix to remove
     * @return text without the specified prefix
     */
    public static String stripLeft(final String text,
                                   final String stripText) {
        return stripLeft(text, stripText, true);
    }

    /**
     * Removes the specified prefix from the given text.
     *
     * @param text      the input text
     * @param stripText the prefix to remove
     * @param repeat    if true, repeatedly removes the prefix while it matches
     * @return text without the specified prefix
     */
    public static String stripLeft(final String text,
                                   final String stripText,
                                   final boolean repeat) {
        if (isNull(text) || isNull(stripText) || stripText.isEmpty()) {
            return text;
        }

        var finalText = text;
        do {
            if (finalText.startsWith(stripText)) {
                finalText = finalText.substring(stripText.length());
            }
        } while (repeat && finalText.startsWith(stripText));

        return finalText;
    }

    /**
     * Removes the specified suffix from the given text.
     * Repeats removal by default while the suffix matches.
     *
     * @param text      the input text
     * @param stripText the suffix to remove
     * @return text without the specified suffix
     */
    public static String stripRight(final String text,
                                    final String stripText) {
        return stripRight(text, stripText, true);
    }

    /**
     * Removes the specified suffix from the given text.
     *
     * @param text      the input text
     * @param stripText the suffix to remove
     * @param repeat    if true, repeatedly removes the suffix while it matches
     * @return text without the specified suffix
     */
    public static String stripRight(final String text,
                                    final String stripText,
                                    final boolean repeat) {
        if (isNull(text) || isNull(stripText) || stripText.isEmpty()) {
            return text;
        }

        var finalText = text;
        do {
            if (finalText.endsWith(stripText)) {
                finalText = finalText.substring(0, finalText.length() - stripText.length());
            }
        } while (repeat && finalText.endsWith(stripText));

        return finalText;
    }

    /**
     * Splits the given text around matches of the supplied regular expression pattern.
     * Empty trailing tokens may be discarded depending on the default behavior.
     *
     * @param text    the input text to split
     * @param pattern the regular expression delimiter
     * @return a list of split tokens, or empty list if text is null
     */
    public static List<String> split(final String text,
                                     final String pattern) {
        return split(text, pattern, 0, false);
    }

    /**
     * Splits the given text around matches of the supplied regular expression pattern,
     * preserving empty tokens including trailing empty strings.
     *
     * @param text    the input text to split
     * @param pattern the regular expression delimiter
     * @return a list of split tokens including empty values, or empty list if text is null
     */
    public static List<String> splitByPreservedEmpty(final String text,
                                                     final String pattern) {
        return split(text, pattern, 0, true);
    }

    /**
     * Splits the given text around matches of the supplied regular expression pattern.
     * <p>
     * The behavior can be configured to:
     * <ul>
     *     <li>Limit the number of resulting elements</li>
     *     <li>Preserve or discard empty tokens</li>
     * </ul>
     *
     * @param text           the input text to split
     * @param pattern        the regular expression delimiter
     * @param limit          the maximum number of elements in the result
     *                       (special handling when preserving empty values)
     * @param preservedEmpty whether empty tokens should be preserved
     * @return a list of split tokens based on the provided configuration,
     * or empty list if text is null
     */
    public static List<String> split(final String text,
                                     final String pattern,
                                     final int limit,
                                     final boolean preservedEmpty) {
        final var result = new ArrayList<String>();
        if (isNull(text)) {
            return result;
        }
        final var finalLimit = preservedEmpty ? -1 : Math.min(0, limit);
        final var splits = text.split(pattern, finalLimit);
        if (preservedEmpty && limit > 0) {
            final var actualLimit = Math.min(limit, splits.length);
            if (splits.length == actualLimit) {
                result.addAll(Arrays.asList(splits));
            } else {
                for (var index = 0; index < actualLimit; index++) {
                    if (index == actualLimit - 1) {
                        result.add(String.join(
                                pattern,
                                Arrays.asList(splits).subList(index, splits.length)
                        ));
                        break;
                    }
                    result.add(splits[index]);
                }
            }
        } else {
            result.addAll(Arrays.asList(splits));
        }
        return result;
    }

    /**
     * Splits the given text from the right around matches of the supplied regular expression pattern.
     * By default, empty trailing tokens are discarded.
     *
     * @param text the input text to split
     * @param pattern the regular expression delimiter
     * @return a list of tokens split from the right, or empty list if text is null
     */
    public static List<String> rsplit(final String text,
                                      final String pattern,
                                      final int limit) {
        return rsplit(text, pattern, limit, false);
    }

    /**
     * Splits the given text from the right around matches of the supplied regular expression pattern,
     * preserving empty tokens including trailing empty strings.
     *
     * @param text the input text to split
     * @param pattern the regular expression delimiter
     * @return a list of tokens split from the right including empty values, or empty list if text is null
     */
    public static List<String> rsplitByPreservedEmpty(final String text,
                                                      final String pattern,
                                                      final int limit) {
        return rsplit(text, pattern, limit, true);
    }

    /**
     * Splits the given text from the right around matches of the supplied regular expression pattern.
     * <p>
     * The behavior can be configured to:
     * <ul>
     *     <li>Limit the number of resulting elements</li>
     *     <li>Preserve or discard empty tokens</li>
     * </ul>
     *
     * @param text the input text to split
     * @param pattern the regular expression delimiter
     * @param limit the maximum number of elements in the result (special handling when preserving empty values)
     * @param preservedEmpty whether empty tokens should be preserved
     * @return a list of split tokens from the right based on the provided configuration,
     *         or empty list if text is null
     */
    public static List<String> rsplit(final String text,
                                      final String pattern,
                                      final int limit,
                                      final boolean preservedEmpty) {
        final var result = new ArrayList<String>();
        if (isNull(text)) {
            return result;
        }
        final var finalLimit = preservedEmpty ? -1 : Math.min(0, limit);
        final var splits = text.split(pattern, finalLimit);
        final var actualLimit = Math.min(limit, splits.length);
        if (preservedEmpty && limit > 0) {
            if (splits.length == actualLimit) {
                result.addAll(Arrays.asList(splits));
            } else {
                result.addAll(handleLimitRSpilt(splits, pattern, actualLimit));
            }
        } else {
            result.addAll(handleLimitRSpilt(splits, pattern, actualLimit));
        }
        return result;
    }

    private static List<String> handleLimitRSpilt(final String[] splits,
                                                  final String pattern,
                                                  final int actualLimit) {
        final var stack = new ArrayDeque<String>(actualLimit);
        var count = 0;
        for (var index = splits.length - 1; index >= 0; index--) {
            count++;
            if (count == actualLimit) {
                stack.push(String.join(
                        pattern,
                        Arrays.asList(splits).subList(0, index + 1)
                ));
                break;
            }
            stack.push(splits[index]);
        }
        return new ArrayList<>(stack);
    }

    private static String emptyIfNull(final String text) {
        return Optional.ofNullable(text).orElse("");
    }

    private static boolean isNull(final Object obj) {
        return Objects.isNull(obj);
    }

    private static String returnEmptyIfNull(final boolean returnEmptyIfNull) {
        return returnEmptyIfNull ? "" : null;
    }
}
