# Graph Report - .  (2026-07-26)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 2129 nodes · 5702 edges · 142 communities (116 shown, 26 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 446 edges (avg confidence: 0.8)
- Token cost: 5,533 input · 3,544 output

## Graph Freshness
- Built from commit: `afb3aed7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Arithmetic Evaluation Tests
- String Utility Tests
- Field Extraction Tests
- Arithmetic Evaluator
- Dynamic Objects Tests
- Date Evaluators
- String Contains Tests
- AST Index Analyzer
- Index Family
- Evaluation Cache
- DB Aggregator Evaluator
- State Machine Actions
- Evaluator Implementations
- Template Configuration
- Plugin Context
- Type Adapters
- Field Expression Filter
- Index Detection Tests
- In-Memory Plugin Store
- Function Registry
- Aggregate Query Family
- JOOQ SQL Dialect
- Function Attributes
- Evaluation Cache Builder
- Sensitive Masker
- Database Query Models
- Expression Language Concepts
- State Machine Tests
- Composite Plugin Loader
- Storage Service
- JOOQ SQL Dialect Implementation
- Default State Machine Tests
- Sensitive Masker Tests
- Payload Evaluator Tests
- Template Engine Tests
- Accessor Benchmark
- Built-in Function Registration
- Clone Context
- Function ID Tests
- JOOQ DB Accessor
- Default State Machine
- Bean Adapter
- Plugin Info Tests
- State Machine Listener Tests
- Plugin Manager Tests
- Plugin Resolution Utilities
- Security Configuration
- Clone Strategy
- AST Evaluator Dry Run
- DB Access Evaluator
- Plugin Sources
- Project Documentation
- Directory Plugin Loader
- Record Adapter
- Message Model Tests
- Plugin Loader Builders
- Sensitive Masker Builders
- Composite Type Adapter
- Path Parser
- State Machine Interceptor Tests
- Transition Exception
- Plugin Library Concepts
- Arguments Tests
- State Machine Builder Tests
- Clone Ignore Strategy
- AST Design Documentation
- State Machine Builder Exception
- Cloner Tests
- Date Extraction Evaluator
- Core Temporal Utilities
- Error Codes
- Test Data Models
- Immutable Clone Strategy
- Immutable Clone Tests
- List Adapter
- Type Adapter Implementations
- Reflection Utilities
- Bean Sensitive Masker
- Plugin Interface
- Composite Plugin Loader Tests
- Plugin Candidate Tests
- Event Tests
- Map Adapter
- String Utility Documentation
- Plugin Library Design
- Map Sensitive Masker Strategy
- Index Column Builder
- Plugin Class Loader Tests
- Greeter Implementations
- String Similarity Evaluator
- Boolean Logic Evaluator
- String Fuzzy Match Evaluator
- Join Types
- Agent Skills
- Agile Theory Knowledge
- Gradle Wrapper Script
- Plugin Configuration
- Graphify Plugin
- Shell Execution Exception
- Greeter Plugin
- Deep Copy Library
- Data Masking Library
- Dynamic Object Access
- Override Supports
- Prompt Template Engine
- KHEZY Mission Vision
- Manual Release Workflow
- Doc Agent
- Learn Command
- Agile Knowledge Skill
- Scrum Knowledge Skill
- Project Overview
- Plugin Instance

## God Nodes (most connected - your core abstractions)
1. `Arguments` - 101 edges
2. `EvaluationContext` - 89 edges
3. `EvaluationOutcome` - 74 edges
4. `Evaluator` - 73 edges
5. `StringUtilTest` - 46 edges
6. `EvaluationResult` - 44 edges
7. `FunctionId` - 42 edges
8. `SchemaRegistry` - 38 edges
9. `Node` - 34 edges
10. `FunctionRegistry` - 31 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS (Project Structure)` --references--> `ast-expression-core Module`  [EXTRACTED]
  AGENTS.md → core/ast-expression-core/README.md
- `AGENTS (Project Structure)` --references--> `state-machine-core Module`  [EXTRACTED]
  AGENTS.md → core/state-machine-core/README.md
- `DefaultSensitiveMasker` --implements--> `SensitiveMasker`  [EXTRACTED]
  securities/data-masker/src/main/java/io/github/khezyapp/datamasker/strategy/DefaultSensitiveMasker.java → securities/data-masker/src/main/java/io/github/khezyapp/datamasker/api/SensitiveMasker.java
