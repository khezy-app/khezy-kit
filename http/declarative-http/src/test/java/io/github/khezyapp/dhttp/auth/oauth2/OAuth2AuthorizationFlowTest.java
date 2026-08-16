package io.github.khezyapp.dhttp.auth.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.error.OAuth2NotConfiguredException;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuth2AuthorizationFlowTest {

    private static final String TOKEN_JSON = "{\"access_token\":\"at-1\","
            + "\"refresh_token\":\"rt-1\",\"expires_in\":3600,\"scope\":\"contacts.read\"}";

    @Test
    @DisplayName("Should build an authorization URL carrying client_id, redirect_uri, scope and state")
    void authorizationUrlCarriesConsentParams() {
        final var flow = OAuth2AuthorizationFlow.create(authCodeCreds());

        final var url = flow.authorizationUrl();

        final var query = queryParams(url);
        assertEquals("code", query.get("response_type"));
        assertEquals("client-1", query.get("client_id"));
        assertEquals("https://app.example.com/callback", query.get("redirect_uri"));
        assertEquals("contacts.read", query.get("scope"));
        assertNotNull(query.get("state"));
        assertNotEquals("", query.get("state"));
    }

    @Test
    @DisplayName("Should exchange a code via the token endpoint and persist the token")
    void exchangeCodePersistsToken() {
        final var transport = new FakeTransport(HttpResult.of(200, TOKEN_JSON));
        final var client = new OAuth2TokenClient(transport, JacksonJsonMapper.INSTANCE);
        final var store = new InMemoryTokenStore();
        final var flow = OAuth2AuthorizationFlow.create("google-sheets", authCodeCreds(), client, store);

        final var token = flow.exchangeCode("auth-code-1");
        flow.persist(token);

        assertEquals(1, transport.callCount());
        final var request = transport.lastRequest();
        final var body = assertInstanceOf(Body.UrlEncodedBody.class, request.body());
        final var form = body.body();
        assertTrue(form.contains("grant_type=authorization_code"));
        assertTrue(form.contains("code=auth-code-1"));
        assertTrue(form.contains("client_id=client-1"));

        assertEquals(token, store.load("google-sheets").orElseThrow());
        assertEquals(token, flow.validate());
    }

    @Test
    @DisplayName("Should validate a stored non-expired token")
    void validatePassesWithValidStoredToken() {
        final var store = new InMemoryTokenStore();
        final var flow = OAuth2AuthorizationFlow.create("google-sheets", authCodeCreds(), null, store);
        store.save("google-sheets",
                new OAuth2Token("at-1", "rt-1", 3600, Instant.now().plusSeconds(3600), "contacts.read"));

        final var token = flow.validate();

        assertEquals("at-1", token.accessToken());
    }

    @Test
    @DisplayName("Should throw OAuth2NotConfiguredException whens no token is stored, sending nothing")
    void validateThrowsWithoutTokenAndSendsNothing() {
        final var transport = new FakeTransport();
        final var flow = OAuth2AuthorizationFlow.create("google-sheets", authCodeCreds(),
                new OAuth2TokenClient(transport, JacksonJsonMapper.INSTANCE), new InMemoryTokenStore());

        assertThrows(OAuth2NotConfiguredException.class, flow::validate);

        assertEquals(0, transport.callCount());
    }

    @Test
    @DisplayName("Should throw OAuth2NotConfiguredException whens the stored token is expired")
    void validateThrowsOnExpiredToken() {
        final var store = new InMemoryTokenStore();
        final var flow = OAuth2AuthorizationFlow.create("google-sheets", authCodeCreds(), null, store);
        store.save("google-sheets",
                new OAuth2Token("at-old", "rt-old", 0, Instant.now().minusSeconds(60), null));

        assertThrows(OAuth2NotConfiguredException.class, flow::validate);
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

    private static OAuth2Credentials authCodeCreds() {
        return new OAuth2Credentials("client-1", "s3cr3t",
                "https://auth.example.com/token", "https://auth.example.com/authorize",
                "https://app.example.com/callback", "contacts.read",
                OAuth2Grant.AUTHORIZATION_CODE, Map.of());
    }
}
