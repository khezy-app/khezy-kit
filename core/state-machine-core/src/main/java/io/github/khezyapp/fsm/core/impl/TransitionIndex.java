package io.github.khezyapp.fsm.core.impl;

import io.github.khezyapp.fsm.core.model.Transition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An O(1) lookup index for state machine transitions.
 * <p>
 * Transitions are indexed by source state and event type using a two-level map:
 * {@code Map<sourceState, Map<eventType, Transition>>}. This gives constant-time
 * lookup during {@code fire()} — a critical optimisation over linear scan when a
 * machine has many transitions.
 * <p>
 * The index is built once during machine construction and is immutable thereafter.
 *
 * @param <S> the state identifier type
 * @param <E> the event type discriminator
 * @param <C> the context type
 */
public final class TransitionIndex<S, E, C> {
    private final Map<S, Map<E, Transition<S, E, C>>> index;
    private final Set<Transition<S, E, C>> allTransitions;

    /**
     * Private constructor — instances are created via {@link #create(Collection)}.
     */
    private TransitionIndex(
        final Map<S, Map<E, Transition<S, E, C>>> index,
        final Set<Transition<S, E, C>> allTransitions
    ) {
        this.index = index;
        this.allTransitions = allTransitions;
    }

    /**
     * Factory method that builds the index from a collection of transitions.
     * <p>
     * Each transition is indexed by its source state and event type. If multiple
     * transitions share the same source + event, the last one wins (though the
     * builder's validation rules should prevent duplicates).
     *
     * @param transitions the collection of transitions to index
     * @param <S>         the state identifier type
     * @param <E>         the event type discriminator
     * @param <C>         the context type
     * @return a fully populated {@link TransitionIndex}
     */
    public static <S, E, C> TransitionIndex<S, E, C> create(final Collection<Transition<S, E, C>> transitions) {
        final var index = new HashMap<S, Map<E, Transition<S, E, C>>>();
        final var all = new HashSet<Transition<S, E, C>>();

        for (final var transition : transitions) {
            all.add(transition);
            index.computeIfAbsent(transition.source(), k -> new HashMap<>())
                .put(transition.eventType(), transition);
        }

        return new TransitionIndex<>(index, all);
    }

    /**
     * Looks up a transition for the given current state and event type.
     *
     * @param currentState the identifier of the machine's current state
     * @param eventType    the event type discriminator
     * @return the matching {@link Transition}, or {@code null} if no transition
     *         is defined for this (state, event) pair
     */
    public Transition<S, E, C> find(final S currentState,
                                     final E eventType) {
        final var bySource = index.get(currentState);
        if (bySource == null) {
            return null;
        }
        return bySource.get(eventType);
    }

    /**
     * Returns all transitions that were used to build this index.
     *
     * @return an immutable set of all registered transitions
     */
    public Set<Transition<S, E, C>> getAllTransitions() {
        return allTransitions;
    }
}
