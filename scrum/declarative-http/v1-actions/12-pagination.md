# Task 12 — Pagination (`pagination/`)

## Objective

Implement the **pagination strategy** layer (R9): the `PaginationStrategy` interface and the
`OffsetPagination` / `CursorPagination` built-ins, plus `PaginationContext` (plan + current response
+ continue-eval). Covers the §4 pagination loop (offset / cursor / custom).

## Hand-off context

- **Design doc:** §2 (`pagination/` tree), §6.5 (`PaginationStrategy` interface + built-ins), §4
  (pagination loop → Transport), §5 R9.
- **Already done (prior tasks):**
  - 01 `spec/`: `PaginationSpec` (`mode`, `pageSize`, `rootProperty`, `limitParam`, `offsetParam`,
    `inQuery`, `continueExpression`).
  - 02 `transport/`: `HttpRequest` + builder, `HttpResult`.
  - 04 `expr/`: `ExpressionEvaluator` (for cursor `continueExpression`).
  - 07 `plan/`: `RequestPlan` references `PaginationStrategy` (minimal interface filled in here).
- **Package:** `io.github.khezyapp.dhttp.pagination`.
- **Conventions:** interface + `final` built-ins. Read `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/pagination/`)

1. `PaginationContext.java` — `record PaginationContext(RequestPlan plan, HttpResult last, boolean continueEvaluated)` — carries the plan + current response + whether to continue.
2. `PaginationStrategy.java` — interface (§6.5):
   - `boolean shouldPaginate(RequestPlan plan, HttpResult last)`
   - `HttpRequest nextRequest(RequestPlan plan, HttpResult last)`
   - `List<OutputRecord> collect(RequestPlan plan, List<OutputRecord> page)`
3. `OffsetPagination.java` — `public final class implements PaginationStrategy` (§6.5):
   - `pageSize` (from `PaginationSpec.pageSize`), `limit`/`offset` param names (query|body),
     `rootProperty`.
   - `shouldPaginate`: true while the page has `pageSize` records and total < a hard cap
     (plan.maxResults when set).
   - `nextRequest`: increment offset by `pageSize`, rebuild `HttpRequest` with new query/body params.
4. `CursorPagination.java` — `public final class implements PaginationStrategy` (§6.5):
   - `continueExpression` (from `PaginationSpec.continueExpression`) evaluated against the last
     response to decide continuation and compute the per-page override.
   - `nextRequest`: apply the resolved cursor value into the request query/body.

## Design notes

- **Offset:** `inQuery` selects query-vs-body placement of `limit`/`offset` params.
- **Capping:** pagination must stop once `plan.maxResults` records have been collected (R8 interplay) —
  `shouldPaginate` returns false at the cap.
- **RootProperty:** the record list is extracted via `DynamicObjects.get(body, rootProperty)` so the
  strategy counts records consistently with post-receive.
- Keep the interface minimal; consumers can implement custom strategies.

## Acceptance criteria

- Compiles + Checkstyle green.
- `OffsetPaginationTest` (with `FakeTransport`):
  - 3 pages of `pageSize=10` → 3 requests with increasing `offset` (0,10,20), `limit=10`.
  - Stops when a page returns fewer than `pageSize` records.
  - Stops at `plan.maxResults` cap (does not over-fetch).
  - `inQuery=false` puts `limit`/`offset` in the body instead.
- `CursorPaginationTest`:
  - `continueExpression` true → next request carries the resolved cursor; false → stop.
- No unused imports; `final` everywhere; method length < 150.

## Hand-off to next task

Task 13 (engine) detects a `PaginationStrategy` in `RequestPlan.pagination` and loops
`send → collect → shouldPaginate → nextRequest → send`. Keep `PaginationStrategy` method names and
`PaginationContext` fields stable.
