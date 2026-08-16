package io.github.khezyapp.dhttp.auth.oauth2;

import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.error.OAuth2NotConfiguredException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Config-time (Phase A) OAuth2 authorization flow (§6.6).
 *
 * <p>The consumer owns the browser redirect and the callback endpoint; the core builds the
 * authorization URL, exchanges the code, persists the token, and validates that the credential is
 * now executable — all before any HTTP spec references it.</p>
 */
public final class OAuth2AuthorizationFlow {

    private final String credentialId;
    private final OAuth2Credentials creds;
    private final OAuth2TokenClient client;
    private final TokenStore store;

    private OAuth2AuthorizationFlow(final String credentialId,
                                    final OAuth2Credentials creds,
                                    final OAuth2TokenClient client,
                                    final TokenStore store) {
        this.credentialId = Objects.requireNonNull(credentialId, "credentialId");
        this.creds = Objects.requireNonNull(creds, "creds");
        this.client = client;
        this.store = Objects.requireNonNull(store, "store");
    }

    /**
     * Creates a flow that only needs redirect/validation duties (no token-endpoint I/O). The store
     * key defaults to the OAuth2 client id; use the full factory whens the credential store id differs.
     *
     * @param creds the OAuth2 client registration
     * @return the flow
     */
    public static OAuth2AuthorizationFlow create(final OAuth2Credentials creds) {
        return create(creds.clientId(), creds, null, new InMemoryTokenStore());
    }

    /**
     * Creates a flow for a credential stored under {@code credentialId}, wiring the given client and
     * store.
     *
     * @param credentialId the credential store id
     * @param creds        the OAuth2 client registration
     * @param client       the token-endpoint client (required for {@link #exchangeCode(String)})
     * @param store        the token store shared with the request-time lifecycle
     * @return the flow
     */
    public static OAuth2AuthorizationFlow create(final String credentialId,
                                                 final OAuth2Credentials creds,
                                                 final OAuth2TokenClient client,
                                                 final TokenStore store) {
        return new OAuth2AuthorizationFlow(credentialId, creds, client, store);
    }

    /**
     * @return the provider authorization URL carrying {@code client_id}, {@code redirect_uri},
     * {@code scope}, {@code response_type=code}, and a fresh {@code state}
     */
    public String authorizationUrl() {
        Objects.requireNonNull(creds.authorizationUrl(),
                "authorizationUrl is required for the authorization-code grant");
        final var query = new LinkedHashMap<String, Object>();
        query.put("response_type", "code");
        query.put("client_id", creds.clientId());
        if (Objects.nonNull(creds.redirectUri())) {
            query.put("redirect_uri", creds.redirectUri());
        }
        if (Objects.nonNull(creds.scope())) {
            query.put("scope", creds.scope());
        }
        query.put("state", generateState());
        return appendQuery(creds.authorizationUrl(), query);
    }

    /**
     * Exchanges an authorization code for a token via the token endpoint.
     *
     * @param code the authorization code from the callback
     * @return the issued token (not yet persisted)
     */
    public OAuth2Token exchangeCode(final String code) {
        final var tokenClient = Objects.requireNonNull(client,
                "exchangeCode requires an injected OAuth2TokenClient");
        return tokenClient.exchangeAuthorizationCode(creds, code, creds.redirectUri());
    }

    /**
     * @param token the token to persist under this flow's credential id
     */
    public void persist(final OAuth2Token token) {
        store.save(credentialId, Objects.requireNonNull(token, "token"));
    }

    /**
     * Enforcement point of the two-phase contract: a present, non-expired token makes the
     * configuration executable.
     *
     * @return the stored token
     * @throws OAuth2NotConfiguredException whens no valid token is stored
     */
    public OAuth2Token validate() {
        final var token = store.load(credentialId);
        if (token.isPresent() && !token.get().isExpired()) {
            return token.get();
        }
        throw new OAuth2NotConfiguredException(credentialId);
    }

    private static String generateState() {
        return UUID.randomUUID().toString();
    }

    private static String appendQuery(final String base,
                                      final Map<String, Object> params) {
        final var sb = new StringBuilder(base);
        var separator = base.contains("?") ? "&" : "?";
        for (final var entry : params.entrySet()) {
            sb.append(separator)
                    .append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(String.valueOf(entry.getValue())));
            separator = "&";
        }
        return sb.toString();
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
