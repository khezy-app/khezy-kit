# Task 04 — Expression SPI + JEXL + `doa.*` namespace (`expr/`)

## Objective

Implement the **per-item expression evaluator** (R5): an `ExpressionEvaluator` SPI, its JEXL default,
an `EvaluationScope` carrying the `$` bindings, and the `DoaNamespace` that exposes `dynamic-object`
as `doa.*` inside expressions. This is the bridge between the spec's `Expression` strings and real
resolved values per input item.

## Hand-off context

- **Design doc:** §2 (`expr/` tree), §6.1 (full contract + `DoaNamespace` + JexlEngineFactory),
  §5 R5, §7 (no secrets in expressions). Dot-notation shares `dynamic-object` semantics.
- **Already done (prior tasks):** Task 01 provides `spec.Expression` (the `=...` / `{{...}}` marker
  wrapper) and `spec.Send`/`PostReceive` that reference expression strings. This task consumes
  `Expression`'s conventions but stands alone.
- **Package:** `io.github.khezyapp.dhttp.expr`.
- **Dependencies:** `org.apache.commons:commons-jexl3:3.7.0`, `com.github.jknack:handlebars:4.5.3`,
  `io.github.khezyapp:dynamic-object:1.0.1` — all already in `build.gradle`.
- **dynamic-object API:** `io.github.khezyapp.doa.DynamicObjects` exposes static
  `get(target, path)`, `set(target, path, value)`, `remove(target, path)` and the constant
  `DynamicObjects.OBJECT_ACCESSOR` (an `ObjectAccessor`). Use those.
- **Conventions:** interfaces + `final` classes; cached JexlEngine. Read
  `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/expr/`)

1. `ExpressionEvaluator.java` — `public interface`:
   - `boolean isExpression(String value)` — true if starts with `=` or is a `{{...}}` template.
   - `<T> T evaluate(String expression, EvaluationScope scope, Class<T> type)`.
2. `EvaluationScope.java` — mutable binding holder:
   - `bind(String name, Object value)`, `Object get(String name)`, `Map<String,Object> bindings()`.
   - Predefine the standard `$` keys as constants: `$credentials`, `$parameter`, `$response`,
     `$responseItem`, `$value`, `$env`, `$item`, `$index`, `$parent`.
3. `jexl/JexlExpressionEvaluator.java` — default impl of `ExpressionEvaluator`:
   - `=`-prefixed → strip `=` and evaluate the rest as a JEXL expression against a cached `JexlEngine`.
   - `{{...}}` → Handlebars template interpolation against the same scope bindings.
   - Delegate engine creation to `JexlEngineFactory`.
4. `jexl/JexlEngineFactory.java` — `cached(ScopeCustomizer)` that builds/returns one `JexlEngine`
   (memoized) and registers:
   - a `doa` namespace backed by `DoaNamespace(DynamicObjects.OBJECT_ACCESSOR)`;
   - the `$` bindings as a Jexl `MapContext` per evaluation.
   Expose a functional `interface ScopeCustomizer` or a builder so consumers can bind extra vars.
5. `DoaNamespace.java` — stateless wrapper (per §6.1):
   `Object get(Object target, String path)`, `Object set(Object target, String path, Object value)`,
   `Object remove(Object target, String path)` delegating to an injected `ObjectAccessor`.

## Design notes

- **JEXL namespace syntax:** after registering `namespace("doa", ns)`, expressions call
  `doa.get($response, "data.items[0].id")`.
- **Thread safety:** the `JexlEngine` is shared/immutable once created (cache it); per-evaluation
  bindings use a fresh `MapContext` so concurrent requests don't collide.
- **`isExpression`:** strings NOT starting with `=` and containing no `{{...}}` are treated as
  literals; evaluate returns the value as-is (typed).
- Do **not** evaluate secrets; expressions are resolved against scope bindings only.

## Acceptance criteria

- Compiles + Checkstyle green under `./gradlew :declarative-http:build`.
- `JexlExpressionEvaluatorTest`:
  - `evaluate("=" + "$parameter.count", scope, Integer.class)` returns the bound param.
  - `evaluate("=" + "doa.get($response, \"data.items[0].id\")", scope, Object.class)` navigates a
    nested Map using `dynamic-object` (proves the `doa.*` namespace).
  - A `{{...}}` Handlebars template interpolates a binding.
  - `isExpression` returns true for `=`/`{{` and false for plain `"hello"`.
  - Concurrent `evaluate` from a small thread pool yields correct independent results (scope isolation).
- No unused imports; `final` everywhere; method length < 150 (split large factory methods).

## Hand-off to next task

Tasks 07 (planner), 11 (actions), 12 (pagination) call `ExpressionEvaluator.isExpression` and
`evaluate`. Keep `EvaluationScope` keys as the documented `$` names so those tasks bind consistently.
