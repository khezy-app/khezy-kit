package io.github.khezyapp.fsm.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    @DisplayName("Should create event with type only via of()")
    void testOfTypeOnly() {
        final var event = Event.of("submit");

        assertEquals("submit", event.type());
        assertNotNull(event.message());
        assertNull(event.message().body());
    }

    @Test
    @DisplayName("Should create event with type and message")
    void testOfTypeAndMessage() {
        final var message = Message.of("kyc-data");
        final var event = Event.of("submit", message);

        assertEquals("submit", event.type());
        assertSame(message, event.message());
        assertEquals("kyc-data", event.message().body());
    }

    @Test
    @DisplayName("Should default message when null")
    void testNullMessageDefaults() {
        final var event = new Event<String, Void>("test", null);

        assertEquals("test", event.type());
        assertNotNull(event.message());
        assertNull(event.message().body());
        assertTrue(event.message().headers().isEmpty());
    }

    @Test
    @DisplayName("Should preserve message headers through event")
    void testMessageHeadersPreserved() {
        final var message = Message.of("data");
        message.withHeader("traceId", "t-001");
        final var event = Event.of("process", message);

        assertEquals("t-001", event.message().headers().get("traceId"));
    }
}
