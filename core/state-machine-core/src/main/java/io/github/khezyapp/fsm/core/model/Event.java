package io.github.khezyapp.fsm.core.model;

/**
 * An event that triggers a state machine transition.
 * <p>
 * Every event carries a <strong>type discriminator</strong> ({@code type}) that the
 * transition index uses to find the matching transition rule for the current state.
 * An optional {@link Message} payload ({@code message}) can carry structured data
 * alongside the event — useful for passing parameters between workflow steps.
 * <p>
 * The event type {@code E} is the only field used for transition lookup. The message
 * body and headers are purely informative side-channel data that listeners and
 * interceptors can read or enrich.
 *
 * @param <E> the type used to discriminate event kinds (e.g. {@code String}, {@code enum})
 * @param <T> the type of the message body carried by this event
 * @param type    the event type discriminator used for transition matching
 * @param message an optional message envelope containing a body and mutable headers
 */
public record Event<E, T>(
    E type,
    Message<T> message
) {
    /**
     * Compact canonical constructor that ensures the message is never null.
     * If a null message is supplied, it is replaced with an empty message
     * (null body, empty headers map).
     */
    public Event {
        message = message != null ? message : new Message<>(null, null);
    }

    /**
     * Creates a fire-and-forget event with no message payload.
     *
     * @param type the event type discriminator
     * @param <E>  the event type
     * @return a new event with a null-message payload
     */
    public static <E> Event<E, Void> of(final E type) {
        return new Event<>(type, null);
    }

    /**
     * Creates an event carrying a typed message payload.
     *
     * @param type    the event type discriminator
     * @param message the message envelope (body + headers)
     * @param <E>     the event type
     * @param <T>     the type of the message body
     * @return a new event with the given message
     */
    public static <E, T> Event<E, T> of(final E type, final Message<T> message) {
        return new Event<>(type, message);
    }
}
