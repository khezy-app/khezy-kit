package io.github.khezyapp.fsm.core.builder;

import io.github.khezyapp.fsm.core.api.Action;
import io.github.khezyapp.fsm.core.api.Guard;

import java.util.ArrayList;
import java.util.List;

/**
 * A fluent sub-builder for configuring a single transition within a
 * {@link StateMachineBuilder}.
 * <p>
 * This configurer lets you attach an optional {@link Guard} and one or more
 * {@link Action}s to a transition. After configuration, call {@link #and()} to
 * return to the parent builder and continue defining the rest of the machine.
 * <p>
 * Usage:
 * <pre>{@code
 * .transition("submit", "DRAFT", "INFO_COLLECTED", "submit")
 *     .guard(ctx -> ctx.name() != null)
 *     .action(ctx -> log.info("Submitted"))
 * .and()
 * }</pre>
 *
 * @param <S> the state identifier type
 * @param <E> the event type discriminator
 * @param <C> the context type
 */
public class TransitionConfigurer<S, E, C> {
    private final StateMachineBuilder<S, E, C> parentBuilder;
    private final String id;
    private final S source;
    private final S target;
    private final E eventType;
    private Guard<C> guard;
    private final List<Action<C>> actions;

    /**
     * Package-private constructor — instances are created by
     * {@link StateMachineBuilder#transition(String, Object, Object, Object)}.
     */
    TransitionConfigurer(
        final StateMachineBuilder<S, E, C> parentBuilder,
        final String id,
        final S source,
        final S target,
        final E eventType
    ) {
        this.parentBuilder = parentBuilder;
        this.id = id;
        this.source = source;
        this.target = target;
        this.eventType = eventType;
        this.actions = new ArrayList<>();
    }

    /**
     * Attaches a guard condition to this transition.
     * The guard is evaluated before any side effects execute.
     *
     * @param guard the guard predicate; {@code null} means always allowed
     * @return this configurer for fluent chaining
     */
    public TransitionConfigurer<S, E, C> guard(final Guard<C> guard) {
        this.guard = guard;
        return this;
    }

    /**
     * Adds a single action to this transition's action list.
     * Actions execute in the order they are added, after the guard passes
     * and before entry actions on the target state.
     *
     * @param action the side-effect action to execute during the transition
     * @return this configurer for fluent chaining
     */
    public TransitionConfigurer<S, E, C> action(final Action<C> action) {
        this.actions.add(action);
        return this;
    }

    /**
     * Adds multiple actions to this transition's action list.
     *
     * @param actions the actions to add
     * @return this configurer for fluent chaining
     */
    public TransitionConfigurer<S, E, C> actions(final List<Action<C>> actions) {
        this.actions.addAll(actions);
        return this;
    }

    /**
     * Registers the configured transition with the parent builder and returns
     * to it for further machine configuration.
     * <p>
     * Call this method when you have finished attaching guard and actions to
     * the current transition.
     *
     * @return the parent {@link StateMachineBuilder}
     */
    public StateMachineBuilder<S, E, C> and() {
        parentBuilder.registerTransition(id, source, target, eventType, guard, List.copyOf(actions));
        return parentBuilder;
    }
}
