package io.github.khezyapp.dhttp.spec;

import java.util.Objects;

/**
 * Reference to a stored credential, used to select how an {@link Operation} authenticates.
 *
 * @param type the credential type (e.g. {@code basic}, {@code oauth2})
 * @param id   the credential identifier in the store
 */
public record CredentialRef(String type, String id) {

    public CredentialRef {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }

    public static CredentialRef of(final String type,
                                   final String id) {
        return new CredentialRef(type, id);
    }
}
