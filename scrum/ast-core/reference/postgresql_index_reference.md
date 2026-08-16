# PostgreSQL Index Reference

> Based on PostgreSQL official documentation (Chapter 11 — Indexes).
> PostgreSQL version: 18 (current stable at time of writing).

---

## Table of Contents

1. [Introduction to Indexes](#1-introduction-to-indexes)
2. [B-tree Index](#2-b-tree-index)
3. [Hash Index](#3-hash-index)
4. [GiST Index](#4-gist-index)
5. [SP-GiST Index](#5-sp-gist-index)
6. [GIN Index](#6-gin-index)
7. [BRIN Index](#7-brin-index)
8. [Multicolumn Indexes](#8-multicolumn-indexes)
9. [Index-Only Scans and Covering Indexes (INCLUDE)](#9-index-only-scans-and-covering-indexes-include)
10. [Combining Multiple Indexes (Bitmap Scans)](#10-combining-multiple-indexes-bitmap-scans)
11. [Partial Indexes](#11-partial-indexes)
12. [Indexes on Expressions](#12-indexes-on-expressions)
13. [Unique Indexes](#13-unique-indexes)
14. [Quick Reference Table](#14-quick-reference-table)

---

## 1. Introduction to Indexes

### What It Is

An index is a database structure that allows the server to find and retrieve specific rows much faster than scanning the entire table row by row (sequential scan). It works like the index at the back of a book — instead of reading every page, you jump directly to the relevant pages.

PostgreSQL creates and maintains indexes automatically after creation. When the table is modified, the index is updated to stay in sync.

### When PostgreSQL Uses an Index

PostgreSQL uses an index when it encounters a query with:

```
indexed-column indexable-operator comparison-value
```

For example, with an index on `id`:
```sql
SELECT content FROM test1 WHERE id = 42;
```

The query planner decides whether to use an index based on table statistics. Use `ANALYZE` to keep statistics up to date.

### When PostgreSQL Does NOT Use an Index

- When the query would match a large fraction of rows (sequential scan is faster).
- When statistics are stale.
- When the operator is not part of the index's operator class.
- When the table is small (sequential scan overhead is lower).

### Overhead of Indexes

- Indexes add overhead to `INSERT`, `UPDATE`, `DELETE`.
- Indexes can prevent heap-only tuples (HOT) updates.
- Unused indexes should be removed.

---

## 2. B-tree Index

### What It Is

The **default index type** in PostgreSQL. B-trees handle equality and range queries on data that can be sorted into some ordering. It's a balanced tree structure where:
- Internal nodes guide searches (navigate to the correct leaf).
- Leaf nodes contain the actual index entries (sorted).
- Leaf nodes are linked for efficient sequential scanning.

### Supported Operators

| Operator | Example | Can Use B-tree Index? |
|---|---|---|
| `=` | `WHERE id = 42` | ✅ Yes |
| `<` | `WHERE age < 18` | ✅ Yes |
| `<=` | `WHERE price <= 100` | ✅ Yes |
| `>` | `WHERE created_at > '2024-01-01'` | ✅ Yes |
| `>=` | `WHERE score >= 90` | ✅ Yes |
| `BETWEEN` | `WHERE age BETWEEN 18 AND 65` | ✅ Yes (expands to `>=` and `<=`) |
| `IN` | `WHERE status IN ('a','b','c')` | ✅ Yes (expands to multiple `=`) |
| `IS NULL` | `WHERE deleted_at IS NULL` | ✅ Yes |
| `IS NOT NULL` | `WHERE email IS NOT NULL` | ✅ Yes |
| `LIKE 'foo%'` | `WHERE name LIKE 'Joh%'` | ✅ Yes (anchored prefix) |
| `~ '^foo'` | `WHERE name ~ '^Joh'` | ✅ Yes (anchored prefix regex) |
| `LIKE '%bar'` | `WHERE name LIKE '%son'` | ❌ No (non-anchored) |
| `ILIKE 'foo%'` | `WHERE name ILIKE 'joh%'` | ✅ Only if pattern starts with non-alphabetic chars |
| `<>` / `!=` | `WHERE status != 'active'` | ❌ Typically not (bitmap scan may combine) |

### How It Works

**B-tree structure:**
```
                        [Root]
                     /    |    \
                 /        |        \
           [Internal]  [Internal]  [Internal]
          /    |    \   /    |   \  /    |    \
        [L1]  [L2] [L3] [L4] [L5] ... leaf nodes (linked list)
```

1. Start at the root. Navigate down through internal nodes using comparisons.
2. Arrive at the correct leaf node.
3. Scan leaf nodes (linearly, since they're linked) for matching entries.
4. Fetch the corresponding heap (table) rows.

### When PostgreSQL Uses a B-tree Index

- **Equality**: `WHERE col = value` — O(log n) lookup.
- **Range**: `WHERE col > value` — find start point, scan forward.
- **Sorting**: `ORDER BY col` — avoid explicit sort, read index in order.
- **MIN/MAX**: Fast — take first/last leaf entry.
- **Prefix matching**: `LIKE 'abc%'` — same as range scan on `'abc'` to `'abd'`.
- **Join conditions**: Index on join column speeds up nested loop joins.

### When PostgreSQL Does NOT Use a B-tree Index

- **Large fraction of rows**: If `>` 5-10% of rows match, sequential scan is usually faster.
- **Non-anchored patterns**: `LIKE '%text'` or `~ 'text$'` cannot use standard B-tree.
- **Non-sargable expressions**: `WHERE function(col) = value` without an expression index.

### Example Queries

```sql
-- Setup
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    status TEXT NOT NULL,
    total_amount DECIMAL(10,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_created_at ON orders (created_at);

-- ✅ Uses index: range query
SELECT * FROM orders WHERE created_at > '2024-06-01';

-- ✅ Uses index: equality
SELECT * FROM orders WHERE user_id = 42;

-- ✅ Uses index: sorting (avoids explicit sort)
SELECT * FROM orders ORDER BY created_at DESC LIMIT 10;

-- ❌ Does NOT use index (non-anchored pattern)
SELECT * FROM orders WHERE status LIKE '%pending%';

-- ❌ Sequential scan preferred whens matching too many rows
SELECT * FROM orders WHERE created_at > '2020-01-01';  -- might skip index
```

---

## 3. Hash Index

### What It Is

A hash index stores a 32-bit hash code derived from the indexed column's value. It only supports simple equality comparisons (`=`).

### How It Works

For each row, PostgreSQL computes `hash(column_value)` and stores the hash in the index. When querying:

```
WHERE col = 'abc'   →   compute hash('abc')   →   search index for that hash
```

Hash collisions are handled (multiple values with the same hash are stored together).

### Supported Operators

| Operator | Can Use Hash Index? |
|---|---|
| `=` | ✅ Yes |
| `<`, `>`, `<=`, `>=`, `BETWEEN` | ❌ No |
| `LIKE`, pattern matching | ❌ No |

### When PostgreSQL Uses a Hash Index

- Simple equality lookups where B-tree would also work but hash is smaller.
- When the indexed column has no meaningful sort order.

### When PostgreSQL Does NOT Use a Hash Index

- Any non-equality comparison.
- Sorting or range queries.
- The planner often prefers B-tree for `=` as well, since B-tree is versatile.

### Example Queries

```sql
CREATE INDEX idx_users_email_hash ON users USING HASH (email);

-- ✅ Uses hash index: simple equality
SELECT * FROM users WHERE email = 'user@example.com';

-- ❌ Cannot use hash index (range query)
SELECT * FROM users WHERE email > 'user@example.com';
```

---

## 4. GiST Index

### What It Is

GiST (Generalized Search Tree) is not a single indexing algorithm — it's an **infrastructure** within which many different indexing strategies can be implemented. It supports:
- Geometric data types (points, polygons, etc.)
- Full-text search (tsvector)
- Range types (int4range, daterange, etc.)
- Nearest-neighbor search (`ORDER BY ... <-> ... LIMIT N`)

### Supported Operators (Geometric Example)

| Operator | Meaning | Can Use GiST? |
|---|---|---|
| `<<` | strictly left of | ✅ |
| `&<` | does not extend to the right of | ✅ |
| `>>` | strictly right of | ✅ |
| `@>` | contains | ✅ |
| `<@` | contained by | ✅ |
| `&&` | overlaps | ✅ |
| `~=` | same as | ✅ |
| `<->` | distance (nearest-neighbor) | ✅ |

### How It Works

GiST provides a **balanced tree structure** similar to B-tree, but the comparison operators are user-defined through operator classes. Each operator class defines how to:
- Split a page (partition data).
- Pick the split (choose how to divide entries).
- Formulate a search predicate (consistent, union).

### When PostgreSQL Uses a GiST Index

- Spatial/geometric queries (points, polygons within a bounding box).
- Range containment/overlap queries.
- Full-text search with specialized operator classes.
- **Nearest-neighbor**: `ORDER BY location <-> point '(0,0)' LIMIT 10`.

### When PostgreSQL Does NOT Use a GiST Index

- Simple equality/range on scalar values (B-tree is better).
- When the operator class does not support the needed operator.
- When the first column has very few distinct values (GiST loses effectiveness).

### Example Queries

```sql
-- Geometric example
CREATE TABLE places (
    name TEXT,
    location POINT
);
CREATE INDEX idx_places_location ON places USING GIST (location);

-- ✅ Uses GiST: find points within a bounding box
SELECT * FROM places WHERE location <@ box '(0,0, 100,100)';

-- ✅ Uses GiST: nearest-neighbor search
SELECT * FROM places ORDER BY location <-> point '(50,50)' LIMIT 10;

-- Full-text search example
CREATE INDEX idx_docs_content ON documents USING GIST (to_tsvector('english', content));

-- ✅ Uses GiST: full-text search (with appropriate operator class)
SELECT * FROM documents WHERE to_tsvector('english', content) @@ to_tsquery('english', 'database & index');
```

---

## 5. SP-GiST Index

### What It Is

SP-GiST (Space-Partitioned Generalized Search Tree) supports **non-balanced disk-based data structures** such as:
- Quadtrees (2D points)
- k-d trees (multi-dimensional data)
- Radix trees / tries (string prefix search)

Unlike GiST (balanced tree), SP-GiST is designed for **space-partitioning** where the search space is divided non-overlappingly.

### Supported Operators (Point Example)

| Operator | Meaning | Can Use SP-GiST? |
|---|---|---|
| `<<` | strictly left of | ✅ |
| `>>` | strictly right of | ✅ |
| `~=` | same as | ✅ |
| `<@` | contained by | ✅ |
| `<->` | distance (nearest-neighbor) | ✅ |

### How It Works

SP-GiST partitions the search space into regions that do not overlap. For example, a quadtree divides a 2D space into four quadrants, then subdivides each quadrant recursively. This allows very efficient point-lookup and spatial queries.

### When PostgreSQL Uses an SP-GiST Index

- **Point data**: Lookup, containment, nearest-neighbor.
- **String prefix search**: Using radix tree (trie).
- **Spatial partitioning**: When data naturally partitions into non-overlapping regions.

### When PostgreSQL Does NOT Use an SP-GiST Index

- General equality/range queries on scalar types (use B-tree).
- When GiST is more appropriate (overlap queries like `&&` on polygons — GiST typically handles these better).

### Example Queries

```sql
-- Setup
CREATE TABLE cities (
    name TEXT,
    location POINT
);
CREATE INDEX idx_cities_location ON cities USING SPGIST (location);

-- ✅ Uses SP-GiST: find point at exact location
SELECT * FROM cities WHERE location ~= point '(48.8566, 2.3522)';

-- ✅ Uses SP-GiST: nearest-neighbor
SELECT * FROM cities ORDER BY location <-> point '(48.8566, 2.3522)' LIMIT 5;
```

---

## 6. GIN Index

### What It Is

GIN (Generalized Inverted Index) is designed for data values that contain **multiple component values**. It's an **inverted index** — for each component value, it stores a list of rows that contain that component.

### Common Use Cases

| Data Type | Example | Indexes |
|---|---|---|
| Arrays | `TEXT[]` | `@>`, `<@`, `=`, `&&` |
| JSONB | `JSONB` | `@>`, `?`, `?|`, `?&` |
| Full-text | `tsvector` | `@@` |

### Supported Operators (Array Example)

```sql
CREATE INDEX idx_products_tags ON products USING GIN (tags);

-- @>  : contains
-- <@  : contained by
-- =   : equal
-- &&  : overlaps (share any element)
```

### How It Works

GIN stores a **separate entry for each component value**. For example, for an array column with value `'{a, b, c}'`, GIN creates three entries: one for `a`, one for `b`, one for `c`. Each entry points to the row's location.

This is called an inverted index because you look up by component value to find rows, rather than by row to find components.

### When PostgreSQL Uses a GIN Index

- **Array containment**: `WHERE tags @> ARRAY['urgent']`.
- **JSONB path queries**: `WHERE data @> '{"status": "active"}'`.
- **JSONB key existence**: `WHERE data ? 'email'`.
- **Full-text search**: `WHERE to_tsvector('english', content) @@ to_tsquery('english', 'cat & dog')`.

### When PostgreSQL Does NOT Use a GIN Index

- **Non-composite values**: B-tree or hash is more efficient for simple scalar `=` or range queries.
- **Queries needing sorting**: GIN does not store ordering information.
- **Index-only scans**: GIN does NOT support index-only scans (each index entry holds only part of the original data value).
- **Queries needing range scans**: GIN cannot do `>`, `<`, `BETWEEN`.

### Unique: Search Effectiveness Independent of Column Order

Unlike B-tree and GiST, multicolumn GIN indexes have the **same search effectiveness regardless of which column(s) the query uses**. The first column has no special priority.

### Example Queries

```sql
-- Array example
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name TEXT,
    tags TEXT[]
);
CREATE INDEX idx_products_tags ON products USING GIN (tags);

-- ✅ Uses GIN: contains query
SELECT * FROM products WHERE tags @> ARRAY['electronics', 'sale'];

-- ✅ Uses GIN: overlap query
SELECT * FROM products WHERE tags && ARRAY['urgent', 'new'];

-- JSONB example
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    data JSONB
);
CREATE INDEX idx_events_data ON events USING GIN (data);

-- ✅ Uses GIN: JSON containment
SELECT * FROM events WHERE data @> '{"type": "purchase", "amount": 100}';

-- ✅ Uses GIN: key existence
SELECT * FROM events WHERE data ? 'user_email';

-- ❌ Cannot use GIN: range condition on JSONB value
SELECT * FROM events WHERE (data->>'amount')::int > 50;  -- needs B-tree
```

---

## 7. BRIN Index

### What It Is

BRIN (Block Range INdex) stores **summaries about the values** stored in consecutive physical block ranges of a table. For data types with linear sort order, it stores the min and max value per block range.

### How It Works

A table's heap is divided into **block ranges** (default 128 blocks per range, configurable via `pages_per_range`). For each block range, BRIN stores:
- The minimum value in that range.
- The maximum value in that range.

```
Block Range 0 (blocks 0-127):  min=2024-01-01, max=2024-01-31
Block Range 1 (blocks 128-255): min=2024-02-01, max=2024-02-28
Block Range 2 (blocks 256-383): min=2024-03-01, max=2024-04-15
...
```

When querying `WHERE created_at = '2024-02-15'`, the index checks each block range's min/max. If `2024-02-15` falls within that range → scan those blocks. If not → skip the entire block range.

### Supported Operators

| Operator | Example | Can Use BRIN? |
|---|---|---|
| `=` | `WHERE date = '2024-06-01'` | ✅ Yes |
| `<` | `WHERE date < '2024-06-01'` | ✅ Yes |
| `>` | `WHERE date > '2024-06-01'` | ✅ Yes |
| `BETWEEN` | `WHERE date BETWEEN '2024-01-01' AND '2024-06-01'` | ✅ Yes |

### When PostgreSQL Uses a BRIN Index

- **Time-series data**: Created_at timestamps are correlated with physical insertion order.
- **Large tables** where index size matters (BRIN is tiny compared to B-tree).
- **Sequentially inserted monotonically increasing columns** (e.g., auto-increment IDs, log timestamps).
- **Queries that scan ranges**: BRIN excels at identifying which block ranges to skip.

### When PostgreSQL Does NOT Use a BRIN Index

- **Randomly distributed values**: If values are not correlated with physical order, BRIN is useless (every block range will contain the full value range, so no skipping is possible).
- **Point lookups**: B-tree is faster for finding single rows.
- **Small tables**: BRIN overhead isn't justified.

### BRIN vs B-tree Size Comparison

For a table with 1 billion rows:

| Index Type | Approximate Size |
|---|---|
| B-tree | ~30 GB |
| BRIN (pages_per_range=128) | ~10 MB |

BRIN can be **3000x smaller** because it stores summaries, not individual entries.

### Example Queries

```sql
-- Setup: time-series data
CREATE TABLE logs (
    id BIGSERIAL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    message TEXT,
    level TEXT
);
CREATE INDEX idx_logs_created_at_brin ON logs USING BRIN (created_at);

-- ✅ Uses BRIN: efficiently narrows block ranges to scan
SELECT * FROM logs WHERE created_at BETWEEN '2024-06-01' AND '2024-06-02';

-- ⚠️ Less efficient but still usable: BRIN supports = as well
SELECT * FROM logs WHERE created_at = '2024-06-15 12:00:00';

-- ❌ BRIN not useful here (values not correlated with insertion order)
CREATE INDEX idx_logs_level_brin ON logs USING BRIN (level);
-- Levels like 'ERROR', 'INFO' are randomly distributed, BRIN can't skip blocks
```

---

## 8. Multicolumn Indexes

### What It Is

An index defined on more than one column. Currently supported by B-tree, GiST, GIN, and BRIN. Up to 32 columns (including `INCLUDE` columns).

### How It Works (B-tree)

A multicolumn B-tree index sorts rows by the **combined** values of all indexed columns:

```sql
CREATE INDEX idx_orders_user_status ON orders (user_id, status, created_at);
```

Rows are sorted by `user_id`, then by `status` within same `user_id`, then by `created_at` within same `(user_id, status)`.

### Leading Column Rule

The index can be used with any subset of its columns, but it's most efficient when there are constraints on the **leading (leftmost) columns**.

**The exact rule**: Equality constraints on leading columns + inequality on the first column without an equality constraint = most efficient scan.

### Skip Scan Optimization (PostgreSQL 17+)

If a leading column is missing from the query, PostgreSQL may apply **skip scan**: it generates dynamic equality constraints internally, searching for each distinct value of the missing column. This is only efficient when there are **few distinct values** in that column.

Example:
```sql
CREATE INDEX ON orders (user_id, status);
-- Query without user_id:
SELECT * FROM orders WHERE status = 'active';
-- Skip scan: internally does WHERE user_id = 1 AND status = 'active'
--                        UNION  WHERE user_id = 2 AND status = 'active' ...
-- Only efficient if there are few distinct user_ids.
```

### B-tree Column Order Comparison

```sql
-- Index A: leading column has equality constraint
CREATE INDEX idx_a ON orders (user_id, created_at);
-- WHERE user_id = 42 AND created_at > '2024-06-01'
--     ✅ equality on user_id (leading)
--     ✅ range on created_at (second)
-- → Very efficient

-- Index B: leading column has no constraint
CREATE INDEX idx_b ON orders (created_at, user_id);
-- WHERE user_id = 42 AND created_at > '2024-06-01'
--     ⚠️ range on created_at (leading)
--     ❌ user_id cannot narrow the scan (comes after range)
-- Must scan all created_at > '2024-06-01' rows, then filter by user_id
```

### GiST Multicolumn Behavior

GiST is different from B-tree: the **first column** is the most important for determining how much of the index needs to be scanned. If the first column has few distinct values, the index is relatively ineffective even if additional columns have many distinct values.

### GIN and BRIN Multicolumn Behavior

Unlike B-tree and GiST, GIN and BRIN have the **same search effectiveness regardless of which column(s) the query uses**. No column has priority over others.

### Example Queries

```sql
-- Setup
CREATE TABLE transactions (
    user_id INT,
    category TEXT,
    amount DECIMAL(10,2),
    created_at DATE
);
-- Optimal index: equality columns first, range column last
CREATE INDEX idx_transactions_opt ON transactions (user_id, category, created_at);

-- ✅ Highly efficient: equality on user_id AND category, range on created_at
SELECT * FROM transactions
WHERE user_id = 42 AND category = 'food' AND created_at > '2024-01-01';

-- ✅ Efficient: equality on leading column, range on second
SELECT * FROM transactions
WHERE user_id = 42 AND created_at > '2024-06-01';

-- ⚠️ Less efficient: no constraint on leading column
-- May use skip scan if user_id has few distinct values
SELECT * FROM transactions
WHERE category = 'food' AND created_at > '2024-06-01';

-- ❌ Does NOT limit scanned portion: range on leading column
SELECT * FROM transactions
WHERE created_at > '2024-06-01' AND user_id = 42;
-- Must scan ALL rows with created_at > '2024-06-01', then filter user_id
```

---

## 9. Index-Only Scans and Covering Indexes (INCLUDE)

### What It Is

An **index-only scan** answers a query using only the index, without accessing the heap (table). This is much faster because:
- The index is typically smaller than the table.
- Index entries are more compact.
- Avoids random I/O to the heap.

A **covering index** is explicitly designed for index-only scans by including extra columns via the `INCLUDE` clause.

### How It Works

```sql
CREATE INDEX idx_covering ON orders (user_id) INCLUDE (total_amount, status);
```

- `user_id` is a **key column** — used for search and ordering.
- `total_amount` and `status` are **payload columns** — stored only in leaf nodes, NOT used for search.
- Payload columns are **suffix-truncated** from upper B-tree levels (saves space).

**Visibility requirement**: Even with an index-only scan, PostgreSQL must verify that rows are visible according to MVCC. It checks the **visibility map** (a compact bitmap tracking which heap pages contain only visible-to-all rows). If the visibility map bit is set for a page, the heap page is skipped entirely.

### Which Index Types Support Index-Only Scans

| Index Type | Supports Index-Only Scans? |
|---|---|
| B-tree | ✅ Yes (always) |
| GiST | ✅ For some operator classes |
| SP-GiST | ✅ For some operator classes |
| GIN | ❌ No |
| BRIN | ❌ No |
| Hash | ❌ No |

### When PostgreSQL Uses an Index-Only Scan

- All required columns are present in the index (key + INCLUDE).
- The visibility map indicates most pages are all-visible.

### When PostgreSQL Does NOT Use an Index-Only Scan

- A needed column is not in the index.
- Table pages are not all-visible (heap visit required anyway).
- The index type doesn't support it (GIN, BRIN, Hash).

### INCLUDE vs Adding Columns to the Index Key

```sql
-- Option A: INCLUDE (recommended for payload columns)
CREATE INDEX idx_a ON orders (user_id) INCLUDE (total_amount);

-- Option B: Add to key (works but has drawbacks)
CREATE INDEX idx_b ON orders (user_id, total_amount);
```

**Why use INCLUDE instead of adding to the key?**

| Aspect | INCLUDE | Key Column |
|---|---|---|
| Used for search? | ❌ No | ✅ Yes |
| Stored in leaf nodes? | ✅ Yes | ✅ Yes |
| Stored in internal nodes? | ❌ No (suffix-truncated) | ✅ Yes |
| Affects uniqueness? | ❌ No | ✅ Yes |
| Affects sort order? | ❌ No | ✅ Yes |

### Example Queries

```sql
-- Setup
CREATE INDEX idx_covering ON orders (user_id) INCLUDE (total_amount, status);

-- ✅ Index-only scan: all needed columns are in the index
SELECT user_id, total_amount FROM orders WHERE user_id = 42;
-- Reads: index only (user_id, total_amount both in index)
-- Skips: heap entirely (if visibility map says pages are all-visible)

-- ✅ Index-only scan: status is in INCLUDE
SELECT user_id, status FROM orders WHERE user_id = 42;

-- ❌ Cannot use index-only scan: 'id' column not in index
SELECT id, user_id FROM orders WHERE user_id = 42;
-- Must access heap to get 'id' (unless id is also in index or INCLUDE)

-- ❌ Cannot use index-only scan: expression not understood by planner
CREATE INDEX idx_expr ON orders (user_id) INCLUDE (total_amount);
SELECT user_id, total_amount * 1.1 AS taxed FROM orders WHERE user_id = 42;
-- Planner doesn't recognize that total_amount * 1.1 can come from total_amount
```

---

## 10. Combining Multiple Indexes (Bitmap Scans)

### What It Is

When a single index cannot satisfy a query, PostgreSQL can combine **multiple indexes** (including multiple uses of the same index) using **bitmap scans**. This handles cases that cannot be implemented by single index scans.

### How It Works

PostgreSQL supports combining indexes for:
- **AND conditions**: Scan each index, create a bitmap of matching row locations, AND the bitmaps together, then fetch rows.
- **OR conditions**: Scan each index, OR the bitmaps together, then fetch rows.

**Process:**
1. Scan each relevant index.
2. Build a bitmap in memory showing which heap pages/rows match.
3. AND/OR the bitmaps as needed.
4. Visit the actual rows in **physical order** (bitmap order, not index order).

### Trade-offs

| Advantage | Disadvantage |
|---|---|
| Can combine separate indexes | Loses sort order (separate sort step needed for ORDER BY) |
| More flexible than multicolumn indexes | Each additional index scan adds time |
| Good for mixed query patterns | Bitmap construction uses memory |

### When to Use Separate Indexes vs Multicolumn Index

| Scenario | Recommended Approach |
|---|---|
| Queries always filter by both `x` and `y` | Single multicolumn index on `(x, y)` |
| Queries sometimes filter by `x`, sometimes by `y`, sometimes both | Two separate indexes on `x` and `y` (bitmap combine) |
| Queries filter by `x` only, or `x + y` | Index on `(x, y)` is sufficient (prefix can serve `x` alone) |
| Queries filter by `y` only, `y` has few distinct values | Skip scan on `(x, y)` might work; else separate index on `y` |

### Example Queries

```sql
-- Setup: separate indexes on different columns
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    author_id INT,
    category_id INT,
    status TEXT,
    created_at TIMESTAMPTZ
);
CREATE INDEX idx_docs_author ON documents (author_id);
CREATE INDEX idx_docs_category ON documents (category_id);

-- ✅ Bitmap AND: combines both indexes
SELECT * FROM documents
WHERE author_id = 42 AND category_id = 5;

-- How it works:
-- 1. Scan idx_docs_author for author_id = 42 → bitmap A
-- 2. Scan idx_docs_category for category_id = 5 → bitmap B
-- 3. Bitmap AND (A & B) → rows matching both conditions
-- 4. Fetch rows in physical order

-- ✅ Bitmap OR: combines both indexes
SELECT * FROM documents
WHERE author_id = 42 OR category_id = 5;

-- How it works:
-- 1. Scan idx_docs_author for author_id = 42 → bitmap A
-- 2. Scan idx_docs_category for category_id = 5 → bitmap B
-- 3. Bitmap OR (A | B) → rows matching either condition
-- 4. Fetch rows in physical order

-- ⚠️ Loses ordering: ORDER BY requires extra sort step
SELECT * FROM documents
WHERE author_id = 42 OR category_id = 5
ORDER BY created_at;  -- extra sort step needed
```

---

## 11. Partial Indexes

### What It Is

A partial index is built over **a subset of a table's rows**, defined by a conditional expression (the predicate). The index contains entries only for rows that satisfy the predicate.

```sql
CREATE INDEX idx_unbilled ON orders (order_nr) WHERE billed IS NOT TRUE;
```

### How It Works

When the query's `WHERE` condition **mathematically implies** the index's predicate, PostgreSQL can use the partial index. The index is smaller and faster because it only contains relevant rows.

**Matching rule**: The system can recognize simple inequality implications (e.g., `x < 1` implies `x < 2`). Otherwise, the predicate condition must **exactly match** part of the query's `WHERE` condition.

### Three Main Uses

1. **Exclude common values**: Avoid indexing values that account for > a few percent of rows.
2. **Exclude uninteresting values**: Index only the subset of rows that queries actually search.
3. **Conditional uniqueness**: Enforce uniqueness only for rows satisfying the predicate.

### When PostgreSQL Uses a Partial Index

- The query's `WHERE` mathematically implies the index's predicate.
- The query returns fewer rows than a full scan would (partial index is smaller).

### When PostgreSQL Does NOT Use a Partial Index

- The query's predicate does not imply the index predicate.
- The query uses parameterized clauses (matching is at planning time, not runtime).
- The index would exclude rows the query needs.

### Example Queries

```sql
-- Example 1: Exclude common values (web server logs)
CREATE TABLE access_log (
    url TEXT,
    client_ip INET,
    accessed_at TIMESTAMPTZ
);
-- Most traffic is internal (192.168.x.x), index only external IPs
CREATE INDEX idx_access_log_external
ON access_log (client_ip)
WHERE NOT (client_ip > inet '192.168.100.0'
           AND client_ip < inet '192.168.100.255');

-- ✅ Uses partial index: external IP
SELECT * FROM access_log WHERE client_ip = inet '212.78.10.32';

-- ❌ Does NOT use partial index: internal IP is excluded from index
SELECT * FROM access_log WHERE client_ip = inet '192.168.100.23';

-- Example 2: Conditional uniqueness
CREATE TABLE tests (
    subject TEXT,
    target TEXT,
    success BOOLEAN
);
-- Only one successful entry per subject+target
CREATE UNIQUE INDEX tests_success_constraint
ON tests (subject, target) WHERE success;

-- ✅ Uses partial unique index: enforces uniqueness only for successful tests
INSERT INTO tests VALUES ('math', 'final', true);   -- OK
INSERT INTO tests VALUES ('math', 'final', true);   -- ERROR: duplicate
INSERT INTO tests VALUES ('math', 'final', false);  -- OK (different subset)
INSERT INTO tests VALUES ('math', 'final', false);  -- OK (different subset)

-- Example 3: Avoid partial index explosion
-- ❌ BAD: many separate partial indexes
CREATE INDEX mytable_cat_1 ON mytable (data) WHERE category = 1;
CREATE INDEX mytable_cat_2 ON mytable (data) WHERE category = 2;
CREATE INDEX mytable_cat_3 ON mytable (data) WHERE category = 3;
-- ... N indexes

-- ✅ BETTER: single multicolumn index
CREATE INDEX mytable_cat_data ON mytable (category, data);
```

---

## 12. Indexes on Expressions

### What It Is

An index based on a **function or expression** rather than a plain column.

```sql
CREATE INDEX idx_lower_email ON users (LOWER(email));
```

### How It Works

The index stores the **result of the expression** for each row. Queries must use the identical expression for the index to be usable.

### When PostgreSQL Uses an Expression Index

- The `WHERE` clause or `ORDER BY` uses the **exact same expression** as the index definition.
- Example: `WHERE LOWER(email) = 'user@example.com'` with an index on `LOWER(email)`.

### When PostgreSQL Does NOT Use an Expression Index

- The query uses a different form of the expression.
- Example: index on `LOWER(email)`, query uses `UPPER(email)`.
- The expression is wrapped in another function or calculation.

### Example Queries

```sql
-- Setup
CREATE INDEX idx_lower_email ON users (LOWER(email));

-- ✅ Uses expression index
SELECT * FROM users WHERE LOWER(email) = 'user@example.com';

-- ❌ Does NOT use expression index (different expression)
SELECT * FROM users WHERE UPPER(email) = 'USER@EXAMPLE.COM';

-- ✅ Expression index for ordering
CREATE INDEX idx_lower_name ON users (LOWER(last_name));
SELECT * FROM users ORDER BY LOWER(last_name);

-- Expression index with INCLUDE for index-only scan workaround
CREATE INDEX idx_expr_with_include ON users (LOWER(email)) INCLUDE (email);
-- Without INCLUDE(email), the planner might not use index-only scan
-- because 'email' is not in the index
```

---

## 13. Unique Indexes

### What It Is

An index that enforces uniqueness of the indexed columns (or column combinations). PostgreSQL automatically creates unique indexes for `PRIMARY KEY` and `UNIQUE` constraints.

```sql
CREATE UNIQUE INDEX idx_unique_email ON users (email);
```

### How It Works

When a new row is inserted or updated, PostgreSQL checks whether any existing row has the same values for the indexed columns. If a duplicate is found, the operation is rejected.

### With INCLUDE Columns

```sql
CREATE UNIQUE INDEX idx_unique_user ON users (user_id) INCLUDE (email);
```

The uniqueness constraint applies **only to the key columns** (`user_id`), not the included columns (`email`). This is useful when you want a unique constraint on one set of columns but include additional columns for index-only scans.

### Example Queries

```sql
-- ✅ Prevents duplicate emails
INSERT INTO users (email, name) VALUES ('user@example.com', 'Alice');
INSERT INTO users (email, name) VALUES ('user@example.com', 'Bob');
-- ERROR: duplicate key value violates unique constraint

-- ✅ Unique index with INCLUDE
CREATE UNIQUE INDEX idx_unique_order ON orders (order_id) INCLUDE (status, total);
-- Unique on order_id, but status and total are available for index-only scans
```

---

## 14. Quick Reference Table

| Index Type | Supported Operators | Multicolumn? | Index-Only Scans? | INCLUDE? | Size | Best For |
|---|---|---|---|---|---|---|
| **B-tree** | `=`, `<`, `<=`, `>`, `>=`, `BETWEEN`, `IN`, `IS NULL`, `LIKE 'x%'` | ✅ Yes | ✅ Yes | ✅ Yes | Medium | General purpose, equality, range, sorting |
| **Hash** | `=` only | ❌ No | ❌ No | ❌ No | Small | Simple equality-only lookups |
| **GiST** | Depends on opclass (geometry, ranges, full-text, nearest-neighbor) | ✅ Yes | ⚠️ Partial | ⚠️ Partial | Medium | Spatial, geometric, range types, nearest-neighbor |
| **SP-GiST** | Depends on opclass (points, strings) | ❌ No | ⚠️ Partial | ❌ No | Medium | Quadtrees, k-d trees, tries, prefix search |
| **GIN** | Array: `@>`, `<@`, `&&`, `=`. JSONB: `@>`, `?`, `?|`, `?&`. Text: `@@` | ✅ Yes | ❌ No | ❌ No | Large | Arrays, JSONB, full-text search (inverted indexes) |
| **BRIN** | `=`, `<`, `<=`, `>`, `>=` | ✅ Yes | ❌ No | ❌ No | Very small | Time-series, monotonically increasing columns, very large tables |

> **Note**: Bloom indexes are available as an extension (`bloom`), but are not covered in this document.

---

## Sources

- Official PostgreSQL 18 Documentation — [Chapter 11: Indexes](https://www.postgresql.org/docs/current/indexes.html)
