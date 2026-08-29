package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.stream.MatchAccumulator;
import io.github.khezyapp.dpriv.stream.TextChunker;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Streaming variant of {@link UrlsCheck} (design §10.3). Windows the input, runs the shared
 * {@link UrlsCheck#detectSpans} sweep over each window, merges the raw spans, reorders them into
 * the pattern-major order the in-memory sweep produces over the full text, and then applies the
 * shared {@link UrlsCheck#finalizeCandidates} and {@link UrlsCheck#isFlagged} steps exactly once —
 * keeping streaming output identical to in-memory output for any input.
 */
final class StreamUrlsCheck implements StreamCheck {

    private final Set<String> allowedSchemes;
    private final Set<String> allowedHosts;

    /**
     * Creates the streaming check for the given normalized allow-lists.
     *
     * @param allowedSchemes the normalized (lowercase) allowed-scheme set; never null
     * @param allowedHosts   the normalized (lowercase) host allow-list; empty disables the host rule
     */
    StreamUrlsCheck(final Set<String> allowedSchemes,
                    final Set<String> allowedHosts) {
        this.allowedSchemes = Objects.requireNonNull(allowedSchemes, "allowedSchemes");
        this.allowedHosts = Objects.requireNonNull(allowedHosts, "allowedHosts");
    }

    @Override
    public void scan(final Reader input,
                     final MatchAccumulator sink) {
        final var spans = WindowedSpanScanner.scan(
                new TextChunker(input),
                meta -> UrlsCheck.detectSpans(meta.text())
                        .stream()
                        .map(span -> new TokenSpan(
                                span.start(),
                                span.end(),
                                span.candidate(),
                                "link",
                                span.patternIndex(),
                                meta.window())
                        )
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        if (spans.isEmpty()) {
            return;
        }
        final var ordered = new ArrayList<>(spans);
        ordered.sort(
                Comparator.comparingInt(TokenSpan::patternIndex)
                        .thenComparingInt(TokenSpan::window)
                        .thenComparingInt(TokenSpan::start)
        );
        final var candidates = ordered.stream()
                .map(TokenSpan::token)
                .toList();
        final var finalized = UrlsCheck.finalizeCandidates(candidates);
        final var blocked = new LinkedHashSet<String>();
        for (final var candidate : finalized) {
            if (UrlsCheck.isFlagged(candidate, allowedSchemes, allowedHosts)) {
                blocked.add(candidate);
            }
        }
        if (blocked.isEmpty()) {
            return;
        }
        sink.addAll("link", List.copyOf(blocked));
    }

    @Override
    public String name() {
        return "StreamUrlsCheck";
    }
}