- `BeanSensitiveMaskerStrategy` --implements--> `SensitiveMaskerStrategy`  [EXTRACTED]
  securities/data-masker/src/main/java/io/github/khezyapp/datamasker/strategy/BeanSensitiveMaskerStrategy.java → securities/data-masker/src/main/java/io/github/khezyapp/datamasker/api/SensitiveMaskerStrategy.java
- `CompositeSensitiveMaskerStrategy` --references--> `SensitiveMaskerStrategy`  [EXTRACTED]
  securities/data-masker/src/main/java/io/github/khezyapp/datamasker/strategy/CompositeSensitiveMaskerStrategy.java → securities/data-masker/src/main/java/io/github/khezyapp/datamasker/api/SensitiveMaskerStrategy.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **AST Expression Core Design Document Set** — ast_design_v1, ast_design_v2, final_design_p1, final_design_p2, final_design_p3, index_impl_plan_p1, index_impl_plan_p2, index_impl_plan_p3, dynamic_index_algorithm, pg_gin_reference [INFERRED 0.90]
- **khezy-kit Composite Modules** — state_machine_core_module, ast_expression_core_module, storage_api_module, storage_fs_module, string_util_module, dynamic_object_module, clone_util_module, data_masker_module, simple_prompt_template_module, pluginlib_module, build_logic [EXTRACTED 1.00]
- **OpenCode Agent Team** — opencode_agents_khezy_dev_agent, opencode_agents_khezy_doc_agent, opencode_agents_other_dev_agent [EXTRACTED 1.00]
- **AST Core Research and Reference Documents** — scrum_ast_core_reference_postgresql_index_reference, scrum_ast_core_research_ast_eval_design_principles, scrum_ast_core_research_database_access_evaluator_design, scrum_ast_core_research_dynamic_index_creation_principles, scrum_ast_core_research_easy_rules_design_principles, scrum_ast_core_research_research_goals_structure [INFERRED 0.75]
- **State Machine Design Documents** — scrum_state_machine_01_state_machine_technical_design, scrum_state_machine_02_core_library_proposal, scrum_state_machine_03_index_concept_technical_design, scrum_state_machine_04_mutability_review, scrum_state_machine_05_unit_test_plan [INFERRED 0.75]
- **Plugin Library Design Documents** — scrum_plugin_lib_multi_plugin_jar, scrum_plugin_lib_plugin_lib_design, scrum_plugin_lib_store_redesign [INFERRED 0.75]
- **Plugin Loader Implementations** — utils_pluginlib_README_serviceloaderpluginloader, utils_pluginlib_README_directorypluginloader, utils_pluginlib_README_compositepluginloader [EXTRACTED 1.00]
- **Three Layers of Representation** — utils_pluginlib_README_plugincandidate, utils_pluginlib_README_installedplugin, utils_pluginlib_README_plugin_instance [EXTRACTED 1.00]
- **StringUtil Feature Groups** — utils_string_util_README_null_safe_checks, utils_string_util_README_comparisons, utils_string_util_README_transformations, utils_string_util_README_advanced_stripping, utils_string_util_README_splitting [INFERRED 0.90]

## Communities (142 total, 26 thin omitted)

### Community 0 - "Arithmetic Evaluation Tests"
Cohesion: 0.09
Nodes (27): ArithmeticEvaluationTests, AstEvaluatorTest, BooleanEvaluationTests, CachingTests, ConstantEvaluationTests, CustomFunctionTests, ErrorHandlingTests, IsEmptyEvaluationTests (+19 more)

### Community 1 - "String Utility Tests"
Cohesion: 0.07
Nodes (9): Arguments, NullSource, StringUtil, DisplayName, MethodSource, ParameterizedTest, Test, StringUtilTest (+1 more)

### Community 2 - "Field Extraction Tests"
Cohesion: 0.12
Nodes (8): AnalyzerFieldExtractionTests, AnalyzerOperatorClassificationTests, ConcreteIndexCoverageTests, DisplayName, Test, PlannerFilterExistingTests, PlannerMinimizeTests, PlannerProjectToConcreteTests

### Community 3 - "Arithmetic Evaluator"
Cohesion: 0.08
Nodes (23): ArithmeticEvaluator, Override, Override, Override, Override, Override, Override, Override (+15 more)

### Community 4 - "Dynamic Objects Tests"
Cohesion: 0.12
Nodes (14): CsvSource, Nested, DynamicObjects, AccountRecord, Address, BasicAccessTests, DeepAccessTests, DynamicObjectsTest (+6 more)

