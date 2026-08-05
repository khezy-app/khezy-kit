# Extending AST Expression Core

A guide for consumers who want to build on top of this library — custom evaluators, error codes, attributes, and rule engines.

---

## How This Library Thinks About Expressions

Before writing an evaluator, it helps to understand the mental model.

An AST node is just an expression. Like a Java expression, it returns a value. The type of that value depends on what the node does:
- `Node.constant(42)` returns `Integer`
- `Node.function(CoreFunctions.ADD, ...)` returns `Number`
- `Node.function(CoreFunctions.STRING_CONTAINS, ...)` returns `Boolean`

Because nodes compose as a tree, any child position can hold any expression — as long as that expression returns a value the parent evaluator knows how to handle. This is exactly how Java works:

```java
// In Java, any expression returning int works here:
int x = (a + b) * (c - d);

// In AST, any node returning Number works here:
//   add(multiply(a, b), divide(c, d))
Node.function(CoreFunctions.ADD,
    Node.function(CoreFunctions.MULTIPLY, a, b),
    Node.function(CoreFunctions.DIVIDE, c, d));
```

Your evaluator receives already-evaluated values. You never call `evaluate()` yourself. The engine walks the tree, evaluates every child, collects the results into an `Arguments` object, and hands it to you. You just transform those values into an output.

---

## Your Evaluator Contract

Every evaluator implements one method:

```java
EvaluationOutcome evaluate(EvaluationContext ctx, Arguments args);
```

The `Arguments` object gives you two collections:
- **`args.positional()`** — `List<Object>` from child nodes in order
- **`args.named()`** — `Map<String, Object>` from named child nodes

You read what you need, compute, and return `EvaluationOutcome`.

---

## Choosing: Positional vs Named Arguments

| Use positional when | Use named when |
|---|---|
| Arguments have a natural order (left operand, right operand) | Arguments have meaning independent of order (table name, field name) |
| The number of arguments is fixed and small | The expression needs to skip some arguments or accept optional ones |
| The evaluator is a pure operation (add, equals, not) | The evaluator carries configuration or metadata |

**Examples:**

```java
// Positional — add(left, right): order matters
Node.function(CoreFunctions.ADD, left, right);

// Named — dbFieldAccess(tableName, fieldName, path): order does not matter
Node.function(CoreFunctions.DB_FIELD_ACCESS, List.of(), Map.of(
    "tableName", Node.constant("orders"),
    "fieldName", Node.constant("email"),
    "path", Node.constant(List.of("customer"))
));
```

The rule is simple: if you'd write it as a method parameter in Java, make it positional. If you'd put it in a config object or builder, make it a named argument.

---

## Step-by-Step: Implementing a Custom Evaluator

Let's build a `score` evaluator that computes a risk score from age and income.

### Step 1: Define your error codes (optional)

```java
package com.myapp.rules;

import io.github.khezyapp.ast.core.error.ErrorCode;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;
import java.util.Map;

public class RiskScoreEvaluator implements Evaluator {

    // Custom error code — unique across your system
    private static final ErrorCode THRESHOLD_EXCEEDED =
        ErrorCode.of("THRESHOLD_EXCEEDED", "Score exceeds allowed threshold");

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        // ...
    }
}
```

**Guidelines:**
- Use `StandardErrors` constants for common problems: `WRONG_ARG_COUNT`, `MISSING_NAMED_ARG`, `NULL_NOT_ALLOWED`, `RUNTIME_ERROR`
- Define custom `ErrorCode` as `static final` in your evaluator class for domain-specific situations
- Error code format: `UPPER_SNAKE_CASE`, 3–40 characters
- Always include a `source` parameter to pinpoint the offending argument: `"arg[0]"`, `"named:fieldName"`, `"positional"`

### Step 2: Read your arguments from the `Arguments` object

```java
@Override
public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                  final Arguments args) {
    // Read positional arguments
    final var age = args.positional().get(0);
    final var income = args.positional().get(1);

    // Read named arguments (optional config)
    final var weights = (Map<String, Number>) args.named().getOrDefault(
        "weights", Map.of("age", 0.3, "income", 0.7));

    // Validate
    if (age == null || income == null) {
        return EvaluationOutcome.failure(EvaluationError.of(
            StandardErrors.MISSING_NAMED_ARG,
            "Age and income are required", "positional"));
    }
    // ...
}
```

Your evaluator receives whatever the child nodes returned. Because the AST is just expressions, those children can be constants, payload lookups, database queries, or even other custom evaluators:

```java
// The AST builder decides what goes in each slot:
Node.function(myScoreFn,
    Node.function(CoreFunctions.PAYLOAD, Map.of("fieldName", Node.constant("age"))),
    Node.function(CoreFunctions.DB_FIELD_ACCESS, ...)
);
```

