package io.github.khezyapp.dhttp.action.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SortByKeyTest {

    @Test
    @DisplayName("Should sort records ascending by the dotted key")
    void sortsAscending() {
        final var records = List.of(
                OutputRecord.ofJson(Map.of("info", Map.of("age", 30), "name", "SOK")),
                OutputRecord.ofJson(Map.of("info", Map.of("age", 20), "name", "VISAL")));

        final var sorted = new SortByKey("info.age", false).apply(records, HttpResult.of(200, "{}"));

        assertEquals("VISAL", sorted.get(0).json().get("name"));
        assertEquals("SOK", sorted.get(1).json().get("name"));
    }

    @Test
    @DisplayName("Should sort records descending by the dotted key")
    void sortsDescending() {
        final var records = List.of(
                OutputRecord.ofJson(Map.of("age", 20, "name", "VISAL")),
                OutputRecord.ofJson(Map.of("age", 30, "name", "SOK")));

        final var sorted = new SortByKey("age", true).apply(records, HttpResult.of(200, "{}"));

        assertEquals("SOK", sorted.get(0).json().get("name"));
        assertEquals("VISAL", sorted.get(1).json().get("name"));
    }
}
