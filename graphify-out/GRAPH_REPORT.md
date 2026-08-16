# Graph Report - khezy-kit  (2026-08-16)

## Corpus Check
- 468 files · ~214,136 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3912 nodes · 11270 edges · 210 communities (184 shown, 26 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 960 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8a1dd429`
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
- NextUrlPagination
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
- InMemoryTokenStore
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
- OAuth2RequestAuthenticatorTest
- Bean Sensitive Masker
- Plugin Interface
- Composite Plugin Loader Tests
- Plugin Candidate Tests
- Event Tests
- Map Adapter
- String Utility Documentation
- Plugin Library Design
- Map Sensitive Masker Strategy
- .withBuiltins
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
- Headers
- Data Masking Library
- Dynamic Object Access
- .capsAtMax
- Prompt Template Engine
- KHEZY Mission Vision
- Manual Release Workflow
- Doc Agent
- Learn Command
- Agile Knowledge Skill
- Scrum Knowledge Skill
- Project Overview
- Plugin Instance
- Literal
- .create
- ChoiceRoutingWorkflowTest.java
- FormSchemaJacksonTest.java
- ServiceLoaderPluginLoader
- PagePaginationTest.java
- SchemaException
- dynamic-form-core/build.gradle
- dynamic-form-core/settings.gradle
- Task 09 — Authenticator SPI + generic auth (`auth/`)
- Task 10 — OAuth2 two-phase config + token lifecycle (`auth/oauth2/`)
- Task 11 — Pre/Post processors + registry (`action/`)
- Task 12 — Pagination (`pagination/`)
- Task 13 — Engine + pipeline (`engine/`)
- Task 14 — Config facade (`config/`)
- Task 15 — Acceptance milestone (§9 of the design doc)
- BuiltinSupport
- 6. SPI Details (what implementers extend)
- Declarative HTTP — v1 Implementation Action Plan
- README.md
- 3. Core Abstractions — Java API Sketch
- JoinType
- .create
- JexlExpressionEvaluator
- JexlExpressionEvaluator
- Cloner
- DisplayName
- SecretRedactor
- DbAccessor
- .of
- .evaluate
- EvaluationScope
- .describeReturnsShapedOptions
- JexlEngineFactory.java
- ListAdapter
- Auth
- OAuth2Grant
- OAuth2AuthorizationFlow
- QueueTransport
- CredentialType.java
- PagePaginationTest.java
- HeaderApiKeyCredentials.java
- HttpHeaderCredentials.java
- HttpMethod
- OAuth2ConfigTimeAcceptanceTest
- khezy-dhttp-testing/SKILL.md
- DeclarativeHttpConfigTest.java
- khezy-jdk-httpclient-gotchas/SKILL.md
- .registerDateBuiltins
- .evaluate
- ArrayFormat
- .switchIsExhaustive
- .evaluate
- InstalledPlugin
- ServiceLoaderPluginLoader
- .describeReturnsShapedOptions
- FilterItemsTest.java
- SetKeyValueTest.java
- .fromMap
- certificate-util
- Cloner
- 3. Core Abstractions — Java API Sketch
- NonStringKeyExpressionException.java
- Scenario demos

## God Nodes (most connected - your core abstractions)
1. `HttpRequest` - 132 edges
2. `HttpResult` - 115 edges
3. `Arguments` - 112 edges
4. `EvaluationContext` - 89 edges
5. `JsonMapper` - 75 edges
6. `EvaluationOutcome` - 74 edges
7. `Evaluator` - 73 edges
8. `RequestShape` - 73 edges
9. `RequestPlan` - 67 edges
10. `HttpRequestSpec` - 66 edges

## Surprising Connections (you probably didn't know these)
- `AccessorBenchmark` --references--> `State`  [EXTRACTED]
  utils/dynamic-object/src/jmh/java/io/github/khezyapp/doa/AccessorBenchmark.java → core/state-machine-core/src/main/java/io/github/khezyapp/fsm/core/model/State.java
- `Builder` --references--> `ClientTlsConfig`  [EXTRACTED]
  http/declarative-http/src/main/java/io/github/khezyapp/dhttp/config/DeclarativeHttpConfig.java → utils/certificate-util/src/main/java/io/github/khezyapp/cert/ClientTlsConfig.java
- `SensitiveData` --references--> `Target`  [EXTRACTED]
  securities/data-masker/src/main/java/io/github/khezyapp/datamasker/annotation/SensitiveData.java → http/declarative-http/src/main/java/io/github/khezyapp/dhttp/spec/Target.java
- `MarkAsImmute` --references--> `Target`  [EXTRACTED]
  utils/clone-util/src/main/java/io/github/khezyapp/clone/annotation/MarkAsImmute.java → http/declarative-http/src/main/java/io/github/khezyapp/dhttp/spec/Target.java
- `HttpRequest` --references--> `ClientTlsConfig`  [EXTRACTED]
  http/declarative-http/src/main/java/io/github/khezyapp/dhttp/transport/HttpRequest.java → utils/certificate-util/src/main/java/io/github/khezyapp/cert/ClientTlsConfig.java

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

## Communities (210 total, 26 thin omitted)

### Community 0 - "Arithmetic Evaluation Tests"
Cohesion: 0.12
Nodes (17): ArithmeticEvaluationTests, AstEvaluatorTest, BooleanEvaluationTests, CachingTests, ConstantEvaluationTests, CustomFunctionTests, ErrorHandlingTests, IsEmptyEvaluationTests (+9 more)

### Community 1 - "String Utility Tests"
Cohesion: 0.06
Nodes (14): ArgumentsTest, CreationTests, EqualityTests, DisplayName, Nested, Test, NullSource, StringUtil (+6 more)

### Community 2 - "Field Extraction Tests"
Cohesion: 0.12
Nodes (7): AnalyzerOperatorClassificationTests, ConcreteIndexCoverageTests, DisplayName, Nested, PlannerFilterExistingTests, PlannerMinimizeTests, PlannerProjectToConcreteTests

### Community 3 - "Arithmetic Evaluator"
Cohesion: 0.24
Nodes (6): BrevoSpecAcceptanceTest, BeforeEach, DisplayName, SecretKey, Test, FakeTransport

### Community 4 - "Dynamic Objects Tests"
Cohesion: 0.08
Nodes (30): Benchmark, BenchmarkMode, CsvSource, Fork, Measurement, OutputTimeUnit, Setup, AccessorBenchmark (+22 more)

### Community 5 - "Date Evaluators"
Cohesion: 0.13
Nodes (14): BasicAuthCredentials, DisplayName, SuppressWarnings, Test, DeclarativeHttpConfigTest, BeforeEach, DisplayName, SecretKey (+6 more)

### Community 6 - "String Contains Tests"
Cohesion: 0.09
Nodes (22): Override, StringContainsEvaluator, Override, StringEndsWithEvaluator, Override, StringFuzzyMatchEvaluator, Override, StringMatchEvaluator (+14 more)

### Community 7 - "AST Index Analyzer"
Cohesion: 0.10
Nodes (8): ArithmeticEvaluator, BooleanLogicEvaluator, Override, ComparisonEvaluator, SuppressWarnings, CoreFunctions, Core, FunctionId

### Community 8 - "Index Family"
Cohesion: 0.54
Nodes (3): HttpResultTest, DisplayName, Test

### Community 9 - "Evaluation Cache"
Cohesion: 0.31
Nodes (3): EndToEndIndexDetectionTest, Test, RealWorldUseCaseTests

### Community 10 - "DB Aggregator Evaluator"
Cohesion: 0.09
Nodes (16): DbAggregatorEvaluator, Override, SuppressWarnings, FilterBuilderEvaluator, Override, DataType, BOOLEAN, FLOAT (+8 more)

### Community 11 - "State Machine Actions"
Cohesion: 0.11
Nodes (8): Action, FunctionalInterface, Guard, FunctionalInterface, StateMachineBuilder, TransitionConfigurer, TransitionIndex, Transition

### Community 13 - "Template Configuration"
Cohesion: 0.24
Nodes (5): Builder, TemplateConfig, ResolverChain, ShellRunner, TemplateEngine

### Community 14 - "Plugin Context"
Cohesion: 0.09
Nodes (9): Builder, PluginContext, PluginEvent, AFTER_RESOLVE, AFTER_SHELL_RUN, BEFORE_RESOLVE, BEFORE_SHELL_RUN, ON_RESOLVE_ERROR (+1 more)

### Community 15 - "Type Adapters"
Cohesion: 0.11
Nodes (7): DoaNamespace, CollectionTypeAdapter, ObjectAccessor, PathParser, AccessorFactory, AccessorFactoryImpl, Override

### Community 16 - "Field Expression Filter"
Cohesion: 0.07
Nodes (23): FieldExpression, FilterCondition, FilterOperator, ALL_KEYS_EXIST, ANY_KEY_EXISTS, ARRAY_CONTAINED_BY, ARRAY_CONTAINS, ARRAY_OVERLAP (+15 more)

### Community 17 - "Index Detection Tests"
Cohesion: 0.37
Nodes (4): DisplayName, SecretKey, Test, OAuth2ConfigTimeAcceptanceTest

### Community 18 - "In-Memory Plugin Store"
Cohesion: 0.20
Nodes (6): InMemoryPluginStore, Override, InMemoryPluginStoreTest, BeforeEach, DisplayName, Test

### Community 19 - "Function Registry"
Cohesion: 0.21
Nodes (8): RootProperty, DisplayName, SecretKey, Test, PaginationAcceptanceTest, DisplayName, Test, RootPropertyTest

### Community 20 - "Aggregate Query Family"
Cohesion: 0.09
Nodes (15): CustomPostReceive, FilterItems, LimitItems, RootProperty, SetKeyValue, SuppressWarnings, SetValue, FunctionalInterface (+7 more)

### Community 21 - "NextUrlPagination"
Cohesion: 0.10
Nodes (17): PEMEncryptedKeyPair, PEMKeyPair, PKCS8EncryptedPrivateKeyInfo, PrivateKeyInfo, KeyStore, PrivateKey, SSLContext, TrustManager (+9 more)

### Community 22 - "Function Attributes"
Cohesion: 0.27
Nodes (8): Named, CreationTests, EqualityTests, FunctionIdTest, DisplayName, Nested, Test, ToStringTests

### Community 24 - "Sensitive Masker"
Cohesion: 0.05
Nodes (33): PropertyDescriptor, Documented, Retention, SensitiveData, SensitiveMasker, SensitiveMaskerContext, SensitiveMaskerStrategy, DataMaskerUtils (+25 more)

### Community 25 - "Database Query Models"
Cohesion: 0.12
Nodes (17): Building on top of the core, Building & Testing, Capturing raw responses with `onResponse`, Configuration & customization, Core Concepts, Declarative HTTP, Design time: `describe(...)`, Expressions (+9 more)

### Community 26 - "Expression Language Concepts"
Cohesion: 0.14
Nodes (27): AST Node (Expression Tree Node), DatabaseAccess Evaluator, Engine Strategies (Linear, Inference, Scoring), Evaluation Context, Evaluation Result (Trace), Expression Language Pluggability, Function Registry, IndexFamily Model (+19 more)

### Community 27 - "State Machine Tests"
Cohesion: 0.19
Nodes (6): StateMachine, BeforeEach, DisplayName, Test, KycContext, KycWorkflowTest

### Community 28 - "Composite Plugin Loader"
Cohesion: 0.14
Nodes (5): CompositePluginLoader, Override, Override, PluginManager, Version

### Community 29 - "Storage Service"
Cohesion: 0.11
Nodes (5): SignedUrlOptions, StorageMetadata, StorageService, FileSystemStorageService, Override

### Community 30 - "JOOQ SQL Dialect Implementation"
Cohesion: 0.05
Nodes (19): FunctionalInterface, PreSendAction, Override, Override, Auth, BasicAuth, NoAuth, HttpRequest (+11 more)

### Community 31 - "Default State Machine Tests"
Cohesion: 0.27
Nodes (3): DefaultStateMachineTest, DisplayName, Test

### Community 32 - "Sensitive Masker Tests"
Cohesion: 0.05
Nodes (27): AesGcmCredentialCipher, Override, SecretKey, TypeReference, CredentialCipher, CredentialRepository, CredentialService, SuppressWarnings (+19 more)

### Community 33 - "Payload Evaluator Tests"
Cohesion: 0.24
Nodes (8): ErrorHandlingTests, BeforeEach, DisplayName, Nested, Test, NestedAccessTests, PayloadEvaluatorTest, SingleLevelTests

### Community 34 - "Template Engine Tests"
Cohesion: 0.27
Nodes (3): ArgumentResolver, Test, TemplateEngineTest

### Community 35 - "Accessor Benchmark"
Cohesion: 0.12
Nodes (17): 5. Extracted User Requirements (the contract a Java core must meet), R10 — Credential abstraction & auth injection, R11 — Rich transport contract (parity with `IHttpRequestOptions`), R12 — Security-first defaults, R13 — Error model, R14 — Cancellation & lifecycle, R15 — Metadata/design-time mode, R16 — Deterministic, testable engine (+9 more)

### Community 36 - "Built-in Function Registration"
Cohesion: 0.30
Nodes (4): ConditionEvaluator, ConditionEvaluatorTest, DisplayName, Test

### Community 37 - "Clone Context"
Cohesion: 0.15
Nodes (10): CloneContext, SuppressWarnings, ArrayStrategy, Override, SuppressWarnings, Override, SuppressWarnings, Override (+2 more)

### Community 38 - "Function ID Tests"
Cohesion: 0.12
Nodes (8): DomainAllowList, SsrfGuard, DomainAllowListTest, DisplayName, Test, DisplayName, Test, SsrfGuardTest

### Community 39 - "JOOQ DB Accessor"
Cohesion: 0.09
Nodes (25): CoalesceEvaluator, Override, Override, NowEvaluator, DefaultIfNullEvaluator, Override, EqualEvaluator, Override (+17 more)

### Community 40 - "Default State Machine"
Cohesion: 0.17
Nodes (4): DefaultStateMachine, Override, Event, State

### Community 41 - "Bean Adapter"
Cohesion: 0.20
Nodes (6): Lookup, MethodHandle, BeanAdapter, Method, Override, Slf4j

### Community 42 - "Plugin Info Tests"
Cohesion: 0.24
Nodes (8): DisplayName, Override, Test, JsonTransformer, PlainTransformer, PluginInfoTest, Transformer, XmlTransformer

### Community 43 - "State Machine Listener Tests"
Cohesion: 0.38
Nodes (4): StateMachineListener, DisplayName, Test, StateMachineListenerTest

### Community 44 - "Plugin Manager Tests"
Cohesion: 0.24
Nodes (8): PluginCandidate, Greeter, HelloGreeter, HiGreeter, DisplayName, Override, Test, PluginManagerTest

### Community 45 - "Plugin Resolution Utilities"
Cohesion: 0.12
Nodes (4): EscapeUtils, Override, PlaceholderResolver, TemplateContext

### Community 46 - "Security Configuration"
Cohesion: 0.13
Nodes (5): Builder, SecurityConfig, Pattern, DefaultShellRunner, Override

### Community 47 - "Clone Strategy"
Cohesion: 0.20
Nodes (5): CloneStrategy, Builder, DefaultCloner, Override, CollectionStrategy

### Community 48 - "AST Evaluator Dry Run"
Cohesion: 0.13
Nodes (11): DbAggregation, DbColumn, DbFilter, DbJoin, DbOrder, Builder, DbQuery, DbTable (+3 more)

### Community 49 - "InMemoryTokenStore"
Cohesion: 0.18
Nodes (8): JexlEngine, Override, SuppressWarnings, JexlExpressionEvaluator, DisplayName, Test, JexlExpressionEvaluatorTest, Sample

### Community 50 - "Plugin Sources"
Cohesion: 0.10
Nodes (11): Charset, AbstractHttpTransport, URI, Multipart, PreparedBody, ArrayFormat, BRACKETS, COMMA (+3 more)

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
Cohesion: 0.10
Nodes (6): Provider, FunctionalInterface, PluginLoader, Builder, Override, ServiceLoaderPluginLoader

### Community 56 - "Sensitive Masker Builders"
Cohesion: 0.12
Nodes (8): DeclarativeHttp, DeclarativeHttpConfig, JacksonJsonMapper, Builder, SecurityPolicy, DescribeAcceptanceTest, SecretKey, ObjectMapper

### Community 57 - "Composite Type Adapter"
Cohesion: 0.25
Nodes (5): CompositeTypeAdapter, Override, DefaultObjectAccessor, Override, SuppressWarnings

### Community 58 - "Path Parser"
Cohesion: 0.22
Nodes (5): DefaultPathParser, Override, IndexToken, PathToken, PropertyToken

### Community 59 - "State Machine Interceptor Tests"
Cohesion: 0.45
Nodes (4): StateMachineInterceptor, DisplayName, Test, StateMachineInterceptorTest

### Community 60 - "Transition Exception"
Cohesion: 0.13
Nodes (3): TransitionExecutionException, StateMachineBuilderException, BeforeEach

### Community 61 - "Plugin Library Concepts"
Cohesion: 0.13
Nodes (15): Classloader Isolation, CompositePluginLoader, DirectoryPluginLoader, Discovery, InMemoryPluginStore, InstalledPlugin, Lifecycle, Plugin Lib (+7 more)

### Community 62 - "Arguments Tests"
Cohesion: 0.26
Nodes (5): HttpErrorFactory, Pattern, HttpErrorFactoryTest, DisplayName, Test

### Community 63 - "State Machine Builder Tests"
Cohesion: 0.45
Nodes (3): DisplayName, Test, StateMachineBuilderTest

### Community 64 - "Clone Ignore Strategy"
Cohesion: 0.09
Nodes (23): Override, TypeReference, FragmentMerger, SuppressWarnings, Builder, Operation, PreSend, Builder (+15 more)

### Community 65 - "AST Design Documentation"
Cohesion: 0.17
Nodes (12): AST Design (v1 Overview), AST Design (v2 Builtins), Dynamic Index Creation Algorithm, Final AST Design (Part 1), Final AST Design (Part 2), Final AST Design (Part 3), Index Detection Plan (Part 1), Index Detection Plan (Part 2) (+4 more)

### Community 66 - "State Machine Builder Exception"
Cohesion: 0.21
Nodes (10): ConstantFactoryTests, EqualityTests, FunctionFactoryListTests, FunctionFactoryVarargsTests, DisplayName, Nested, Test, NodeTest (+2 more)

### Community 67 - "Cloner Tests"
Cohesion: 0.30
Nodes (12): NoArgsConstructor, Address, ClonesTest, AllArgsConstructor, DisplayName, Getter, Setter, Test (+4 more)

### Community 68 - "Date Extraction Evaluator"
Cohesion: 0.06
Nodes (17): FunctionAttributes, Builder, FunctionDefinition, FunctionRegistry, ParamSpec, ParamType, ANY, BOOLEAN (+9 more)

### Community 69 - "Core Temporal Utilities"
Cohesion: 0.09
Nodes (16): Override, OffsetPagination, Override, PagePagination, PaginationContext, PaginationStrategy, RequestPlan, HttpResult (+8 more)

### Community 70 - "Error Codes"
Cohesion: 0.27
Nodes (4): Custom, ErrorCode, Standard, StandardErrors

### Community 71 - "Test Data Models"
Cohesion: 0.09
Nodes (21): Attach attributes when:, Attribute key convention:, Building a Rule Engine on Top, Choosing: Positional vs Named Arguments, Do NOT attach attributes when:, Extending AST Expression Core, How This Library Thinks About Expressions, Minimal rule engine example (+13 more)

### Community 72 - "Immutable Clone Strategy"
Cohesion: 0.24
Nodes (5): URI, RedirectPolicy, DisplayName, Test, RedirectPolicyTest

### Community 73 - "Immutable Clone Tests"
Cohesion: 0.29
Nodes (7): ClonesImmutableTest, DisplayName, MethodSource, ParameterizedTest, TestEnum, ACTIVE, INACTIVE

### Community 74 - "List Adapter"
Cohesion: 0.21
Nodes (5): Override, BeforeEach, DisplayName, Test, OAuth2RequestAuthenticatorTest

### Community 75 - "Type Adapter Implementations"
Cohesion: 0.28
Nodes (3): Entry, Override, MapCache

### Community 76 - "OAuth2RequestAuthenticatorTest"
Cohesion: 0.37
Nodes (5): ColumnRef, Literal, DisplayName, Test, JooqSqlDialectTest

### Community 77 - "Bean Sensitive Masker"
Cohesion: 0.07
Nodes (15): AstIndexAnalyzer, Builder, ExpressionIndexResolver, FunctionalInterface, IndexResolverRegistry, AggregateQueryFamily, Override, ExpressionIndexMetadata (+7 more)

### Community 79 - "Composite Plugin Loader Tests"
Cohesion: 0.50
Nodes (3): CompositePluginLoaderTest, DisplayName, Test

### Community 80 - "Plugin Candidate Tests"
Cohesion: 0.36
Nodes (4): Foo, DisplayName, Test, PluginCandidateTest

### Community 81 - "Event Tests"
Cohesion: 0.54
Nodes (3): EventTest, DisplayName, Test

### Community 82 - "Map Adapter"
Cohesion: 0.18
Nodes (4): Override, SuppressWarnings, MapAdapter, TypeAdapter

### Community 83 - "String Utility Documentation"
Cohesion: 0.25
Nodes (8): Advanced Stripping, Comparisons, Null-Safe Checks, Splitting (Left & Right), StringUtil Class, StringUtil Library, Transformation & Formatting, Unicode NFKC Normalization

### Community 84 - "Plugin Library Design"
Cohesion: 0.43
Nodes (7): PluginCandidate, PluginLoader, PluginManager, PluginStore, Multi-Plugin Per JAR Analysis, Plugin Lib Design, PluginStore Redesign

### Community 85 - "Map Sensitive Masker Strategy"
Cohesion: 0.36
Nodes (4): Field, Override, SuppressWarnings, ReflectionStrategy

### Community 86 - ".withBuiltins"
Cohesion: 0.29
Nodes (4): SensitiveOutputRedactor, DisplayName, Test, SensitiveOutputRedactorTest

### Community 87 - "Plugin Class Loader Tests"
Cohesion: 0.57
Nodes (3): DisplayName, Test, PluginClassLoaderTest

### Community 88 - "Greeter Implementations"
Cohesion: 0.09
Nodes (9): ConcreteIndex, IndexFamily, Override, IndexType, AGGREGATION, FUNCTIONAL, GIN, IndexPlanner (+1 more)

### Community 89 - "String Similarity Evaluator"
Cohesion: 0.15
Nodes (13): 10.1 Default transport — `transport/jdk/JdkHttpTransport` (§2, §6.4), 10.2 Batching throttle — `BatchingSpec` + `executeAll`, 10. Milestone 2 — Default JDK transport + batching throttle, 1. Design Principles, 2. Package Structure, 4. The Execution Pipeline, 5. Requirements → Design Mapping, 7. Security Contract (non-negotiable) (+5 more)

### Community 90 - "Boolean Logic Evaluator"
Cohesion: 0.14
Nodes (12): Override, SSLContext, URI, X509TrustManager, JdkHttpTransport, MtlsKey, HttpClient, SSLParameters (+4 more)

### Community 91 - "String Fuzzy Match Evaluator"
Cohesion: 0.22
Nodes (9): 1. The Big Idea, 2.1 `IHttpRequestOptions` — the _canonical HTTP request_ (transport contract), 2.2 `DeclarativeRestApiSettings.ResultOptions` — the _request plan_ (engine contract), 2. The Two Configuration Layers, 3. The Runtime Engine — `routing-node.ts`, Expression context keys observed in the wild, `getRequestOptionsFromParameters` — the merge rules (critical semantics), Research — n8n's Declarative HTTP System & Real-World Usage (+1 more)

### Community 92 - "Join Types"
Cohesion: 0.22
Nodes (9): 4.1 `requestDefaults` — node-level defaults, 4.2 `routing.request` — per-operation request shape, 4.3 `routing.send` — parameter value → request mapping, 4.4 `routing.output` + `postReceive` — response shaping, 4.5 Pagination, 4.6 Credentials / authentication, 4.7 Design-time (metadata) HTTP — `loadOptions` / `listSearch` / resource locators, 4.8 Transport / security behaviors worth porting (+1 more)

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

### Community 98 - "Shell Execution Exception"
Cohesion: 0.14
Nodes (5): PluginRegistry, Override, Pattern, ShellPlaceholderResolver, ShellExecutionException

### Community 99 - "Greeter Plugin"
Cohesion: 0.67
Nodes (3): FriendlyGreeter, Greeter, @PluginInfo

### Community 101 - "Headers"
Cohesion: 0.32
Nodes (5): DisplayName, Override, Test, OffsetPaginationTest, QueueTransport

### Community 104 - ".capsAtMax"
Cohesion: 0.07
Nodes (34): BearerAuth, BinaryBody, BodyKind, BINARY, FORM, JSON, NONE, RAW (+26 more)

### Community 142 - "Literal"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/spec/`), Hand-off context, Hand-off to next task, Objective, Task 01 — Spec model (`spec/`)

