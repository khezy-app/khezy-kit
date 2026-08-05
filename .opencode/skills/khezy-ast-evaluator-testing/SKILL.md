---
name: khezy-ast-evaluator-testing
description: "Patterns for testing evaluators and the evaluation pipeline in ast-expression-core. Covers anonymous stubs, custom registration, dry-run, and defensive copy gotchas."
---

Use this skill when writing tests for the `ast-expression-core` module's evaluators, pipeline, or result types.

## No Mockito — use anonymous class stubs

This project does not use Mockito. Stub external dependencies with anonymous inner class implementations:

```java
final var dbAccessor = new DbAccessor() {
    @Override public List<Map<String, Object>> executeQuery(final DbQuery query) {
        throw new UnsupportedOperationException("Should not be called in dry run");
    }
    @Override public DryRunResult dryRunQuery(final DbQuery query) {
        return new DryRunResult(List.of("col1", "col2"), 100L, "SELECT ...");
    }
    @Override public SchemaRegistry getSchemaRegistry() { return null; }
};
```

Throw `UnsupportedOperationException` for methods the test should not invoke — this catches unintended calls.

## Register custom evaluators manually

Evaluators that require external dependencies (DbAccessor, SchemaRegistry) are **not** pre-registered in `FunctionRegistry.withBuiltins()`. Register them in the test:

```java
final var registry = FunctionRegistry.empty(NullStrategies.PROPAGATE);
registry.register(FunctionDefinition.builder()
    .functionId(CoreFunctions.DB_ACCESS)
    .evaluator(new DbAccessEvaluator(dbAccessor))
    .positionalParam(ParamSpec.required("table", ParamType.ANY))
    .namedParam(ParamSpec.optional("columns", ParamType.ANY))
    .build());
```

### Dry-run test setup

```java
final var ctx = new EvaluationContext.Builder(registry)
    .body(Map.of())
    .dryRun(true)
    .build();
final var result = evaluator.evaluate(node, ctx);
assertTrue((boolean) result.getAttribute("meta.dryRun"));
```

### Normal execution test (expects error when stub throws)

```java
final var ctx = new EvaluationContext.Builder(registry)
    .body(Map.of())
    .dryRun(false)
    .build();
final var result = evaluator.evaluate(node, ctx);
assertFalse(result.errors().isEmpty());  // stubs throw UnsupportedOperationException
```

## Defensive copying: `List.copyOf` rejects nulls

**`List.copyOf()` throws `NullPointerException` if the collection contains null elements.** This is not obvious and caused a production bug.

```java
// THROWS NPE — positional may contain nulls (e.g., constant(null), PROPAGATE strategy)
return new Arguments(List.copyOf(positional), Map.copyOf(named));

// FIX — null-safe alternative
return new Arguments(
    Collections.unmodifiableList(new ArrayList<>(positional)),
    Map.copyOf(named));
```

`Map.copyOf()` is safe — Maps tolerate null values. Only `List.copyOf` is the problem.

## AstEvaluator cache: check before early-return

The cache lookup (`ctx.cache().get(hash)`) must happen before any early-return path in `AstEvaluator.evaluate()`. Originally the constant leaf returned before checking the cache, so constants were never cached. The correct order is:

```java
final long hash = node.hashCode();
final var cache = ctx.cache();
if (Objects.nonNull(cache)) {
    final var cached = cache.get(hash);
    if (Objects.nonNull(cached)) {
        return cached.withTrace(new EvaluationTrace(false, true, 0));
    }
}
// ... constant path, function path ...
```

## Test conventions specific to ast-expression-core

- Use `@BeforeEach` to create `FunctionRegistry` and `AstEvaluator` fresh per test
- Builder helper methods for `EvaluationContext` keep tests DRY
- Use `ctxWithBody(body)` for payload tests, `ctxWithCache(cache)` for caching tests
- Short-circuit tests verify `result.children().get(n).isSkipped()` for skipped positions
- Error propagation tests verify `result.flattenErrors()` aggregates nested errors
