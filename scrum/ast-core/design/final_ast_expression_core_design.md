# AST Expression Core — Final Design Proposal

## Scope

This document specifies the **final** Java API for a reusable AST expression library under package `io.github.khezyapp.ast.core`. It supersedes the first-draft design in `ast_expression_core_design.md` (package `io.github.khezyapp.core.v1`) and incorporates all lessons from the index detection proof-of-concept.

**Goal**: A production-ready core that any project (AML, risk-rating, business rules, feature flags) can consume. The core provides Node, Evaluator, Registry, Context, Error handling, and a rich set of builtin evaluators. Consumers extend functions, error codes, and null-handling strategy.

**Key additions over draft**:
- Concrete guidelines for `attributes`, `trace`, and error codes
- Per-evaluator null strategy override
- String evaluator family (startWith, endWith, fuzzy, similarity, match, length, trim, substring, replace)
- SQL data-access evaluator family (dbAccess, dbAggregator) with pluggable dialect interface and default jOOQ implementation
- Dry-run mode for validation without side effects

---

## 1. Package Structure

The core package is organized into logical sub-packages so each group of related classes is easy to find.

```
io.github.khezyapp.ast.core/
├── model/                    # Core data types — AST tree, function identity, parameters
│   ├── Node.java             #   AST node (immutable)
│   ├── FunctionId.java       #   Sealed identity: Core / Named
│   ├── CoreFunctions.java    #   Built-in function ID constants
│   ├── ParamSpec.java        #   Parameter spec (name, type, required, default)
│   ├── ParamType.java        #   Parameter type enum
│   └── Arguments.java        #   Resolved positional + named values
│
├── function/                 # Function system — definitions, metadata, registry
│   ├── FunctionDefinition.java   # Function + evaluator + params + metadata + nullOverride
│   ├── FunctionAttributes.java   # Per-function metadata (short-circuit, cost)
│   └── FunctionRegistry.java     # Thread-safe registry + freeze
│
├── eval/                     # Evaluation engine
│   ├── Evaluator.java        #   @FunctionalInterface
│   ├── AstEvaluator.java     #   Recursive evaluation loop
│   ├── EvaluationContext.java #   Runtime context (message, cache, flags, dryRun)
│   └── EvaluationCache.java  #   Thread-safe subtree cache
│
├── result/                   # Evaluation result types — outcomes, traces, summaries
│   ├── EvaluationOutcome.java    # Evaluator return (value + errors + attributes)
│   ├── EvaluationResult.java     # Result tree (value + trace + attributes)
│   ├── EvaluationTrace.java      # Skipped / cached / duration metadata
│   ├── EvaluationSummary.java    # Aggregate statistics
│   └── FunctionStats.java        # Per-function statistics
│
├── error/                    # Error handling
│   ├── ErrorCode.java        #   Extensible error code (sealed: Standard / Custom)
│   ├── StandardErrors.java   #   Built-in error codes
│   └── EvaluationError.java  #   Structured error (code + message + source)
│
├── message/                  # Payload container
│   └── Message.java          #   Headers + body envelope
│
├── nullstrategy/             # Null handling
│   ├── NullHandlingStrategy.java  # Pluggable null behavior
│   └── NullStrategies.java        # Built-in null strategies (PROPAGATE, COERCE, FAIL)
│
├── builtin/                  # Builtin evaluator implementations
│   ├── string/               # String evaluator sub-package
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
│   ├── DateEvaluators.java         # Now, datePlus, dateMinus, dateDiff, dateFormat, dateParse, extract*
│   ├── DefaultIfNullEvaluator.java
│   ├── EqualEvaluator.java
│   ├── IsEmptyEvaluator.java
│   ├── ListEvaluator.java
│   ├── NotEvaluator.java
│   └── PayloadEvaluator.java
│
└── sql/                      # SQL data-access evaluators
    ├── model/
    │   ├── DbTable.java            # Table reference (name, schema, alias)
    │   ├── DbColumn.java           # Column reference (table + name + alias)
    │   ├── DbFilter.java           # Filter condition (source + op + value)
    │   ├── DbAggregation.java      # Aggregation spec (function + column + alias)
    │   ├── DbJoin.java             # Join clause (type, table, ON condition)
    │   ├── JoinType.java           # INNER / LEFT / RIGHT / CROSS
    │   ├── DbOrder.java            # ORDER BY spec (column + direction)
    │   ├── SortDirection.java      # ASC / DESC
    │   ├── DbQuery.java            # Full query model (from, columns, filters, aggregations, order, joins, limit, offset)
    │   ├── FilterValue.java        # Sealed: Literal | ColumnRef | Subquery | FunctionExpr
    │   ├── TableMetadata.java      # Known table schema — fields + links
    │   ├── FieldMetadata.java      # Column definition (name, type, nullable)
    │   ├── LinkMetadata.java       # FK relationship definition
    │   ├── SchemaRegistry.java     # Registry of known tables for validation
    │   └── DataType.java           # STRING / INTEGER / FLOAT / BOOLEAN / TIMESTAMP
    ├── SqlDialect.java             # Interface: translate DbQuery → SQL string
    ├── SqlRenderStyle.java         # INDEXED / NAMED / INLINED
    ├── DbAccessor.java             # Interface: execute query + field access
    ├── DbAccessEvaluator.java      # Evaluator for dbAccess (query-based)
    ├── DbAggregatorEvaluator.java  # Evaluator for dbAggregator
    ├── DbFieldAccessEvaluator.java # Evaluator for dbFieldAccess (path-based)
    ├── FilterBuilderEvaluator.java # Evaluator for buildFilter (construct DbFilter)
    └── dialect/
        ├── JooqSqlDialect.java     # jOOQ-based SQL generation
        └── JooqDbAccessor.java     # Default jOOQ DbAccessor implementation
```

**Navigation guide**:

| When you need... | Look in... | Key classes |
|-----------------|-----------|-------------|
| Build an AST tree | `model/` | `Node`, `FunctionId`, `CoreFunctions`, `ParamSpec` |
| Define/register a function | `function/` | `FunctionDefinition`, `FunctionAttributes`, `FunctionRegistry` |
| Run evaluation | `eval/` | `Evaluator`, `AstEvaluator`, `EvaluationContext` |
| Read results / evidence | `result/` | `EvaluationResult`, `EvaluationOutcome`, `EvaluationSummary` |
| Handle errors | `error/` | `ErrorCode`, `StandardErrors`, `EvaluationError` |
| Create input payload | `message/` | `Message` |
| Configure null behavior | `nullstrategy/` | `NullHandlingStrategy`, `NullStrategies` |
| Use builtin logic | `builtin/` | Arithmetic, BooleanLogic, StringStartWith, List, etc. |
| Query a database | `sql/` | `DbAccessor`, `SqlDialect`, `DbQuery`, `DbFieldAccessEvaluator`, `FilterBuilderEvaluator` |
| Construct filter conditions | `sql/` | `FilterBuilderEvaluator`, `FilterValue`, `DbFilter` |
| Validate schema before deployment | `sql/model/` | `SchemaRegistry`, `TableMetadata`, `FieldMetadata`, `LinkMetadata` |