### Community 143 - ".create"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/transport/`), Hand-off context, Hand-off to next task, Objective, Task 02 — Transport value object + SPI (`transport/`)

### Community 144 - "ChoiceRoutingWorkflowTest.java"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/json/`), Hand-off context, Hand-off to next task, Objective, Task 03 — JSON / object-mapper SPI (`json/`)

### Community 145 - "FormSchemaJacksonTest.java"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/expr/`), Hand-off context, Hand-off to next task, Objective, Task 04 — Expression SPI + JEXL + `doa.*` namespace (`expr/`)

### Community 146 - "ServiceLoaderPluginLoader"
Cohesion: 0.06
Nodes (14): EvaluationError, AstEvaluator, ChildResult, DefaultEvaluationCache, Override, EvaluationCache, EvaluationResult, Override (+6 more)

### Community 147 - "PagePaginationTest.java"
Cohesion: 0.15
Nodes (9): CursorPagination, Override, SuppressWarnings, PaginationSupport, Body, JsonBody, CursorPaginationTest, DisplayName (+1 more)

### Community 148 - "SchemaException"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/error/`), Hand-off context, Hand-off to next task, Objective, Task 05 — Error model (`error/`)

### Community 149 - "dynamic-form-core/build.gradle"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/security/`), Hand-off context, Hand-off to next task, Objective, Task 06 — Security-first behaviors (`security/`)

