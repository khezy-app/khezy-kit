# AST Expression Core — Design

## Overview

A reusable AST expression evaluation library under `io.github.khezyapp.ast.core`. Any project (AML, risk-rating, business rules, feature flags) can consume it. Provides Node, Evaluator, Registry, Context, Error handling, builtin evaluators, and SQL data-access evaluators.

## Package Structure

```
io.github.khezyapp.ast.core/
├── CoreUtils.java                          # Shared utilities
│
├── model/                                  # Core data types
│   ├── Node.java                           # AST node (immutable)
│   ├── FunctionId.java                     # Sealed identity: Core / Named
│   ├── CoreFunctions.java                  # Built-in function ID constants
│   ├── ParamSpec.java                      # Parameter spec
│   ├── ParamType.java                      # Parameter type enum
│   └── Arguments.java                      # Resolved positional + named values
│
├── function/                               # Function system
│   ├── FunctionDefinition.java             # Function + evaluator + params + metadata
│   ├── FunctionAttributes.java             # Per-function metadata (short-circuit, cost)
│   └── FunctionRegistry.java              # Thread-safe registry + freeze
│
├── eval/                                   # Evaluation engine
│   ├── Evaluator.java                      # @FunctionalInterface
│   ├── AstEvaluator.java                   # Recursive evaluation loop
│   ├── EvaluationContext.java              # Runtime context (message, cache, flags, dryRun)
│   ├── EvaluationCache.java                # Thread-safe subtree cache
│   └── DefaultEvaluationCache.java         # Default cache implementation
│
├── result/                                 # Evaluation result types
│   ├── EvaluationOutcome.java              # Evaluator return (value + errors + attributes)
│   ├── EvaluationResult.java               # Result tree (value + trace + attributes)
│   ├── EvaluationTrace.java                # Skipped / cached / duration metadata
│   ├── EvaluationSummary.java              # Aggregate statistics
│   └── FunctionStats.java                  # Per-function statistics
│
├── error/                                  # Error handling
│   ├── ErrorCode.java                      # Extensible error code (sealed: Standard / Custom)
│   ├── StandardErrors.java                 # Built-in error codes
│   └── EvaluationError.java                # Structured error (code + message + source)
│
├── message/                                # Payload container
│   └── Message.java                        # Headers + body envelope
│
├── nullstrategy/                           # Null handling
│   ├── NullHandlingStrategy.java           # Pluggable null behavior
│   └── NullStrategies.java                 # Built-in strategies (PROPAGATE, COERCE, FAIL)
│
├── builtin/                                # Builtin evaluator implementations
│   ├── string/                             # String evaluator sub-package
│   │   ├── StringContainsEvaluator.java
│   │   ├── StringStartsWithEvaluator.java
│   │   ├── StringEndsWithEvaluator.java
│   │   ├── StringFuzzyMatchEvaluator.java
│   │   ├── StringSimilarityEvaluator.java
│   │   ├── StringMatchEvaluator.java
│   │   ├── StringLengthEvaluator.java
│   │   ├── StringTrimEvaluator.java
│   │   ├── StringSubstringEvaluator.java
│   │   └── StringReplaceEvaluator.java
│   ├── ArithmeticEvaluator.java
│   ├── BooleanLogicEvaluator.java
│   ├── CoalesceEvaluator.java
│   ├── ComparisonEvaluator.java
│   ├── DateEvaluators.java                  # Now, datePlus, dateMinus, dateDiff, dateFormat, dateParse, extract*
│   ├── DefaultIfNullEvaluator.java
│   ├── EqualEvaluator.java
│   ├── IsEmptyEvaluator.java
│   ├── ListEvaluator.java
│   ├── NotEvaluator.java
│   └── PayloadEvaluator.java
│
└── sql/                                    # SQL data-access evaluators
    ├── SqlDialect.java                     # Interface: translate DbQuery → SQL
    ├── SqlRenderStyle.java                 # INDEXED / NAMED / INLINED
    ├── DbAccessor.java                     # Interface: execute query + field access
    ├── DbAccessEvaluator.java              # dbAccess evaluator
    ├── DbAggregatorEvaluator.java          # dbAggregator evaluator
    ├── DbFieldAccessEvaluator.java         # dbFieldAccess evaluator (path-based)
    ├── FilterBuilderEvaluator.java         # buildFilter evaluator (construct DbFilter)
    ├── model/
    │   ├── DbTable.java                    # Table reference (name, schema, alias)
    │   ├── DbColumn.java                   # Column reference (table + name + alias)
    │   ├── DbFilter.java                   # Filter condition (source + op + value)
    │   ├── DbAggregation.java              # Aggregation spec (function + column + alias)
    │   ├── FilterValue.java                # Sealed: Literal | ColumnRef | Subquery | FunctionExpr
    │   ├── DbQuery.java                    # Full query model (from, joins, filters, aggregations, order)
    │   ├── DbJoin.java                     # Join clause (type, table, ON condition)
    │   ├── JoinType.java                   # INNER / LEFT / RIGHT / CROSS
    │   ├── DbOrder.java                    # ORDER BY spec (column + direction)
    │   ├── SortDirection.java              # ASC / DESC
    │   ├── TableMetadata.java              # Known table schema
    │   ├── FieldMetadata.java              # Column definition
    │   ├── LinkMetadata.java               # FK relationship definition
    │   ├── SchemaRegistry.java             # Registry of known tables for validation
    │   └── DataType.java                   # STRING / INTEGER / FLOAT / BOOLEAN / TIMESTAMP
    └── dialect/
        ├── JooqSqlDialect.java             # Default: jOOQ-based SQL generation
        └── JooqDbAccessor.java              # Default jOOQ DbAccessor implementation
```

