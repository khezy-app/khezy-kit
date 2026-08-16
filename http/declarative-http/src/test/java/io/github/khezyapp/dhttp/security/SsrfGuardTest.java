package io.github.khezyapp.dhttp.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.error.HttpApiException;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SsrfGuardTest {

    @Test
    @DisplayName("Should pass an allowed host")
    void allowedHostPasses() {
        SsrfGuard.validate("http://localhost:8080/contacts", List.of("localhost"), false);
    }

    @Test
    @DisplayName("Should throw whens the host cannot be resolved")
    void unresolvableHostThrows() {
        assertThrows(HttpApiException.class,
                () -> SsrfGuard.validate("http://ssrf.invalid/", List.of("localhost"), false));
    }

    @Test
    @DisplayName("Should pass an IP literal that is on the allow-list")
    void allowedLiteralPasses() {
        SsrfGuard.validate("http://192.168.1.1/", List.of("192.168.1.1"), false);
    }

    @Test
    @DisplayName("Should throw for an IP literal outside the allow-list")
    void disallowedLiteralThrows() {
        assertThrows(HttpApiException.class,
                () -> SsrfGuard.validate("http://10.0.0.5/", List.of("192.168.1.1"), false));
    }

    @Test
    @DisplayName("Should bypass the allow-list for IP literals whens opted in")
    void ipLiteralOptIn() {
        SsrfGuard.validate("http://10.0.0.5/", List.of(), true);
    }

    @Test
    @DisplayName("Should throw for a malformed URL")
    void malformedUrlThrows() {
        assertThrows(HttpApiException.class,
                () -> SsrfGuard.validate("not a url", List.of("localhost"), false));
    }

    @Test
    @DisplayName("Should expose a name-level allow-list helper")
    void allowsHelper() {
        assertTrue(SsrfGuard.allows("api.brevo.com", List.of("brevo.com")));
        assertTrue(SsrfGuard.allows("192.168.1.1", List.of("192.168.1.1")));
    }
}
