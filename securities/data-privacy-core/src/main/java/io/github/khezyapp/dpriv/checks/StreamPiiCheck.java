package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.PiiConfig;
import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.policy.PiiPatterns;
import io.github.khezyapp.dpriv.stream.MatchAccumulator;
import io.github.khezyapp.dpriv.stream.TextChunker;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Streaming variant of {@link PiiCheck} (design §10.3). Windows the input and runs the same
 * per-entity pattern scans (with the same strict checksum validation) over each window, records
 * the matches into a {@link MatchAccumulator}, and drains the per-entity groups in the same order
 * the in-memory {@link PiiCheck#run} reports them (catalog order, then custom rules in config
 * order). The shared {@link WindowedSpanScanner#scan} boundary filters make windowed detection
 * identical to whole-input detection for tokens no longer than the chunk overlap.
 */
final class StreamPiiCheck implements StreamCheck {

    private final PiiConfig config;

    /**
     * Creates the streaming check for the given policy.
     *
     * @param config the PII policy; never null
     */
    StreamPiiCheck(final PiiConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public void scan(final Reader input,
                     final MatchAccumulator sink) {
        final var entities = PiiCheck.resolveFor(config);
        if (entities.isEmpty()) {
            return;
        }
        final var spans = WindowedSpanScanner.scan(new TextChunker(input), this::detect);
        if (spans.isEmpty()) {
            return;
        }
        for (final var entity : entities) {
            drain(sink, entity.type(), spans);
        }
        for (final var custom : config.customRegexes()) {
            if (Objects.isNull(custom) || custom.name().isBlank()) {
                continue;
            }
            drain(sink, custom.name(), spans);
        }
    }

    @Override
    public String name() {
        return "StreamPiiCheck";
    }

    private List<TokenSpan> detect(final WindowMeta meta) {
        final var result = new ArrayList<TokenSpan>();
        for (final var entity : PiiCheck.resolveFor(config)) {
            final var matcher = PiiPatterns.forEntity(entity).matcher(meta.text());
            while (matcher.find()) {
                final var token = matcher.group();
                final var accepted = config.strict()
                        ? PiiPatterns.isStrictMatch(entity, token)
                        : PiiPatterns.isNonStrictMatch(entity, token);
                if (accepted) {
                    result.add(
                            new TokenSpan(
                                    matcher.start(),
                                    matcher.end(),
                                    token,
                                    entity.type(),
                                    0,
                                    meta.window()
                            )
                    );
                }
            }
        }
        for (final var custom : config.customRegexes()) {
            if (Objects.isNull(custom) || custom.name().isBlank()) {
                continue;
            }
            for (final var pattern : custom.patterns()) {
                if (Objects.isNull(pattern)) {
                    continue;
                }
                final var matcher = pattern.matcher(meta.text());
                while (matcher.find()) {
                    result.add(
                            new TokenSpan(
                                    matcher.start(),
                                    matcher.end(),
                                    matcher.group(),
                                    custom.name(),
                                    0,
                                    meta.window()
                            )
                    );
                }
            }
        }
        return result;
    }

    private static void drain(final MatchAccumulator sink,
                              final String entityType,
                              final List<TokenSpan> spans) {
        final var tokens = spans.stream()
                .filter(span -> span.entityType().equals(entityType))
                .map(TokenSpan::token)
                .toList();
        if (!tokens.isEmpty()) {
            sink.addAll(entityType, tokens);
        }
    }
}