### Community 150 - "dynamic-form-core/settings.gradle"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/plan/`), Hand-off context, Hand-off to next task, Objective, Task 07 — Plan & context (`plan/`) — integration checkpoint

### Community 151 - "Task 09 — Authenticator SPI + generic auth (`auth/`)"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/auth/`), Hand-off context, Hand-off to next task, Objective, Task 09 — Authenticator SPI + generic auth (`auth/`)

### Community 152 - "Task 10 — OAuth2 two-phase config + token lifecycle (`auth/oauth2/`)"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/auth/oauth2/`), Hand-off context, Hand-off to next task, Objective, Task 10 — OAuth2 two-phase config + token lifecycle (`auth/oauth2/`)

### Community 153 - "Task 11 — Pre/Post processors + registry (`action/`)"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create, Hand-off context, Hand-off to next task, Objective, Task 11 — Pre/Post processors + registry (`action/`)

### Community 154 - "Task 12 — Pagination (`pagination/`)"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/pagination/`), Hand-off context, Hand-off to next task, Objective, Task 12 — Pagination (`pagination/`)

### Community 155 - "Task 13 — Engine + pipeline (`engine/`)"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/engine/`), Hand-off context, Hand-off to next task, Objective, Task 13 — Engine + pipeline (`engine/`)

