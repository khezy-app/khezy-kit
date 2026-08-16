package io.github.khezyapp.dhttp.auth.oauth2;

import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import tools.jackson.core.type.TypeReference;

/**
 * Performs raw token-endpoint calls (code exchange, client-credentials, password, refresh) by
 * reusing {@link HttpTransport}/{@link HttpRequest} so SSRF guards and TLS policy apply uniformly
 * (§6.6). JEXL-free.
 */
public final class OAuth2TokenClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final HttpTransport transport;
    private final JsonMapper jsonMapper;

    public OAuth2TokenClient(final HttpTransport transport,
                             final JsonMapper jsonMapper) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    /**
     * Exchanges an authorization code for a token pair.
     *
     * @param creds       the OAuth2 client registration
     * @param code        the authorization code from the callback
     * @param redirectUri the redirect URI used in the original consent request
     * @return the issued token
     */
    public OAuth2Token exchangeAuthorizationCode(final OAuth2Credentials creds,
                                                 final String code,
                                                 final String redirectUri) {
        final var params = new LinkedHashMap<String, String>();
        params.put("grant_type", "authorization_code");
        params.put("code", Objects.requireNonNull(code, "code"));
        params.put("redirect_uri", redirectUri);
        return requestToken(creds, params);
    }

    /**
     * Acquires a token with the client-credentials grant.
     *
     * @param creds the OAuth2 client registration
     * @return the issued token
     */
    public OAuth2Token clientCredentials(final OAuth2Credentials creds) {
        final var params = new LinkedHashMap<String, String>();
        params.put("grant_type", "client_credentials");
        return requestToken(creds, params);
    }

    /**
     * Acquires a token with the resource-owner-password grant.
     *
     * @param creds    the OAuth2 client registration
     * @param username the resource owner username
     * @param password the resource owner password
     * @return the issued token
     */
    public OAuth2Token password(final OAuth2Credentials creds,
                                final String username,
                                final String password) {
        final var params = new LinkedHashMap<String, String>();
        params.put("grant_type", "password");
        params.put("username", Objects.requireNonNull(username, "username"));
        params.put("password", Objects.requireNonNull(password, "password"));
        return requestToken(creds, params);
    }

    /**
     * Refreshes an expired token pair.
     *
     * @param creds        the OAuth2 client registration
     * @param refreshToken the refresh token
     * @return the refreshed token pair
     */
    public OAuth2Token refresh(final OAuth2Credentials creds,
                               final String refreshToken) {
        final var params = new LinkedHashMap<String, String>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", Objects.requireNonNull(refreshToken, "refreshToken"));
        return requestToken(creds, params);
    }

    private OAuth2Token requestToken(final OAuth2Credentials creds,
                                     final Map<String, String> grantParams) {
        final var form = new LinkedHashMap<String, String>();
        form.putAll(grantParams);
        form.put("client_id", creds.clientId());
        if (Objects.nonNull(creds.clientSecret())) {
            form.put("client_secret", creds.clientSecret());
        }
        if (Objects.nonNull(creds.scope())) {
            form.put("scope", creds.scope());
        }
        for (final var entry : creds.extraBodyParams().entrySet()) {
            form.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        final var request = HttpRequest.builder()
                .url(creds.tokenUrl())
                .method(HttpMethod.POST)
                .header("Accept", "application/json")
                .body(new Body.UrlEncodedBody(form))
                .build();
        final var result = transport.send(request);
        if (!result.ok()) {
            throw new HttpApiException(result.status(), null, -1,
                    "OAuth2 token request failed with HTTP status " + result.status());
        }
        return parseToken(result.bodyString());
    }

    private OAuth2Token parseToken(final String json) {
        final Map<String, Object> data;
        try {
            data = jsonMapper.read(json, MAP_TYPE);
        } catch (final RuntimeException e) {
            throw new HttpApiException(HttpApiException.NO_STATUS, null, -1,
                    "OAuth2 token response could not be parsed", e);
        }
        final var accessToken = data.get("access_token");
        if (Objects.isNull(accessToken)) {
            throw new HttpApiException(HttpApiException.NO_STATUS, null, -1,
                    "OAuth2 token response is missing access_token");
        }
        final var refreshToken = data.get("refresh_token");
        final var expiresIn = numberValue(data.get("expires_in"));
        final var expiresAt = Objects.isNull(expiresIn) ? null : Instant.now().plusSeconds(expiresIn);
        final var scope = data.get("scope");
        return new OAuth2Token(String.valueOf(accessToken),
                Objects.isNull(refreshToken) ? null : String.valueOf(refreshToken),
                Objects.isNull(expiresIn) ? 0L : expiresIn,
                Objects.isNull(expiresAt) ? null : expiresAt,
                Objects.isNull(scope) ? null : String.valueOf(scope));
    }

    private static Long numberValue(final Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        return null;
    }
}
