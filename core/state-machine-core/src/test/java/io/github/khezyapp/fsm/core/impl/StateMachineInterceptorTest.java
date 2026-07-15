package io.github.khezyapp.fsm.core.impl;

import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.api.StateMachineInterceptor;
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

class StateMachineInterceptorTest {

    private StateMachine<String, String, Void> buildMachine() throws StateMachineBuilderException {
        return StateMachineBuilder.<String, String, Void>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .transition("t1", "A", "B", "go")
            .and()
            .build();
    }

    @Test
    @DisplayName("Should allow transition when preTransition returns true")
    void testPreTransitionAllows() throws StateMachineBuilderException {
        final var machine = buildMachine();
        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String sourceState,
                final String targetState,
                final Event<String, ?> event,
                final Void context
            ) {
                return true;
            }
        });

        machine.fire(Event.of("go"), null);

        assertEquals("B", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should veto transition when preTransition returns false")
    void testPreTransitionVetoes() throws StateMachineBuilderException {
        final var machine = buildMachine();
        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String sourceState,
                final String targetState,
                final Event<String, ?> event,
                final Void context
            ) {
                return false;
            }
        });

        machine.fire(Event.of("go"), null);

        assertEquals("A", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should fire postTransition after successful transition")
    void testPostTransitionFires() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var captured = new AtomicReference<String>();

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public void postTransition(
                final String sourceState, final String targetState,
                final Event<String, ?> event, final Void context
            ) {
                captured.set(sourceState + "->" + targetState);
            }
        });

        machine.fire(Event.of("go"), null);

        assertEquals("A->B", captured.get());
    }

    @Test
    @DisplayName("Should not fire postTransition when vetoed")
    void testPostTransitionNotFiredOnVeto() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var postFired = new AtomicBoolean(false);

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String sourceState, final String targetState,
                final Event<String, ?> event, final Void context
            ) {
                return false;
            }

            @Override
            public void postTransition(
                final String sourceState, final String targetState,
                final Event<String, ?> event, final Void context
            ) {
                postFired.set(true);
            }
        });

        machine.fire(Event.of("go"), null);

        assertFalse(postFired.get());
    }

    @Test
    @DisplayName("Should execute multiple interceptors in order")
    void testMultipleInterceptorsInOrder() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var order = new ArrayList<Integer>();

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String s, final String t,
                final Event<String, ?> e, final Void c
            ) {
                order.add(1);
                return true;
            }

            @Override
            public void postTransition(
                final String s, final String t,
                final Event<String, ?> e, final Void c
            ) {
                order.add(3);
            }
        });

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String s, final String t,
                final Event<String, ?> e, final Void c
            ) {
                order.add(2);
                return true;
            }

            @Override
            public void postTransition(
                final String s, final String t,
                final Event<String, ?> e, final Void c
            ) {
                order.add(4);
            }
        });

        machine.fire(Event.of("go"), null);

        assertEquals(List.of(1, 2, 3, 4), order);
    }

    @Test
    @DisplayName("Should skip remaining interceptors on veto")
    void testSkipRemainingOnVeto() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var secondPreCalled = new AtomicBoolean(false);

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String s, final String t,
                final Event<String, ?> e, final Void c
            ) {
                return false;
            }
        });

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String s, final String t,
                final Event<String, ?> e, final Void c
            ) {
                secondPreCalled.set(true);
                return true;
            }
        });

        machine.fire(Event.of("go"), null);

        assertFalse(secondPreCalled.get());
    }

    @Test
    @DisplayName("Should allow interceptor to mutate message headers")
    void testInterceptorMutatesMessageHeaders() throws StateMachineBuilderException {
        final var machine = buildMachine();
        final var message = io.github.khezyapp.fsm.core.model.Message.of("data");

        machine.addInterceptor(new StateMachineInterceptor<String, String, Void>() {
            @Override
            public boolean preTransition(
                final String s, final String t,
                final Event<String, ?> event, final Void c
            ) {
                event.message().withHeader("enriched", "true");
                return true;
            }
        });

        machine.fire(Event.of("go", message), null);

        assertEquals("true", message.headers().get("enriched"));
    }
}
