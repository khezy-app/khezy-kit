package io.github.khezyapp.dpriv.api;

/**
 * SPI for deterministic (non-LLM) checks (design §5.2). Implementations run over a whole in-memory
 * {@code String} and produce a {@link GuardrailResult}.
 *
 * <p>Checks that also support the streaming engine override {@link #toStream()} to bridge to a
 * {@link StreamCheck}; checks that cannot stream keep the throwing default (design §10).
 */
@FunctionalInterface
public interface GuardrailCheck {

    /**
     * Runs this check against the given input.
     *
     * @param input the text to check
     * @return the immutable result of this check
     */
    GuardrailResult run(String input);

    /**
     * Returns the human-readable display name of this check; defaults to the simple class name.
     *
     * @return the check name
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Bridges this in-memory check to its streaming counterpart.
     *
     * @return the {@link StreamCheck} equivalent
     * @throws UnsupportedOperationException if this check does not support streaming
     */
    default StreamCheck toStream() {
        throw new UnsupportedOperationException(name() + " does not support streaming");
    }
}
