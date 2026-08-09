package io.github.khezyapp.fsm.core.impl;

import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilder;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilderException;
import io.github.khezyapp.fsm.core.model.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeTest {

    private record KycContext(String decision) { }

    private StateMachine<String, String, KycContext> buildMachine() throws StateMachineBuilderException {
        return StateMachineBuilder.<String, String, KycContext>builder()
            .initialState("manual_review")
            .state("manual_review")
            .finalState("approved")
            .transition("approve", "manual_review", "approved", "approve")
            .and()
            .resume("manual_review")
            .build();
    }

    @Test
    @DisplayName("Should resume at saved state and continue")
    void testResumeThenContinue() throws StateMachineBuilderException {
        final var machine = buildMachine();

        assertEquals("manual_review", machine.getCurrentState().id());
        assertFalse(machine.isFinal());

        machine.fire(Event.of("approve"), new KycContext("approved"));

        assertEquals("approved", machine.getCurrentState().id());
        assertTrue(machine.isFinal());
    }

    @Test
    @DisplayName("Should reflect resumed state in isFinal")
    void testIsFinalReflectsResumedState() throws StateMachineBuilderException {
        final var machine = StateMachineBuilder.<String, String, KycContext>builder()
            .initialState("manual_review")
            .state("manual_review")
            .finalState("approved")
            .transition("approve", "manual_review", "approved", "approve")
            .and()
            .resume("approved")
            .build();

        assertTrue(machine.isFinal());
    }

    @Test
    @DisplayName("Should ignore events when resumed into a final state")
    void testResumeIntoFinalStateIgnoresEvents() throws StateMachineBuilderException {
        final var machine = StateMachineBuilder.<String, String, KycContext>builder()
            .initialState("manual_review")
            .state("manual_review")
            .finalState("approved")
            .transition("approve", "manual_review", "approved", "approve")
            .and()
            .resume("approved")
            .build();

        machine.fire(Event.of("approve"), new KycContext("approved"));

        assertEquals("approved", machine.getCurrentState().id());
        assertTrue(machine.isFinal());
    }
}
