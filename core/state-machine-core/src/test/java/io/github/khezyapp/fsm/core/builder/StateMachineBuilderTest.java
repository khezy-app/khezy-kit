package io.github.khezyapp.fsm.core.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateMachineBuilderTest {

    @Test
    @DisplayName("Should fail when initial state is not set")
    void testInitialStateNotSet() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .state("A")
                .build()
        );
        assertTrue(e.getViolations().stream().anyMatch(v -> v.contains("InitialStateNotSet")));
    }

    @Test
    @DisplayName("Should fail when initial state is not in states set")
    void testInitialStateNotFound() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .initialState("X")
                .state("A")
                .build()
        );
        assertTrue(e.getViolations().stream().anyMatch(v -> v.contains("InitialStateNotFound")));
    }

    @Test
    @DisplayName("Should fail when no states defined")
    void testNoStatesDefined() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .initialState("A")
                .build()
        );
        assertTrue(e.getViolations().stream().anyMatch(v -> v.contains("NoStatesDefined")));
    }

    @Test
    @DisplayName("Should fail when transition source state not found")
    void testSourceStateNotFound() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .initialState("A")
                .state("A")
                .state("B")
                .transition("t1", "X", "B", "go")
                .and()
                .build()
        );
        assertTrue(e.getViolations().stream().anyMatch(v -> v.contains("SourceStateNotFound")));
    }

    @Test
    @DisplayName("Should fail when transition target state not found")
    void testTargetStateNotFound() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .initialState("A")
                .state("A")
                .transition("t1", "A", "X", "go")
                .and()
                .build()
        );
        assertTrue(e.getViolations().stream().anyMatch(v -> v.contains("TargetStateNotFound")));
    }

    @Test
    @DisplayName("Should fail on duplicate transition (same source + event)")
    void testDuplicateTransition() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .initialState("A")
                .state("A")
                .state("B")
                .transition("t1", "A", "B", "go")
                .and()
                .transition("t2", "A", "B", "go")
                .and()
                .build()
        );
        assertTrue(e.getViolations().stream().anyMatch(v -> v.contains("DuplicateTransition")));
    }

    @Test
    @DisplayName("Should collect all violations in single exception")
    void testAllViolationsCollected() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .transition("t1", "X", "Y", "go")
                .and()
                .build()
        );
        assertTrue(e.getViolations().size() >= 3);
    }

    @Test
    @DisplayName("Should build valid machine successfully")
    void testValidBuild() throws StateMachineBuilderException {
        final var machine = StateMachineBuilder.<String, String, Void>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .finalState("B")
            .transition("t1", "A", "B", "go")
            .and()
            .build();

        assertNotNull(machine);
        assertEquals("A", machine.getCurrentState().id());
        assertFalse(machine.isFinal());
    }

    @Test
    @DisplayName("Should allow same source and event with different targets")
    void testMultipleTargetsAllowed() throws StateMachineBuilderException {
        final var machine = StateMachineBuilder.<String, String, String>builder()
            .initialState("route")
            .state("route")
            .state("known")
            .state("unknown")
            .transition("t1", "route", "known", "check")
                .guard(ctx -> "known".equals(ctx))
            .and()
            .transition("t2", "route", "unknown", "check")
                .guard(ctx -> "unknown".equals(ctx))
            .and()
            .build();

        assertEquals(2, machine.getTransitions().size());
    }

    @Test
    @DisplayName("Should reject all-guard-less transitions sharing source and event")
    void testAmbiguousTransitionsRejected() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .initialState("A")
                .state("A")
                .state("B")
                .state("C")
                .transition("t1", "A", "B", "go")
                .and()
                .transition("t2", "A", "C", "go")
                .and()
                .build()
        );
        assertTrue(e.getViolations().stream().anyMatch(v -> v.contains("AmbiguousTransition")));
    }

    @Test
    @DisplayName("Should allow guard-less fallback plus guarded candidates sharing source and event")
    void testGuardedPlusFallbackAllowed() throws StateMachineBuilderException {
        final var machine = StateMachineBuilder.<String, String, String>builder()
            .initialState("route")
            .state("route")
            .state("known")
            .state("unknown")
            .transition("known", "route", "known", "check")
                .guard(ctx -> "known".equals(ctx))
            .and()
            .transition("fallback", "route", "unknown", "check")
            .and()
            .build();

        assertNotNull(machine);
    }

    @Test
    @DisplayName("Should fail when resume references an undefined state")
    void testResumeStateNotFound() {
        final var e = assertThrows(StateMachineBuilderException.class, () ->
            StateMachineBuilder.<String, String, Void>builder()
                .initialState("A")
                .state("A")
                .state("B")
                .transition("t1", "A", "B", "go")
                .and()
                .resume("X")
                .build()
        );
        assertTrue(e.getViolations().stream().anyMatch(v -> v.contains("ResumeStateNotFound")));
    }

    @Test
    @DisplayName("Should build machine positioned at resumed state")
    void testResumeDefinedState() throws StateMachineBuilderException {
        final var machine = StateMachineBuilder.<String, String, Void>builder()
            .initialState("A")
            .state("A")
            .state("B")
            .finalState("B")
            .transition("t1", "A", "B", "go")
            .and()
            .resume("B")
            .build();

        assertEquals("B", machine.getCurrentState().id());
        assertTrue(machine.isFinal());
    }
}
