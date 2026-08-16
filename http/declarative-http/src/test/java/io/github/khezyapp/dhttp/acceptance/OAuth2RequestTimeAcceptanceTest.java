package io.github.khezyapp.dhttp.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.auth.oauth2.InMemoryTokenStore;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Grant;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Token;
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
import io.github.khezyapp.dhttp.transport.Auth;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * §9 acceptance item 7: the OAuth2 request-time lifecycle through the assembled facade — (a) a warm
 * token is reused with no token-endpoint call; (b) an expired token triggers a {@code refresh_token}
 * grant; (c) a 401 on a warm token triggers a single refresh + retry; (d) concurrent requests share
 * one refresh (single-flight lock). Token-endpoint and protected calls share the same transport and
 * are distinguished by host.
 */
class OAuth2RequestTimeAcceptanceTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    private static final String TOKEN_JSON = "{\"access_token\":\"at-new\","
            + "\"refresh_token\":\"rt-new\",\"expires_in\":3600}";
    private static final String API_JSON = "{\"data\":[{\"id\":1,\"name\":\"SOK\"}]}";

    @Test
    @DisplayName("Item 7a: a warm token is reused with no token-endpoint call")
    void reusesWarmToken() {
        final var transport = new RoutingTransport(List.of(HttpResult.of(200, API_JSON)),
                List.of(HttpResult.of(200, TOKEN_JSON)));
        final var tokenStore = new InMemoryTokenStore();
        final var http = facade(transport, tokenStore);
        final var id = oauth2Credential(http);
        tokenStore.save(id, warmToken("at-warm", "rt-warm"));

        final var records = http.execute(oauth2Spec(id), ctx());

        assertEquals(0, transport.tokenCalls());
        assertEquals(1, transport.apiCalls());
        assertEquals("at-warm", bearer(transport.lastApiRequest()));
        assertEquals(1, records.size());
    }

    @Test
    @DisplayName("Item 7b: an expired token triggers a refresh_token grant")
    void expiredTokenRefreshes() {
        final var transport = new RoutingTransport(List.of(HttpResult.of(200, API_JSON)),
                List.of(HttpResult.of(200, TOKEN_JSON)));
        final var tokenStore = new InMemoryTokenStore();
        final var http = facade(transport, tokenStore);
        final var id = oauth2Credential(http);
        tokenStore.save(id, new OAuth2Token("at-old", "rt-1", 0,
                Instant.now().minusSeconds(30), null));

        final var records = http.execute(oauth2Spec(id), ctx());

        assertEquals(1, transport.tokenCalls());
        assertEquals(1, transport.apiCalls());
        final var form = assertInstanceOf(Body.UrlEncodedBody.class,
                transport.lastTokenRequest().body()).body();
        assertTrue(form.contains("grant_type=refresh_token"));
        assertEquals("at-new", bearer(transport.lastApiRequest()));
        assertEquals(1, records.size());
    }

    @Test
    @DisplayName("Item 7c: a 401 on a warm token triggers a single refresh and one retry")
    void retriesOnceOn401() {
        final var transport = new RoutingTransport(
                List.of(HttpResult.of(401, "unauthorized"), HttpResult.of(200, API_JSON)),
                List.of(HttpResult.of(200, TOKEN_JSON)));
        final var tokenStore = new InMemoryTokenStore();
        final var http = facade(transport, tokenStore);
        final var id = oauth2Credential(http);
        tokenStore.save(id, warmToken("at-warm", "rt-1"));

        final var records = http.execute(oauth2Spec(id), ctx());

        assertEquals(1, transport.tokenCalls());
        assertEquals(2, transport.apiCalls());
        assertEquals("at-new", bearer(transport.lastApiRequest()));
        assertEquals(1, records.size());
    }

    @Test
    @DisplayName("Item 7d: concurrent requests share exactly one refresh")
    void concurrentRequestsShareOneRefresh() throws Exception {
        final var transport = new RoutingTransport(List.of(HttpResult.of(200, API_JSON)),
                List.of(HttpResult.of(200, TOKEN_JSON)));
        final var tokenStore = new InMemoryTokenStore();
        final var http = facade(transport, tokenStore);
        final var id = oauth2Credential(http);
        tokenStore.save(id, new OAuth2Token("at-old", "rt-1", 0,
                Instant.now().minusSeconds(30), null));
        final var spec = oauth2Spec(id);
        final var ctx = ctx();
        final var start = new CountDownLatch(1);
        final var executor = Executors.newFixedThreadPool(8);
        final var futures = new java.util.ArrayList<Future<Integer>>();
        for (int i = 0; i < 8; i++) {
            futures.add(executor.submit(() -> {
                await(start);
                return http.execute(spec, ctx).size();
            }));
        }
        start.countDown();
        for (final Future<Integer> future : futures) {
            assertEquals(1, future.get());
        }
        executor.shutdownNow();

        assertEquals(1, transport.tokenCalls());
        assertEquals(8, transport.apiCalls());
    }

    @Test
    @DisplayName("Item 7 + redaction: a repeated 401 surfaces an HttpApiException without secrets")
    void repeated401ThrowsRedacted() {
        final var transport = new RoutingTransport(
                List.of(HttpResult.of(401, "unauthorized"), HttpResult.of(401, "unauthorized")),
                List.of(HttpResult.of(200, TOKEN_JSON)));
        final var tokenStore = new InMemoryTokenStore();
        final var http = facade(transport, tokenStore);
        final var id = oauth2Credential(http);
        tokenStore.save(id, warmToken("at-warm", "rt-1"));

        final var e = assertThrows(HttpApiException.class,
                () -> http.execute(oauth2Spec(id), ctx()));

        assertEquals(401, e.getStatus());
        assertFalse(e.getMessage().contains("at-warm"));
        assertFalse(e.getMessage().contains("s3cr3t"));
        assertFalse(e.getMessage().contains("at-new"));
    }

    private static void await(final CountDownLatch start) {
        try {
            start.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for the start latch", e);
        }
    }

    private static String oauth2Credential(final DeclarativeHttp http) {
        return http.credentialService().create("oauth2", creds());
    }

    private static DeclarativeHttp facade(final HttpTransport transport,
                                          final InMemoryTokenStore tokenStore) {
        final var config = DeclarativeHttpConfig.builder()
                .transport(transport)
                .tokenStore(tokenStore)
                .keyProvider(OAuth2RequestTimeAcceptanceTest::newKey)
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

    private static OAuth2Token warmToken(final String accessToken,
                                         final String refreshToken) {
        return new OAuth2Token(accessToken, refreshToken, 3600,
                Instant.now().plusSeconds(3600), null);
    }

    private static OAuth2Credentials creds() {
        return new OAuth2Credentials("client-1", "s3cr3t", "https://auth.example.com/token",
                null, null, "contacts.read", OAuth2Grant.CLIENT_CREDENTIALS, Map.of());
    }

    private static RequestContext ctx() {
        return new RequestContext("contact.list", null, Map.of(),
                JSON.toMap(creds()), Map.of(), null);
    }

    private static HttpRequestSpec oauth2Spec(final String id) {
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data"))), null, List.of()));
        return new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), CredentialRef.of("oauth2", id), null,
                SecurityPolicy.defaults());
    }

    private static String bearer(final HttpRequest request) {
        return assertInstanceOf(Auth.BearerAuth.class, request.auth()).token();
    }

    private static final class RoutingTransport implements HttpTransport {

        private static final String TOKEN_HOST = "auth.example.com";

        private final List<HttpResult> apiResponses;
        private final List<HttpResult> tokenResponses;
        private final AtomicInteger apiCalls = new AtomicInteger();
        private final AtomicInteger tokenCalls = new AtomicInteger();
        private final List<HttpRequest> apiRequests = new CopyOnWriteArrayList<>();
        private final List<HttpRequest> tokenRequests = new CopyOnWriteArrayList<>();

        private RoutingTransport(final List<HttpResult> apiResponses,
                                 final List<HttpResult> tokenResponses) {
            this.apiResponses = apiResponses;
            this.tokenResponses = tokenResponses;
        }

        @Override
        public HttpResult send(final HttpRequest request) throws HttpApiException {
            if (request.url().contains(TOKEN_HOST)) {
                tokenRequests.add(request);
                return tokenResponses.get(Math.min(tokenCalls.getAndIncrement(),
                        tokenResponses.size() - 1));
            }
            apiRequests.add(request);
            return apiResponses.get(Math.min(apiCalls.getAndIncrement(), apiResponses.size() - 1));
        }

        private int apiCalls() {
            return apiCalls.get();
        }

        private int tokenCalls() {
            return tokenCalls.get();
        }

        private HttpRequest lastApiRequest() {
            return apiRequests.get(apiRequests.size() - 1);
        }

        private HttpRequest lastTokenRequest() {
            return tokenRequests.get(tokenRequests.size() - 1);
        }
    }
}