### Community 156 - "Task 14 — Config facade (`config/`)"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Design notes, Files to create (`src/main/java/io/github/khezyapp/dhttp/config/`), Hand-off context, Hand-off to next task, Objective, Task 14 — Config facade (`config/`)

### Community 157 - "Task 15 — Acceptance milestone (§9 of the design doc)"
Cohesion: 0.25
Nodes (7): Acceptance criteria, Completion, Design notes, Files to create (all in `src/test/java/io/github/khezyapp/dhttp/acceptance/`), Hand-off context, Objective, Task 15 — Acceptance milestone (§9 of the design doc)

### Community 158 - "BuiltinSupport"
Cohesion: 0.21
Nodes (9): DSLContext, Field, Override, SuppressWarnings, JooqSqlDialect, Condition, SelectFieldOrAsterisk, SelectQuery (+1 more)

### Community 159 - "6. SPI Details (what implementers extend)"
Cohesion: 0.29
Nodes (7): 6.1 `ExpressionEvaluator` — JEXL default, 6.2 `CredentialStore` / `CredentialService` — unified credential management, 6.3 `Authenticator`, 6.4 `HttpTransport`, 6.5 `PaginationStrategy`, 6.6 OAuth2 — two-phase configuration & token lifecycle, 6. SPI Details (what implementers extend)

