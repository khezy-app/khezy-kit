package io.github.khezyapp.datamasker.strategy;

import io.github.khezyapp.datamasker.annotation.SensitiveData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSensitiveMaskerTest {

    private DefaultSensitiveMasker defaultSensitiveMasker;

    @BeforeEach
    void setUp() {
        final var passwordMask = MapSensitiveMaskerStrategy.KeyValueMask.builder()
                .key("password")
                .mask("####")
                .ignore(false)
                .build();

        final var ssnMask = MapSensitiveMaskerStrategy.KeyValueMask.builder()
                .key("ssn")
                .mask("HIDDEN")
                .ignore(false)
                .build();

        final var strategies = List.of(
                new MapSensitiveMaskerStrategy(List.of(passwordMask, ssnMask)),
                new CollectionSensitiveMaskerStrategy()
        );

        final var compositeStrategy = new CompositeSensitiveMaskerStrategy(strategies);
        defaultSensitiveMasker = new DefaultSensitiveMasker(compositeStrategy);
    }

    @Test
    @DisplayName("Should mask sensitive keys in a simple Map")
    void testMaskSimpleMap() {
        final var payload = Map.of(
                "username", "admin",
                "password", "secret123",
                "ssn", "123-456"
        );

        final var result = (Map<?, ?>) defaultSensitiveMasker.mask(payload);

        assertEquals("admin", result.get("username"));
        assertEquals("####", result.get("password"));
        assertEquals("HIDDEN", result.get("ssn"));
    }

    @Test
    @DisplayName("Should mask sensitive keys inside a nested Collection")
    void testMaskNestedCollection() {
        final var user1 = Map.of("username", "user1", "password", "p1");
        final var user2 = Map.of("username", "user2", "password", "p2");
        final var payload = List.of(user1, user2);

        final var result = (List<?>) defaultSensitiveMasker.mask(payload);

        assertEquals(2, result.size());
        assertEquals("####", ((Map<?, ?>) result.get(0)).get("password"));
        assertEquals("####", ((Map<?, ?>) result.get(1)).get("password"));
    }

    @Test
    @DisplayName("Should handle circular references without StackOverflow")
    void testCircularReference() {
        final var circularMap = new HashMap<String, Object>();
        circularMap.put("name", "self");
        circularMap.put("password", "secret");
        circularMap.put("link", circularMap);

        final var result = (Map<?, ?>) defaultSensitiveMasker.mask(circularMap);

        assertEquals("####", result.get("password"));
        assertSame(result, result.get("link"));
    }

    @Test
    @DisplayName("Should return null when payload is null")
    void testNullPayload() {
        final var result = defaultSensitiveMasker.mask(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Should return original value for primitive types")
    void testPrimitivePayload() {
        final var payload = "Simple String";
        final var result = defaultSensitiveMasker.mask(payload);
        assertEquals(payload, result);
    }

    @Test
    @DisplayName("Should mask array elements correctly")
    void testMaskArray() {
        final var map1 = Map.of("password", "a");
        final var map2 = Map.of("password", "b");
        final var payload = new Map[]{map1, map2};

        final var result = (Object[]) defaultSensitiveMasker.mask(payload);

        assertEquals(2, result.length);
        assertEquals("####", ((Map<?, ?>) result[0]).get("password"));
        assertEquals("####", ((Map<?, ?>) result[1]).get("password"));
    }

    @Test
    @DisplayName("Should mask simple POJO fields with @SensitiveData")
    void testMaskSimplePojo() {
        @Getter
        @Setter
        @Accessors(chain = true)
        class UserProfile {
            private String username;
            @SensitiveData(mask = "CONFIDENTIAL")
            private String password;
            private int age;
        }

        final var payload = new UserProfile()
                .setUsername("jdoe")
                .setPassword("secret123")
                .setAge(30);

        // Register the Bean strategy
        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(payload);

        assertEquals("jdoe", result.get("username"));
        assertEquals("CONFIDENTIAL", result.get("password"));
        assertEquals(30, result.get("age"));
    }

    @Test
    @DisplayName("Should mask nested POJOs recursively")
    void testMaskNestedPojo() {
        @Getter
        @Setter
        @Accessors(chain = true)
        class Address {
            private String street;
            @SensitiveData(mask = "HIDDEN_CITY")
            private String city;
        }

        @Getter
        @Setter
        @Accessors(chain = true)
        class Employee {
            private String name;
            private Address address;
        }

        final var payload = new Employee()
                .setName("Alice")
                .setAddress(new Address().setStreet("Main St").setCity("New York"));

        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(payload);
        final var maskedAddress = (Map<?, ?>) result.get("address");

        assertEquals("Alice", result.get("name"));
        assertEquals("Main St", maskedAddress.get("street"));
        assertEquals("HIDDEN_CITY", maskedAddress.get("city"));
    }

    @Test
    @DisplayName("Should mask inherited fields using ReflectionUtils lookup")
    void testMaskInheritedFields() {
        @Getter
        class BaseEntity {
            @SensitiveData(mask = "BASE_MASK")
            private String internalId = "12345";
        }

        @Getter
        class ChildEntity extends BaseEntity {
            private String publicName = "Test";
        }

        final var payload = new ChildEntity();
        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(payload);

        assertEquals("Test", result.get("publicName"));
        assertEquals("BASE_MASK", result.get("internalId"));
    }

    @Test
    @DisplayName("Should completely exclude fields when @SensitiveData(ignore=true)")
    void testIgnoreTrueExcludesField() {
        @Getter
        @AllArgsConstructor
        class SecurityToken {
            private String publicId;
            @SensitiveData(ignore = true)
            private String secretKey;
            @SensitiveData(mask = "HIDDEN")
            private String tokenType;
        }

        final var payload = new SecurityToken("PUB-123", "SECRET-ABC", "Bearer");
        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(payload);

        // Assertions
        assertTrue(result.containsKey("publicId"), "Public field should exist");
        assertEquals("PUB-123", result.get("publicId"));

        assertTrue(result.containsKey("tokenType"), "Masked field should exist");
        assertEquals("HIDDEN", result.get("tokenType"));

        // CRITICAL: secretKey must be absent
        assertFalse(result.containsKey("secretKey"), "Field with ignore=true must be excluded from the map");
    }

    @Test
    @DisplayName("Should exclude field when ignore=true is placed on the Getter")
    void testIgnoreOnGetterExcludesField() {
        class GetterIgnoreBean {
            @Getter
            private String visible = "hello";
            private String hidden = "world";

            @SensitiveData(ignore = true)
            public String getHidden() {
                return hidden;
            }
        }

        final var payload = new GetterIgnoreBean();
        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(payload);

        assertEquals("hello", result.get("visible"));
        assertFalse(result.containsKey("hidden"), "Property with ignored getter should be excluded");
    }

    @Test
    @DisplayName("Should handle circular references between POJOs")
    void testCircularPojoReference() {
        @Getter
        @Setter
        class Node {
            private String name;
            private Node parent;
            @SensitiveData
            private String secret;
        }

        final var child = new Node();
        child.setName("Child");
        child.setSecret("sssh");

        final var parent = new Node();
        parent.setName("Parent");
        parent.setSecret("top-secret");

        child.setParent(parent);
        parent.setParent(child); // Circular link

        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(child);
        final var parentResult = (Map<?, ?>) result.get("parent");

        assertEquals("***", result.get("secret"));
        assertEquals("***", parentResult.get("secret"));
        assertSame(result, parentResult.get("parent"));
    }

    @Test
    @DisplayName("Should mask when @SensitiveData is placed on the Getter method")
    void testMaskGetterAnnotation() {
        class User {
            private String email;
            User(final String email) {
                this.email = email;
            }

            public String getEmail() {
                return email;
            }

            @SensitiveData(mask = "EMAIL_MASKED")
            public String getEmailSensitive() {
                return email;
            }

            // Simulating a property that only has a getter with annotation
            @SensitiveData(mask = "GETTER_ONLY")
            public String getVirtualProperty() {
                return "top-secret";
            }
        }

        final var payload = new User("test@example.com");
        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(payload);

        assertEquals("test@example.com", result.get("email"));
        assertEquals("EMAIL_MASKED", result.get("emailSensitive"));
        assertEquals("GETTER_ONLY", result.get("virtualProperty"));
    }

    @Test
    @DisplayName("Should prioritize Field annotation over Getter annotation if both exist")
    void testAnnotationPriority() {
        class PriorityBean {
            @SensitiveData(mask = "FIELD_MASK")
            private String data = "secret";

            @SensitiveData(mask = "GETTER_MASK")
            public String getData() {
                return data;
            }
        }

        final var payload = new PriorityBean();
        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(payload);

        // Field annotation wins
        assertEquals("FIELD_MASK", result.get("data"));
    }

    @Test
    @DisplayName("Should mask complex object return type from annotated Getter")
    void testAnnotatedGetterWithComplexObject() {
        @Getter
        @AllArgsConstructor
        class SecretDetails {
            private String code;
        }

        class SecureService {
            @SensitiveData(mask = "OBJECT_MASKED")
            public SecretDetails getDetails() {
                return new SecretDetails("12345");
            }
        }

        final var payload = new SecureService();
        final var beanStrategy = new BeanSensitiveMaskerStrategy();
        final var masker = new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(List.of(beanStrategy)));

        final var result = (Map<?, ?>) masker.mask(payload);

        // Should return the mask string instead of recursing into SecretDetails
        assertEquals("OBJECT_MASKED", result.get("details"));
    }
}
