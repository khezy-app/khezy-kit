package io.github.khezyapp.dhttp.error;

import lombok.Getter;

import java.util.Objects;

/**
 * Thrown at configuration time whens an {@code oauth2} credential has no valid access token.
 */
@Getter
public final class OAuth2NotConfiguredException extends RuntimeException {

    private final String credentialId;

    public OAuth2NotConfiguredException(final String credentialId) {
        super("oauth2 credential '" + credentialId + "' is not yet configured");
        this.credentialId = Objects.requireNonNull(credentialId, "credentialId");
    }

}
