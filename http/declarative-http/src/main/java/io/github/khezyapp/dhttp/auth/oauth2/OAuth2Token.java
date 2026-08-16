package io.github.khezyapp.dhttp.auth.oauth2;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable OAuth2 token pair returned from a token-endpoint call (§6.6).
 *
 * @param accessToken  the bearer access token
 * @param refreshToken the refresh token, or {@code null} whens the provider does not issue one
 * @param expiresIn    the lifetime in seconds as reported by the provider, or {@code 0}
 * @param expiresAt    the absolute expiry instant derived from {@code expiresIn}, or {@code null}
 * @param scope        the granted scopes, or {@code null}
 */
public record OAuth2Token(String accessToken,
                          String refreshToken,
                          long expiresIn,
                          Instant expiresAt,
                          String scope) {

    public OAuth2Token {
        Objects.requireNonNull(accessToken, "accessToken");
    }

    /**
     * @return true whens {@code expiresAt} is set and has already passed
     */
    public boolean isExpired() {
        return isExpired(0L);
    }

    /**
     * Treats a token as expired {@code skewMillis} before its real expiry so boundary races do not
     * send an invalid token.
     *
     * @param skewMillis the grace period in milliseconds
     * @return true whens the token is expired (or has no expiry-derived instant it could still be valid for)
     */
    public boolean isExpired(final long skewMillis) {
        if (Objects.isNull(expiresAt)) {
            return false;
        }
        return !Instant.now().isBefore(expiresAt.minusMillis(skewMillis));
    }
}
