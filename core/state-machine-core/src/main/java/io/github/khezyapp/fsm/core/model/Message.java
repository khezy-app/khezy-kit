package io.github.khezyapp.fsm.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A message envelope that carries a typed body and a mutable map of string-keyed headers.
 * <p>
 * Inspired by the Spring Integration {@code Message} abstraction, this record separates
 * payload data (a typed {@code body}) from metadata (the {@code headers} map). Unlike the
 * other model records in this package, the <strong>headers map is intentionally mutable</strong>
 * so that actions, guards, and interceptors can enrich the message with tracing information,
 * step markers, or business context as it flows through the state machine.
 * <p>
 * The mutability of headers was an explicit design decision documented in the mutability
 * review — see {@code 04-mutability-review.md}. Headers are backed by a {@link HashMap}
 * and are safe to mutate only within a {@code synchronized fire()} call.
 *
 * @param <T> the type of the message body
 * @param body    the typed payload (may be null)
 * @param headers a mutable map of string-keyed metadata (never null, always a {@code HashMap})
 */
public record Message<T>(
    T body,
    Map<String, Object> headers
) {
    /**
     * Compact canonical constructor that ensures headers is always a mutable HashMap.
     * If a null map is supplied it becomes an empty HashMap.
     */
    public Message {
        headers = Objects.nonNull(headers) ? new HashMap<>(headers) : new HashMap<>();
    }

    /**
     * Creates a message with a body and no headers.
     *
     * @param body the typed payload
     * @param <T>  the type of the body
     * @return a new message with an empty headers map
     */
    public static <T> Message<T> of(final T body) {
        return new Message<>(body, null);
    }

    /**
     * Adds or replaces a single header entry and returns {@code this} for fluent chaining.
     *
     * @param key   the header name
     * @param value the header value
     * @return this message instance (mutated in-place)
     */
    public Message<T> withHeader(final String key,
                                  final Object value) {
        headers.put(key, value);
        return this;
    }

    /**
     * Merges all entries from the supplied map into the current headers.
     *
     * @param additional a map of header entries to add or replace
     * @return this message instance (mutated in-place)
     */
    public Message<T> withHeaders(final Map<String, Object> additional) {
        headers.putAll(additional);
        return this;
    }
}
