package io.github.khezyapp.fsm.core.impl;

import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.api.StateMachineListener;
import io.github.khezyapp.fsm.core.api.TransitionExecutionException;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilder;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilderException;
import io.github.khezyapp.fsm.core.model.Event;
import io.github.khezyapp.fsm.core.model.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DefaultStateMachineTest {

    private StateMachine<String, String, StringBuilder> machine;
    private final List<String> actionLog = new ArrayList<>();

    @BeforeEach
    void setUp() throws StateMachineBuilderException {
        actionLog.clear();
        machine = StateMachineBuilder.<String, String, StringBuilder>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .finalState("C")
            .transition("a_to_b", "A", "B", "go")
                .action(ctx -> actionLog.add("transition:go"))
            .and()
            .transition("b_to_c", "B", "C", "finish")
            .and()
            .build();
    }

    @Test
    @DisplayName("Should transition to target state on valid event")
    void testSimpleTransition() {
        machine.fire(Event.of("go"), new StringBuilder());

        assertEquals("B", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should ignore null event")
    void testNullEvent() {
        machine.fire(null, new StringBuilder());

        assertEquals("A", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should stay on current state for unknown event type")
    void testUnknownEvent() {
        machine.fire(Event.of("unknown"), new StringBuilder());

        assertEquals("A", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should stay on current state when no matching transition from current state")
    void testNoMatchingTransition() {
        machine.fire(Event.of("go"), new StringBuilder());
        assertEquals("B", machine.getCurrentState().id());

        machine.fire(Event.of("go"), new StringBuilder());

        assertEquals("B", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should notify listener onError when event has no matching transition")
    void testListenerNotifiedOnUnknownEvent() {
        final var captured = new AtomicReference<String>();
        machine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onError(final String state, final Event<String, ?> event, final Exception e) {
                captured.set(state + ":" + event.type());
            }
        });

        machine.fire(Event.of("unknown"), new StringBuilder());

        assertEquals("A:unknown", captured.get());
        assertEquals("A", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should ignore all events when in final state")
    void testFinalStateIgnoresEvents() {
        machine.fire(Event.of("go"), new StringBuilder());
        machine.fire(Event.of("finish"), new StringBuilder());

        assertTrue(machine.isFinal());
        assertEquals("C", machine.getCurrentState().id());

        machine.fire(Event.of("go"), new StringBuilder());

        assertEquals("C", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should allow transition when guard returns true")
    void testGuardAllows() throws StateMachineBuilderException {
        final var guarded = StateMachineBuilder.<String, String, String>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .transition("t1", "A", "B", "go")
                .guard(ctx -> ctx.equals("allow"))
            .and()
            .build();

        guarded.fire(Event.of("go"), "allow");

        assertEquals("B", guarded.getCurrentState().id());
    }

    @Test
    @DisplayName("Should block transition when guard returns false")
    void testGuardBlocks() throws StateMachineBuilderException {
        final var guarded = StateMachineBuilder.<String, String, String>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .transition("t1", "A", "B", "go")
                .guard(ctx -> ctx.equals("allow"))
            .and()
            .build();

        guarded.fire(Event.of("go"), "deny");

        assertEquals("A", guarded.getCurrentState().id());
    }

    @Test
    @DisplayName("Should treat guard exception as denial")
    void testGuardExceptionTreatedAsDenial() throws StateMachineBuilderException {
        final var guarded = StateMachineBuilder.<String, String, String>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .transition("t1", "A", "B", "go")
                .guard(ctx -> {
                    throw new RuntimeException("fail");
                })
            .and()
            .build();

        guarded.fire(Event.of("go"), "anything");

        assertEquals("A", guarded.getCurrentState().id());
    }

    @Test
    @DisplayName("Should execute exit actions before transition")
    void testExitActionsExecute() throws StateMachineBuilderException {
        final var log = new ArrayList<String>();
        final var m = StateMachineBuilder.<String, String, StringBuilder>builder()
            .initialState("A")
            .state("A", List.of(), List.of(ctx -> log.add("exit:A")))
            .state("B")
            .transition("t1", "A", "B", "go")
            .and()
            .build();

        m.fire(Event.of("go"), new StringBuilder());

        assertTrue(log.contains("exit:A"));
    }

    @Test
    @DisplayName("Should execute entry actions after transition")
    void testEntryActionsExecute() throws StateMachineBuilderException {
        final var log = new ArrayList<String>();
        final var m = StateMachineBuilder.<String, String, StringBuilder>builder()
            .initialState("A")
            .state("A")
            .state("B", List.of(ctx -> log.add("entry:B")), List.of())
            .transition("t1", "A", "B", "go")
            .and()
            .build();

        m.fire(Event.of("go"), new StringBuilder());

        assertTrue(log.contains("entry:B"));
    }

    @Test
    @DisplayName("Should execute transition actions")
    void testTransitionActionsExecute() {
        machine.fire(Event.of("go"), new StringBuilder());

        assertEquals(List.of("transition:go"), actionLog);
    }

    @Test
    @DisplayName("Should execute actions in order: exit → transition → entry")
    void testActionExecutionOrder() throws StateMachineBuilderException {
        final var log = new ArrayList<String>();
        final var m = StateMachineBuilder.<String, String, StringBuilder>builder()
            .initialState("A")
            .state("A", List.of(), List.of(ctx -> log.add("exit")))
            .state("B", List.of(ctx -> log.add("entry")), List.of())
            .transition("t1", "A", "B", "go")
                .action(ctx -> log.add("transition"))
            .and()
            .build();

        m.fire(Event.of("go"), new StringBuilder());

        assertEquals(List.of("exit", "transition", "entry"), log);
    }

    @Test
    @DisplayName("Should throw when action fails and keep old state")
    void testActionFailureKeepsOldState() throws StateMachineBuilderException {
        final var m = StateMachineBuilder.<String, String, StringBuilder>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .transition("t1", "A", "B", "go")
                .action(ctx -> {
                    throw new RuntimeException("action failed");
                })
            .and()
            .build();

        final var e = assertThrows(TransitionExecutionException.class, () ->
            m.fire(Event.of("go"), new StringBuilder())
        );

        assertEquals("A", m.getCurrentState().id());
        assertInstanceOf(RuntimeException.class, e.getCause());
    }

    @Test
    @DisplayName("Should return false from isFinal for non-final state")
    void testIsFinalReturnsFalse() {
        assertFalse(machine.isFinal());
    }

    @Test
    @DisplayName("Should return true from isFinal for final state")
    void testIsFinalReturnsTrue() {
        machine.fire(Event.of("go"), new StringBuilder());
        machine.fire(Event.of("finish"), new StringBuilder());

        assertTrue(machine.isFinal());
    }

    @Test
    @DisplayName("Should return all registered states")
    void testGetStates() {
        final var states = machine.getStates();

        final var stateIds = states.stream()
            .map(State::id)
            .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("A", "B", "C"), stateIds);
    }

    @Test
    @DisplayName("Should return all registered transitions")
    void testGetTransitions() {
        final var transitions = machine.getTransitions();

        assertEquals(2, transitions.size());
    }

    @Test
    @DisplayName("Should return initial state")
    void testGetInitialState() {
        assertEquals("A", machine.getInitialState().id());
    }

    @Test
    @DisplayName("Should propagate event message through fire")
    void testEventMessageAvailable() throws StateMachineBuilderException {
        final var captured = new AtomicReference<String>();
        final var m = StateMachineBuilder.<String, String, StringBuilder>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .transition("t1", "A", "B", "go")
                .action(ctx -> captured.set("executed"))
            .and()
            .build();

        final var message = io.github.khezyapp.fsm.core.model.Message.of("payload");
        m.fire(Event.of("go", message), new StringBuilder());

        assertEquals("executed", captured.get());
    }
}
