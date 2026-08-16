package io.github.khezyapp.dhttp.auth.credential.type;

import java.util.Objects;

/**
 * Typed configuration for a {@code basic-auth} credential (§6.2).
 *
 * @param username the username
 * @param password the password
 */
public record BasicAuthCredentials(String username, String password) {

    public BasicAuthCredentials {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
    }
}