## Core AST Model

### Node — single flexible AST node

```java
public final class Node {
    public static Node constant(Object value)
    public static Node function(FunctionId function, List<Node> children, Map<String, Node> namedChildren)
    public static Node function(FunctionId function, Node... children)

    public boolean isConstant()
    public Object constant()
    public FunctionId function()
    public List<Node> children()
    public Map<String, Node> namedChildren()
}
```

Two shapes: **constant leaf** (wraps a literal value) and **function call** (invokes an evaluator). Constants are produced only by `Node.constant()`. Named arguments pass metadata (table names, field names, config); positional children are sub-expressions evaluated recursively.

### Function Identity — sealed interface

```java
public sealed interface FunctionId permits FunctionId.Core, FunctionId.Named {
    record Core(String value) implements FunctionId {}
    record Named(String value) implements FunctionId {}
}
```

Constants in `CoreFunctions` for builtins: `EQUAL`, `AND`, `OR`, `NOT`, `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, `GREATER`, `GREATER_OR_EQUAL`, `LESS`, `LESS_OR_EQUAL`, `STRING_CONTAINS`, `STRING_STARTS_WITH`, `STRING_ENDS_WITH`, `STRING_FUZZY_MATCH`, `STRING_SIMILARITY`, `STRING_MATCH`, `STRING_LENGTH`, `STRING_TRIM`, `STRING_SUBSTRING`, `STRING_REPLACE`, `CONSTANT`, `PAYLOAD`, `IS_EMPTY`, `LIST`, `NOW`, `DATE_PLUS`, `DATE_MINUS`, `DATE_DIFF`, `DATE_FORMAT`, `DATE_PARSE`, `EXTRACT_YEAR`, `EXTRACT_MONTH`, `EXTRACT_DAY`, `EXTRACT_HOUR`, `EXTRACT_MINUTE`, `EXTRACT_SECOND`, `COALESCE`, `DEFAULT_IF_NULL`, `DB_ACCESS`, `DB_AGGREGATOR`, `DB_FIELD_ACCESS`, `BUILD_FILTER`.

## Evaluation Runtime

### Evaluator — functional interface

```java
@FunctionalInterface
public interface Evaluator {
    EvaluationOutcome evaluate(EvaluationContext ctx, Arguments args);
}
```

Never throw — always return `EvaluationOutcome.failure(...)`. The engine catches exceptions and wraps them as `RUNTIME_ERROR`.

### AstEvaluator — recursive evaluation loop

Phases:
1. Resolve function definition
2. Evaluate children (cached)
3. Build Arguments (positional + named)
4. Apply null strategies
5. Check short-circuit
6. Invoke evaluator
7. Build result tree with trace
8. Return EvaluationResult

### EvaluationContext

Carries: registry, message body (`Object`), evaluation cache, dry-run flag, clock, circuit-breaking flag, cost-optimization flag. Builder-pattern construction.

### NullHandlingStrategy

Per-function override via `FunctionDefinition.nullStrategy()` with `FunctionRegistry` fallback.

| Strategy | Behavior |
|----------|----------|
| `PROPAGATE` | Null args → null result |
| `COERCE` | Null → default value for type |
| `FAIL` | Null → evaluation error |

## Function System

### FunctionDefinition

```java
public final class FunctionDefinition {
    // Builder:
    .functionId(FunctionId)
    .evaluator(Evaluator)
    .positionalParam(ParamSpec)
    .namedParam(ParamSpec)
    .attributes(FunctionAttributes)
    .nullStrategy(NullHandlingStrategy)
    .build()
}
```

### FunctionRegistry

Thread-safe. Supports `freeze()`. `FunctionRegistry.withBuiltins(NullHandlingStrategy)` registers all builtin evaluators except SQL evaluators (`DbAccessEvaluator`, `DbAggregatorEvaluator`, `DbFieldAccessEvaluator`, `FilterBuilderEvaluator`) — consumers register those manually with their own `DbAccessor`.

## Results & Errors

### EvaluationOutcome — what evaluators return

```java
public static EvaluationOutcome success(Object value, Map<String, Object> attributes)
public static EvaluationOutcome failure(List<EvaluationError> errors)
```

Attributes follow convention: `evidence.*` (compliance), `audit.*` (identity), `meta.*` (performance), `debug.*` (diagnostic).

### EvaluationResult — tree node produced by engine

Contains: `function()`, `returnValue()`, `errors()`, `attributes()`, `trace()`, `children()`. Supports `flatten()` and `flattenErrors()`.

### Error handling

`ErrorCode` sealed interface: `Standard(code, message)` / `Custom(code, message)`. `StandardErrors` has constants: `WRONG_ARG_COUNT`, `WRONG_ARG_TYPE`, `MISSING_NAMED_ARG`, `NULL_NOT_ALLOWED`, `DIVISION_BY_ZERO`, `FUNCTION_NOT_FOUND`, `EVALUATION_ERROR`, `RUNTIME_ERROR`, `SCHEMA_VALIDATION`.

## SQL Data-Access Layer

### 7.1 Schema Model

```java
public record TableMetadata(String name, String schema, Map<String, FieldMetadata> fields, Map<String, LinkMetadata> links) {}
public record FieldMetadata(String name, String dataType, boolean nullable, boolean isPrimaryKey, boolean isForeignKey) {}
public record LinkMetadata(String name, String parentTableName, String parentFieldName, String childTableName, String childFieldName) {}
public enum DataType { STRING, INTEGER, FLOAT, BOOLEAN, TIMESTAMP }
```

**SchemaRegistry** — validates table/field existence, operator validity, type compatibility:

```java
public final class SchemaRegistry {
    SchemaRegistry(List<TableMetadata> tables)
    TableMetadata getTable(String name)
    FieldMetadata getField(String tableName, String fieldName)
    DataType getFieldType(String tableName, String fieldName)
    List<EvaluationError> validatePath(String startTable, List<String> path)
    LinkMetadata getLink(String tableName, String linkName)
    EvaluationError validateField(String tableName, String fieldName)
    boolean isOperatorValid(String operator)
    boolean isOperatorValidForType(String operator, DataType fieldType)
    boolean isUnaryOperator(String operator)
    boolean isAggregatorValid(String aggregator)
    boolean isAggregatorValidForType(String aggregator, DataType fieldType)
    Object defaultAggregatorValue(String aggregator)
    EvaluationError validateValueType(Object value, DataType fieldType)
    EvaluationError validateValueAgainstType(Object value, DataType fieldType)
    boolean hasTable(String tableName)
}
```

Supported operators: `=`, `!=`, `>`, `>=`, `<`, `<=`, `IN`, `NOT_IN`, `IS_NULL`, `IS_NOT_NULL`, `CONTAINS`, `NOT_CONTAINS`, `CONTAINS_ANY`, `NOT_CONTAINS_ANY`, `STARTS_WITH`, `ENDS_WITH`, `WILDCARD`, `MATCH`.

Supported aggregators: `COUNT`, `COUNT_DISTINCT`, `SUM`, `AVG`, `MAX`, `MIN`, `STDDEV`, `MEDIAN`, `PERCENTILE`.

### 7.2 Query Data Model

#### DbTable — table reference

```java
public record DbTable(String name, String schema, String alias) {
    public static DbTable of(String name)
    public String qualifiedName()
}
```

#### DbColumn — column reference

```java
public record DbColumn(DbTable table, String name, String alias) {
    public static DbColumn of(DbTable table, String name)
}
```

#### FilterValue — sealed expression type for filter conditions

```java
public sealed interface FilterValue
    permits FilterValue.Literal, FilterValue.ColumnRef,
            FilterValue.Subquery, FilterValue.FunctionExpr {

    record Literal(Object value) implements FilterValue {}
    record ColumnRef(DbColumn column) implements FilterValue {}
    record Subquery(DbQuery query) implements FilterValue {}
    record FunctionExpr(
        String function,
        List<FilterValue> args,
        Map<String, Object> qualifiers
    ) implements FilterValue {}
}
```

#### DbFilter — filter condition (WHERE clause)

```java
public record DbFilter(
    FilterValue source,
    String operator,
    FilterValue value
) {
    public static DbFilter of(DbColumn column, String operator, FilterValue value)
}
```

#### DbAggregation — SELECT aggregation

```java
public record DbAggregation(String function, DbColumn column, String alias, boolean distinct) {
    public static DbAggregation of(String function, DbColumn column, String alias)
}
```

#### DbJoin — JOIN clause

```java
public record DbJoin(
    JoinType type,
    DbTable targetTable,
    DbColumn sourceColumn,
    DbColumn targetColumn,
    List<DbFilter> extraConditions
) {}
```

#### JoinType

```java
public enum JoinType { INNER, LEFT, RIGHT, CROSS }
```

#### DbOrder — ORDER BY spec

```java
public record DbOrder(DbColumn column, SortDirection direction) {}
public enum SortDirection { ASC, DESC }
```

#### DbQuery — complete query

```java
public record DbQuery(
    DbTable from,
    List<DbColumn> columns,
    List<DbFilter> filters,
    List<DbColumn> groupBy,
    List<DbAggregation> aggregations,
    List<DbOrder> orderBy,
    List<DbJoin> joins,
    Long limit,
    Long offset
) {
    public static Builder builder() { return new Builder(); }
}
```

### 7.3 SqlDialect Interface

```java
public interface SqlDialect {
    String toSql(DbQuery query);
    String toSql(DbQuery query, SqlRenderStyle style);
    List<Object> extractBindValues(DbQuery query);
}

