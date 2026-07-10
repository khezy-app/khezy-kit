# PluginStore Redesign — From Discovery Cache to Plugin Registry

## 1. Problem

The current `PluginStore` interface does not model the concept of **installing** plugins.
It treats the store as a passive cache for discovery results (`saveCandidate`) and carries
KYC-specific execution provenance (`saveExecution`/`loadExecutions`).

### Root cause

The interface was directly influenced by the `poc-plugins` KYC workflow design — `ExecutionRecord`
with `workflowId`, `status`, and `executedAt` is a domain concept, not a generic plugin concern.
The existing design doc §10 explicitly excludes PluginContext/PluginResult as KYC-specific, yet
`ExecutionRecord` was included anyway, creating a contradiction.

### Concrete issues

| Issue | Current API | Problem |
|-------|-------------|---------|
| Wrong semantics | `saveCandidate(candidate)` | A "candidate" is a transient discovery artifact; storing it implies installation, but the name says "maybe". Consumers cannot distinguish "discovered but not installed" from "installed". |
| KYC leak | `saveExecution(record)` / `loadExecutions(wfId)` | `workflowId`, `status`, `executedAt` — domain-specific. Violates §10 of the design doc. |
| Weak metadata | `PluginMetadata(name, version, description, enabled, settings)` | No `author`, `installPath`, `installedAt`, `license`. The `settings` map is a catch-all with no schema. |
| No lifecycle | `delete(name, version)` → no `install`/`uninstall` | The interface has `delete` but no symmetric `install`. Installation is currently an implicit side-effect of discovery inside `PluginManager.ensureLoaded()`. |
| No state | implicit `enabled` boolean only | Cannot enable/disable after installation; no `setEnabled()` method. |
| No source tracking | nothing | Doesn't track where a plugin was installed from (file path, classpath, URL). |

---

## 2. What "Plugin Installation" means

A VS Code extension / IntelliJ plugin / WordPress plugin installation means:

```
User chooses a plugin → System downloads/copies it → Registry records it → 
Plugin appears in listings → Can be enabled/disabled → Can be uninstalled later
```

The **registry** is the source of truth. Discovery (scanning directories, reading classpath)
is a *separate* concern that feeds into the registry but does not replace it.

### Two-tier model

```
 ┌──────────────┐     discovery      ┌──────────────────┐     install      ┌──────────────┐
 │  PluginLoader │ ────────────────── │ PluginCandidate  │ ───────────────→ │  PluginStore │
 │  (finds jars) │                    │ (ephemeral info) │                  │  (registry)  │
 └──────────────┘                     └──────────────────┘                  └──────────────┘
                                                                                 │
                                                                          query at startup
                                                                                 │
                                                                                 ▼
                                                                         ┌──────────────┐
                                                                         │  Load actual  │
                                                                         │  classes via  │
                                                                         │  PluginLoader │
                                                                         └──────────────┘
```

1. **Discovery** (`PluginLoader.loadPlugins()`) — scans sources, returns `PluginCandidate<T>`
2. **Install** (registry) — user/system selects a candidate and registers it permanently
3. **Load** — at runtime, query the registry for installed plugins, then load via `PluginLoader`

---

## 3. Proposed redesign

### 3a. Remove `ExecutionRecord` entirely

Delete the class and its methods from `PluginStore`. Execution provenance is the consumer's
responsibility — they can build it on top using whatever storage they want.

### 3b. Replace `PluginMetadata` with richer `InstalledPlugin`

```java
public record InstalledPlugin(
    String id,              // unique identifier, e.g. "publisher.my-plugin"
    String name,            // simple name, e.g. "my-plugin"
    String version,
    String displayName,     // human-readable, e.g. "My Awesome Plugin"
    String description,
    String vendor,          // publisher/author
    PluginSource source,    // where it lives
    boolean enabled,
    Instant installedAt,
    Instant updatedAt,
    Map<String, String> attributes   // extensible metadata (license, homepage, etc.)
) {}

public sealed interface PluginSource {
    record ClasspathSource() implements PluginSource {}
    record FileSource(Path jarPath) implements PluginSource {}
    record UrlSource(URL url) implements PluginSource {}
}
```

If the full `InstalledPlugin` record feels too heavy, a minimal viable version:

```java
public record InstalledPlugin(
    String name,
    String version,
    String displayName,
    Path sourcePath,
    boolean enabled,
    Instant installedAt
) {}
```

### 3c. New `PluginStore` interface

```java
public interface PluginStore {

    // ── Install lifecycle ──
    InstalledPlugin install(InstalledPlugin plugin);
    void uninstall(String name, String version);
    void uninstallAll(String name);

    // ── Query ──
    List<InstalledPlugin> list();
    Optional<InstalledPlugin> get(String name, String version);
    List<InstalledPlugin> getAll(String name);
    Optional<InstalledPlugin> getLatest(String name);

    // ── State ──
    void setEnabled(String name, String version, boolean enabled);

    // ── Maintenance ──
    void clear();
}
```

### 3d. Impact on `PluginManager`

The `PluginManager.Builder.store()` stays, but the store is no longer implicitly populated
by `ensureLoaded()`. Instead:

```java
// New — explicit installation from discovered candidates
void install(PluginCandidate<T> candidate, Path sourcePath, String displayName);

// Existing — loads only what's in the store
List<T> loadEager();
Stream<T> loadLazy();
```

The manager queries the store for installed plugins, then uses the loader to resolve and
instantiate them. Uninstalled-but-discovered plugins are simply ignored.

---

## 4. Migration impact

| File | Change |
|------|--------|
| `ExecutionRecord.java` | **Delete** — out of scope |
| `PluginMetadata.java` | **Delete** — replaced by `InstalledPlugin` |
| `PluginCandidate.java` | **Unchanged** — still needed for discovery |
| `PluginStore.java` | **Rewrite** — new interface above |
| `InMemoryPluginStore.java` | **Rewrite** — implement new interface |
| `PluginManager.java` | **Update** — separate install from discovery |
| Tests | **Update** — remove execution tests, add install/uninstall tests |

---

## 5. Open questions

1. **`sourcePath` granularity** — Should the store track individual JAR paths, or just the
   directory? VS Code tracks the extension folder; individual files are resolved at load time.
   Proposed: track the install root (directory or JAR path), not individual class files.

2. **`install` returns vs void** — Returning `InstalledPlugin` allows the implementation to
   enrich the record (e.g., set server-generated timestamps). This matches common repository
   patterns (JPA `save` returns the entity).

3. **Thread safety** — Current `InMemoryPluginStore` uses `ConcurrentHashMap`. New interface
   should specify thread-safety contract in Javadoc.

4. **`getLatest` version comparison** — Should use the same semver logic from `PluginManager.Version`.
   Could extract a shared `Version` utility.

5. **Discovery vs Installation coupling** — Currently `PluginManager.ensureLoaded()` calls
   `store.saveCandidate()`. In the new model, should `PluginManager` still auto-install
   discovered plugins? **Decision: No.** Installation should be explicit. The manager's
   `loadEager()`/`loadLazy()` should only load plugins that are already in the store.
   A convenience method like `discoverAndInstall()` could be added as shorthand.

---

## 6. Recommendation

Adopt the redesigned `PluginStore` with `install`/`uninstall` semantics and remove
`ExecutionRecord`. This aligns the library with its stated scope (§10) and makes it
a proper foundation for VS Code–style plugin management.
