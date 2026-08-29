package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.stream.MatchAccumulator;
import io.github.khezyapp.dpriv.stream.TextChunker;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Streaming variant of {@link KeywordsCheck} (design §10.3). Windows the input and runs the same
 * compiled whole-word pattern over each window, then applies the case-folded first-seen dedupe
 * over the surviving spans so the reported {@code "keyword"} list matches the in-memory check
 * exactly. A configured keyword longer than the chunk overlap can be truncated by the shared
 * boundary filters (out of scope; see the chunker contract).
 */
final class StreamKeywordsCheck implements StreamCheck {

    private final Pattern pattern;

    /**
     * Creates the streaming check for the given compiled keyword pattern. A {@code null} pattern
     * (no configured keywords) is a no-op, mirroring {@link KeywordsCheck}.
     *
     * @param pattern the compiled keyword matcher; may be null
     */
    StreamKeywordsCheck(final Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public void scan(final Reader input,
                     final MatchAccumulator sink) {
        if (Objects.isNull(pattern)) {
            return;
        }
        final var spans = WindowedSpanScanner.scan(new TextChunker(input), meta -> {
            final var result = new ArrayList<TokenSpan>();
            final var matcher = pattern.matcher(meta.text());
            while (matcher.find()) {
                final var token = matcher.group();
                if (!token.isEmpty()) {
                    result.add(
                            new TokenSpan(
                                    matcher.start(),
                                    matcher.end(),
                                    token,
                            "keyword",
                                    0,
                                    meta.window()
                            )
                    );
                }
            }
            return result;
        });
        if (spans.isEmpty()) {
            return;
        }
        final var seen = new HashSet<String>();
        final var drain = new LinkedHashSet<String>();
        for (final var span : spans) {
            if (seen.add(span.token().toLowerCase(Locale.ROOT))) {
                drain.add(span.token());
            }
        }
        sink.addAll("keyword", List.copyOf(drain));
    }

    @Override
    public String name() {
        return "StreamKeywordsCheck";
    }
}