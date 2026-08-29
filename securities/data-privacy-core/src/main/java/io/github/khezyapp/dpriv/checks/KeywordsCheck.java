package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.api.KeywordsConfig;
import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.redact.Redactor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Keyword content-filter check (design §9.4). Whole-word, case-insensitive matching against the
 * configured keyword list using {@code \p{L} | \p{N} | _} boundaries (not the ASCII-only
 * {@code \b}), so punctuation-adjacent keywords still match and embedded substrings do not. A
 * keyword that starts or ends with a non-word character drops that side's boundary, mirroring the
 * reference behavior. Configured keywords are stripped of trailing punctuation
 * ({@code "urgent!"}  matches {@code "urgent"}).
 *
 * <p>When {@code config.toMask()} is true the matched keywords are redacted to {@code <KEYWORD>};
 * otherwise the text passes through unchanged and only {@code detected} is set. The reported
 * {@code maskEntities["keyword"]} always holds the unique case-preserved (case-folded first-seen)
 * matches. Detection is whole-word, but masking follows the shared Redactor contract: every
 * occurrence of a matched keyword value is replaced, including inside a longer word. Matching is
 * deterministic and reusable by Task 09's {@code StreamKeywordsCheck} because it is a single
 * compiled pattern over immutable config.
 */
public final class KeywordsCheck implements GuardrailCheck {

    /**
     * Unicode-aware word character: any letter, number, or underscore. Used instead of {@code \b},
     * which is ASCII-only.
     */
    private static final String WORD_CHAR_CLASS = "[\\p{L}\\p{N}_]";

    private static final String LEFT_WORD_BOUNDARY = "(?<!" + WORD_CHAR_CLASS + ")";
    private static final String RIGHT_WORD_BOUNDARY = "(?!" + WORD_CHAR_CLASS + ")";

    /**
     * Trailing sentence punctuation stripped from configured keywords (reference behavior).
     */
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.,!?;:]+$");

    private final KeywordsConfig config;
    private final Redactor redactor;
    private final Pattern pattern;

    /**
     * Creates the check for the given policy and redactor. The combined matcher is compiled once at
     * construction; an empty keyword list yields a no-op check.
     *
     * @param config   the keyword policy; never null
     * @param redactor the redactor used to compute {@code cleanedValue}; never null
     */
    public KeywordsCheck(final KeywordsConfig config,
                         final Redactor redactor) {
        this.config = Objects.requireNonNull(config, "config");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        this.pattern = compilePattern(config.keywords());
    }

    @Override
    public GuardrailResult run(final String input) {
        Objects.requireNonNull(input, "input");
        final var found = scan(input);
        if (found.isEmpty()) {
            return GuardrailResult.pass("keyword", input);
        }
        final var maskEntities = Map.of("keyword", List.copyOf(found));
        final var cleanedValue = config.toMask() ? redactor.redact(input, maskEntities) : input;
        return GuardrailResult.fail("keyword", cleanedValue, maskEntities);
    }

    @Override
    public String name() {
        return "KeywordsCheck";
    }

    @Override
    public StreamCheck toStream() {
        return new StreamKeywordsCheck(pattern);
    }

    private List<String> scan(final String input) {
        if (Objects.isNull(pattern)) {
            return List.of();
        }
        final var seen = new HashSet<String>();
        final var found = new ArrayList<String>();
        final var matcher = pattern.matcher(input);
        while (matcher.find()) {
            final var matched = matcher.group();
            if (seen.add(matched.toLowerCase(Locale.ROOT))) {
                found.add(matched);
            }
        }
        return found;
    }

    private static Pattern compilePattern(final List<String> keywords) {
        final var alternatives = new ArrayList<String>();
        for (final var keyword : keywords) {
            if (Objects.isNull(keyword)) {
                continue;
            }
            final var sanitized = TRAILING_PUNCTUATION.matcher(keyword).replaceAll("");
            if (sanitized.isEmpty()) {
                continue;
            }
            final var left = isWordCodePoint(sanitized.codePointAt(0)) ? LEFT_WORD_BOUNDARY : "";
            final var right = isWordCodePoint(sanitized.codePointBefore(sanitized.length()))
                    ? RIGHT_WORD_BOUNDARY
                    : "";
            alternatives.add(left + Pattern.quote(sanitized) + right);
        }
        if (alternatives.isEmpty()) {
            return null;
        }
        return Pattern.compile("(?:" + String.join("|", alternatives) + ")",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static boolean isWordCodePoint(final int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '_';
    }
}
