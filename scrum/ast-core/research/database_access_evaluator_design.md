# DatabaseAccess Evaluator — Core Concept Design

## 1. What It Is

`DatabaseAccess` is an AST evaluator that reads a field value from a database table by traversing a **join path** starting from the trigger object (the payload being evaluated). It is the bridge between the rule's AST expression and the actual data stored in per-organization client databases.

**Purpose**: When a rule needs to compare a field from a **related table** (not just the trigger object), `DatabaseAccess` follows links defined in the data model to reach the target table and read the field.

## 2. Usage

The evaluator is registered as `FUNC_DB_ACCESS` and expects three named arguments:

| Named Argument | Type | Description |
|---|---|---|
| `tableName` | `string` | The **trigger table name** — the table the current evaluation payload belongs to |
| `fieldName` | `string` | The target field to read from the destination table |
| `path` | `[]any` (of strings) | A sequence of **link names** that form the join traversal from the trigger table to the destination table |

**Return value**: The value of the targeted field, or `nil` if any intermediate foreign key is null or the target row doesn't exist.

## 3. Core Concept: How Join Tables Work

### 3.1. The Data Model

The data model defines:
- **Tables** — each with `Fields` and `LinksToSingle`
- **LinksToSingle** — named relationships between tables

Each `LinkToSingle` defines a parent-child (directional) relationship:

```go
type LinkToSingle struct {
    Id              string
    Name            string          // Link name, used in the AST path
    ParentTableName string          // The table on the "one" side
    ParentFieldName string          // Column in parent table (PK typically)
    ChildTableName  string          // The table on the "many" side
    ChildFieldName  string          // Column in child table (FK referencing parent)
}
```

**Example relationship**:

```
transactions (child)                    accounts (parent)
┌─────────────────────────┐            ┌──────────────────────┐
│ object_id (PK)          │            │ object_id (PK)       │
│ account_id (FK)  ───────┼───────────▶│ account_number       │
│ amount                  │            │ account_type         │
│ created_at              │            │                      │
└─────────────────────────┘            └──────────────────────┘

Link name: "account"
ChildTableName:  "transactions"
ChildFieldName:  "account_id"    ← FK in transactions
ParentTableName: "accounts"
ParentFieldName: "object_id"     ← PK in accounts
```

### 3.2. The Path Mechanism

The `path` argument is a **chain of link names** that navigates the data model graph starting from the trigger table. Each element in the path jumps to the parent table defined by that link.

**Algorithm**:

```
Start at triggerTable (from payload's tableName)

For each linkName in path:
    1. Look up linkName in currentTable.LinksToSingle
    2. Move to link.ParentTableName (the parent table)
    3. Record the join condition for the SQL query

Read fieldName from the last table reached
```

**Why a path and not a single link?** Because related tables can themselves have relationships to other tables, forming a chain:

```
transactions → account → customer
     │                      │
     │ Link: "account"      │ Link: "customer"
     │ Parent: "accounts"   │ Parent: "customers"
     ▼                      ▼
   accounts table        customers table
```

### 3.3. SQL Generation (Real Execution)

When not in dry-run mode, the evaluator builds a `DbQuery` model and delegates to `dbAccessor.executeQuery()`:

1. **Read the FK value from the payload**: `payload.get(link.childFieldName())` — this is the value in the trigger object that references the parent (e.g., `account_id = "acc-123"`).

2. **Build the DbQuery**:
   - `FROM` = the first hop's parent table (e.g., `accounts AS table_1`)
   - `WHERE` = `table_1.parentFieldName = :fkValue` (the FK value from the payload)
   - `SELECT` = the target field from the last hop's parent table
   - For multi-hop paths: add `LEFT JOIN` clauses for each subsequent hop, chaining parent → child PK/FK pairs

3. **Aliases**: Tables are aliased as `table_1`, `table_2`, ..., `table_N` where `N = len(path)`.

4. **No soft-delete filtering**: The current implementation does not add `valid_until = 'Infinity'` filters. If temporal/SCD filtering is needed, it can be added via `DbJoin.extraConditions`.

