package io.github.khezyapp.fsm.core.api;

import io.github.khezyapp.fsm.core.model.Event;
import io.github.khezyapp.fsm.core.model.State;
import io.github.khezyapp.fsm.core.model.Transition;

import java.util.Set;

/**
 * The primary contract for a finite state machine instance.
 * <p>
 * A {@code StateMachine} is a generic, type-safe engine that processes events against
 * a predefined set of states and transition rules. It encapsulates the full lifecycle
 * of a deterministic FSM:
 * <ol>
 *   <li>Receive an event via {@link #fire(Event, Object)}</li>
 *   <li>Look up the matching transition (O(1) via index)</li>
 *   <li>Evaluate guards, run interceptors, execute actions</li>
 *   <li>Update the current state</li>
 *   <li>Notify registered listeners</li>
 * </ol>
 * <p>
 * Concrete implementations guarantee that {@code fire()} is thread-safe — only one
 * event is processed at a time per machine instance.
 *
 * @param <S> the type used to identify states
 * @param <E> the type used to discriminate events
 * @param <C> the type of the shared context object
 */
public interface StateMachine<S, E, C> {

    /**
     * Fires an event against the machine's current state.
     * <p>
     * This is the main entry point for driving the state machine forward. The method:
     * <ol>
     *   <li>Checks if the machine is in a final state (if so, ignores the event)</li>
     *   <li>Looks up the transition for the current state + event type</li>
     *   <li>Runs interceptor pre-hooks (interceptors may veto the transition)</li>
     *   <li>Evaluates the guard (if present)</li>
     *   <li>Executes exit actions, transition actions, and entry actions</li>
     *   <li>Updates the current state</li>
     *   <li>Notifies listeners and interceptor post-hooks</li>
     * </ol>
     *
     * @param event   the event to process (its {@code type} is used for transition lookup)
     * @param context the shared context passed to all actions, guards, and interceptors
     * @return the state after processing (same as previous state if the event was ignored or blocked)
     */
    State<S, C> fire(Event<E, ?> event, C context);

    /**
     * Returns the machine's current state.
     *
     * @return the current state at this moment
     */
    State<S, C> getCurrentState();

    /**
     * Returns the machine's initial (start) state as configured at build time.
     *
     * @return the initial state, never changes during the machine's lifetime
     */
    State<S, C> getInitialState();

    /**
     * Returns an immutable snapshot of all states registered in this machine.
     *
     * @return a set of all defined states
     */
    Set<State<S, C>> getStates();

    /**
     * Returns an immutable snapshot of all transitions registered in this machine.
     *
     * @return a set of all defined transitions
     */
    Set<Transition<S, E, C>> getTransitions();

    /**
     * Returns {@code true} if the machine is currently in a final (terminal) state.
     * When a machine is in a final state, all incoming events are silently ignored.
     *
     * @return {@code true} if the current state is a final state
     */
    boolean isFinal();

    /**
     * Registers a read-only listener that receives lifecycle notifications.
     * Listeners observe but cannot influence the machine's behaviour.
     *
     * @param listener the listener to add
     */
    void addListener(StateMachineListener<S, E> listener);

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    void removeListener(StateMachineListener<S, E> listener);

    /**
     * Registers an interceptor that can inspect and optionally veto transitions.
     * Interceptors run before listeners and can block a transition by returning
     * {@code false} from {@link StateMachineInterceptor#preTransition preTransition()}.
     *
     * @param interceptor the interceptor to add
     */
    void addInterceptor(StateMachineInterceptor<S, E, C> interceptor);

    /**
     * Removes a previously registered interceptor.
     *
     * @param interceptor the interceptor to remove
     */
    void removeInterceptor(StateMachineInterceptor<S, E, C> interceptor);
}
