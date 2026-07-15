package io.github.khezyapp.fsm.core.builder;

import io.github.khezyapp.fsm.core.api.Action;
import io.github.khezyapp.fsm.core.api.Guard;
import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.impl.DefaultStateMachine;
import io.github.khezyapp.fsm.core.impl.TransitionIndex;
import io.github.khezyapp.fsm.core.model.State;
import io.github.khezyapp.fsm.core.model.Transition;

import java.util.*;

/**
 * A fluent builder for constructing fully validated {@link StateMachine} instances.
 * <p>
 * Use this builder to define states (with optional entry/exit actions), mark final
 * states, configure transitions (with optional guards and actions), and then call
 * {@link #build()} to produce a ready-to-use state machine.
 * <p>
 * The builder performs comprehensive validation at {@code build()} time — all
 * violations are collected and reported together as a
 * {@link StateMachineBuilderException}. This ensures that invalid machine definitions
 * are caught early (at configuration time) rather than at runtime.
 * <p>
 * Example — a simple KYC workflow:
 * <pre>{@code
 * StateMachine<String, String, KycContext> machine =
 *     StateMachineBuilder.<String, String, KycContext>builder()
 *         .initialState("DRAFT")
 *         .state("DRAFT")
 *         .state("INFO_COLLECTED")
 *         .finalState("APPROVED")
 *         .transition("submit", "DRAFT", "INFO_COLLECTED", "submit")
 *             .guard(ctx -> ctx.name() != null)
 *         .and()
 *         .build();
 * }</pre>
 *
 * @param <S> the state identifier type
 * @param <E> the event type discriminator
 * @param <C> the context type
 */
public class StateMachineBuilder<S, E, C> {
    private S initialStateId;
    private final Map<S, State<S, C>> states;
    private final Set<S> finalStateIds;
    private final List<Transition<S, E, C>> transitions;

    /** Private constructor — use {@link #builder()} */
    private StateMachineBuilder() {
        this.states = new HashMap<>();
        this.finalStateIds = new HashSet<>();
        this.transitions = new ArrayList<>();
    }

    /**
     * Creates a new {@code StateMachineBuilder}.
     *
     * @param <S> the state identifier type
     * @param <E> the event type discriminator
     * @param <C> the context type
     * @return a new builder instance
     */
    public static <S, E, C> StateMachineBuilder<S, E, C> builder() {
        return new StateMachineBuilder<>();
    }

    /**
     * Sets the initial (start) state of the machine.
     * This is a required field — if not set, {@link #build()} will fail with
     * {@code InitialStateNotSet}.
     *
     * @param stateId the identifier of the initial state
     * @return this builder for fluent chaining
     */
    public StateMachineBuilder<S, E, C> initialState(final S stateId) {
        this.initialStateId = stateId;
        return this;
    }

    /**
     * Registers a simple state with no entry or exit actions.
     *
     * @param stateId the state identifier
     * @return this builder for fluent chaining
     */
    public StateMachineBuilder<S, E, C> state(final S stateId) {
        this.states.put(stateId, State.of(stateId));
        return this;
    }

    /**
     * Registers a state with entry and exit lifecycle actions.
     * <p>
     * Entry actions run every time the machine transitions <em>into</em> this state.
     * Exit actions run every time the machine transitions <em>out of</em> this state.
     *
     * @param stateId the state identifier
     * @param onEntry actions to execute on entering this state
     * @param onExit  actions to execute on leaving this state
     * @return this builder for fluent chaining
     */
    public StateMachineBuilder<S, E, C> state(
        final S stateId,
        final List<Action<C>> onEntry,
        final List<Action<C>> onExit
    ) {
        this.states.put(stateId, new State<>(stateId, onEntry, onExit));
        return this;
    }

    /**
     * Registers a final (terminal) state.
     * <p>
     * When the machine is in a final state, all incoming events are silently ignored.
     * Final states are also automatically registered as regular states.
     *
     * @param stateId the state identifier
     * @return this builder for fluent chaining
     */
    public StateMachineBuilder<S, E, C> finalState(final S stateId) {
        this.finalStateIds.add(stateId);
        this.states.put(stateId, State.of(stateId));
        return this;
    }

