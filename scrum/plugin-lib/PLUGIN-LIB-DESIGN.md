# Plugin Lib — Generic Plugin Loading Library

Design document for extracting a reusable generic library from the `poc-plugins` POC.

---

## 1. Goal

Provide a single dependency that handles **plugin discovery + loading + classloader isolation** for any user-defined SPI type `T`, so developers never need to write `URLClassLoader`, `ServiceLoader`, or parent-last delegation code again.

```java
// Define your own plugin contract
interface TextProcessor {
    String process(String input);
}

// Use the library
PluginManager<TextProcessor> engine = PluginManager.of(TextProcessor.class)
    .classpath()
    .directory(Path.of("extensions"))
    .build();

List<TextProcessor> all = engine.loadEager();          // all now
Stream<TextProcessor> lazy = engine.loadLazy();         // on-demand
TextProcessor v2 = engine.get("my-plugin", "2.0.0");   // by name + version
```

---

## 2. Package

```
io.github.khezyapp.pluginlib
```

---

## 3. Core API

### 3.1 `PluginLoader<T>` — extension point

```java
@FunctionalInterface
public interface PluginLoader<T> {
    List<PluginCandidate<T>> loadPlugins();

    static <T> PluginLoader<T> serviceLoader(Class<T> type);
    static <T> PluginLoader<T> directory(Class<T> type, Path dir, boolean recursive, String... delegateFirst);
    static <T> PluginLoader<T> composite(List<PluginLoader<T>> loaders);
}
```

Returns `PluginCandidate<T>` (not `T`) to support versioning before instantiation. `PluginCandidate` holds metadata needed for lazy loading.

### 3.2 `PluginCandidate<T>` — discovered but not yet instantiated

```java
public record PluginCandidate<T>(
    String name,
    String version,
    Class<? extends T> providerClass
) {
    /** Optionally extract version from class manifest */
    static <T> PluginCandidate<T> fromProvider(
        String name, ServiceLoader.Provider<T> provider
    ) { ... }

    T newInstance() {
        return providerClass.getConstructor().newInstance();
    }
}
```

### 3.3 `PluginManager<T>` — orchestrator

```java
public class PluginManager<T> implements AutoCloseable {

    // --- Builder ---
    public static <T> Builder<T> of(Class<T> type);

    public static class Builder<T> {
        public Builder<T> classpath();
        public Builder<T> directory(Path dir);
        public Builder<T> directory(Path dir, boolean recursive, String... delegateFirstPrefixes);
        public Builder<T> loader(PluginLoader<T> custom);
        public Builder<T> namedBy(Function<T, String> nameFn);
        public Builder<T> versionedBy(Function<T, String> versionFn);
        public Builder<T> store(PluginStore store);
        public PluginManager<T> build();
    }

    // --- Loading modes ---
    public List<T> loadEager();
    public Stream<T> loadLazy();

    // --- Access (eager or lazy) ---
    public List<T> getPlugins();
    public Optional<T> get(String name);
    public Optional<T> get(String name, String version);
    public List<T> getAll(String name);   // all versions of a named plugin

    // --- Store ---
    public PluginStore getStore();

    // --- Lifecycle ---
    @Override public void close();
}
```

**Default configuration (the "best option"):**
- Loading mode: **lazy** — discover metadata first, cache instances on first access
- Directory scan: **recursive** — scans `dir/**/*.jar`
- Naming: uses `NamedPlugin` interface if `T` extends it, otherwise falls back to `simpleClassName`

### 3.4 `PluginClassLoader` — generic parent-last URLClassLoader

```java
public class PluginClassLoader extends URLClassLoader {
    // DELEGATE_FIRST_PREFIXES is configurable via constructor
    // Defaults: java., javax., jdk., sun.
    // User adds their SPI package to avoid class cast conflicts

    public PluginClassLoader(URL[] urls, ClassLoader parent, String... delegateFirstPackages);
}
```

