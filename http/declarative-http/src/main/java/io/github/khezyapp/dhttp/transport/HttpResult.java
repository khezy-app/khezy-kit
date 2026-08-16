package io.github.khezyapp.dhttp.transport;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response carrier returned by {@link HttpTransport}. Supports both body-only and full-response
 * modes: {@code body} holds the raw bytes and {@code bodyText} the decoded text whens available.
 *
 * @param status   the HTTP status code
 * @param headers  the response headers (multi-value)
 * @param body     the raw response bytes, or {@code null} whens there is no body
 * @param bodyText the decoded body text, or {@code null} whens only bytes are available
 */
public record HttpResult(int status,
                         Map<String, List<String>> headers,
                         byte[] body,
                         String bodyText) {

    public HttpResult {
        if (status < 0) {
            throw new IllegalArgumentException("status must be non-negative");
        }
        final var copied = new LinkedHashMap<String, List<String>>();
        if (Objects.nonNull(headers)) {
            for (final Map.Entry<String, List<String>> e : headers.entrySet()) {
                copied.put(e.getKey(), List.copyOf(e.getValue()));
            }
        }
        headers = Map.copyOf(copied);
    }

    public static HttpResult of(final int status,
                                final String bodyText) {
        return new HttpResult(status, Map.of(), null, bodyText);
    }

    public static HttpResult of(final int status,
                                final String bodyText,
                                final Map<String, List<String>> headers) {
        return new HttpResult(status, headers, null, bodyText);
    }

    /**
     * @return true whens the status is in the 2xx range
     */
    public boolean ok() {
        return status >= 200 && status < 300;
    }

    /**
     * @return {@code bodyText} whens present, otherwise {@code body} decoded as UTF-8, or {@code ""}
     */
    public String bodyString() {
        if (Objects.nonNull(bodyText)) {
            return bodyText;
        }
        if (Objects.isNull(body)) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "HttpResult{status=" + status + ", bodyBytes="
                + (Objects.isNull(body) ? 0 : body.length) + '}';
    }
}