    /**
     * Begins configuring a new transition rule and returns a {@link TransitionConfigurer}
     * for attaching a guard and actions.
     * <p>
     * Call {@link TransitionConfigurer#and()} to finish configuring this transition
     * and return to the parent builder.
     *
     * @param id        a human-readable transition identifier (used in logs and error messages)
     * @param source    the source state identifier
     * @param target    the target state identifier
     * @param eventType the event type that triggers this transition
     * @return a {@link TransitionConfigurer} for fluent sub-configuration
     */
    public TransitionConfigurer<S, E, C> transition(
        final String id,
        final S source,
        final S target,
        final E eventType
    ) {
        return new TransitionConfigurer<>(this, id, source, target, eventType);
    }

    /**
     * Package-private method used by {@link TransitionConfigurer#and()} to
     * register a fully configured transition.
     */
    void registerTransition(
        final String id,
        final S source,
        final S target,
        final E eventType,
        final Guard<C> guard,
        final List<Action<C>> actions
    ) {
        final var transition = new Transition<>(id, source, target, eventType, guard, actions);
        this.transitions.add(transition);
    }

    /**
     * Builds the state machine after validating the entire definition.
     * <p>
     * Validation checks (all violations collected before throwing):
     * <ol>
     *   <li>Initial state must be set</li>
     *   <li>Initial state must exist in defined states</li>
     *   <li>At least one state must be defined</li>
     *   <li>All transition source states must exist</li>
     *   <li>All transition target states must exist</li>
     *   <li>No duplicate transitions (same source + same event)</li>
     *   <li>All final states must exist in defined states</li>
     * </ol>
     *
     * @return a fully initialised {@link StateMachine} ready for use
     * @throws StateMachineBuilderException if any validation rule is violated
     */
    public StateMachine<S, E, C> build() throws StateMachineBuilderException {
        final var violations = new ArrayList<String>();

        validate(violations);

        if (!violations.isEmpty()) {
            throw new StateMachineBuilderException(violations);
        }

        final var stateMap = new HashMap<>(this.states);
        final var finalStates = new HashSet<>(this.finalStateIds);
        final var index = TransitionIndex.create(this.transitions);
        final var initialState = stateMap.get(initialStateId);

        return new DefaultStateMachine<>(initialState, initialState, stateMap, finalStates, index);
    }

    /**
     * Validates the machine definition and appends violation messages to the given list.
     */
    private void validate(final List<String> violations) {
        if (Objects.isNull(initialStateId)) {
            violations.add("InitialStateNotSet: initial state must be set");
        }

        if (states.isEmpty()) {
            violations.add("NoStatesDefined: at least one state must be defined");
        }

        if (Objects.nonNull(initialStateId) && !states.containsKey(initialStateId)) {
            violations.add("InitialStateNotFound: initial state '" + initialStateId + "' not found in defined states");
        }

        for (final var finalId : finalStateIds) {
            if (!states.containsKey(finalId)) {
                violations.add("FinalStateNotFound: final state '" + finalId + "' not found in defined states");
            }
        }

        final var transitionKeySet = new HashSet<String>();
        for (final var t : transitions) {
            if (!states.containsKey(t.source())) {
                violations.add("SourceStateNotFound: transition '" + t.id() + "' references source state '"
                    + t.source() + "' which is not defined");
            }
            if (!states.containsKey(t.target())) {
                violations.add("TargetStateNotFound: transition '" + t.id() + "' references target state '"
                    + t.target() + "' which is not defined");
            }

            final var key = t.source() + "::" + t.eventType();
            if (!transitionKeySet.add(key)) {
                violations.add("DuplicateTransition: duplicate transition for source '" + t.source()
                    + "' and event '" + t.eventType() + "'");
            }
        }
    }
}
