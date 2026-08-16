package io.github.khezyapp.dhttp.auth.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.auth.credential.DecryptedCredential;
import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.error.OAuth2NotConfiguredException;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.transport.Auth;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuth2RequestAuthenticatorTest {

    private static final String TOKEN_JSON = "{\"access_token\":\"at-new\","
            + "\"refresh_token\":\"rt-new\",\"expires_in\":3600}";

    private FakeTransport tokenTransport;
    private InMemoryTokenStore store;
    private OAuth2RequestAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        tokenTransport = new FakeTransport(HttpResult.of(200, TOKEN_JSON));
        store = new InMemoryTokenStore();
        final var client = new OAuth2TokenClient(tokenTransport, JacksonJsonMapper.INSTANCE);
        authenticator = new OAuth2RequestAuthenticator(store, client);
    }

    @Test
    @DisplayName("Should reuse a warm token with no token-endpoint call")
    void reusesWarmToken() {
        store.save("cc-1", new OAuth2Token("at-warm", "rt-warm", 3600,
                Instant.now().plusSeconds(3600), "contacts.read"));

        final var token = authenticator.tokenFor("cc-1", creds(), OAuth2Grant.CLIENT_CREDENTIALS);

        assertEquals("at-warm", token.accessToken());
        assertEquals(0, tokenTransport.callCount());
    }

    @Test
    @DisplayName("Should trigger a refresh_token grant for an expired token")
    void expiredTokenRefreshes() {
        store.save("cc-1", new OAuth2Token("at-old", "rt-old", 0,
                Instant.now().minusSeconds(30), null));

        final var token = authenticator.tokenFor("cc-1", creds(), OAuth2Grant.CLIENT_CREDENTIALS);

        assertEquals("at-new", token.accessToken());
        assertEquals(1, tokenTransport.callCount());
        assertTrue(formOf(tokenTransport.lastRequest()).contains("grant_type=refresh_token"));
        assertEquals("at-new", store.load("cc-1").orElseThrow().accessToken());
    }

    @Test
    @DisplayName("Should acquire a fresh token on first request for client-credentials")
    void acquiresFreshTokenWhenAbsent() {
        final var token = authenticator.tokenFor("cc-1", creds(), OAuth2Grant.CLIENT_CREDENTIALS);

        assertEquals("at-new", token.accessToken());
        assertEquals(1, tokenTransport.callCount());
        assertTrue(formOf(tokenTransport.lastRequest()).startsWith("grant_type=client_credentials"));
    }

    @Test
    @DisplayName("Should throw OAuth2NotConfiguredException for an authorization-code grant with no token")
    void authCodeGrantWithoutTokenThrows() {
        assertThrows(OAuth2NotConfiguredException.class,
                () -> authenticator.tokenFor("cc-1", authCodeCreds(), OAuth2Grant.AUTHORIZATION_CODE));
        assertEquals(0, tokenTransport.callCount());
    }

    @Test
    @DisplayName("Should inject a Bearer token into the request")
    void authenticateInjectsBearer() {
        store.save("cc-1", new OAuth2Token("at-warm", "rt-warm", 3600,
                Instant.now().plusSeconds(3600), null));

        final var result = authenticator.authenticate(credential(), protectedRequest());

        final var auth = assertInstanceOf(Auth.BearerAuth.class, result.auth());
        assertEquals("at-warm", auth.token());
        assertEquals(0, tokenTransport.callCount());
    }

    @Test
    @DisplayName("Should refresh once and replay on 401")
    void retryOn401RefreshesAndReplays() {
        store.save("cc-1", new OAuth2Token("at-warm", "rt-1", 3600,
                Instant.now().plusSeconds(3600), null));
        final var protectedTransport = new ScriptedTransport(
                HttpResult.of(401, "unauthorized"), HttpResult.of(200, "{\"ok\":true}"));

        final var result = authenticator.retryOn401(credential(), protectedRequest(), protectedTransport);

        assertEquals(200, result.status());
        assertEquals(2, protectedTransport.callCount());
        assertEquals(1, tokenTransport.callCount());
        final var retried = assertInstanceOf(Auth.BearerAuth.class, protectedTransport.lastRequest().auth());
        assertEquals("at-new", retried.token());
        assertEquals("at-new", store.load("cc-1").orElseThrow().accessToken());
    }

    @Test
    @DisplayName("Should throw HttpApiException(401) whens the retry also returns 401")
    void retryOn401ThrowsOnRepeated401() {
        store.save("cc-1", new OAuth2Token("at-warm", "rt-1", 3600,
                Instant.now().plusSeconds(3600), null));
        final var protectedTransport = new ScriptedTransport(
                HttpResult.of(401, "unauthorized"), HttpResult.of(401, "unauthorized"));

        final var e = assertThrows(HttpApiException.class,
                () -> authenticator.retryOn401(credential(), protectedRequest(), protectedTransport));

        assertEquals(401, e.getStatus());
        assertEquals(2, protectedTransport.callCount());
    }

    @Test
    @DisplayName("Should share exactly one refresh across concurrent requests")
    void concurrentRequestsShareOneRefresh() throws Exception {
        final var threads = 8;
        final var start = new CountDownLatch(1);
        final var executor = Executors.newFixedThreadPool(threads);
        final var futures = new ArrayList<Future<OAuth2Token>>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> callTokenFor(start, authenticator, creds())));
        }
        start.countDown();
        for (final Future<OAuth2Token> future : futures) {
            assertEquals("at-new", future.get().accessToken());
        }
        executor.shutdownNow();

        assertEquals(1, tokenTransport.callCount());
    }

    private static OAuth2Token callTokenFor(final CountDownLatch start,
                                            final OAuth2RequestAuthenticator authenticator,
                                            final OAuth2Credentials creds) {
        try {
            start.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for the start latch", e);
        }
        return authenticator.tokenFor("cc-1", creds, OAuth2Grant.CLIENT_CREDENTIALS);
    }

    private static DecryptedCredential<?> credential() {
        return new DecryptedCredential<>("cc-1", "oauth2",
                JacksonJsonMapper.INSTANCE.toMap(creds()), null);
    }

    private static OAuth2Credentials creds() {
        return new OAuth2Credentials("client-1", "s3cr3t", "https://auth.example.com/token",
                null, null, "contacts.read", OAuth2Grant.CLIENT_CREDENTIALS, Map.of());
    }

    private static OAuth2Credentials authCodeCreds() {
        return new OAuth2Credentials("client-1", "s3cr3t", "https://auth.example.com/token",
                "https://auth.example.com/authorize", "https://app.example.com/callback",
                "contacts.read", OAuth2Grant.AUTHORIZATION_CODE, Map.of());
    }

    private static HttpRequest protectedRequest() {
        return HttpRequest.builder()
                .url("https://api.example.com/v1/contacts")
                .method(HttpMethod.GET)
                .build();
    }

    private static String formOf(final HttpRequest request) {
        return assertInstanceOf(Body.UrlEncodedBody.class, request.body()).body();
    }

    private static final class ScriptedTransport implements HttpTransport {

        private final List<HttpResult> responses;
        private int callCount;
        private int index;
        private HttpRequest lastRequest;

        private ScriptedTransport(final HttpResult... responses) {
            this.responses = List.of(responses);
        }

        @Override
        public HttpResult send(final HttpRequest request) throws HttpApiException {
            lastRequest = request;
            callCount++;
            return responses.get(Math.min(index++, responses.size() - 1));
        }

        private int callCount() {
            return callCount;
        }

        private HttpRequest lastRequest() {
            return lastRequest;
        }
    }
}