---

## 2. Guidelines

### 2.1. When and How to Populate `attributes` of `EvaluationOutcome`

`EvaluationOutcome.attributes` is the **extension slot** for attaching metadata to a single evaluation step. The core propagates it into `EvaluationResult.attributes` automatically (see §2.4).

**When to populate**:

| Scenario | Example | Attribute keys |
|----------|---------|---------------|
| Compliance evidence | DB accessor attaching source records | `evidence.records`, `evidence.recordCount` |
| Score breakdown | Scoring evaluator showing sub-scores | `evidence.scoreBreakdown` |
| Filter diagnostics | Filter evaluator showing what was excluded | `evidence.filteredOut` |
| Input snapshots | Snapshot of the input data used | `evidence.inputSnapshot` |
| Performance hints | Row counts, query duration | `meta.rowsScanned`, `meta.queryDurationMs` |
| Debug info | Intermediate values for debugging | `debug.intermediateValues` |
| Audit identity | Which rule/definition produced this | `audit.ruleId`, `audit.ruleVersion` |

**How to populate** — convention for attribute keys:

```
<domain>.<category>[.<subcategory>]
```

- `evidence.*` — Compliance evidence (source records, score breakdowns)
- `audit.*` — Audit trail (rule ID, version, trace ID)
- `meta.*` — Performance/cardinality metadata
- `debug.*` — Debug/diagnostic information

Rule: **Keep evidence maps small by reference**. If an aggregator produces 10 000 source records, do not embed them in attributes. Instead, write them to an external store and put a reference:

```java
return EvaluationOutcome.success(sum, Map.of(
    "evidence.recordCount", 10_000,
    "evidence.recordsRef",  "/evidence/batch-123.json"
));
```

**When NOT to populate**:
- The evaluator is a pure function (add, equals, not) — return `Map.of()`
- The information is already available in the result tree (children values)

### 2.2. Error Code Consistency

Consumers define new error codes without modifying the core:

```java
// Standard errors — always use these for common situations
EvaluationError.of(StandardErrors.WRONG_ARG_COUNT, "Expected 2 args, got 1", "positional");
EvaluationError.of(StandardErrors.DIVISION_BY_ZERO, "Denominator is zero", "arg[1]");
EvaluationError.of(StandardErrors.NULL_NOT_ALLOWED, "Field name must not be null", "named:fieldName");

// Custom errors — for domain-specific situations
ErrorCode THRESHOLD_EXCEEDED = ErrorCode.of("THRESHOLD_EXCEEDED", "Score exceeds allowed threshold");
EvaluationError.of(THRESHOLD_EXCEEDED, "AML score 950 exceeds threshold 500");
```

**Guidelines**:
1. **Always use `StandardErrors` constants** for validation, type mismatch, missing args, null violations, division by zero, function-not-found, and runtime errors.
2. **Define custom `ErrorCode`** as a `static final` field in your evaluator class.
3. **Error code format**: `UPPER_SNAKE_CASE`, 3–40 characters, unique per system.
4. **Comparison**: Use `error.errorCode().code().equals("MY_CODE")` — never compare by enum, never match on message text.
5. **Source field** pinpoints the exact argument: `"arg[0]"`, `"named:fieldName"`, `"positional"`.
6. **Never throw** from an evaluator — always return `EvaluationOutcome.failure(...)`. The engine catches unexpected exceptions and wraps them as `StandardErrors.RUNTIME_ERROR`.

### 2.3. How `trace` Works and How It Is Populated in `EvaluationResult`

`EvaluationTrace` is a record set **exclusively by the engine**, never by evaluators:

```java
public record EvaluationTrace(boolean skipped, boolean cached, long durationNanos) {
    public static final EvaluationTrace EVALUATED = new EvaluationTrace(false, false, 0);
    public static final EvaluationTrace SKIPPED   = new EvaluationTrace(true, false, 0);
}
```

**Population rules** (enforced in `AstEvaluator.evaluate()`):

| Condition | `skipped` | `cached` | `durationNanos` | Set by |
|-----------|-----------|----------|-----------------|--------|
| Normal evaluation | `false` | `false` | `System.nanoTime() - start` | Engine (Phase 8) |
| Cache hit | `false` | `true` | `0` | Engine (cache check) |
| Short-circuit skip | `true` | `false` | `0` | Engine (via `EvaluationResult.skipped()`) |
| Short-circuit return | `false` | `false` | measured | Engine (short-circuit branch) |

**Consumer usage**:
```java
EvaluationResult result = evaluator.evaluate(ast, ctx);

// Performance analysis
EvaluationSummary summary = EvaluationSummary.from(result);
System.out.println("Total: " + summary.totalDurationNanos() + "ns");
System.out.println("Cached: " + summary.cachedNodes() + "/" + summary.totalNodes());

// Check if a specific node was actually computed or skipped
for (EvaluationResult node : result.flatten()) {
    if (node.trace().skipped()) { /* short-circuited */ }
    if (node.trace().cached())  { /* from cache */ }
}
```

### 2.4. How `attributes` in `EvaluationResult` Can Be Used (Evidence and UI)

The engine propagates `EvaluationOutcome.attributes` directly into `EvaluationResult.attributes` — there is no transformation. This gives consumers two primary usage patterns:

#### Pattern 1: Compliance Audit

Walk the result tree and collect evidence for audit trails:

```java
EvaluationResult result = evaluator.evaluate(ast, ctx);
ComplianceReport report = new ComplianceReport();

for (EvaluationResult node : result.flatten()) {
    if (node.hasAttribute("evidence.records")) {
        @SuppressWarnings("unchecked")
        List<EvidenceRecord> records = (List<EvidenceRecord>) node.getAttribute("evidence.records");
        report.addRecords(node.function().value(), records);
    }
    if (node.hasAttribute("evidence.scoreBreakdown")) {
        ScoreBreakdown breakdown = (ScoreBreakdown) node.getAttribute("evidence.scoreBreakdown");
        report.addBreakdown(node.function().value(), breakdown);
    }
}
```