---

## 4. Lazy loading semantics

```
loadLazy():
  1. Each PluginLoader<T> returns List<PluginCandidate<T>>
     — reads class names without calling getConstructor().newInstance()
  2. PluginManager stores candidates in PluginStore
  3. Returns Stream<T> backed by:
     - ConcurrentHashMap<String, Supplier<T>> (one per name+version)
     - On first terminal op: calls PluginCandidate.newInstance() + caches
     - Subsequent calls return cached instance
  4. Thread-safe: ConcurrentHashMap.computeIfAbsent

loadEager():
  1. Same discovery via PluginLoader
  2. Instantiates ALL candidates immediately
  3. Full list in memory
```

**Lazy is the default.** Reason: plugin JARs may contain heavy initialization (ML models, DB connections). Users who don't need all plugins benefit from deferred instantiation. Call `loadEager()` if fail-fast at startup is preferred.

**PluginClassLoader lifecycle in lazy mode:**
- ClassLoader is created during `loadLazy()` and kept open
- Closed when `manager.close()` is called
- This means JARs remain open for the lifetime of the manager (acceptable for long-running apps; users who need hot-reload can call `close()` + rebuild)

---

## 5. Versioning & backward compatibility

### 5.1 Multi-version coexistence

Multiple versions of the same-named plugin can coexist in the classpath and plugin directory:

```
plugins/
├── my-plugin-1.0.0.jar    ← registers: name="my-plugin", version="1.0.0"
└── my-plugin-2.0.0.jar    ← registers: name="my-plugin", version="2.0.0"
```

### 5.2 How version is determined

Priority order:
1. **Manifest attribute** — `Plugin-Version` in `META-INF/MANIFEST.MF` inside the JAR
2. **ServiceLoader Provider** — `ServiceLoader.Provider<T>.type()` annotation (if using `@AutoService` with version metadata)
3. **Default** — `"1.0.0"` if nothing else declares it

### 5.3 Access API

```java
// Latest version (highest semver)
Optional<T> plugin = engine.get("my-plugin");

// Specific version
Optional<T> plugin = engine.get("my-plugin", "1.0.0");

// All versions
List<T> allVersions = engine.getAll("my-plugin");
```

Version comparison uses semantic versioning (`org.semver` or built-in comparator).

### 5.4 Registration deduplication

When registering, if the same (name, version) tuple appears from both classpath and directory:

| Scenario | Behavior |
|----------|----------|
| Same name, different version | Both registered (coexist) |
| Same name + version, different jar | First wins (classpath > directory by default) |
| Same name + version, same jar | Deduplicated by class name |

---

## 6. Built-in loaders

### 6.1 `ServiceLoaderPluginLoader<T>`

```java
new ServiceLoaderPluginLoader<>(TextProcessor.class)
// wraps ServiceLoader.load(TextProcessor.class)
// returns PluginCandidate for each provider
```

Uses `ServiceLoader.Provider<T>` to read class name without instantiating (supports lazy).

### 6.2 `DirectoryPluginLoader<T>`

```java
new DirectoryPluginLoader<>(TextProcessor.class, Path.of("extensions"),
    /*recursive=*/true,
    /*delegateFirst=*/"io.github.khezyapp.pluginlib.api"
)
```

Steps:
1. Walk directory (recursive by default, configurable) → collect `*.jar`
2. Create `PluginClassLoader` with JAR URLs + parent + delegate prefixes
3. `ServiceLoader.load(type, pluginClassLoader)` → `PluginCandidate` list
4. Close classloader in eager mode; keep open in lazy mode

### 6.3 `CompositePluginLoader<T>`

```java
new CompositePluginLoader<>(List.of(classpathLoader, dirLoader))
// concatenates, deduplicates by (name, version)
```

---

## 7. `NamedPlugin` — opt-in naming interface

Included in the library so `manager.get("name")` works out of the box:

