package io.github.khezyapp.dhttp.auth.credential.type;

import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Grant;

import java.util.Map;
import java.util.Objects;

/**
 * Typed configuration for an {@code oauth2} credential (§6.2).
 *
 * @param clientId       the OAuth2 client id
 * @param clientSecret   the OAuth2 client secret
 * @param tokenUrl       the token endpoint URL
 * @param authorizationUrl the authorization endpoint URL (authorization-code flow)
 * @param redirectUri    the configured redirect URI
 * @param scope          the requested scopes
 * @param grantType      the grant flow
 * @param extraBodyParams additional body parameters for the token request
 */
public record OAuth2Credentials(String clientId,
                                String clientSecret,
                                String tokenUrl,
                                String authorizationUrl,
                                String redirectUri,
                                String scope,
                                OAuth2Grant grantType,
                                Map<String, Object> extraBodyParams) {

    public OAuth2Credentials {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(tokenUrl, "tokenUrl");
        Objects.requireNonNull(grantType, "grantType");
        extraBodyParams = extraBodyParams == null ? Map.of() : Map.copyOf(extraBodyParams);
    }
}