#### Pattern 2: UI Rendering

A frontend can render the result tree as nested cards/panels. Attributes provide the content:

```json
{
  "function": "and",
  "value": true,
  "children": [
    {
      "function": "dbAggregator",
      "value": 1250000.50,
      "attributes": {
        "meta.rowsScanned": 5000,
        "evidence.recordsRef": "/evidence/query-abc.json"
      }
    },
    {
      "function": "stringStartsWith",
      "value": true,
      "attributes": {
        "debug.inputValue": "URGENT_FLAG",
        "debug.pattern": "URGENT"
      }
    }
  ]
}
```

The UI can:
- Show a **drill-down** for any node that has `evidence` attributes
- Display **performance badges** (rows scanned, duration) for nodes with `meta.*` attributes
- Hide `debug.*` attributes by default, show in developer mode
- Link to external evidence stores via `evidence.*Ref` keys

#### Attribute Propagation

Attributes **do NOT** automatically merge upward. Each node carries only the attributes its evaluator attached. To aggregate evidence, consumers walk the tree:

```java
// Collect all evidence.records from all descendants
List<EvidenceRecord> allRecords = result.flatten().stream()
    .filter(n -> n.hasAttribute("evidence.records"))
    .flatMap(n -> ((List<EvidenceRecord>) n.getAttribute("evidence.records")).stream())
    .toList();
```

### 2.5. Node Structure Guideline — Constants vs Functions

The `Node` type has two shapes: constant leaf nodes and function call nodes. Correct usage is critical because the engine handles each shape differently.

**Constant leaf node** — wraps a literal value as a child of the AST:
```java
Node idNode = Node.constant("transactions");      // constant string value
Node amountNode = Node.constant(100);              // constant number
Node flagNode = Node.constant(true);               // constant boolean
Node pathNode = Node.constant(List.of("account", "customer"));  // constant list
```

Only `Node.constant()` produces a constant node. The engine checks `node.isConstant()` which tests whether the internal function is `CoreFunctions.CONSTANT`. The value is then read via `node.constant()`.

**Function call node** — invokes an evaluator:
```java
Node eqNode = Node.function(CoreFunctions.EQUAL, leftChild, rightChild);
Node andNode = Node.function(CoreFunctions.AND, cond1, cond2);
Node dbAccess = Node.function(CoreFunctions.DB_FIELD_ACCESS,
    List.of(),
    Map.of(
        "tableName", Node.constant("transactions"),
        "fieldName", Node.constant("account_type"),
        "path",      Node.constant(List.of("account"))
    )
);
```

**Rules for evaluator implementations**:
1. All named arguments that are metadata (table names, field names, config flags) must be passed as `Node.constant(...)` — the evaluator extracts them via `args.named().get("key")` which returns the resolved value directly.
2. All positional arguments that are sub-expressions (operands to `add`, conditions to `and`) must be child `Node` trees — the engine evaluates them recursively.
3. Never wrap a `Node` as a constant of another `Node`.
4. An evaluator that receives a `Node.constant(...)` argument accesses the value directly from the `Arguments` object — it does **not** call `evaluate()` on it again.

**Convention for dbAccessors**: Named arguments `tableName`, `fieldName`, `path` are always constants. Filters and aggregations may be constants (static config) or sub-expressions (dynamic config built from list evaluators or payload access).

---

## 3. Feature: Per-Evaluator NullHandlingStrategy Override

### Motivation

The current design applies a single `NullHandlingStrategy` globally (from `FunctionRegistry`). However, different functions within the same evaluation often need different null behavior:

| Function | Desired Behavior | Reason |
|----------|-----------------|--------|
| `add`, `subtract` | FAIL | Null arithmetic is always a bug |
| `eq` | PROPAGATE | `null == null` should evaluate to true |
| `stringContains` | COERCE_DEFAULT | `null.contains("x")` should return false, not throw |
| `dbAccess` | PROPAGATE | A null filter value means "no filter" |
| `and`, `or` | COERCE_DEFAULT | Treat null as false |

### Design

Add an optional `NullHandlingStrategy` field to `FunctionDefinition`. The engine checks the per-function strategy first, falling back to the registry-level strategy.

```java
public final class FunctionDefinition {
    private final FunctionId functionId;
    private final Evaluator evaluator;
    private final List<ParamSpec> positionalParams;
    private final Map<String, ParamSpec> namedParams;
    private final FunctionAttributes attributes;
    private final @Nullable NullHandlingStrategy nullStrategy;  // NEW — per-function override

    // ...

    public @Nullable NullHandlingStrategy nullStrategy() { return nullStrategy; }

    public static final class Builder {
        // ...
        private @Nullable NullHandlingStrategy nullStrategy;

        public Builder nullStrategy(NullHandlingStrategy strategy) {
            this.nullStrategy = strategy;
            return this;
        }
        // ...
    }
}
```

**Engine change** (in `AstEvaluator`):

```java
// Phase 5: apply null strategy — per-function first, then registry fallback
NullHandlingStrategy effectiveStrategy = def.nullStrategy() != null
    ? def.nullStrategy()
    : ctx.registry().nullHandlingStrategy();
var resolvedArgs = applyNullStrategy(rawArgs, def, effectiveStrategy);
```

**Example registration with override**:

```java
registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.ADD)
    .evaluator(new ArithmeticEvaluator(CoreFunctions.ADD))
    .positionalParam(ParamSpec.required("left", ParamType.INTEGER))
    .positionalParam(ParamSpec.required("right", ParamType.INTEGER))
    .nullStrategy(NullStrategies.FAIL)   // arithmetic must never receive null
    .build());

registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.STRING_CONTAINS)
    .evaluator(new StringContainsEvaluator())
    .positionalParam(ParamSpec.required("string", ParamType.STRING))
    .positionalParam(ParamSpec.required("substring", ParamType.STRING))
    .nullStrategy(NullStrategies.COERCE_DEFAULT)  // null → "" → safe contains
    .build());
```

---

## 4. Feature: Builtin String Evaluators

### 4.1. New Core Function IDs

```java
// Existing
public static final FunctionId STRING_CONTAINS = core("stringContains");

// NEW — String evaluators
public static final FunctionId STRING_STARTS_WITH      = core("stringStartsWith");
public static final FunctionId STRING_ENDS_WITH        = core("stringEndsWith");
public static final FunctionId STRING_FUZZY_MATCH      = core("stringFuzzyMatch");
public static final FunctionId STRING_SIMILARITY       = core("stringSimilarity");
public static final FunctionId STRING_MATCH            = core("stringMatch");       // regex
public static final FunctionId STRING_LENGTH           = core("stringLength");
public static final FunctionId STRING_TRIM             = core("stringTrim");
public static final FunctionId STRING_SUBSTRING        = core("stringSubstring");
public static final FunctionId STRING_REPLACE          = core("stringReplace");
```