### Community 5 - "Date Evaluators"
Cohesion: 0.09
Nodes (24): ChronoUnit, CoalesceEvaluator, Override, DateDiffEvaluator, DateFormatEvaluator, DateMinusEvaluator, DateParseEvaluator, DatePlusEvaluator (+16 more)

### Community 6 - "String Contains Tests"
Cohesion: 0.15
Nodes (16): Override, StringContainsEvaluator, Override, Override, Override, Arguments, DisplayName, Nested (+8 more)

### Community 7 - "AST Index Analyzer"
Cohesion: 0.10
Nodes (13): AstIndexAnalyzer, Builder, ExpressionIndexResolver, FunctionalInterface, IndexResolverRegistry, ExpressionIndexMetadata, FunctionalColumn, GinColumn (+5 more)

### Community 8 - "Index Family"
Cohesion: 0.09
Nodes (10): ConcreteIndex, IndexFamily, Override, IndexType, AGGREGATION, FUNCTIONAL, GIN, IndexPlanner (+2 more)

### Community 9 - "Evaluation Cache"
Cohesion: 0.09
Nodes (8): EvaluationError, DefaultEvaluationCache, Override, EvaluationResult, Override, EvaluationSummary, EvaluationTrace, FunctionStats

### Community 10 - "DB Aggregator Evaluator"
Cohesion: 0.08
Nodes (16): DbAggregatorEvaluator, Override, SuppressWarnings, FilterBuilderEvaluator, Override, DataType, BOOLEAN, FLOAT (+8 more)

### Community 11 - "State Machine Actions"
Cohesion: 0.10
Nodes (8): Action, FunctionalInterface, Guard, FunctionalInterface, StateMachineBuilder, TransitionConfigurer, TransitionIndex, Transition

### Community 13 - "Template Configuration"
Cohesion: 0.11
Nodes (9): Builder, TemplateConfig, PluginRegistry, PlaceholderResolver, ResolverChain, Override, Pattern, ShellPlaceholderResolver (+1 more)

### Community 14 - "Plugin Context"
Cohesion: 0.08
Nodes (10): Builder, PluginContext, PluginEvent, AFTER_RESOLVE, AFTER_SHELL_RUN, BEFORE_RESOLVE, BEFORE_SHELL_RUN, ON_RESOLVE_ERROR (+2 more)

### Community 15 - "Type Adapters"
Cohesion: 0.15
Nodes (6): CollectionTypeAdapter, ObjectAccessor, PathParser, AccessorFactory, AccessorFactoryImpl, Override

### Community 16 - "Field Expression Filter"
Cohesion: 0.07
Nodes (23): FieldExpression, FilterCondition, FilterOperator, ALL_KEYS_EXIST, ANY_KEY_EXISTS, ARRAY_CONTAINED_BY, ARRAY_CONTAINS, ARRAY_OVERLAP (+15 more)

### Community 18 - "In-Memory Plugin Store"
Cohesion: 0.20
Nodes (6): InMemoryPluginStore, Override, InMemoryPluginStoreTest, BeforeEach, DisplayName, Test

### Community 19 - "Function Registry"
Cohesion: 0.10
Nodes (6): ComparisonEvaluator, SuppressWarnings, FunctionRegistry, CoreFunctions, Core, FunctionId

### Community 20 - "Aggregate Query Family"
Cohesion: 0.10
Nodes (4): AggregateQueryFamily, Builder, Override, PlannerPlanIndexFamiliesTests

### Community 21 - "JOOQ SQL Dialect"
Cohesion: 0.33
Nodes (6): ColumnRef, Literal, DisplayName, DSLContext, Test, JooqSqlDialectTest

### Community 22 - "Function Attributes"
Cohesion: 0.13
Nodes (6): FunctionAttributes, Builder, FunctionDefinition, ParamSpec, FunctionalInterface, NullHandlingStrategy

### Community 23 - "Evaluation Cache Builder"
Cohesion: 0.08
Nodes (3): EvaluationCache, Builder, Message

### Community 24 - "Sensitive Masker"
Cohesion: 0.15
Nodes (8): SensitiveMaskerContext, SensitiveMaskerStrategy, Override, CollectionSensitiveMaskerStrategy, Override, RequiredArgsConstructor, Override, SuppressWarnings

### Community 25 - "Database Query Models"
Cohesion: 0.11
Nodes (12): DbAggregation, DbColumn, DbFilter, DbJoin, DbOrder, Builder, DbTable, FilterValue (+4 more)

