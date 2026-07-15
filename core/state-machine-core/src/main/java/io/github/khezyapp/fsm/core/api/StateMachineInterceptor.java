package io.github.khezyapp.fsm.core.api;

import io.github.khezyapp.fsm.core.model.Event;

/**
 * A pre- and post-transition hook that can inspect, enrich, or <strong>veto</strong>
 * state machine transitions.
 * <p>
 Unlike the read-only {@link StateMachineListener}, an interceptor has the power to
 * <strong>block</strong> a transition by returning {@code false} from
 * {@link #preTransition preTransition()}. This makes interceptors suitable for
 * cross-cutting concerns such as:
 * <ul>
 *   <li>Authorization and access control</li>
 *   <li>Transaction management (open before, commit/rollback after)</li>
 *   <li>Enriching message headers with tracing metadata</li>
 *   <li>Rate limiting or circuit-breaking logic</li>
 * </ul>
 * <p>
 * Both methods have default implementations so consumers only override what they need.
 * Interceptors execute <strong>before</strong> listeners in the transition pipeline.
 *
 * @param <S> the state identifier type
 * @param <E> the event type discriminator
 * @param <C> the context type
 */
public interface StateMachineInterceptor<S, E, C> {

    /**
     * Called before the transition's side effects execute.
     * <p>
     * Return {@code true} to allow the transition to proceed, or {@code false} to veto
     * (block) it. If the transition is vetoed, no actions run, the current state does
     * not change, and an error notification is sent to all registered listeners.
     *
     * @param sourceState the current state (before transition)
     * @param targetState the proposed target state
     * @param event       the event that triggered the transition
     * @param context     the shared machine context (readable and writable)
     * @return {@code true} to allow the transition, {@code false} to veto it
     */
    default boolean preTransition(final S sourceState,
                                   final S targetState,
                                   final Event<E, ?> event,
                                   final C context) {
        return true;
    }

    /**
     * Called after a transition has completed successfully — after all actions have
     * executed and the current state has been updated.
     * <p>
     * This is a notification-only hook; the return value is ignored and the transition
     * cannot be undone at this point.
     *
     * @param sourceState the state the machine was in before the transition
     * @param targetState the state the machine is now in
     * @param event       the event that triggered the transition
     * @param context     the shared machine context
     */
    default void postTransition(final S sourceState,
                                 final S targetState,
                                 final Event<E, ?> event,
                                 final C context) {
    }
}