### Community 160 - "Declarative HTTP — v1 Implementation Action Plan"
Cohesion: 0.29
Nodes (6): Cross-cutting acceptance guardrails, Declarative HTTP — v1 Implementation Action Plan, Dependency graph, Module & conventions (read first), Sequencing notes, Task list

### Community 161 - "README.md"
Cohesion: 0.25
Nodes (5): Contents, declarative-http — Scrum Workspace, Flow, Status: Implementation done (v1 milestone), Status: Milestone 2 done (default JDK transport + batching throttle)

### Community 165 - "JoinType"
Cohesion: 0.15
Nodes (10): PaginationRegistry, FunctionalInterface, PaginationStrategyFactory, PaginationSpec, DisplayName, Test, PaginationRegistryTest, DisplayName (+2 more)

### Community 166 - ".create"
Cohesion: 0.12
Nodes (9): JoinType, CROSS, INNER, LEFT, RIGHT, SortDirection, ASC, DESC (+1 more)

### Community 167 - "JexlExpressionEvaluator"
Cohesion: 0.25
Nodes (5): Documented, Retention, MarkAsImmute, ImmutableStrategy, Override

### Community 169 - "Cloner"
Cohesion: 0.29
Nodes (4): DateDiffEvaluator, DateExtractEvaluator, DateFormatEvaluator, DateParseEvaluator

