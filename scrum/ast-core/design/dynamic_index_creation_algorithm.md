# Algorithm Design: Dynamic Index Creation from AST

## 1. Problem Statement

Given a set of rule expressions encoded as AST nodes, each potentially containing aggregation queries over database tables, automatically derive the minimal set of database indexes required to execute those queries efficiently.

### Constraints

- An AST expression may contain zero or more aggregation nodes (e.g., `Aggregator(tableName, fieldName, filters)`).
- Each aggregation has filters with operators: equality (`=`), inequality (`<`, `<=`, `>`, `>=`), and others (IsInList, FuzzyMatch, StartsWith, etc.).
- Existing indexes must be reused when possible.
- The number of new indexes created must be minimized.

## 2. Algorithm Overview

```
AST Nodes
    │
    ▼
[1. Extract Aggregate Query Families]
    │  Walk AST recursively, identify FUNC_AGGREGATOR nodes
    │  For each: extract tableName, fieldName, filter conditions
    │  Classify filters into Eq / Ineq / Other
    │  Deduplicate by hash
    │
    ▼
[2. Project Query Families to Index Families]
    │  For each query family:
    │  - Eq columns → Flex (unordered indexed set)
    │  - Other columns → Included (cover-only)
    │  - Ineq columns → one family per column, each as Last
    │
    ▼
[3. Filter Against Existing Indexes]
    │  For each index family:
    │  - Check if any existing ConcreteIndex covers it
    │  - If yes, skip
    │  - If no, keep for creation
    │
    ▼
[4. Minimize Index Families]
    │  Group by table. For each table:
    │  - Sort families
    │  - Try to merge overlapping families via refinement
    │
    ▼
[5. Project to Concrete Indexes]
    │  For each remaining family:
    │  - Build ordered Indexed: Fixed + sorted(Flex) + Last
    │  - Generate name with random suffix
    │  - Set type = AGGREGATION
```

## 3. Detailed Algorithm Steps

### Step 1: Extract Aggregate Query Families

**Input**: A slice of AST `Node` objects (from scenario triggers and rule formulas).

**Output**: A set of `AggregateQueryFamily` objects (deduplicated).

**Procedure**:

```
function extractQueryFamiliesFromAst(node):
    families = empty set
    
    if node.function == FUNC_AGGREGATOR:
        family = new AggregateQueryFamily
        family.tableName = node.namedChildren["tableName"].constant
        family.fieldName = node.namedChildren["fieldName"].constant
        
        if node has "filters" named child:
            for each filter in filters.children:
                operator = filter.namedChildren["operator"].constant
                fieldName = filter.namedChildren["fieldName"].constant
                
                switch operator:
                    case "=":
                        family.eqConditions.add(fieldName)
                    case "<", "<=", ">", ">=":
                        // Only if not already an eq condition
                        if fieldName not in family.eqConditions:
                            family.ineqConditions.add(fieldName)
                    default:  // IsInList, FuzzyMatch, etc.
                        // Only if not already eq or ineq
                        if fieldName not in family.eqConditions
                           and fieldName not in family.ineqConditions:
                            family.otherConditions.add(fieldName)
        
        // The aggregated field itself goes into other conditions
        // if not already covered by eq or ineq
        if family.fieldName not in family.eqConditions
           and family.fieldName not in family.ineqConditions:
            family.otherConditions.add(family.fieldName)
        
        families.add(family)
    
    // Recurse into children and named children
    for each child in node.children ∪ node.namedChildren:
        families.union(extractQueryFamiliesFromAst(child))
    
    return families
```

**Deduplication**: Uses a hash function that sorts column names within each category, producing a stable string key. `EqConditions: {b, a}` and `EqConditions: {a, b}` produce the same hash.

---

### Step 2: Project Query Families to Index Families

**Input**: A single `AggregateQueryFamily`.

**Output**: A set of `IndexFamily` objects.

**Key insight**: A B-tree index can only use one column for range scans. If a query has multiple inequality conditions, separate indexes are needed — each with a different inequality column as the last indexed column.

**Procedure**:

