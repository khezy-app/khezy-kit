package io.github.khezyapp.dhttp.auth.credential.type;

import java.util.Map;
import java.util.Objects;

/**
 * Typed configuration for an {@code http-header} credential: a set of headers merged into the request
 * (§6.2).
 *
 * @param headers the headers to inject
 */
public record HttpHeaderCredentials(Map<String, String> headers) {

    public HttpHeaderCredentials {
        headers = Map.copyOf(Objects.requireNonNullElseGet(headers, Map::of));
    }
}