### 4.2. Evaluator Specifications

| Evaluator | Positional Args | Named Args | Returns | Notes |
|-----------|----------------|------------|---------|-------|
| `StringStartsWithEvaluator` | `input` (STRING), `prefix` (STRING) | `caseSensitive` (BOOLEAN, default `true`) | `Boolean` | `caseSensitive=false` → `input.lower().startsWith(prefix.lower())` |
| `StringEndsWithEvaluator` | `input` (STRING), `suffix` (STRING) | `caseSensitive` (BOOLEAN, default `true`) | `Boolean` | Same case-sensitivity pattern |
| `StringFuzzyMatchEvaluator` | `input` (STRING), `pattern` (STRING) | `caseSensitive` (BOOLEAN, default `true`), `threshold` (FLOAT, default `0.8`) | `Boolean` | Levenshtein-based; returns true if similarity ≥ threshold |
| `StringSimilarityEvaluator` | `input` (STRING), `other` (STRING) | `algorithm` (STRING, default `"levenshtein"`), `caseSensitive` (BOOLEAN, default `true`) | `FLOAT` (0.0–1.0) | Returns raw similarity score. Algorithms: `levenshtein`, `jaroWinkler`, `cosine` |
| `StringMatchEvaluator` | `input` (STRING), `regex` (STRING) | `caseSensitive` (BOOLEAN, default `true`) | `Boolean` | `Pattern.matches(regex, input)` |
| `StringLengthEvaluator` | `input` (STRING) | — | `Integer` | `input.length()` |
| `StringTrimEvaluator` | `input` (STRING) | — | `STRING` | `input.trim()` |
| `StringSubstringEvaluator` | `input` (STRING) | `start` (INTEGER, required), `end` (INTEGER, optional) | `STRING` | `input.substring(start, end)` |
| `StringReplaceEvaluator` | `input` (STRING) | `target` (STRING, required), `replacement` (STRING, required), `regex` (BOOLEAN, default `false`) | `STRING` | `input.replace(target, replacement)` or `input.replaceAll(target, replacement)` |

### 4.3. Case-Sensitivity Pattern

All string evaluators that support case-sensitivity follow the same pattern:

```java
// StringStartsWithEvaluator example
public class StringStartsWithEvaluator implements Evaluator {
    @Override
    public EvaluationOutcome evaluate(EvaluationContext ctx, Arguments args) {
        String input = (String) args.positional().get(0);
        String prefix = (String) args.positional().get(1);
        boolean caseSensitive = (boolean) args.named().getOrDefault("caseSensitive", true);

        if (!caseSensitive) {
            input = input.toLowerCase();
            prefix = prefix.toLowerCase();
        }

        return EvaluationOutcome.success(input.startsWith(prefix));
    }
}
```

### 4.4. Rationale for Additional Proposals

| Evaluator | Use Case |
|-----------|----------|
| `stringMatch` (regex) | Pattern validation — email format, account number format, SWIFT/BIC validation |
| `stringLength` | Input validation — password min length, description max length |
| `stringTrim` | Sanitization — clean user input before comparison |
| `stringSubstring` | Extract portions — first 4 chars of card number, date parts |
| `stringReplace` | Redaction — mask sensitive data, normalize formats |

---

## 5. Feature: Builtin List Evaluator

### 5.1. Motivation

Aggregator and query evaluators often need dynamic filter lists constructed from sub-expressions rather than static constants. A general-purpose `list` evaluator collects positional child results into an ordered `List<Object>`.

**Without list evaluator** — filters must be a pre-built constant:
```java
// Only static lists possible
"filters", Node.constant(List.of(new DbFilter(...)))
```

**With list evaluator** — filters can be dynamic, mixing constants and computed values:
```java
Node.function(CoreFunctions.LIST,
    payloadFilterNode,     // computed by another evaluator
    constantFilterNode     // static filter
)
```

### 5.2. Evaluator Specification

| Function ID | Core constant | Positional args | Returns |
|-------------|--------------|-----------------|---------|
| `"list"` | `CoreFunctions.LIST` | Any number of `ANY` values | `List<Object>` |

**Evaluator behavior**:
1. Collects the return value of each positional child into a `List<Object>`.
2. Preserves order — the result list maintains child evaluation order.
3. Empty list if no positional children (`List.of()`).
4. No attributes attached (pure construction).

### 5.3. Usage Example

```java
// Build individual filters using the buildFilter evaluator
Node statusFilter = Node.function(CoreFunctions.BUILD_FILTER, List.of(), Map.of(
    "column",    Node.constant(DbColumn.of(DbTable.of("transactions"), "status")),
    "operator",  Node.constant("="),
    "value",     Node.constant("COMPLETED"),
    "valueType", Node.constant("literal")
));

Node dateFilter = Node.function(CoreFunctions.BUILD_FILTER, List.of(), Map.of(
    "column",    Node.constant(DbColumn.of(DbTable.of("transactions"), "created_at")),
    "operator",  Node.constant(">="),
    "value",     Node.constant("2024-01-01"),
    "valueType", Node.constant("literal")
));

// Combine into a dynamic filter list using list evaluator
Node dynamicFilters = Node.function(CoreFunctions.LIST,
    statusFilter,
    dateFilter
);

// dbAggregator(transactions, SUM(amount), filters: dynamicFilters)
Node agg = Node.function(CoreFunctions.DB_AGGREGATOR,
    Node.constant(DbTable.of("transactions")),
    List.of(),
    Map.of(
        "aggregation", Node.constant(new DbAggregation("SUM",
            DbColumn.of(DbTable.of("transactions"), "amount"), "total", false)),
        "filters", dynamicFilters   // ← dynamically constructed filter list
    )
);
```

### 5.4. Core Function ID

```java
public static final FunctionId LIST = core("list");
```

---

## 6. Feature: SQL Data-Access Evaluators

### 6.1. Table Metadata Model (in `io.github.khezyapp.ast.core.sql.model`)

To validate table names, field names, and link paths before deployment, the system needs a model representing available database schema:

```java
// TableMetadata — describes a known table in the system
public record TableMetadata(
    String name,
    @Nullable String schema,
    Map<String, FieldMetadata> fields,
    Map<String, LinkMetadata> links
)

// FieldMetadata — describes a column in a table
public record FieldMetadata(
    String name,
    String dataType,       // "STRING", "INTEGER", "BOOLEAN", "FLOAT", "TIMESTAMP", "IP_ADDRESS"
    boolean nullable,
    boolean isPrimaryKey,
    boolean isForeignKey
)

// LinkMetadata — describes a FK relationship (child → parent)
public record LinkMetadata(
    String name,                // Link name, used in AST path arguments
    String parentTableName,     // The table on the "one" side
    String parentFieldName,     // Column in parent table (PK typically)
    String childTableName,      // The table on the "many" side
    String childFieldName       // Column in child table (FK)
)

// SchemaRegistry — holds all available table metadata for validation
public final class SchemaRegistry {
    private final Map<String, TableMetadata> tables;  // keyed by table name

    public SchemaRegistry(List<TableMetadata> tables) { /* ... */ }

    public @Nullable TableMetadata getTable(String name) { /* ... */ }
    public boolean hasTable(String name) { /* ... */ }
    public @Nullable FieldMetadata getField(String tableName, String fieldName) { /* ... */ }
    public @Nullable LinkMetadata getLink(String tableName, String linkName) { /* ... */ }

    /** Validate a join path: check each link name exists and chains correctly. */
    public List<EvaluationError> validatePath(String startTable, List<String> path) { /* ... */ }

    /** Validate a field exists on a given table. */
    public @Nullable EvaluationError validateField(String tableName, String fieldName) { /* ... */ }
}
```

### 6.2. Query Data Model (in `io.github.khezyapp.ast.core.sql.model`)

```java
// DbTable — represents a table or view in the database
public record DbTable(
    String name,
    @Nullable String schema,
    @Nullable String alias
) {
    public static DbTable of(String name) { return new DbTable(name, null, null); }
    public String qualifiedName() {
        return schema != null ? schema + "." + name : name;
    }
}

// DbColumn — a column reference
public record DbColumn(
    DbTable table,
    String name,
    @Nullable String alias
) {
    public static DbColumn of(DbTable table, String name) { return new DbColumn(table, name, null); }
}

// FilterValue — the value side of a filter condition, supporting expressions
public sealed interface FilterValue permits FilterValue.Literal, FilterValue.ColumnRef, FilterValue.Subquery, FilterValue.FunctionExpr {

    /** A literal value (string, number, boolean, null). */
    record Literal(@Nullable Object value) implements FilterValue {}

    /** A reference to another column in the same or joined table. */
    record ColumnRef(DbColumn column) implements FilterValue {}

    /** A subquery result. */
    record Subquery(DbQuery query) implements FilterValue {}
}

// DbFilter — a WHERE clause condition with expression-aware value
public record DbFilter(
    FilterValue source,
    String operator,    // "=", "<", ">", "<=", ">=", "!=", "IN", "LIKE", "IS_NULL", "IS_NOT_NULL"
    FilterValue value   // can be Literal, ColumnRef, Subquery, or FunctionExpr
)

// DbAggregation — an aggregation function application
public record DbAggregation(
    String function,    // "COUNT", "SUM", "AVG", "MIN", "MAX"
    DbColumn column,
    @Nullable String alias,
    boolean distinct
)

// DbQuery — the complete query model
public record DbQuery(
    DbTable from,
    List<DbColumn> columns,
    List<DbFilter> filters,
    List<DbColumn> groupBy,
    List<DbAggregation> aggregations,
    List<DbOrder> orderBy,
    List<DbJoin> joins,
    @Nullable Long limit,
    @Nullable Long offset
) {
    public static Builder builder() { return new Builder(); }
}

### 6.3. SQL Dialect Interface

Translates `DbQuery` into database-specific SQL. Consumers implement this for their target database technology.

```java
public interface SqlDialect {
    /**
     * Translate a DbQuery into executable SQL string.
     * @param query the logical query model
     * @return SQL string with ? placeholders for bind values
     */
    String toSql(DbQuery query);

    /**
     * Translate a DbQuery into SQL with a specific parameter rendering style.
     */
    String toSql(DbQuery query, SqlRenderStyle style);

    /**
     * Return the bind values in order matching the ? placeholders.
     */
    List<Object> extractBindValues(DbQuery query);
}

public enum SqlRenderStyle { INDEXED, NAMED, INLINED }
```

### 6.4. Database Accessor Interface

Abstracts database interaction. Consumers implement this with their chosen technology (JDBC, jOOQ, Hibernate, R2DBC, etc.).

```java
public interface DbAccessor {
    /**
     * Execute a query and return rows as a list of maps (columnName → value).
     * Used by dbAccess evaluator.
     */
    List<Map<String, Object>> executeQuery(DbQuery query, Object focalObject);

    /**
     * Execute an aggregation query and return a single row.
     * Used by dbAggregator evaluator.
     */
    Map<String, Object> executeAggregation(DbQuery query, Object focalObject);

    /**
     * Generate SQL string from a DbQuery (inlined params, no execution).
     */
    String generateSql(DbQuery query);

    /**
     * Dry-run: return schema metadata without executing.
     * Used when ctx isDryRun() returns true.
     */
    DryRunResult dryRunQuery(DbQuery query);

    /**
     * Return the SchemaRegistry used by this accessor for path/field validation.
     */
    SchemaRegistry getSchemaRegistry();
}

