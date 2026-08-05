package io.github.khezyapp.dynamicform.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormValuesTest {

    @Test
    @DisplayName("Should read top-level and nested dot-path values")
    void testGetDotPath() {
        final var values = FormValues.of(Map.of(
            "country", "US",
            "documents", Map.of("idType", "passport", "idNumber", "A123")
        ));

        assertEquals("US", values.get("country"));
        assertEquals("passport", values.get("documents.idType"));
        assertEquals("A123", values.get("documents.idNumber"));
        assertNull(values.get("documents.missing"));
        assertNull(values.get("missing"));
    }

    @Test
    @DisplayName("Should read collection row values by index path")
    void testGetIndexPath() {
        final var values = FormValues.of(Map.of(
            "directors", List.of(
                Map.of("name", "SOK", "idNumber", "N1"),
                Map.of("name", "SAO", "idNumber", "N2")
            )
        ));

        assertEquals("SOK", values.get("directors[0].name"));
        assertEquals("N2", values.get("directors[1].idNumber"));
        assertNull(values.get("directors[2].name"));
    }

    @Test
    @DisplayName("Should report presence via has")
    void testHas() {
        final var values = FormValues.of(Map.of("name", "VISAL"));

        assertTrue(values.has("name"));
        assertFalse(values.has("age"));
    }

    @Test
    @DisplayName("Should set nested values immutably via with")
    void testWithNested() {
        final var initial = FormValues.empty();
        final var updated = initial.with("documents.idType", "dl").with("country", "KH");

        assertEquals("dl", updated.get("documents.idType"));
        assertEquals("KH", updated.get("country"));
        assertTrue(initial.isEmpty());
    }

    @Test
    @DisplayName("Should set collection row values by index")
    void testWithIndex() {
        final var values = FormValues.empty()
            .with("directors", List.of(Map.of("name", "SOK")))
            .with("directors[0].idNumber", "N1");

        assertEquals("N1", values.get("directors[0].idNumber"));
        assertEquals("SOK", values.get("directors[0].name"));
    }

    @Test
    @DisplayName("Should remove values via without")
    void testWithout() {
        final var values = FormValues.of(Map.of("country", "US", "notes", "x"));
        final var updated = values.without("notes");

        assertFalse(updated.has("notes"));
        assertEquals("US", updated.get("country"));
        assertTrue(values.has("notes"));
    }

    @Test
    @DisplayName("Should expose an immutable asMap copy")
    void testAsMapImmutable() {
        final var values = FormValues.of(Map.of("country", "US"));
        final var map = values.asMap();

        assertThrows(UnsupportedOperationException.class, () -> map.put("x", "y"));
    }
}
