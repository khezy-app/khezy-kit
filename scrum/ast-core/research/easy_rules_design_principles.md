# Rule Engine Design Principles — From easy-rules to a Reusable Core Library

## 1. Scope and Motivation

This document extracts design principles from the [easy-rules](https://github.com/j-easy/easy-rules) codebase and bridges them with a generalized AST-based evaluation model. The goal is to define a reusable core rule-engine library that can serve AML rule engines, Customer Risk-Rating, KYC scoring, and similar use cases — without each project reinventing the evaluation runtime.

## 2. Core Separation: Rule Structure vs. Rule Execution

The single most important design insight from easy-rules is the separation between what a rule *is* and how it *runs*.

```
Rule interface (what)          RulesEngine (how)
┌──────────────────────┐      ┌───────────────────────┐
│ evaluate(Facts)       │      │ fire(Rules, Facts)    │
│ execute(Facts)        │      │ check(Rules, Facts)   │
│ getName()             │      │ getParameters()       │
│ getPriority()         │      │ RuleListeners         │
└──────────────────────┘      └───────────────────────┘
```

**Principle:** The rule describes the decision logic; the engine controls the execution strategy (order, skipping, inference loops, listener hooks).

In easy-rules, `DefaultRulesEngine` is one strategy (linear priority order), `InferenceRulesEngine` is another (loop-until-stable). The `Rule` interface is shared between both. This separation allows the same rules to be evaluated under different orchestration strategies without modification.

## 3. Condition-Action Decomposition

easy-rules decomposes each rule into two independently pluggable units:

- **Condition**: A pure predicate `Facts -> boolean` (functional interface).
- **Action**: A side-effecting procedure `Facts -> void` (functional interface).

```
Rule
├── Condition (evaluate: Facts → boolean)
├── Action[]  (execute: Facts → void, one or more)
├── name: String
├── description: String
└── priority: int
```

**Principle:** Decoupling Condition from Action enables three key reuse scenarios:
1. Same condition can trigger different actions in different rules.
2. Conditions can be composed (AND, OR, NOT) without changing the action logic.
3. The engine can evaluate conditions (via `check()`) without executing actions — useful for dry-run, scoring, or audit.

easy-rules implements this in `DefaultRule` (package-private) and `RuleBuilder`, and also provides `Condition.FALSE` and `Condition.TRUE` as no-op constants.

## 4. Facts as a Named Namespace

Facts are not just a `Map<String, Object>`. They are a named namespace with:

- Uniqueness constraint: Fact names are unique within a `Facts` instance.
- Immutability of identity: `Fact` equality is based on name only.
- Convenience access: `facts.get("name")`, `facts.put("name", value)`, `facts.asMap()`.

```java
facts.put("temperature", 30);
facts.put("person", new Person("Tom", 17));
```

**Principle:** The fact namespace should be discoverable and traceable. In easy-rules, `Facts` implements `Iterable<Fact<?>>`, so listeners and tools can enumerate all known facts. This is critical for debugging, auditing, and rule explanation — requirements common in AML and KYC domains.

## 5. Priority-Based Rule Ordering

easy-rules uses a `TreeSet<Rule>` (natural ordering by `compareTo`) to maintain rule priority. Lower priority numbers fire first.

```java
@Override
public int compareTo(Rule rule) {
    if (getPriority() < rule.getPriority()) return -1;
    if (getPriority() > rule.getPriority()) return 1;
    return getName().compareTo(rule.getName());
}
```

**Principle:** Rules should have an explicit, deterministic execution order. Priority provides:
1. Conflict resolution when multiple rules match the same facts.
2. Fallback chains (e.g., "if not matched by any specific rule, use default").
3. Priority thresholding (`RulesEngineParameters.priorityThreshold`) — stop execution after a certain priority level, enabling staged evaluation.

## 6. Pluggable Expression Language via Strategy Pattern

The most architecturally significant pattern in easy-rules is how it decouples rule semantics from expression language. The core defines `Condition` and `Action` as interfaces; each expression language module provides implementations:

```
Core API:            Condition  ───  Action
                          ▲              ▲
                          │              │
JEXL module:       JexlCondition    JexlAction
MVEL module:       MVELCondition    MVELAction  
SpEL module:       SpELCondition    SpELAction
```

Each `XxxCondition`/`XxxAction` follows the same pattern:
1. Accept expression string at construction time.
2. Compile it using the respective language engine.
3. On `evaluate`/`execute`, convert `Facts` to a map and evaluate.

```java
// MVELCondition
compiledExpression = MVEL.compileExpression(expression);

public boolean evaluate(Facts facts) {
    return (boolean) MVEL.executeExpression(compiledExpression, facts.asMap());
}
```

**Principle:** The evaluation of conditions and actions should be pluggable. The core defines the contract; concrete implementations handle:
1. Expression parsing and compilation.
2. Variable binding from facts to expression context.
3. Execution semantics (boolean for conditions, void for actions).

This enables a "core + adapter" architecture where the rule engine core has zero dependency on any expression language. MVEL, JEXL, SpEL, or even a custom DSL are all legitimate choices depending on the domain.

## 7. External Rule Descriptors

easy-rules supports defining rules in external YAML and JSON files through a layered pipeline:

```
YAML/JSON file
    ↓
RuleDefinitionReader (YamlRuleDefinitionReader / JsonRuleDefinitionReader)
    ↓  reads into
RuleDefinition (POJO: name, condition, actions, composingRules)
    ↓
AbstractRuleFactory.createRule(RuleDefinition)
    ↓  dispatches to
createSimpleRule() / createCompositeRule()
    ↓
XxxRule (MVELRule, SpELRule, JexlRule, etc.)
```

The `RuleDefinition` is the canonical intermediate representation:
```yaml
name: "alcohol rule"
condition: "person.isAdult() == false"
actions:
  - "System.out.println(\"Sorry, you are not allowed to buy alcohol\");"
```

**Principle:** Separate rule *definition* (descriptive, serializable) from rule *implementation* (executable). The `RuleDefinition` serves as a format-agnostic intermediate representation that can be produced from YAML, JSON, database, or programmatic construction.

This is a key insight for a reusable core library: if we define a portable AST or definition format, we can decouple rule authoring (UI, file, API) from rule execution entirely.

## 8. Composite Rules

easy-rules provides three composite rule types that implement the `Rule` interface and wrap child rules:

| Composite Type | Semantic | Behavior |
|---|---|---|
| `UnitRuleGroup` | All-or-nothing | All children must evaluate to true; then all execute |
| `ActivationRuleGroup` | XOR / first-match | First child that evaluates to true executes; others skipped |
| `ConditionalRuleGroup` | If-then | First child (highest priority) acts as gate; if true, remaining children that also evaluate to true execute |

**Principle:** Rule composition should be first-class, not ad-hoc. Composite rules implement the same `Rule` interface as leaf rules, making them transparent to the engine. This enables hierarchical rule structures:

```
Rules (namespace)
├── Rule A (leaf)
├── Rule B (leaf)
└── UnitRuleGroup
    ├── Rule C (leaf, required)
    └── Rule D (leaf, required)
```

The composite pattern is essential for domains like KYC where identity verification, AML screening, and risk scoring are composite checks.

## 9. Engine Parameters and Listener Hooks

easy-rules provides two orthogonal extension mechanisms:

### Engine Parameters (configuration, not code)
```java
RulesEngineParameters params = new RulesEngineParameters()
    .skipOnFirstAppliedRule(true)
    .skipOnFirstFailedRule(false)
    .priorityThreshold(100);
```

### Listeners (events, not logic)
```java
interface RuleListener {
    boolean beforeEvaluate(Rule, Facts);       // veto evaluation
    void afterEvaluate(Rule, Facts, boolean);  // observe result
    void onEvaluationError(Rule, Facts, Exception);
    void beforeExecute(Rule, Facts);
    void onSuccess(Rule, Facts);
    void onFailure(Rule, Facts, Exception);
}
```

**Principle:** Engine behavior should be configurable through parameters (policy) and observable through listeners (telemetry). These mechanisms should be orthogonal to the rule logic itself. This is especially important for AML/KYC scenarios where every evaluation must be auditable and configurable per jurisdiction.

## 10. Inference Engine: Iterative Evaluation

The `InferenceRulesEngine` demonstrates a fundamentally different execution strategy using the same `Rule` interface:

```java
// InferenceRulesEngine
do {
    selectedRules = selectCandidates(rules, facts); // re-evaluate ALL rules
    if (!selectedRules.isEmpty()) {
        delegate.fire(selectedRules, facts);        // fire candidates
    }
} while (!selectedRules.isEmpty());
```

**Principle:** The same rule set can be executed under different strategies without modification. The default engine is "fire once per rule"; the inference engine is "fire until quiescence." This maps directly to business patterns:
- Default engine: Scoring (evaluate all rules, accumulate score).
- Inference engine: State-machine transitions (each rule changes facts, triggering more rules).

## 11. Annotation-Based Rule Definition

easy-rules supports defining rules declaratively via annotations, with `RuleProxy` bridging annotated POJOs to the `Rule` interface:

```java
@Rule(name = "weather rule")
public class WeatherRule {
    @Condition
    public boolean itRains(@Fact("rain") boolean rain) {
        return rain;
    }

    @Action
    public void then(@Fact("rain") boolean rain) {
        System.out.println("It rains!");
    }
}
```

`RuleProxy` uses JDK dynamic proxy to intercept `Rule` interface methods and dispatch to annotated methods. It handles:
- Fact injection via `@Fact("name")` parameter binding.
- Priority extraction from `@Priority` method / `@Rule.priority()` attribute.
- Action ordering via `@Action(order)`.

**Principle:** Multiple rule definition styles (annotations, builder, expression languages, external files) should coexist and produce the same `Rule` interface. The engine should not care how a rule was defined.

## 12. Architecture for a Reusable Core Library

### Current easy-rules architecture

```
┌─────────────────────────────────────────────┐
│                RulesEngine                    │
│  (DefaultRulesEngine / InferenceRulesEngine)  │
├─────────────────────────────────────────────┤
│                  Rule                         │
│  ┌───────────────┐  ┌─────────────────────┐  │
│  │ Condition     │  │ Action[]            │  │
│  │ (functional)  │  │ (functional)        │  │
│  └───────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────┤
│             Facts (named namespace)          │
├─────────────────────────────────────────────┤
│  Pluggable Expression Adapters               │
│  MVEL │ JEXL │ SpEL                           │
└─────────────────────────────────────────────┘
```

### Proposed reusable core library extension

```
┌──────────────────────────────────────────────────────┐
│              Execution Strategies                      │
│  DefaultEngine │ InferenceEngine │ ScoringEngine       │
├──────────────────────────────────────────────────────┤
│                  Rule (interface)                      │
│  ┌────────────────────┐  ┌─────────────────────────┐  │
│  │ Condition          │  │ Action                  │  │
│  │ └─ boolean expr    │  │ └─ side-effect          │  │
│  │ └─ AST Node        │  │ └─ AST Node             │  │
│  │ └─ composite (AND) │  │                         │  │
│  └────────────────────┘  └─────────────────────────┘  │
├──────────────────────────────────────────────────────┤
│              Evaluation Context                        │
│  Facts (input) + Cache + Trace + Scope                │
├──────────────────────────────────────────────────────┤
│              AST / Expression Model                    │
│  Node: Function │ Constant │ Children │ NamedChildren │
├──────────────────────────────────────────────────────┤
│           Function Registry (pluggable evaluators)     │
│  eq │ gt │ and │ or │ not │ score │ filter │ ...     │
├──────────────────────────────────────────────────────┤
│           Definition Layer (format-agnostic)           │
│  YAML │ JSON │ Database │ API                          │
└──────────────────────────────────────────────────────┘
```

### Core components

| Layer | Responsibility | Key Interfaces |
|---|---|---|
| **Rule** | Decision logic unit | `evaluate(Context) -> Result`, `execute(Context) -> void` |
| **Condition** | Pure predicate | `evaluate(Context) -> EvaluationResult` |
| **Action** | Side-effect | `execute(Context) -> ExecutionResult` |
| **Engine** | Orchestration strategy | `fire(Rules, Facts)`, `check(Rules, Facts)` |
| **Context** | Evaluation environment | Facts + cache + trace + variable scope |
| **AST** | Expression intermediate representation | Node (function, constant, children, namedChildren) |
| **Function Registry** | Pluggable evaluators | `evaluate(Node, Context) -> Value` |
| **Rule Definition** | Format-agnostic descriptor | name, condition, actions, priority, composingRules |

### Design rules

1. **Rule is the unit of reuse.** Everything — leaf rule, composite rule, function, condition, action — should be expressible through the Rule interface.

2. **Condition and Action are independently pluggable.** They should be composable (AND, OR, NOT for conditions; sequence for actions).

3. **Facts are a typed namespace.** Not just a map. They should support nesting, scoping, and tracing.

4. **Expression evaluation is replaceable.** The core defines the AST model; concrete evaluators (MVEL, SpEL, custom DSL) plug in via the function registry.

5. **Execution strategy is orthogonal to rule definition.** The same rules can run in default mode, inference mode, scoring mode, or batch mode.

6. **Everything is observable.** Listeners at engine level, rule level, and expression level for debugging, auditing, and explanation.

7. **Rule definition is format-agnostic.** A `RuleDefinition` intermediate representation decouples authoring format from execution.

8. **Composite rules are first-class.** Grouping, activation, and conditional semantics should be built into the core, not an afterthought.

9. **Evaluation produces a rich result.** Returning just a boolean is insufficient. The engine should capture errors, child results, and trace metadata.

10. **Priorities drive deterministic ordering.** Explicit priority values, thresholding, and unique name fallback ensure predictable execution.

## 13. Comparison: easy-rules vs. Proposed Core

| Concern | easy-rules | Proposed Core |
|---|---|---|
| Expression representation | Opaque strings | AST with function registry |
| Condition composition | None (single boolean) | AND, OR, NOT, composite conditions |
| Action composition | Ordered list | Ordered list + conditional branching |
| Facts structure | Flat map | Nested typed namespace + scope |
| Evaluation result | `boolean` + side effects | Rich result with trace, errors, metadata |
| Short-circuiting | Engine-level (skip parameters) | Expression-level (per function) |
| Optimization | None | Optional caching, cost-based ordering |
| External rules | YAML/JSON via readers | YAML/JSON/database via definition pipeline |
| Domain extension | Via expression language | Via function registry + custom evaluators |
| Traceability | Listener hooks | Built into evaluation result |
