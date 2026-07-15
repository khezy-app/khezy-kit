package io.github.khezyapp.fsm.core.builder;

import java.util.List;

/**
 * Exception thrown at build time when a state machine definition fails validation.
 * <p>
 * The builder collects <strong>all</strong> validation violations before throwing,
 * so callers receive a complete picture of what is wrong with their machine definition
 * rather than failing on the first error. Each violation message follows a structured
 * format: {@code ErrorCode: description}.
 * <p>
 * This is an unchecked exception extending {@link IllegalStateException} because an
 * invalid machine definition represents a programming error that should not be
 * caught at runtime.
 *
 * @see StateMachineBuilder#build()
 */
public class StateMachineBuilderException extends IllegalStateException {
    private final List<String> violations;

    /**
     * Constructs an exception with the full list of validation violations.
     *
     * @param violations the list of human-readable violation messages
     */
    public StateMachineBuilderException(final List<String> violations) {
        super("Invalid state machine definition: " + violations);
        this.violations = List.copyOf(violations);
    }

    /**
     * Returns the list of validation violations that caused the build to fail.
     *
     * @return an immutable list of structured error messages
     */
    public List<String> getViolations() {
        return violations;
    }
}
