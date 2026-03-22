# data-masker

A lightweight, reflection-based Java library designed to redact or remove sensitive information from objects, 
maps, and collections. It is built to help developers comply with data privacy regulations (like GDPR or PCI-DSS) 
by ensuring sensitive data is masked before logging or transmitting.

---

## Introduction

`data-masker` provides a flexible and extensible framework for deep-cleaning your data structures. 
Whether you are dealing with a simple POJO, a deeply nested Map, or a complex Collection, the library traverses 
the object graph to apply masking rules based on annotations or pre-defined key patterns.

---

## 💡 The Problem We Solve

In modern applications, sensitive data (passwords, credit card numbers, PII) often accidentally leaks into logs or 
analytics. Manually masking every object is error-prone and creates significant boilerplate.

`data-masker` solves this by:

- **Declarative Masking**: Use the `@SensitiveData` annotation to mark fields for masking or total removal (`ignore = true`).
- **Automatic Pattern Matching**: The library comes with a `MapSensitiveMaskerStrategy` that recognizes common keys 
like `password`, `ssn`, and `creditCard` out of the box.
- **Circular Dependency Support**: Safely handles self-referencing objects (e.g., Parent -> Child -> Parent) 
without causing `StackOverflowError`.
- **Immutability-Friendly**: Instead of modifying your original objects, the library generates a masked representation
(usually as a Map or a new Collection).

---

## Installation

Add the library to your project using the following coordinates:

### Maven

```xml
<dependency>
    <groupId>io.github.khezyapp</groupId>
    <artifactId>data-masker</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation 'io.github.khezyapp:data-masker:1.0.0'
}
```

---

## Usage

### 1. Simple POJO Masking

Annotate your fields or getter methods to define how they should be handled.
```java
public class UserRequest {
    private String username;

    @SensitiveData(mask = "********")
    private String password;

    @SensitiveData(ignore = true)
    private String internalToken; // This field will be removed from the output
}

// Usage
UserRequest request = new UserRequest("admin", "secret123", "tkn_456");
Object masked = DataMaskerUtils.mask(request); 
// Result: { "username": "admin", "password": "********" }
```

### 2. Automatic Map Masking

If you pass a `Map`, the library automatically redacts values based on standard security keys.
```java
Map<String, Object> data = Map.of(
    "email", "user@example.com",
    "apiKey", "12345-secret-key"
);

Object masked = DataMaskerUtils.mask(data);
// Result: { "email": "******@****.com", "apiKey": null } (apiKey is ignored/removed by default)
```

---

## 🛠️ Customization & Strategies

If the default behavior doesn't meet your needs, you can create a custom strategy and register it using the 
`DataMaskerUtils` helper.

### 1. Creating a Custom Strategy

Implement the `SensitiveMaskerStrategy` interface:

```java
public class MyCustomStrategy implements SensitiveMaskerStrategy {
    @Override
    public boolean supports(Object payload) {
        return payload instanceof MySpecialCrate;
    }

    @Override
    public Object mask(Object payload, SensitiveMaskerContext context) {
        // Your custom masking logic here
        return "REDACTED_CRATE";
    }
}
```

### 2. Registering with Helper Class

You can create a custom `SensitiveMasker` instance without changing the global singleton:
```java
SensitiveMasker customMasker = DataMaskerUtils.custom(builder -> {
    builder.registerStrategy(new MyCustomStrategy());
});

Object result = customMasker.mask(myObject);
```
Or pass strategies directly:
```java
SensitiveMasker quickMasker = DataMaskerUtils.custom(new MyCustomStrategy());
```

---

## 🛠️ Advanced Customization: The Two Golden Rules

When you implement a custom `mask(Object payload, SensitiveMaskerContext context)` method, you are acting 
as a gatekeeper for a tree of nested data. To ensure your strategy works safely with the rest of the library, 
you must follow these two rules for handling **Identity and Recursion**.

### 1. Register Before You Process (registerVisited)

**The Goal**: Prevent Infinite Loops (`StackOverflow`).

If Object A points to Object B, and Object B points back to Object A, a standard recursive loop will crash your application.

- **How it works**: As soon as you create your "output" container (like a new Map or a new instance), you must register 
the relationship between the **Original** and the **New object** in the context.
- **The Logic**: This tells the engine: "If you encounter this specific original object again during this run, 
don't re-process it—just return this new version I already started."

```java
final var maskedResult = new HashMap<String, Object>(); 
// Rule #1: Register IMMEDIATELY to stop circular loops
context.registerVisited(payload, maskedResult);
```

### 2. Delegate the Deep Work (`processMask`)

**The Goal**: Support Nested Masking.

Your strategy might know how to handle a specific "Box" object, but that box might contain a `User`, a `Map`, 
or a `List`. You don't need to write the logic to mask those sub-objects; the context already knows how to do it.

- **How it works**: For every field or item inside your object, pass it back to the context using processMask.
- **The Logic**: The context will automatically find the best strategy (`Bean`, `Map`, or `Collection`) for that specific sub-value.

```java
Object innerValue = originalField.get(payload);
// Rule #2: Don't mask it yourself, let the context decide
Object maskedValue = context.processMask(innerValue); 
maskedResult.put("fieldName", maskedValue);
```

### 💡 Complete Implementation Example

Here is how a developer would combine these rules into a custom strategy:
```java
public class MyCustomStrategy implements SensitiveMaskerStrategy {
    @Override
    public boolean supports(Object payload) {
        return payload instanceof CustomDataHolder;
    }

    @Override
    public Object mask(Object payload, SensitiveMaskerContext context) {
        CustomDataHolder original = (CustomDataHolder) payload;
        
        // 1. Register the new container to handle circular references
        Map<String, Object> result = new HashMap<>();
        context.registerVisited(original, result);

        // 2. Use processMask for nested fields (Recursion)
        // This ensures if 'innerData' is a Map or a Bean, it gets masked correctly!
        result.put("innerData", context.processMask(original.getInnerData()));
        result.put("publicLabel", original.getLabel());

        return result;
    }
}
```

### Summary Table

| Method	                             | Role	                 | Benefit                                                                            |
|-------------------------------------|-----------------------|------------------------------------------------------------------------------------|
| `context.registerVisited(old, new)` | 	Identity Mapping     | 	Safety. Stops the library from re-processing the same object, preventing crashes. |
| `context.processMask(value)`	       | Recursive Delegation	 | Thoroughness. Ensures nested data (Maps inside Beans, etc.) is also masked.        |
