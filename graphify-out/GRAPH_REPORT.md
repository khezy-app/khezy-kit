# Graph Report - .  (2026-07-11)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 863 nodes · 1848 edges · 60 communities (57 shown, 3 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 139 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b7bcda2d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- StringUtilTest
- PluginCandidate
- SensitiveMaskerStrategy
- PluginManager
- DefaultSensitiveMaskerTest
- BeanAdapter
- DisplayName
- InstalledPlugin
- PluginRegistry
- FileSystemStorageService
- Builder
- TemplateContext
- AccessorBenchmark
- CloneContext
- TemplateEngineTest
- TemplateResult
- CloneStrategy
- CompositeTypeAdapter
- CollectionTypeAdapter
- DirectoryPluginLoader
- SecurityConfig
- PathToken
- .deepClone
- TypeAdapter
- ClonesTest.java
- ImmutableStrategy
- ClonesImmutableTest.java
- ObjectAccessor
- .copy
- ListAdapter
- MapAdapter
- .copy
- DisplayName
- gradlew
- dependencies

## God Nodes (most connected - your core abstractions)
1. `StringUtilTest` - 46 edges
2. `PluginCandidate` - 26 edges
3. `StringUtil` - 25 edges
4. `CloneStrategy` - 23 edges
5. `SensitiveMaskerStrategy` - 22 edges
6. `TemplateContext` - 21 edges
7. `BeanAdapter` - 20 edges
8. `TypeAdapter` - 20 edges
9. `PluginRegistry` - 19 edges
10. `PluginManager` - 19 edges

## Surprising Connections (you probably didn't know these)
- `CompositeSensitiveMaskerStrategy` --references--> `SensitiveMaskerStrategy`  [EXTRACTED]
  securities/data-masker/src/main/java/io/github/khezyapp/datamasker/strategy/CompositeSensitiveMaskerStrategy.java → securities/data-masker/src/main/java/io/github/khezyapp/datamasker/api/SensitiveMaskerStrategy.java
- `DefaultSensitiveMasker` --references--> `SensitiveMaskerStrategy`  [EXTRACTED]
  securities/data-masker/src/main/java/io/github/khezyapp/datamasker/strategy/DefaultSensitiveMasker.java → securities/data-masker/src/main/java/io/github/khezyapp/datamasker/api/SensitiveMaskerStrategy.java
- `SensitiveMaskerBuilder` --references--> `SensitiveMaskerStrategy`  [EXTRACTED]
  securities/data-masker/src/main/java/io/github/khezyapp/datamasker/strategy/SensitiveMaskerBuilder.java → securities/data-masker/src/main/java/io/github/khezyapp/datamasker/api/SensitiveMaskerStrategy.java
- `Builder` --references--> `TemplateContext`  [EXTRACTED]
  templates/simple-prompt-template/src/main/java/io/github/khezyapp/templates/plugin/PluginContext.java → templates/simple-prompt-template/src/main/java/io/github/khezyapp/templates/TemplateContext.java
- `PluginContext` --references--> `TemplateContext`  [EXTRACTED]
  templates/simple-prompt-template/src/main/java/io/github/khezyapp/templates/plugin/PluginContext.java → templates/simple-prompt-template/src/main/java/io/github/khezyapp/templates/TemplateContext.java

## Import Cycles
- None detected.

## Communities (60 total, 3 thin omitted)

### Community 0 - "StringUtilTest"
Cohesion: 0.07
Nodes (9): Arguments, NullSource, StringUtil, DisplayName, MethodSource, ParameterizedTest, Test, StringUtilTest (+1 more)

### Community 1 - "PluginCandidate"
Cohesion: 0.06
Nodes (29): Provider, PluginCandidate, Retention, Target, PluginInfo, Override, ServiceLoaderPluginLoader, CompositePluginLoaderTest (+21 more)

### Community 2 - "SensitiveMaskerStrategy"
Cohesion: 0.08
Nodes (21): PropertyDescriptor, Documented, Retention, Target, SensitiveData, SensitiveMaskerContext, SensitiveMaskerStrategy, Field (+13 more)

### Community 3 - "PluginManager"
Cohesion: 0.07
Nodes (13): FunctionalInterface, CompositePluginLoader, Override, PluginLoader, Builder, Override, PluginManager, Version (+5 more)

### Community 4 - "DefaultSensitiveMaskerTest"
Cohesion: 0.11
Nodes (13): SensitiveMasker, DataMaskerUtils, CompositeSensitiveMaskerStrategy, Override, RequiredArgsConstructor, DefaultSensitiveMasker, Override, RequiredArgsConstructor (+5 more)

### Community 5 - "BeanAdapter"
Cohesion: 0.09
Nodes (16): Constructor, Entry, Lookup, MethodHandle, RecordComponent, BeanAdapter, Method, Override (+8 more)

### Community 6 - "DisplayName"
Cohesion: 0.13
Nodes (13): CsvSource, Nested, AccountRecord, Address, BasicAccessTests, DeepAccessTests, DynamicObjectsTest, EdgeCaseTests (+5 more)

### Community 7 - "InstalledPlugin"
Cohesion: 0.13
Nodes (8): InMemoryPluginStore, Override, InstalledPlugin, PluginStore, InMemoryPluginStoreTest, BeforeEach, DisplayName, Test

