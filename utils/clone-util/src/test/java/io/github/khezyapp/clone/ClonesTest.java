package io.github.khezyapp.clone;

import io.github.khezyapp.clone.annotation.IgnoreClone;
import io.github.khezyapp.clone.api.CloneContext;
import io.github.khezyapp.clone.api.CloneStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClonesTest {

    @Test
    @DisplayName("Should perform deep clone on simple POJO")
    void testDeepClonePojo() {
        final var address = new Address("Bangkok");
        final var original = new User("Gemini", address);

        final var clone = Clones.deepClone(original);

        assertNotNull(clone);
        assertNotSame(original, clone);
        assertNotSame(original.getAddress(), clone.getAddress());
        assertEquals(original.getName(), clone.getName());
        assertEquals(original.getAddress().getCity(), clone.getAddress().getCity());
    }

    @Test
    @DisplayName("Should handle circular references without StackOverflow")
    void testCircularReference() {
        final var node1 = new Node("Node 1");
        final var node2 = new Node("Node 2");
        node1.setNext(node2);
        node2.setNext(node1); // Circularity

        final var clone = Clones.deepClone(node1);

        assertNotSame(node1, clone);
        assertSame(clone, clone.getNext().getNext(), "Circular reference integrity should be maintained");
    }

    @Test
    @DisplayName("Should respect @IgnoreClone annotation on fields")
    void testIgnoreCloneAnnotation() {
        final var original = new SecretData("Visible", "HiddenPassword");

        final var clone = Clones.deepClone(original);

        assertEquals("Visible", clone.publicInfo);
        assertNull(clone.getSecretKey(), "Field with @IgnoreClone should be null in clone");
    }

    @Test
    @DisplayName("Should use custom strategy when registered")
    void testCustomStrategyRegistration() {
        // Custom strategy that always returns a fixed object for specific class
        final var mockStrategy = new CloneStrategy() {
            @Override
            public boolean support(final Class<?> clz) {
                return clz == SpecialService.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T copy(final T origin, final CloneContext context) {
                return (T) new SpecialService("Mocked");
            }
        };

        final var customCloner = Clones.defaultCloner(mockStrategy);
        final var original = new SpecialService("Real");

        final var clone = customCloner.deepClone(original);

        assertEquals("Mocked", clone.getValue(), "Custom strategy should take priority over ReflectionStrategy");
    }

    @Test
    @DisplayName("Should handle complex nested collections")
    void testNestedCollections() {
        final var original = new HashMap<String, List<Integer>>();
        original.put("scores", new ArrayList<>(List.of(10, 20, 30)));

        final var clone = Clones.deepClone(original);

        assertNotSame(original, clone);
        assertNotSame(original.get("scores"), clone.get("scores"));
        assertEquals(original.get("scores"), clone.get("scores"));
    }

    @Test
    void testThirdPartyImmutables() {
        // Even if this is a complex object, your library sees the package
        // and decides to skip cloning it.
        final org.joda.time.DateTime jodaTime = org.joda.time.DateTime.now();

        final var clone = Clones.deepClone(jodaTime);

        assertSame(jodaTime, clone, "Joda-Time objects should be treated as immutable by name check");
    }

    // --- Helper Classes for Testing ---

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private String name;
        private Address address;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address {
        private String city;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Node {
        private String name;
        private Node next;

        Node(final String name) {
            this.name = name;
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class SecretData {
        private String publicInfo;
        @IgnoreClone
        private String secretKey;

    }

    @Getter
    @AllArgsConstructor
    public static class SpecialService {
        private String value;
    }
}
