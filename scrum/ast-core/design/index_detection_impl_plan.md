# Implementation Plan: Dynamic Index Detection Core

## 1. Scope

A reusable algorithm-only module (no database drivers, no ORM, no SQL) that, given AST nodes containing aggregate expressions and a list of existing database indexes, outputs the minimal set of concrete indexes to create.

### What it is NOT
- Not a DDL executor (no `CREATE INDEX` execution)
- Not an ORM integration
- Not a query planner

### What it IS
- Pure data model + algorithm that can be embedded in any Java project
- Input: AST `Node` objects (our existing model) + existing `ConcreteIndex` list
- Output: `List<ConcreteIndex>` ready for any DDL converter

---

## 2. Package Structure

```
io.github.khezyapp.ast.core.index/
├── model/                                  # Data models & value objects
│   ├── FieldExpression.java                # extracted column name + functional flag
│   ├── FilterOperator.java                 # enum: EQUAL, LESS_THAN, etc.
│   ├── FilterCondition.java                # parsed filter with FieldExpression + operator
│   ├── IndexType.java                      # enum: AGGREGATION, FUNCTIONAL
│   ├── AggregateQueryFamily.java           # extracted query family from AST
│   ├── IndexFamily.java                    # projected index family: Fixed/Flex/Last/Included
│   └── ConcreteIndex.java                  # final ordered output
├── analyzer/                               # AST analysis layer
│   └── AstIndexAnalyzer.java               # Step 1: walk AST → extract query families
└── planner/                                # Planning algorithm layer
    ├── IndexPlanner.java                   # Steps 2-5: plan → filter → minimize → project
    └── RefinementMerger.java               # Step 4c: the refine-merge algorithm
```

### Rationale for grouping

| Sub-package | Contains | Responsibility |
|---|---|---|
| `model` | `FieldExpression`, `FilterOperator`, `FilterCondition`, `IndexType`, `AggregateQueryFamily`, `IndexFamily`, `ConcreteIndex` | Pure data carriers with no external dependencies beyond JDK. Records, enums, and simple beans with defensive copies. |
| `analyzer` | `AstIndexAnalyzer` | Walks the AST, extracts `FieldExpression` from filter nodes, builds `AggregateQueryFamily` instances. The only class that depends on `Node` from `io.github.khezyapp.ast.core.model`. |
| `planner` | `IndexPlanner`, `RefinementMerger` | Orchestrates the algorithm pipeline (plan → filter → minimize → project). Package-private `RefinementMerger` is an internal implementation detail of the planner. |

All groups depend only on the `model` sub-package and JDK. This enforces a clean dependency hierarchy: `planner → model ← analyzer`.

---

## 3. Data Model (records / value objects)

### FieldExpression — extracted from a filter's field-name expression tree

A filter's field name is not always a simple column name. It can be:

| Pattern | Example AST | Extracted meaning |
|---------|------------|-------------------|
| Simple column | `constant("status")` | column `status` |
| Payload access | `function(payload, named: {fieldName: constant("status")})` | column `status` |
| Function-wrapped | `function(upper, [constant("status")])` | column `status` with functional transform |
| Expression | `function(concat, [constant("first"), constant("_last")])` | non-indexable (composite expression) |

```java
public record FieldExpression(
    String columnName,         // the resolved base column name
    boolean isFunctional,      // true if wrapped in a function (UPPER, LOWER, etc.)
    String transformFunction   // "" if not functional; e.g. "upper", "lower"
)
```

### ExpressionIndexMetadata — sealed hierarchy replacing the boolean flag

`FieldExpression` uses a closed boolean flag (`isFunctional`) — every new expression category requires modifying the record. The sealed hierarchy replaces this with an extensible, type-safe contract.

```java
public sealed interface ExpressionIndexMetadata
    permits PlainColumn, FunctionalColumn, GinColumn, NonIndexable {

    String columnName();

    default boolean isIndexable()    { return !(this instanceof NonIndexable); }
    default boolean isFunctional()   { return this instanceof FunctionalColumn; }
    default boolean isGin()          { return this instanceof GinColumn; }
}

record PlainColumn(String columnName)
    implements ExpressionIndexMetadata {}

record FunctionalColumn(String columnName, String transformFunction)
    implements ExpressionIndexMetadata {}

record GinColumn(String columnName, String operatorClass)
    implements ExpressionIndexMetadata {}

record NonIndexable(String expressionRepresentation)
    implements ExpressionIndexMetadata {}
```

**Mapping to IndexType:**

| Metadata variant | IndexType | Example expression |
|---|---|---|
| `PlainColumn("status")` | AGGREGATION | `constant("status")` |
| `FunctionalColumn("email", "upper")` | FUNCTIONAL | `upper(email)` |
| `GinColumn("body", "tsvector_ops")` | GIN | `to_tsvector('english', body)` |
| `GinColumn("data", "jsonb_ops")` | GIN | `payload("data")` used with `@>` |
| `NonIndexable("concat(...)")` | none | `concat(a, "_", b)` |

**Extraction rule** (now delegated to resolvers, see Section 12):
- Each functionId has a registered `ExpressionIndexResolver`
- The resolver returns the correct `ExpressionIndexMetadata` variant
- The `AstIndexAnalyzer` switches on the sealed type instead of checking boolean flags

### FilterCondition — a single parsed filter from the AST

```java
public record FilterCondition(
    FieldExpression field,       // the field side of the filter
    FilterOperator operator,      // classified operator
    boolean valueIsExpression    // true if the value side is an expression, not a constant
)
```