### 3.4. Null Handling

If the FK value in the payload is `null` at any step, the entire query is skipped and `nil` is returned directly (no database call). This is an important optimization — no point in querying for a null foreign key.

### 3.5. Dry Run Mode

When `ReturnFakeValue` is `true` (used for scenario dry-runs/testing), the evaluator:
- Walks the data model following the path links (validates the path exists).
- Returns a fake typed value based on the field's data type:
  - `Bool` → `true`
  - `String` → `"fake value for DbAccess:<table>.<path>.<field>"`
  - `Int` → `1`
  - `Float` → `1.0`
  - `Timestamp` → `time.Now()`
  - `IpAddress` → `net.ParseIP("1.2.3.4")`

This allows rules to be tested without actual database access.

## 4. AST Node Structures

### 4.1. Single Table Access (No Join)

Reading a field directly from the **trigger table** itself. The path is empty because no join is needed.

**Scenario**: Evaluating a transaction rule that reads the `amount` field from the trigger transaction itself.

**Data model**:
```
transactions table
├── amount (field)
├── currency (field)
└── account_id (FK → accounts)
```

**AST Node**:
```json
{
  "function": "FUNC_DB_ACCESS",
  "namedChildren": {
    "tableName": { "function": "FUNC_CONSTANT", "constant": "transactions" },
    "fieldName": { "function": "FUNC_CONSTANT", "constant": "amount" },
    "path":       { "function": "FUNC_CONSTANT", "constant": [] }
  }
}
```

**What happens**:
- Dry run: returns `"fake value for DbAccess:transactions..amount"`
- Real execution: reads `amount` from the payload's `Data` map directly → no SQL needed (the payload IS the trigger table row).

Wait — this depends on whether `path` is empty. Let me re-examine...

Looking at the real execution path: `GetDbField` returns an error if `path` is empty. So for the **real execution**, a path is always required. The "single table access" case would actually be handled by `FUNC_PAYLOAD` (reading from the trigger object directly), not `FUNC_DB_ACCESS`.

However, in the **dry run**, an empty path works and reads from the trigger table.

### 4.2. Single Join (One Link)

Reading a field from a **directly related table** via one link.

**Scenario**: In a transaction rule, read the `account_type` from the `accounts` table that the transaction belongs to.

**Data model link**:
```
Link name: "account"
ChildTableName:  "transactions"
ChildFieldName:  "account_id"     ← FK in transaction row
ParentTableName: "accounts"
ParentFieldName: "object_id"      ← PK in account row
```

**AST Node**:
```json
{
  "function": "FUNC_DB_ACCESS",
  "namedChildren": {
    "tableName": { "function": "FUNC_CONSTANT", "constant": "transactions" },
    "fieldName": { "function": "FUNC_CONSTANT", "constant": "account_type" },
    "path": {
      "function": "FUNC_CONSTANT",
      "constant": ["account"]
    }
  }
}
```

**Generated SQL** (real execution):
```sql
SELECT table_1.account_type
FROM accounts AS table_1
WHERE table_1.object_id = '<value from payload.account_id>'
  AND table_1.valid_until = 'Infinity'
```

With payload containing `account_id = "acc-123"`:
```sql
SELECT table_1.account_type
FROM accounts AS table_1
WHERE table_1.object_id = 'acc-123'
  AND table_1.valid_until = 'Infinity'
```

**Returned**: `"checking"` (the account_type value).

### 4.3. Multi-Hop Join (Chained Links)

Reading a field from a table reachable via **multiple successive links**.

**Scenario**: In a transaction rule, read the `country` from the `customers` table, reachable via `transaction → account → customer`.

**Data model**:
```
transactions (trigger table)
├── account_id (FK → accounts)
│
accounts table
├── customer_id (FK → customers)   ← Link: "customer"
├── account_type
│
customers table
├── country                        ← target field
├── customer_name
```