public enum SqlRenderStyle { INDEXED, NAMED, INLINED }
```

### 7.4 DbAccessor Interface

```java
public interface DbAccessor {
    List<Map<String, Object>> executeQuery(DbQuery query, Object focalObject);
    Map<String, Object> executeAggregation(DbQuery query, Object focalObject);
    String generateSql(DbQuery query);
    DryRunResult dryRunQuery(DbQuery query);
    SchemaRegistry getSchemaRegistry();
}

public record DryRunResult(List<String> availableColumns, long estimatedRowCount, String generatedSql) {}
```

### 7.5 Default: JooqSqlDialect + JooqDbAccessor

- `JooqSqlDialect` backed by jOOQ `DSLContext` (PostgreSQL dialect via injected DSLContext)
- Uses imperative `dsl.selectQuery()` API (not step-builder)
- Supports: SELECT, WHERE (all 18 operators), GROUP BY, aggregation (SUM/AVG/COUNT/MAX/MIN/STDDEV/MEDIAN/PERCENTILE), LIMIT/OFFSET, ORDER BY (ASC/DESC), INNER/LEFT/RIGHT/CROSS JOIN, schema-qualified tables, table aliases, column aliases, subquery IN/NOT_IN
- Renders function expressions: `UPPER`, `LOWER`, `LENGTH`, `TRIM`, `CONCAT`, `COALESCE`, `ABS` (with fallback for unknown functions)
- Handles `FilterValue.FunctionExpr` in both source and value positions
- Handles `FilterValue.Literal` in source position (e.g. `WHERE 1 = id`)
- `toSql(DbQuery, SqlRenderStyle)` overload maps to jOOQ `ParamType.{INDEXED,NAMED,INLINED}`
- `JooqDbAccessor` wraps `JooqSqlDialect` + consumer-provided `DSLContext` (no `DataSource` constructor)

### 7.6 Evaluator Implementations

#### DbAccessEvaluator — function id `"dbAccess"`

Named args: `tableName` (String), `columns` (List<String>), `filters` (List<DbFilter>), `limit` (Long), `offset` (Long).

Returns: `List<Map<String, Object>>`.

#### DbAggregatorEvaluator — function id `"dbAggregator"`

Named args: `tableName` (String), `fieldName` (String), `aggregator` (String), `filters` (List<DbFilter>), `groupBy` (List<String>).

Validation: checks aggregator is known, checks aggregator is valid for field's data type. On null result, returns `defaultAggregatorValue(aggregator)`.

Returns: `Number`.

#### DbFieldAccessEvaluator — function id `"dbFieldAccess"`

Named args: `tableName` (String), `fieldName` (String), `path` (List<String>, default empty).

Returns: `Object` — field value from target table via join path traversal. Null FK optimization: return null immediately if any FK is null.

#### FilterBuilderEvaluator — function id `"buildFilter"`

Named args: `tableName` (String), `fieldName` (String), `operator` (String), `value` (any), `valueType` (String, default `"literal"`).

Validation: operator is valid, operator is valid for field type, value type matches field type (for non-unary operators).

Returns: `DbFilter`.

### 7.7 Filter Expression Patterns

| Pattern | SQL | Model |
|---------|-----|-------|
| Simple column comparison | `WHERE name = 'SOK'` | `source=ColumnRef(name), op="=", value=Literal("SOK")` |
| Function on column | `WHERE UPPER(name) = 'SOK'` | `source=FunctionExpr("UPPER", [ColumnRef(name)]), op="=", value=Literal("SOK")` |
| Function with qualifier | `WHERE EXTRACT(YEAR FROM date) = 2024` | `source=FunctionExpr("EXTRACT", [ColumnRef(date)], {field: "YEAR"}), op="=", value=Literal(2024)` |
| Function with literal+column args | `WHERE DATE_TRUNC('month', date) = '2024-01-01'` | `source=FunctionExpr("DATE_TRUNC", [Literal("month"), ColumnRef(date)]), op="=", value=Literal("2024-01-01")` |
| Function on both sides | `WHERE LOWER(email) = LOWER(temp.email)` | `source=FunctionExpr("LOWER", [ColumnRef(email)]), op="=", value=FunctionExpr("LOWER", [ColumnRef(temp.email)])` |
| Nested functions | `WHERE COALESCE(nickname, name) = 'SOK'` | `source=FunctionExpr("COALESCE", [ColumnRef(nickname), ColumnRef(name)]), op="=", value=Literal("SOK")` |
| Inequality join condition | `AND orders.date >= users.last_login` | `source=ColumnRef(orders.date), op=">=", value=ColumnRef(users.last_login)` in `DbJoin.extraConditions` |
| CONCAT for fuzzy matching | `WHERE CONCAT(first_name, ' ', last_name) = 'SOK KIMLENG'` | `source=FunctionExpr("CONCAT", [ColumnRef(first_name), Literal(" "), ColumnRef(last_name)]), op="=", value=Literal("SOK KIMLENG")` |

### 7.8 Dry-Run Support

`EvaluationContext.isDryRun()` flag switches evaluators to validation mode:
- DB evaluators call `dbAccessor.dryRunQuery()` instead of executing
- `DbFieldAccessEvaluator` validates path + field against `SchemaRegistry`
- Returns fake typed values instead of real data
- Attaches `meta.generatedSql`, `meta.estimatedRowCount`, `meta.availableColumns` attributes

## Builtin Evaluators

| Function ID | Evaluator | Args | Returns |
|---|---|---|---|
| `add` / `subtract` / `multiply` / `divide` | `ArithmeticEvaluator` | `left`, `right` (positional NUMBER) | Number |
| `and` / `or` | `BooleanLogicEvaluator` | positional BOOLEAN... | Boolean |
| `not` | `NotEvaluator` | positional BOOLEAN | Boolean |
| `eq` | `EqualEvaluator` | `left`, `right` (positional ANY) | Boolean |
| `gt` / `gte` / `lt` / `lte` | `ComparisonEvaluator` | `left`, `right` (positional NUMBER) | Boolean |
| `payload` | `PayloadEvaluator` | `path` (named List<String>) | Object |
| `isEmpty` | `IsEmptyEvaluator` | positional ANY | Boolean |
| `stringContains` | `StringContainsEvaluator` | `input` (positional STRING); `substring` (named STRING) | Boolean |
| `stringStartsWith` | `StringStartsWithEvaluator` | `input` (positional STRING); `prefix` (named STRING) | Boolean |
| `stringEndsWith` | `StringEndsWithEvaluator` | `input` (positional STRING); `suffix` (named STRING) | Boolean |
| `stringFuzzyMatch` | `StringFuzzyMatchEvaluator` | `input` (positional STRING); `pattern` (named STRING); `threshold` (named FLOAT) | Boolean |
| `stringSimilarity` | `StringSimilarityEvaluator` | `input` (positional STRING); `other` (named STRING); `algorithm` (named STRING) | Float |
| `stringMatch` | `StringMatchEvaluator` | `input` (positional STRING); `regex` (named STRING) | Boolean |
| `stringLength` | `StringLengthEvaluator` | `input` (positional STRING) | Integer |
| `stringTrim` | `StringTrimEvaluator` | `input` (positional STRING) | String |
| `stringSubstring` | `StringSubstringEvaluator` | `input` (positional STRING); `start`, `end` (named INTEGER) | String |
| `stringReplace` | `StringReplaceEvaluator` | `input` (positional STRING); `target`, `replacement` (named STRING); `regex` (named BOOLEAN) | String |
| `list` | `ListEvaluator` | positional ANY (optional, single) | List<Object> |
| `now` | `DateEvaluators` (lambda) | (no args) | Instant |
| `datePlus` | `DateEvaluators` (lambda) | `date` (named Instant), `amount` (named NUMBER), `unit` (named STRING) | Instant |
| `dateMinus` | `DateEvaluators` (lambda) | `date` (named Instant), `amount` (named NUMBER), `unit` (named STRING) | Instant |
| `dateDiff` | `DateEvaluators` (lambda) | `date1`, `date2` (named Instant), `unit` (named STRING) | Long |
| `dateFormat` | `DateEvaluators` (lambda) | `date` (named Instant), `pattern` (named STRING) | String |
| `dateParse` | `DateEvaluators` (lambda) | `input` (named STRING), `pattern` (named STRING) | Instant |
| `extractYear` / `extractMonth` / `extractDay` / `extractHour` / `extractMinute` / `extractSecond` | `DateEvaluators` (lambdas) | `date` (named Instant) | Integer |
| `coalesce` | `CoalesceEvaluator` | positional ANY... (null-coalescing) | Object |
| `defaultIfNull` | `DefaultIfNullEvaluator` | `value` (positional ANY), `default` (named ANY) | Object |

## Usage Example — End-to-End

```java
// 1. Registry setup
FunctionRegistry registry = FunctionRegistry.withBuiltins(NullStrategies.PROPAGATE);

