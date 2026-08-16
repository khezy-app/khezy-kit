package io.github.khezyapp.dhttp.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.error.HttpApiException;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainAllowListTest {

    private static final List<String> DOMAINS = List.of("brevo.com", "khezy.app");

    @Test
    @DisplayName("Should allow an exact host match")
    void exactMatch() {
        assertTrue(DomainAllowList.isAllowed("brevo.com", DOMAINS));
    }

    @Test
    @DisplayName("Should allow a subdomain of an allowed domain")
    void subdomainMatch() {
        assertTrue(DomainAllowList.isAllowed("api.brevo.com", DOMAINS));
        assertTrue(DomainAllowList.isAllowed("deep.sub.brevo.com", DOMAINS));
    }

    @Test
    @DisplayName("Should reject unrelated and similar-looking hosts")
    void rejectsUnrelatedHosts() {
        assertFalse(DomainAllowList.isAllowed("notbrevo.com", DOMAINS));
        assertFalse(DomainAllowList.isAllowed("evil-brevo.com", DOMAINS));
        assertFalse(DomainAllowList.isAllowed("example.org", DOMAINS));
    }

    @Test
    @DisplayName("Should match IP literals only exactly")
    void ipLiteralExactMatch() {
        assertTrue(DomainAllowList.isAllowed("192.168.1.1", List.of("192.168.1.1")));
        assertFalse(DomainAllowList.isAllowed("192.168.1.1", List.of("168.1.1")));
    }

    @Test
    @DisplayName("Should reject a null host")
    void rejectsNullHost() {
        assertFalse(DomainAllowList.isAllowed(null, DOMAINS));
    }

    @Test
    @DisplayName("Should throw whens a host is not allowed")
    void requireAllowedThrows() {
        assertThrows(HttpApiException.class,
                () -> DomainAllowList.requireAllowed("example.org", DOMAINS));
        DomainAllowList.requireAllowed("api.brevo.com", DOMAINS);
    }
}
