package io.github.khezyapp.pluginlib;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginCandidateTest {

    static class Foo {

        Foo() {
        }

        String value() {
            return "test";
        }
    }

    @Test
    @DisplayName("Should create candidate and instantiate")
    void testNewInstance() {
        @SuppressWarnings("unchecked")
        final var candidate = new PluginCandidate<Foo>(
                "foo", "1.0.0", (Class<? extends Foo>) Foo.class);

        assertEquals("foo", candidate.name());
        assertEquals("1.0.0", candidate.version());
        assertSame(Foo.class, candidate.providerClass());

        final var instance = candidate.newInstance();
        assertNotNull(instance);
        assertInstanceOf(Foo.class, instance);
    }

    @Test
    @DisplayName("Should reject null fields")
    void testNullRejection() {
        assertThrows(NullPointerException.class,
                () -> new PluginCandidate<>(null, "1.0.0", Foo.class));
        assertThrows(NullPointerException.class,
                () -> new PluginCandidate<>("foo", null, Foo.class));
        assertThrows(NullPointerException.class,
                () -> new PluginCandidate<>("foo", "1.0.0", null));
    }
}
