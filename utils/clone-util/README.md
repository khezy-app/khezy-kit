# Clone Util Library

A lightweight, reflection-based deep cloning library for Java. It is designed to create complete, 
detached copies of complex object graphs while maintaining referential integrity (handling circular references) 
and respecting object immutability.

---

## ❓ What is this library used for?

In Java, the default `.clone()` method performs a shallow copy, and manual copy constructors become unmaintainable 
as object graphs grow. This library:

* **Decouples Objects**: Creates a new instance where changes to the clone do not affect the original.
* **Handles Circular References**: Uses an `IdentityHashMap` to ensure that if **Object A** references **Object B** 
and vice versa, the clone reflects that same relationship without causing a `StackOverflowError`.
* **Supports Modern Java**: Natively handles Records, Enums, and Immutable Collections.

---

## 📦 Installation

### Maven
```xml
<dependency>
    <groupId>io.github.khezyapp</groupId>
    <artifactId>clone-util</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.khezyapp:clone-util:1.0.0'
```

---

## 📖 How to Use

The library is designed for "plug-and-play" use via the `Clones` facade.

### Basic Cloning

```java
User original = new User("John", new Address("New York"));
User copy = Clones.deepClone(original);

assert copy != original;
assert copy.getAddress() != original.getAddress();
```

### Advanced Configuration

If you need specific custom strategies, you can create a dedicated cloner instance:

```java
Cloner myCloner = Clones.defaultCloner(new MySpecialStrategy());
User copy = myCloner.deepClone(original);
```

---

## 🏷️ Using Annotations

You can control the cloning behavior at the class or field level using built-in annotations.

### 1. Excluding Classes or Fields (`@IgnoreClone`)

Use this when you have sensitive data (like passwords) or system resources (like Thread or InputStream) 
that should not be copied.

```java
public class User {
    private String name;

    @IgnoreClone
    private String password; // This field will be null in the clone
}

@IgnoreClone
public class InternalSystemLogger { 
}
```

### 2. Optimizing with `@MarkAsImmute`

If you have a custom complex object that you know is immutable, mark it to skip the expensive reflection process.
The cloner will simply copy the reference.

```java
@MarkAsImmute
public class TaxRules {
    private final double rate = 0.15;
    // Shared across all clones to save memory
}
```

---

## 🛠️ Custom Strategies

If the default `ReflectionStrategy` or `CollectionStrategy` doesn't fit your specific requirements, 
you can implement your own logic.

### Step 1: Create the Strategy
```java
public class JsonNodeStrategy implements CloneStrategy {
    @Override
    public boolean support(Class<?> clz) {
        return JsonNode.class.isAssignableFrom(clz);
    }

    @Override
    public <T> T copy(T origin, CloneContext context) {
        // JsonNode is often effectively immutable or has its own deepCopy()
        return (T) ((JsonNode) origin).deepCopy();
    }
}
```

### Step 2: Register the Strategy

Custom strategies are registered at the front of the chain, meaning they take priority over the default library strategies.
```java
Cloner cloner = DefaultCloner.builder()
    .registerStrategy(new JsonNodeStrategy())
    .build();

MyData copy = cloner.deepClone(originalData);
```

---

## ⚙️ How it Works (The Strategy Chain)

When `deepClone()` is called, the library evaluates strategies in this order:

* **Custom Strategies**: Any strategy you registered manually.
* **Immutable**: Handles primitives, Strings, Enums, Records, and @MarkAsImmute.
* **Map**: Deep clones Map keys and values.
* **Collection**: Deep clones List, Set, etc.
* **Array**: Handles both primitive and object arrays.
* **Reflection**: The "catch-all" that clones standard Java Beans/POJOs by traversing fields.