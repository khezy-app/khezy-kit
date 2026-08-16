package io.github.khezyapp.dhttp.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedirectPolicyTest {

    private final RedirectPolicy defaultPolicy = new RedirectPolicy();

    @Test
    @DisplayName("Should not strip credentials for a same-origin redirect")
    void sameOriginDoesNotStrip() {
        assertFalse(defaultPolicy.shouldStripCredentials("https://a.com/x", "https://a.com/y"));
        assertFalse(defaultPolicy.shouldStripCredentials("http://a.com/x", "http://a.com:80/y"));
        assertFalse(defaultPolicy.shouldStripCredentials("https://a.com:443/x", "https://a.com/y"));
    }

    @Test
    @DisplayName("Should strip credentials for a cross-origin redirect by default")
    void crossOriginStripsByDefault() {
        assertTrue(defaultPolicy.shouldStripCredentials("https://a.com/x", "https://b.com/y"));
        assertTrue(defaultPolicy.shouldStripCredentials("https://a.com/x", "http://a.com/y"));
        assertTrue(defaultPolicy.shouldStripCredentials("https://a.com/x", "https://a.com:8443/y"));
    }

    @Test
    @DisplayName("Should not strip credentials whens cross-origin is opted in")
    void crossOriginOptInDoesNotStrip() {
        final var permissive = new RedirectPolicy(true);

        assertFalse(permissive.shouldStripCredentials("https://a.com/x", "https://b.com/y"));
    }

    @Test
    @DisplayName("Should strip credentials whens a URL cannot be parsed")
    void unparseableRedirectStrips() {
        assertTrue(defaultPolicy.shouldStripCredentials("https://a.com/x", "not a url"));
    }
}
