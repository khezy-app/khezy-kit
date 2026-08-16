package io.github.khezyapp.dhttp.action.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RootPropertyTest {

    @Test
    @DisplayName("Should extract a dotted property from the response body as records")
    void extractsDottedProperty() {
        final var response = HttpResult.of(200, "{\"data\":{\"items\":["
                + "{\"id\":1,\"name\":\"SOK\"},{\"id\":2,\"name\":\"VISAL\"}]}}");
        final var action = new RootProperty("data.items");

        final var records = action.apply(List.of(), response);

        assertEquals(2, records.size());
        assertEquals(1, records.get(0).json().get("id"));
        assertEquals("SOK", records.get(0).json().get("name"));
        assertEquals(2, records.get(1).json().get("id"));
        assertEquals("VISAL", records.get(1).json().get("name"));
    }

    @Test
    @DisplayName("Should produce a single record for a map-valued property")
    void extractsSingleMap() {
        final var response = HttpResult.of(200, "{\"user\":{\"name\":\"RATH\"}}");

        final var records = new RootProperty("user").apply(List.of(), response);

        assertEquals(1, records.size());
        assertEquals("RATH", records.get(0).json().get("name"));
    }

    @Test
    @DisplayName("Should yield no records whens the property is absent")
    void missingPropertyYieldsNothing() {
        final var response = HttpResult.of(200, "{\"data\":{}}");

        final var records = new RootProperty("data.items").apply(List.of(), response);

        assertTrue(records.isEmpty());
    }

    @Test
    @DisplayName("Should treat an array response as the root records")
    void arrayResponseIsRoot() {
        final var response = HttpResult.of(
                200, "[{\"id\":1,\"name\":\"SOK\"},{\"id\":2,\"name\":\"VISAL\"}]");

        final var records = new RootProperty("").apply(List.of(), response);

        assertEquals(2, records.size());
        assertEquals("SOK", records.get(0).json().get("name"));
        assertEquals(2, records.get(1).json().get("id"));
    }

    @Test
    @DisplayName("Should wrap array elements that are not objects")
    void scalarArrayElementsAreWrapped() {
        final var response = HttpResult.of(200, "[1,2,3]");

        final var records = new RootProperty("").apply(List.of(), response);

        assertEquals(3, records.size());
        assertEquals(1, records.get(0).json().get("value"));
        assertEquals(3, records.get(2).json().get("value"));
    }

    @Test
    @DisplayName("Should wrap a JSON string response as a single value record")
    void jsonStringBecomesValueRecord() {
        final var response = HttpResult.of(200, "\"ready\"");

        final var records = new RootProperty("").apply(List.of(), response);

        assertEquals(1, records.size());
        assertEquals("ready", records.get(0).json().get("value"));
    }

    @Test
    @DisplayName("Should preserve a non-JSON plain text response")
    void plainTextBecomesValueRecord() {
        final var response = HttpResult.of(200, "welcome to khezy");

        final var records = new RootProperty("").apply(List.of(), response);

        assertEquals(1, records.size());
        assertEquals("welcome to khezy", records.get(0).json().get("value"));
    }
}
