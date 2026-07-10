# Plugin Lib — Generic Plugin Discovery & Loading for Java

A zero-dependency library for discovering, loading, and managing plugins in any Java application. Plugin Lib supports any user-defined SPI type using the standard `ServiceLoader` mechanism, directory-based JAR scanning with classloader isolation, version-aware lookups, and optional metadata persistence.

## Overview

Building a plugin system means solving several problems:

- **Discovery** — finding plugin implementations on the classpath, in JAR files, or from remote locations.
- **Classloader isolation** — preventing plugin JARs from conflicting with each other or with the host application.
- **Version management** — supporting multiple versions of the same plugin and resolving the "latest" version.
- **Lifecycle** — loading plugins eagerly or lazily, enabling/disabling them, and tracking metadata.

Plugin Lib provides all of this out of the box with **no external dependencies**. You bring your own plugin interface (a plain Java interface or abstract class), and Plugin Lib handles the rest.

## Quick Start

### Maven

```xml
<dependency>
    <groupId>io.github.khezyapp</groupId>
    <artifactId>pluginlib</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.khezyapp:pluginlib:1.0.0'
```

### Minimal Example

Define your plugin interface:

```java
public interface Greeter {
    String greet(String name);
}
```

Write an implementation and annotate it:

```java
@PluginInfo(name = "friendly", version = "1.0.0", description = "A friendly greeter")
public class FriendlyGreeter implements Greeter {
    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
```

Register it in `META-INF/services/io.khezyapp.demo.Greeter`:

```
io.khezyapp.demo.FriendlyGreeter
```

Then load and use it:

```java
PluginManager<Greeter> manager = PluginManager.of(Greeter.class)
    .classpath()
    .build();

List<Greeter> plugins = manager.loadEager();
plugins.forEach(p -> System.out.println(p.greet("World")));
```

## Core Concepts

Plugin Lib is built around a few core abstractions. Understanding them makes everything else fall into place.

### Plugins, Candidates, and Installed Plugins — Three Layers of Representation

The library distinguishes three stages of a plugin's lifecycle:

| Stage | Type | Meaning |
|---|---|---|
| **Discovered** | `PluginCandidate<T>` | A potential plugin that has been found (name + version + provider class), but not yet instantiated. |
| **Installed** | `InstalledPlugin` | Metadata record stored in the `PluginStore` after a candidate has been registered. Includes name, version, display name, description, vendor, source, enabled/disabled status, timestamps, and custom attributes. This is purely metadata — no plugin instance. |
| **Instantiated** | `T` (your type) | A live instance created from a `PluginCandidate` via `newInstance()`. |

The separation means you can discover plugins, inspect their metadata, and decide whether to instantiate them later.

### Discovery — PluginLoader

`PluginLoader<T>` is the strategy interface for finding plugins. It has one method:

```java
List<PluginCandidate<T>> loadPlugins();
```

Plugin Lib ships with three concrete implementations, plus a static factory on the `PluginLoader` interface:

| Loader | Factory method | What it does |
|---|---|---|
| `ServiceLoaderPluginLoader` | `PluginLoader.serviceLoader(type)` | Scans the classpath via `java.util.ServiceLoader`. |
| `DirectoryPluginLoader` | `PluginLoader.directory(type, dir, recursive, ...)` | Scans a directory for JAR files, loads each with an isolated classloader, then uses `ServiceLoader` inside each JAR. |
| `CompositePluginLoader` | `PluginLoader.composite(loaders)` | Chains multiple loaders together and deduplicates results. |

### Version-Aware Lookups

The `PluginManager` and `InMemoryPluginStore` both understand versions. You can:

- Query a specific version: `manager.get("my-plugin", "2.1.0")`
- Get the latest version (by semver comparison): `manager.get("my-plugin")` (returns highest version)
- List all versions of a plugin: `manager.getAll("my-plugin")`

Version comparison follows `major.minor.patch` — non-numeric segments are treated as `0`.

### Plugin Registry — PluginStore

`PluginStore` is an interface for persisting plugin metadata. It stores `InstalledPlugin` records and supports install, uninstall, lookup, enable/disable, and enumerate operations.

The default implementation is `InMemoryPluginStore` (backed by `ConcurrentHashMap`). You can provide your own (e.g., backed by a database) and pass it to the `PluginManager` builder:

```java
PluginManager<Greeter> manager = PluginManager.of(Greeter.class)
    .store(myDatabaseStore)
    .build();
```

### @PluginInfo Annotation

`@PluginInfo` is a runtime-retained annotation you place on your plugin implementation classes. It supplies the metadata that loaders need to build `PluginCandidate` records:

```java
@PluginInfo(name = "my-plugin", version = "2.1.0",
            description = "Does something useful", vendor = "Acme Corp")
public class MyPlugin implements SomeSpi { ... }
```

## Usage Guide

### Classpath Discovery (ServiceLoader)

If all your plugins are on the application classpath, use the `ServiceLoaderPluginLoader`:

```java
PluginManager<Greeter> manager = PluginManager.of(Greeter.class)
    .classpath()
    .build();
```

This discovers implementations registered in `META-INF/services/` files. Each provider class is scanned for `@PluginInfo`; if none is found, the class simple name and version `"1.0.0"` are used as defaults.

### Directory Discovery (JAR Scanning)

To load plugins from JAR files in a directory:

```java
PluginManager<Greeter> manager = PluginManager.of(Greeter.class)
    .directory(Path.of("/opt/myapp/plugins"), true)
    .build();
```

