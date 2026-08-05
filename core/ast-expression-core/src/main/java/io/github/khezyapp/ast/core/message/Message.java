package io.github.khezyapp.ast.core.message;

import java.util.HashMap;
import java.util.Map;

/**
 * Container for the input payload passed to expression evaluation.
 * <p>
 * A {@code Message} consists of headers (metadata key-value pairs) and a body
 * (the primary data object, typically a {@code Map<String, Object>}).
 * It is used by the {@link io.github.khezyapp.ast.core.eval.EvaluationContext}
 * and accessed by evaluators such as {@link io.github.khezyapp.ast.core.builtin.PayloadEvaluator}.
 * </p>
 */
public final class Message {
    private final Map<String, Object> headers;
    private final Object body;

    private Message(final Map<String, Object> headers,
                    final Object body) {
        this.headers = Map.copyOf(headers);
        this.body = body;
    }

    /**
     * Creates a message with the given body and empty headers.
     *
     * @param body the message body
     * @return a new message
     */
    public static Message of(final Object body) {
        return new Message(Map.of(), body);
    }

    /**
     * Creates a message with headers and a body.
     *
     * @param headers the message headers
     * @param body    the message body
     * @return a new message
     */
    public static Message withHeaders(final Map<String, Object> headers,
                                      final Object body) {
        return new Message(headers, body);
    }

    /**
     * Returns the message headers.
     *
     * @return an unmodifiable map of headers
     */
    public Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * Returns a specific header value.
     *
     * @param name the header name
     * @return the header value, or {@code null}
     */
    public Object getHeader(final String name) {
        return headers.get(name);
    }

    /**
     * Returns the message body.
     *
     * @return the body object
     */
    public Object getBody() {
        return body;
    }

    /**
     * Returns the message body cast to the requested type.
     *
     * @param <T>  the expected type
     * @param type the class of the expected type
     * @return the body cast to the given type
     * @throws ClassCastException if the body is not of the expected type
     */
    public <T> T getBodyAs(final Class<T> type) {
        return type.cast(body);
    }

    /**
     * Returns a new message with an additional header.
     *
     * @param key   the header key
     * @param value the header value
     * @return a new message with the added header
     */
    public Message copyWithHeader(final String key,
                                  final Object value) {
        final var h = new HashMap<>(headers);
        h.put(key, value);
        return new Message(h, body);
    }
}
