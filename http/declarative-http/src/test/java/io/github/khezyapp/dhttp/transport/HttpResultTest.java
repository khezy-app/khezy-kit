package io.github.khezyapp.dhttp.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpResultTest {

    @Test
    @DisplayName("Should report ok and body text for a 200 JSON response")
    void okWithJsonBody() {
        final var result = HttpResult.of(200, "{\"name\":\"VISAL\"}",
                Map.of("Content-Type", List.of("application/json")));

        assertTrue(result.ok());
        assertEquals("{\"name\":\"VISAL\"}", result.bodyText());
        assertEquals("{\"name\":\"VISAL\"}", result.bodyString());
        assertEquals("application/json", result.headers().get("Content-Type").get(0));
    }

    @Test
    @DisplayName("Should decode bytes whens only body is present")
    void decodesBytes() {
        final var result = new HttpResult(200, Map.of(), "hello".getBytes(StandardCharsets.UTF_8), null);

        assertTrue(result.ok());
        assertEquals("hello", result.bodyString());
    }

    @Test
    @DisplayName("Should not be ok for non-2xx status")
    void non2xxIsNotOk() {
        assertFalse(HttpResult.of(404, "missing").ok());
        assertFalse(HttpResult.of(500, "boom").ok());
    }

    @Test
    @DisplayName("Should defensively copy headers")
    void copiesHeaders() {
        final var list = new java.util.ArrayList<>(List.of("a"));
        final var headers = new java.util.LinkedHashMap<String, List<String>>();
        headers.put("X-Test", list);
        final var result = new HttpResult(200, headers, null, "");

        list.add("b");
        headers.put("X-Other", List.of("c"));

        assertEquals(List.of("a"), result.headers().get("X-Test"));
        assertFalse(result.headers().containsKey("X-Other"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.headers().get("X-Test").add("x"));
    }
}
