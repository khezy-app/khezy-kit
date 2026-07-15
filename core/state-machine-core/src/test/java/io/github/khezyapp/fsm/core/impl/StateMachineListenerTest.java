package io.github.khezyapp.fsm.core.impl;

import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.api.StateMachineInterceptor;
import io.github.khezyapp.fsm.core.api.StateMachineListener;
import io.github.khezyapp.fsm.core.api.TransitionExecutionException;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilder;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilderException;
import io.github.khezyapp.fsm.core.model.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StateMachineListenerTest {

    private StateMachine<String, String, Void> buildMachine() throws StateMachineBuilderException {
        return StateMachineBuilder.<String, String, Void>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .transition("t1", "A", "B", "go")
                .action(ctx -> { })
            .and()
            .build();
    }

    @Test
    @DisplayName("Should fire onTransitionStart before transition")
    void testOnTransitionStart() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var captured = new AtomicReference<String>();

        machine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onTransitionStart(final String source, final String target, final Event<String, ?> event) {
                captured.set(source + "->" + target + ":" + event.type());
            }
        });

        machine.fire(Event.of("go"), null);

        assertEquals("A->B:go", captured.get());
    }

    @Test
    @DisplayName("Should fire onTransitionComplete after transition")
    void testOnTransitionComplete() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var captured = new AtomicReference<String>();

        machine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onTransitionComplete(final String source, final String target, final Event<String, ?> event) {
                captured.set(source + "->" + target + ":" + event.type());
            }
        });

        machine.fire(Event.of("go"), null);

        assertEquals("A->B:go", captured.get());
    }

    @Test
    @DisplayName("Should fire onStateChanged when state updates")
    void testOnStateChanged() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var captured = new AtomicReference<String>();

        machine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onStateChanged(final String oldState, final String newState) {
                captured.set(oldState + "->" + newState);
            }
        });

        machine.fire(Event.of("go"), null);

        assertEquals("A->B", captured.get());
    }

    @Test
    @DisplayName("Should not fire onStateChanged when transition is vetoed")
    void testOnStateChangedNotFiredOnVeto() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var stateChanged = new AtomicBoolean(false);

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String source, final String target,
                final Event<String, ?> event, final Void context
            ) {
                return false;
            }
        });
        machine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onStateChanged(final String oldState, final String newState) {
                stateChanged.set(true);
            }
        });

        machine.fire(Event.of("go"), null);

        assertFalse(stateChanged.get());
    }

    @Test
    @DisplayName("Should fire onError when interceptor vetoes")
    void testOnErrorOnVeto() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var capturedError = new AtomicReference<String>();

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String source, final String target,
                final Event<String, ?> event, final Void context
            ) {
                return false;
            }
        });
        machine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onError(final String state, final Event<String, ?> event, final Exception e) {
                capturedError.set(state + ":" + event.type() + ":" + e.getMessage());
            }
        });

        machine.fire(Event.of("go"), null);

        assertNotNull(capturedError.get());
        assertTrue(capturedError.get().contains("A:go"));
        assertTrue(capturedError.get().contains("vetoed"));
    }

    @Test
    @DisplayName("Should fire onError when action fails")
    void testOnErrorOnActionFailure() throws StateMachineBuilderException {
        final var failingMachine = StateMachineBuilder.<String, String, Void>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .transition("t1", "A", "B", "go")
                .action(ctx -> {
                    throw new RuntimeException("boom");
                })
            .and()
            .build();
        final var capturedError = new AtomicReference<String>();

        failingMachine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onError(final String state, final Event<String, ?> event, final Exception e) {
                capturedError.set(state + ":" + event.type() + ":" + e.getMessage());
            }
        });

        assertThrows(TransitionExecutionException.class, () ->
            failingMachine.fire(Event.of("go"), null)
        );

        assertNotNull(capturedError.get());
        assertTrue(capturedError.get().contains("A:go"));
    }

    @Test
    @DisplayName("Should fire onTransitionStart before onTransitionComplete")
    void testNotificationOrder() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var order = new ArrayList<String>();

        machine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onTransitionStart(final String source, final String target, final Event<String, ?> event) {
                order.add("start");
            }

            @Override
            public void onStateChanged(final String oldState, final String newState) {
                order.add("changed");
            }

            @Override
            public void onTransitionComplete(final String source, final String target, final Event<String, ?> event) {
                order.add("complete");
            }
        });

        machine.fire(Event.of("go"), null);

        assertEquals(List.of("start", "changed", "complete"), order);
    }

    @Test
    @DisplayName("Should fire onError when event has no matching transition")
    void testOnErrorOnNoMatchingTransition() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var capturedError = new AtomicReference<String>();

        machine.addListener(new StateMachineListener<String, String>() {
            @Override
            public void onError(final String state, final Event<String, ?> event, final Exception e) {
                capturedError.set(state + ":" + event.type() + ":" + e.getMessage());
            }
        });

        machine.fire(Event.of("unknown"), null);

        assertNotNull(capturedError.get());
        assertTrue(capturedError.get().contains("A:unknown"));
        assertTrue(capturedError.get().contains("No matching transition"));
        assertEquals("A", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should not fire removed listener")
    void testRemoveListener() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var fired = new AtomicBoolean(false);
        final var listener = new StateMachineListener<String, String>() {
            @Override
            public void onTransitionComplete(final String source, final String target, final Event<String, ?> event) {
                fired.set(true);
            }
        };

        machine.addListener(listener);
        machine.removeListener(listener);
        machine.fire(Event.of("go"), null);

        assertFalse(fired.get());
    }
}
