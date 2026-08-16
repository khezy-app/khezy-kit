package io.github.khezyapp.dhttp.auth.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.auth.credential.type.ClientCertificateCredentials;
import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Grant;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.spec.CredentialRef;

import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CredentialServiceTest {

    private CredentialService service;
    private CredentialRepository repository;
    private AesGcmCredentialCipher cipher;
    private SecretKey key;

    @BeforeEach
    void setUp() throws Exception {
        key = newKey();
        cipher = new AesGcmCredentialCipher(key);
        repository = new InMemoryCredentialRepository();
        service = new CredentialService(repository, cipher, JacksonJsonMapper.INSTANCE);
    }

    @Test
    @DisplayName("Should store only the encrypted payload, never plaintext")
    void storesNoPlaintext() {
        final var id = service.create("oauth2", oauth2Config());

        final var data = repository.findById(id).orElseThrow().data();

        assertTrue(data.containsKey("algorithm"));
        assertTrue(data.containsKey("iv"));
        assertTrue(data.containsKey("ciphertext"));
        assertEquals(3, data.size());
        assertFalse(data.containsKey("clientId"));
        assertFalse(data.containsKey("clientSecret"));
    }

    @Test
    @DisplayName("Should round-trip a typed credential through encrypt and decrypt")
    void roundTripsTypedCredential() {
        final var config = oauth2Config();

        final var id = service.create("oauth2", config);
        final var decrypted = service.get(id, OAuth2Credentials.class).orElseThrow();

        assertEquals(id, decrypted.id());
        assertEquals("oauth2", decrypted.type());
        assertEquals(config, decrypted.data());
    }

    @Test
    @DisplayName("Should produce identical stored payloads for map and typed creates")
    void mapAndTypedCreateProduceIdenticalPayloads() {
        final var config = oauth2Config();
        final var props = JacksonJsonMapper.INSTANCE.toMap(config);

        final var mapId = service.create("oauth2", props);
        final var typedId = service.create("oauth2", config);

        assertEquals(repository.findById(mapId).orElseThrow().data(),
                repository.findById(typedId).orElseThrow().data());
    }

    @Test
    @DisplayName("Should re-encrypt on update and remove on delete")
    void updateReEncryptsAndDeleteRemoves() {
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
    @DisplayName("Should expose only id and type whens listing")
    void listExposesOnlyIdAndType() {
        service.create("oauth2", oauth2Config());
        service.create("api-key", Map.of("headerName", "api-key", "value", "abc"));

        final var summaries = service.list();

        assertEquals(2, summaries.size());
        for (final CredentialSummary summary : summaries) {
            assertNotNull(summary.id());
            assertNotNull(summary.type());
        }
        assertTrue(summaries.stream()
                .allMatch(s -> "oauth2".equals(s.type()) || "api-key".equals(s.type())));
    }

    @Test
    @DisplayName("Should drive encryption from the consumer-supplied key")
    void keyProviderDrivesCipher() {
        final var providerCipher = new AesGcmCredentialCipher(() -> key);

        final var payload = providerCipher.encrypt(Map.of("username", "sok"));

        assertEquals("sok", providerCipher.decrypt(payload).get("username"));
    }

    @Test
    @DisplayName("Should fail to decrypt with a wrong key")
    void wrongKeyThrowsOnDecrypt() throws Exception {
        final var id = service.create("oauth2", oauth2Config());
        final var stored = repository.findById(id).orElseThrow();
        final var wrongCipher = new AesGcmCredentialCipher(newKey());

        assertThrows(RuntimeException.class,
                () -> wrongCipher.decrypt(EncryptedPayload.fromMap(stored.data())));
    }

    @Test
    @DisplayName("Should resolve credentials through asStore")
    void asStoreResolvesByRef() {
        final var id = service.create("oauth2", oauth2Config());
        final var store = service.asStore();

        final var resolved = store.resolve(CredentialRef.of("oauth2", id),
                new RequestContext("op", Map.of()));

        assertTrue(resolved.isPresent());
        assertEquals(id, resolved.get().id());
        assertEquals("oauth2", resolved.get().type());
        assertTrue(store.resolve(CredentialRef.of("oauth2", "missing"),
                new RequestContext("op", Map.of())).isEmpty());
    }

    @Test
    @DisplayName("Should round-trip client-certificate PEM through encrypt and decrypt")
    void roundTripsClientCertificateCredential() {
        final var config = new ClientCertificateCredentials(
                "-----BEGIN CERTIFICATE-----\nMIIB\n-----END CERTIFICATE-----\n",
                "-----BEGIN PRIVATE KEY-----\nMIIE\n-----END PRIVATE KEY-----\n",
                "p@ss");

        final var id = service.create("client-certificate", config);
        final var decrypted = service.get(id, ClientCertificateCredentials.class).orElseThrow();

        assertEquals(id, decrypted.id());
        assertEquals("client-certificate", decrypted.type());
        assertEquals(config, decrypted.data());
        assertFalse(repository.findById(id).orElseThrow().data().containsKey("certChainPem"));
    }

    private static SecretKey newKey() throws Exception {
        final var generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    private static OAuth2Credentials oauth2Config() {
        return new OAuth2Credentials(
                "client-1", "s3cr3t-client", "https://auth.example.com/token",
                "https://auth.example.com/authorize", "https://app.example.com/callback",
                "contacts.read", OAuth2Grant.AUTHORIZATION_CODE,
                Map.of("audience", "https://api.example.com"));
    }

    private static OAuth2Credentials oauth2Config2() {
        return new OAuth2Credentials(
                "client-2", "other-secret", "https://auth.example.com/token",
                null, null, "contacts.write", OAuth2Grant.REFRESH_TOKEN,
                Map.of());
    }
}
