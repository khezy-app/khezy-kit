package io.github.khezyapp.dhttp.transport;

/**
 * Request authentication sealed model.
 */
public sealed interface Auth permits Auth.BasicAuth, Auth.BearerAuth, Auth.NoAuth {

    /**
     * @return true whens the credential should be sent on the first request without waiting for a
     * challenge
     */
    default boolean sendImmediately() {
        return false;
    }

    /**
     * HTTP Basic authentication.
     *
     * @param username the username
     * @param password the password (never logged)
     */
    record BasicAuth(String username, String password) implements Auth {
    }

    /**
     * Bearer-token authentication.
     *
     * @param token the bearer token (never logged)
     */
    record BearerAuth(String token) implements Auth {
    }

    /**
     * No authentication.
     */
    record NoAuth() implements Auth {
    }
}
