package io.github.khezyapp.fsm.core.impl;

import io.github.khezyapp.fsm.core.model.Transition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitionIndexTest {

    private static final String ROUTE = "route";
    private static final String KNOWN = "known";
    private static final String UNKNOWN = "unknown";
    private static final String CHECK = "check";

    private static Transition<String, String, Void> transition(final String id, final String target) {
        return new Transition<>(id, ROUTE, target, CHECK, null, List.of());
    }

    @Test
    @DisplayName("Should return candidates in definition order")
    void testFindAllPreservesOrder() {
        final var index = TransitionIndex.create(List.of(
            transition("first", KNOWN),
            transition("second", UNKNOWN),
            transition("third", KNOWN)
        ));

        final var candidates = index.findAll(ROUTE, CHECK);

        assertEquals(List.of("first", "second", "third"),
            candidates.stream().map(Transition::id).toList());
    }

    @Test
    @DisplayName("Should return empty list for unknown source and event")
    void testFindAllEmptyForUnknownPair() {
        final var index = TransitionIndex.create(List.of(transition("first", KNOWN)));

        assertTrue(index.findAll("nowhere", CHECK).isEmpty());
        assertTrue(index.findAll(ROUTE, "other").isEmpty());
    }

    @Test
    @DisplayName("Should return first candidate from find")
    void testFindReturnsFirstCandidate() {
        final var index = TransitionIndex.create(List.of(
            transition("first", KNOWN),
            transition("second", UNKNOWN)
        ));

        assertEquals("first", index.find(ROUTE, CHECK).id());
    }

    @Test
    @DisplayName("Should return null from find when no candidate exists")
    void testFindReturnsNullForUnknownPair() {
        final var index = TransitionIndex.create(List.of(transition("first", KNOWN)));

        assertNull(index.find(ROUTE, "other"));
    }
}
