package io.github.khezyapp.dhttp.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.auth.oauth2.InMemoryTokenStore;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2AuthorizationFlow;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Grant;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Token;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2TokenClient;
import io.github.khezyapp.dhttp.auth.oauth2.TokenStore;
import io.github.khezyapp.dhttp.config.DeclarativeHttp;
import io.github.khezyapp.dhttp.config.DeclarativeHttpConfig;
import io.github.khezyapp.dhttp.error.OAuth2NotConfiguredException;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.spec.CredentialRef;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * §9 acceptance item 6: the OAuth2 config-time contract (two phases). Phase A — the authorization
 * URL carries the consent params, and a consumer-style callback handler exchanges the code and
 * persists the token. Phase B — {@code engine.validate} passes with a valid stored token and throws
 * {@link OAuth2NotConfiguredException} without one, sending no HTTP request.
 */
class OAuth2ConfigTimeAcceptanceTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    private static final String TOKEN_JSON = "{\"access_token\":\"at-1\","
            + "\"refresh_token\":\"rt-1\",\"expires_in\":3600,\"scope\":\"sheets.read\"}";

    @Test
    @DisplayName("Item 6a: authorizationUrl carries client_id, redirect_uri, scope and state")
    void authorizationUrlCarriesConsentParams() {
        final var flow = OAuth2AuthorizationFlow.create(oauth2Config());

        final var query = queryParams(flow.authorizationUrl());

        assertEquals("code", query.get("response_type"));
        assertEquals("client-1", query.get("client_id"));
        assertEquals("https://app.example.com/callback", query.get("redirect_uri"));
        assertEquals("sheets.read", query.get("scope"));
        assertNotNull(query.get("state"));
        assertFalse(query.get("state").isBlank());
    }

    @Test
    @DisplayName("Item 6b: a consumer-style callback exchanges the code and persists the token")
    void exchangeCodePersistsTokenAndValidates() {
        final var transport = new FakeTransport(HttpResult.of(200, TOKEN_JSON));
        final var tokenStore = new InMemoryTokenStore();
        final var http = facade(transport, tokenStore);
        final var id = http.credentialService().create("oauth2", oauth2Config());
        final var creds = http.credentialService().get(id, OAuth2Credentials.class)
                .orElseThrow().data();
        final var flow = OAuth2AuthorizationFlow.create(id, creds,
                new OAuth2TokenClient(transport, JSON), tokenStore);

        final var token = flow.exchangeCode("auth-code-1");
        flow.persist(token);

        assertEquals(token, tokenStore.load(id).orElseThrow());
        http.validate(oauth2Spec(id));
        assertEquals(1, transport.callCount());
    }

    @Test
    @DisplayName("Item 6c: validate passes with a valid stored token, sending no request")
    void validatePassesWithStoredToken() {
        final var transport = new FakeTransport();
        final var tokenStore = new InMemoryTokenStore();
        final var http = facade(transport, tokenStore);
        final var id = http.credentialService().create("oauth2", oauth2Config());
        tokenStore.save(id, new OAuth2Token("at-1", "rt-1", 3600,
                Instant.now().plusSeconds(3600), null));

        http.validate(oauth2Spec(id));

        assertEquals(0, transport.callCount());
    }

    @Test
    @DisplayName("Item 6c: validate throws OAuth2NotConfiguredException without a token, sending nothing")
    void validateThrowsWithoutToken() {
        final var transport = new FakeTransport();
        final var http = facade(transport, new InMemoryTokenStore());
        final var id = http.credentialService().create("oauth2", oauth2Config());

        assertThrows(OAuth2NotConfiguredException.class, () -> http.validate(oauth2Spec(id)));

        assertEquals(0, transport.callCount());
    }

    private static DeclarativeHttp facade(final HttpTransport transport,
                                          final TokenStore tokenStore) {
        final var config = DeclarativeHttpConfig.builder()
                .transport(transport)
                .tokenStore(tokenStore)
                .keyProvider(OAuth2ConfigTimeAcceptanceTest::newKey)
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

    private static Map<String, String> queryParams(final String url) {
        final var params = new LinkedHashMap<String, String>();
        final var query = url.substring(url.indexOf('?') + 1);
        for (final String pair : query.split("&")) {
            final var kv = pair.split("=", 2);
            params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    private static OAuth2Credentials oauth2Config() {
        return new OAuth2Credentials("client-1", "s3cr3t", "https://auth.example.com/token",
                "https://auth.example.com/authorize", "https://app.example.com/callback",
                "sheets.read", OAuth2Grant.AUTHORIZATION_CODE, Map.of());
    }

    private static HttpRequestSpec oauth2Spec(final String id) {
        final var shape = new RequestShape(HttpMethod.GET, "/values/A1:B2", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("sheets.values.get",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        return new HttpRequestSpec("https://sheets.googleapis.com/v4", Map.of(), 30000L, false,
                List.of(operation), CredentialRef.of("oauth2", id), null,
                SecurityPolicy.defaults());
    }
}
