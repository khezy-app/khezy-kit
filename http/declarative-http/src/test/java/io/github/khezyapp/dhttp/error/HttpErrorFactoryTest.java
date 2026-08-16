package io.github.khezyapp.dhttp.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.transport.HttpResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpErrorFactoryTest {

    @Test
    @DisplayName("Should carry status, operationId, and itemIndex")
    void carriesStructuredFields() {
        final var e = HttpErrorFactory.of(401, "op", 3, "unauthorized");

        assertEquals(401, e.getStatus());
        assertEquals("op", e.getOperationId());
        assertEquals(3, e.getItemIndex());
    }

    @Test
    @DisplayName("Should derive status and message from a non-2xx result")
    void derivesFromHttpResult() {
        final var e = HttpErrorFactory.http("op", 0, HttpResult.of(404, "not found"));

        assertEquals(404, e.getStatus());
        assertTrue(e.getMessage().contains("404"));
        assertTrue(e.getMessage().contains("non-2xx"));
    }

    @Test
    @DisplayName("Should redact bearer tokens from messages")
    void redactsTokens() {
        final var e = HttpErrorFactory.of(500, "op", 0, "Bearer abc123 failed");

        assertFalse(e.getMessage().contains("abc123"));
        assertTrue(e.getMessage().contains("Bearer [REDACTED]"));
    }

    @Test
    @DisplayName("Should carry the cause")
    void carriesCause() {
        final var cause = new IllegalStateException("boom");
        final var e = HttpErrorFactory.of(500, "op", 0, "failed", cause);

        assertEquals(cause, e.getCause());
    }

    @Test
    @DisplayName("OAuth2 not configured exception exposes the credential id")
    void oauth2NotConfigured() {
        final var e = new OAuth2NotConfiguredException("creds-1");

        assertEquals("creds-1", e.getCredentialId());
        assertTrue(e.getMessage().contains("creds-1"));
    }
}