### Community 170 - "DisplayName"
Cohesion: 0.24
Nodes (5): Override, NextUrlPagination, DisplayName, Test, NextUrlPaginationTest

### Community 171 - "SecretRedactor"
Cohesion: 0.27
Nodes (4): SecretRedactor, DisplayName, Test, SecretRedactorTest

### Community 173 - ".of"
Cohesion: 0.24
Nodes (7): BatchingSpec, BatchingAcceptanceTest, DisplayName, Override, SecretKey, Test, TimingTransport

### Community 174 - ".evaluate"
Cohesion: 0.31
Nodes (5): InMemoryTokenStore, DisplayName, SecretKey, Test, OAuth2RequestTimeAcceptanceTest

### Community 175 - "EvaluationScope"
Cohesion: 0.25
Nodes (8): Custom CredentialRepository / CredentialCipher / KeyProvider, Custom ExpressionEvaluator, Custom HttpTransport, Custom pagination, Custom post-receive action, Custom TokenStore, Extending the library, Pre-send hooks

### Community 176 - ".describeReturnsShapedOptions"
Cohesion: 0.36
Nodes (3): ActionRegistry, FunctionalInterface, PostReceiveFactory

### Community 177 - "JexlEngineFactory.java"
Cohesion: 0.25
Nodes (6): FunctionalInterface, JexlEngine, JexlEngineFactory, ScopeCustomizer, JexlBuilder, MapContext

