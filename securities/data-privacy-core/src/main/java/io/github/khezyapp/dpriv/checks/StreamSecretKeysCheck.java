package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.SecretConfig;
import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.internal.SecretCandidateFilter;
import io.github.khezyapp.dpriv.stream.MatchAccumulator;
import io.github.khezyapp.dpriv.stream.TextChunker;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Streaming variant of {@link SecretKeysCheck} (design §10.3). Tokenizes each window with the
 * shared {@link SecretKeysCheck#tokenPattern} and applies the shared {@link SecretCandidateFilter}
 * against a one-character context extension so the strict-mode adjacency rule reads the real
 * predecessor across the window boundary. Custom patterns are scanned per window in config order;
 * the two families (built-in filter-validated tokens, then custom patterns in ordinal order) are
 * drained into the {@code "secret"} bucket in the same first-seen order the in-memory check
 * produces, after reordering the built-in tokens by absolute position.
 */
final class StreamSecretKeysCheck implements StreamCheck {

    private final SecretConfig config;
    private final SecretCandidateFilter filter;

    /**
     * Creates the streaming check for the given policy.
     *
     * @param config the secret scanning policy; never null
     */
    StreamSecretKeysCheck(final SecretConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.filter = new SecretCandidateFilter(config.preset().params());
    }

    @Override
    public void scan(final Reader input,
                     final MatchAccumulator sink) {
        final var spans = WindowedSpanScanner.scan(new TextChunker(input), this::detect);
        if (spans.isEmpty()) {
            return;
        }
        final var drain = new LinkedHashSet<String>();
        final var builtIn = spans.stream()
                .filter(span -> span.patternIndex() == 0)
                .sorted(Comparator.comparingInt(TokenSpan::window)
                        .thenComparingInt(TokenSpan::start))
                .map(TokenSpan::token)
                .toList();
        drain.addAll(builtIn);
        for (var ordinal = 1; ordinal <= maxOrdinal(spans); ordinal++) {
            final var group = ordinal;
            final var custom = spans.stream()
                    .filter(span -> span.patternIndex() == group)
                    .sorted(Comparator.comparingInt(TokenSpan::window)
                            .thenComparingInt(TokenSpan::start))
                    .map(TokenSpan::token)
                    .toList();
            drain.addAll(custom);
        }
        sink.addAll("secret", List.copyOf(drain));
    }

    @Override
    public String name() {
        return "StreamSecretKeysCheck";
    }

    private List<TokenSpan> detect(final WindowMeta meta) {
        final var result = new ArrayList<TokenSpan>();
        final var text = meta.text();
        final var ctx = meta.first() ? text : meta.prevChar() + text;
        final var shift = meta.first() ? 0 : 1;
        final var matcher = SecretKeysCheck.tokenPattern().matcher(text);
        while (matcher.find()) {
            final var start = matcher.start();
            final var end = matcher.end();
            if (filter.accept(ctx, shift + start, shift + end)) {
                result.add(
                        new TokenSpan(
                                start,
                                end,
                                text.substring(start, end),
                                "secret",
                                0,
                                meta.window()
                        )
                );
            }
        }
        var ordinal = 1;
        for (final var patterns : config.customPatterns().values()) {
            for (final var pattern : patterns) {
                if (Objects.isNull(pattern)) {
                    continue;
                }
                final var customMatcher = pattern.matcher(text);
                while (customMatcher.find()) {
                    result.add(
                            new TokenSpan(
                                    customMatcher.start(),
                                    customMatcher.end(),
                                    customMatcher.group(),
                                    "secret",
                                    ordinal,
                                    meta.window()
                            ));
                }
                ordinal++;
            }
        }
        return result;
    }

    private static int maxOrdinal(final List<TokenSpan> spans) {
        var max = 0;
        for (final var span : spans) {
            if (span.patternIndex() > max) {
                max = span.patternIndex();
            }
        }
        return max;
    }
}