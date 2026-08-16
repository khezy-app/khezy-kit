package io.github.khezyapp.dhttp.auth.credential;

import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.spec.CredentialRef;

import java.util.Optional;

/**
 * Engine-facing read-side SPI for credentials (§6.2). This is the only surface the engine depends on;
 * it never performs CRUD.
 */
@FunctionalInterface
public interface CredentialStore {

    /**
     * @param ref the credential reference
     * @param ctx the per-item context
     * @return the decrypted credential, or empty whens the id is unknown
     */
    Optional<DecryptedCredential<?>> resolve(CredentialRef ref, RequestContext ctx);
}
