package io.github.khezyapp.dhttp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveOutputRedactorTest {

    @Test
    @DisplayName("Should mask a top-level sensitive field")
    void redactsTopLevelField() {
        final Map<String, Object> json = Map.of("token", "s3cr3t", "name", "SOK");

        final var redacted = SensitiveOutputRedactor.redact(json, List.of("token"));

        assertEquals("***", redacted.get("token"));
        assertEquals("SOK", redacted.get("name"));
    }

    @Test
    @DisplayName("Should mask a nested dotted field")
    void redactsNestedField() {
        final Map<String, Object> json = Map.of("data", Map.of("token", "s3cr3t", "name", "SOK"));

        final var redacted = SensitiveOutputRedactor.redact(json, List.of("data.token"));

        assertEquals("***", ((Map<?, ?>) redacted.get("data")).get("token"));
        assertEquals("SOK", ((Map<?, ?>) redacted.get("data")).get("name"));
    }

    @Test
    @DisplayName("Should apply a dotted field to every list element")
    void redactsFieldsInListElements() {
        final Map<String, Object> json = Map.of("data", List.of(
                Map.of("token", "a", "name", "SOK"),
                Map.of("token", "b", "name", "DARA")));

        final var redacted = SensitiveOutputRedactor.redact(json, List.of("data.token"));

        final var data = (List<?>) redacted.get("data");
        assertEquals("***", ((Map<?, ?>) data.get(0)).get("token"));
        assertEquals("***", ((Map<?, ?>) data.get(1)).get("token"));
        assertEquals("DARA", ((Map<?, ?>) data.get(1)).get("name"));
    }

    @Test
    @DisplayName("Should mask a whole nested map and ignore missing paths")
    void masksWholeNestedMapAndIgnoresMissing() {
        final Map<String, Object> json = Map.of("data", Map.of("secret", "x"), "name", "SOK");

        final var redacted = SensitiveOutputRedactor.redact(json, List.of("data", "missing.token"));

        assertEquals("***", redacted.get("data"));
        assertEquals("SOK", redacted.get("name"));
    }

    @Test
    @DisplayName("Should keep the input when no fields are configured")
    void keepsInputWithoutFields() {
        final Map<String, Object> json = Map.of("name", "SOK");

        assertSame(json, SensitiveOutputRedactor.redact(json, List.of()));
        assertSame(json, SensitiveOutputRedactor.redact(json, null));
    }
}
