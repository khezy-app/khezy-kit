package io.github.khezyapp.dhttp.spec;

import lombok.Builder;

import java.util.Map;
import java.util.Objects;

/**
 * Describes the concrete request produced by a route.
 *
 * <p>{@code json} is the default/static body and accepts any JSON-capable value: a {@link Map} for
 * an object root, a {@link java.util.List} for an array root, or a scalar. It is serialized when the
 * request is planned. When no {@code Send} targets the body it is used as-is; otherwise a
 * {@code Map} root acts as the base and each body {@code Send} overrides the matching dotted path
 * per invocation (defaults + customization). A {@link String} is treated as raw JSON text and used
 * verbatim.</p>
 *
 * <p>{@code baseUrl} optionally overrides the spec's base URL for this route (e.g. per-provider
 * endpoints sharing an operation id). When it is blank and {@code path} is an absolute URL, the
 * path is used as the full URL instead of being joined with the spec base.</p>
 *
 * @param method   the HTTP method
 * @param path     the URL path (may be templated)
 * @param headers  case-insensitive request headers
 * @param query    query parameters
 * @param json     a literal JSON body ({@link Map}, {@link java.util.List}, or raw JSON
 *                 {@link String}), or {@code null}
 * @param encoding body encoding, or {@code null} to default
 * @param baseUrl  per-route base URL override, or {@code null} to use the spec's base URL
 */
@Builder
public record RequestShape(HttpMethod method,
                           String path,
                           Map<String, String> headers,
                           Map<String, Object> query,
                           Object json,
                           String encoding,
                           String baseUrl) {

    public RequestShape {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        headers = Map.copyOf(Objects.requireNonNullElseGet(headers, Map::of));
        query = Map.copyOf(Objects.requireNonNullElseGet(query, Map::of));
    }

    public RequestShape(final HttpMethod method,
                        final String path,
                        final Map<String, String> headers,
                        final Map<String, Object> query,
                        final Object json,
                        final String encoding) {
        this(method, path, headers, query, json, encoding, null);
    }
}
