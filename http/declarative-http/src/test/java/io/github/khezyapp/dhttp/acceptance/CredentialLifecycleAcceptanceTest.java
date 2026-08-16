package io.github.khezyapp.dhttp.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.auth.credential.AesGcmCredentialCipher;
import io.github.khezyapp.dhttp.auth.credential.CredentialService;
import io.github.khezyapp.dhttp.auth.credential.InMemoryCredentialRepository;
import io.github.khezyapp.dhttp.auth.credential.type.BasicAuthCredentials;
import io.github.khezyapp.dhttp.auth.credential.type.ClientCertificateCredentials;
import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Grant;
import io.github.khezyapp.dhttp.config.DeclarativeHttp;
import io.github.khezyapp.dhttp.config.DeclarativeHttpConfig;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.spec.CredentialRef;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * §9 acceptance item 8: the credential lifecycle (§6.2) — no plaintext at rest, typed round-trips,
 * identical map/typed payloads, update/delete, id+type-only listing, the consumer-supplied key
 * driving the cipher, and the engine resolving through {@code CredentialStore} without CRUD.
 */
class CredentialLifecycleAcceptanceTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    private InMemoryCredentialRepository repository;
    private CredentialService service;

    @BeforeEach
    void setUp() throws Exception {
        final var generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        final var key = generator.generateKey();
        repository = new InMemoryCredentialRepository();
        service = new CredentialService(repository, new AesGcmCredentialCipher(() -> key), JSON);
    }

    @Test
    @DisplayName("Item 8a: create stores only the encrypted payload, never plaintext")
    void createStoresNoPlaintext() {
        final var id = service.create("oauth2", oauth2Config());

        final var data = repository.findById(id).orElseThrow().data();

        assertEquals(Set.of("algorithm", "iv", "ciphertext"), data.keySet());
        assertFalse(data.containsKey("clientId"));
        assertFalse(data.containsKey("clientSecret"));
    }

    @Test
    @DisplayName("Item 8b: get round-trips decrypt to a type-safe OAuth2Credentials")
    void getRoundTripsTyped() {
        final var id = service.create("oauth2", oauth2Config());

        final var decrypted = service.get(id, OAuth2Credentials.class).orElseThrow();

        assertEquals(id, decrypted.id());
        assertEquals("oauth2", decrypted.type());
        assertEquals(oauth2Config(), decrypted.data());
    }

    @Test
    @DisplayName("Item 8c: map-form and type-safe creates produce identical stored payloads")
    void mapAndTypedCreatesMatch() {
        final var mapId = service.create("oauth2", JSON.toMap(oauth2Config()));
        final var typedId = service.create("oauth2", oauth2Config());

        assertEquals(repository.findById(mapId).orElseThrow().data(),
                repository.findById(typedId).orElseThrow().data());
    }

    @Test
    @DisplayName("Item 8d: update re-encrypts and delete removes from the repository")
    void updateAndDeleteMutateRepository() {
        final var id = service.create("oauth2", oauth2Config());
        final var before = repository.findById(id).orElseThrow().data();

        final var updated = service.update(id, oauth2Config2());
        assertEquals("client-2", updated.data().clientId());
        assertNotEquals(before, repository.findById(id).orElseThrow().data());

        service.delete(id);
        assertTrue(service.get(id).isEmpty());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Item 8e: list exposes only id and type, never secrets")
    void listExposesOnlyIdAndType() {
        service.create("oauth2", oauth2Config());
        service.create("basic-auth", new BasicAuthCredentials("SOK", "s3cr3t"));

        final var summaries = service.list();

        assertEquals(2, summaries.size());
        assertTrue(summaries.stream().noneMatch(summary -> summary.toString().contains("s3cr3t")));
        assertTrue(summaries.stream()
                .allMatch(summary -> "oauth2".equals(summary.type())
                        || "basic-auth".equals(summary.type())));
    }

    @Test
    @DisplayName("Item 8f: the consumer-supplied KeyProvider key drives the cipher")
    void keyProviderDrivesCipher() throws Exception {
        final var generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        final var key = generator.generateKey();
        final var cipher = new AesGcmCredentialCipher(() -> key);

        final var payload = cipher.encrypt(Map.of("username", "SOK"));

        assertEquals("SOK", cipher.decrypt(payload).get("username"));
        assertThrows(RuntimeException.class,
                () -> new AesGcmCredentialCipher(() -> newKey()).decrypt(payload));
    }

    @Test
    @DisplayName("Item 8g: the engine resolves through CredentialStore and never touches CRUD")
    void engineResolvesWithoutCrd() {
        final var transport = new FakeTransport();
        final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
                .transport(transport)
                .credentialService(service)
                .keyProvider(CredentialLifecycleAcceptanceTest::newKey)
                .build());
        final var id = service.create("api-key",
                Map.of("headerName", "api-key", "value", "xkeysib-super-secret"));

        final var records = http.execute(apiKeySpec(id), new RequestContext("contact.list", Map.of()));

        assertEquals("xkeysib-super-secret",
                transport.lastRequest().headers().first("api-key").orElseThrow());
        assertTrue(records.isEmpty());

        service.delete(id);
        assertThrows(HttpApiException.class,
                () -> http.execute(apiKeySpec(id), new RequestContext("contact.list", Map.of())));
    }

    @Test
    @DisplayName("Item 8h: a client-certificate credential flows to the transport as a tlsConfig")
    void clientCertificateFlowsToTransport() {
        final var transport = new FakeTransport();
        final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
                .transport(transport)
                .credentialService(service)
                .keyProvider(CredentialLifecycleAcceptanceTest::newKey)
                .build());
        final var id = service.create("client-certificate",
                new ClientCertificateCredentials("chain-pem", "key-pem", "p@ss"));

        http.execute(certSpec(id), new RequestContext("contact.list", Map.of()));

        final var tls = transport.lastRequest().tlsConfig();
        assertNotNull(tls);
        assertEquals("chain-pem", tls.certChainPem());
        assertEquals("key-pem", tls.privateKeyPem());
        assertEquals("p@ss", tls.privateKeyPassword());
    }

    private static SecretKey newKey() {
        try {
            final var generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (final java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("AES unavailable", e);
        }
    }

    private static OAuth2Credentials oauth2Config() {
        return new OAuth2Credentials("client-1", "s3cr3t-client", "https://auth.example.com/token",
                "https://auth.example.com/authorize", "https://app.example.com/callback",
                "contacts.read", OAuth2Grant.AUTHORIZATION_CODE,
                Map.of("audience", "https://api.example.com"));
    }

    private static OAuth2Credentials oauth2Config2() {
        return new OAuth2Credentials("client-2", "other-secret", "https://auth.example.com/token",
                null, null, "contacts.write", OAuth2Grant.REFRESH_TOKEN, Map.of());
    }

    private static HttpRequestSpec apiKeySpec(final String id) {
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data"))), null, List.of()));
        return new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), CredentialRef.of("api-key", id), null,
                SecurityPolicy.defaults());
    }

    private static HttpRequestSpec certSpec(final String id) {
        final var shape = new RequestShape(HttpMethod.GET, "/secure", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data"))), null, List.of()));
        return new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, true,
                List.of(operation), CredentialRef.of("client-certificate", id), null,
                SecurityPolicy.defaults());
    }
}
