package io.github.khezyapp.dhttp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.auth.credential.DecryptedCredential;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.transport.Auth;
import io.github.khezyapp.dhttp.transport.HttpRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GenericAuthenticatorTest {

    private final GenericAuthenticator authenticator = new GenericAuthenticator();

    @Test
    @DisplayName("Should inject an api-key header from headerName + value fields")
    void injectsApiKeyHeader() {
        final var credential = new DecryptedCredential<>("api-1", "api-key",
                Map.of("headerName", "api-key", "value", "s3cr3t"), null);
        final var out = new AuthResult();

        final var result = authenticator.apply(credential, request(), out);

        assertEquals("s3cr3t", result.headers().first("api-key").orElseThrow());
        assertTrue(out.applied());
        assertEquals("api-1", out.credentialId());
    }

    @Test
    @DisplayName("Should set HTTP Basic auth from username + password fields")
    void injectsBasicAuth() {
        final var credential = new DecryptedCredential<>("basic-1", "basic-auth",
                Map.of("username", "SOK", "password", "p@ss"), null);

        final var result = authenticator.apply(credential, request(), new AuthResult());

        final var auth = assertInstanceOf(Auth.BasicAuth.class, result.auth());
        assertEquals("SOK", auth.username());
        assertEquals("p@ss", auth.password());
        final var expected = "Basic " + Base64.getEncoder()
                .encodeToString("SOK:p@ss".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, "Basic " + Base64.getEncoder()
                .encodeToString((auth.username() + ":" + auth.password()).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Should merge all headers from a header-map credential")
    void mergesHttpHeaders() {
        final var credential = new DecryptedCredential<>("hdr-1", "http-header",
                Map.of("headers", Map.of("X-Tenant", "khezy", "Accept", "application/json")), null);

        final var result = authenticator.apply(credential, request(), new AuthResult());

        assertEquals("khezy", result.headers().first("X-Tenant").orElseThrow());
        assertEquals("application/json", result.headers().first("Accept").orElseThrow());
    }

    @Test
    @DisplayName("Should report applied=false whens no auth strategy matches")
    void noMatchReportsNotApplied() {
        final var credential = new DecryptedCredential<>("custom-1", "custom",
                Map.of("someField", "value"), null);
        final var out = new AuthResult();

        final var result = authenticator.apply(credential, request(), out);

        assertFalse(out.applied());
        assertEquals("custom-1", out.credentialId());
        assertEquals(Auth.NoAuth.class, result.auth().getClass());
    }

    @Test
    @DisplayName("Should never leak the secret into the request log output")
    void secretsDoNotLeakIntoToString() {
        final var credential = new DecryptedCredential<>("api-2", "api-key",
                Map.of("headerName", "api-key", "value", "s3cr3t-token"), null);

        final var result = authenticator.apply(credential, request(), new AuthResult());

        assertFalse(result.toString().contains("s3cr3t-token"));
        assertFalse(result.toString().contains("api-key"));
    }

    @Test
    @DisplayName("Should present an mTLS client identity from certChainPem + privateKeyPem fields")
    void injectsClientCertificate() {
        final var credential = new DecryptedCredential<>("cert-1", "client-certificate",
                Map.of("certChainPem", "chain", "privateKeyPem", "key",
                        "privateKeyPassword", "p@ss"), null);
        final var out = new AuthResult();

        final var result = authenticator.apply(credential, request(), out);

        final var tls = result.tlsConfig();
        assertNotNull(tls);
        assertEquals("chain", tls.certChainPem());
        assertEquals("key", tls.privateKeyPem());
        assertEquals("p@ss", tls.privateKeyPassword());
        assertTrue(out.applied());
        assertEquals("cert-1", out.credentialId());
    }

    @Test
    @DisplayName("Should present an mTLS client identity without a private key password")
    void injectsClientCertificateWithoutPassword() {
        final var credential = new DecryptedCredential<>("cert-2", "client-certificate",
                Map.of("certChainPem", "chain", "privateKeyPem", "key"), null);

        final var result = authenticator.apply(credential, request(), new AuthResult());

        final var tls = result.tlsConfig();
        assertNotNull(tls);
        assertEquals("chain", tls.certChainPem());
        assertEquals("key", tls.privateKeyPem());
    }

    private static HttpRequest request() {
        return HttpRequest.builder()
                .url("https://api.brevo.com/v3/contacts")
                .method(HttpMethod.GET)
                .build();
    }
}
