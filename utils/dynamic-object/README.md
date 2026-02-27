# Dynamic Objects Library

A lightweight, Java library for navigating and manipulating complex object graphs. 
It provides a unified API to access properties across Maps, Records, POJOs, and Lists using a simple string-path syntax, 
effectively eliminating manual casting and boilerplate null-checks.

---

## ❓ Why use Dynamic Objects?

In standard Java, you use getters and setters. However, in many professional scenarios, you lose the ability 
to call those methods directly.

### Scenario: The "Deep Casting" Nightmare

Imagine you are building a generic Notification System. You receive a configuration object (`Map<String, Object>`) 
from a database or a JSON API. To get the city of a user, you would normally have to write this:

```java
// The "Manual" way
Map<String, Object> config = getDynamicConfig();
Object userObj = config.get("user");
if (userObj instanceof Map) {
    Map<String, Object> userMap = (Map<String, Object>) userObj;
    Object addressObj = userMap.get("address");
    if (addressObj instanceof Map) {
        String city = (String) ((Map<String, Object>) addressObj).get("city");
    }
}
```

### Scenario: The "Dynamic Objects" Way

With this library, you treat the object as a graph, regardless of whether it is a Map, a Record, or a POJO.

```java
// The "Dynamic" way
Object config = getDynamicConfig();
String city = (String) DynamicObjects.get(config, "user.address.city");
```

### Real-world Use Cases
* **Generic Rules Engines**: When you need to evaluate conditions like `order.total > 100` where the `order` could be any type of object.
* **Data Mapping/ETL**: Moving data between different systems where the source and target structures are defined in a configuration file.
* **Templating**: If you are building a custom email template engine and need to inject variables into a string.
* **Interoperability**: Working with a mix of legacy Java Beans and modern Java Records without changing your access logic.

---

## Features

* **Unified Access**: One API for Records, Maps, Lists, and Beans.
* **Immutability Support**: Transparently handles Java Records using copy-on-write logic.
* **Safe Navigation**: Support for safe access tokens to prevent `NullPointerException`.
* **Extensible**: Easily plug in custom adapters for JSON nodes or third-party frameworks.

---

## Installation

Add the following dependency to your project:

### Maven

```xml
<dependency>
    <groupId>io.github.khezyapp</groupId>
    <artifactId>dynamic-object</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.khezyapp:dynamic-object:1.0.0'
```

---

## Usage

### Basic Operations

The `DynamicObjects` facade is the easiest way to get started.

```java
public record User(String name, Address address) {}
public record Address(String city, List<String> tags) {}

User user = new User("Alice", new Address("New York", List.of("work", "home")));

// 1. Deep Read
String city = (String) DynamicObjects.get(user, "address.city"); // "New York"
String tag = (String) DynamicObjects.get(user, "address.tags[0]"); // "work"

// 2. Deep Write (Returns a new Record instance for immutable types)
User updatedUser = (User) DynamicObjects.set(user, "address.city", "London");
```

---

## Customization

If the default behavior does not fit your specific requirements, you can customize the engine.

### 1. Custom TypeAdapter

Create an adapter for specialized types, such as Jackson `JsonNode`.

```java
public class JsonNodeAdapter implements TypeAdapter {\
    
    @Override
    public boolean supports(Object target) {
    return target instanceof com.fasterxml.jackson.databind.JsonNode;
    }
    
    @Override
    public Object getValue(Object target, String property) {
        return ((JsonNode) target).get(property);
    }
    
    @Override
    public Object setValue(Object target, String property, Object value) {
        ((ObjectNode) target).put(property, value.toString());
        return target;
    }
}
```

### 2. Manual Configuration

Use the `AccessorFactoryImpl` to build a custom engine with your adapters.

```java
ObjectAccessor customAccessor = new AccessorFactoryImpl()
    .registerAdapter(new JsonNodeAdapter()) // Custom logic takes priority
    .registerAdapter(new MapAdapter())
    .registerAdapter(new BeanAdapter(new MapCache<>(), new MapCache<>()))
    .withParser(new DefaultPathParser(new MapCache<>()))
    .build();

// Use the custom accessor instance
Object value = customAccessor.get(myJsonNode, "metadata/version");
```

---

## Architecture

The library follows a modular design:
* **PathParser**: Breaks string paths into tokens (Property or Index).
* **TypeAdapter**: Logic for reading/writing to specific object types.
* **ObjectAccessor**: The engine that coordinates tokens and adapters.

