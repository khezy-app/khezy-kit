package io.github.khezyapp.dhttp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecretRedactorTest {

    private final SecretRedactor redactor = SecretRedactor.get();

    @Test
    @DisplayName("Should mask every occurrence of a secret")
    void masksSecretOccurrences() {
        final var message = "Authorization: Bearer abc123xyz, callback?access_token=abc123xyz";

        assertEquals("Authorization: Bearer ***, callback?access_token=***",
                redactor.redact(message, List.of("abc123xyz")));
    }

    @Test
    @DisplayName("Should leave plain text unchanged")
    void plainTextUnchanged() {
        assertEquals("hello world", redactor.redact("hello world", List.of("s3cr3t")));
    }

    @Test
    @DisplayName("Should tolerate null text and empty secrets")
    void toleratesNulls() {
        assertNull(redactor.redact(null, List.of("s3cr3t")));
        assertEquals("hello", redactor.redact("hello", List.of()));
    }

    @Test
    @DisplayName("Should extract secret-like values from a map")
    void extractsSecretValues() {
        final var secrets = redactor.extractSecrets(Map.of(
                "password", "p@ss",
                "Authorization", "Bearer abc",
                "grant_type", "code"));

        assertEquals(2, secrets.size());
        assertTrue(secrets.contains("p@ss"));
        assertTrue(secrets.contains("Bearer abc"));
    }

    @Test
    @DisplayName("Should redact a message using secret-like map values")
    void redactsFromMap() {
        final var message = redactor.redact("password is p@ss", Map.of("password", "p@ss"));

        assertEquals("password is ***", message);
    }
}
