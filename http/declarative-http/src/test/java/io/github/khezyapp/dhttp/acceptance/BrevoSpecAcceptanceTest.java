package io.github.khezyapp.dhttp.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.khezyapp.dhttp.config.DeclarativeHttp;
import io.github.khezyapp.dhttp.config.DeclarativeHttpConfig;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.spec.Condition;
import io.github.khezyapp.dhttp.spec.CredentialRef;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.spec.Send;
import io.github.khezyapp.dhttp.spec.Target;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;

import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * §9 acceptance items 1, 2, 4: a Brevo-style spec (2 operations + Send body mapping + RootProperty
 * post-receive) executed through the assembled facade, asserting the exact resolved
 * {@code HttpRequest}, the exact output records, JEXL resolution from {@code $parameter} and
 * {@code $credentials}, and DynamicObjects dot-path resolution in Send and RootProperty.
 */
class BrevoSpecAcceptanceTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    private FakeTransport transport;
    private DeclarativeHttp http;
    private String credentialId;

    @BeforeEach
    void setUp() throws Exception {
        transport = new FakeTransport();
        http = facade(transport);
        credentialId = http.credentialService().create("api-key",
                Map.of("headerName", "api-key", "value", "xkeysib-super-secret"));
    }

    @Test
    @DisplayName("Item 1+2+4: create resolves the exact request, JEXL-bound headers, and output records")
    void contactCreateResolvesExactRequestAndRecords() {
        transport = new FakeTransport(HttpResult.of(200, JSON.write(Map.of("data",
                Map.of("id", 42, "name", "SOK", "email", "sok@example.com")))));
        http = facade(transport);
        credentialId = http.credentialService().create("api-key",
                Map.of("headerName", "api-key", "value", "xkeysib-super-secret"));

        final var records = http.execute(brevoSpec(credentialId),
                new RequestContext("contact.create", null,
                        Map.of("action", "create",
                                "contact", Map.of("name", "SOK", "email", "sok@example.com",
                                        "attributes", Map.of("city", "Battambang"))),
                        Map.of("value", "xkeysib-super-secret"), Map.of(), null));

        assertEquals(HttpMethod.POST, transport.lastRequest().method());
        assertEquals("https://api.brevo.com/v3/contacts", transport.lastRequest().url());
        assertEquals("application/json",
                transport.lastRequest().headers().first("Accept").orElseThrow());
        assertEquals("SOK",
                transport.lastRequest().headers().first("X-Contact-Name").orElseThrow());
        assertEquals("xkeysib-super-secret",
                transport.lastRequest().headers().first("X-Api-Key").orElseThrow());
        assertEquals("xkeysib-super-secret",
                transport.lastRequest().headers().first("api-key").orElseThrow());
        final var parsed = JSON.read(((Body.JsonBody) transport.lastRequest().body()).json(),
                Map.class);
        assertEquals(Map.of("city", "Battambang"), parsed.get("attributes"));

        assertEquals(1, records.size());
        assertEquals(Map.of("id", 42, "name", "SOK", "email", "sok@example.com"),
                records.get(0).json());
    }

    @Test
    @DisplayName("Item 2+4: list resolves the RootProperty dot path into the exact output records")
    void contactListResolvesRootPropertyDotPath() {
        transport = new FakeTransport(HttpResult.of(200, JSON.write(Map.of("data",
                Map.of("items", List.of(Map.of("id", 1, "name", "SOK"),
                        Map.of("id", 2, "name", "VISAL")))))));
        http = facade(transport);
        credentialId = http.credentialService().create("api-key",
                Map.of("headerName", "api-key", "value", "xkeysib-super-secret"));

        final var records = http.execute(brevoSpec(credentialId),
                new RequestContext("contact.list", null,
                        Map.of("action", "list"), Map.of(), Map.of(), null));

        assertEquals(HttpMethod.GET, transport.lastRequest().method());
        assertEquals("https://api.brevo.com/v3/contacts", transport.lastRequest().url());
        assertEquals(2, records.size());
        assertEquals("SOK", records.get(0).json().get("name"));
        assertEquals("VISAL", records.get(1).json().get("name"));
    }

    private static DeclarativeHttp facade(final FakeTransport transport) {
        final var config = DeclarativeHttpConfig.builder()
                .transport(transport)
                .keyProvider(BrevoSpecAcceptanceTest::newKey)
                .build();
        return DeclarativeHttp.create(config);
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

    private static HttpRequestSpec brevoSpec(final String credentialId) {
        final var createShape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of("X-Contact-Name", "= {{ $parameter.contact.name }}",
                        "X-Api-Key", "= {{ $credentials.value }}"),
                Map.of(), null, null);
        final var createSend = new Send("contact", Target.BODY, "attributes", true, null);
        final var createOp = new Operation("contact.create",
                List.of(new Condition("action", "create")),
                new Route(createShape, List.of(createSend),
                        new Output(50, List.of(new PostReceive.RootProperty("data"))),
                        null, List.of()));
        final var listShape = new RequestShape(HttpMethod.GET, "/contacts",
                Map.of(), Map.of(), null, null);
        final var listOp = new Operation("contact.list",
                List.of(new Condition("action", "list")),
                new Route(listShape, List.of(),
                        new Output(50, List.of(new PostReceive.RootProperty("data.items"))),
                        null, List.of()));
        return new HttpRequestSpec("https://api.brevo.com/v3",
                Map.of("Accept", "application/json"), 30000L, false,
                List.of(createOp, listOp), CredentialRef.of("api-key", credentialId), null,
                SecurityPolicy.defaults());
    }
}
