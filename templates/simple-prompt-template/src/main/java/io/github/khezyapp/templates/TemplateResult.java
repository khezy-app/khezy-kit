package io.github.khezyapp.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of a template resolution.
 * <p>
 * Contains the final resolved text, a list of shell commands that were
 * executed, and a list of errors that occurred during resolution.
 */
public final class TemplateResult {

    private final String resolvedText;
    private final List<String> executedCommands;
    private final List<String> errors;

    /**
     * Creates a new TemplateResult.
     *
     * @param resolvedText     the final resolved template text
     * @param executedCommands shell commands that were executed
     * @param errors           errors encountered during resolution
     */
    public TemplateResult(
            final String resolvedText,
            final List<String> executedCommands,
            final List<String> errors
    ) {
        this.resolvedText = Objects.requireNonNull(resolvedText);
        this.executedCommands = Collections.unmodifiableList(new ArrayList<>(executedCommands));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Returns the final resolved template text.
     *
     * @return resolved text
     */
    public String resolvedText() {
        return resolvedText;
    }

    /**
     * Returns an unmodifiable list of shell commands that were executed.
     *
     * @return executed commands
     */
    public List<String> executedCommands() {
        return executedCommands;
    }

    /**
     * Returns an unmodifiable list of errors encountered during resolution.
     *
     * @return errors
     */
    public List<String> errors() {
        return errors;
    }
}