Your evaluator does not care how the values were produced. It only cares what types they are.

### Step 3: Compute and return `EvaluationOutcome`

```java
    final double ageScore = ((Number) age).doubleValue() * weights.get("age").doubleValue();
    final double incomeScore = ((Number) income).doubleValue() * weights.get("income").doubleValue();
    final double total = ageScore + incomeScore;

    if (total > 1000) {
        return EvaluationOutcome.failure(EvaluationError.of(
            THRESHOLD_EXCEEDED, "Risk score " + total + " exceeds 1000"));
    }

    return EvaluationOutcome.success(total);
}
```

### Putting it all together

```java
public class RiskScoreEvaluator implements Evaluator {
    private static final ErrorCode THRESHOLD_EXCEEDED =
        ErrorCode.of("THRESHOLD_EXCEEDED", "Score exceeds allowed threshold");

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var age = args.positional().get(0);
        final var income = args.positional().get(1);
        final var weights = (Map<String, Number>) args.named().getOrDefault(
            "weights", Map.of("age", 0.3, "income", 0.7));

        if (age == null || income == null) {
            return EvaluationOutcome.failure(EvaluationError.of(
                StandardErrors.WRONG_ARG_COUNT,
                "Age and income positional arguments are required", "positional"));
        }

        final double ageScore = ((Number) age).doubleValue() * weights.get("age").doubleValue();
        final double incomeScore = ((Number) income).doubleValue() * weights.get("income").doubleValue();
        final double total = ageScore + incomeScore;

        if (total > 1000) {
            return EvaluationOutcome.failure(EvaluationError.of(
                THRESHOLD_EXCEEDED, "Risk score " + total + " exceeds 1000"));
        }

        return EvaluationOutcome.success(total);
    }
}
```

---

## When and Why to Attach Attributes

Attributes are metadata attached to a single evaluation step. They propagate into `EvaluationResult.attributes` so consumers can inspect them when walking the result tree.

### Attach attributes when:

| Scenario | Example | Attribute key |
|---|---|---|
| You want to show *why* a value was produced | A score evaluator attaching sub-score breakdown | `evidence.scoreBreakdown` |
| You want to expose performance data | A DB evaluator attaching rows scanned | `meta.rowsScanned` |
| You want to help debugging | A string evaluator attaching the actual input/pattern | `debug.inputValue` |
| You want to track which rule produced this | A rule evaluator attaching rule identity | `audit.ruleId` |

### Do NOT attach attributes when:

- Your evaluator is a pure function (add, equals, not) — return `EvaluationOutcome.success(value)` with no attributes
- The information is already in the child results — consumers can walk the tree

### Attribute key convention:

```
<domain>.<category>[.<subcategory>]
```

- `evidence.*` — Compliance data (records, scores, snapshots)
- `audit.*` — Audit trail (rule ID, version, trace ID)
- `meta.*` — Performance/cardinality (rows scanned, duration, SQL)
- `debug.*` — Diagnostic information (intermediate values)

**Keep evidence maps small by reference.** If you produce 10 000 source records, do not embed them:

```java
return EvaluationOutcome.success(sum, Map.of(
    "evidence.recordCount", 10_000,
    "evidence.recordsRef",  "/evidence/batch-123.json"
));
```

Attributes do NOT automatically merge upward. Each node carries only what its own evaluator attached. To collect all attributes from the tree:

```java
for (var node : result.flatten()) {
    if (node.hasAttribute("evidence.recordCount")) {
        totalRecords += (int) node.getAttribute("evidence.recordCount");
    }
}
```

---

## Registering Your Evaluator

Create a `FunctionRegistry` (empty or with builtins), build a `FunctionDefinition`, and register it:

```java
var registry = FunctionRegistry.empty(NullStrategies.PROPAGATE);

registry.register(FunctionDefinition.builder()
    .functionId(FunctionId.of("my:riskScore"))
    .evaluator(new RiskScoreEvaluator())
    .positionalParam(ParamSpec.required("age", ParamType.ANY))
    .positionalParam(ParamSpec.required("income", ParamType.ANY))
    .namedParam(ParamSpec.optional("weights", ParamType.MAP))
    .nullStrategy(NullStrategies.FAIL)    // optional per-function override
    .build());
```

| Builder method | Purpose |
|---|---|
| `.functionId(id)` | How the AST references this evaluator |
| `.evaluator(impl)` | Your evaluator instance |
| `.positionalParam(spec)` | Declares a positional parameter for validation |
| `.namedParam(spec)` | Declares a named parameter for validation |
| `.attributes(attrs)` | Short-circuit, commutativity, cost hints |
| `.nullStrategy(strategy)` | Override the default null strategy for this function |

