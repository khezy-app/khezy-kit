package io.github.khezyapp.dhttp.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.spec.HttpMethod;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpRequestTest {

    @Test
    @DisplayName("Should build a GET with bracket-format query array and one header")
    void buildsGetWithBracketsQuery() {
        final var request = HttpRequest.builder()
                .url("https://api.example.com/contacts")
                .method(HttpMethod.GET)
                .header("Accept", "application/json")
                .query("ids", List.of("1", "2"))
                .queryArrayFormat(ArrayFormat.BRACKETS)
                .body(new Body.NoBody())
                .build();

        assertEquals("https://api.example.com/contacts", request.url());
        assertEquals(HttpMethod.GET, request.method());
        assertEquals(ArrayFormat.BRACKETS, request.queryArrayFormat());
        assertEquals(List.of("1", "2"), request.query().get("ids"));
        assertEquals(new Body.NoBody(), request.body());
    }

    @Test
    @DisplayName("Should read headers back case-insensitively")
    void headersAreCaseInsensitive() {
        final var request = HttpRequest.builder()
                .url("https://api.example.com")
                .method(HttpMethod.GET)
                .header("X-Api-Key", "secret")
                .build();

        assertTrue(request.headers().first("x-api-key").isPresent());
        assertEquals("secret", request.headers().first("X-API-KEY").get());
        assertEquals("secret", request.headers().getAll("X-Api-Key").get(0));
    }

    @Test
    @DisplayName("Should round-trip through toBuilder")
    void toBuilderRoundTrip() {
        final var original = HttpRequest.builder()
                .url("https://api.example.com/contacts")
                .method(HttpMethod.POST)
                .header("Accept", "application/json")
                .query("limit", 10)
                .body(new Body.JsonBody("{\"name\":\"SOK\"}"))
                .auth(new Auth.BearerAuth("token-1"))
                .returnFullResponse(true)
                .build();

        final var rebuilt = original.toBuilder().url("https://api.example.com/contacts/2").build();

        assertEquals("https://api.example.com/contacts/2", rebuilt.url());
        assertEquals(HttpMethod.POST, rebuilt.method());
        assertEquals("application/json", rebuilt.headers().first("Accept").get());
        assertEquals(10, rebuilt.query().get("limit"));
        assertEquals(new Body.JsonBody("{\"name\":\"SOK\"}"), rebuilt.body());
        assertEquals(new Auth.BearerAuth("token-1"), rebuilt.auth());
        assertTrue(rebuilt.returnFullResponse());

        assertEquals("https://api.example.com/contacts", original.url());
    }

    @Test
    @DisplayName("Should require url and method")
    void validatesRequiredFields() {
        assertThrows(IllegalStateException.class,
                () -> HttpRequest.builder().method(HttpMethod.GET).build());
        assertThrows(IllegalStateException.class,
                () -> HttpRequest.builder().url("https://api.example.com").build());
    }
}