### Community 26 - "Expression Language Concepts"
Cohesion: 0.14
Nodes (27): AST Node (Expression Tree Node), DatabaseAccess Evaluator, Engine Strategies (Linear, Inference, Scoring), Evaluation Context, Evaluation Result (Trace), Expression Language Pluggability, Function Registry, IndexFamily Model (+19 more)

### Community 27 - "State Machine Tests"
Cohesion: 0.19
Nodes (6): StateMachine, BeforeEach, DisplayName, Test, KycContext, KycWorkflowTest

### Community 28 - "Composite Plugin Loader"
Cohesion: 0.13
Nodes (5): CompositePluginLoader, Override, Override, PluginManager, Version

### Community 29 - "Storage Service"
Cohesion: 0.11
Nodes (5): SignedUrlOptions, StorageMetadata, StorageService, FileSystemStorageService, Override

### Community 30 - "JOOQ SQL Dialect Implementation"
Cohesion: 0.22
Nodes (9): Condition, DSLContext, Override, SuppressWarnings, JooqSqlDialect, Field, SelectFieldOrAsterisk, SelectQuery (+1 more)

### Community 31 - "Default State Machine Tests"
Cohesion: 0.23
Nodes (4): DefaultStateMachineTest, BeforeEach, DisplayName, Test

### Community 32 - "Sensitive Masker Tests"
Cohesion: 0.27
Nodes (7): CompositeSensitiveMaskerStrategy, DefaultSensitiveMasker, Override, DefaultSensitiveMaskerTest, BeforeEach, DisplayName, Test

### Community 33 - "Payload Evaluator Tests"
Cohesion: 0.25
Nodes (8): ErrorHandlingTests, BeforeEach, DisplayName, Nested, Test, NestedAccessTests, PayloadEvaluatorTest, SingleLevelTests

### Community 34 - "Template Engine Tests"
Cohesion: 0.23
Nodes (5): ArgumentResolver, Pattern, TemplateEngine, Test, TemplateEngineTest

### Community 35 - "Accessor Benchmark"
Cohesion: 0.19
Nodes (17): Benchmark, BenchmarkMode, Fork, Measurement, OutputTimeUnit, Setup, State, AccessorBenchmark (+9 more)

### Community 36 - "Built-in Function Registration"
Cohesion: 0.11
Nodes (10): ParamType, ANY, BOOLEAN, FLOAT, INTEGER, LIST, MAP, STRING (+2 more)

### Community 37 - "Clone Context"
Cohesion: 0.12
Nodes (10): CloneContext, SuppressWarnings, Override, SuppressWarnings, CollectionStrategy, Override, SuppressWarnings, Override (+2 more)

### Community 38 - "Function ID Tests"
Cohesion: 0.27
Nodes (8): Named, CreationTests, EqualityTests, FunctionIdTest, DisplayName, Nested, Test, ToStringTests

### Community 39 - "JOOQ DB Accessor"
Cohesion: 0.16
Nodes (10): DSLContext, Override, JooqDbAccessor, DbQuery, SqlDialect, SqlRenderStyle, INDEXED, INLINED (+2 more)

### Community 40 - "Default State Machine"
Cohesion: 0.23
Nodes (4): DefaultStateMachine, Override, Event, State

### Community 41 - "Bean Adapter"
Cohesion: 0.20
Nodes (6): Lookup, MethodHandle, BeanAdapter, Method, Override, Slf4j

### Community 42 - "Plugin Info Tests"
Cohesion: 0.17
Nodes (11): Retention, Target, PluginInfo, DisplayName, Override, Test, JsonTransformer, PlainTransformer (+3 more)

### Community 43 - "State Machine Listener Tests"
Cohesion: 0.28
Nodes (4): StateMachineListener, DisplayName, Test, StateMachineListenerTest

### Community 44 - "Plugin Manager Tests"
Cohesion: 0.28
Nodes (6): Provider, PluginCandidate, Override, DisplayName, Test, PluginManagerTest

### Community 46 - "Security Configuration"
Cohesion: 0.15
Nodes (4): Builder, SecurityConfig, DefaultShellRunner, Override

### Community 47 - "Clone Strategy"
Cohesion: 0.21
Nodes (5): CloneStrategy, Builder, DefaultCloner, Override, ArrayStrategy

### Community 48 - "AST Evaluator Dry Run"
Cohesion: 0.24
Nodes (5): AstEvaluator, ChildResult, DbAccessDryRunTest, DisplayName, Test