public record DryRunResult(
    List<String> availableColumns,
    long estimatedRowCount,
    String generatedSql
)
```

### 6.5. Evaluator Implementations

#### `DbAccessEvaluator` — function id `"dbAccess"`

Returns rows from a database table matching filters. All args are named (no positional).

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| named:tableName | STRING | yes | Target table |
| named:columns | LIST<STRING> | no | Columns to select (default: *) |
| named:filters | LIST<DbFilter> | no | WHERE conditions |
| named:limit | INTEGER | no | Row limit |
| named:offset | INTEGER | no | Offset |

**Returns**: `List<Map<String, Object>>` — matching rows.

**Attributes attached**:
- `meta.generatedSql` — the SQL that was executed
- `meta.estimatedRowCount` — estimated rows (dry-run only)

#### `DbAggregatorEvaluator` — function id `"dbAggregator"`

Computes an aggregate over matching rows. All args are named (no positional).

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| named:tableName | STRING | yes | Target table |
| named:fieldName | STRING | yes | Aggregation field |
| named:aggregator | STRING | yes | Aggregation function (COUNT, SUM, AVG, MAX, MIN, STDDEV, MEDIAN, PERCENTILE) |
| named:filters | LIST<DbFilter> | no | WHERE conditions |
| named:groupBy | LIST<STRING> | no | GROUP BY columns |

**Returns**: `Number` (scalar aggregate result).

**Attributes attached**:
- `meta.generatedSql` — the SQL that was executed
- `meta.estimatedRowCount` — estimated rows (dry-run only)

#### `DbFieldAccessEvaluator` — function id `"dbFieldAccess"`

Reads a single field value from a target table reachable via a **join path** (chain of FK relationships). This is the bridge between the trigger payload and related data — essential for AML rules that need to compare transaction fields against account or customer attributes.

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| named:tableName | STRING | yes | Trigger table name (the payload's table) |
| named:fieldName | STRING | yes | Target field to read from the destination table |
| named:path | LIST<STRING> | no | Join path — chain of link names (default empty; error if empty at runtime) |

**Path traversal algorithm**:
1. Start at the trigger table (from `tableName`).
2. For each link name in `path`:
   - Look up the link in the `SchemaRegistry` via `getLink()` (validated during dry-run).
   - Build a `DbJoin`: LEFT JOIN parent table ON parentFieldName = payload[childFieldName].
   - Move to the parent table defined by the link.
3. Read `fieldName` from the final table reached — SELECT last link's parentTableName.fieldName.

**Returns**: `Object` — the field value, or `null` if any FK in the chain is null.

**Null FK optimization**: If any FK value is `null`, the evaluator returns `null` immediately without any database query.

**Implementation detail**: The evaluator builds a `DbQuery` with FROM = first hop's parent table, JOIN chain for subsequent hops, WHERE = equality on parent PK = payload FK, SELECT = last table's target field. Then calls `dbAccessor.executeQuery(query, payload)` instead of a dedicated `executeFieldAccess` method. The result is attached as `meta.generatedSql` attribute.

**Attributes attached**:
- `meta.generatedSql` — the SQL that was executed (inlined params)

**Dry-run behavior**: When `ctx.isDryRun()` is `true`, the evaluator:
1. Walks the path in `SchemaRegistry` to validate each link exists and validates the target field exists on the final table.
2. Returns a fake typed value based on `FieldMetadata.dataType`:
   - `"STRING"` → `"fake:<tableName>.<fieldName>"`
   - `"INTEGER"` → `1`
   - `"BOOLEAN"` → `true`
   - `"FLOAT"` → `1.0`
   - `"TIMESTAMP"` → `Instant.now()`
3. Attaches `meta.generatedSql` and `meta.dryRun` attributes.

**Example AST**:
```java
// Read account_type from accounts table via the "account" link
Node fieldAccess = Node.function(CoreFunctions.DB_FIELD_ACCESS,
    List.of(),
    Map.of(
        "tableName", Node.constant("transactions"),
        "fieldName", Node.constant("account_type"),
        "path",      Node.constant(List.of("account"))
    )
);
```

#### `BuildFilterEvaluator` — function id `"buildFilter"`

Constructs a single `DbFilter` from evaluated sub-expressions. This enables dynamic filter construction where column, operator, and value can all be computed at evaluation time.

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| named:tableName | STRING | yes | The table name for the filter column |
| named:fieldName | STRING | yes | The field name for the filter column |
| named:operator | STRING | yes | Comparison operator (`"="`, `"<"`, `">"`, `"IN"`, `"LIKE"`, etc.) |
| named:value | ANY | yes | The filter value (literal, column ref, or subquery) |
| named:valueType | STRING | no | `"literal"`, `"columnRef"`, or `"subquery"` (default: `"literal"`) |

**Returns**: `DbFilter` — constructed from the evaluated arguments.

**Behavior**:
1. Extracts `tableName`, `fieldName`, `operator`, `value`, and `valueType` from named args.
2. Builds `DbColumn.of(DbTable.of(tableName), fieldName)` as the filter source.
3. Wraps `value` in the appropriate `FilterValue` variant based on `valueType`:
   - `"literal"` → `FilterValue.Literal(value)`
   - `"columnRef"` → `FilterValue.ColumnRef((DbColumn) value)`
   - `"subquery"` → `FilterValue.Subquery((DbQuery) value)`
4. Returns `DbFilter.of(column, operator, filterValue)`.

**Example — static filter**:
```java
Node.function(CoreFunctions.BUILD_FILTER,
    List.of(),
    Map.of(
        "tableName", Node.constant("transactions"),
        "fieldName", Node.constant("status"),
        "operator", Node.constant("="),
        "value",    Node.constant("COMPLETED"),
        "valueType", Node.constant("literal")
    )
)
// Returns: DbFilter(source=transactions.status, operator="=", value=Literal("COMPLETED"))
```

**Example — column reference filter** (compare two columns):
```java
Node.function(CoreFunctions.BUILD_FILTER,
    List.of(),
    Map.of(
        "tableName", Node.constant("transactions"),
        "fieldName", Node.constant("amount"),
        "operator", Node.constant(">"),
        "value",    Node.constant(DbColumn.of(DbTable.of("transactions"), "threshold")),
        "valueType", Node.constant("columnRef")
    )
)
// Returns: DbFilter(source=transactions.amount, operator=">", value=ColumnRef(transactions.threshold))
```

**Combined with list evaluator** for dynamic filter lists:
```java
Node dynamicFilters = Node.function(CoreFunctions.LIST,
    Node.function(CoreFunctions.BUILD_FILTER, List.of(), Map.of(
        "tableName", Node.constant("transactions"),
        "fieldName", Node.constant("status"),
        "operator", Node.constant("="),
        "value",    Node.constant("COMPLETED"),
        "valueType", Node.constant("literal")
    )),
    Node.function(CoreFunctions.BUILD_FILTER, List.of(), Map.of(
        "tableName", Node.constant("transactions"),
        "fieldName", Node.constant("created_at"),
        "operator", Node.constant(">="),
        "value",    Node.constant("2024-01-01"),
        "valueType", Node.constant("literal")
    ))
);
```

### 6.6. Default jOOQ Dialect Implementation
```java
public class JooqSqlDialect implements SqlDialect {
    private final DSLContext dsl;

    public JooqSqlDialect(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public String toSql(DbQuery query) {
        return createQuery(query).getSQL(ParamType.NAMED);
    }

    @Override
    public String toSql(DbQuery query, SqlRenderStyle style) {
        ParamType paramType = switch (style) {
            case INDEXED -> ParamType.INDEXED;
            case NAMED -> ParamType.NAMED;
            case INLINED -> ParamType.INLINED;
        };
        return createQuery(query).getSQL(paramType);
    }

    @Override
    public List<Object> extractBindValues(DbQuery query) {
        return createQuery(query).getBindValues();
    }

    private SelectQuery<?> createQuery(DbQuery query) {
        var select = dsl.selectQuery();
        // Build jOOQ query from DbQuery model
        // ...
        return select;
    }
}
```

A default `JooqDbAccessor` implementation that wraps a consumer-provided `DSLContext` + `JooqSqlDialect`:

```java
public class JooqDbAccessor implements DbAccessor {
    private final JooqSqlDialect dialect;
    private final DSLContext dsl;

