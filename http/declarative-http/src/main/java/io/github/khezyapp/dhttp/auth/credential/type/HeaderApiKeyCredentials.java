package io.github.khezyapp.dhttp.auth.credential.type;

import java.util.Objects;

/**
 * Typed configuration for an {@code api-key} credential sent as a header (§6.2).
 *
 * @param headerName the header to inject (e.g. {@code api-key})
 * @param value      the secret value
 */
public record HeaderApiKeyCredentials(String headerName, String value) {

    public HeaderApiKeyCredentials {
        Objects.requireNonNull(headerName, "headerName");
        Objects.requireNonNull(value, "value");
    }
}
