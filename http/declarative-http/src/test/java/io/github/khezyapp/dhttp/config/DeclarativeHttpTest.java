package io.github.khezyapp.dhttp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.auth.credential.type.BasicAuthCredentials;
import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Grant;
import io.github.khezyapp.dhttp.error.OAuth2NotConfiguredException;
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
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;

import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeclarativeHttpTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    private SecretKey key;

    @BeforeEach
    void setUp() throws Exception {
        final var generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        key = generator.generateKey();
    }

    @Test
    @DisplayName("Should validate an OAuth2 spec through the engine without sending a request")
    void validatesThroughEngine() {
        final var transport = new FakeTransport();
        final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
                .transport(transport)
                .keyProvider(() -> key)
                .build());
        final var id = http.credentialService().create("oauth2", oauth2Config());

        assertThrows(OAuth2NotConfiguredException.class,
                () -> http.validate(specWithCredential(id)));
        assertEquals(0, transport.callCount());
    }

    @Test
    @DisplayName("Should execute a spec through the engine returning output records")
    void executesThroughEngine() {
        final var transport = new FakeTransport(HttpResult.of(200, JSON.write(Map.of("data",
                List.of(Map.of("id", 1, "name", "SOK"))))));
        final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
                .transport(transport)
                .keyProvider(() -> key)
                .build());

        final var records = http.execute(contactListSpec(),
                new RequestContext("contact.list", Map.of()));

        assertEquals(1, records.size());
        assertEquals("SOK", records.get(0).json().get("name"));
    }

    @Test
    @DisplayName("Should expose a credential service the consumer can create and resolve")
    void credentialServiceManagesCredentials() {
        final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
                .transport(new FakeTransport())
                .keyProvider(() -> key)
                .build());

        final var id = http.credentialService().create("basic-auth",
                new BasicAuthCredentials("SOK", "s3cr3t"));
        final var resolved = http.credentialService().get(id, BasicAuthCredentials.class);

        assertTrue(resolved.isPresent());
        assertEquals("SOK", resolved.get().data().username());
    }

    @Test
    @DisplayName("Should fail to build without a keyProvider (the key is consumer-owned)")
    void buildRequiresKeyProvider() {
        final var e = assertThrows(IllegalArgumentException.class,
                () -> DeclarativeHttp.builder().transport(new FakeTransport()).build());

        assertTrue(e.getMessage().contains("keyProvider"));
    }

    private static OAuth2Credentials oauth2Config() {
        return new OAuth2Credentials("client-1", "s3cr3t", "https://auth.example.com/token",
                "https://auth.example.com/authorize", "https://app.example.com/callback",
                "sheets.read", OAuth2Grant.CLIENT_CREDENTIALS, Map.of());
    }

    private static HttpRequestSpec specWithCredential(final String id) {
        final var shape = new RequestShape(HttpMethod.GET, "/values/A1:B2", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("sheets.values.get",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        return new HttpRequestSpec("https://sheets.googleapis.com/v4", Map.of(), 30000L, false,
                List.of(operation), CredentialRef.of("oauth2", id), null,
                SecurityPolicy.defaults());
    }

    private static HttpRequestSpec contactListSpec() {
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data"))), null, List.of()));
        return new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());
    }
}
