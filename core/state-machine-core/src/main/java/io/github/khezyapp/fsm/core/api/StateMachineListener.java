package io.github.khezyapp.fsm.core.api;

import io.github.khezyapp.fsm.core.model.Event;

/**
 * A read-only observer that receives lifecycle notifications from a state machine.
 * <p>
 * Every method in this interface has a <strong>default no-op implementation</strong>,
 * so consumers only need to override the callbacks they care about. Listeners
 * <strong>cannot affect</strong> the machine's behaviour — they observe but do not
 * control transitions.
 * <p>
 * Use cases include logging, metrics collection, audit trails, and UI updates.
 * For hooks that <em>can</em> influence the machine (e.g. veto a transition), see
 * {@link StateMachineInterceptor}.
 *
 * @param <S> the state identifier type
 * @param <E> the event type discriminator
 */
public interface StateMachineListener<S, E> {

    /**
     * Called just before a transition's side effects execute (after the guard passes
     * but before exit/transition/entry actions run).
     *
     * @param sourceState the state the machine is leaving
     * @param targetState the state the machine is entering
     * @param event       the event that triggered the transition
     */
    default void onTransitionStart(final S sourceState,
                                    final S targetState,
                                    final Event<E, ?> event) { }

    /**
     * Called after a transition completes successfully — after all actions have been
     * executed and the current state has been updated.
     *
     * @param sourceState the state the machine was in before the transition
     * @param targetState the state the machine is now in
     * @param event       the event that triggered the transition
     */
    default void onTransitionComplete(final S sourceState,
                                       final S targetState,
                                       final Event<E, ?> event) { }

    /**
     * Called when the machine's current state changes to a new value.
     * This fires after the state update but before {@code onTransitionComplete}.
     *
     * @param oldState the previous state
     * @param newState the new current state
     */
    default void onStateChanged(final S oldState,
                                 final S newState) { }

    /**
     * Called when an error occurs during transition processing. This includes:
     * <ul>
     *   <li>No matching transition found for the current state + event type</li>
     *   <li>An interceptor vetoed the transition</li>
     *   <li>An action threw an exception</li>
     * </ul>
     *
     * @param currentState the state the machine was in when the error occurred
     * @param event        the event being processed when the error occurred
     * @param exception    the exception that was thrown (or a synthetic exception
     *                     describing the problem)
     */
    default void onError(final S currentState,
                          final Event<E, ?> event,
                          final Exception exception) { }
}
