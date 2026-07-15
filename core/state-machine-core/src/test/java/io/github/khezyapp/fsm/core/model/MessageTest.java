package io.github.khezyapp.fsm.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    @DisplayName("Should create message with body and headers")
    void testCreateWithBodyAndHeaders() {
        final var headers = new HashMap<String, Object>();
        headers.put("traceId", "abc-123");
        final var message = new Message<>("hello", headers);

        assertEquals("hello", message.body());
        assertEquals("abc-123", message.headers().get("traceId"));
    }

    @Test
    @DisplayName("Should default headers to empty map when null")
    void testNullHeadersDefaultsToEmpty() {
        final var message = new Message<>("hello", null);

        assertNotNull(message.headers());
        assertTrue(message.headers().isEmpty());
    }

    @Test
    @DisplayName("Should create message with of() factory")
    void testOfFactory() {
        final var message = Message.of(42);

        assertEquals(42, message.body());
        assertTrue(message.headers().isEmpty());
    }

    @Test
    @DisplayName("Should mutate headers via withHeader")
    void testWithHeaderMutatesInPlace() {
        final var message = new Message<>("test", null);

        message.withHeader("key1", "value1");

        assertEquals("value1", message.headers().get("key1"));
    }

    @Test
    @DisplayName("Should merge multiple headers via withHeaders")
    void testWithHeadersMergesMultiple() {
        final var message = new Message<>("test", null);

        message.withHeaders(Map.of("k1", "v1", "k2", "v2"));

        assertEquals("v1", message.headers().get("k1"));
        assertEquals("v2", message.headers().get("k2"));
    }

    @Test
    @DisplayName("Should return this from withHeader for chaining")
    void testWithHeaderReturnsThis() {
        final var message = new Message<>("test", null);

        final var result = message.withHeader("k", "v");

        assertSame(message, result);
    }

    @Test
    @DisplayName("Should support direct mutation via headers() accessor")
    void testDirectHeadersMutation() {
        final var message = new Message<>("test", null);

        message.headers().put("direct", "yes");

        assertEquals("yes", message.headers().get("direct"));
    }

    @Test
    @DisplayName("Should copy input map to mutable HashMap")
    void testInputMapIsCopied() {
        final var original = new HashMap<String, Object>();
        original.put("initial", "value");
        final var message = new Message<>("test", original);

        original.put("after", "should-not-appear");

        assertNull(message.headers().get("after"));
    }
}
