package io.github.khezyapp.fsm.core.impl;

import io.github.khezyapp.fsm.core.api.Action;
import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.api.TransitionExecutionException;
import io.github.khezyapp.fsm.core.api.StateMachineInterceptor;
import io.github.khezyapp.fsm.core.api.StateMachineListener;
import io.github.khezyapp.fsm.core.model.Event;
import io.github.khezyapp.fsm.core.model.State;
import io.github.khezyapp.fsm.core.model.Transition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The default, thread-safe runtime implementation of a {@link StateMachine}.
 * <p>
 * This class implements the full 11-step transition algorithm as specified in
 * the technical design. Every call to {@link #fire(Event, Object)} is
 * <strong>synchronized</strong> so only one event is processed at a time per
 * machine instance.
 * <p>
 * Listeners and interceptors are stored in {@link CopyOnWriteArrayList} instances,
 * making it safe to add or remove them while the machine is running (e.g., from
 * within a listener callback).
 * <p>
 * <strong>Transition order (fire algorithm):</strong>
 * <ol>
 *   <li>Check if machine is in a final state — if so, ignore the event</li>
 *   <li>Check for null event — silently ignore</li>
 *   <li>Look up transition by (currentState, event.type) — O(1)</li>
 *   <li>Run interceptor pre-hooks — any interceptor may veto</li>
 *   <li>Evaluate guard — must return true for the transition to proceed</li>
 *   <li>Notify listeners that a transition is starting</li>
 *   <li>Execute exit actions on the source state</li>
 *   <li>Execute transition actions</li>
 *   <li>Update the current state to the target</li>
 *   <li>Execute entry actions on the new state</li>
 *   <li>Run interceptor post-hooks and notify listeners</li>
 * </ol>
 * If any action throws, the exception is caught, listeners are notified via
 * {@code onError}, and a {@link TransitionExecutionException} is thrown. The
 * machine's state is left at whichever state it was in when the failure occurred
 * (partial transitions are not rolled back).
 *
 * @param <S> the state identifier type
 * @param <E> the event type discriminator
 * @param <C> the context type
 */
public final class DefaultStateMachine<S, E, C> implements StateMachine<S, E, C> {
    private final State<S, C> initialState;
    private final Map<S, State<S, C>> stateMap;
    private final Set<S> finalStateIds;
    private final TransitionIndex<S, E, C> transitionIndex;
    private final List<StateMachineListener<S, E>> listeners;
    private final List<StateMachineInterceptor<S, E, C>> interceptors;

    private volatile State<S, C> currentState;

    /**
     * Constructs a new state machine with the given definition.
     *
     * @param initialState    the initial (start) state — never changes
     * @param currentState    the starting current state (typically the same as initialState)
     * @param stateMap        the full map of registered state identifiers to State objects
     * @param finalStateIds   the set of state identifiers that are terminal
     * @param transitionIndex the lookup index for all transitions
     */
    public DefaultStateMachine(
        final State<S, C> initialState,
        final State<S, C> currentState,
        final Map<S, State<S, C>> stateMap,
        final Set<S> finalStateIds,
        final TransitionIndex<S, E, C> transitionIndex
    ) {
        this.initialState = initialState;
        this.currentState = currentState;
        this.stateMap = stateMap;
        this.finalStateIds = finalStateIds;
        this.transitionIndex = transitionIndex;
        this.listeners = new CopyOnWriteArrayList<>();
        this.interceptors = new CopyOnWriteArrayList<>();
    }

    @Override
    public synchronized State<S, C> fire(final Event<E, ?> event,
                                          final C context) {
        // Step 1: Final state check — ignore events in terminal states
        if (isFinal()) {
            return currentState;
        }

        // Step 2: Null event guard
        if (Objects.isNull(event)) {
            return currentState;
        }

        // Step 3: O(1) transition lookup
        final var transition = transitionIndex.find(currentState.id(), event.type());
        if (Objects.isNull(transition)) {
            notifyListenersOnError(currentState, event, new TransitionExecutionException(
                "No matching transition for event '" + event.type() + "' in state '" + currentState.id() + "'",
                null,
                currentState.id(), event.type(), null
            ));
            return currentState;
        }

        final var oldState = currentState;

        // Step 4: Interceptor pre-hooks (any may veto)
        for (final var interceptor : interceptors) {
            if (!interceptor.preTransition(oldState.id(), transition.target(), event, context)) {
                notifyListenersOnError(oldState, event, new TransitionExecutionException(
                    "Transition vetoed by interceptor", null,
                    oldState.id(), event.type(), transition.target()
                ));
                return currentState;
            }
        }

        // Step 5: Guard evaluation
        if (Objects.nonNull(transition.guard())) {
            try {
                if (!transition.guard().evaluate(context)) {
                    return currentState;
                }
            } catch (final Exception e) {
                return currentState;
            }
        }

        // Step 6: Notify transition start
        notifyListenersOnTransitionStart(oldState, transition, event);

        try {
            // Step 7: Exit actions on source state
            executeActions(oldState.onExit(), context);

            // Step 8: Transition actions
            executeActions(transition.actions(), context);

            // Step 9: Update current state
            currentState = stateMap.get(transition.target());

            // Step 10: Entry actions on target state
            executeActions(currentState.onEntry(), context);

            // Step 11a: Interceptor post-hooks
            for (final var interceptor : interceptors) {
                interceptor.postTransition(oldState.id(), currentState.id(), event, context);
            }

            // Step 11b: Listener notifications
            notifyListenersOnStateChanged(oldState, currentState);
            notifyListenersOnTransitionComplete(oldState, currentState, event);
        } catch (final Exception e) {
            notifyListenersOnError(oldState, event, e);
            throw new TransitionExecutionException(
                "Transition action failed", e,
                oldState.id(), event.type(), transition.target()
            );
        }

        return currentState;
    }

    @Override
    public State<S, C> getCurrentState() {
        return currentState;
    }

    @Override
    public State<S, C> getInitialState() {
        return initialState;
    }

    @Override
    public Set<State<S, C>> getStates() {
        return Set.copyOf(stateMap.values());
    }

    @Override
    public Set<Transition<S, E, C>> getTransitions() {
        return transitionIndex.getAllTransitions();
    }

    @Override
    public boolean isFinal() {
        return finalStateIds.contains(currentState.id());
    }

    @Override
    public void addListener(final StateMachineListener<S, E> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final StateMachineListener<S, E> listener) {
        listeners.remove(listener);
    }

    @Override
    public void addInterceptor(final StateMachineInterceptor<S, E, C> interceptor) {
        interceptors.add(interceptor);
    }

    @Override
    public void removeInterceptor(final StateMachineInterceptor<S, E, C> interceptor) {
        interceptors.remove(interceptor);
    }

    /**
     * Executes a list of actions in order, stopping at the first failure.
     */
    private static <C> void executeActions(final List<Action<C>> actions,
                                            final C context) throws Exception {
        for (final var action : actions) {
            action.execute(context);
        }
    }

    private void notifyListenersOnTransitionStart(
        final State<S, C> oldState,
        final Transition<S, E, C> transition,
        final Event<E, ?> event
    ) {
        for (final var listener : listeners) {
            listener.onTransitionStart(oldState.id(), transition.target(), event);
        }
    }

    private void notifyListenersOnStateChanged(final State<S, C> oldState,
                                                final State<S, C> newState) {
        for (final var listener : listeners) {
            listener.onStateChanged(oldState.id(), newState.id());
        }
    }

    private void notifyListenersOnTransitionComplete(
        final State<S, C> oldState,
        final State<S, C> newState,
        final Event<E, ?> event
    ) {
        for (final var listener : listeners) {
            listener.onTransitionComplete(oldState.id(), newState.id(), event);
        }
    }

    private void notifyListenersOnError(final State<S, C> state,
                                         final Event<E, ?> event,
                                         final Exception e) {
        for (final var listener : listeners) {
            listener.onError(state.id(), event, e);
        }
    }
}