### Community 178 - "ListAdapter"
Cohesion: 0.39
Nodes (3): Override, SuppressWarnings, ListAdapter

### Community 179 - "Auth"
Cohesion: 0.13
Nodes (14): AuthRequest, RequestContext, CredentialRef, HttpRequestSpec, Builder, SuppressWarnings, Output, CustomPostReceive (+6 more)

### Community 180 - "OAuth2Grant"
Cohesion: 0.06
Nodes (23): Authenticator, FunctionalInterface, CredentialStore, FunctionalInterface, OAuth2Credentials, OAuth2AuthorizationFlow, OAuth2Grant, AUTHORIZATION_CODE (+15 more)

### Community 181 - "OAuth2AuthorizationFlow"
Cohesion: 0.17
Nodes (8): DSLContext, Override, JooqDbAccessor, SqlRenderStyle, INDEXED, INLINED, NAMED, Select

### Community 182 - "QueueTransport"
Cohesion: 0.14
Nodes (7): DbAccessEvaluator, Override, SuppressWarnings, DbAccessor, DryRunResult, DbFieldAccessEvaluator, LinkMetadata

### Community 184 - "PagePaginationTest.java"
Cohesion: 0.45
Nodes (3): DisplayName, Test, PagePaginationTest

### Community 187 - "HttpMethod"
Cohesion: 0.30
Nodes (5): Getter, OAuth2NotConfiguredException, DisplayName, Test, OAuth2AuthorizationFlowTest

