package io.github.khezyapp.fsm.core.model;

import io.github.khezyapp.fsm.core.api.Action;
import io.github.khezyapp.fsm.core.api.Guard;

import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of a single transition rule in a finite state machine.
 * <p>
 * A transition binds together a <strong>source state</strong>, an <strong>event type</strong>,
 * and a <strong>target state</strong>. When the machine is in the source state and receives
 * a matching event, this rule is triggered. An optional {@link Guard} can further restrict
 * whether the transition is allowed based on the current context, and a list of {@link Action}s
 * defines the side effects that execute as part of the transition.
 * <p>
 * The transition {@code id} is a human-readable label useful for logging, debugging, and
 * traceability — it is not used for lookup (lookup is by source state + event type).
 *
 * @param <S> the state identifier type
 * @param <E> the event type discriminator
 * @param <C> the context type passed to guards and actions
 * @param id        a human-readable identifier for this transition (used in logs and errors)
 * @param source    the source state the machine must be in for this transition to apply
 * @param target    the target state the machine moves to after this transition
 * @param eventType the event type that triggers this transition
 * @param guard     an optional condition; {@code null} means the transition is always allowed
 * @param actions   the side-effect actions that execute during the transition (immutable list)
 */
public record Transition<S, E, C>(
    String id,
    S source,
    S target,
    E eventType,
    Guard<C> guard,
    List<Action<C>> actions
) {
    /**
     * Compact canonical constructor that normalises a null action list to an empty immutable list.
     * A null guard is left as-is (treated as "always allow" during evaluation).
     */
    public Transition {
        actions = Objects.nonNull(actions) ? List.copyOf(actions) : List.of();
    }
}
