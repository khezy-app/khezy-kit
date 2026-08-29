package io.github.khezyapp.dpriv.api;

import io.github.khezyapp.dpriv.stream.MatchAccumulator;

import java.io.Reader;

/**
 * SPI for streaming checks (design §5.2, §10). Implementations consume a chunked {@link Reader}
 * without ever seeing the whole input and push matches into {@link MatchAccumulator}.
 */
@FunctionalInterface
public interface StreamCheck {

    /**
     * Scans the given input stream and records matches into the sink.
     *
     * @param input the chunked input to scan
     * @param sink  the accumulator that collects {@code (entityType, token)} matches
     */
    void scan(Reader input, MatchAccumulator sink);

    /**
     * Returns the human-readable display name of this check.
     *
     * @return the check name
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
