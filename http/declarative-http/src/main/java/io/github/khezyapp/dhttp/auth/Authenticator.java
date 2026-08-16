package io.github.khezyapp.dhttp.auth;

import io.github.khezyapp.dhttp.auth.credential.DecryptedCredential;
import io.github.khezyapp.dhttp.transport.HttpRequest;

/**
 * Injects authentication into a request from a resolved credential ({@code R10}, §6.3).
 *
 * <p>Side-channel state (whether auth was applied, the credential id, token expiry) is reported
 * through {@link AuthResult}.</p>
 */
@FunctionalInterface
public interface Authenticator {

    /**
     * @param credential the resolved, decrypted credential
     * @param request    the request to authenticate
     * @param out        side-channel carrier for auth metadata
     * @return a new request with authentication applied (immutable)
     */
    HttpRequest apply(DecryptedCredential<?> credential, HttpRequest request, AuthResult out);
}