---

## Building a Rule Engine on Top

The library does not ship a rule engine — it ships the expression evaluator that a rule engine needs. You build the rule part yourself.

The pattern is straightforward:

### Rule = Condition + Action

```java
public record Rule(String name, Node condition, Node action, int priority) {}
```

A condition is an AST node that evaluates to a boolean. An action is an AST node that evaluates to some result (a score, a label, a decision).

### Rule Engine = Many Rules to Execute

```java
public class RuleEngine {
    private final FunctionRegistry registry;
    private final List<Rule> rules;
    private final AstEvaluator evaluator = new AstEvaluator();

    public RuleEngine(final FunctionRegistry registry,
                      final List<Rule> rules) {
        this.registry = registry;
        this.rules = rules;
    }

    public List<RuleResult> evaluate(final Object payload) {
        final var ctx = new EvaluationContext.Builder(registry)
            .body(payload)
            .build();

        final var results = new ArrayList<RuleResult>();

        for (final var rule : rules) {
            // Evaluate condition
            final var condResult = evaluator.evaluate(rule.condition(), ctx);

            if (Boolean.TRUE.equals(condResult.returnValue())) {
                // Condition matched — evaluate action
                final var actionResult = evaluator.evaluate(rule.action(), ctx);
                results.add(new RuleResult(rule.name(), actionResult, condResult));
            }
        }

        return results;
    }
}
```

### Using `and` / `or` as the root condition

Combine multiple conditions using the built-in boolean evaluators:

```java
// Rule: "Flag transactions over $10 000 from high-risk countries"
var highRiskRule = new Rule(
    "high-value-foreign",
    Node.function(CoreFunctions.AND,
        // amount > 10000
        Node.function(CoreFunctions.GREATER_THAN,
            Node.function(CoreFunctions.PAYLOAD,
                Map.of("fieldName", Node.constant("amount"))),
            Node.constant(10000)),
        // risk_country = true (from payload lookup)
        Node.function(CoreFunctions.EQUAL,
            Node.function(CoreFunctions.PAYLOAD,
                Map.of("fieldName", Node.constant("risk_country"))),
            Node.constant(true))
    ),
    // Action: set flag to "manual_review"
    Node.constant("manual_review"),
    10
);
```

### Putting the options together

Use `or` when any condition is sufficient, `and` when all must match:

```java
// Any of these conditions triggers the rule
Node condition = Node.function(CoreFunctions.OR,
    amountAboveThreshold,
    isHighRiskCountry,
    isPoliticallyExposed
);
```

Because conditions are just AST expressions, you can compose them arbitrarily — the same way you compose boolean expressions in Java with `&&` and `||`.

### Minimal rule engine example

```java
// 1. Create registry with builtin evaluators
var registry = FunctionRegistry.withBuiltins(NullStrategies.PROPAGATE);

// 2. Add domain-specific evaluators
registry.register(FunctionDefinition.builder()
    .functionId(FunctionId.of("my:riskScore"))
    .evaluator(new RiskScoreEvaluator())
    .positionalParam(ParamSpec.required("age", ParamType.ANY))
    .positionalParam(ParamSpec.required("income", ParamType.ANY))
    .build());

// 3. Define rules
var rules = List.of(
    new Rule("high-value", highValueCondition, Node.constant("review"), 10),
    new Rule("low-risk", lowRiskCondition, Node.constant("approve"), 20)
);

// 4. Execute
var engine = new RuleEngine(registry, rules);
var results = engine.evaluate(payload);
```

---

## Rules of Thumb

1. **Never throw from an evaluator** — always return `EvaluationOutcome.failure(...)`. The engine catches unexpected exceptions and wraps them as `StandardErrors.RUNTIME_ERROR`.

2. **Your evaluator receives already-evaluated values** — you never call `evaluate()` on child nodes. The engine handles recursion.

3. **Don't validate what the engine already validates** — the engine checks argument count and required named args before calling your evaluator. Your evaluator only needs to validate business logic.

4. **Pure functions don't need attributes** — arithmetic, equality, and boolean evaluators return `EvaluationOutcome.success(value)` with `Map.of()`.

5. **Named arguments for configuration, positional for data** — if it looks like a config key, make it named. If it looks like an operand, make it positional.

6. **Null strategy is a safety net, not validation** — set `NullStrategies.FAIL` on functions that must never receive null. Use `COERCE_DEFAULT` for functions where null means "use a sensible default". Use `PROPAGATE` when null should flow through as a valid value.
