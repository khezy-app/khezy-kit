# Dynamic Index Creation from AST Nodes — Core Principles

## Overview

This document describes the design principles behind Marble's system for automatically deriving database indexes from AST-based rule expressions. The system analyzes aggregation nodes in rule ASTs, extracts access patterns, and generates optimal database indexes — without any manual DDL management.

## 1. The Core Idea: Indexes from Expressions

Rule engines frequently evaluate expressions that involve aggregations with filters over database tables.

Example rule expression (simplified):
```
Aggregator(tableName: "transactions", fieldName: "amount", filters: [
  Filter(fieldName: "user_id", operator: "=", value: ...),
  Filter(fieldName: "created_at", operator: ">", value: ...)
])
```

This means: "sum the amount in transactions where user_id = X and created_at > Y".

Without proper indexes, this query is expensive. The engine's insight: **the AST already describes the access pattern needed for an optimal index**.

## 2. Pipeline: AST → Query Families → Index Families → Concrete Indexes

```
┌─────────┐     ┌──────────────────────┐     ┌──────────────┐     ┌────────────────┐
│ AST     │ ──▶ │ AggregateQueryFamily  │ ──▶ │ IndexFamily  │ ──▶ │ ConcreteIndex  │
│ Node    │     │                      │     │              │     │                │
└─────────┘     └──────────────────────┘     └──────────────┘     └────────────────┘
```

### Stage 1: Extract Query Families from AST

Walk the AST tree recursively. When an aggregation node is found, classify its filters by operator kind:
- **Equality conditions** (`=`) — exact match, best for B-tree prefix
- **Inequality/range conditions** (`<`, `<=`, `>`, `>=`) — range scan, must be last in indexed columns
- **Other conditions** (IsInList, FuzzyMatch, StartsWith, etc.) — cannot be efficiently indexed, go to included columns

### Stage 2: Query Families → Index Families

For each query family, generate one or more index family templates:

- Equality columns go into the index's flexible set (can be in any order).
- Inequality columns each produce a separate index family because a B-tree can only range-scan one column.
- Other columns go into the index's "included" set (cover-only, not used for search).

### Stage 3: Index Families → Concrete Indexes

- Compare against existing indexes. Skip if already covered.
- Merge overlapping families where possible to minimize the number of indexes.
- Project each family to an ordered concrete index.
- Generate a deterministic name with a random suffix to avoid collisions.

## 3. B-tree Index Fundamentals