### Community 49 - "DB Access Evaluator"
Cohesion: 0.18
Nodes (5): DbAccessEvaluator, DbAccessor, DryRunResult, DbFieldAccessEvaluator, LinkMetadata

### Community 50 - "Plugin Sources"
Cohesion: 0.16
Nodes (7): InstalledPlugin, ClasspathSource, FileSource, URL, PluginSource, UrlSource, PluginStore

### Community 51 - "Project Documentation"
Cohesion: 0.12
Nodes (17): AGENTS (Project Structure), ast-expression-core Module, build-logic Convention Plugins, clone-util Module, AST Expression Core README, State Machine Core README, data-masker Module, dynamic-object Module (+9 more)

### Community 52 - "Directory Plugin Loader"
Cohesion: 0.17
Nodes (7): ClassLoader, URLClassLoader, DirectoryPluginLoader, Override, Override, URL, PluginClassLoader

### Community 53 - "Record Adapter"
Cohesion: 0.23
Nodes (7): Constructor, RecordComponent, Method, Override, RecordAdapter, RecordMeta, Cache

### Community 54 - "Message Model Tests"
Cohesion: 0.28
Nodes (4): Message, DisplayName, Test, MessageTest

### Community 55 - "Plugin Loader Builders"
Cohesion: 0.12
Nodes (4): FunctionalInterface, PluginLoader, Builder, ServiceLoaderPluginLoader

### Community 56 - "Sensitive Masker Builders"
Cohesion: 0.18
Nodes (4): SensitiveMasker, DataMaskerUtils, RequiredArgsConstructor, SensitiveMaskerBuilder

### Community 57 - "Composite Type Adapter"
Cohesion: 0.25
Nodes (5): CompositeTypeAdapter, Override, DefaultObjectAccessor, Override, SuppressWarnings

### Community 58 - "Path Parser"
Cohesion: 0.21
Nodes (5): DefaultPathParser, Override, IndexToken, PathToken, PropertyToken

### Community 59 - "State Machine Interceptor Tests"
Cohesion: 0.32
Nodes (4): StateMachineInterceptor, DisplayName, Test, StateMachineInterceptorTest

### Community 61 - "Plugin Library Concepts"
Cohesion: 0.13
Nodes (15): Classloader Isolation, CompositePluginLoader, DirectoryPluginLoader, Discovery, InMemoryPluginStore, InstalledPlugin, Lifecycle, Plugin Lib (+7 more)

### Community 62 - "Arguments Tests"
Cohesion: 0.35
Nodes (6): ArgumentsTest, CreationTests, EqualityTests, DisplayName, Nested, Test

### Community 63 - "State Machine Builder Tests"
Cohesion: 0.45
Nodes (3): DisplayName, Test, StateMachineBuilderTest

### Community 64 - "Clone Ignore Strategy"
Cohesion: 0.19
Nodes (8): IgnoreClone, Documented, Retention, Target, Field, Override, SuppressWarnings, ReflectionStrategy

### Community 65 - "AST Design Documentation"
Cohesion: 0.17
Nodes (12): AST Design (v1 Overview), AST Design (v2 Builtins), Dynamic Index Creation Algorithm, Final AST Design (Part 1), Final AST Design (Part 2), Final AST Design (Part 3), Index Detection Plan (Part 1), Index Detection Plan (Part 2) (+4 more)

### Community 67 - "Cloner Tests"
Cohesion: 0.22
Nodes (5): Cloner, Clones, ClonesTest, DisplayName, Test

### Community 69 - "Core Temporal Utilities"
Cohesion: 0.29
Nodes (3): Override, CoreUtils, Temporal

### Community 70 - "Error Codes"
Cohesion: 0.27
Nodes (4): Custom, ErrorCode, Standard, StandardErrors

### Community 71 - "Test Data Models"
Cohesion: 0.58
Nodes (9): NoArgsConstructor, Address, AllArgsConstructor, Getter, Setter, Node, SecretData, SpecialService (+1 more)

### Community 72 - "Immutable Clone Strategy"
Cohesion: 0.22
Nodes (6): Documented, Retention, Target, MarkAsImmute, ImmutableStrategy, Override

### Community 73 - "Immutable Clone Tests"
Cohesion: 0.27
Nodes (7): ClonesImmutableTest, DisplayName, MethodSource, ParameterizedTest, TestEnum, ACTIVE, INACTIVE

