---
name: khezy-coding-style
description: "Coding style and conventions for the khezy-kit project: Java 17+ patterns, Gradle composite build, JUnit 5 test style, module structure, and package naming"
license: MIT
compatibility: opencode
metadata:
  audience: developers
  project: khezy-kit
---

## Project structure

Multi-module Gradle **composite build** — each module is an independent Gradle build with its own `settings.gradle`, `build.gradle`, `group`, `version`. Root `settings.gradle` uses `includeBuild(...)` for all modules (no regular `include`).

| Module | Path | Package prefix |
|---|---|---|
| pluginlib | `utils/pluginlib` | `io.github.khezyapp.pluginlib` |
| string-util | `utils/string-util` | `io.github.khezyapp.stringutil` |
| dynamic-object | `utils/dynamic-object` | `io.github.khezyapp.doa` |
| clone-util | `utils/clone-util` | `io.github.khezyapp.clone` |
| data-masker | `securities/data-masker` | `io.github.khezyapp.datamasker` |
| simple-prompt-template | `templates/simple-prompt-template` | `io.github.khezyapp.prompttemplate` |
| storage-api | `storage/storage-api` | `io.github.khezyapp.storageapi` |
| storage-fs | `storage/storage-fs` | `io.github.khezyapp.storagefs` |

Package naming: module dir may contain hyphens, but **Java package name never does** — strip/compress them (e.g. `data-masker` -> `datamasker`, `string-util` -> `stringutil`, `storage-api` -> `storageapi`, `plugin-lib` -> `pluginlib`).

Build-logic lives in `build-logic/` as a separate included build with convention plugins (`khezy.*`).

## Build system conventions

- **Gradle 8.14.5** wrapper, **JDK 21** toolchain, `--release 17` target bytecode
- Convention plugins in `build-logic/src/main/groovy/`:
  - `khezy.java-library` — **always applied** (aggregates base-java-library + junit5 + maven-publish + code-quality). **Do NOT apply `java-library` directly**.
  - `khezy.java-lombok` — **opt-in** (Lombok 1.18.42, SLF4J 2.0.17); only clone-util and data-masker tests use Lombok
- Checkstyle 13.1.0 always enforced (custom rules in `build-logic/src/main/resources/config/checkstyle/checkstyle.xml`); Javadoc lint suppressed
- Publishing via `com.vanniktech.maven.publish` plugin to Maven Central

### Module `build.gradle` recipe

```groovy
plugins {
    id("khezy.java-library")
    // id("khezy.java-lombok")  // only when Lombok needed
}

group = "io.github.khezyapp.<group-suffix>"
version = "1.0.0"

mavenPublishing {
    pom {
        name = 'Module Name'
        description = """Description."""
    }
}
```

### Module `settings.gradle` recipe

```groovy
pluginManagement {
    includeBuild("../../build-logic")
}
rootProject.name = "<module-name>"
```

## Java coding conventions

### Formatting
- 4-space indentation, no tabs (Checkstyle `FileTabCharacter`)
- 120-char line limit (`LineLength`)
- Egyptian/OTBS brace style (opening brace on same line, closing on its own)
- Braces always required even for single-statement blocks (`NeedBraces`)
- Files end with newline, no trailing whitespace
- UTF-8 encoding

### `final` keyword
- **All method parameters** are `final` (enforced by Checkstyle)
- **All local variables** use `final var` (universal pattern)
- Instance fields are `final` when possible
- Method parameters: `public void foo(final String bar)`

### `var` usage
- **Universal** for local variables, even in complex generic contexts
- Always combined with `final`: `final var x = ...`
- Examples: `final var list = new ArrayList<T>()`, `final var info = cls.getAnnotation(...)`

### Naming

| Element | Convention | Example |
|---|---|---|
| Classes/Interfaces | PascalCase | `PluginManager`, `InMemoryPluginStore` |
| Methods | camelCase | `loadPlugins()`, `loadEager()` |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_VERSION`, `KEY_SEPARATOR` |
| Fields | camelCase | `type`, `dir`, `recursive` |
| Parameters | camelCase | `type`, `dir`, `recursive` |
| Type params | single uppercase | `T`, `K`, `V` |

### Imports
- No star imports in production code (allowed in tests for `Assertions.*`)
- No unused or redundant imports (enforced)
- Standard order: JDK first, then third-party, then project

### Exception handling
- Catch param always `final e`: `} catch (final IOException e) {`
- Wrap checked in `RuntimeException` with descriptive message
- Ignore trivial exceptions with `// ignore` or `// ignore close exception` comment
- No custom exception classes (except `ShellExecutionException` in templates module)

### Nullability
- No JSR-305 annotations (`@Nullable`/`@NonNull`)
- `Objects.requireNonNull(value, "message")` for parameter validation
- `Optional` for return types that may be empty
- `Objects.isNull` / `Objects.nonNull` for conditional checks

### Annotations
- Single annotation per line
- `@Override` on all overriding methods
- `@Test` + `@DisplayName("Human readable")` on every test
- `@SuppressWarnings("unchecked")` for unchecked casts

## Class structure

### Field ordering
1. `private static final` constants
2. `private final` instance fields
3. Mutable instance fields
4. No blank lines between fields usually

### Method ordering
1. Static factory methods / constructors
2. Public API methods (grouped by feature)
3. `@Override` implementations
4. Private helpers (bottom)
5. Inner classes / records / builders (very bottom)

### Constructor patterns
- Records: compact constructors for validation
- Utility classes: `private ClassName() { }`
- Static factory methods preferred: `PluginManager.of(type)` over `new PluginManager<>()`
- `Objects.requireNonNull` for parameter validation
- Builder pattern for configuration-heavy classes (inner `Builder` class, fluent setters, terminal `build()`)

## Key language patterns

- **Records** for data carriers: `public record Foo<T>(String name, T value) { }`
  - Compact constructors for validation + defensive copies
- **Sealed interfaces** with record implementations for discriminated unions
- **`@FunctionalInterface`** for single-method interfaces
- **Strategy pattern**: `support()` + `copy()`/`mask()` method pairs
- **Static facade**: final class, private constructor, static delegation
- **`synchronized`** sparingly (lazy init only)
- **`ConcurrentHashMap`** + **`volatile`** for thread-safe caching
- **`Collections.unmodifiable*`** / **`List.copyOf`** for defensive returns

## Test conventions

- Test class: `{ClassName}Test`, package-private (`class FooTest`)
- Mirrors source package structure
- `@Test` + `@DisplayName("Should ...")` on every test method
- JUnit 5 assertions via static import `org.junit.jupiter.api.Assertions.*`
- `assertEquals(expected, actual)` order
- `@Nested` for grouping related tests
- `@TempDir` for temporary directories
- `@BeforeEach` for setup
- `@ParameterizedTest` + `@MethodSource`, `@ValueSource`, `@CsvSource`
- Anonymous lambda loaders for test PluginLoader: `(PluginLoader<T>) () -> List.of(...)`
- Helper classes as static inner classes in test file
- Helper methods: `private static`
- Helper providers: `static Stream<Arguments> xxxProvider()`