Each JAR is loaded with a dedicated `PluginClassLoader`, providing isolation between plugins. If multiple loaders are configured, results are merged and deduplicated.

### Combining Classpath and Directory

You can chain both sources — the `Builder` aggregates everything into a `CompositePluginLoader`:

```java
PluginManager<Greeter> manager = PluginManager.of(Greeter.class)
    .classpath()
    .directory(Path.of("/opt/myapp/plugins"))
    .directory(Path.of("/opt/myapp/extra-plugins"), false)
    .build();
```

The order matters: earlier sources take priority when duplicates are detected (dedup key: `name:version:className`).

### Adding a Custom PluginLoader

If you have a custom discovery strategy (e.g., scanning a database or a remote URL), implement `PluginLoader<T>` and add it via `.loader()`:

```java
PluginManager<Greeter> manager = PluginManager.of(Greeter.class)
    .classpath()
    .loader(new RemotePluginLoader<>(Greeter.class, "https://plugins.example.com"))
    .build();
```

### Eager vs Lazy Loading

Call `loadEager()` to discover and instantiate every plugin immediately:

```java
List<Greeter> all = manager.loadEager();
```

Call `loadLazy()` to get a `Stream` that instantiates plugins one at a time as the stream is consumed:

```java
try (Stream<Greeter> stream = manager.loadLazy()) {
    stream.forEach(p -> System.out.println(p.greet("World")));
}
```

Lazy loading is useful when some plugins are expensive to initialize and you may not need all of them.

### Lookups After Loading

Once plugins are loaded, you can query them individually:

```java
// Highest version of "friendly"
manager.get("friendly").ifPresent(g -> System.out.println(g.greet("Alice")));

// Exact version
manager.get("friendly", "2.0.0")
    .orElseThrow(() -> new RuntimeException("Plugin not found"));

// All versions
List<Greeter> allFriendly = manager.getAll("friendly");

// All currently instantiated plugins
List<Greeter> all = manager.getPlugins();
```

### Access the PluginStore

The `PluginStore` is available for direct inspection of installed metadata:

```java
PluginStore store = manager.getStore();
List<InstalledPlugin> allMetadata = store.list();
store.setEnabled("friendly", "1.0.0", false);
```

## The @PluginInfo Annotation

`@PluginInfo` is the primary way to attach metadata to a plugin implementation. Place it on the concrete provider class:

```java
@PluginInfo(
    name = "analytics",
    version = "3.0.1",
    description = "Collects and aggregates usage metrics",
    vendor = "Khezy Inc."
)
public class AnalyticsPlugin implements ReportPlugin { ... }
```

**Fields:**

| Field | Required | Default | Description |
|---|---|---|---|
| `name` | Yes | — | Unique identifier for the plugin |
| `version` | No | `"1.0.0"` | Plugin version string |
| `description` | No | `""` | Short description of the plugin's purpose |
| `vendor` | No | `""` | Organisation or author |

The annotation is read at runtime by both `ServiceLoaderPluginLoader` and `DirectoryPluginLoader`.

If a provider class is **not** annotated, the loader falls back to the class simple name as the plugin name and resolves the version from the next priority source (see below).

## Version Resolution Priority

When determining a plugin's version string, the library applies this priority order:

1. **@PluginInfo annotation** — the `version()` field on the provider class takes precedence.
2. **JAR Manifest** — the `Plugin-Version` attribute in the JAR's `MANIFEST.MF` file. Only applicable for directory-loaded JARs.
3. **Default** — `"1.0.0"` is used when neither annotation nor manifest is available.

This lets plugin authors override the manifest version at the class level when needed.

## Classloader Isolation

When loading plugins from JAR files, `DirectoryPluginLoader` uses a dedicated `PluginClassLoader` for each JAR (all JARs share a single loader, but the loader is isolated from the application classpath).

`PluginClassLoader` implements a **delegate-first** (parent-last with exceptions) strategy:

1. Check if the class is already loaded.
2. If the class name starts with a delegate-first prefix (default: `java.`, `javax.`, `jdk.`, `sun.`), try the parent loader first.
3. Try to find the class locally (from the JAR).
4. Fall back to the normal `URLClassLoader` delegation.

This ensures that core JDK classes are always resolved by the parent class loader, while plugin classes are resolved from the JAR first. You can customise the delegate-first prefixes:

```java
PluginManager.of(MySpi.class)
    .directory(Path.of("/plugins"), true, "java.", "jakarta.")
    .build();
```

## Installation API — Manual Plugin Registration

`PluginManager` supports manual installation of plugins that weren't discovered through any loader. This is useful for registering external plugins at runtime:

```java
PluginCandidate<Greeter> candidate =
    new PluginCandidate<>("external", "1.0.0", ExternalGreeter.class);

manager.install(candidate,
    new PluginSource.FileSource(Path.of("/tmp/external.jar")),
    "External Greeter Plugin",
    "Third Party Inc.");
```

The `install()` method adds the candidate to the internal registry and records a corresponding `InstalledPlugin` in the `PluginStore`. The plugin is instantiated lazily on first access via `get()` or `getAll()`.

## Building from Source

```bash
./gradlew :pluginlib:build
```

Requires JDK 21 (targets bytecode level 17) and applies the `khezy.java-library` convention plugin (Checkstyle + JUnit 5 + Maven publishing).

## License

This project is part of the Khezy ecosystem. See the root project for license information.