    public JooqDbAccessor(DSLContext dsl, SchemaRegistry schemaRegistry) {
        this.dsl = dsl;
        this.dialect = new JooqSqlDialect(dsl);
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public List<Map<String, Object>> executeQuery(DbQuery query, Object focalObject) {
        var sql = dialect.toSql(query, SqlRenderStyle.INLINED);
        // Execute via dsl ...
    }

    @Override
    public String generateSql(DbQuery query) {
        return dialect.toSql(query, SqlRenderStyle.INLINED);
    }

    @Override
    public DryRunResult dryRunQuery(DbQuery query) {
        var sql = dialect.toSql(query, SqlRenderStyle.INLINED);
        return new DryRunResult(List.of(), 0, sql);
    }
}
```

### 6.7. AST Construction Example

```java
// dbAggregator(
//     tableName: "transactions",
//     fieldName: "amount",
//     aggregator: "SUM",
//     filters: [
//         {fieldName: "status", operator: "=", value: "COMPLETED"},
//         {fieldName: "created_at", operator: ">=", value: "2024-01-01"}
//     ]
// )
Node aggNode = Node.function(FunctionId.of("dbAggregator"),
    List.of(),
    Map.of(
        "tableName", Node.constant("transactions"),
        "fieldName", Node.constant("amount"),
        "aggregator", Node.constant("SUM"),
        "filters", Node.constant(List.of(
            new DbFilter(DbColumn.of(DbTable.of("transactions"), "status"), "=", new FilterValue.Literal("COMPLETED")),
            new DbFilter(DbColumn.of(DbTable.of("transactions"), "created_at"), ">=", new FilterValue.Literal("2024-01-01"))
        ))
    )
);
```

---

## 7. Feature: Dry-Run Validation

### Motivation

DB access and aggregation evaluators execute real queries against the database. During config validation, CI/CD pipelines, or UI preview, we need to validate the AST structure **without touching the database**.

### Design

Add `boolean dryRun` to `EvaluationContext`:

```java
public final class EvaluationContext {
    // ... existing fields ...
    private final boolean dryRun;  // NEW

    // ...
    public boolean isDryRun() { return dryRun; }

    public static final class Builder {
        // ...
        private boolean dryRun = false;

        public Builder dryRun(boolean v) { this.dryRun = v; return this; }
        // ...
    }
}
```

### Evaluator Contract for Dry-Run

Evaluators that perform side effects (DB access, network calls, file I/O) **must** check `ctx.isDryRun()`:

```java
public class DbAggregatorEvaluator implements Evaluator {
    private final DbAccessor dbAccessor;

    @Override
    public EvaluationOutcome evaluate(EvaluationContext ctx, Arguments args) {
        String tableName = (String) args.named().get("tableName");
        String fieldName = (String) args.named().get("fieldName");
        String aggregator = (String) args.named().get("aggregator");
        @SuppressWarnings("unchecked")
        List<DbFilter> filters = (List<DbFilter>) args.named().getOrDefault("filters", List.of());

        DbQuery query = buildQuery(tableName, fieldName, aggregator, filters);

        if (ctx.isDryRun()) {
            DryRunResult dry = dbAccessor.dryRunQuery(query);
            return EvaluationOutcome.success(
                0.0,  // placeholder value
                Map.of(
                    "meta.dryRun", true,
                    "meta.generatedSql", dry.generatedSql(),
                    "meta.estimatedRowCount", dry.estimatedRowCount(),
                    "meta.availableColumns", dry.availableColumns()
                )
            );
        }

        Map<String, Object> result = dbAccessor.executeAggregation(query, ctx.getBody());
        Number value = (Number) result.get(aggregator);
        return EvaluationOutcome.success(value, Map.of(
            "meta.generatedSql", dbAccessor.generateSql(query)
        ));
    }
}
```

### Usage in Validation Pipeline

```java
// Validate a config without side effects
EvaluationContext dryCtx = new EvaluationContext.Builder(registry)
    .body(inputData)
    .dryRun(true)
    .build();

EvaluationResult result = new AstEvaluator().evaluate(ast, dryCtx);

if (!result.flattenErrors().isEmpty()) {
    System.out.println("Config validation FAILED:");
    result.flattenErrors().forEach(e ->
        System.out.println("  [" + e.errorCode().code() + "] " + e.message()));
} else {
    System.out.println("Config is valid.");
    // Inspect generated SQL from dry-run attributes
    for (EvaluationResult node : result.flatten()) {
        if (node.hasAttribute("meta.generatedSql")) {
            System.out.println("SQL: " + node.getAttribute("meta.generatedSql"));
        }
    }
}
```

---

## 8. Updated Core API Changes from Draft

### 8.1. FunctionDefinition — added nullStrategy field

```java
// NEW
public @Nullable NullHandlingStrategy nullStrategy() { return nullStrategy; }

// Builder
public Builder nullStrategy(NullHandlingStrategy strategy) { ... }
```

### 8.2. EvaluationContext — added dryRun field

```java
// NEW
public boolean isDryRun() { return dryRun; }

// Builder
public Builder dryRun(boolean v) { ... }
```

### 8.3. AstEvaluator — null strategy resolution and dry-run propagation

```java
// Phase 5: per-function null strategy with registry fallback
NullHandlingStrategy effectiveStrategy = def.nullStrategy() != null
    ? def.nullStrategy()
    : ctx.registry().nullHandlingStrategy();
var resolvedArgs = applyNullStrategy(rawArgs, def, effectiveStrategy);
```

### 8.4. FunctionRegistry — withBuiltins registers all new evaluators

All string evaluators, list evaluator, date evaluators, coalesce evaluators are registered as builtins by `FunctionRegistry.withBuiltins()`. SQL evaluators (`DbAccessEvaluator`, `DbAggregatorEvaluator`, `DbFieldAccessEvaluator`, `FilterBuilderEvaluator`) are NOT included — consumers register them manually with their own `DbAccessor`.

### 8.5. CoreFunctions — new constants

Added:
- `STRING_STARTS_WITH`, `STRING_ENDS_WITH`, `STRING_FUZZY_MATCH`, `STRING_SIMILARITY`
- `STRING_MATCH`, `STRING_LENGTH`, `STRING_TRIM`, `STRING_SUBSTRING`, `STRING_REPLACE`
- `LIST`
- `NOW`, `DATE_PLUS`, `DATE_MINUS`, `DATE_DIFF`, `DATE_FORMAT`, `DATE_PARSE`
- `EXTRACT_YEAR`, `EXTRACT_MONTH`, `EXTRACT_DAY`, `EXTRACT_HOUR`, `EXTRACT_MINUTE`, `EXTRACT_SECOND`
- `COALESCE`, `DEFAULT_IF_NULL`
- `BUILD_FILTER`
- `DB_ACCESS`, `DB_AGGREGATOR`, `DB_FIELD_ACCESS`

---

## 9. Complete Usage Flow

### 9.1. Set up Registry (app startup)

```java
FunctionRegistry registry = FunctionRegistry.withBuiltins(NullStrategies.PROPAGATE);

// Register custom SQL accessor
DbAccessor dbAccessor = new JooqDbAccessor(dslContext, schemaReg);
SchemaRegistry schemaReg = dbAccessor.getSchemaRegistry();
registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.DB_AGGREGATOR)
    .evaluator(new DbAggregatorEvaluator(dbAccessor, schemaReg))
    .namedParam(ParamSpec.required("tableName", ParamType.STRING))
    .namedParam(ParamSpec.required("fieldName", ParamType.STRING))
    .namedParam(ParamSpec.required("aggregator", ParamType.STRING))
    .namedParam(ParamSpec.optional("filters", ParamType.ANY))
    .namedParam(ParamSpec.optional("groupBy", ParamType.ANY))
    .nullStrategy(NullStrategies.PROPAGATE)
    .build());

registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.DB_FIELD_ACCESS)
    .evaluator(new DbFieldAccessEvaluator(dbAccessor, schemaReg))
    .namedParam(ParamSpec.required("tableName", ParamType.STRING))
    .namedParam(ParamSpec.required("fieldName", ParamType.STRING))
    .namedParam(ParamSpec.optional("path", ParamType.ANY))
    .nullStrategy(NullStrategies.PROPAGATE)
    .build());
```

### 9.2. Dry-Run Validation (CI/CD)

```java
EvaluationContext dryCtx = new EvaluationContext.Builder(registry)
    .body(inputData)
    .dryRun(true)
    .build();

EvaluationResult dryResult = new AstEvaluator().evaluate(ruleAst, dryCtx);
assert dryResult.flattenErrors().isEmpty() : "Config validation failed";
```

### 9.3. Production Evaluation

```java
Message msg = Message.withHeaders(
    Map.of("requestId", "req-456", "tenant", "acme"),
    inputPayload
);

EvaluationContext ctx = new EvaluationContext.Builder(registry)
    .message(msg)
    .build();

EvaluationResult result = new AstEvaluator().evaluate(ruleAst, ctx);

// Use result
boolean approved = Boolean.TRUE.equals(result.returnValue());
List<EvaluationError> errors = result.flattenErrors();

// Compliance audit
for (EvaluationResult node : result.flatten()) {
    if (node.hasAttribute("evidence.records")) {
        complianceLog.write("Records: " + node.getAttribute("evidence.records"));
    }
}

// Performance monitoring
EvaluationSummary summary = EvaluationSummary.from(result);
metrics.record("eval.duration", summary.totalDurationNanos());
metrics.record("eval.nodes", summary.totalNodes());
```

---

## 10. Builtin Evaluator Registration

All builtin evaluators are registered by `FunctionRegistry.withBuiltins()` — calling it sets up:

### Core evaluators
- `ArithmeticEvaluator` (add, subtract, multiply, divide)
- `BooleanLogicEvaluator` (and, or)
- `NotEvaluator` (not)
- `EqualEvaluator` (eq)
- `ComparisonEvaluator` (gt, gte, lt, lte)
- `PayloadEvaluator` (payload)
- `IsEmptyEvaluator` (isEmpty)
- `ListEvaluator` (list)
- `CoalesceEvaluator` (coalesce)
- `DefaultIfNullEvaluator` (defaultIfNull)

### String evaluators (in `builtin.string` sub-package)
- `StringContainsEvaluator`, `StringStartsWithEvaluator`, `StringEndsWithEvaluator`
- `StringFuzzyMatchEvaluator`, `StringSimilarityEvaluator`, `StringMatchEvaluator`
- `StringLengthEvaluator`, `StringTrimEvaluator`, `StringSubstringEvaluator`, `StringReplaceEvaluator`

### Date evaluators (single class with lambdas)
- `DateEvaluators` registers: now, datePlus, dateMinus, dateDiff, dateFormat, dateParse
- `DateEvaluators` registers extract lambdas: extractYear, extractMonth, extractDay, extractHour, extractMinute, extractSecond

### SQL evaluators (NOT registered by withBuiltins — consumer must register manually)
- `DbAccessEvaluator`, `DbAggregatorEvaluator`, `DbFieldAccessEvaluator`, `FilterBuilderEvaluator`

---

## 11. Summary of Changes from Draft

| Concern | Draft (`io.github.khezyapp.core.v1`) | Final (`io.github.khezyapp.ast.core`) |
|---------|--------------------------------------|--------------------------------------|
| Package | `io.github.khezyapp.core.v1` (flat) | `io.github.khezyapp.ast.core` (9 sub-packages) |
| Null strategy | Single global per-registry | Per-function override + global fallback |
| String evaluators | Only `stringContains` | 10 evaluators (contains, startsWith, endsWith, fuzzyMatch, similarity, match, length, trim, substring, replace) |
| List evaluator | None | `list` — construct dynamic lists from evaluated children |
| Filter evaluator | None | `buildFilter` — construct `DbFilter` with expression-aware `FilterValue` (Literal / ColumnRef / Subquery) |
| SQL evaluators | None | DbAccess (query-based), DbAggregator, **DbFieldAccess** (path-based FK traversal), **FilterBuilder** |
| SQL schema model | None | `SchemaRegistry`, `TableMetadata`, `FieldMetadata`, `LinkMetadata`, `FilterValue` |
| Dry-run | None | `EvaluationContext.dryRun` flag + `DbAccessor.dryRunQuery()` |
| Guidelines | Implicit | Explicit: attributes, error codes, trace, evidence, UI patterns, **Node structure conventions** |
| Evidence | Mentioned but no convention | Full convention table + propagation rules |
| FunctionRegistry | Mutable, no freeze | Added `freeze()` for production immutability |
| Builtin count | 14 function IDs | 28 function IDs (+14 new) |
