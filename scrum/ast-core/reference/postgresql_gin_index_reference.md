# PostgreSQL GIN Index Reference

> Based on PostgreSQL official documentation (Chapter 65.4 — GIN Indexes).
> PostgreSQL version: 18 (current stable at time of writing).

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [What GIN Is](#2-what-gin-is)
3. [Architecture: Inverted Index Structure](#3-architecture-inverted-index-structure)
4. [Supported Data Types and Operators](#4-supported-data-types-and-operators)
5. [Operator Classes](#5-operator-classes)
6. [How It Works Internally](#6-how-it-works-internally)
7. [GIN Fast Update Technique](#7-gin-fast-update-technique)
8. [Partial Match Algorithm](#8-partial-match-algorithm)
9. [Search Modes](#9-search-modes)
10. [When PostgreSQL Uses a GIN Index](#10-when-postgresql-uses-a-gin-index)
11. [When PostgreSQL Does NOT Use a GIN Index](#11-when-postgresql-does-not-use-a-gin-index)
12. [Multicolumn GIN Indexes](#12-multicolumn-gin-indexes)
13. [GIN vs B-tree](#13-gin-vs-b-tree)
14. [Configuration Parameters](#14-configuration-parameters)
15. [Limitations](#15-limitations)
16. [Tips and Tricks](#16-tips-and-tricks)
17. [Quick Reference Table](#17-quick-reference-table)

---

## 1. Introduction

GIN (Generalized Inverted Index) is PostgreSQL's answer to searching **inside** composite data types. While B-tree indexes answer "which rows have column X = value?", GIN answers "which rows contain value Y inside column X?".

### What It Is

GIN is an **inverted index** designed for data values that contain **multiple component values**. For each component value, GIN stores a list of rows that contain that component. It was first released in PostgreSQL 8.2 (2006), authored by Teodor Sigaev and Oleg Bartunov.

### What It Is NOT

- Not a replacement for B-tree — GIN does not support equality or range on scalar columns
- Not a sorted index — GIN does not store ordering information
- Not for index-only scans — GIN does NOT support index-only scans
- Not for exact-match on whole composite values — GIN decomposes into keys

### Core Concept: Item vs Key

| Term | Meaning | Example |
|---|---|---|
| **Item** | The full composite value being indexed | A JSONB document, an array, a tsvector |
| **Key** | An individual element extracted from the item | A JSONB key name, an array element, a lexeme |

GIN stores (key → posting list) pairs, where a posting list is a set of row IDs (TIDs) in which the key appears.

---

## 2. What GIN Is

### The Inverted Index Principle

A traditional B-tree stores: `row → value` (for each row, where is its value in the tree?).

A GIN inverted index stores: `value → rows` (for each component value, which rows contain it?).

```
B-tree:    row 1 → {'a','b'}     → stored under 'a' and 'b' separately
GIN:       key 'a' → {row 1, row 3, row 7}
           key 'b' → {row 1, row 5}
           key 'c' → {row 3}
```

Because keys are stored once regardless of how many rows contain them, GIN is **very compact** for cases where the same key appears many times.

### The "Generalized" Part

GIN is "generalized" in the same sense as GiST: the index access method code does **not** know the specific operations it accelerates. Instead, it relies on **operator classes** that define:

- How to extract keys from indexed items (`extractValue`)
- How to extract keys from query conditions (`extractQuery`)
- Whether a row containing certain keys satisfies the query (`consistent`)
- How to compare keys for sorting (`compare`)

This abstraction means GIN can index arrays, JSONB, full-text search vectors, trigrams, and any future data type that provides the required operator class.

---

## 3. Architecture: Inverted Index Structure

### High-Level Structure

```
┌─────────────────────────────────────────────────────────────┐
│                      Metapage                               │
│  (head/tail of pending list, page counts, version number)    │
└─────────────────────────────────────────────────────────────┘
                            │
                ┌───────────┴───────────┐
                │                       │
        ┌───────▼───────┐       ┌───────▼───────┐
        │  Entry Tree   │       │  Pending List  │
        │  (B-tree of   │       │  (unsorted,    │
        │   keys)       │       │   fastupdate)  │
        └───────────────┘       └───────────────┘
               │
      ┌────────┴────────┐
      │                 │
  ┌───▼───┐        ┌────▼────┐
  │Posting│        │ Posting │
  │ List  │        │  Tree   │
  │(inline│        │ (B-tree │
  │ TIDs) │        │ of TIDs)│
  └───────┘        └─────────┘
```

A GIN index consists of:

| Component | Description |
|---|---|
| **Metapage** | Index metadata: pending list head/tail pointers, page counts, GIN version number |
| **Entry Tree** | A B-tree indexed over key values. Leaf entries contain either a posting list (inline) or a pointer to a posting tree |
| **Posting List** | A compressed list of ItemPointers (TIDs) stored inline in the entry tree leaf when a key matches few rows |
| **Posting Tree** | A separate B-tree of ItemPointers created when a key's posting list grows too large for one index tuple |
| **Pending List** | A temporary unsorted list of pending entries used by fastupdate (see Section 7) |

### Entry Tree Leaf Page Layout

```
┌──────────────────────────────────┐
│  Page Header (pd_lower, etc.)    │
├──────────────────────────────────┤
│  Index Tuple 1: key 'a'          │
│    ├── key data                  │
│    └── posting list (compressed  │
│        TIDs) OR pointer to       │
│        posting tree root         │
├──────────────────────────────────┤
│  Index Tuple 2: key 'b'          │
│    ...                           │
├──────────────────────────────────┤
│  Free space                      │
├──────────────────────────────────┤
│  ItemIdData array                │
├──────────────────────────────────┤
│  GinPageOpaqueData               │
│  ├── rightlink (next page)       │
│  ├── maxoff                      │
│  └── flags                       │
└──────────────────────────────────┘
```

### Posting Tree (Overflow for Many TIDs)

When a key appears in more rows than fit in a single index tuple, GIN creates a separate **posting tree** — a B-tree keyed by ItemPointer (block number, offset). Posting tree leaf pages use compressed posting lists segmented for efficient random access.

### Posting List Compression

ItemPointers are stored in sorted order using **varbyte encoding** — only the difference from the previous TID is stored. This compresses posting lists significantly (often 3-6x).

```
Uncompressed:  (block=1, off=1), (block=1, off=5), (block=2, off=3)
Compressed:    (1,1), +4, +(1,-2)  [deltas with varbyte encoding]
```

---

## 4. Supported Data Types and Operators

### Array Operators

| Operator | Meaning | Example |
|---|---|---|
| `&&` | Overlaps (share any element) | `WHERE tags && ARRAY['urgent']` |
| `@>` | Contains all elements | `WHERE tags @> ARRAY['postgresql', 'performance']` |
| `<@` | Is contained by | `WHERE tags <@ ARRAY['a', 'b', 'c']` |
| `=` | Equal (same elements) | `WHERE tags = ARRAY['a', 'b']` |

### JSONB Operators

| Operator | Meaning | Supported by jsonb_ops | Supported by jsonb_path_ops |
|---|---|---|---|
| `@>` | Contains (structural containment) | ✅ | ✅ |
| `?` | Key exists (top-level only) | ✅ | ❌ |
| `?|` | Any key exists | ✅ | ❌ |
| `?&` | All keys exist | ✅ | ❌ |
| `@?` | JSONPath predicate check | ✅ | ✅ |
| `@@` | JSONPath predicate check (SQL/JSON) | ✅ | ✅ |

### Full-Text Search Operators (tsvector)

| Operator | Meaning | Example |
|---|---|---|
| `@@` | tsvector matches tsquery | `WHERE search_vector @@ to_tsquery('english', 'cat & dog')` |

### Trigram Operators (pg_trgm extension)

| Operator | Meaning | Example |
|---|---|---|
| `%` | Similarity (GIN trgm) | `WHERE title % 'kuberntes'` |
| `LIKE '%x%'` | Substring match (GIN trgm) | `WHERE title LIKE '%search%'` |
| `ILIKE '%x%'` | Case-insensitive substring | `WHERE title ILIKE '%Search%'` |
| `~` | Regex match (GIN trgm) | `WHERE title ~ 'pattern'` |

### Other contrib Module Operator Classes

| Module | Data Types |
|---|---|
| `btree_gin` | Standard scalar types (int, text, timestamp, etc.) — implements B-tree-compatible operators for GIN |
| `hstore` | `hstore` type — `@>`, `?`, `?|`, `?&`, `->` |
| `intarray` | `int[]` — additional array operators with ordering |
| `pg_trgm` | `text` — trigram-based `LIKE`, `ILIKE`, `~`, `%` |

---

## 5. Operator Classes

### Default vs Path-Ops for JSONB

| Feature | `jsonb_ops` (default) | `jsonb_path_ops` |
|---|---|---|
| Indexes | Every key and value separately | Path-to-value hashes |
| Index size | Baseline | ~40% smaller |
| Containment `@>` | ✅ Supported | ✅ Supported (faster) |
| Existence `?`, `?|`, `?&` | ✅ Supported | ❌ Not supported |
| JSONPath `@?`, `@@` | ✅ Supported | ✅ Supported |
| Best for | Flexible querying across varied keys | Containment-only queries |

### Choosing an Operator Class

```sql
-- Flexible: supports all JSONB operators (larger index)
CREATE INDEX idx_data_gin ON events USING gin (data);

-- Containment-optimized: smaller and faster for @> only
CREATE INDEX idx_data_path ON events USING gin (data jsonb_path_ops);
```

### Key Decision: `jsonb_path_ops` Limitation

`jsonb_path_ops` does NOT create index entries for **value-less structures** like `{"a": {}}`. If you need to search for such structures, use the default `jsonb_ops`.

### Full-Text Search Operator Classes

| Class | Data Type | Operators |
|---|---|---|
| `tsvector_ops` (default) | `tsvector` | `@@` |

### Trigram Operator Classes (pg_trgm)

| Class | Operators | Use Case |
|---|---|---|
| `gin_trgm_ops` | `%`, `LIKE`, `ILIKE`, `~`, `~*` | Fuzzy text matching, substring search |

---

## 6. How It Works Internally

### Key Extraction

When a row is inserted into a table with a GIN index, PostgreSQL:

1. Calls the operator class's `extractValue` function on the indexed column
2. Receives an array of `Datum` keys extracted from the item
3. For each unique key, inserts or updates the entry tree entry

Example — indexing an array `{a, b, c}`:
```
extractValue({a, b, c}) → ['a', 'b', 'c']
```

Example — indexing a JSONB document `{"name": "Alice", "age": 30}`:
```
extractValue({"name": "Alice", "age": 30})
  → ['name', '"Alice"', 'age', '30']
```

### Query Execution

When a query uses an indexable operator, PostgreSQL:

1. Calls `extractQuery` to extract search keys from the query condition
2. Searches the entry tree for each key
3. Collects the posting lists (inline TIDs or from posting trees)
4. Combines results based on the operator (AND/OR logic)
5. Returns candidate TIDs to the bitmap scan

### Page Structure Details

**Entry tree pages** — standard B-tree pages with:
- Index tuples containing key data + optional inline posting list
- Right-links for sibling navigation
- No dedicated "high key" — the rightmost tuple on each page acts as the high key

**Posting tree pages** — pages that store only ItemPointers:
- Internal pages hold `PostingItem` structs: `(child_block, right_bound_key)`
- Leaf pages hold compressed posting lists (GinPostingList segments)
- Right bounds stored after page header for efficient navigation

### Data Page Distinction

Internal GIN uses opaque data flags to distinguish page types:

| Flag | Page Type |
|---|---|
| `GIN_ROOT` | Root of entry tree |
| `GIN_INTERNAL` | Internal entry tree page |
| `GIN_LEAF` | Leaf entry tree page |
| `GIN_DATA` | Posting tree page (data page) |
| `GIN_DATA | GIN_LEAF` | Posting tree leaf |
| `GIN_LIST` | Pending list page |
| `GIN_LIST | GIN_LEAF` | Pending list leaf |
| `GIN_COMPRESSED` | Compressed posting tree leaf (v9.4+) |

---

## 7. GIN Fast Update Technique

### The Problem

Updating a GIN index is inherently expensive — inserting or updating one heap row can cause **many** index entries to be inserted (one per extracted key). An array column with 50 elements creates up to 50 GIN index entries for a single INSERT.

### The Solution: Pending List

GIN postpones index updates by inserting new entries into a **temporary, unsorted list** (pending list) stored on disk as special "list pages". Entries are moved to the main entry tree later through bulk insertion.

### When the Pending List Is Flushed

```
INSERT → pending list grows
         ↓
    ┌────┴────┐
    │ Trigger │  Any of:
    └────┬────┘
         │
    ┌────┴──────────────────┐
    │  VACUUM / autovacuum  │
    │  gin_clean_pending_list│
    │  gin_pending_list_limit│
    └────┬──────────────────┘
         ↓
    pending list merged into main entry tree
    (bulk insert — fast)
```

The pending list is flushed when:

1. **VACUUM** or **autovacuum** runs on the table
2. **`gin_clean_pending_list()`** function is called manually
3. The pending list size exceeds **`gin_pending_list_limit`** (default 4 MB)

### Trade-offs

| Aspect | fastupdate=on (default) | fastupdate=off |
|---|---|---|
| INSERT/UPDATE speed | Fast (deferred) | Slow (immediate) |
| Query speed | Slower (must scan pending list + main tree) | Consistent |
| Vacuum overhead | Higher (must flush pending list) | Lower |
| Latency spikes | Periodic (when limit is hit) | None |
| Best for | Write-heavy workloads | Read-heavy, consistent-latency workloads |

### Configuration

```sql
-- Disable fastupdate for consistent query latency
ALTER INDEX idx_events_data SET (fastupdate = off);

-- Tune pending list threshold (default 4MB)
ALTER INDEX idx_events_data SET (gin_pending_list_limit = 256);
```

---

## 8. Partial Match Algorithm

GIN supports **partial match** queries — searching for keys that fall within a range of the key ordering, rather than exact matches.

### How It Works

1. `extractQuery` returns a lower-bound key value and sets `pmatch = true`
2. GIN scans the entry tree from that lower bound forward
3. For each key encountered, `comparePartial` is called:
   - Returns `0` → match
   - Returns `< 0` → not a match, but keep scanning (more keys in range)
   - Returns `> 0` → past the range, stop scanning

### Use Case: Trigram Prefix Search

The `pg_trgm` extension uses partial match to implement:
```sql
SELECT * FROM articles WHERE title LIKE '%search%';
```

GIN trgm extracts trigrams (3-character substrings) from both the indexed text and the query pattern, then uses partial match to find rows where enough trigrams match.

---

## 9. Search Modes

`extractQuery` can specify one of three search modes via the `searchMode` output argument:

| Mode | Behavior | Use Case |
|---|---|---|
| `GIN_SEARCH_MODE_DEFAULT` | Only items matching at least one returned key are candidates | Standard search |
| `GIN_SEARCH_MODE_INCLUDE_EMPTY` | Also considers items with no keys (null/empty) as candidates | `is-subset-of` operators |
| `GIN_SEARCH_MODE_ALL` | All non-null items in the index are candidates | Corner cases for very rare operators |

---

## 10. When PostgreSQL Uses a GIN Index

### Array Containment Queries

```sql
CREATE INDEX idx_products_tags ON products USING gin (tags);

-- ✅ Contains all specified elements
SELECT * FROM products WHERE tags @> ARRAY['electronics', 'sale'];

-- ✅ Overlaps (shares any element)
SELECT * FROM products WHERE tags && ARRAY['urgent', 'new'];
```

### JSONB Path and Key Queries

```sql
CREATE INDEX idx_events_data ON events USING gin (data);

-- ✅ JSONB containment
SELECT * FROM events WHERE data @> '{"type": "purchase", "amount": 100}';

-- ✅ Key existence
SELECT * FROM events WHERE data ? 'user_email';

-- ✅ Any key in a list exists
SELECT * FROM events WHERE data ?| ARRAY['email', 'phone'];

-- ✅ All keys in a list exist
SELECT * FROM events WHERE data ?& ARRAY['email', 'phone'];
```

### Full-Text Search

```sql
CREATE INDEX idx_articles_search ON articles USING gin (search_vector);

-- ✅ Full-text search
SELECT title FROM articles
WHERE search_vector @@ to_tsquery('english', 'postgresql & performance');
```

### Fuzzy Text Search (pg_trgm)

```sql
CREATE INDEX idx_articles_title ON articles USING gin (title gin_trgm_ops);

-- ✅ LIKE with no anchor (cannot use B-tree)
SELECT * FROM articles WHERE title LIKE '%postgresql%';

-- ✅ Fuzzy match
SELECT * FROM articles WHERE title % 'postgres';

-- ✅ Regex match
SELECT * FROM articles WHERE title ~ 'postgr[s]ql';
```

### Detecting GIN Usage in EXPLAIN

Look for:
```
Bitmap Index Scan on <gin_index_name>
```

The most common mistake — a sequential scan with a GIN index that exists but is not used — appears as:
```
Seq Scan on events
  Filter: (data @> '{"type":"purchase"}'::jsonb)
```

If the index exists but you see a Seq Scan, the query operator is not compatible with the operator class.

---

## 11. When PostgreSQL Does NOT Use a GIN Index

### Operator Mismatch

| Query | Why GIN is NOT used |
|---|---|
| `WHERE data->>'status' = 'active'` | `->>` returns text; GIN indexes JSONB structure, not extracted text |
| `WHERE (data->>'amount')::int > 100` | Range condition on extracted scalar; needs B-tree expression index |
| `WHERE jsonb_array_length(data) > 5` | Function wrapping the column; expression index needed |

### Common Mistake: GIN + Extraction Operator

The single most common JSONB indexing error:

```sql
-- ❌ WRONG: GIN index, but query uses extraction (->>)
CREATE INDEX idx_data_gin ON events USING gin (data);
SELECT * FROM events WHERE data->>'type' = 'click';  -- Seq Scan!

-- ✅ Fix A: Use containment operator (GIN-friendly)
SELECT * FROM events WHERE data @> '{"type": "click"}';

-- ✅ Fix B: Use expression B-tree index
CREATE INDEX idx_events_type ON events ((data->>'type'));
SELECT * FROM events WHERE data->>'type' = 'click';
```

### Other Cases Where GIN Is Not Used

- **Non-composite values**: B-tree or hash is more efficient for simple scalar `=` or range
- **Queries needing sorting**: GIN does not store ordering information
- **Index-only scans**: GIN does NOT support index-only scans
- **Range scans**: GIN cannot do `>`, `<`, `BETWEEN` on the indexed value itself
- **Small tables**: Sequential scan overhead is negligible; index overhead is not worth it
- **B-tree-accelerable operators**: For `WHERE status = 'active'`, a B-tree is strictly superior

---

## 12. Multicolumn GIN Indexes

### How They Work

Unlike multicolumn B-tree indexes, GIN builds a **single B-tree over composite values** `(column_number, key_value)`. The key values for different columns can be of different types.

### Search Effectiveness

**Critical difference from B-tree and GiST**: GIN and BRIN have the **same search effectiveness regardless of which column(s) the query uses**. No column has priority over others.

```sql
-- Multicolumn GIN index
CREATE INDEX idx_events_multi ON events USING gin (customer_id, data);

-- Both of these use the index equally well:
SELECT * FROM events WHERE customer_id = 42;
SELECT * FROM events WHERE data @> '{"type": "purchase"}';
SELECT * FROM events WHERE customer_id = 42 AND data @> '{"type": "purchase"}';
```

### When to Use Multicolumn GIN

- When you have multiple columns that each benefit from GIN indexing
- To cover multiple query patterns with a single index
- NOT to improve selectivity — GIN doesn't work that way

### When to Use Separate Indexes Instead

- If some columns are better suited to B-tree (scalar equality/range)
- If write overhead from a combined GIN is too high
- Example: `customer_id` (scalar, B-tree) + `data` (JSONB, GIN)

```sql
-- Often better: separate indexes for different access patterns
CREATE INDEX idx_events_customer ON events (customer_id);  -- B-tree
CREATE INDEX idx_events_data ON events USING gin (data);    -- GIN
```

PostgreSQL can combine these with BitmapAnd scans.

---

## 13. GIN vs B-tree

### When to Choose GIN Over B-tree

| You have... | And you query with... | Use |
|---|---|---|
| Array column | `@>`, `&&`, `<@` | GIN with `array_ops` |
| JSONB column | `@>`, `?`, `?|`, `?&` | GIN with `jsonb_ops` |
| JSONB column | `@>` only | GIN with `jsonb_path_ops` |
| tsvector column | `@@` | GIN with `tsvector_ops` |
| Text column | `LIKE '%x%'`, `ILIKE`, `~` | GIN with `gin_trgm_ops` |
| Scalar column | `=`, `<`, `>`, `BETWEEN` | B-tree (never GIN) |

### Performance Comparison Example

For a table with 1M rows, JSONB column, querying `WHERE data @> '{"status":"active"}'`:

| Index Type | Query Time | Index Size | Insert Overhead |
|---|---|---|---|
| No index | 285 ms | 0 MB | Baseline |
| GIN (`jsonb_ops`) | 1.2 ms | 124 MB | +38% |
| GIN (`jsonb_path_ops`) | 0.9 ms | 78 MB | +29% |
| B-tree on `(data->>'status')` | 0.08 ms | 21 MB | +8% |

### Decision Framework

```
Is the indexed column composite (array, JSONB, tsvector)?
  ├── Yes → Do you query with composite operators (@>, ?, &&, @@)?
  │         ├── Yes → Use GIN
  │         │        └── JSONB + only @> queries → jsonb_path_ops
  │         └── No → Is it a single extracted key you filter by?
  │                  └── Yes → Use B-tree expression index
  └── No (scalar) → Use B-tree
```

### The 3,500x Trap

```sql
-- ❌ GIN index, extraction query: Seq Scan (285 ms)
CREATE INDEX idx_gin ON events USING gin (data);
SELECT * FROM events WHERE data->>'type' = 'click';

-- ✅ Same data, B-tree expression index: Index Scan (0.08 ms)
CREATE INDEX idx_btree ON events ((data->>'type'));
SELECT * FROM events WHERE data->>'type' = 'click';
```

This is a 3,500x difference. **Always match the index type to the query operator.**

---

## 14. Configuration Parameters

### `gin_pending_list_limit`

Controls the maximum size of the pending list before it is flushed to the main tree.

| Setting | Effect |
|---|---|
| Default | 4 MB (4096 kB) |
| Too low | Frequent foreground flushes → latency spikes |
| Too high | Slow queries (must scan large pending list + main tree) |
| Tuning | Raise for write-heavy tables; lower for read-heavy tables |

```sql
-- Per-index setting (overrides global)
ALTER INDEX idx_events_data SET (gin_pending_list_limit = 256);

-- Global default
SET gin_pending_list_limit = '8MB';
```

### `gin_fuzzy_search_limit`

Soft upper limit on the number of rows returned by a GIN scan. When the search matches many rows (e.g., a full-text search for a common word), this limits the result set to a random subset.

| Setting | Effect |
|---|---|
| Default | 0 (no limit) |
| Recommended | 5000–20000 for full-text search |
| Behavior | Random subset of whole result set |

```sql
SET gin_fuzzy_search_limit = 10000;
```

### `fastupdate` (storage parameter)

Controls whether the pending list mechanism is used.

| Value | Effect |
|---|---|
| `on` (default) | Fast inserts, slower queries (pending list overhead) |
| `off` | Consistent query latency, slower inserts |

### `maintenance_work_mem`

GIN index creation and pending-list cleanup are very sensitive to this setting. Higher values significantly accelerate both operations.

```sql
SET maintenance_work_mem = '1GB';
CREATE INDEX idx_events_data ON events USING gin (data);
```

---

## 15. Limitations

### No Index-Only Scans

GIN does NOT support index-only scans. Each index entry holds only part of the original data value (a single key), so the full item must always be fetched from the heap.

### No Sort Order

GIN does not store ordering information. Queries with `ORDER BY` require a separate sort step.

### Strict Operators Only

GIN assumes all indexable operators are **strict** (return null for null input):

- `extractValue` is NOT called on null item values (placeholder entry created automatically)
- `extractQuery` is NOT called on null query values (query is presumed unsatisfiable)

### Write Amplification

A single row update can create many GIN index entries (one per key extracted from the value). On tables with 1000+ updates/minute, GIN write overhead can become a significant performance concern.

### Pending List Cleanup Cost

When the pending list is flushed (either by reaching `gin_pending_list_limit` or by VACUUM), the cleanup operation can take **multiple seconds** on large indexes. GitLab reported 465 ms–3155 ms cleanup times on their production GIN indexes.

### Fuzzy Search Limit Is "Soft"

`gin_fuzzy_search_limit` does not guarantee exactly that many results — it returns a random subset. For precise pagination, a `LIMIT` clause with `ORDER BY` is still needed.

---

## 16. Tips and Tricks

### Bulk Loading

For bulk INSERT operations, **drop the GIN index first, load data, then recreate**:

```sql
-- Slow with GIN:
COPY millions_of_rows FROM 'data.csv';  -- each row touches GIN index

-- Fast:
DROP INDEX idx_events_data;
COPY millions_of_rows FROM 'data.csv';
CREATE INDEX idx_events_data ON events USING gin (data);  -- single bulk build
```

### Generated tsvector Columns (PG 12+)

Use generated columns instead of triggers for full-text search:

```sql
ALTER TABLE articles ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(body, '')), 'B')
    ) STORED;

CREATE INDEX idx_articles_search ON articles USING gin (search_vector);
```

### Expression Index for Hot JSONB Paths

If you always query the same JSONB key, create a dedicated B-tree expression index:

```sql
-- GIN for broad JSONB queries
CREATE INDEX idx_events_data ON events USING gin (data jsonb_path_ops);

-- B-tree expression index for the single hot path
CREATE INDEX idx_events_customer ON events ((data->>'customer_id'));
```

### Partial Index on Recent Data

For time-bounded queries, make the GIN index partial:

```sql
CREATE INDEX idx_events_recent ON events USING gin (data jsonb_path_ops)
    WHERE created_at > NOW() - INTERVAL '90 days';
```

The partial index is typically 5-10x smaller than a full GIN index.

### Tuning for Write-Heavy Tables

On tables with high write throughput:

1. Increase `gin_pending_list_limit` to reduce foreground flush frequency
2. Make autovacuum more aggressive to flush pending list in background
3. Or disable `fastupdate` entirely if consistent query latency is critical
4. Consider partitioning the table if GIN write overhead remains problematic

### Periodic Monitoring

```sql
-- Check pending list size
SELECT n_pending_pages, n_pending_heap_tuples
FROM pg_stat_user_indexes
WHERE indexrelname = 'idx_events_data';

-- Manually flush pending list (off-peak)
SELECT gin_clean_pending_list('idx_events_data');
```

### Detecting Missing GIN Indexes

```sql
-- Find tables with JSONB/array columns missing GIN indexes
SELECT
    schemaname,
    tablename,
    attname,
    typname
FROM pg_stats, pg_type
WHERE typname IN ('jsonb', 'anyarray')
  AND schemaname NOT IN ('pg_catalog', 'information_schema');
```

Then confirm with:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM products WHERE attributes @> '{"color": "blue"}';
-- Seq Scan + Filter → missing GIN index
-- Bitmap Index Scan → GIN index present and used
```

---

## 17. Quick Reference Table

| Aspect | GIN |
|---|---|
| Full name | Generalized Inverted Index |
| Structure | Inverted index: B-tree of (key → posting list) pairs |
| Multicolumn? | ✅ Yes (column_number, key) B-tree |
| Index-only scans? | ❌ No |
| INCLUDE? | ❌ No |
| Sort order? | ❌ No (no ordering information) |
| Size | Large (one entry per key per row) |
| UPDATE speed | Slow (many entries per row) |
| fastupdate | ✅ Yes (default on — pending list defers writes) |
| Partial match? | ✅ Yes (via `comparePartial` callback) |
| Best for | Arrays, JSONB, full-text search, trigram text search |
| Worst for | Scalar equality/range (use B-tree), sorted output |
| Operator classes | `array_ops`, `jsonb_ops` (default), `jsonb_path_ops`, `tsvector_ops`, `gin_trgm_ops` |
| Built-in contrib | `btree_gin`, `hstore`, `intarray`, `pg_trgm` |
| Fuzzy search limit | `gin_fuzzy_search_limit` (soft limit, random subset) |
| Pending list limit | `gin_pending_list_limit` (default 4 MB) |
| Version added | PostgreSQL 8.2 (2006) |
| Primary authors | Teodor Sigaev, Oleg Bartunov |

---

## Sources

- Official PostgreSQL 18 Documentation — [Chapter 65.4: GIN Indexes](https://www.postgresql.org/docs/current/gin.html)
- Official PostgreSQL 18 Documentation — [Chapter 70.4: GIN Implementation](https://www.postgresql.org/docs/current/gin-implementation.html)
- PostgreSQL source code — [`src/backend/access/gin/README`](https://github.com/postgres/postgres/blob/master/src/backend/access/gin/README)
- PostgreSQL source code — [`src/include/access/ginblock.h`](https://github.com/postgres/postgres/blob/master/src/include/access/ginblock.h)
- PostgreSQL source code — [`src/backend/access/gin/ginbtree.c`](https://github.com/postgres/postgres/blob/master/src/backend/access/gin/ginbtree.c)
- PostgreSQL GIN Indexes: JSONB, Arrays & Full-Text Search — [DEV Community](https://dev.to/philip_mcclarence_2ef9475/postgresql-gin-indexes-jsonb-arrays-full-text-search-29i2)
- Understanding Postgres GIN Indexes — [pganalyze](https://pganalyze.com/blog/gin-index)
