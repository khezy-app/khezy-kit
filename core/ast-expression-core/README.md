# AST Expression Core

**Version 1.0.0** — A reusable, extensible Abstract Syntax Tree (AST) expression evaluation library for Java.

> This library grew from studying how **[Marble](https://github.com/checkmarble/marble-backend)** (an open-source AML engine) and **[easy-rules](https://github.com/j-easy/easy-rules)** (a lightweight Java rules engine) approach expression evaluation. Marble's AST-first evaluation model and easy-rules' pluggable architecture showed us how much the Java ecosystem was missing a simple, reusable AST evaluator core. This project is our attempt to fill that gap — nothing more. If [Marble's Go implementation](https://github.com/checkmarble/marble-backend) or [easy-rules](https://github.com/j-easy/easy-rules) already solves your problem, use them. We built this because we needed something in between.

---

## Overview

AST Expression Core provides a complete framework for defining, registering, and evaluating tree-structured expressions. It is designed for rule engines, dynamic query builders, and any system that needs to evaluate declarative expressions against input payloads.

The library features a recursive evaluation engine with pluggable function definitions, short-circuit optimization, result caching, comprehensive error handling, null-strategy support, and a rich set of built-in evaluators covering arithmetic, boolean logic, string operations, date/time manipulation, collections, and coalescing. An additional SQL data-access layer with jOOQ dialect support enables safe database query building and execution directly from AST expressions. A dynamic index detection subsystem analyzes query patterns and recommends database indexes.

---

## Quick Start

```java
// 1. Create a function registry with all built-in evaluators
var registry = FunctionRegistry.withBuiltins(NullStrategies.PROPAGATE);

// 2. Build an AST: 1 + 2 * 3
var ast = Node.function(CoreFunctions.ADD,
    Node.constant(1),
    Node.function(CoreFunctions.MULTIPLY,
        Node.constant(2),
        Node.constant(3)));

// 3. Create an evaluation context with payload data
var ctx = new EvaluationContext.Builder(registry)
    .body(Map.of("field", "value"))
    .build();

// 4. Evaluate
var evaluator = new AstEvaluator();
var result = evaluator.evaluate(ast, ctx);
System.out.println(result.returnValue()); // 7.0

// 5. Get performance summary
var summary = EvaluationSummary.from(result);
System.out.println("Took " + summary.totalDurationNanos() + " ns");
```

---

## Core Concepts

### AST Model

The expression tree is built from **Nodes** identified by **FunctionIds**:

- **`Node`** — Immutable AST node that is either a constant value or a function invocation with positional/named child nodes.
- **`FunctionId`** — Sealed interface distinguishing built-in core functions (`Core`) from user-defined named functions (`Named`). Use `FunctionId.of("name")` to resolve.
- **`CoreFunctions`** — Central registry of all built-in function identifier constants (`ADD`, `AND`, `PAYLOAD`, `STRING_CONTAINS`, `NOW`, `COALESCE`, etc.).
- **`ParamSpec`** / **`ParamType`** — Parameter specification with name, required flag, expected type, and default value. `ParamType` enumerates `BOOLEAN`, `INTEGER`, `FLOAT`, `STRING`, `LIST`, `MAP`, `ANY`.
- **`Arguments`** — Container for resolved positional and named argument values passed to evaluators.

### Function System

- **`Evaluator`** — `@FunctionalInterface` that receives an `EvaluationContext` and `Arguments`, returning `EvaluationOutcome`.
- **`FunctionDefinition`** — Complete registration binding a `FunctionId` to an `Evaluator`, along with parameter specs, `FunctionAttributes`, and optional null strategy. Built via the fluent `Builder`.
- **`FunctionRegistry`** — Thread-safe, freeze-able registry. Use `withBuiltins(NullStrategies)` for a pre-populated registry or `empty()` for custom registration.
- **`FunctionAttributes`** — Describes evaluation attributes: lazy child evaluation (short-circuit), commutativity, and computational cost for optimization.

### Evaluation Engine

- **`AstEvaluator`** — The recursive engine that walks the AST and produces `EvaluationResult`. The pipeline: cache lookup → constant shortcut → child evaluation (with short-circuit) → named child evaluation → error propagation → argument assembly → null strategy → validation → invocation → result construction.
- **`EvaluationContext`** — Immutable context carrying the registry, input message (payload), evaluation cache, and configuration flags (dry-run, circuit-breaking, cost optimization, clock).
- **`EvaluationCache`** / **`DefaultEvaluationCache`** — Thread-safe cache backed by `ConcurrentHashMap` to avoid re-evaluating identical subtrees.

### Results

- **`EvaluationResult`** — Complete result of evaluating an AST node: function ID, return value, errors, child results, trace metadata, and attributes. Supports recursive flattening (`flatten()`, `flattenErrors()`).
- **`EvaluationOutcome`** — Immediate outcome from an evaluator (value + errors + attributes). Wrapped into `EvaluationResult` by the engine.
- **`EvaluationTrace`** — Metadata per result: skipped flag, cached flag, duration in nanos.
- **`EvaluationSummary`** — Aggregate statistics computed from a root result: total duration, total/skipped/cached node counts, all errors, per-function stats.
- **`FunctionStats`** — Per-function invocation count, cache hits, skips, and total duration.

### Errors

- **`ErrorCode`** — Sealed interface with `Standard` (predefined) and `Custom` variants. Create custom codes with `ErrorCode.of(code, description)`.
- **`StandardErrors`** — Predefined codes: `WRONG_ARG_COUNT`, `MISSING_NAMED_ARG`, `ARGUMENT_TYPE_MISMATCH`, `DIVISION_BY_ZERO`, `FUNCTION_NOT_FOUND`, `NULL_NOT_ALLOWED`, `RUNTIME_ERROR`, `DATA_NOT_FOUND`, `EMPTY_INPUT`, `MISSING_FIELD`, `NULL_FIELD_VALUE`, `SCHEMA_VALIDATION`, `INVALID_REGEX`, `INVALID_FILTER_VALUE`.
- **`EvaluationError`** — Record with error code, message, and optional source identifier.

### Null Strategies

- **`NullHandlingStrategy`** — `@FunctionalInterface` that decides what to do when a resolved argument is null.
- **`NullStrategies`** — Predefined implementations:
  - `PROPAGATE` — pass null through
  - `COERCE_DEFAULT` — substitute type-appropriate defaults (0, false, "")
  - `FAIL` — throw `IllegalArgumentException`

### Message

- **`Message`** — Payload container with headers and a body. The body is typically a `Map<String, Object>` accessed by the `payload` evaluator.

---

## Builtin Evaluators

### Arithmetic

| Function | ID | Description |
|---|---|---|
| Add | `add` | Addition of two numbers |
| Subtract | `subtract` | Subtraction of two numbers |
| Multiply | `multiply` | Multiplication of two numbers |
| Divide | `divide` | Division (returns `DIVISION_BY_ZERO` error on /0) |

### Boolean

| Function | ID | Description |
|---|---|---|
| AND | `and` | Logical AND with short-circuit (commutative) |
| OR | `or` | Logical OR with short-circuit (commutative) |
| NOT | `not` | Logical negation |

### Comparison

| Function | ID | Description |
|---|---|---|
| Equal | `eq` | Equality check (via `Objects.equals`) |
| Greater Than | `gt` | Greater-than comparison |
| Greater or Equal | `gte` | Greater-than-or-equal comparison |
| Less Than | `lt` | Less-than comparison |
| Less or Equal | `lte` | Less-than-or-equal comparison |

### String

| Function | ID | Description |
|---|---|---|
| Contains | `stringContains` | Checks if input contains substring |
| Starts With | `stringStartsWith` | Checks if input starts with prefix (case-insensitive opt) |
| Ends With | `stringEndsWith` | Checks if input ends with suffix (case-insensitive opt) |
| Fuzzy Match | `stringFuzzyMatch` | Levenshtein similarity against threshold |
| Similarity | `stringSimilarity` | Returns similarity score (levenshtein/jaroWinkler) |
| Match | `stringMatch` | Regex pattern matching |
| Length | `stringLength` | Returns string length |
| Trim | `stringTrim` | Trims whitespace |
| Substring | `stringSubstring` | Extracts substring (start/end) |
| Replace | `stringReplace` | Search-and-replace (literal or regex) |

### Date/Time

| Function | ID | Description |
|---|---|---|
| Now | `now` | Current timestamp from context clock |
| Date Plus | `datePlus` | Add duration to a date |
| Date Minus | `dateMinus` | Subtract duration from a date |
| Date Diff | `dateDiff` | Difference between two dates |
| Date Format | `dateFormat` | Format date as string |
| Date Parse | `dateParse` | Parse string to Instant |
| Extract Year | `extractYear` | Extract year component |
| Extract Month | `extractMonth` | Extract month component |
| Extract Day | `extractDay` | Extract day-of-month |
| Extract Hour | `extractHour` | Extract hour component |
| Extract Minute | `extractMinute` | Extract minute component |
| Extract Second | `extractSecond` | Extract second component |

### List

| Function | ID | Description |
|---|---|---|
| List | `list` | Creates an immutable list from positional args |

### Coalesce

| Function | ID | Description |
|---|---|---|
| Coalesce | `coalesce` | Returns first non-null positional argument |
| Default If Null | `defaultIfNull` | Returns value or optional default |

### Payload

| Function | ID | Description |
|---|---|---|
| Payload | `payload` | Access payload field by dot-separated path |

---

## SQL Data-Access Layer

The SQL layer enables building and executing database queries from AST expressions:

- **`SqlDialect`** — Interface for converting `DbQuery` models to SQL strings.
- **`SqlRenderStyle`** — Parameter style: `INDEXED`, `NAMED`, `INLINED`.
- **`DbAccessor`** — Interface for executing queries and aggregations. Includes `DryRunResult` for validation.
- **`DbAccessEvaluator`** — Evaluator for `dbAccess`: builds SELECT queries with columns, filters, limit/offset. Supports dry-run.
- **`DbAggregatorEvaluator`** — Evaluator for `dbAggregator`: validates aggregators against schema, builds aggregation queries.
- **`DbFieldAccessEvaluator`** — Evaluator for `dbFieldAccess`: resolves join paths and accesses fields across related tables.
- **`FilterBuilderEvaluator`** — Evaluator for `buildFilter`: builds `DbFilter` conditions with operator/type validation.
- **`JooqSqlDialect`** — jOOQ-based dialect implementation supporting all operators, aggregation functions, JOINs, ORDER BY, LIMIT/OFFSET.
- **`JooqDbAccessor`** — jOOQ-based accessor that executes queries via `DSLContext`.

### SQL Model

| Class | Description |
|---|---|
| `DbTable` | Table reference with optional schema and alias |
| `DbColumn` | Column reference qualified by table |
| `DbFilter` | Filter condition (source, operator, value) |
| `DbQuery` | Complete query model with builder |
| `DbAggregation` | Aggregation function on a column |
| `DbJoin` | JOIN specification with type and ON condition |
| `JoinType` | INNER, LEFT, RIGHT, CROSS |
| `DbOrder` | ORDER BY column and direction |
| `FilterValue` | Sealed: Literal, ColumnRef, Subquery, FunctionExpr |
| `TableMetadata` | Table metadata with fields and links |
| `FieldMetadata` | Column metadata (name, type, nullable, keys) |
| `LinkMetadata` | Foreign key relationship metadata |
| `SchemaRegistry` | Central schema registry with operator and aggregator validation |
| `DataType` | STRING, INTEGER, FLOAT, BOOLEAN, TIMESTAMP |
| `SortDirection` | ASC, DESC |

---

## Dynamic Index Detection

The `index/` subpackage analyzes AST expressions and recommends database indexes:

- **`AstIndexAnalyzer`** — Walks the AST, extracts query families from `dbAggregator`/`dbAccess` nodes, classifies filter operators, and resolves field expressions.
- **`ExpressionIndexResolver`** — Strategy for resolving AST nodes to index metadata.
- **`IndexResolverRegistry`** — Registry with built-in resolvers for `payload`, `field`, `to_tsvector`, `jsonb_extract_path_text`, `upper`, `lower`, `trim`.

### Index Model

| Class | Description |
|---|---|
| `ExpressionIndexMetadata` | Sealed: PlainColumn, FunctionalColumn, GinColumn, NonIndexable |
| `PlainColumn` | Directly indexable column |
| `FunctionalColumn` | Expression-based index (e.g., UPPER(col)) |
| `GinColumn` | GIN-indexable column with operator class |
| `NonIndexable` | Expression that cannot be indexed |
| `FieldExpression` | Field with optional transform function |
| `FilterCondition` | Filter with field, operator, and expression flag |
| `FilterOperator` | Enum of recognized operators (EQUAL, LESS_THAN, FUZZY_MATCH, JSONB_CONTAINS, etc.) |
| `AggregateQueryFamily` | Query access pattern with categorized conditions |
| `ConcreteIndex` | Physical index definition (B-tree or GIN) |
| `IndexFamily` | Set of columns for a composite index |
| `IndexType` | AGGREGATION, FUNCTIONAL, GIN |

### Index Planner

- **`IndexPlanner`** — Plans index families from query families, filters against existing indexes, minimizes overlapping families, projects to concrete indexes, and plans GIN indexes.
- **`RefinementMerger`** — Merges overlapping index families.

---

## Dry-Run Validation

Enable dry-run mode in `EvaluationContext` to validate expression structure without executing database queries:

```java
var ctx = new EvaluationContext.Builder(registry)
    .body(payload)
    .dryRun(true)
    .build();
```

In dry-run mode:
- `DbAccessEvaluator` returns metadata (`generatedSql`, `estimatedRowCount`, `availableColumns`) instead of executing queries.
- `DbAggregatorEvaluator` validates aggregator functions and field types against the schema registry without running the query.
- `DbFieldAccessEvaluator` validates join paths and returns fake values based on field data types.

---

## Usage Examples

### Basic expression evaluation

```java
var registry = FunctionRegistry.withBuiltins(NullStrategies.PROPAGATE);
var ctx = new EvaluationContext.Builder(registry).body(Map.of()).build();
var evaluator = new AstEvaluator();

// (10 - 4) / 2
var ast = Node.function(CoreFunctions.DIVIDE,
    Node.function(CoreFunctions.SUBTRACT,
        Node.constant(10), Node.constant(4)),
    Node.constant(2));

var result = evaluator.evaluate(ast, ctx);
System.out.println(result.returnValue()); // 3.0
```

### Database field access with join paths

```java
var registry = FunctionRegistry.empty(NullStrategies.PROPAGATE);
registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.DB_FIELD_ACCESS)
    .evaluator(new DbFieldAccessEvaluator(dbAccessor, schemaRegistry))
    .namedParam(ParamSpec.required("tableName", ParamType.STRING))
    .namedParam(ParamSpec.required("fieldName", ParamType.STRING))
    .namedParam(ParamSpec.required("path", ParamType.ANY))
    .build());

var ast = Node.function(CoreFunctions.DB_FIELD_ACCESS,
    List.of(),
    Map.of(
        "tableName", Node.constant("orders"),
        "fieldName", Node.constant("email"),
        "path", Node.constant(List.of("customer"))
    ));

var result = evaluator.evaluate(ast, ctx);
```

### Aggregation query

```java
var ast = Node.function(CoreFunctions.DB_AGGREGATOR,
    List.of(),
    Map.of(
        "tableName", Node.constant("orders"),
        "fieldName", Node.constant("amount"),
        "aggregator", Node.constant("SUM"),
        "filters", Node.constant(List.of())
    ));
var result = evaluator.evaluate(ast, ctx);
```

### Dry-run validation

```java
var ctx = new EvaluationContext.Builder(registry)
    .body(Map.of())
    .dryRun(true)
    .build();
var result = evaluator.evaluate(ast, ctx);
System.out.println(result.getAttribute("meta.generatedSql"));
System.out.println(result.getAttribute("meta.estimatedRowCount"));
```

### Custom evaluator registration

```java
var registry = FunctionRegistry.empty(NullStrategies.PROPAGATE);
var customId = FunctionId.of("custom:square");
registry.register(FunctionDefinition.builder()
    .functionId(customId)
    .evaluator((ctx, args) ->
        EvaluationOutcome.success(
            Math.pow(((Number) args.positional().get(0)).doubleValue(), 2)))
    .positionalParam(ParamSpec.required("x", ParamType.ANY))
    .build());

var ast = Node.function(customId, Node.constant(5));
var result = new AstEvaluator().evaluate(ast,
    new EvaluationContext.Builder(registry).body(Map.of()).build());
System.out.println(result.returnValue()); // 25.0
```

### Performance analysis with EvaluationSummary

```java
var result = evaluator.evaluate(ast, ctx);
var summary = EvaluationSummary.from(result);

System.out.println("Total nodes: " + summary.totalNodes());
System.out.println("Cached: " + summary.cachedNodes());
System.out.println("Skipped: " + summary.skippedNodes());
System.out.println("Duration: " + summary.totalDurationNanos() + " ns");
summary.perFunction().forEach((fn, stats) ->
    System.out.println(fn + ": called " + stats.count()
        + " times, " + stats.totalDurationNanos() + " ns"));
```

---

## Architecture

The library follows a 4-layer architecture:

```
┌─────────────────────────────────────────────────┐
│               AST Model Layer                    │
│  Node, FunctionId, CoreFunctions, Arguments     │
│  ParamSpec, ParamType                           │
├─────────────────────────────────────────────────┤
│            Evaluation Runtime                    │
│  Evaluator, FunctionDefinition,                 │
│  FunctionRegistry, FunctionAttributes           │
├─────────────────────────────────────────────────┤
│            Rule Abstraction Layer                │
│  AstEvaluator, EvaluationContext,               │
│  EvaluationCache, NullHandlingStrategy          │
│  EvaluationResult, EvaluationOutcome,           │
│  EvaluationTrace, EvaluationSummary             │
├─────────────────────────────────────────────────┤
│            Engine Strategies                     │
│  Built-in Evaluators, SQL Data-Access Layer,    │
│  Dynamic Index Detection                        │
└─────────────────────────────────────────────────┘
```

1. **AST Model Layer** — Defines the expression tree structure and function identification.
2. **Evaluation Runtime** — Provides the function system with registries, definitions, and attributes.
3. **Rule Abstraction Layer** — The evaluation engine, context, caching, null handling, and result types.
4. **Engine Strategies** — Concrete implementations: built-in evaluators, SQL data-access, and index detection.

---

## Inspiration

This library's design is deeply influenced by two open-source projects we learned from:

- **[Marble](https://github.com/checkmarble/marble-backend)** — A production AML engine written in Go. Marble's AST evaluation model (Node → recursive evaluator → trace) is the direct ancestor of our evaluation pipeline. The concepts of `FunctionId`, `Arguments`, `EvaluationTrace`, and the separation of AST (data) from evaluator (behavior) all come from Marble's architecture.

- **[easy-rules](https://github.com/j-easy/easy-rules)** — A lightweight Java rules engine. easy-rules showed us how a simple, composable API can make rule engines accessible. Its registry pattern, composite rules, and engine strategies inspired our `FunctionRegistry`, `FunctionAttributes`, and the 4-layer architecture.

Both projects are worth studying on their own. We encourage you to look at them — they may have what you need without reaching for yet another library.

---

## Dependencies

- **jOOQ 3.21.6** — API scope (only required by the SQL dialect layer). No runtime database dependency is required for AST evaluation alone.

---

## Building & Testing

```sh
# Build, test, and checkstyle
./gradlew :ast-expression-core:build

# Run tests only
./gradlew :ast-expression-core:test

# Checkstyle only
./gradlew :ast-expression-core:checkstyleMain

# Run a specific test
./gradlew :ast-expression-core:test --tests "*AstEvaluatorTest*"
```