The entire design is built on how B-tree indexes work in relational databases (PostgreSQL in Marble's case). This section explains the key concepts you need to understand before diving into the IndexFamily model.

### 3.1. Anatomy of a Composite B-tree Index

A composite B-tree index stores rows sorted by the combined values of its columns. The **order of columns in the index definition** determines what query patterns the index can efficiently serve.

Consider this index:

```sql
CREATE INDEX idx_example ON transactions (user_id, status, created_at) INCLUDE (amount);
```

The index stores rows sorted by `(user_id, status, created_at)`. Visually:

```
B-tree root
 │
 ├── (user_id='aaa', status='active',   created_at='2024-01-01')  → leaf (amount=50)
 ├── (user_id='aaa', status='active',   created_at='2024-03-15')  → leaf (amount=200)
 ├── (user_id='aaa', status='pending',  created_at='2024-02-01')  → leaf (amount=75)
 ├── (user_id='bbb', status='active',   created_at='2024-01-10')  → leaf (amount=150)
 ├── (user_id='bbb', status='active',   created_at='2024-04-20')  → leaf (amount=300)
 └── (user_id='bbb', status='blocked',  created_at='2024-05-01')  → leaf (amount=25)
```

### 3.2. What "Prefix" Means

The **prefix** is the set of leftmost columns used for equality lookups. The B-tree navigates directly to the exact value — an O(log n) tree traversal.

**Example query:**
```sql
SELECT SUM(amount) FROM transactions
WHERE user_id = 'bbb' AND status = 'active' AND created_at > '2024-03-01';
```

**With the index `(user_id, status, created_at)`:**

```
Step 1: Navigate to (user_id='bbb', status='active')
        ↓  B-tree directly jumps to this point (equality prefix)
Step 2: Scan forward from created_at > '2024-03-01'
        ↓  Sequential scan from this point
```

- `user_id` and `status` form the **equality prefix** — the database finds exactly this position instantly.
- `created_at` is the **range scan column** — the database scans forward (sequential, fast).
- `amount` is **INCLUDEd** — stored in the leaf node, so no separate table lookup needed.

### 3.3. What "Last Indexed Column" Means

The **last indexed column** is the column that comes immediately after all equality-prefix columns. It is used for range scans (`>`, `<`, `>=`, `<=`). 

**Critical constraint**: A B-tree index can only use **one** column for range scanning. Any additional columns after the range column cannot participate in index-based filtering.

**What happens with multiple range conditions:**

```sql
WHERE user_id = 'bbb' AND status = 'active'
  AND created_at > '2024-03-01' AND amount < 1000
```

With a single index `(user_id, status, created_at, amount)`:

```
Equality prefix: (user_id='bbb', status='active')
Range scan:      created_at > '2024-03-01'   ✅ efficiently used
Next column:     amount < 1000               ❌ cannot be used for range
```

The database would need to **post-filter** all matching rows for `amount < 1000`. The index cannot navigate directly to the correct range because `amount` comes after `created_at`, which is already doing a range scan — rows aren't sorted by `amount` within the `created_at > X` portion.

### 3.4. INCLUDE Columns

The `INCLUDE` clause adds columns to the leaf nodes of the index without including them in the sort order. This enables **index-only scans** — the database can read the value directly from the index without fetching the table row.

```sql
CREATE INDEX idx ON transactions (user_id, status, created_at) INCLUDE (amount);
```

When querying `SUM(amount)` with filters on `(user_id, status, created_at)`, the database reads `amount` directly from the index leaf — no table access needed.

### 3.5. Why Separate Indexes Per Inequality Column

Since a B-tree can only range-scan one column, **each inequality column needs its own index** with that column as the last indexed position.

**Example with two range columns — `created_at > X` and `amount < Y`:**

| Index | Equals Prefix | Range Column | Included |
|---|---|---|---|
| `idx_1` | `(user_id, status)` | `created_at` | `amount` |
| `idx_2` | `(user_id, status)` | `amount` | `created_at` |

**Index 1** — optimizes for `created_at > X`:
```sql
CREATE INDEX idx_transactions_1 ON transactions (user_id, status, created_at) INCLUDE (amount);

-- Efficient for:
WHERE user_id = 'abc' AND status = 'active' AND created_at > '2024-01-01'
```

**Index 2** — optimizes for `amount < Y`:
```sql
CREATE INDEX idx_transactions_2 ON transactions (user_id, status, amount) INCLUDE (created_at);

-- Efficient for:
WHERE user_id = 'abc' AND status = 'active' AND amount < 500
```

### 3.6. Full Walkthrough: From AST Node to CREATE INDEX

**AST node (simplified):**
```
Aggregator(tableName: "transactions", fieldName: "amount", filters: [
  Filter(fieldName: "user_id",    operator: "="),
  Filter(fieldName: "status",     operator: "="),
  Filter(fieldName: "created_at", operator: ">"),
])
```

**Step 1 — Classify filters:**
| Column | Operator | Category |
|---|---|---|
| `user_id` | `=` | Equality (goes to prefix) |
| `status` | `=` | Equality (goes to prefix) |
| `created_at` | `>` | Inequality (goes to range/last) |
| `amount` | — | Aggregated field (goes to included) |

**Step 2 — Build IndexFamily:**
```
Fixed: []                    (no forced prefix order)
Flex:  {user_id, status}     (equality columns)
Last:  created_at            (range column — alone, so only one family needed)
Included: {amount}           (aggregated field + non-indexable filters)
```

**Step 3 — Produce ConcreteIndex and SQL:**
```
Indexed:  [user_id, status, created_at]
Included: [amount]
```

```sql
CREATE INDEX idx_transactions_user_id_status_created_at_a1b2c3
ON transactions (user_id, status, created_at)
INCLUDE (amount);
```

### 3.7. Why Understanding This Matters

The pipeline (Stages 1–3) and the IndexFamily model in the next section are a direct translation of these database mechanics into a structured data model:

| Database Concept | IndexFamily Field |
|---|---|
| Equality-prefix columns (first N in index) | `Fixed` or `Flex` |
| Range-scan column (one per index) | `Last` |
| Non-searchable / aggregated columns | `Included` |
| Equality columns with no fixed order | `Flex` (unordered set) |
| Equality columns with enforced order | `Fixed` (ordered slice) |

The `Flex` vs `Fixed` distinction is needed because some index families have a forced column order (from the refinement algorithm merging results), while newly created families from a single query have no ordering constraints — any permutation of the equality columns works equally well.

## 4. The IndexFamily Model

An `IndexFamily` is a four-part template that bridges logical query requirements and physical index structure:

| Component | Purpose | Example |
|---|---|---|
| **Fixed** | Ordered prefix columns (must appear first, in exact order) | `[user_id, status]` |
| **Flex** | Unordered set of indexed columns (can appear in any order) | `{category, region}` |
| **Last** | A single range/inequality column | `created_at` |
| **Included** | Cover-only columns (not used for search, but stored in index) | `{amount, description}` |

This model is important because B-tree indexes have strict ordering requirements:
- Columns used for equality lookups should come first.
- Only one column can be used for range scans.
- Additional columns can be included for index-only scans.

## 5. The Refinement Algorithm: Minimization

When multiple index families are required across different rules, the engine tries to merge them into fewer, more general indexes. This is a key optimization that prevents an explosion of indexes.

The merge rules:

1. **Common prefix handling**: If two families share a Fixed prefix, strip it, try to merge, then prepend it back.

2. **Subset merging**: If one family's columns are a subset of another's, the larger index can cover both.

3. **Size equality merging**: If both families index the same columns (possibly in different order), merge by choosing a canonical order and combining Included columns.

This algorithm ensures the system creates the **minimum number of indexes** needed to satisfy all rule expressions.

## 6. Coverage Checking

Before creating any index, the system checks whether existing indexes already cover the required access pattern. The `Covers()` function checks:

1. **Table name** must match.
2. **Fixed prefix** in the index family must match the first N columns of the existing index.
3. **Flex columns** must appear contiguously after the fixed prefix.
4. **Last column** (if any) must be in the correct position.
5. **Included columns** must be a subset of the existing index's included columns.

This prevents redundant index creation.

## 7. Design Principles

### Principle 1: Derive from Structure, Not Configuration

The index requirements are derived entirely from the AST structure — no manual index configuration needed. The rule expression itself encodes the access pattern.

### Principle 2: Minimize Without Loss

The minimization algorithm ensures the system never creates more indexes than necessary, but never drops a required access pattern.

### Principle 3: Separate Concerns

- The AST describes what queries are needed.
- The query family captures the logical access pattern.
- The index family models the physical index requirements.
- The concrete index is the actual DDL statement.

Each layer adds a different concern without mixing them.

### Principle 4: Idempotent and Incremental

The system checks existing indexes before creating new ones. Publishing the same scenario twice does not create redundant indexes.

### Principle 5: Handle Inequality Correctly

A single B-tree index can only use one range scan column. The system correctly handles this by generating one index family per inequality column, ensuring each index can efficiently serve its query.

## 8. Relevance for a Java Rule Engine

For a reusable Java core library, this design shows that:

- **AST-driven index management** is feasible and powerful.
- The pipeline from expressions to physical indexes can be generic if the AST model supports function identification (like `FUNC_AGGREGATOR`) and filter operators.
- The IndexFamily model (Fixed/Flex/Last/Included) is a clean abstraction that maps well to relational database index design.
- The minimization algorithm is reusable across databases.

A Java implementation would need:
- An AST with typed function nodes (like `AggregationFunction`, `FilterFunction`).
- A visitor/pattern-matching system to walk the AST and extract filter patterns.
- A metadata layer to know about existing indexes.
- A DDL generation layer for the target database.
