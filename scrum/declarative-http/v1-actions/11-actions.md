# Task 11 — Pre/Post processors + registry (`action/`)

## Objective

Implement the **pre-send and post-receive action** layer (R6, R7, R4): the `PreSendAction` /
`PostReceiveAction` functional interfaces, the `ActionRegistry` (name → factory), and the **built-in
post-receive actions** (`RootProperty`, `FilterItems`, `LimitItems`, `SetValue`, `SortByKey`,
`SetKeyValue`, `BinaryData`) that shape responses — with dot-paths resolved through `dynamic-object`.

## Hand-off context

- **Design doc:** §2 (`action/` tree), §3.1 (sealed `PostReceive` variants → action mapping), §4
  (pipeline "postReceive actions" via `RootProperty`/`filter`/`limit`/...), §5 R4, R6, R7.
- **Already done (prior tasks):**
  - 01 `spec/`: `PostReceive` sealed interface (variants `RootProperty`, `FilterItems`, `LimitItems`,
    `SetValue`, `SortByKey`, `SetKeyValue`, `BinaryData`, `CustomPostReceive`), `PreSend`, `Output`,
    `Expression`.
  - 02 `transport/`: `HttpResult`, `HttpRequest`.
  - 04 `expr/`: `ExpressionEvaluator`, `EvaluationScope`.
  - 07 `plan/`: `RequestPlan` references `PreSendAction`/`PostReceiveStep` (minimal interfaces this
    task now fills in).
- **Package:** `io.github.khezyapp.dhttp.action` and `.builtin`.
- **dynamic-object:** use `io.github.khezyapp.doa.DynamicObjects.get/set` for all path resolution.
- **Conventions:** `@FunctionalInterface`; sealed/record action descriptors. Read
  `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create

`src/main/java/io/github/khezyapp/dhttp/action/`:
1. `PreSendAction.java` — `@FunctionalInterface` (per §3.2):
   `HttpRequest apply(HttpRequest request)`.
2. `PostReceiveAction.java` — `@FunctionalInterface` (per §3.2):
   `List<OutputRecord> apply(List<OutputRecord> records, HttpResult response)`.
   (`OutputRecord` from Task 13 — create it in `engine/` first, or define it here and promote; keep
   a stable type name `OutputRecord`.)
3. `PostReceiveStep.java` — small record binding a `PostReceiveAction` + the props/expression
   context used to invoke it (evaluated per record).
4. `ActionRegistry.java` — `name → factory` mapping: `register(String name, PostReceiveFactory)`,
   `Optional<PostReceiveFactory> get(String name)`, `ActionRegistry withBuiltins()`.
   `@FunctionalInterface PostReceiveFactory { PostReceiveAction create(PostReceive desc, ExpressionEvaluator evaluator); }`.

`src/main/java/io/github/khezyapp/dhttp/action/builtin/` (each a `public final class` + factory or a
record implementing a `PostReceiveAction`):
5. `RootProperty.java` — extracts `DynamicObjects.get(body, property)` from the response and uses it
   as the record list (R4). Covers `PostReceive.RootProperty`.
6. `FilterItems.java` — keeps records where the `Expression pass` is true. Covers `PostReceive.FilterItems`.
7. `LimitItems.java` — caps at `int max`. Covers `PostReceive.LimitItems`.
8. `SetValue.java` — sets a value on each record via `DynamicObjects.set`. Covers `PostReceive.SetValue`.
9. `SortByKey.java` — sorts by key asc/desc. Covers `PostReceive.SortByKey`.
10. `SetKeyValue.java` — applies `DynamicObjects.set` per key from a `Map<String, Expression>`.
    Covers `PostReceive.SetKeyValue`.
11. `BinaryData.java` — marks a record as binary at a destination property. Covers
    `PostReceive.BinaryData`.
12. `CustomPostReceive.java` — bridges `PostReceive.CustomPostReceive` to a registry-looked-up action.

## Design notes

- **Sealed → action mapping:** switch on the sealed `PostReceive` variant (from Task 01) to produce
  the corresponding built-in `PostReceiveAction`; unknown `CustomPostReceive` goes through the registry.
- **Dot-paths:** every string path is resolved via `DynamicObjects` — never hand-parse.
- **Expressions:** each action that takes an `Expression` evaluates it per record against an
  `EvaluationScope` bound with `$response`, `$value`, `$item`, `$index` as appropriate.
- **Ordering:** built-ins run in list order; `RootProperty` first, then filters/limit/set/sort as
  declared (matches §4 pipeline "rootProperty/filter/limit/set/sort/setKeyValue/binaryData").
- Keep `ActionRegistry.withBuiltins()` under 150 lines by splitting registration into helpers.

## Acceptance criteria

- Compiles + Checkstyle green.
- `ActionRegistryTest`: `withBuiltins()` resolves all 7 named built-ins; unknown name returns empty;
  a custom action can be registered and looked up.
- `RootPropertyTest`: given a `HttpResult` body JSON `{"data":{"items":[{...},{...}]}}`, applying
  `RootProperty("data.items")` yields 2 records (via `DynamicObjects`).
- `LimitItemsTest`: caps N records to `max`.
- `FilterItemsTest`: keeps only records where the expression passes.
- `SetKeyValueTest`: `DynamicObjects.set` applied per key on a record.
- `SortByKeyTest`: sorts records by key asc/desc.
- No unused imports; `final` everywhere; method length < 150.

## Hand-off to next task

Task 13 (engine) uses `ActionRegistry.withBuiltins()` to materialize `PostReceiveStep`s from a
`Route`'s `Output.postReceive`, and runs them in order inside `Pipeline`. Keep `PostReceiveAction`
and `ActionRegistry` signatures stable.
