package io.github.khezyapp.fsm.core.api;

/**
 * A functional interface that represents a Boolean predicate used to conditionally
 * allow or block a state machine transition.
 * <p>
 * Guards are evaluated <strong>before</strong> any side effects run. If the guard
 * returns {@code false} (or throws an exception), the transition is denied: no exit
 * actions, transition actions, or entry actions execute; the machine stays in its
 * current state.
 * <p>
 * A {@code null} guard on a {@link io.github.khezyapp.fsm.core.model.Transition}
 * is treated as "always allowed" — the transition proceeds unconditionally.
 * <p>
 * Usage:
 * <pre>{@code
 * Guard<KycContext> nameRequired = ctx -> ctx.name() != null && !ctx.name().isBlank();
 * }</pre>
 *
 * @param <C> the type of the shared context object used for evaluation
 */
@FunctionalInterface
public interface Guard<C> {

    /**
     * Evaluates whether the transition should be allowed to proceed.
     *
     * @param context the shared machine context (readable only; the guard should
     *                not mutate the context)
     * @return {@code true} if the transition is allowed, {@code false} to block it
     * @throws Exception if evaluation fails; the exception is caught by the machine
     *                   and treated as a denial (the transition is blocked)
     */
    boolean evaluate(C context) throws Exception;
}