**Links**:
```
Link "account":   transactions.account_id → accounts.object_id
Link "customer":  accounts.customer_id → customers.object_id
```

**AST Node**:
```json
{
  "function": "FUNC_DB_ACCESS",
  "namedChildren": {
    "tableName": { "function": "FUNC_CONSTANT", "constant": "transactions" },
    "fieldName": { "function": "FUNC_CONSTANT", "constant": "country" },
    "path": {
      "function": "FUNC_CONSTANT",
      "constant": ["account", "customer"]
    }
  }
}
```

**Generated SQL**:
```sql
SELECT table_2.country
FROM accounts AS table_1
JOIN customers AS table_2
  ON table_1.customer_id = table_2.object_id
WHERE table_1.object_id = '<value from payload.account_id>'
  AND table_1.valid_until = 'Infinity'
  AND table_2.valid_until = 'Infinity'
```

With payload containing `account_id = "acc-123"`:
```sql
SELECT table_2.country
FROM accounts AS table_1
JOIN customers AS table_2
  ON table_1.customer_id = table_2.object_id
WHERE table_1.object_id = 'acc-123'
  AND table_1.valid_until = 'Infinity'
  AND table_2.valid_until = 'Infinity'
```

### 4.4. Typical Rule Expression Combining DB Access

A complete rule AST that compares a DB field against a constant:

```json
{
  "function": "FUNC_EQUAL",
  "children": [
    {
      "function": "FUNC_DB_ACCESS",
      "namedChildren": {
        "tableName": { "function": "FUNC_CONSTANT", "constant": "transactions" },
        "fieldName": { "function": "FUNC_CONSTANT", "constant": "account_type" },
        "path":       { "function": "FUNC_CONSTANT", "constant": ["account"] }
      }
    },
    {
      "function": "FUNC_CONSTANT",
      "constant": "checking"
    }
  ]
}
```

This represents the rule: **"Is the transaction's account type equal to 'checking'?"**

## 5. Architecture Summary

```
┌─────────────────────────────────────────────────────────────┐
│                     Rule AST Expression                       │
│                                                              │
│   FUNC_EQUAL                                                 │
│   ├── FUNC_DB_FIELD_ACCESS                                   │
│   │   ├── tableName: "transactions"                          │
│   │   ├── fieldName: "account_type"                          │
│   │   └── path:      ["account"]                             │
│   └── FUNC_CONSTANT: "checking"                              │
│                                                              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              DbFieldAccessEvaluator.Evaluate()                │
│                                                              │
│   1. Extract tableName, fieldName, path from named args      │
│   2. Resolve path via SchemaRegistry.getLink() chain         │
│   3. Build DbQuery (FROM+JOIN+WHERE+SELECT)                  │
│   4. Execute via dbAccessor.executeQuery(query, payload)      │
│   5. Attach meta.generatedSql to result                      │
│                                                              │
└──────────────────────┬──────────────────────────────────────┘
                       │
          ┌────────────┴────────────┐
          ▼                        ▼
   Dry Run Mode               Real Execution
   (ctx.isDryRun=true)         (ctx.isDryRun=false)
          │                        │
          ▼                        ▼
   Walk links in Schema      Read FK from payload
   Registry, validate        Build DbQuery model
   path + target field       Execute query via
   Return fake typed         DbAccessor.executeQuery
   value based on            Return field value
   FieldMetadata.dataType    or null
          │                        │
          └────────────┬───────────┘
                       ▼
             Return field value or nil
```

## 6. Key Design Points

1. **Path is directional**: The path always follows `LinksToSingle` in the parent direction (child → parent). This reflects a "belongs_to" style traversal.

2. **Path validates at dry-run time**: When testing, the evaluator walks the data model following the links, validating that each link name exists. Errors are caught before deployment.

3. **Null FK = nil result**: If any FK in the chain is null, the evaluator returns `nil` without querying. This is correct behavior — a null FK means "no related row".

4. **Table aliasing**: All joined tables are aliased as `table_N` to avoid ambiguity when the same physical table appears multiple times in a path chain.
