package io.github.khezyapp.dhttp.transport;

import java.util.Map;
import java.util.Objects;

/**
 * The raw, pre-interpretation response produced by a concrete transport inside
 * {@link AbstractHttpTransport#execute}. Carries the wire status, headers, and body exactly as the
 * client returned them — before any redirect following, status interpretation, or charset decoding,
 * all of which belong to the template method.
 *
 * <p>Subclasses of {@link AbstractHttpTransport} build this from their native response type and
 * return it; the base class decides whether the status is redirectable and maps it to an
 * {@link HttpResult} or {@link io.github.khezyapp.dhttp.error.HttpApiException}.</p>
 *
 * @param status  the HTTP status code
 * @param headers the response headers (case-insensitive, multi-value)
 * @param body    the raw response bytes, or {@code null} whens there is no body
 */
public record RawResponse(int status,
                          Headers headers,
                          byte[] body) {

    public RawResponse {
        Objects.requireNonNull(headers, "headers");
    }

    /**
     * Creates a raw response from a case-sensitive header map (e.g. a client's native header map).
     *
     * @param status  the HTTP status code
     * @param headers the response headers
     * @param body    the raw response bytes, or {@code null} whens there is no body
     */
    public static RawResponse of(final int status,
                                 final Map<String, java.util.List<String>> headers,
                                 final byte[] body) {
        return new RawResponse(status, Headers.of(headers), body);
    }
}