### Community 188 - "OAuth2ConfigTimeAcceptanceTest"
Cohesion: 0.18
Nodes (3): DeclarativeHttpEngine, OptionItem, OptionPage

### Community 189 - "khezy-dhttp-testing/SKILL.md"
Cohesion: 0.33
Nodes (5): 1. Records come from post-receives, not from the response itself, 2. Measuring batching pacing in a fake transport, 3. n8n batching semantics (throttle, not payload-combining), 4. Worker-thread exceptions, 5. Local HTTP(S) servers for transport tests

### Community 190 - "DeclarativeHttpConfigTest.java"
Cohesion: 0.24
Nodes (3): EvaluationScope, SuppressWarnings, RequestPlanner

### Community 192 - ".registerDateBuiltins"
Cohesion: 0.34
Nodes (6): ClientTlsConfigTest, DisplayName, KeyPair, PrivateKey, Test, X509Certificate

### Community 193 - ".evaluate"
Cohesion: 0.17
Nodes (8): HttpMethod, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT

### Community 194 - "ArrayFormat"
Cohesion: 0.20
Nodes (8): AuthResult, Setter, DecryptedCredential, Override, Override, GenericAuthenticatorTest, DisplayName, Test

### Community 195 - ".switchIsExhaustive"
Cohesion: 0.07
Nodes (16): BinaryData, Override, BuiltinSupport, SuppressWarnings, Override, Override, Override, Override (+8 more)

### Community 196 - ".evaluate"
Cohesion: 0.08
Nodes (20): ChronoUnit, Override, Override, Override, Override, Override, DateMinusEvaluator, Override (+12 more)

### Community 197 - "InstalledPlugin"
Cohesion: 0.15
Nodes (7): InstalledPlugin, ClasspathSource, FileSource, URL, PluginSource, UrlSource, PluginStore

### Community 198 - "ServiceLoaderPluginLoader"
Cohesion: 0.24
Nodes (8): Target, BODY, QUERY, IgnoreClone, Documented, Retention, Retention, PluginInfo

### Community 200 - "FilterItemsTest.java"
Cohesion: 0.12
Nodes (16): BinaryData, FilterItems, SetKeyValue, SortByKey, FilterItemsTest, DisplayName, Test, DisplayName (+8 more)

### Community 202 - ".fromMap"
Cohesion: 0.53
Nodes (4): LimitItems, DisplayName, Test, LimitItemsTest

### Community 203 - "certificate-util"
Cohesion: 0.33
Nodes (5): certificate-util, ClientTlsConfig, Coordinates, Dependency, What it does

### Community 206 - "Cloner"
Cohesion: 0.51
Nodes (3): ActionRegistryTest, DisplayName, Test

### Community 207 - "3. Core Abstractions — Java API Sketch"
Cohesion: 0.40
Nodes (5): 3.1 The spec model (declarative, immutable), 3.2 The plan & context (per-item runtime), 3.3 The engine (pure, deterministic), 3.4 Transport-neutral request value object, 3. Core Abstractions — Java API Sketch

### Community 214 - "Scenario demos"
Cohesion: 0.40
Nodes (5): (a) An api-key REST API with two operations, (b) OAuth2, Google-Sheets style (two phases), (c) Searchable/paginated dropdown via `describe(...)`, (d) Guidelines: custom post-receive action for `describe(...)`, Scenario demos

## Knowledge Gaps
- **336 isolated node(s):** `$schema`, `.opencode/plugins/graphify.js`, `EQUAL`, `LESS_THAN`, `LESS_OR_EQUAL` (+331 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **26 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Condition` connect `BuiltinSupport` to `Sensitive Masker Builders`, `Clone Ignore Strategy`, `Built-in Function Registration`?**
  _High betweenness centrality (0.278) - this node is a cross-community bridge._
- **Why does `Target` connect `ServiceLoaderPluginLoader` to `Clone Ignore Strategy`, `Sensitive Masker`, `Sensitive Masker Builders`, `JexlExpressionEvaluator`?**
  _High betweenness centrality (0.116) - this node is a cross-community bridge._
- **Why does `Operation` connect `Clone Ignore Strategy` to `Built-in Function Registration`, `Auth`, `OAuth2Grant`, `Aggregate Query Family`, `Sensitive Masker Builders`, `OAuth2ConfigTimeAcceptanceTest`, `BuiltinSupport`?**
  _High betweenness centrality (0.107) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `HttpResult` (e.g. with `.copiesHeaders()` and `.decodesBytes()`) actually correct?**
  _`HttpResult` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 28 inferred relationships involving `Arguments` (e.g. with `.missingFieldNameArg()` and `.evaluate()`) actually correct?**
  _`Arguments` has 28 INFERRED edges - model-reasoned connections that need verification._
- **What connects `$schema`, `.opencode/plugins/graphify.js`, `EQUAL` to the rest of the system?**
  _336 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Arithmetic Evaluation Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.11818181818181818 - nodes in this community are weakly interconnected._