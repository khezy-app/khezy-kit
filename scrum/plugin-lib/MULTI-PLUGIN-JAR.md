# Multi-Plugin Per JAR — Analysis & Plan

## 1. Use case

A single JAR can provide multiple implementations of the same SPI (e.g. `JsonFormatter`,
`XmlFormatter` both implementing `TextProcessor`). Each provider should be independently
discoverable, named, and versioned.

ServiceLoader supports this natively — `META-INF/services/io.myapp.TextProcessor` lists
both implementation classes. The current code correctly discovers all of them.

## 2. What currently works

| Scenario | Works? | Detail |
|----------|--------|--------|
| Multiple providers in one JAR, discovered via ServiceLoader | ✅ | `ServiceLoader.load()` returns all providers |
| Different class names → different candidate names | ✅ | Each gets `getSimpleName()` |
| Deduplication across loaders | ✅ | `CompositePluginLoader` keys by `name:version:className` |

## 3. What is broken or missing

### 3.1 `PluginCandidate.fromProvider()` sets version to class name (BUG)

```java
// PluginCandidate.java:20
return new PluginCandidate<>(name, provider.type().getSimpleName(), provider.type());
//                                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                                   This is the CLASS NAME, not a version!
```

Used by `ServiceLoaderPluginLoader` only. `DirectoryPluginLoader` creates candidates
directly and is unaffected. The fix is trivial — default to `"1.0.0"`.

### 3.2 Version is JAR-level, not provider-level

`DirectoryPluginLoader.resolveVersion()` reads `Plugin-Version` from the JAR manifest.
Every provider in the same JAR gets the same version. There is no mechanism for a
provider class to declare its own version.

### 3.3 Name is always the class simple name

Both loaders use `providerClass.getSimpleName()` as the name. A provider class like
`com.example.JsonFormatterV2` gets name `"JsonFormatterV2"`. There is no way to give
a meaningful short name without renaming the class.

### 3.4 No per-provider metadata

There is no way to attach `description`, `vendor`, or `displayName` to a provider class.
These would have to be set at install time via `PluginManager.install()` parameters.

### 3.5 No source-JAR tracking

`PluginCandidate` does not record which JAR a provider came from. If two JARs contain
providers with the same class name, the deduplication in `CompositePluginLoader` may
hide the conflict.

## 4. Design options for provider-level metadata

### Option A: Annotation on provider class (recommended)

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginInfo {
    String name();
    String version() default "1.0.0";
    String description() default "";
    String vendor() default "";
}
```

**Pros**: Per-provider metadata. No instantiation required — annotation is read from
`Class` object at discovery time. Compatible with lazy loading. Familiar pattern
(Spring `@Component`, JAX-RS `@Path`).

**Cons**: Requires the annotation JAR as a compile-time dependency for plugin authors.

### Option B: Properties file inside JAR

A `META-INF/plugins/` directory with one `.properties` file per provider class, e.g.
`META-INF/plugins/com.example.JsonFormatter.properties`:

```properties
name=json-formatter
version=2.1.0
description=JSON formatter plugin
vendor=Acme Corp
```

**Pros**: No library dependency for plugin authors. Metadata lives alongside the class.
**Cons**: Discovery requires scanning for `.properties` files. No compile-time validation.

### Option C: Extend `PluginCandidate` with metadata

Add optional metadata fields directly to `PluginCandidate`:

```java
public record PluginCandidate<T>(
    String name,
    String version,
    Class<? extends T> providerClass,
    String description,
    String vendor
) {}
```

**Pros**: Straightforward. Works with any metadata source.
**Cons**: Every caller must provide (or default) the extra fields. Breaks existing callers.

## 5. Recommended plan

| Priority | Task | Impact | Effort |
|----------|------|--------|--------|
| **P0** | Fix `PluginCandidate.fromProvider()` version bug | Restores correct version for ServiceLoader path | 1 file, 1 line |
| **P0** | Fix `ServiceLoaderPluginLoader` to use `"1.0.0"` default | Restores correct version for classpath path | 1 file, 2 lines |
| **P1** | Add `@PluginInfo` annotation | Enables per-provider name + version + metadata | 1 new file |
| **P1** | Update `DirectoryPluginLoader` to read `@PluginInfo` | Applies annotation on directory-scan path | 1 file |
| **P1** | Update `ServiceLoaderPluginLoader` to read `@PluginInfo` | Applies annotation on classpath path | 1 file |
| **P2** | Add `PluginCandidate.sourceJar` field | Tracks originating JAR for dedup/audit | 2 files |
| **P3** | Add `PluginCandidate.description` / `vendor` fields | Richer metadata on candidate | 2 files |

### P0 — Bug fixes (do now)

```java
// PluginCandidate.java
public static <T> PluginCandidate<T> fromProvider(
        final String name,
        final ServiceLoader.Provider<T> provider) {
    return new PluginCandidate<>(name, "1.0.0", provider.type());
}

// ServiceLoaderPluginLoader.java — version is now "1.0.0" from fromProvider, 
// no change needed in the loader itself if fromProvider is fixed
```

### P1 — Annotation support

New file: `PluginInfo.java`

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginInfo {
    String name();
    String version() default "1.0.0";
    String description() default "";
    String vendor() default "";
}
```

Update `DirectoryPluginLoader.java`:

```java
for (final var provider : providers) {
    final var providerClass = provider.type();
    final var info = providerClass.getAnnotation(PluginInfo.class);
    final var name = info != null ? info.name() : providerClass.getSimpleName();
    final var version = info != null ? info.version() : resolveVersion(providerClass);
    final var description = info != null ? info.description() : "";
    final var vendor = info != null ? info.vendor() : "";
    candidates.add(new PluginCandidate<>(name, version, providerClass));
}
```

## 6. Priority recommendation

1. **Fix P0 bugs immediately** — they affect correctness
2. **Implement P1 annotations** — enables the multi-plugin-per-jar use case with
   per-provider identity
3. **Evaluate P2/P3** — source JAR tracking and richer candidate metadata; can wait
   until a consumer needs them