```
function queryFamilyToIndexFamilies(qFamily):
    if qFamily has no EqConditions AND no IneqConditions:
        return empty set  // nothing indexable
    
    base = new IndexFamily
    base.tableName = qFamily.tableName
    base.flex = qFamily.eqConditions        // equality → Flex
    base.included = qFamily.otherConditions  // other → Included
    
    if qFamily has no IneqConditions:
        return {base}
    
    // Generate one family per inequality column
    families = empty set
    for each ineqField in qFamily.ineqConditions:
        family = copy(base)
        family.last = ineqField
        // All other inequality columns go to Included
        for each otherIneq in qFamily.ineqConditions:
            if otherIneq != ineqField:
                family.included.add(otherIneq)
        families.add(family)
    
    return families
```

**Example**:

```
Query: Eq={user_id, status}, Ineq={created_at, amount}, Other={description}

Family 1: Flex={user_id, status}, Last=created_at, Included={amount, description}
Family 2: Flex={user_id, status}, Last=amount,     Included={created_at, description}
```

---

### Step 3: Filter Against Existing Indexes

**Input**: A set of `IndexFamily` objects + a list of existing `ConcreteIndex` objects.

**Output**: A filtered set of `IndexFamily` objects (those not already covered).

**Coverage check** (`ConcreteIndex.covers(IndexFamily)`):

```
function covers(concreteIndex, indexFamily):
    if tableName doesn't match: return false
    if len(concreteIndex.indexed) < len(indexFamily.fixed): return false
    
    // Check fixed prefix
    for i = 0 to len(indexFamily.fixed) - 1:
        if concreteIndex.indexed[i] != indexFamily.fixed[i]:
            return false
    
    // Check flex columns appear contiguously after fixed
    start = len(indexFamily.fixed)
    flexCount = indexFamily.flex.size()
    if start + flexCount > len(concreteIndex.indexed):
        return false
    
    flexSlice = concreteIndex.indexed[start : start + flexCount]
    if set(flexSlice) != indexFamily.flex:
        return false
    
    // Check last column (if any)
    if indexFamily.last != "":
        if indexFamily.size() > len(concreteIndex.indexed):
            return false
        if concreteIndex.indexed[indexFamily.size() - 1] != indexFamily.last:
            return false
    
    // Check included columns
    return concreteIndex.included is superset of indexFamily.included
```

---

### Step 4: Minimize Index Families

**Input**: A set of `IndexFamily` objects (all on the same table).

**Output**: A minimized set where overlapping families have been merged.

**Approach**: Sort families by hash, then iterate. For each input family, try to merge it with an existing output family using the refinement algorithm. If merge succeeds, replace the output family with the merged result. If not, add the input as a new output family.

#### The Refinement Algorithm (`refineIdxFamilies`)

This is the most complex part. It tries to merge two index families L and R into one that covers both.

**Notation**: Each family has:
- `Fixed`: ordered prefix columns
- `Flex`: unordered indexed set
- `Last`: optional single range column
- `Included`: cover-only columns

**Case analysis**:

**Case A — Both have non-empty Fixed with different values at any position**:
→ Cannot merge. Return failure.

**Case B — Both have non-empty Fixed with matching prefix**:
→ Strip the common prefix, recursively attempt merge on the remainder, then prepend prefix back.

**Case C — One has empty Fixed (the "flexible" one)**:
- **Subcase C1**: The flexible family (A) is larger than the other (B), and B's indexed columns are a subset of A's.
  → B can use A's index. Result is B's structure with A's Included merged in.
  
- **Subcase C2**: Both have same total indexed columns.
  → If same columns (possibly different order), merge by taking B's structure + A's Included.
  
- **Subcase C3**: The flexible family (A) is smaller than the other (B), and A's columns are a subset of B's.
  → More complex. B's Fixed columns must appear in A's Flex. A's Last (if any) must be in B's Flex. 
    Result uses B as base, adds A's remaining Flex columns to B's Fixed, merges Included.

**Pseudo-code**:

