package io.github.khezyapp.dhttp.auth.credential;

import java.util.Objects;

/**
 * Marker for a credential type: its name plus the {@code Class} of its typed configuration record.
 *
 * @param name     the credential type name (e.g. {@code oauth2})
 * @param dataType the typed config class (e.g. {@code OAuth2Credentials.class})
 */
public record CredentialType(String name, Class<?> dataType) {

    public CredentialType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dataType, "dataType");
    }
}
