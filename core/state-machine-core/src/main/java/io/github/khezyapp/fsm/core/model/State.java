package io.github.khezyapp.fsm.core.model;

import io.github.khezyapp.fsm.core.api.Action;

import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of a single state in a finite state machine.
 * <p>
 * A state is identified by a unique {@code id} of type {@code S}. It can optionally carry
 * lifecycle actions that execute automatically when the machine <em>enters</em>
 * ({@code onEntry}) or <em>leaves</em> ({@code onExit}) this state during a transition.
 * These hooks fire <strong>every time</strong> the boundary is crossed, regardless of
 * which event or transition caused the movement.
 * <p>
 * The action lists are defensively copied at construction time and are never modified
 * afterwards — the machine's topology is fixed once built.
 *
 * @param <S> the type used to identify states (e.g. {@code String}, {@code enum})
 * @param <C> the type of the shared context object passed to entry/exit actions
 * @param id  the unique identifier for this state within the machine
 * @param onEntry actions executed every time the machine transitions <em>into</em> this state
 * @param onExit  actions executed every time the machine transitions <em>out of</em> this state
 */
public record State<S, C>(
    S id,
    List<Action<C>> onEntry,
    List<Action<C>> onExit
) {
    /**
     * Compact canonical constructor that normalises null action lists to empty immutable lists.
     */
    public State {
        onEntry = Objects.nonNull(onEntry) ? List.copyOf(onEntry) : List.of();
        onExit = Objects.nonNull(onExit) ? List.copyOf(onExit) : List.of();
    }

    /**
     * Creates a simple state with no entry or exit actions.
     *
     * @param id  the state identifier
     * @param <S> the state identifier type
     * @param <C> the context type
     * @return a new state with empty onEntry and onExit lists
     */
    public static <S, C> State<S, C> of(final S id) {
        return new State<>(id, null, null);
    }
}
