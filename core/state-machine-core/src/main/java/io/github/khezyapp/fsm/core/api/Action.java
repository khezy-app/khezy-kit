package io.github.khezyapp.fsm.core.api;

/**
 * A functional interface that represents a side effect executed during a state machine
 * transition or state lifecycle hook.
 * <p>
 * Actions are the primary mechanism for producing side effects in the FSM — they run
 * at three points in the transition pipeline:
 * <ul>
 *   <li><strong>Exit actions</strong> — when leaving the current state</li>
 *   <li><strong>Transition actions</strong> — during the transition itself</li>
 *   <li><strong>Entry actions</strong> — when entering the target state</li>
 * </ul>
 * <p>
 * Each action receives the shared context object {@code C} and can freely read from or
 * mutate it. If an action throws a checked exception the transition is aborted and the
 * exception propagates to the caller wrapped in a
 * {@link TransitionExecutionException}.
 * <p>
 * Usage:
 * <pre>{@code
 * Action<KycContext> logAction = ctx -> auditLog.log("KYC processed: " + ctx.name());
 * }</pre>
 *
 * @param <C> the type of the shared context object passed to this action
 */
@FunctionalInterface
public interface Action<C> {

    /**
     * Executes the side effect against the given context.
     *
     * @param context the shared machine context (readable and writable)
     * @throws Exception if the action fails; the exception is caught by the machine
     *                   and propagated as a {@link TransitionExecutionException}
     */
    void execute(C context) throws Exception;
}
