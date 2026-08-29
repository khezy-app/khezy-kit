# Graph Report - khezy-kit  (2026-08-30)

## Corpus Check
- 600 files · ~298,811 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 5420 nodes · 14564 edges · 290 communities (265 shown, 25 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 1101 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `013fa028`
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
- .create
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
- Design doc → action plan
- JacksonJsonMapperTest
- InMemoryCredentialRepository
- DisplayName
- CredentialLifecycleAcceptanceTest
- Scenario demos
- 01 — Principle & Reference Theory
- 02 — Reference Example: the n8n Guardrails node
- 03 — Java Library Implementation Mapping
- .create
- Files to create (all under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`)
- DisplayName
- .switchIsExhaustive
- Task 09 — Streaming core: `stream/TextChunker` + `stream/Tokenizer` + `redact/StreamRedactor` + stream variants & parity
- Task 10 — Pipeline (in-memory): `pipeline/GuardrailPipeline` + `pipeline/StageResult` + `internal/ParallelStageRunner` + `api/Guardrails` facade
- Task 03 — Redaction engine: `redact/Placeholders` + `redact/Redactor` + `internal/AhoCorasick`
- Task 08 — LLM check core: `policy/LlmContract` + `policy/LlmPolicyPrompts` + `checks/LlmCheck`
- Task 12 — Spring AI adapter: `securities/data-privacy-spring-ai` + `SpringAiLlmClassifier`
- Data Privacy & Content Security — Guardrails Research Docs
- Task 01 — Module scaffold (core + Spring AI adapter) & root wiring
- Task 04 — PII catalog: `policy/PiiPatterns` (33 patterns) + `policy/ChecksumValidators`
- Task 05 — Secret keys: `checks/SecretKeysCheck` + `internal/SecretCandidateFilter`
- Task 06 — URL + keyword checks: `checks/UrlsCheck` + `checks/KeywordsCheck`
- Task 07 — PII check + custom regex check: `checks/PiiCheck` + `checks/CustomRegexCheck`
- Task 11 — Streaming pipeline: `pipeline/StreamPipeline` + `Guardrails.scan(Reader)` / `redact(Reader, Writer)`
- Task 13 — Acceptance: guarantee-scope regression (G1–G7), end-to-end, READMEs
- Data Privacy — v1 Implementation Action Plan
- 11. Family B: LLM-as-judge checks — Spring AI is the default (decision §1.1)
- 9. Family A checks (deterministic)
- 12. Public API & usage scenarios
- 5. Core contract types
- 6. Family B: model-based checks (Java)
- SortDirection
- 2. Positioning: a data privacy & security library, not an LLM library
- CREDITS
- PiiPatternsTest
- AesGcmCredentialCipher
- RawResponse
- .decryptTyped
- Target
- Cloner
- PiiPatterns
- .capsAtMax
- HttpResultTest
- ServiceLoaderPluginLoader
- TextChunkerTest
- PagePaginationTest
- ObjectAccessor
- .sortsAscending
- OAuth2AuthorizationFlow
- Files to create
- DataPrivacyAdvisorStreamTest.java
- .withBuiltins
- Builder
- .of
- Task 04 — Composition acceptance: end-to-end order, guarantee regression, README
- Builder
- Task 02 — `DataPrivacyAdvisor` (mitigate pattern): builder, before/after, streaming
- Task 03 — `GuardrailAdvisor` (prevent pattern): builder, before/after, streaming
- .evaluate
- AdvisorTypesTest
- CustomRegexCheckTest
- .isStrictMatch
- GuardrailReport.java
- 8. Behavior specification
- .capsAtMax
- Advisors
- 11. Family B: LLM-as-judge checks — Spring AI is the default (decision §1.1)
- ChecksumValidators
- 10. Streaming engine (decision §1.4)
- 6. Pipeline
- 2. Positioning: a data privacy & security library, not an LLM library
- 3. Compliance / masking policy: guarantee scope
- 4. Module & package layout
- 7. Redactor & the `<ENTITY>` placeholder contract
- 8. PII catalog — the full entity list (decision §1.3)

## God Nodes (most connected - your core abstractions)
1. `HttpRequest` - 132 edges
2. `HttpResult` - 115 edges
3. `Arguments` - 114 edges
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

## Communities (290 total, 25 thin omitted)

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
Cohesion: 0.16
Nodes (8): DeclarativeHttp, BrevoSpecAcceptanceTest, BeforeEach, DisplayName, SecretKey, Test, FakeTransport, Override

### Community 4 - "Dynamic Objects Tests"
Cohesion: 0.06
Nodes (34): Benchmark, BenchmarkMode, Fork, ConditionEvaluator, ConditionEvaluatorTest, DisplayName, Test, Measurement (+26 more)

### Community 5 - "Date Evaluators"
Cohesion: 0.40
Nodes (4): DisplayName, Test, MarkerPagination, PaginationRegistryTest

### Community 6 - "String Contains Tests"
Cohesion: 0.10
Nodes (17): Override, Override, StringFuzzyMatchEvaluator, Override, Override, StringSimilarityEvaluator, Override, DisplayName (+9 more)

### Community 7 - "AST Index Analyzer"
Cohesion: 0.29
Nodes (7): 13. Threading & failure policy (03 §8, confirmed), 14. Testing strategy, 15. Relation to existing modules, 16. Non-goals & future work, 17. Standards & attribution, 1. Resolved decisions (answers to 03 §11), Data Privacy Core — Java Library Design (v1)

### Community 8 - "Index Family"
Cohesion: 0.06
Nodes (34): PiiEntity, AU_ABN, AU_ACN, AU_MEDICARE, AU_TFN, CREDIT_CARD, CRYPTO, DATE_TIME (+26 more)

### Community 9 - "Evaluation Cache"
Cohesion: 0.31
Nodes (3): EndToEndIndexDetectionTest, Test, RealWorldUseCaseTests

### Community 10 - "DB Aggregator Evaluator"
Cohesion: 0.05
Nodes (24): DbAccessEvaluator, Override, SuppressWarnings, DbAccessor, DryRunResult, DbAggregatorEvaluator, Override, SuppressWarnings (+16 more)

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
Cohesion: 0.10
Nodes (11): Charset, AbstractHttpTransport, URI, Multipart, PreparedBody, ArrayFormat, BRACKETS, COMMA (+3 more)

### Community 18 - "In-Memory Plugin Store"
Cohesion: 0.20
Nodes (6): InMemoryPluginStore, Override, InMemoryPluginStoreTest, BeforeEach, DisplayName, Test

### Community 19 - "Function Registry"
Cohesion: 0.10
Nodes (21): 10. Config reference — "when to apply the control", 11.1 Chain mechanics (why the defaults compose correctly), 11.2 Interplay table, 11. Ordering & composition, 12. Testing strategy, 13. Build & release plan, 14. Suggested implementation order (task sketch), 15. Non-goals & future work (explicitly not in v2) (+13 more)

### Community 20 - "Aggregate Query Family"
Cohesion: 0.07
Nodes (22): BinaryData, Override, CustomPostReceive, FilterItems, LimitItems, RootProperty, SetKeyValue, SuppressWarnings (+14 more)

### Community 21 - "NextUrlPagination"
Cohesion: 0.10
Nodes (17): PEMEncryptedKeyPair, PEMKeyPair, PKCS8EncryptedPrivateKeyInfo, PrivateKeyInfo, KeyStore, PrivateKey, SSLContext, TrustManager (+9 more)

### Community 22 - "Function Attributes"
Cohesion: 0.27
Nodes (8): Named, CreationTests, EqualityTests, FunctionIdTest, DisplayName, Nested, Test, ToStringTests

### Community 23 - "Evaluation Cache Builder"
Cohesion: 0.08
Nodes (3): EvaluationCache, Builder, Message

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
Cohesion: 0.07
Nodes (14): FunctionalInterface, PreSendAction, Override, Auth, BasicAuth, NoAuth, HttpRequest, Builder (+6 more)

### Community 31 - "Default State Machine Tests"
Cohesion: 0.27
Nodes (3): DefaultStateMachineTest, DisplayName, Test

### Community 32 - "Sensitive Masker Tests"
Cohesion: 0.06
Nodes (23): AesGcmCredentialCipher, Override, SecretKey, TypeReference, CredentialCipher, CredentialRepository, SuppressWarnings, CredentialSummary (+15 more)

### Community 33 - "Payload Evaluator Tests"
Cohesion: 0.29
Nodes (7): ErrorHandlingTests, DisplayName, Nested, Test, NestedAccessTests, PayloadEvaluatorTest, SingleLevelTests

### Community 34 - "Template Engine Tests"
Cohesion: 0.27
Nodes (3): ArgumentResolver, Test, TemplateEngineTest

### Community 35 - "Accessor Benchmark"
Cohesion: 0.12
Nodes (17): 5. Extracted User Requirements (the contract a Java core must meet), R10 — Credential abstraction & auth injection, R11 — Rich transport contract (parity with `IHttpRequestOptions`), R12 — Security-first defaults, R13 — Error model, R14 — Cancellation & lifecycle, R15 — Metadata/design-time mode, R16 — Deterministic, testable engine (+9 more)

### Community 36 - "Built-in Function Registration"
Cohesion: 0.08
Nodes (13): Builder, Builder, LlmCheckConfig, LlmClassifier, Override, LlmCheck, LlmContract, DisplayName (+5 more)

### Community 37 - "Clone Context"
Cohesion: 0.15
Nodes (10): CloneContext, SuppressWarnings, ArrayStrategy, Override, SuppressWarnings, Override, SuppressWarnings, Override (+2 more)

### Community 38 - "Function ID Tests"
Cohesion: 0.11
Nodes (8): DomainAllowList, SsrfGuard, DomainAllowListTest, DisplayName, Test, DisplayName, Test, SsrfGuardTest

### Community 39 - "JOOQ DB Accessor"
Cohesion: 0.24
Nodes (7): Redactor, DisplayName, Test, StreamCheckParityTest, DisplayName, Test, RedactorTest

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
Cohesion: 0.06
Nodes (43): DSLContext, Override, JooqDbAccessor, DSLContext, Field, Override, SuppressWarnings, JooqSqlDialect (+35 more)

### Community 49 - "InMemoryTokenStore"
Cohesion: 0.06
Nodes (21): CapturingClassifier, GuardrailsStreamingTest, DisplayName, Override, Test, CapturingClassifier, FlaggingClassifier, GuardrailsTest (+13 more)

### Community 50 - "Plugin Sources"
Cohesion: 0.15
Nodes (3): DeclarativeHttpEngine, OptionItem, OptionPage

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
Cohesion: 0.09
Nodes (15): OAuth2Credentials, HttpApiException, Getter, Getter, OAuth2NotConfiguredException, JacksonJsonMapper, AuthRequest, CredentialRef (+7 more)

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
Cohesion: 0.23
Nodes (7): CompositionOrderTest, ChatResponse, DisplayName, Override, Prompt, Test, RecordingChatModel

### Community 63 - "State Machine Builder Tests"
Cohesion: 0.45
Nodes (3): DisplayName, Test, StateMachineBuilderTest

### Community 64 - "Clone Ignore Strategy"
Cohesion: 0.25
Nodes (8): Builder, RequestShape, Builder, Send, DisplayName, Operation, Test, RequestPlannerTest

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
Cohesion: 0.05
Nodes (24): NotEvaluator, AstEvaluator, ChildResult, FunctionAttributes, Builder, FunctionDefinition, FunctionRegistry, ParamSpec (+16 more)

### Community 69 - "Core Temporal Utilities"
Cohesion: 0.05
Nodes (26): BuiltinSupport, SuppressWarnings, Override, Override, Override, Override, Override, OutputRecord (+18 more)

### Community 70 - "Error Codes"
Cohesion: 0.27
Nodes (4): Custom, ErrorCode, Standard, StandardErrors

### Community 71 - "Test Data Models"
Cohesion: 0.09
Nodes (21): Attach attributes when:, Attribute key convention:, Building a Rule Engine on Top, Choosing: Positional vs Named Arguments, Do NOT attach attributes when:, Extending AST Expression Core, How This Library Thinks About Expressions, Minimal rule engine example (+13 more)

### Community 72 - "Immutable Clone Strategy"
Cohesion: 0.27
Nodes (5): GuardrailAdvisorTest, ChatClientRequest, ChatClientResponse, DisplayName, Test

### Community 73 - "Immutable Clone Tests"
Cohesion: 0.29
Nodes (7): ClonesImmutableTest, DisplayName, MethodSource, ParameterizedTest, TestEnum, ACTIVE, INACTIVE

### Community 74 - "List Adapter"
Cohesion: 0.09
Nodes (32): CoalesceEvaluator, Override, DateMinusEvaluator, DateParseEvaluator, Override, NowEvaluator, DefaultIfNullEvaluator, Override (+24 more)

### Community 75 - "Type Adapter Implementations"
Cohesion: 0.28
Nodes (3): Entry, Override, MapCache

### Community 76 - "OAuth2RequestAuthenticatorTest"
Cohesion: 0.18
Nodes (7): GuaranteeScopeAdvisorTest, ChatResponse, DisplayName, Override, Prompt, Test, RecordingChatModel

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
Cohesion: 0.15
Nodes (7): InstalledPlugin, ClasspathSource, FileSource, URL, PluginSource, UrlSource, PluginStore

### Community 91 - "String Fuzzy Match Evaluator"
Cohesion: 0.50
Nodes (4): 3. The Runtime Engine — `routing-node.ts`, Expression context keys observed in the wild, `getRequestOptionsFromParameters` — the merge rules (critical semantics), The pipeline, precisely

### Community 92 - "Join Types"
Cohesion: 0.14
Nodes (14): 1. The Big Idea, 2.1 `IHttpRequestOptions` — the _canonical HTTP request_ (transport contract), 2.2 `DeclarativeRestApiSettings.ResultOptions` — the _request plan_ (engine contract), 2. The Two Configuration Layers, 4.1 `requestDefaults` — node-level defaults, 4.2 `routing.request` — per-operation request shape, 4.3 `routing.send` — parameter value → request mapping, 4.4 `routing.output` + `postReceive` — response shaping (+6 more)

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
Cohesion: 0.31
Nodes (5): DisplayName, Override, Test, OffsetPaginationTest, QueueTransport

### Community 104 - ".capsAtMax"
Cohesion: 0.09
Nodes (25): BearerAuth, FilePart, FormBody, DisplayName, Test, PagePaginationTest, HttpRequestTest, DisplayName (+17 more)

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
Nodes (15): BooleanLogicEvaluator, Override, ComparisonEvaluator, SuppressWarnings, EvaluationError, DefaultEvaluationCache, Override, CoreFunctions (+7 more)

### Community 147 - "PagePaginationTest.java"
Cohesion: 0.19
Nodes (3): EvaluationScope, SuppressWarnings, RequestPlanner

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
Cohesion: 0.05
Nodes (19): AuditRecord, ScanOutcome, FunctionalInterface, StreamCheck, Detector, FunctionalInterface, Override, Pattern (+11 more)

### Community 159 - "6. SPI Details (what implementers extend)"
Cohesion: 0.29
Nodes (7): 6.1 `ExpressionEvaluator` — JEXL default, 6.2 `CredentialStore` / `CredentialService` — unified credential management, 6.3 `Authenticator`, 6.4 `HttpTransport`, 6.5 `PaginationStrategy`, 6.6 OAuth2 — two-phase configuration & token lifecycle, 6. SPI Details (what implementers extend)

### Community 160 - "Declarative HTTP — v1 Implementation Action Plan"
Cohesion: 0.29
Nodes (6): Cross-cutting acceptance guardrails, Declarative HTTP — v1 Implementation Action Plan, Dependency graph, Module & conventions (read first), Sequencing notes, Task list

### Community 161 - "README.md"
Cohesion: 0.25
Nodes (5): Contents, declarative-http — Scrum Workspace, Flow, Status: Implementation done (v1 milestone), Status: Milestone 2 done (default JDK transport + batching throttle)

### Community 162 - "3. Core Abstractions — Java API Sketch"
Cohesion: 0.20
Nodes (7): JexlEngine, SuppressWarnings, JexlExpressionEvaluator, SetKeyValue, DisplayName, Test, SetKeyValueTest

### Community 165 - "JoinType"
Cohesion: 0.11
Nodes (8): GuardrailResult, CustomRegexCheck, Override, Pattern, Override, PiiCheck, Match, MatchSelection

### Community 166 - ".create"
Cohesion: 0.10
Nodes (15): Pattern, SecretConfig, Override, Pattern, SecretKeysCheck, SecretCandidateFilter, params(), SecretPreset (+7 more)

### Community 167 - "JexlExpressionEvaluator"
Cohesion: 0.25
Nodes (5): Documented, Retention, MarkAsImmute, ImmutableStrategy, Override

### Community 169 - "Cloner"
Cohesion: 0.21
Nodes (9): CustomRegexConfig, Pattern, PiiConfig, PiiCoverage, ALL, SELECTED, DisplayName, Test (+1 more)

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
Cohesion: 0.35
Nodes (4): DisplayName, SecretKey, Test, OAuth2RequestTimeAcceptanceTest

### Community 175 - "EvaluationScope"
Cohesion: 0.25
Nodes (8): Custom CredentialRepository / CredentialCipher / KeyProvider, Custom ExpressionEvaluator, Custom HttpTransport, Custom pagination, Custom post-receive action, Custom TokenStore, Extending the library, Pre-send hooks

### Community 176 - ".describeReturnsShapedOptions"
Cohesion: 0.24
Nodes (5): URI, RedirectPolicy, DisplayName, Test, RedirectPolicyTest

### Community 177 - "JexlEngineFactory.java"
Cohesion: 0.25
Nodes (6): FunctionalInterface, JexlEngine, JexlEngineFactory, ScopeCustomizer, JexlBuilder, MapContext

### Community 178 - "ListAdapter"
Cohesion: 0.39
Nodes (3): Override, SuppressWarnings, ListAdapter

### Community 179 - "Auth"
Cohesion: 0.08
Nodes (15): CredentialService, CredentialStore, FunctionalInterface, FunctionalInterface, SecretKey, KeyProvider, TokenStore, Builder (+7 more)

### Community 180 - "OAuth2Grant"
Cohesion: 0.07
Nodes (20): Override, OAuth2AuthorizationFlow, OAuth2Grant, AUTHORIZATION_CODE, CLIENT_CREDENTIALS, PASSWORD, REFRESH_TOKEN, OAuth2RequestAuthenticator (+12 more)

### Community 181 - "OAuth2AuthorizationFlow"
Cohesion: 0.36
Nodes (4): UrlsConfig, DisplayName, Test, UrlsCheckTest

### Community 182 - "QueueTransport"
Cohesion: 0.08
Nodes (11): GuardrailCheck, FunctionalInterface, GuardrailCheckFactory, FunctionalInterface, Guardrails, GuardrailsConfig, ParallelStageRunner, GuardrailPipeline (+3 more)

### Community 184 - "PagePaginationTest.java"
Cohesion: 0.17
Nodes (7): CapturingClassifier, FlaggingClassifier, GuardrailPipelineTest, DisplayName, Override, Test, ThrowingClassifier

### Community 187 - ".create"
Cohesion: 0.16
Nodes (10): Authenticator, FunctionalInterface, AuthResult, Setter, DecryptedCredential, Override, Override, GenericAuthenticatorTest (+2 more)

### Community 188 - "OAuth2ConfigTimeAcceptanceTest"
Cohesion: 0.22
Nodes (8): EndToEndGuardrailAdvisorTest, ChatResponse, DisplayName, Flux, Override, Prompt, Test, RecordingChatModel

### Community 189 - "khezy-dhttp-testing/SKILL.md"
Cohesion: 0.33
Nodes (5): 1. Records come from post-receives, not from the response itself, 2. Measuring batching pacing in a fake transport, 3. n8n batching semantics (throttle, not payload-combining), 4. Worker-thread exceptions, 5. Local HTTP(S) servers for transport tests

### Community 190 - "DeclarativeHttpConfigTest.java"
Cohesion: 0.12
Nodes (14): Hand-off Log — Data Privacy Spring AI v2 (Advisors), Task 01 — Shared types — DONE, Task 02 — DataPrivacyAdvisor — DONE, Task 03 — GuardrailAdvisor — DONE, Task 04 — Composition acceptance — DONE, Template (copy into the END of this file when a task finishes), Cross-cutting acceptance guardrails, Data Privacy Spring AI — v2 Advisor Action Plan (+6 more)

### Community 192 - ".registerDateBuiltins"
Cohesion: 0.34
Nodes (6): ClientTlsConfigTest, DisplayName, KeyPair, PrivateKey, Test, X509Certificate

### Community 193 - ".evaluate"
Cohesion: 0.14
Nodes (11): BasicAuthCredentials, DeclarativeHttpConfigTest, BeforeEach, DisplayName, SecretKey, Test, DeclarativeHttpTest, BeforeEach (+3 more)

### Community 194 - "ArrayFormat"
Cohesion: 0.34
Nodes (5): RootProperty, DisplayName, Test, RootPropertyTest, BeforeEach

### Community 195 - ".switchIsExhaustive"
Cohesion: 0.11
Nodes (14): BinaryBody, Body, BodyKind, BINARY, FORM, JSON, NONE, RAW (+6 more)

### Community 196 - ".evaluate"
Cohesion: 0.24
Nodes (8): EndToEndDataPrivacyAdvisorTest, ChatResponse, DisplayName, Flux, Override, Prompt, Test, RecordingChatModel

### Community 197 - "InstalledPlugin"
Cohesion: 0.19
Nodes (5): Override, Pattern, URI, UrlsCheck, UrlSpan

### Community 198 - "ServiceLoaderPluginLoader"
Cohesion: 0.35
Nodes (4): DisplayName, SecretKey, Test, PaginationAcceptanceTest

### Community 200 - "FilterItemsTest.java"
Cohesion: 0.24
Nodes (8): FilterItems, SortByKey, FilterItemsTest, DisplayName, Test, DisplayName, Test, SortByKeyTest

### Community 201 - "SetKeyValueTest.java"
Cohesion: 0.07
Nodes (18): AhoCorasick, FunctionalInterface, MatchVisitor, Node, Output, Pattern, Placeholders, StreamRedactor (+10 more)

### Community 202 - ".fromMap"
Cohesion: 0.32
Nodes (5): ChecksumValidatorsTest, DisplayName, ParameterizedTest, Test, ValueSource

### Community 203 - "certificate-util"
Cohesion: 0.33
Nodes (5): certificate-util, ClientTlsConfig, Coordinates, Dependency, What it does

### Community 206 - "Cloner"
Cohesion: 0.20
Nodes (7): EndToEndSpringAiTest, ChatResponse, DisplayName, Override, Prompt, Test, StubChatModel

### Community 207 - "3. Core Abstractions — Java API Sketch"
Cohesion: 0.40
Nodes (5): 3.1 The spec model (declarative, immutable), 3.2 The plan & context (per-item runtime), 3.3 The engine (pure, deterministic), 3.4 Transport-neutral request value object, 3. Core Abstractions — Java API Sketch

### Community 208 - "NonStringKeyExpressionException.java"
Cohesion: 0.33
Nodes (5): DisplayName, MethodSource, ParameterizedTest, Test, PiiPatternsTest

### Community 209 - "Design doc → action plan"
Cohesion: 0.10
Nodes (19): `00-HANDOFF.md`, `00-INDEX.md`, Cross-cutting acceptance guardrails (write into INDEX), Deliverable specs, Design doc → action plan, Exit criteria, Focus mode (`focus` argument), Hand-off protocol (write into INDEX verbatim) (+11 more)

### Community 210 - "JacksonJsonMapperTest"
Cohesion: 0.26
Nodes (5): HttpErrorFactory, Pattern, HttpErrorFactoryTest, DisplayName, Test

### Community 211 - "InMemoryCredentialRepository"
Cohesion: 0.28
Nodes (6): DataPrivacyAdvisorTest, ChatClientRequest, ChatClientResponse, DisplayName, Test, UserMessage

### Community 212 - "DisplayName"
Cohesion: 0.21
Nodes (9): InMemoryTokenStore, RequestContext, Output, DeclarativeHttpEngineTest, FailingTransport, DisplayName, SuppressWarnings, Test (+1 more)

### Community 213 - "CredentialLifecycleAcceptanceTest"
Cohesion: 0.07
Nodes (14): BeanOutputConverter, Verdict, LlmPolicyPrompts, DisplayName, Test, LlmPolicyPromptsTest, Builder, ChatClient (+6 more)

### Community 214 - "Scenario demos"
Cohesion: 0.40
Nodes (5): (a) An api-key REST API with two operations, (b) OAuth2, Google-Sheets style (two phases), (c) Searchable/paginated dropdown via `describe(...)`, (d) Guidelines: custom post-receive action for `describe(...)`, Scenario demos

### Community 215 - "01 — Principle & Reference Theory"
Cohesion: 0.12
Nodes (16): 01 — Principle & Reference Theory, 0. The problem being solved, 10. Routing / operation theory, 11. Uniform result contract (the API surface to preserve), 1. Governing standards & reference bodies, 2.1 Family A — Deterministic / rule-based checks, 2.2 Family B — Model-based (LLM-as-a-judge / classifier) checks, 2. Core design pattern: a _check = (type × config)_ predicate (+8 more)

### Community 216 - "02 — Reference Example: the n8n Guardrails node"
Cohesion: 0.13
Nodes (15): 02 — Reference Example: the n8n Guardrails node, 10. Extension recipe (how new guardrails are added), 1. One uniform abstraction: the `CheckFn`, 2. Configuration surface (the "policy schema"), 3. Staged pipeline builder (`process.ts`), 4. Stage runner (`helpers/base.ts`), 5.1 `pii` — entity catalog + regex analyzer, 5.2 `secretKeys` — entropy + heuristics (+7 more)

### Community 217 - "03 — Java Library Implementation Mapping"
Cohesion: 0.10
Nodes (20): 03 — Java Library Implementation Mapping, 10. Suggested module layout, 11. Open questions to resolve in the implementation task, 1. Design goals (from the theory), 2. Core types (language-neutral → Java), 3. Pipeline (Theory §3, Reference §3–4), 4. Redactor (Theory §8), 5.1 `PiiCheck` — entity catalog + regex analyzer (Theory §6, Reference §5.1) (+12 more)

### Community 218 - ".create"
Cohesion: 0.31
Nodes (5): Override, DisplayName, Test, JexlExpressionEvaluatorTest, Sample

### Community 219 - "Files to create (all under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`)"
Cohesion: 0.15
Nodes (12): Acceptance criteria, `api/` — configuration schema (design §5.4 schema; §8 default table), `api/` — outcome records (design §5.3), `api/` — page/token budget & `Guardrails` placeholder, `api/` — records, functional types, facade, config, Files to create (all under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md) (+4 more)

### Community 220 - "DisplayName"
Cohesion: 0.12
Nodes (13): Getter, NonStringKeyExpressionException, FragmentMerger, SuppressWarnings, Builder, Operation, PreSend, Builder (+5 more)

### Community 221 - ".switchIsExhaustive"
Cohesion: 0.23
Nodes (7): KeywordsConfig, Override, Pattern, KeywordsCheck, DisplayName, Test, KeywordsCheckTest

### Community 222 - "Task 09 — Streaming core: `stream/TextChunker` + `stream/Tokenizer` + `redact/StreamRedactor` + stream variants & parity"
Cohesion: 0.17
Nodes (11): 1. `stream/TextChunker.java`, 2. `stream/Tokenizer.java`, 3. `redact/StreamRedactor.java`, 4. Streaming variants — `GuardrailCheck.toStream()` overrides, Acceptance criteria, Files to create (under `.../dpriv/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md) (+3 more)

### Community 223 - "Task 10 — Pipeline (in-memory): `pipeline/GuardrailPipeline` + `pipeline/StageResult` + `internal/ParallelStageRunner` + `api/Guardrails` facade"
Cohesion: 0.17
Nodes (11): 1. `pipeline/StageResult.java`, 2. `internal/ParallelStageRunner.java`, 3. `pipeline/GuardrailPipeline.java`, 4. `api/Guardrails.java` (fill the stub’s in-memory half), Acceptance criteria, Files to create (under `.../dpriv/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md) (+3 more)

### Community 224 - "Task 03 — Redaction engine: `redact/Placeholders` + `redact/Redactor` + `internal/AhoCorasick`"
Cohesion: 0.18
Nodes (10): 1. `redact/Placeholders.java`, 2. `internal/AhoCorasick.java`, 3. `redact/Redactor.java`, Acceptance criteria, Files to create (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective (+2 more)

### Community 225 - "Task 08 — LLM check core: `policy/LlmContract` + `policy/LlmPolicyPrompts` + `checks/LlmCheck`"
Cohesion: 0.18
Nodes (10): 1. `policy/LlmContract.java` — decision logic (testable without any LLM), 2. `policy/LlmPolicyPrompts.java` — bundled prompt templates, 3. `checks/LlmCheck.java`, Acceptance criteria, Files to create (under `.../dpriv/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective (+2 more)

### Community 226 - "Task 12 — Spring AI adapter: `securities/data-privacy-spring-ai` + `SpringAiLlmClassifier`"
Cohesion: 0.18
Nodes (10): 1. `SpringAiLlmClassifier.java`, 2. `SpringAiLlmClassifierFactory.java` (optional convenience), Acceptance criteria, Files to create (under `securities/data-privacy-spring-ai/src/main/java/io/github/khezyapp/dpriv/springai/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Installations, Objective (+2 more)

### Community 228 - "Data Privacy & Content Security — Guardrails Research Docs"
Cohesion: 0.18
Nodes (11): Data Privacy & Content Security — Guardrails Research Docs, Document map, For agents (copilot / coding agents), For humans, For the Java next-task handoff, Glossary, How to use these docs, Source & attribution (+3 more)

### Community 229 - "Task 01 — Module scaffold (core + Spring AI adapter) & root wiring"
Cohesion: 0.20
Nodes (9): Acceptance criteria, CREDITS.md (core module), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Module 1 — `securities/data-privacy-core`, Module 2 — `securities/data-privacy-spring-ai`, Objective, Root wiring — `settings.gradle` (repo root) (+1 more)

### Community 230 - "Task 04 — PII catalog: `policy/PiiPatterns` (33 patterns) + `policy/ChecksumValidators`"
Cohesion: 0.20
Nodes (9): 1. `policy/PiiPatterns.java`, 2. `policy/ChecksumValidators.java`, Acceptance criteria, Files to create (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective, Task 04 — PII catalog: `policy/PiiPatterns` (33 patterns) + `policy/ChecksumValidators` (+1 more)

### Community 231 - "Task 05 — Secret keys: `checks/SecretKeysCheck` + `internal/SecretCandidateFilter`"
Cohesion: 0.20
Nodes (9): 1. `internal/SecretCandidateFilter.java` (shared predicate, package-visible), 2. `checks/SecretKeysCheck.java`, Acceptance criteria, Files to create (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective, Task 05 — Secret keys: `checks/SecretKeysCheck` + `internal/SecretCandidateFilter` (+1 more)

### Community 232 - "Task 06 — URL + keyword checks: `checks/UrlsCheck` + `checks/KeywordsCheck`"
Cohesion: 0.20
Nodes (9): 1. `checks/UrlsCheck.java`, 2. `checks/KeywordsCheck.java`, Acceptance criteria, Files to create (under `.../dpriv/checks/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective, Task 06 — URL + keyword checks: `checks/UrlsCheck` + `checks/KeywordsCheck` (+1 more)

### Community 233 - "Task 07 — PII check + custom regex check: `checks/PiiCheck` + `checks/CustomRegexCheck`"
Cohesion: 0.20
Nodes (9): 1. `checks/CustomRegexCheck.java`, 2. `checks/PiiCheck.java`, Acceptance criteria, Files to create (under `.../dpriv/checks/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective, Task 07 — PII check + custom regex check: `checks/PiiCheck` + `checks/CustomRegexCheck` (+1 more)

### Community 234 - "Task 11 — Streaming pipeline: `pipeline/StreamPipeline` + `Guardrails.scan(Reader)` / `redact(Reader, Writer)`"
Cohesion: 0.20
Nodes (9): 1. `pipeline/StreamPipeline.java`, 2. `api/Guardrails.java` (fill the stub’s streaming half), Acceptance criteria, Files to create / edit (under `.../dpriv/`), Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective, Task 11 — Streaming pipeline: `pipeline/StreamPipeline` + `Guardrails.scan(Reader)` / `redact(Reader, Writer)` (+1 more)

### Community 235 - "Task 13 — Acceptance: guarantee-scope regression (G1–G7), end-to-end, READMEs"
Cohesion: 0.20
Nodes (9): 1. Guarantee regression suite — `securities/data-privacy-core/src/test/java/io/github/khezyapp/dpriv/GuaranteeScopeTest.java`, 2. Cross-module smoke test — `securities/data-privacy-spring-ai/src/test/.../EndToEndSpringAiTest.java`, 3. Documentation, Acceptance criteria, Deliverables, Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective (+1 more)

### Community 236 - "Data Privacy — v1 Implementation Action Plan"
Cohesion: 0.29
Nodes (7): Cross-cutting acceptance guardrails, Data Privacy — v1 Implementation Action Plan, Dependency graph, Hand-off protocol (required), Modules & conventions (read first), Sequencing notes, Task list

### Community 237 - "11. Family B: LLM-as-judge checks — Spring AI is the default (decision §1.1)"
Cohesion: 0.24
Nodes (8): Target, BODY, QUERY, IgnoreClone, Documented, Retention, Retention, PluginInfo

### Community 238 - "9. Family A checks (deterministic)"
Cohesion: 0.33
Nodes (6): 9.1 `PiiCheck` — §8 above., 9.2 `SecretKeysCheck` — entropy + heuristics (theory §5, 02 §5.2), 9.3 `UrlsCheck` — staged validator (theory §7, 02 §5.3), 9.4 `KeywordsCheck` — unicode-aware filter (02 §5.4), 9.5 `CustomRegexCheck`, 9. Family A checks (deterministic)

### Community 239 - "12. Public API & usage scenarios"
Cohesion: 0.40
Nodes (5): 12.1 Redact before logging, 12.2 Data pipeline sanitization (streaming), 12.3 DLP scan & quarantine, 12.4 LLM guardrails (one use case among several), 12. Public API & usage scenarios

### Community 240 - "5. Core contract types"
Cohesion: 0.40
Nodes (5): 5.1 `GuardrailResult` — the uniform contract (theory §11, unchanged shape), 5.2 Checks, 5.3 Outcome records, 5.4 Policy schema (03 §7, as records), 5. Core contract types

### Community 241 - "6. Family B: model-based checks (Java)"
Cohesion: 0.28
Nodes (4): Tokenizer, DisplayName, Test, TokenizerTest

### Community 242 - "SortDirection"
Cohesion: 0.12
Nodes (14): Attributions, CREDITS, API surface, Building and testing, Classification with an LLM classifier (Spring AI), Credits, data-privacy-core, Enabling keywords and URL validation (+6 more)

### Community 243 - "2. Positioning: a data privacy & security library, not an LLM library"
Cohesion: 0.54
Nodes (3): DisplayName, Test, SecretCandidateFilterTest

### Community 244 - "CREDITS"
Cohesion: 0.18
Nodes (8): HttpMethod, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT

### Community 249 - "PiiPatternsTest"
Cohesion: 0.18
Nodes (10): Building and testing, Built-in families, Custom classifier, data-privacy-spring-ai, Gradle, Installation, Introduction, Maven (+2 more)

### Community 250 - "AesGcmCredentialCipher"
Cohesion: 0.11
Nodes (13): Builder, DataPrivacyAdvisor, AdvisorChain, ChatClientRequest, ChatClientResponse, Flux, Override, Prompt (+5 more)

### Community 251 - "RawResponse"
Cohesion: 0.32
Nodes (3): ActionRegistry, FunctionalInterface, PostReceiveFactory

### Community 252 - ".decryptTyped"
Cohesion: 0.13
Nodes (12): Override, SSLContext, URI, X509TrustManager, JdkHttpTransport, MtlsKey, HttpClient, SSLParameters (+4 more)

### Community 253 - "Target"
Cohesion: 0.27
Nodes (5): CapturingClassifier, DisplayName, Override, Test, LlmCheckTest

### Community 254 - "Cloner"
Cohesion: 0.12
Nodes (17): Hand-off Log — Data Privacy v1, Task 01 — module scaffold — DONE, Task 02 — API contract — DONE, Task 03 — redaction engine — DONE, Task 04 — PII catalog — DONE, Task 05 — secret keys: SecretKeysCheck + SecretCandidateFilter — DONE, Task 06 — URL + keyword checks: UrlsCheck + KeywordsCheck — DONE, Task 07 — PII check + custom regex check — DONE (+9 more)

### Community 255 - "PiiPatterns"
Cohesion: 0.39
Nodes (3): DisplayName, Test, StreamRedactorTest

### Community 257 - "HttpResultTest"
Cohesion: 0.54
Nodes (3): HttpResultTest, DisplayName, Test

### Community 258 - "ServiceLoaderPluginLoader"
Cohesion: 0.28
Nodes (8): ChunkedChatModel, GuardrailAdvisorStreamTest, ChatResponse, DisplayName, Flux, Override, Prompt, Test

### Community 259 - "TextChunkerTest"
Cohesion: 0.37
Nodes (3): DisplayName, Test, TextChunkerTest

### Community 260 - "PagePaginationTest"
Cohesion: 0.09
Nodes (10): DataPrivacyException, GuardrailEvaluationException, PolicyViolationException, RedactionException, ProtectionScope, BOTH, INPUT, OUTPUT (+2 more)

### Community 261 - "ObjectAccessor"
Cohesion: 0.27
Nodes (6): Override, TypeReference, JacksonJsonMapperTest, DisplayName, Test, Sample

### Community 262 - ".sortsAscending"
Cohesion: 0.57
Nodes (3): DisplayName, Test, PaginationSpecTest

### Community 263 - "OAuth2AuthorizationFlow"
Cohesion: 0.13
Nodes (9): TypeReference, CursorPagination, Override, SuppressWarnings, PaginationSupport, JsonBody, CursorPaginationTest, DisplayName (+1 more)

### Community 264 - "Files to create"
Cohesion: 0.12
Nodes (15): 1. `ProtectionScope.java`, 2. `RedactMode.java`, 3. `RedactionReport.java`, 4. `GuardrailReport.java`, 5. `exception/DataPrivacyException.java`, 6. `exception/RedactionException.java`, 7. `exception/PolicyViolationException.java`, 8. `exception/GuardrailEvaluationException.java` (+7 more)

### Community 265 - "DataPrivacyAdvisorStreamTest.java"
Cohesion: 0.21
Nodes (10): ChatModel, ChunkedChatModel, DataPrivacyAdvisorStreamTest, ChatResponse, DisplayName, Flux, Override, Prompt (+2 more)

### Community 266 - ".withBuiltins"
Cohesion: 0.29
Nodes (7): CustomPostReceive, DisplayName, SuppressWarnings, Test, ActionRegistryTest, DisplayName, Test

### Community 267 - "Builder"
Cohesion: 0.33
Nodes (3): DateDiffEvaluator, DateExtractEvaluator, DatePlusEvaluator

### Community 268 - ".of"
Cohesion: 0.36
Nodes (4): BinaryData, HttpRequestSpecTest, DisplayName, Test

### Community 269 - "Task 04 — Composition acceptance: end-to-end order, guarantee regression, README"
Cohesion: 0.15
Nodes (12): 1. `EndToEndDataPrivacyAdvisorTest.java`, 2. `EndToEndGuardrailAdvisorTest.java`, 3. `CompositionOrderTest.java` — the parity/composition slice (design §11), 4. `GuaranteeScopeAdvisorTest.java` — G8–G16 regression (design §5.1), 5. README update — `securities/data-privacy-spring-ai/README.md`, Design notes, Files to create, Hand-off context (+4 more)

### Community 271 - "Task 02 — `DataPrivacyAdvisor` (mitigate pattern): builder, before/after, streaming"
Cohesion: 0.18
Nodes (10): 1. `DataPrivacyAdvisor.java`, Acceptance criteria, Behavior spec (design §8.1–8.4, pinned), Design notes, Files to create, Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective (+2 more)

### Community 272 - "Task 03 — `GuardrailAdvisor` (prevent pattern): builder, before/after, streaming"
Cohesion: 0.18
Nodes (10): 1. `GuardrailAdvisor.java`, Acceptance criteria, Behavior spec (design §8.5–8.8, pinned), Design notes, Files to create, Hand-off context, Hand-off to next task (log in 00-HANDOFF.md), Objective (+2 more)

### Community 273 - ".evaluate"
Cohesion: 0.09
Nodes (17): ChronoUnit, ArithmeticEvaluator, Override, Override, Override, Override, DateFormatEvaluator, Override (+9 more)

### Community 274 - "AdvisorTypesTest"
Cohesion: 0.37
Nodes (4): DisplayName, SecretKey, Test, OAuth2ConfigTimeAcceptanceTest

### Community 275 - "CustomRegexCheckTest"
Cohesion: 0.45
Nodes (3): CustomRegexCheckTest, DisplayName, Test

### Community 276 - ".isStrictMatch"
Cohesion: 0.35
Nodes (3): Holder, Pattern, PiiPatterns

### Community 277 - "GuardrailReport.java"
Cohesion: 0.12
Nodes (14): BaseAdvisor, GuardrailsOutcome, Operation, CLASSIFY, SANITIZE, GuardrailAdvisor, AdvisorChain, ChatClientRequest (+6 more)

### Community 278 - "8. Behavior specification"
Cohesion: 0.20
Nodes (10): 8.1 `DataPrivacyAdvisor.before(...)` — input redaction (scope INPUT | BOTH), 8.2 `DataPrivacyAdvisor.after(...)` — output redaction (scope OUTPUT | BOTH), 8.3 `DataPrivacyAdvisor.adviseStream(...)`, 8.4 `DataPrivacyAdvisor` failure semantics, 8.5 `GuardrailAdvisor.before(...)` — input gating (scope INPUT | BOTH), 8.6 `GuardrailAdvisor.after(...)` — output gating (scope OUTPUT | BOTH), 8.7 `GuardrailAdvisor.adviseStream(...)`, 8.8 `GuardrailAdvisor` failure semantics (+2 more)

### Community 279 - ".capsAtMax"
Cohesion: 0.53
Nodes (4): LimitItems, DisplayName, Test, LimitItemsTest

### Community 280 - "Advisors"
Cohesion: 0.22
Nodes (9): Advisors, Exception reference, Non-guarantees, Observability, Ordering rule, Quick-start 1 — redaction only (MITIGATE), Quick-start 2 — redaction + gating (MITIGATE + PREVENT), Scope and mode reference (+1 more)

### Community 281 - "11. Family B: LLM-as-judge checks — Spring AI is the default (decision §1.1)"
Cohesion: 0.33
Nodes (6): 11.1 The SPI stays in core (zero Spring dependency), 11.2 The contract (unchanged from theory §4 / 02 §6), 11.3 `SpringAiLlmClassifier` — the canonical implementation, 11.4 Built-in policy prompts (ported from 02 §6), 11.5 Claims, not guarantees (N1), 11. Family B: LLM-as-judge checks — Spring AI is the default (decision §1.1)

### Community 283 - "10. Streaming engine (decision §1.4)"
Cohesion: 0.50
Nodes (4): 10.1 `TextChunker` — windowed reader with overlap, 10.2 Bounded memory guarantee, 10.3 Streaming variants per check, 10. Streaming engine (decision §1.4)

### Community 284 - "6. Pipeline"
Cohesion: 0.50
Nodes (4): 6.1 In-memory path (classify / sanitize), 6.2 Streaming path (detect + redact — the data-pipeline/log use case), 6.3 Why classification (incl. LLM) is not streaming (decision §1.4), 6. Pipeline

### Community 285 - "2. Positioning: a data privacy & security library, not an LLM library"
Cohesion: 0.67
Nodes (3): 2.1 Use cases, 2.2 Design consequences of the reframing, 2. Positioning: a data privacy & security library, not an LLM library

### Community 286 - "3. Compliance / masking policy: guarantee scope"
Cohesion: 0.67
Nodes (3): 3.1 What the library guarantees, 3.2 What the library does NOT guarantee (claims, not guarantees), 3. Compliance / masking policy: guarantee scope

### Community 287 - "4. Module & package layout"
Cohesion: 0.67
Nodes (3): 4.1 `securities/data-privacy-core` — build.gradle, 4.2 `securities/data-privacy-spring-ai` — build.gradle, 4. Module & package layout

### Community 288 - "7. Redactor & the `<ENTITY>` placeholder contract"
Cohesion: 0.67
Nodes (3): 7.1 Placeholder contract (decision §1.2), 7.2 Replacement engine, 7. Redactor & the `<ENTITY>` placeholder contract

### Community 289 - "8. PII catalog — the full entity list (decision §1.3)"
Cohesion: 0.67
Nodes (3): 8.1 Analyzer engine, 8.2 Optional checksum validation (design addition, opt-in `strict` flag), 8. PII catalog — the full entity list (decision §1.3)

## Knowledge Gaps
- **713 isolated node(s):** `$schema`, `.opencode/plugins/graphify.js`, `EQUAL`, `LESS_THAN`, `LESS_OR_EQUAL` (+708 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **25 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Condition` connect `AST Evaluator Dry Run` to `Sensitive Masker Builders`, `DisplayName`, `Aggregate Query Family`, `Dynamic Objects Tests`?**
  _High betweenness centrality (0.271) - this node is a cross-community bridge._
- **Why does `Arguments` connect `List Adapter` to `String Utility Tests`, `Payload Evaluator Tests`, `Date Extraction Evaluator`, `String Contains Tests`, `DB Aggregator Evaluator`, `NonStringKeyExpressionException.java`, `.evaluate`, `ServiceLoaderPluginLoader`?**
  _High betweenness centrality (0.264) - this node is a cross-community bridge._
- **Why does `PiiEntity` connect `Index Family` to `NonStringKeyExpressionException.java`, `Cloner`, `.isStrictMatch`, `JoinType`?**
  _High betweenness centrality (0.206) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `HttpResult` (e.g. with `.copiesHeaders()` and `.decodesBytes()`) actually correct?**
  _`HttpResult` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 28 inferred relationships involving `Arguments` (e.g. with `.missingFieldNameArg()` and `.evaluate()`) actually correct?**
  _`Arguments` has 28 INFERRED edges - model-reasoned connections that need verification._
- **What connects `$schema`, `.opencode/plugins/graphify.js`, `EQUAL` to the rest of the system?**
  _713 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Arithmetic Evaluation Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.11818181818181818 - nodes in this community are weakly interconnected._