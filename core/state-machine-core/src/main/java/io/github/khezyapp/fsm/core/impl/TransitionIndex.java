package io.github.khezyapp.fsm.core.impl;

import io.github.khezyapp.fsm.core.model.Transition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An O(1) lookup index for state machine transitions.
 * <p>
 * Transitions are indexed by source state and event type using a two-level map:
 * {@code Map<sourceState, Map<eventType, List<Transition>>>}. Multiple candidates
 * may share a {@code (source, event)} pair (guard-driven branching); the list
 * preserves their definition (insertion) order so the first matching guard wins
 * during {@code fire()}. Lookup is still constant-time.
 * <p>
 * The index is built once during machine construction and is immutable thereafter.
 *
 * @param <S> the state identifier type
 * @param <E> the event type discriminator
 * @param <C> the context type
 */
public final class TransitionIndex<S, E, C> {
    private final Map<S, Map<E, List<Transition<S, E, C>>>> index;
    private final Set<Transition<S, E, C>> allTransitions;

    /**
     * Private constructor — instances are created via {@link #create(Collection)}.
     */
    private TransitionIndex(
        final Map<S, Map<E, List<Transition<S, E, C>>>> index,
        final Set<Transition<S, E, C>> allTransitions
    ) {
        this.index = index;
        this.allTransitions = allTransitions;
    }

    /**
     * Factory method that builds the index from a collection of transitions.
     * <p>
     * Each transition is indexed by its source state and event type. Transitions
     * sharing the same {@code (source, event)} pair are collected into an ordered
     * list, preserving the definition order of the supplied collection.
     *
     * @param transitions the collection of transitions to index
     * @param <S>         the state identifier type
     * @param <E>         the event type discriminator
     * @param <C>         the context type
     * @return a fully populated {@link TransitionIndex}
     */
    public static <S, E, C> TransitionIndex<S, E, C> create(final Collection<Transition<S, E, C>> transitions) {
        final var index = new HashMap<S, Map<E, List<Transition<S, E, C>>>>();
        final var all = new HashSet<Transition<S, E, C>>();

        for (final var transition : transitions) {
            all.add(transition);
            final var bySource = index.computeIfAbsent(transition.source(), k -> new HashMap<>());
            bySource.computeIfAbsent(transition.eventType(), k -> new ArrayList<>()).add(transition);
        }

        return new TransitionIndex<>(index, all);
    }

    /**
     * Returns all candidate transitions for the given current state and event type
     * in definition (insertion) order.
     *
     * @param currentState the identifier of the machine's current state
     * @param eventType    the event type discriminator
     * @return an immutable, ordered list of matching transitions, or an empty list
     *         if none are defined for this (state, event) pair
     */
    public List<Transition<S, E, C>> findAll(final S currentState,
                                             final E eventType) {
        final var bySource = index.get(currentState);
        if (bySource == null) {
            return List.of();
        }
        final var candidates = bySource.get(eventType);
        if (candidates == null) {
            return List.of();
        }
        return List.copyOf(candidates);
    }

    /**
     * Looks up the first candidate transition for the given current state and event type.
     *
     * @param currentState the identifier of the machine's current state
     * @param eventType    the event type discriminator
     * @return the first matching {@link Transition}, or {@code null} if no transition
     *         is defined for this (state, event) pair
     */
    public Transition<S, E, C> find(final S currentState,
                                     final E eventType) {
        final var candidates = findAll(currentState, eventType);
        return candidates.isEmpty() ? null : candidates.get(0);
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
