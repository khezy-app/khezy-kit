package io.github.khezyapp.dhttp.plan;

import io.github.khezyapp.dhttp.spec.CredentialRef;

import java.util.Objects;

/**
 * What credential should authenticate the planned request and how ({@code R10}).
 *
 * @param ref  the resolved credential reference
 * @param type the credential/auth type (e.g. {@code api-key}, {@code basic-auth}, {@code oauth2})
 */
public record AuthRequest(CredentialRef ref, String type) {

    public AuthRequest {
        Objects.requireNonNull(ref, "ref");
        Objects.requireNonNull(type, "type");
    }
}
