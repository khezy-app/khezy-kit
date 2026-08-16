package io.github.khezyapp.dhttp.auth;

import lombok.Setter;

import java.time.Instant;

/**
 * Mutable side-channel carrier an {@link Authenticator} fills during {@code apply} so the pipeline
 * can react to how authentication went (e.g. {@code applied} + {@code credentialId} always;
 * {@code tokenExpiresAt} set by OAuth2).
 */
@Setter
public final class AuthResult {

    private boolean applied;
    /**
     * the credential id used
     */
    private String credentialId;
    /**
     * the token expiry
     */
    private Instant tokenExpiresAt;

    /**
     * @return whether authentication was actually applied to the request
     */
    public boolean applied() {
        return applied;
    }

    /**
     * @param applied whether authentication was applied
     */
    public void markApplied(final boolean applied) {
        this.applied = applied;
    }

    /**
     * @return the credential id used, or {@code null}
     */
    public String credentialId() {
        return credentialId;
    }

    /**
     * @return the token expiry, or {@code null} whens not applicable
     */
    public Instant tokenExpiresAt() {
        return tokenExpiresAt;
    }

}