Using `FilterCondition` instead of raw column name strings allows the analyzer to preserve SARGability information.

### AggregateQueryFamily

```java
public record AggregateQueryFamily(
    String tableName,
    String fieldName,              // the aggregated field (SUM, COUNT, etc.)
    Set<String> eqConditions,     // simple "=" filter column names
    Set<String> ineqConditions,   // "<", "<=", ">", ">=" filter column names
    Set<String> otherConditions,  // IsInList, FuzzyMatch, non-indexable, etc.
    Set<String> ginConditions,    // columns with GIN-only operators (jsonb, fulltext, array)
    Set<String> functionalColumns // column names wrapped in a function (UPPER, etc.)
)
```

- **Deduplication**: Two families with the same tableName + sorted(eq) + sorted(ineq) + sorted(other) + sorted(functional) are considered equal.
- The aggregated `fieldName` is auto-added to `otherConditions` if not already in eq or ineq.
- Columns in `functionalColumns` appear in BOTH their respective condition set (eq/ineq/other) AND this set. This signals that a functional index is required.
- A filter whose field is unresolvable (composite expression) adds its field name representation to `otherConditions` only — no column name extracted.

### IndexFamily

```java
public record IndexFamily(
    String tableName,
    List<String> fixed,     // ordered prefix columns
    Set<String> flex,       // unordered equality columns (can be reordered)
    String last,            // optional single range column ("")
    Set<String> included,   // cover-only columns (non-indexed)
    Set<String> functional  // subset of indexed columns needing functional index
)
```

When projecting to `ConcreteIndex`, if `functional` is non-empty, the index type is `FUNCTIONAL` instead of `AGGREGATION`.

### ConcreteIndex

```java
public record ConcreteIndex(
    String tableName,
    List<String> indexed,   // ordered: fixed + sorted(flex) + [last]
    List<String> included,  // sorted cover-only columns
    IndexType type,
    String operatorClass   // "" for B-tree; "jsonb_ops", "array_ops", etc. for GIN
)
```

### FilterOperator

```java
public enum FilterOperator {
    EQUAL, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL,
    IS_IN_LIST, FUZZY_MATCH, STARTS_WITH,
    JSONB_CONTAINS, JSONB_KEY_EXISTS, ANY_KEY_EXISTS, ALL_KEYS_EXIST,
    JSONB_PATH_MATCH, ARRAY_CONTAINS, ARRAY_OVERLAP, ARRAY_CONTAINED_BY,
    FULLTEXT_MATCH, REGEX_MATCH, SIMILAR_TO, OTHER
}
```

### IndexType

```java
public enum IndexType {
    AGGREGATION,     // plain column indexes
    FUNCTIONAL,      // at least one column needs a function wrapper (UPPER, LOWER, etc.)
    GIN              // GIN index (jsonb, full-text, array)
}
```

---

## 4. Algorithm Pipeline

### Step 1 — AstIndexAnalyzer.extractQueryFamilies(Node root)

Walk the AST recursively. For each node whose function value equals `"aggregator"`:

1. Read `namedChildren["tableName"].constant` → table name
2. Read `namedChildren["fieldName"].constant` → aggregated field
3. If `namedChildren["filters"]` exists, iterate its children list:
   - Each filter node has `namedChildren["operator"].constant` (string: "=", "<", etc.)
   - Each filter node has a `namedChildren["fieldName"]` **expression node** (may be constant, payload, or function-wrapped)
   - Each filter node may have a `namedChildren["value"]` expression node
   - **Extract** the `FieldExpression` from the fieldName node via `extractFieldExpression(node)`
   - **Classify** the filter operator into eq/ineq/other
   - If `FieldExpression.isFunctional` → add column name to both the condition set AND `functionalColumns`
   - If `FieldExpression` is unresolvable → add the expression string representation to `otherConditions`
   - If the value side is an expression (not constant) → mark for documentation, doesn't affect index columns
4. Auto-add `fieldName` to `otherConditions` if not already in eq/ineq
5. Deduplicate via `AggregateQueryFamily.equals/hashCode` (order-independent on sets)

**FieldExpression extraction** — now delegated to the `IndexResolverRegistry`:

```
function resolveFieldExpression(expr, registry):
    return registry.resolve(expr)
    // Built-in resolvers handle:
    //   constant     → PlainColumn(value)
    //   payload/field → PlainColumn(extracted fieldName) or nested
    //   upper/lower/trim → FunctionalColumn(innerColumn, fnName)
    //   to_tsvector  → GinColumn(innerColumn, "tsvector_ops")
    //   unknown      → NonIndexable(toString())
```

The pseudo-code for the built-in `"payload"` / `"field"` resolver:

```
function resolvePayload(expr, registry):
    fieldNameNode = expr.namedChildren().get("fieldName")
    if fieldNameNode == null:
        return NonIndexable(expr.toString())
    if fieldNameNode.isConstant():
        return PlainColumn(fieldNameNode.constant().toString())
    return registry.resolve(fieldNameNode)   // recursive
```

The built-in transform resolver (handles `"upper"`, `"lower"`, `"trim"`, `"extractHour"`, etc.):

```
function resolveTransform(expr, registry):
    if expr.children().isEmpty():
        return NonIndexable(expr.toString())
    inner = registry.resolve(expr.children().get(0))
    if inner.isIndexable():
        return FunctionalColumn(inner.columnName(), expr.function().value())
    return NonIndexable(expr.toString())
```

**Pseudo-code** (using the resolver registry):

