package io.github.khezyapp.doa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class DynamicObjectsTest {

    /* --- TEST MODELS --- */
    public static class Address {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class UserProfile {
        private String username;
        private Address address;

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(final Address address) {
            this.address = address;
        }
    }

    public record AccountRecord(String id, int balance) {
    }

    /* --- UNIT TESTS --- */

    @Nested
    @DisplayName("Basic Property Access (Get/Set)")
    class BasicAccessTests {

        @Test
        @DisplayName("testMapAccess: Should get and set values in a simple Map")
        void testMapAccess() {
            final var data = new HashMap<String, Object>();
            DynamicObjects.set(data, "key", "value");

            assertEquals("value", data.get("key"));
            assertEquals("value", DynamicObjects.get(data, "key"));
        }

        @Test
        @DisplayName("testBeanAccess: Should interact with POJO via reflection handles")
        void testBeanAccess() {
            final var profile = new UserProfile();
            DynamicObjects.set(profile, "username", "dev_user");

            assertEquals("dev_user", profile.getUsername());
            assertEquals("dev_user", DynamicObjects.get(profile, "username"));
        }

        @Test
        @DisplayName("testRecordAccess: Should handle immutability by returning new record instance")
        void testRecordAccess() {
            final var original = new AccountRecord("ACC-01", 100);
            final var updated = (AccountRecord) DynamicObjects.set(original, "balance", 250);

            assertNotSame(original, updated);
            assertEquals(100, original.balance());
            assertEquals(250, updated.balance());
            assertEquals("ACC-01", updated.id());
        }
    }

    @Nested
    @DisplayName("Nested and Collection Access")
    class DeepAccessTests {

        @Test
        @DisplayName("testNestedPathCreation: Should auto-initialize middle maps when path is missing")
        void testNestedPathCreation() {
            final var root = new HashMap<String, Object>();
            DynamicObjects.set(root, "meta.info.version", "1.0.0");

            assertNotNull(root.get("meta"));
            assertEquals("1.0.0", DynamicObjects.get(root, "meta.info.version"));
        }

        @Test
        @DisplayName("testListIndexAccess: Should get and set values at specific list indices")
        void testListIndexAccess() {
            final var tags = new ArrayList<>(Arrays.asList("java", "spring"));
            DynamicObjects.set(tags, "[1]", "kotlin");

            assertEquals("kotlin", tags.get(1));
            assertEquals("java", DynamicObjects.get(tags, "[0]"));
        }

        @Test
        @DisplayName("testComplexGraph: Should navigate mixed Record, Bean, and List structures")
        void testComplexGraph() {
            final var root = new HashMap<String, Object>();
            root.put("users", new ArrayList<>());

            // Set nested bean inside a list inside a map
            DynamicObjects.set(root, "users[0].address.city", "Bangkok");

            final var city = DynamicObjects.get(root, "users[0].address.city");
            assertEquals("Bangkok", city);
        }

        @Test
        @DisplayName("testComplexPathParsing: Should handle quoted keys with dots")
        void testComplexPathParsing() {
            final var data = new HashMap<String, Object>();
            final var nested = new HashMap<String, Object>();
            nested.put("standard.key", "success");
            data.put("complex", nested);

            // Accessing a key that contains a dot using brackets
            final var result = DynamicObjects.get(data, "complex['standard.key']");
            assertEquals("success", result);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @ParameterizedTest
        @CsvSource({
            "invalid.path",
            "users[99].name"
        })
        @DisplayName("testNullSafeAccess: Should return null for non-existent paths with '?' operator")
        void testNullSafeAccess(final String path) {
            final var target = new HashMap<String, Object>();
            // Testing the safeAccess logic from PropertyToken (e.g. "user?")
            assertDoesNotThrow(() -> {
                final var result = DynamicObjects.get(target, path + "?");
                assertNull(result);
            });
        }

        @Test
        @DisplayName("testOutOfBoundsList: Should expand list automatically on set")
        void testOutOfBoundsList() {
            final var list = new ArrayList<>();
            DynamicObjects.set(list, "[2]", "third");

            assertEquals(3, list.size());
            assertNull(list.get(0));
            assertEquals("third", list.get(2));
        }

        @Test
        @DisplayName("testBlankPath: Should validate input path")
        void testBlankPath() {
            final var map = new HashMap<String, Object>();
            assertThrows(IllegalArgumentException.class, () -> DynamicObjects.get(map, "  "));
        }

        @Test
        @DisplayName("testCaseSensitivity: Property names should be case sensitive after decapitalization")
        void testCaseSensitivity() {
            final var profile = new UserProfile();
            DynamicObjects.set(profile, "username", "Alice");

            // "Username" should fail if BeanAdapter expects "username"
            assertNull(DynamicObjects.get(profile, "Username"));
        }

        @Test
        @DisplayName("testMalformedPath: Should throw exception on unmatched brackets")
        void testMalformedPath() {
            final var map = new HashMap<String, Object>();
            assertThrows(IllegalArgumentException.class, () ->
                DynamicObjects.get(map, "users[0")
            );
        }
    }

    @Nested
    @DisplayName("Remove Operations")
    class RemoveTests {

        @Test
        @DisplayName("testRemoveSimpleMapKey: Should remove a top-level key from a Map")
        void testRemoveSimpleMapKey() {
            final var data = new HashMap<String, Object>();
            data.put("name", "Alice");
            data.put("age", 30);

            DynamicObjects.remove(data, "name");

            assertFalse(data.containsKey("name"));
            assertEquals(30, data.get("age"));
        }

        @Test
        @DisplayName("testRemoveNestedMapKey: Should remove a key from a nested Map")
        void testRemoveNestedMapKey() {
            final var root = new HashMap<String, Object>();
            final var config = new HashMap<String, Object>();
            config.put("debug", true);
            config.put("port", 8080);
            root.put("config", config);

            DynamicObjects.remove(root, "config.debug");

            assertFalse(config.containsKey("debug"));
            assertEquals(8080, config.get("port"));
        }

        @Test
        @DisplayName("testRemoveReturnsRemovedValue: Should return the modified target")
        void testRemoveReturnsRemovedValue() {
            final var data = new HashMap<String, Object>();
            data.put("key", "value");

            final var result = DynamicObjects.remove(data, "key");

            assertSame(data, result);
            assertFalse(data.containsKey("key"));
        }

        @Test
        @DisplayName("testRemoveNonExistentKey: Should return target and not throw")
        void testRemoveNonExistentKey() {
            final var data = new HashMap<String, Object>();
            data.put("existing", "data");

            final var result = DynamicObjects.remove(data, "nonexistent");

            assertSame(data, result);
            assertEquals("data", data.get("existing"));
        }

        @Test
        @DisplayName("testRemoveFromMapInsideList: Should remove key from Map inside a List")
        void testRemoveFromMapInsideList() {
            final var root = new HashMap<String, Object>();
            final var items = new ArrayList<HashMap<String, Object>>();
            final var item = new HashMap<String, Object>();
            item.put("id", 1);
            item.put("name", "Widget");
            items.add(item);
            root.put("items", items);

            DynamicObjects.remove(root, "items[0].name");

            assertFalse(item.containsKey("name"));
            assertEquals(1, item.get("id"));
        }

        @Test
        @DisplayName("testRemoveDeepNestedMap: Should remove at deep path and keep intermediate maps")
        void testRemoveDeepNestedMap() {
            final var root = new HashMap<String, Object>();
            DynamicObjects.set(root, "a.b.c.d", "deep");

            DynamicObjects.remove(root, "a.b.c.d");

            assertNotNull(root.get("a"));
            assertNotNull(((HashMap<?, ?>) root.get("a")).get("b"));
            assertNotNull(((HashMap<?, ?>) ((HashMap<?, ?>) root.get("a")).get("b")).get("c"));
            final var c = (HashMap<?, ?>) ((HashMap<?, ?>) ((HashMap<?, ?>) root.get("a")).get("b")).get("c");
            assertFalse(c.containsKey("d"));
        }

        @Test
        @DisplayName("testRemoveWithSafeAccess: Should not throw on broken path with safe access")
        void testRemoveWithSafeAccess() {
            final var data = new HashMap<String, Object>();

            assertDoesNotThrow(() -> DynamicObjects.remove(data, "nonexistent.path?"));
        }

        @Test
        @DisplayName("testRemoveBeanProperty: Should set POJO property to null")
        void testRemoveBeanProperty() {
            final var profile = new UserProfile();
            profile.setUsername("Alice");

            DynamicObjects.remove(profile, "username");

            assertNull(profile.getUsername());
        }

        @Test
        @DisplayName("testRemoveRecordComponent: Should return new record with null component")
        void testRemoveRecordComponent() {
            final var original = new AccountRecord("ACC-01", 100);

            final var updated = (AccountRecord) DynamicObjects.remove(original, "id");

            assertNotSame(original, updated);
            assertNull(updated.id());
            assertEquals(100, updated.balance());
        }

        @Test
        @DisplayName("testRemoveListElement: Should remove element at index from List")
        void testRemoveListElement() {
            final var list = new ArrayList<>(Arrays.asList("a", "b", "c"));

            DynamicObjects.remove(list, "[1]");

            assertEquals(2, list.size());
            assertEquals("a", list.get(0));
            assertEquals("c", list.get(1));
        }
    }
}
