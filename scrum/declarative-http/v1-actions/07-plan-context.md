# Task 07 — Plan & context (`plan/`) — integration checkpoint

## Objective

Implement the **per-item runtime context** and the **pure request-planning core** (R2, R3, R16):
`RequestContext` (bindings), `RequestPlan` (output), `FragmentMerger` (deep-merge defaults +
fragments), `ConditionEvaluator` (precondition gating), and `RequestPlanner` (spec + context →
`RequestPlan`). This is the first task that wires multiple earlier leaves together — a good
integration checkpoint.

## Hand-off context

- **Design doc:** §2 (`plan/` tree), §3.2 (records `RequestContext` / `RequestPlan`), §4 (pipeline
  steps "Select active Operation via preconditions" → "RequestPlanner.plan"), §5 R2, R3, R16.
- **Already done (prior tasks):**
  - 01 `spec/`: `HttpRequestSpec`, `Operation`, `Route`, `Send`, `Output`, `PostReceive`,
    `PreSend`, `PaginationSpec`, `SecurityPolicy`, `CredentialRef`, `RequestShape`, `HttpMethod`,
    `Expression`, `Target`, `Condition`.
  - 02 `transport/`: `HttpRequest` + builder, `HttpResult`, `Body`, `Auth`, `ArrayFormat`,
    `HttpTransport`.
  - 04 `expr/`: `ExpressionEvaluator`, `EvaluationScope`, `JexlExpressionEvaluator`,
    `JexlEngineFactory`, `DoaNamespace`.
  - 05 `error/`: `HttpApiException`, `HttpErrorFactory`, `OAuth2NotConfiguredException`.
- **Package:** `io.github.khezyapp.dhttp.plan`.
- **Conventions:** pure/stateless planner; records for plan/context. Read
  `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/plan/`)

1. `RequestContext.java` — record (§3.2):
   `record RequestContext(String operationId, OutputRecord item, Map<String,Object> parameters, Map<String,Object> credentials, Map<String,Object> variables, Consumer<HttpResult> onResponse)`.
   `OutputRecord` is defined in Task 13 (`engine/`) — until then, define a minimal local
   `OutputRecord` in `engine/` first (or a placeholder in this task that Task 13 promotes). Prefer:
   create the real `engine.OutputRecord` record now (see Task 13 for its shape) so the context is stable.
2. `RequestPlan.java` — record (§3.2):
   `record RequestPlan(HttpRequest request, List<PreSendAction> preSends, List<PostReceiveStep> postReceives, PaginationStrategy pagination, int maxResults, AuthRequest authRequest)`.
   The referenced `PreSendAction`/`PostReceiveStep`/`PaginationStrategy`/`AuthRequest` types are
   produced in Tasks 11/12/09 — define minimal interfaces here now (same package or imported), to be
   refined by those tasks. Keep `RequestPlan` generic over an `HttpRequest`.
3. `FragmentMerger.java` — deep-merge of Route fragments (R2):
   - `Route mergeDefaults(HttpRequestSpec spec, Operation op, Route route)` — merge spec-level
     defaults (`defaultHeaders`, `defaultTimeoutMillis`, `defaultPagination`) under operation-level
     overrides, then route-level overrides win.
   - Deep-merge maps/lists (operation headers win over default headers per-key; nested maps merged
     recursively).
4. `ConditionEvaluator.java` — R3 gating:
   - `boolean evaluate(List<Condition> when, RequestContext ctx)` — all conditions must pass for an
     `Operation` to be active. A condition compares `ctx.parameters()`/item value at `property`
     against `equals` (or checks existence when `exists` is set).
   - `Operation selectOperation(HttpRequestSpec spec, RequestContext ctx)` — first operation whose
     `when` passes (or the single operation when no `when`).
5. `RequestPlanner.java` — the pure engine core (R16):
   - `RequestPlan plan(HttpRequestSpec spec, Operation operation, RequestContext ctx)`:
     1. `FragmentMerger` produces the merged `Route`.
     2. Build a transport `HttpRequest` from `RequestShape` (method, path, headers, query).
     3. Apply `Send` mappings: for each `Send`, resolve the param value from `ctx.parameters()`
        (`dotNotation` → `DynamicObjects.get(paramValue, property)`), place into body/query.
     4. Resolve any `Expression` values via the `ExpressionEvaluator` against an `EvaluationScope`
        built from `ctx` (bind `$parameter`, `$credentials`, etc.).
     5. Assemble `RequestPlan` (request + pipeline refs + maxResults + authRequest from
        `spec.defaultCredential` or operation override).
   - Must be **pure** (no state; all inputs explicit) to satisfy R16.

## Design notes

- **Dot-notation:** `Send.property` with `dotNotation=true` resolves via
  `io.github.khezyapp.doa.DynamicObjects.get(value, property)` — do NOT hand-parse paths.
- **Expressions:** strings that `ExpressionEvaluator.isExpression(...)` are evaluated; others pass
  through as literals. Bind `$parameter`, `$credentials`, `$variables` into the scope.
- **Selecting the operation:** delegate to `ConditionEvaluator.selectOperation`; plan only the
  selected operation.
- Keep `RequestPlanner` a single `final` class with `plan(...)`; keep helpers private and under
  150 lines each.

## Acceptance criteria

- Compiles + Checkstyle green.
- `FragmentMergerTest`: default headers merged with operation override (per-key); operation header
  wins; nested map deep-merged.
- `ConditionEvaluatorTest`: operation with matching precondition is selected; non-matching is skipped;
  no `when` → single operation selected.
- `RequestPlannerTest` (uses `FakeTransport` from Task 02 test scope):
  - A Brevo-style spec (baseUrl + operation with a `Send(..., BODY, "attributes", true, null)`)
    plans to a `RequestPlan` whose `request` has correct method/path/headers/body.
  - A dot-notation `Send` resolves a nested param via `DynamicObjects`.
  - An `Expression` valueOverride resolves via the JEXL evaluator.
  - Same input twice → identical `RequestPlan` (determinism/R16).
- No unused imports; `final` everywhere; method length < 150.

## Hand-off to next task

Tasks 09/10/11/12 will fill in the concrete `AuthRequest`, `PreSendAction`, `PostReceiveStep`, and
`PaginationStrategy` types that `RequestPlan` references; keep those minimal interfaces stable so the
planner does not need rework. Task 13 (engine) drives `plan` then runs the pipeline.
