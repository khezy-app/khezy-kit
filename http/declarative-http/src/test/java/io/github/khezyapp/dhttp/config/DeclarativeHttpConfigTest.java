package io.github.khezyapp.dhttp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.auth.credential.AesGcmCredentialCipher;
import io.github.khezyapp.dhttp.auth.credential.CredentialService;
import io.github.khezyapp.dhttp.auth.credential.InMemoryCredentialRepository;
import io.github.khezyapp.dhttp.auth.oauth2.InMemoryTokenStore;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.jdk.JdkHttpTransport;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;
import io.github.khezyapp.dhttp.pagination.PaginationStrategy;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeclarativeHttpConfigTest {

    private SecretKey key;

    @BeforeEach
    void setUp() throws Exception {
        final var generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        key = generator.generateKey();
    }

    @Test
    @DisplayName("Should assemble a full config returning the injected pieces and a built engine")
    void assemblesFullConfig() {
        final var jsonMapper = JacksonJsonMapper.INSTANCE;
        final var transport = new FakeTransport(HttpResult.of(200, "{}"));
        final var evaluator = new JexlExpressionEvaluator();
        final var tokenStore = new InMemoryTokenStore();
        final var service = new CredentialService(new InMemoryCredentialRepository(),
                new AesGcmCredentialCipher(() -> key), jsonMapper);

        final var config = DeclarativeHttpConfig.builder()
                .jsonMapper(jsonMapper)
                .transport(transport)
                .evaluator(evaluator)
                .tokenStore(tokenStore)
                .credentialService(service)
                .keyProvider(() -> key)
                .build();

        assertSame(jsonMapper, config.jsonMapper());
        assertSame(transport, config.transport());
        assertSame(evaluator, config.evaluator());
        assertSame(tokenStore, config.tokenStore());
        assertSame(service, config.credentialService());
        assertNotNull(config.credentialStore());
        assertNotNull(config.engine());
    }

    @Test
    @DisplayName("Should build a default cipher-backed credential service from a keyProvider")
    void keyProviderBuildsDefaultService() {
        final var config = DeclarativeHttpConfig.builder()
                .transport(new FakeTransport())
                .keyProvider(() -> key)
                .build();

        assertNotNull(config.credentialService());
        assertNotNull(config.credentialStore());
        assertNotNull(config.engine());
        assertTrue(config.credentialService().list().isEmpty());
    }

    @Test
    @DisplayName("Should default the transport to a real JdkHttpTransport whens none is injected")
    void defaultsToJdkTransport() {
        final var config = DeclarativeHttpConfig.builder()
                .keyProvider(() -> key)
                .build();

        assertInstanceOf(JdkHttpTransport.class, config.transport());
        assertNotNull(config.engine());
    }

    @Test
    @DisplayName("Should resolve a custom pagination mode through registerPagination")
    void resolvesCustomPaginationThroughRegisterPagination() {
        final var transport = new FakeTransport(HttpResult.of(200,
                "{\"data\":{\"items\":[{\"id\":1,\"name\":\"SOK\"}]}}"));
        final var config = DeclarativeHttpConfig.builder()
                .transport(transport)
                .keyProvider(() -> key)
                .registerPagination("marker", (spec, evaluator, jsonMapper) -> new MarkerStrategy())
                .build();
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data.items"))),
                new PaginationSpec("marker", 10, "data.items", null, null, true, null),
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = DeclarativeHttp.create(config).execute(spec,
                new RequestContext("contact.list", Map.of()));

        assertEquals(1, records.size());
        assertEquals("SOK", records.get(0).json().get("name"));
    }

    private static final class MarkerStrategy implements PaginationStrategy {

        @Override
        public boolean shouldPaginate(final RequestPlan plan,
                                      final HttpResult last) {
            return false;
        }

        @Override
        public HttpRequest nextRequest(final RequestPlan plan,
                                       final HttpResult last) {
            return null;
        }

        @Override
        public List<OutputRecord> collect(final RequestPlan plan,
                                          final HttpResult last,
                                          final List<OutputRecord> page) {
            return page;
        }
    }
}