DbAccessor dbAccessor = new JooqDbAccessor(dslContext, schemaReg);
SchemaRegistry schemaReg = dbAccessor.getSchemaRegistry();

registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.DB_ACCESS)
    .evaluator(new DbAccessEvaluator(dbAccessor))
    .namedParam(ParamSpec.required("tableName", ParamType.STRING))
    .namedParam(ParamSpec.optional("columns", ParamType.ANY))
    .namedParam(ParamSpec.optional("filters", ParamType.ANY))
    .namedParam(ParamSpec.optional("limit", ParamType.INTEGER))
    .namedParam(ParamSpec.optional("offset", ParamType.INTEGER))
    .build());

registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.DB_AGGREGATOR)
    .evaluator(new DbAggregatorEvaluator(dbAccessor, schemaReg))
    .namedParam(ParamSpec.required("tableName", ParamType.STRING))
    .namedParam(ParamSpec.required("fieldName", ParamType.STRING))
    .namedParam(ParamSpec.required("aggregator", ParamType.STRING))
    .namedParam(ParamSpec.optional("filters", ParamType.ANY))
    .namedParam(ParamSpec.optional("groupBy", ParamType.ANY))
    .build());

registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.DB_FIELD_ACCESS)
    .evaluator(new DbFieldAccessEvaluator(dbAccessor, schemaReg))
    .namedParam(ParamSpec.required("tableName", ParamType.STRING))
    .namedParam(ParamSpec.required("fieldName", ParamType.STRING))
    .namedParam(ParamSpec.optional("path", ParamType.ANY))
    .build());