```
function refineIdxFamilies(L, R):
    // Strip common Fixed prefix
    prefixLen = min(len(L.fixed), len(R.fixed))
    if prefixLen > 0:
        for i = 0 to prefixLen-1:
            if L.fixed[i] != R.fixed[i]:
                return failure
        prefix = L.fixed[0:prefixLen]
        merged = refineIdxFamilies(
            L.removePrefix(prefix),
            R.removePrefix(prefix)
        )
        if merged:
            return merged.prependPrefix(prefix)
        return failure
    
    // At this point, one has empty Fixed (or both)
    if len(R.fixed) == 0:
        short, long = R, L
    else:
        short, long = L, R
    
    return mergeWhenFirstHasNoFixed(short, long)
```

---

### Step 5: Project to Concrete Indexes

**Input**: A minimized set of `IndexFamily` objects.

**Output**: A list of `ConcreteIndex` objects ready for DDL creation.

**Procedure**:

```
function projectToConcreteIndex(indexFamily):
    indexed = []
    indexed.addAll(indexFamily.fixed)
    indexed.addAll(sorted(indexFamily.flex))
    if indexFamily.last != "":
        indexed.add(indexFamily.last)
    
    return ConcreteIndex(
        tableName = indexFamily.tableName,
        indexed = indexed,
        included = sorted(indexFamily.included),
        type = IndexType.AGGREGATION
    )
```

## 4. Naming Strategy

Index names must be unique and deterministic to allow comparison but must not collide when an index is recreated.

**Scheme**: `idx_{TABLE}_{COLUMNS}_{RANDOM_SUFFIX}`

- Prefix: `idx_` for aggregation indexes
- Table name and column names: hyphen-separated
- Random suffix (UUID-based): prevents collisions when a previous version exists but is invalid
- Total length truncated to 63 characters (PostgreSQL identifier limit)

## 5. Integration Flow

When a scenario is published:

1. Fetch the scenario iteration's AST (trigger condition + all rules).
2. Fetch existing valid indexes from the client database.
3. Run the pipeline: AST → Query Families → Index Families → Filter → Minimize → Concrete Indexes.
4. If there are pending index creations, wait or return early.
5. Create new indexes asynchronously (non-blocking).
6. Optionally register a callback for when index creation succeeds.

## 6. Complexity Analysis

- AST walk: O(N) where N = total AST nodes.
- Query family extraction: O(N × F) where F = max filters per aggregation.
- Index family projection: O(Q × I) where Q = query families, I = ineq columns per family (at most I families).
- Coverage check: O(F × C × K) where F = families, C = existing indexes, K = indexed columns per index.
- Minimization: O(F² × K) worst case (each family compared against all output families).

The pipeline is trivially parallelizable per scenario iteration.

## 7. Edge Cases

| Case | Handling |
|---|---|
| No aggregations in AST | Empty output, no indexes created |
| Aggregation with no filters | Outputs a family with just Included (the aggregated field), but only if Eq or Ineq exist. Otherwise skipped. |
| All filters are non-indexable (FuzzyMatch, etc.) | No indexable conditions → no family → no index |
| Aggregation referencing a different table than its filters | Returns error — filter tableName must match parent aggregator |
| Duplicate aggregations across rules | Deduplicated by hash at query family stage |
| Existing index already covers requirement | Skipped entirely, no redundant creation |
| Postgres 63-char name limit | Index name is truncated with random suffix |

## 8. Java Core Library Adaptation

For a reusable Java core:

```
interface IndexAnalyzer {
    Set<AggregateQueryFamily> extractQueryFamilies(AstNode root);
}

interface IndexPlanner {
    Set<IndexFamily> planIndexFamilies(AggregateQueryFamily queryFamily);
    Set<IndexFamily> minimize(Set<IndexFamily> families, List<ConcreteIndex> existing);
}

interface IndexExecutor {
    List<ConcreteIndex> createConcreteIndexes(Set<IndexFamily> families);
    boolean indexExists(ConcreteIndex index);
    void createIndexAsync(ConcreteIndex index, Runnable onSuccess);
}
```

The AST model must support:
- Function type identification (is this an aggregator? a filter?)
- Named child extraction (tableName, fieldName, operator)
- Recursive traversal (children + named children)