```java
package io.github.khezyapp.pluginlib;

public interface NamedPlugin {
    String name();
    default String version() { return "1.0.0"; }
}
```

If `T extends NamedPlugin`, the builder auto-detects and uses it. Otherwise the user provides `namedBy(...)` and `versionedBy(...)` functions.

---

## 8. `PluginStore` — optional persistence

Becomes version-aware: `PluginMetadata` key is `(name, version)` instead of just `name`.

```java
public interface PluginStore {
    void saveCandidate(PluginCandidate<?> candidate);
    Optional<PluginMetadata> load(String name, String version);
    List<PluginMetadata> loadAll();
    List<PluginMetadata> loadByName(String name);
    void delete(String name, String version);
    void saveExecution(ExecutionRecord record);
    List<ExecutionRecord> loadExecutions(String workflowId);
    void clear();
}
```

`PluginMetadata` updated:

```java
public record PluginMetadata(
    String name,
    String version,
    String description,
    boolean enabled,
    Map<String, Object> settings
) {}
```

---

## 9. Package structure

```
io.github.khezyapp.pluginlib/
├── PluginLoader.java               ← @FunctionalInterface
├── PluginManager.java              ← builder + orchestrator
├── PluginCandidate.java            ← record (name, version, providerClass)
├── PluginClassLoader.java          ← parent-last URLClassLoader
├── NamedPlugin.java                ← opt-in naming interface
├── ServiceLoaderPluginLoader.java
├── DirectoryPluginLoader.java
├── CompositePluginLoader.java
├── PluginStore.java                ← optional persistence interface
├── InMemoryPluginStore.java        ← default
├── PluginMetadata.java             ← record
└── ExecutionRecord.java            ← record
```

---

## 10. What is NOT included

| Feature | Reason |
|---------|--------|
| `init()` / `destroy()` lifecycle | Domain-specific — consumer implements via their own `Plugin` interface |
| YAML config | Domain-specific — consumer implements their own config model |
| `PluginContext` / `PluginResult` | KYC-specific — consumer defines their own execution model |
| Hot-reload | Orthogonal feature — can be built on top using `WatchService` |
| Security sandbox | Orthogonal — JVM `SecurityManager` is separate concern |

The library stays focused on **discovery + loading + isolation + versioning**.

---

## 11. Migration from poc-plugins

The current `poc-plugins/` project becomes a consumer of `pluginlib/`:

```
Before:                     After:
poc-plugins/ (self-contained)  pluginlib/ (standalone library)
  PluginLoader                  ↑ depends on
  PluginManager                 │
  PluginClassLoader             │
                               poc-plugins/ (consumer)
                               └── KYC-specific Plugin interface on top
                               └── uses pluginlib for loading
```

No behavioral change — the KYC `Plugin`, `PluginContext`, `PluginResult`, `GreetingPlugin`, `PluginDemoApp` stay in `poc-plugins`. Only `PluginLoader`, `PluginManager`, `PluginClassLoader`, and `PluginStore` move to the library (or delegate to it).

---

## 12. Implementation order

| Step | What | Files |
|------|------|-------|
| 1 | Create `pluginlib/` composite Gradle module | `settings.gradle`, `build.gradle` |
| 2 | API types | `NamedPlugin`, `PluginCandidate`, `PluginMetadata`, `ExecutionRecord` |
| 3 | ClassLoader | `PluginClassLoader` |
| 4 | Loader interface + built-ins | `PluginLoader`, `ServiceLoaderPluginLoader`, `DirectoryPluginLoader`, `CompositePluginLoader` |
| 5 | Store | `PluginStore`, `InMemoryPluginStore` |
| 6 | Manager | `PluginManager` with builder, eager, lazy, versioned access |
| 7 | Tests | Unit tests for each component |
| 8 | Migration | Refactor `poc-plugins` to depend on `pluginlib` |
