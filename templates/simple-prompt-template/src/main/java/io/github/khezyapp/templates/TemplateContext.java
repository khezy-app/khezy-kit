package io.github.khezyapp.templates;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable carrier for the raw input string and positional argument list
 * used during template resolution.
 */
public final class TemplateContext {

    private final String rawInput;
    private final List<String> positionalArgs;

    /**
     * Creates a new TemplateContext.
     *
     * @param rawInput       the joined raw input string (all args space-joined)
     * @param positionalArgs the individual positional argument values
     */
    public TemplateContext(final String rawInput,
                           final List<String> positionalArgs) {
        this.rawInput = Objects.requireNonNull(rawInput);
        this.positionalArgs = Collections.unmodifiableList(
                Objects.requireNonNull(positionalArgs)
        );
    }

    /**
     * Returns the joined raw input string (all arguments space-separated).
     *
     * @return raw input string
     */
    public String rawInput() {
        return rawInput;
    }

    /**
     * Returns the individual positional argument values.
     *
     * @return unmodifiable list of positional arguments
     */
    public List<String> positionalArgs() {
        return positionalArgs;
    }
}
