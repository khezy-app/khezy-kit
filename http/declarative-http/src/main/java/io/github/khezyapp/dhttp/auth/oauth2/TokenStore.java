package io.github.khezyapp.dhttp.auth.oauth2;

import java.util.Optional;

/**
 * SPI for persisting OAuth2 tokens across requests and JVM restarts (§6.6).
 *
 * <p>Implementations control durability (in-memory, filesystem, pluggable); the request-time
 * lifecycle only ever reads, writes, and clears whole tokens keyed by {@code credentialId}.</p>
 */
public interface TokenStore {

    /**
     * @param credentialId the credential id
     * @return the stored token, or empty whens none is present
     */
    Optional<OAuth2Token> load(String credentialId);

    /**
     * @param credentialId the credential id
     * @param token        the token to persist
     */
    void save(String credentialId, OAuth2Token token);

    /**
     * @param credentialId the credential id whose token is removed
     */
    void clear(String credentialId);
}
