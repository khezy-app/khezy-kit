package io.github.khezyapp.fsm.core.api;

/**
 * Exception thrown when a state machine transition fails at runtime.
 * <p>
 * Unlike the build-time {@link io.github.khezyapp.fsm.core.builder.StateMachineBuilderException
 * StateMachineBuilderException}, this exception signals a <strong>runtime</strong>
 * failure — typically an action threw an exception during execution. The exception
 * carries the source state, event type, and target state involved in the failed
 * transition so that callers can reconstruct what went wrong without inspecting
 * the machine's internals.
 * <p>
 * This is an unchecked exception ({@link RuntimeException}) because transition
 * failures are typically unrecoverable at the machine level and should propagate
 * to the caller's error-handling layer.
 */
public class TransitionExecutionException extends RuntimeException {
    private final Object sourceState;
    private final Object eventType;
    private final Object targetState;

    /**
     * Creates a simple exception with no transition context.
     * Useful for synthetic errors like "no matching transition found".
     *
     * @param message the detail message
     * @param cause   the root cause, or {@code null} if the error is synthetic
     */
    public TransitionExecutionException(final String message,
                                        final Throwable cause) {
        this(message, cause, null, null, null);
    }

    /**
     * Creates a fully detailed exception carrying the transition's context.
     *
     * @param message     the detail message
     * @param cause       the root cause, or {@code null}
     * @param sourceState the state the machine was in when the error occurred
     * @param eventType   the event type that triggered the failed transition
     * @param targetState the intended target state (may be null if not yet known)
     */
    public TransitionExecutionException(final String message,
                                          final Throwable cause,
                                          final Object sourceState,
                                          final Object eventType,
                                          final Object targetState) {
        super(message, cause);
        this.sourceState = sourceState;
        this.eventType = eventType;
        this.targetState = targetState;
    }

    /**
     * Returns the source state at the time of the failure.
     *
     * @return the source state identifier, or {@code null} if not set
     */
    public Object getSourceState() {
        return sourceState;
    }

    /**
     * Returns the event type that triggered the failed transition.
     *
     * @return the event type, or {@code null} if not set
     */
    public Object getEventType() {
        return eventType;
    }

    /**
     * Returns the intended target state of the failed transition.
     *
     * @return the target state identifier, or {@code null} if not set
     */
    public Object getTargetState() {
        return targetState;
    }
}