### Community 8 - "PluginRegistry"
Cohesion: 0.09
Nodes (10): Builder, TemplateConfig, PluginRegistry, PlaceholderResolver, ResolverChain, Override, Pattern, ShellPlaceholderResolver (+2 more)

### Community 9 - "FileSystemStorageService"
Cohesion: 0.11
Nodes (5): SignedUrlOptions, StorageMetadata, StorageService, FileSystemStorageService, Override

### Community 10 - "Builder"
Cohesion: 0.11
Nodes (8): Builder, PluginContext, PluginEvent, AFTER_RESOLVE, AFTER_SHELL_RUN, BEFORE_RESOLVE, BEFORE_SHELL_RUN, ON_RESOLVE_ERROR

### Community 11 - "TemplateContext"
Cohesion: 0.11
Nodes (4): Plugin, Override, Pattern, TemplateContext

### Community 12 - "AccessorBenchmark"
Cohesion: 0.19
Nodes (17): Benchmark, BenchmarkMode, Fork, Measurement, OutputTimeUnit, Setup, State, AccessorBenchmark (+9 more)

### Community 13 - "CloneContext"
Cohesion: 0.12
Nodes (9): CloneContext, SuppressWarnings, Cloner, Clones, Override, SuppressWarnings, Override, SuppressWarnings (+1 more)

### Community 14 - "TemplateEngineTest"
Cohesion: 0.27
Nodes (4): ArgumentResolver, TemplateEngine, Test, TemplateEngineTest

### Community 16 - "CloneStrategy"
Cohesion: 0.21
Nodes (5): CloneStrategy, Builder, DefaultCloner, Override, ArrayStrategy

### Community 17 - "CompositeTypeAdapter"
Cohesion: 0.25
Nodes (5): CompositeTypeAdapter, Override, DefaultObjectAccessor, Override, SuppressWarnings

### Community 18 - "CollectionTypeAdapter"
Cohesion: 0.19
Nodes (5): CollectionTypeAdapter, PathParser, AccessorFactory, AccessorFactoryImpl, Override

### Community 19 - "DirectoryPluginLoader"
Cohesion: 0.17
Nodes (7): ClassLoader, URLClassLoader, DirectoryPluginLoader, Override, Override, URL, PluginClassLoader

### Community 20 - "SecurityConfig"
Cohesion: 0.18
Nodes (4): Builder, SecurityConfig, DefaultShellRunner, Override

### Community 21 - "PathToken"
Cohesion: 0.22
Nodes (5): DefaultPathParser, Override, IndexToken, PathToken, PropertyToken

### Community 22 - ".deepClone"
Cohesion: 0.42
Nodes (3): ClonesTest, DisplayName, Test

### Community 24 - "ClonesTest.java"
Cohesion: 0.58
Nodes (9): NoArgsConstructor, Address, AllArgsConstructor, Getter, Setter, Node, SecretData, SpecialService (+1 more)

### Community 25 - "ImmutableStrategy"
Cohesion: 0.22
Nodes (6): Documented, Retention, Target, MarkAsImmute, ImmutableStrategy, Override

### Community 26 - "ClonesImmutableTest.java"
Cohesion: 0.27
Nodes (7): ClonesImmutableTest, DisplayName, MethodSource, ParameterizedTest, TestEnum, ACTIVE, INACTIVE

### Community 28 - ".copy"
Cohesion: 0.19
Nodes (8): IgnoreClone, Documented, Retention, Target, Field, Override, SuppressWarnings, ReflectionStrategy

### Community 29 - "ListAdapter"
Cohesion: 0.39
Nodes (3): Override, SuppressWarnings, ListAdapter

### Community 30 - "MapAdapter"
Cohesion: 0.46
Nodes (3): Override, SuppressWarnings, MapAdapter

### Community 31 - ".copy"
Cohesion: 0.40
Nodes (3): CollectionStrategy, Override, SuppressWarnings

### Community 32 - "DisplayName"
Cohesion: 0.57
Nodes (3): DisplayName, Test, PluginClassLoaderTest

### Community 34 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 35 - "dependencies"
Cohesion: 0.50
Nodes (3): @opencode-ai/plugin, dependencies, @opencode-ai/plugin

## Knowledge Gaps
- **9 isolated node(s):** `@opencode-ai/plugin`, `BEFORE_RESOLVE`, `AFTER_RESOLVE`, `BEFORE_SHELL_RUN`, `AFTER_SHELL_RUN` (+4 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `UserProfile` connect `DisplayName` to `DefaultSensitiveMaskerTest`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **Why does `User` connect `ClonesTest.java` to `DefaultSensitiveMaskerTest`, `.deepClone`?**
  _High betweenness centrality (0.081) - this node is a cross-community bridge._
- **Why does `DynamicObjects` connect `ObjectAccessor` to `DisplayName`, `TypeAdapter`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Are the 13 inferred relationships involving `PluginCandidate` (e.g. with `.testCombine()` and `.testDeduplication()`) actually correct?**
  _`PluginCandidate` has 13 INFERRED edges - model-reasoned connections that need verification._
- **What connects `@opencode-ai/plugin`, `BEFORE_RESOLVE`, `AFTER_RESOLVE` to the rest of the system?**
  _9 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `StringUtilTest` be split into smaller, more focused modules?**
  _Cohesion score 0.07465108730931516 - nodes in this community are weakly interconnected._
- **Should `PluginCandidate` be split into smaller, more focused modules?**
  _Cohesion score 0.059676044330775786 - nodes in this community are weakly interconnected._