// 2. Dry-run validation
EvaluationContext dryCtx = new EvaluationContext.Builder(registry)
    .body(inputData)
    .dryRun(true)
    .build();
EvaluationResult dryResult = new AstEvaluator().evaluate(ruleAst, dryCtx);

// 3. Production evaluation
EvaluationContext ctx = new EvaluationContext.Builder(registry)
    .body(inputPayload)
    .build();
EvaluationResult result = new AstEvaluator().evaluate(ruleAst, ctx);
```

## Design Decisions

1. **Single flexible `Node` class** (not sealed hierarchy) — simpler construction, easier serialization, Marble pattern
2. **`from` is single `DbTable`** — multiple tables expressed via `List<DbJoin>` instead
3. **`DbFilter.source` is `FilterValue`** (not `DbColumn`) — both filter sides can be expressions
4. **`FilterValue.FunctionExpr`** supports arbitrary SQL functions with positional args and qualifier map
5. **`DbOrder` with `SortDirection`** — explicit record rather than bare `DbColumn`
6. **Join conditions** — equality column pair + `List<DbFilter> extraConditions` for compound/inequality
7. **Evaluators use string named args** — `tableName`, `fieldName`, `aggregator` not positional `DbTable` objects
8. **`DbAccessor` receives focal object** — `executeQuery(DbQuery, Object)` passes the payload for filter resolution
9. **Per-evaluator null strategy override** — `FunctionDefinition.nullStrategy()` with registry fallback
10. **jOOQ imperative `selectQuery()` API** — avoids complex type chains with Java 17 generics
11. **PostgreSQL-specific via jOOQ dialect** — `DSLContext` injected by consumer; `SqlRenderStyle` controls parameter rendering