### Community 74 - "List Adapter"
Cohesion: 0.39
Nodes (3): Override, SuppressWarnings, ListAdapter

### Community 75 - "Type Adapter Implementations"
Cohesion: 0.13
Nodes (4): Entry, TypeAdapter, Override, MapCache

### Community 76 - "Reflection Utilities"
Cohesion: 0.36
Nodes (3): PropertyDescriptor, Field, ReflectionUtils

### Community 77 - "Bean Sensitive Masker"
Cohesion: 0.25
Nodes (6): Documented, Retention, Target, SensitiveData, BeanSensitiveMaskerStrategy, Slf4j

### Community 79 - "Composite Plugin Loader Tests"
Cohesion: 0.50
Nodes (3): CompositePluginLoaderTest, DisplayName, Test

### Community 80 - "Plugin Candidate Tests"
Cohesion: 0.33
Nodes (4): Foo, DisplayName, Test, PluginCandidateTest

### Community 81 - "Event Tests"
Cohesion: 0.54
Nodes (3): EventTest, DisplayName, Test

### Community 82 - "Map Adapter"
Cohesion: 0.46
Nodes (3): Override, SuppressWarnings, MapAdapter

### Community 83 - "String Utility Documentation"
Cohesion: 0.25
Nodes (8): Advanced Stripping, Comparisons, Null-Safe Checks, Splitting (Left & Right), StringUtil Class, StringUtil Library, Transformation & Formatting, Unicode NFKC Normalization

### Community 84 - "Plugin Library Design"
Cohesion: 0.43
Nodes (7): PluginCandidate, PluginLoader, PluginManager, PluginStore, Multi-Plugin Per JAR Analysis, Plugin Lib Design, PluginStore Redesign

### Community 85 - "Map Sensitive Masker Strategy"
Cohesion: 0.52
Nodes (5): Builder, Getter, Setter, KeyValueMask, MapSensitiveMaskerStrategy

### Community 87 - "Plugin Class Loader Tests"
Cohesion: 0.57
Nodes (3): DisplayName, Test, PluginClassLoaderTest

### Community 88 - "Greeter Implementations"
Cohesion: 0.33
Nodes (4): Greeter, HelloGreeter, HiGreeter, Override

### Community 92 - "Join Types"
Cohesion: 0.40
Nodes (5): JoinType, CROSS, INNER, LEFT, RIGHT

### Community 93 - "Agent Skills"
Cohesion: 0.40
Nodes (5): khezy-dev Agent, other-dev Agent, AST Evaluator Testing Skill, Checkstyle Gotchas Skill, Coding Style Skill

### Community 94 - "Agile Theory Knowledge"
Cohesion: 0.50
Nodes (4): Agile Methodology, Scrum Framework, Agile Theory Knowledge Base, Scrum Theory Knowledge Base

### Community 95 - "Gradle Wrapper Script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 96 - "Plugin Configuration"
Cohesion: 0.50
Nodes (3): plugin, $schema, .opencode/plugins/graphify.js

### Community 99 - "Greeter Plugin"
Cohesion: 0.67
Nodes (3): FriendlyGreeter, Greeter, @PluginInfo

## Knowledge Gaps
- **117 isolated node(s):** `BEFORE_RESOLVE`, `AFTER_RESOLVE`, `BEFORE_SHELL_RUN`, `AFTER_SHELL_RUN`, `ON_RESOLVE_ERROR` (+112 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **26 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MapCache` connect `Type Adapter Implementations` to `Record Adapter`?**
  _High betweenness centrality (0.267) - this node is a cross-community bridge._
- **Why does `DynamicObjects` connect `Dynamic Objects Tests` to `Type Adapter Implementations`, `Type Adapters`?**
  _High betweenness centrality (0.241) - this node is a cross-community bridge._
- **Why does `UserProfile` connect `Dynamic Objects Tests` to `Sensitive Masker Tests`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Are the 28 inferred relationships involving `Arguments` (e.g. with `.missingFieldNameArg()` and `.evaluate()`) actually correct?**
  _`Arguments` has 28 INFERRED edges - model-reasoned connections that need verification._
- **What connects `BEFORE_RESOLVE`, `AFTER_RESOLVE`, `BEFORE_SHELL_RUN` to the rest of the system?**
  _117 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Arithmetic Evaluation Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.0938375350140056 - nodes in this community are weakly interconnected._
- **Should `String Utility Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.07465108730931516 - nodes in this community are weakly interconnected._