```
for each node in walk(root):
  if node.function().value().equals("aggregator"):
    table   = node.namedChildren().get("tableName").constant()
    field   = node.namedChildren().get("fieldName").constant()
    filters = node.namedChildren().get("filters")  // may be null
    
    eq    = new LinkedHashSet<>()
    ineq  = new LinkedHashSet<>()
    other = new LinkedHashSet<>()
    func  = new LinkedHashSet<>()
    gin   = new LinkedHashSet<>()
    
    if filters != null:
      for filterNode in filters.children():
        metadata = registry.resolve(filterNode.namedChildren().get("fieldName"))
        op       = filterNode.namedChildren().get("operator").constant()
        
        if metadata instanceof NonIndexable:
          other.add(metadata.columnName())
        else if isGinOperator(op):
          col = metadata.columnName()
          gin.add(col)
          if metadata instanceof FunctionalColumn:
            func.add(col)
        else:
          col = metadata.columnName()
          classify(op, col, eq, ineq, other)
          if metadata instanceof FunctionalColumn:
            func.add(col)
    
    if field not in eq and field not in ineq and field not in gin:
      other.add(field)
    
    families.add(new AggregateQueryFamily(table, field, eq, ineq, other, gin, func))
```

### Step 2 — IndexPlanner.planIndexFamilies(AggregateQueryFamily qf)

Map one query family to one or more index families.

**Rule**:
- Eq columns → `flex` (unordered indexed set)
- Other conditions → `included` (cover-only)
- Ineq columns → each generates a separate family with that column as `last`
- `functionalColumns` → carried through to `IndexFamily.functional`; projected as `FUNCTIONAL` index type

**Edge case**: If no eq and no ineq → return empty (nothing indexable).

### Step 3 — IndexPlanner.filterExisting(Set<IndexFamily> families, List<ConcreteIndex> existing)

For each family, check if any existing `ConcreteIndex` covers it:

**Coverage** (ConcreteIndex.covers(IndexFamily)):
- Table name must match
- ConfixIndex.indexed must have at least `fixed.size()` columns matching prefix
- After the fixed prefix, a contiguous slice of flex columns must match as a set
- If `last` is present, it must be at column position `fixed.size() + flex.size()`
- Existing index's included must be a superset of family's included

### Step 4 — IndexPlanner.minimize(Set<IndexFamily> families)

Group families by table, then for each group:

1. Sort families by canonical representation
2. Iterate: for each input family, try to merge with an existing output family via `RefinementMerger.refine()`
3. If merge succeeds → replace output entry with merged result
4. If merge fails → add input as new output entry

The **refinement algorithm** handles:
- **Case A**: Both have non-empty fixed with different columns → cannot merge
- **Case B**: Matching fixed prefix → strip prefix, recurse on remainder, prepend back
- **Case C**: One has empty fixed → three sub-cases for subset merging

### Step 5 — IndexPlanner.projectToConcrete(Set<IndexFamily> families)

For each minimized family:
- Build `indexed`: fixed + sorted(flex) + [last if present]
- Build `included`: sorted(remaining)
- Generate deterministic name: `idx_{TABLE}_{sorted-columns}_{short-uuid}`, truncated to 63 chars

---

## 5. Dependencies

| Class | Depends On |
|-------|-----------|
| `FieldExpression` | — |
| `FilterOperator` | — |
| `FilterCondition` | `FilterOperator`, `FieldExpression` |
| `IndexType` | — |
| `AggregateQueryFamily` | `FieldExpression` |
| `IndexFamily` | — |
| `ConcreteIndex` | `IndexType` |
| `AstIndexAnalyzer` | `Node` (from `io.github.khezyapp.ast.core.model`), `AggregateQueryFamily`, `FieldExpression`, `FilterOperator` |
| `RefinementMerger` | `IndexFamily` |
| `IndexPlanner` | `AggregateQueryFamily`, `IndexFamily`, `ConcreteIndex`, `RefinementMerger` |

### Sub-package dependency flow

```
┌─────────────┐     ┌──────────────┐
│  analyzer   │────▶│    model     │◀────┐
│ AstAnalyzer │     │ (all model   │     │ planner
└─────────────┘     │  classes)    │─────┤ IndexPlanner
                    └──────────────┘     │ RefinementMerger
                                         └──────────────┘
```

---

## 6. Implementation Order

| Phase | Files | Sub-package | Why this order |
|-------|-------|-------------|---------------|
| 1 | `IndexType`, `FilterOperator`, `FieldExpression` | `model` | No dependencies, used by all later types |
| 2 | `FilterCondition`, `AggregateQueryFamily`, `IndexFamily`, `ConcreteIndex` | `model` | Depend only on enums/records above |
| 3 | `AstIndexAnalyzer` | `analyzer` | Depends only on model + `Node` from core |
| 4 | `RefinementMerger` | `planner` | Depends only on `IndexFamily` |
| 5 | `IndexPlanner` | `planner` | Depends on model + merger |
| 6 | `EndToEndIndexDetectionTest` | test | Exercises the full pipeline with sample ASTs |

---

## 7. Testing Strategy

### Scenarios for the end-to-end sample test:

| # | Scenario | What it verifies |
|---|----------|-----------------|
| 1 | Basic: one aggregator, single eq filter | Simple family extraction |
| 2 | Multiple eq + no ineq | One index family with flex only |
| 3 | Eq + ineq (single) | One family with last |
| 4 | Eq + multiple ineq | Multiple families (one per ineq column) |
| 5 | No eq, no ineq → no index | Empty output |
| 6 | Duplicate aggregations across rules | Deduplication |
| 7 | Existing index covers family | Filtered out from output |
| 8 | Partial coverage from existing | Non-covered families survive |
| 9 | Minimization: merge two overlapping families | Reduced count after merge |
| 10 | Nested aggregators in deep AST | Recursive extraction works |
| 11 | All filter types (eq, ineq, IsInList, FuzzyMatch) | Correct classification |
| 12 | Aggregation with no filters at all | fieldName goes to other → no index (no eq/ineq) |
| 13 | Expression filter: `UPPER(status) = "ACTIVE"` | FieldExpression extracted: functional=true, column=status; IndexType=FUNCTIONAL |
| 14 | Expression filter: fieldName is payload access (`payload(fieldName="status")`) | Extracted to simple column `status` |
| 15 | Expression filter: value side is expression (`amount > payload("threshold")`) | Column extracted as `amount` (value expression doesn't affect index) |
| 16 | Mixed: some plain eq, one functional eq, one ineq | One FunctionalIndexFamily for the functional + separate families for ineq |

---

### Java Core Library Adaptation

The algorithm is implemented as concrete final classes (not interfaces — see Decision #design-by-struct):

| Go concept | Java equivalent |
|---|---|
| `parseAST(root, registry)` | `AstIndexAnalyzer.extractQueryFamilies(root, registry)` |
| `planIndexFamilies(qf)` | `IndexPlanner.planIndexFamilies(AggregateQueryFamily qf)` — static method |
| `filterExisting(families, existing)` | `IndexPlanner.filterExisting(Set<IndexFamily>, List<ConcreteIndex>)` — static method |
| `minimize(families)` | `IndexPlanner.minimize(Set<IndexFamily>)` — static method |
| `projectToConcrete(families)` | `IndexPlanner.projectToConcrete(Set<IndexFamily>)` — static method |
| `analyzeAndPlan(root, existing)` | `IndexPlanner.analyzeAndPlan(Node root, List<ConcreteIndex> existing)` — top-level entry point |

Note: There is no `IndexExecutor` interface. DDL execution is left to the consumer (out of scope).

The **value side** being an expression never changes what index is needed — it only affects query planning (the DB evaluates the expression before the index lookup). The **field side** being an expression changes both the index column extraction AND potentially requires a functional index if wrapped in a transform.

### SARGability rules enforced by the analyzer

```
filter(operator="=", fieldName=UPPER(status), value="ACTIVE")
                          ↓
  FieldExpression.columnName = "status"
  FieldExpression.isFunctional = true
  FieldExpression.transformFunction = "upper"
                          ↓
  Added to eqConditions AND functionalColumns
                          ↓
  ConcreteIndex(type = FUNCTIONAL)
  CREATE INDEX ... ON table (UPPER(status), ...)
```

### FilterOperator classification with expressions

The operator classification remains the same regardless of whether the field/value are expressions:

| Operator string | Classification | Side expression impact |
|---|---|---|
| `"="` | EQUAL → eqConditions | None — index column unchanged |
| `"<"`, `">"`, etc. | INEQ → ineqConditions | None — index column unchanged |
| `"IsInList"`, `"FuzzyMatch"` | OTHER → otherConditions | None |
| Any operator + unresolvable field | OTHER → otherConditions | Field is expression, can't extract column |

---

## 9. Real-World Use-Case Scenarios

These scenarios are derived from PostgreSQL B-tree index behavior (Chapter 11 — PostgreSQL Index Reference). Each scenario exercises a specific real-world query pattern and validates the full pipeline (analyzer → planner → minimize → project).

### UC-1: E-commerce order listing with customer + status + date range

```
Table: orders
Query: SELECT SUM(amount) FROM orders
       WHERE customer_id = 42 AND status = 'shipped' AND created_at > '2024-06-01'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={customer_id, status}, ineq={created_at}, no functional |
| Plan | 1 IndexFamily: flex={customer_id, status}, last=created_at |
| Minimize | Unchanged (single family) |
| Project | ConcreteIndex indexed=[customer_id, status, created_at], type=AGGREGATION |

**Real-world relevance**: Most common dashboard query pattern — drill into a customer's shipped orders within a date range. The B-tree index should have equality columns first, then the range column.

### UC-2: Multi-tenant analytics with time range

```
Table: events
Query: SELECT COUNT(*) FROM events
       WHERE tenant_id = 'acme' AND event_type = 'purchase'
       AND occurred_at BETWEEN '2024-01-01' AND '2024-03-31'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={tenant_id, event_type}, ineq={occurred_at}, no functional |
| Plan | 1 IndexFamily: flex={tenant_id, event_type}, last=occurred_at |
| Project | indexed=[event_type, occurred_at, tenant_id] (sorted flex) |

**Real-world relevance**: SaaS multi-tenant architecture where every query filters by tenant_id first, then by event type and time range.

### UC-3: Case-insensitive auth lookup (functional index)

```
Table: users
Query: SELECT SUM(logins) FROM users
       WHERE UPPER(email) = 'USER@EXAMPLE.COM' AND status = 'active'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={email, status}, functional={email} (UPPER transform) |
| Plan | 1 IndexFamily: flex={status}, functional={email} |
| Project | ConcreteIndex indexed=[status, email], type=FUNCTIONAL |

**Real-world relevance**: Case-insensitive login lookup. PostgreSQL requires a functional index on `UPPER(email)` for SARGability.

### UC-4: Existing composite index covers new query

```
Table: orders
Existing index: idx_orders_customer ON orders (customer_id, status, created_at) INCLUDE (amount)
Query: SELECT SUM(amount) FROM orders
       WHERE customer_id = 42 AND status = 'shipped' AND created_at > '2024-06-01'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={customer_id, status}, ineq={created_at} |
| Plan | 1 IndexFamily: flex={customer_id, status}, last=created_at, included={amount} |
| filterExisting | existing `(customer_id, status, created_at)` covers → filtered OUT |

**Real-world relevance**: Avoids redundant index recommendations when an existing composite index already satisfies the query.

### UC-5: Partial coverage — existing index is missing a column

```
Table: orders
Existing index: idx_orders_customer ON orders (customer_id, created_at) INCLUDE (amount)
Query: SELECT SUM(amount) FROM orders
       WHERE customer_id = 42 AND category = 'food' AND created_at > '2024-06-01'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={customer_id, category}, ineq={created_at} |
| Plan | 1 IndexFamily: flex={customer_id, category}, last=created_at |
| filterExisting | existing `(customer_id, created_at)` does NOT cover → survives |

**Real-world relevance**: Detects missing columns — the existing index has `customer_id` + `created_at` but the query also needs `category`. A new index `(customer_id, created_at, category)` would be more efficient, or the system should recommend splitting.

### UC-6: Multiple range conditions generate separate index families

```
Table: products
Query: SELECT COUNT(*) FROM products
       WHERE category = 'electronics'
       AND price BETWEEN 100 AND 500
       AND rating >= 4
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={category}, ineq={price, rating} |
| Plan | 2 IndexFamilies: (1) flex={category}, last=price; (2) flex={category}, last=rating |
| Minimize | Cannot merge (differ in last column) → 2 families |
| Project | 2 ConcreteIndexes: `[category, price]` and `[category, rating]` |

**Real-world relevance**: PostgreSQL can only use one range column per index scan. The algorithm correctly suggests two composite indexes, each optimized for a specific range column.

### UC-7: Query with no filters at all → empty output

```
Table: logs
Query: SELECT COUNT(*) FROM logs
       WHERE no filter conditions (bare aggregation)
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={}, ineq={}, other={fieldName}, no functional |
| Plan | Empty (no eq or ineq) |
| Project | Empty list |

**Real-world relevance**: Some aggregations have no WHERE clause. No index can help filter rows, so no recommendation is made. (A partial index or BRIN index might help, but that's beyond the scope of B-tree recommendation.)

### UC-8: Case-insensitive search + range (mixed functional + plain range)

```
Table: users
Query: SELECT COUNT(*) FROM users
       WHERE UPPER(last_name) = 'SOK' AND created_at > '2024-01-01'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={last_name}, ineq={created_at}, functional={last_name} |
| Plan | 1 IndexFamily: flex={}, last=created_at, functional={last_name} |
| Project | ConcreteIndex indexed=[created_at, last_name], type=FUNCTIONAL |

**Real-world relevance**: Real applications combine functional comparisons with range filters. The functional index on `UPPER(last_name)` cannot serve the range on `created_at` at the same B-tree position. The algorithm correctly outputs a composite functional + range index.

### UC-9: Merge two overlapping index families after inequality splitting

```
Table: transactions
Query: SELECT SUM(amount) FROM transactions
       WHERE user_id = 42 AND status = 'completed'
       AND created_at > '2024-06-01' AND amount > 100
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={user_id, status}, ineq={created_at, amount} |
| Plan | 2 families: (A) flex={user_id, status}, last=created_at; (B) flex={user_id, status}, last=amount |
| Minimize | Cannot merge (different last) → 2 families |
| filterExisting | If none cover → both survive |
| Project | 2 ConcreteIndexes |

**Real-world relevance**: When a query has multiple range columns, PostgreSQL picks one for the index scan and filters the rest. The algorithm outputs both options, leaving the choice to the DBA.

### UC-10: Duplicate aggregations across rules (deduplication)

```
Two rules in the same ruleset:
Rule A: SELECT SUM(revenue) FROM orders WHERE customer_id = 42 AND created_at > '2024-06-01'
Rule B: SELECT SUM(revenue) FROM orders WHERE customer_id = 42 AND created_at > '2024-06-01'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | Two identical AggregateQueryFamily instances |
| Dedup | Set de-duplicates to 1 entry |
| Plan → Project | 1 ConcreteIndex |

**Real-world relevance**: Rule engines often have multiple rules that query the same aggregation pattern. Deduplication prevents redundant index recommendations.

### UC-11: Chained payload access in filter expression

```
AST: filter(
       operator="=",
       fieldName=function(payload, named: {fieldName: function(payload, named: {fieldName: constant("nested_field")})}),
       value=constant("expected")
     )
```

| Pipeline step | Expected behavior |
|---|---|
| extractFieldExpression | Resolves recursively: `nested_field` with isFunctional=false |
| Analyzer | eq={nested_field}, no functional |
| Plan | 1 IndexFamily: flex={nested_field} |

**Real-world relevance**: JSON/payload field access can be nested (e.g., `payload.payload.nested_field`). The recursive extraction correctly resolves deeply nested field accesses.

### UC-12: Payload field access filter

```
AST: filter(
       operator="=",
       fieldName=function(payload, named: {fieldName: constant("status")}),
       value=constant("active")
     )
Query: SELECT SUM(total) FROM orders WHERE payload(status) = 'active'
```

| Pipeline step | Expected behavior |
|---|---|
| extractFieldExpression | Resolves to `status` with isFunctional=false |
| Analyzer | eq={status} |
| Plan | 1 IndexFamily: flex={status}, no functional |

**Real-world relevance**: Object/document databases often store fields inside a JSONB payload column. The analyzer understands `payload(fieldName=...)` as a regular column access.

### UC-13: Nested aggregators at different AST depths

```
Root expression: CONCAT(
   aggregator("orders", "total", filter("=", constant("status"), constant("active"))),
   aggregator("invoices", "amount", filter(">", constant("total"), constant(100)))
)
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | 2 families extracted: orders(status=eq) and invoices(total=ineq) |
| Plan | orders → 1 IndexFamily flex={status}; invoices → 1 IndexFamily last=total |
| Project | 2 ConcreteIndexes |

**Real-world relevance**: Complex business rule expressions may aggregate over multiple tables within the same expression tree. The recursive walker finds all aggregators regardless of nesting depth.

### UC-14: Mixed filter types — eq, ineq, IsInList, fuzzy match

```
Table: tickets
Query: SELECT COUNT(*) FROM tickets
       WHERE assignee_id = 42
       AND priority IN ('high', 'critical')
       AND title FuzzyMatch 'urgent'
       AND created_at > '2024-06-01'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={assignee_id}, ineq={created_at}, other={priority, title} |
| Plan | 1 IndexFamily: flex={assignee_id}, last=created_at, included={priority, title} |

**Real-world relevance**: Real queries mix exact match, IN-list, fuzzy text search, and date range. IN-list and fuzzy operators cannot be indexed directly for range scans, so they become INCLUDE (covering) columns.

### UC-15: No indexable conditions → nothing to recommend

```
Table: orders
Query filtered only with IsInList and FuzzyMatch (no eq or ineq):
SELECT SUM(amount) FROM orders
WHERE status IN ('pending', 'processing') AND description FuzzyMatch 'urgent'
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={}, ineq={}, other={fieldName, status, description} |
| Plan | Empty (no eq or ineq) → nothing indexable |
| Project | Empty list |

**Real-world relevance**: When filters are entirely non-equality and non-range (IN-list, fuzzy match, full-text), a B-tree index cannot accelerate the query. The algorithm correctly returns nothing, indicating that a GIN index or separate index per condition would be needed (out of scope).

### UC-16: Value side expression does not affect index detection

```
Table: orders
Query: SELECT SUM(amount) FROM orders
       WHERE total > payload("threshold")
```

| Pipeline step | Expected behavior |
|---|---|
| Analyze | eq={}, ineq={total}, value side expression ignored for index |
| Plan | 1 IndexFamily: last=total |

**Real-world relevance**: The value side being a dynamic expression (another column, a computation) doesn't change what column needs indexing. Only the field side matters.

---

## 10. Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Functions identified by string | `node.function().value().equals("aggregator")` | Core doesn't know about "aggregator" — it's a consumer-defined function name. The analyzer is a consumer tool. |
| Sets vs lists for conditions | `Set<String>` for eq/ineq/other | Order doesn't matter for equality conditions; deduplication uses set equality |
| `last` as single column | `String`, not `List<String>` | B-tree index can only use one column for range scan |
| Fixed prefix from refinement | `List<String>` | Order matters — it's the surviving structure from merging |
| No `@Nullable` | Empty string for missing `last` | Simpler, avoids null checks in records |
| No external dependencies | Pure Java + `Node` from `io.github.khezyapp.ast.core.model` | Reusable across any project |
| Sub-package isolation | `model` / `analyzer` / `planner` | Clean dependency hierarchy: planner→model←analyzer; no cross-talk between analyzer and planner |

---

## 11. GIN Index Support

### Scope

GIN (Generalized Inverted Index) support extends the index detection pipeline to recommend GIN indexes for queries with JSONB containment, array containment, full-text search, and trigram operators. All GIN features described in this section are already implemented in the current codebase (not a future version). GIN indexes are fundamentally different from B-tree indexes:

- **No column ordering** — all indexed columns are equally effective regardless of order
- **No range scan** — GIN supports containment, existence, and overlap operators, not `<`, `>`, `BETWEEN`
- **No INCLUDE** — GIN does not support included columns for index-only scans
- **Operator classes** — GIN requires specifying an operator class (`jsonb_ops`, `array_ops`, `gin_trgm_ops`, `tsvector_ops`)

### Data Model Changes

#### FilterOperator (new GIN-specific values)

```
enum FilterOperator {
    ... existing ...
    JSONB_CONTAINS,       // @> on JSONB
    JSONB_KEY_EXISTS,     // ? on JSONB
    ANY_KEY_EXISTS,       // ?| on JSONB
    ALL_KEYS_EXIST,       // ?& on JSONB
    JSONB_PATH_MATCH,     // @? or @@ on JSONB
    ARRAY_CONTAINS,       // @> on array
    ARRAY_OVERLAP,        // && on array
    ARRAY_CONTAINED_BY,   // <@ on array
    FULLTEXT_MATCH,       // @@ on tsvector
    REGEX_MATCH,          // ~ on text (trigram)
    SIMILAR_TO,            // SIMILAR TO on text (trigram)
    ... existing OTHER ...
}
```

#### AggregateQueryFamily (new ginConditions field)

```java
public record AggregateQueryFamily(
    ...
    Set<String> ginConditions,    // NEW — columns with GIN-only operators
    ...
)
```

- Columns with GIN operators are tracked in `ginConditions`, separate from `otherConditions`
- A column can appear in BOTH `eqConditions`/`ineqConditions` AND `ginConditions` (mixed query)
- `hasGinConditions()` returns true when `ginConditions` is non-empty

#### ConcreteIndex (GIN support)

```java
public record ConcreteIndex(
    String tableName,
    List<String> indexed,
    List<String> included,
    IndexType type,
    String operatorClass      // "" for B-tree; "jsonb_ops", "array_ops", etc. for GIN
)
```

- `type = GIN` indicates a GIN index
- `operatorClass` is the recommended GIN operator class (informational — DDL converter may refine)
- `isGin()` convenience method
- `coversGin(ConcreteIndex proposed)` — GIN-specific coverage check:
  - Same table + same indexed column
  - `jsonb_ops` covers all JSONB operators (including `jsonb_path_ops`)
  - Other operator classes must match exactly

#### IndexType (new GIN value)

```java
public enum IndexType {
    AGGREGATION,
    FUNCTIONAL,
    GIN    // NEW
}
```

### Operator Classification Updates

The `AstIndexAnalyzer.classifyOperator()` now maps GIN operator strings:

| Operator string | FilterOperator | Classification |
|---|---|---|
| `"jsonb_contains"` | JSONB_CONTAINS | `ginConditions` |
| `"jsonb_key_exists"` | JSONB_KEY_EXISTS | `ginConditions` |
| `"jsonb_any_key_exists"` | ANY_KEY_EXISTS | `ginConditions` |
| `"jsonb_all_keys_exist"` | ALL_KEYS_EXIST | `ginConditions` |
| `"jsonb_path_match"` | JSONB_PATH_MATCH | `ginConditions` |
| `"array_contains"` | ARRAY_CONTAINS | `ginConditions` |
| `"array_overlap"` | ARRAY_OVERLAP | `ginConditions` |
| `"array_contained_by"` | ARRAY_CONTAINED_BY | `ginConditions` |
| `"fulltext_match"` | FULLTEXT_MATCH | `ginConditions` |
| `"regex_match"` | REGEX_MATCH | `ginConditions` |
| `"similar_to"` | SIMILAR_TO | `ginConditions` |

### GIN Pipeline

The GIN pipeline runs parallel to the B-tree pipeline:

```
AST Node
  │
  ▼
AstIndexAnalyzer.extractQueryFamilies()
  │
  ├──▶ B-tree path: planIndexFamilies → filterExisting → minimize → projectToConcrete
  │
  └──▶ GIN path: planGinIndexes → filterExistingGin → (no minimize) → ConcreteIndex list
```

#### Priority Rule

B-tree is prioritized over GIN. If a query has B-tree-indexable conditions (`eqConditions` or `ineqConditions`), only B-tree indexes are recommended — GIN conditions are ignored for index planning. GIN indexes are only recommended when there are NO B-tree-indexable conditions but there ARE GIN-indexable conditions.

This avoids over-indexing: a query with `WHERE status = 'active' AND data @> '{"key":"val"}'` gets a B-tree on `status` (which covers the equality filter), not a GIN on `data` (which covers a filter that can't be the primary scan regardless).

#### Step: planGinIndexes(AggregateQueryFamily)

```
function planGinIndexes(qf):
    if qf.hasIndexableConditions():         // B-tree has priority
        return empty
    if not qf.hasGinConditions():
        return empty
    
    result = new Set
    for each column in qf.ginConditions():
        result.add(ConcreteIndex.gin(qf.tableName(), column, "jsonb_ops"))
    return result
```

- Produces one `ConcreteIndex` per GIN column directly (no `IndexFamily` intermediate — GIN has no fixed/flex/last/included)
- Currently uses `"jsonb_ops"` as the default operator class for all GIN columns
- No minimization needed (each GIN column is independent)

#### Step: filterExistingGin(Set<ConcreteIndex>, List<ConcreteIndex>)

For each proposed GIN index, check if any existing GIN index covers it:

```
function coversGin(existing, proposed):
    if existing.type != GIN or proposed.type != GIN:
        return false
    if existing.tableName != proposed.tableName:
        return false
    if existing.indexed(0) != proposed.indexed(0):
        return false
    // jsonb_ops covers all JSONB operator classes
    return existing.operatorClass == "jsonb_ops"
           || existing.operatorClass == proposed.operatorClass
```

#### Name Generation

GIN index names follow `idx_{TABLE}_{COLUMN}_gin_{short-uuid}`, truncated to 63 characters.

### Use-Case Scenarios (GIN)

| # | Scenario | What it verifies |
|---|----------|-----------------|
| 17 | JSONB containment without B-tree conditions | GIN index recommended on JSONB column |
| 18 | B-tree + GIN mixed | B-tree prioritized; GIN not recommended |
| 19 | JSONB key existence | GIN index recommended |
| 20 | Full-text search | GIN index recommended |
| 21 | Existing GIN covers new GIN proposal | Filtered out (not re-recommended) |
| 22 | Multiple GIN columns | Multiple GIN indexes recommended |
| 23 | Array containment | GIN index recommended |
| 24 | GIN different column not covered | Survives filtering |
| 25 | GIN different table not covered | Survives filtering |
| 26 | GIN name generation | Correct naming prefix |

### Limitations & Future Work

- **Operator class selection**: Currently defaults to `"jsonb_ops"` for all GIN columns. Future work should infer the operator class from the specific GIN operator used (e.g., `jsonb_path_ops` for containment-only, `gin_trgm_ops` for trigram).
- **Functional GIN indexes**: GIN indexes on function-wrapped expressions (e.g., `GIN(to_tsvector('english', body))`) are not yet supported.
- **Partial GIN indexes**: Queries with constant conditions could benefit from partial GIN index recommendations.
- **GIN on multiple columns**: A single GIN index can index multiple columns (`GIN(col1, col2)`). The current implementation recommends one index per GIN column, which may over-recommend.

---

## 12. ExpressionIndexResolver Pattern

### Problem

The original `FieldExpression` uses a **closed boolean flag** (`isFunctional`) for B-tree functional transforms. GIN support was added **orthogonally** — it bypasses `FieldExpression` entirely and uses a separate `ginConditions` set in `AggregateQueryFamily`. This creates two problems:

1. Every new expression category requires modifying `FieldExpression` (closed for extension)
2. GIN and B-tree functional paths use different data structures, forcing the analyzer to manage parallel condition sets

### Solution: functionId → resolver (mirroring the AST evaluator pattern)

The AST evaluation engine uses an open registry pattern: `functionId` → `Evaluator`. The index detection applies the same pattern: `functionId` → `ExpressionIndexResolver`.

```
AST evaluator:   functionId → FunctionRegistry.getEvaluator(id) → Evaluator.evaluate(ctx, args)
Index resolver:  functionId → IndexResolverRegistry.resolve(node) → ExpressionIndexMetadata
```

### Contract Interface

```java
// In analyzer sub-package (depends on Node for expression walking)
@FunctionalInterface
public interface ExpressionIndexResolver {
    ExpressionIndexMetadata resolve(Node expressionNode);
}
```

### Resolver Registry

```java
public final class IndexResolverRegistry {
    private final Map<String, ExpressionIndexResolver> resolvers;

    /** Register a resolver for a functionId */
    public IndexResolverRegistry register(String functionName, ExpressionIndexResolver resolver);

    /** Resolve an expression node using registered resolvers */
    public ExpressionIndexMetadata resolve(Node node);

    /** Create the default registry with built-in resolvers */
    public static IndexResolverRegistry withBuiltins();
}
```

### Built-in resolvers

| FunctionId | Resolver behavior | Returns |
|---|---|---|
| `constant` | (short-circuited — leaf node) | `PlainColumn(constant value)` |
| `"payload"` / `"field"` | Extract `fieldName` named child, recurse | `PlainColumn(...)` or nested result |
| `"upper"`, `"lower"`, `"trim"` | Extract first positional child, recurse, wrap | `FunctionalColumn(innerColumn, functionName)` |
| `"to_tsvector"` | Extract second positional child (the text field), recurse | `GinColumn(innerColumn, "tsvector_ops")` |
| `"jsonb_extract_path_text"` | Extract first positional child (the JSONB field), recurse | `GinColumn(innerColumn, "jsonb_ops")` |
| unknown | Fallback | `NonIndexable(node.toString())` |

Note: `extractHour`, `extractYear`, `extractMonth`, etc. are registered as date evaluator lambdas in the AST core (not as index resolvers). If consumers want functional B-tree index detection for these extract functions, they must register custom resolvers with `IndexResolverRegistry.register()`.

### How the analyzer uses it

The `AstIndexAnalyzer` accepts an `IndexResolverRegistry` (injectable) instead of hardcoding transform function recognition:

```java
public final class AstIndexAnalyzer {
    private static final IndexResolverRegistry DEFAULT_RESOLVERS = IndexResolverRegistry.withBuiltins();
    private final IndexResolverRegistry resolverRegistry;

    public AstIndexAnalyzer() { this(DEFAULT_RESOLVERS); }
    public AstIndexAnalyzer(IndexResolverRegistry resolverRegistry) { ... }

    private ExpressionIndexMetadata resolveFieldExpression(Node expr) {
        return resolverRegistry.resolve(expr);
    }

    // extractFilter uses the resolved metadata:
    var metadata = resolveFieldExpression(fieldExprNode);
    if (!metadata.isIndexable()) {
        builder.addOther(metadata.columnName());
        return;
    }

    if (metadata instanceof FunctionalColumn) {
        builder.addEqFunctional(col);   // → functionalColumns + eqConditions
    } else {
        builder.addEq(col);             // → eqConditions only
    }

    if (metadata instanceof GinColumn gin && isGinOperator(op)) {
        builder.addGin(gin.columnName());
    }
}
```

### What this unifies

| Before (two parallel paths) | After (single resolver path) |
|---|---|
| `FieldExpression.isFunctional` boolean | `FunctionalColumn` sealed type |
| `transformFunction` string (mixed semantics) | `FunctionalColumn.transformFunction` (B-tree) or `GinColumn.operatorClass` (GIN) |
| `aggregateQueryFamily.ginConditions` (separate set) | `GinColumn` metadata → same `AggregateQueryFamily.ginConditions` |
| Static `TRANSFORM_FUNCTIONS` set in `AstIndexAnalyzer` | `IndexResolverRegistry` registration |
| Hardcoded if-else in `extractFieldExpression()` | `IndexResolverRegistry.resolve()` dispatch |

### Extensibility example

Adding support for `extractYear` requires no analyzer changes — just register a resolver:

```java
registry.register("extractYear", node -> {
    var inner = registry.resolve(node.children().get(0));
    if (inner.isIndexable()) {
        return new FunctionalColumn(inner.columnName(), "extractYear");
    }
    return new NonIndexable(node.toString());
});
```

The same for a custom GIN expression:

```java
registry.register("my_gin_func", node -> {
    var inner = registry.resolve(node.children().get(0));
    if (inner.columnName().isPresent()) {
        return new GinColumn(inner.columnName().get(), "jsonb_ops");
    }
    return new NonIndexable(node.toString());
});
```
