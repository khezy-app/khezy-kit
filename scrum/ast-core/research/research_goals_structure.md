# Research Goals & Feature Structure: AST-First Rule Engine Core

## Why This Direction

The core insight: **expressions (AST) are the universal primitive, not rules.**

Existing engines (easy-rules, Drools) couple condition-evaluation with action-execution at the rule level. This makes them hard to integrate when:
- **AML typologies**: need configurable scoring branches, not fixed actions
- **Risk-rating**: need to return *how* a score was computed (trace), not just a final boolean
- **Business rules**: need to express calculations, aggregations, and conditions uniformly

AST evaluation solves this because it is **value-returning** — not just boolean predicates. The same tree infrastructure handles:
- Conditions (returns boolean)
- Calculations (returns numeric)
- Compositions (AND/OR/NOT returns boolean)
- Scoring (returns aggregated value)
- Any domain function (returns whatever the domain needs)

## Core Architecture Layers

```
┌──────────────────────────────────────────────┐
│  Layer 4: Integration                         │
│  (rule definition format, serialization, API) │
├──────────────────────────────────────────────┤
│  Layer 3: Engine Strategies                   │
│  (orchestration: linear, inference, scoring)  │
├──────────────────────────────────────────────┤
│  Layer 2: Rule Abstraction                    │
│  (condition + action built on AST)            │
├──────────────────────────────────────────────┤
│  Layer 1: Evaluation Runtime                  │
│  (context, function registry, caching, trace) │
├──────────────────────────────────────────────┤
│  Layer 0: AST Model                           │
│  (Node, NodeEvaluation, expression tree)      │
└──────────────────────────────────────────────┘
```

Each layer builds on the layer below. A consumer can use just Layer 0–1 (pure expression evaluation with trace) or the full stack (Layer 0–4, rule engine with definitions).

---

## Layer 0: AST Model — The Expression Tree

**Goal:** Define a generic, composable expression tree that can represent any logical, arithmetic, or domain-specific operation.

### Core Features

| Feature | Description |
|---------|-------------|
| **Node types** | Function node (has children) and Constant node (leaf value) |
| **Node structure** | `function`, `constant`, `children` (positional), `namedChildren` (keyed) |
| **Node identity** | Each node is a data object — no behavior, no evaluation logic |
| **Tree composition** | Nodes nest arbitrarily to form expressions |
| **Serializable** | The tree must be serializable (JSON, YAML, binary) |

### Key Design Decisions

- The AST is a **data model, not a policy layer** — it describes *what*, not *how*
- Both positional and named children are supported (Marble pattern)
- Functions are identified by a symbol (string/enum), not by Java class
- The tree itself is immutable after construction

### Research Questions

1. Should Node be a sealed interface/class hierarchy, or a single flexible class with optional fields?
2. How to handle type metadata in the AST (optional type hints on children)?
3. Should constants support only primitives, or any serializable value?

---

## Layer 1: Evaluation Runtime — Walking the Tree

**Goal:** Execute an AST against input data and produce a value + trace.

### Core Features

| Feature | Description |
|---------|-------------|
| **Recursive evaluator** | Walk the tree depth-first: evaluate children, feed results to function evaluator |
| **Function registry** | Maps function symbols → evaluator implementations (pluggable, extensible) |
| **Evaluation context** | Holds facts/data, variable scope, cache, and configuration |
| **Short-circuit support** | Functions can signal "stop evaluating children" (AND/OR, switch) |
| **Trace / evaluation result** | Each evaluation returns value + errors + child results + metadata |
| **Error handling** | Per-node errors collected without aborting entire evaluation (graceful degradation) |

### Key Design Decisions

- The evaluator is **generic** — it knows how to walk the tree, but not what each function means
- Domain-specific behavior lives in **function evaluators**, not in the evaluator loop
- The evaluation context is the bridge between the AST (structure) and the input data (facts)
- Trace is built into the result, not bolted on as an afterthought

### Research Questions

1. How to design the function registry for both compile-time (known functions) and runtime (dynamic registration)?
2. Should short-circuit be a property of the function definition, or a hint in the AST node?
3. How to handle caching of sub-tree results without changing the evaluator's core loop?
4. What should the trace data structure look like for serialization to JSON (for the UI/audit trail)?

---

## Layer 2: Rule Abstraction — Condition + Action via AST

**Goal:** Define a Rule in terms of AST expressions, reusing the same evaluation machinery.

### Core Features

| Feature | Description |
|---------|-------------|
| **Condition expression** | An AST node that evaluates to a boolean (the "when") |
| **Action expression(s)** | AST nodes that evaluate to side-effects or results (the "then") |
| **Rule metadata** | name, description, priority, group |
| **Rule result** | The condition result + action results + full evaluation trace |
| **Composite rules** | AND-group, OR-group, conditional-gate (built on conditions) |

### Key Design Decisions

- A Rule is a **thin wrapper** over two AST roots: one for condition, one for action
- Condition and Action are independently evaluable (they are just ASTs)
- The rule abstraction exists so that engines can orchestrate (order, skip, group) without knowing AST internals
- Composite rules work because conditions are composable AST trees

### Research Questions

1. Is an Action fundamentally different from a Condition at the AST level, or is it just a convention?
2. Should actions be able to produce values (for scoring/computation) or should they be void?
3. How to model action sequences at the AST level (list of action nodes)?

---

## Layer 3: Engine Strategies — Orchestration

**Goal:** Provide reusable execution strategies that operate on rules and facts.

### Core Features

| Feature | Description |
|---------|-------------|
| **Linear engine** | Evaluate rules in priority order, configurable skip behavior |
| **Scoring engine** | Evaluate all rules, accumulate results into a score with trace |
| **Inference engine** | Loop: evaluate → fire → re-evaluate until stable |
| **Batch engine** | Evaluate rules against multiple fact sets in one pass |
| **Engine parameters** | Skip-on-first, priority threshold, max iterations |
| **Listeners / hooks** | beforeEvaluate, afterEvaluate, onError, beforeExecute, etc. |

### Key Design Decisions

- Engines operate on the `Rule` interface — they don't know about AST internals
- Different engines use the same rules; strategy is orthogonal to definition
- Listeners are the primary extension point for observability (audit, logging, metrics)

### Research Questions

1. Should the scoring engine be a separate strategy, or a configuration of the linear engine?
2. How to design the listener/observer API to expose evaluation traces?
3. How to handle rule dependencies (rule B depends on result of rule A)?

---

## Layer 4: Integration — Definitions and Formats

**Goal:** Decouple rule authoring from rule execution through a portable intermediate representation.

### Core Features

| Feature | Description |
|---------|-------------|
| **Rule definition** | A serializable descriptor (name, condition-AST, action-AST, metadata) |
| **Format readers** | Parse YAML/JSON/XML into rule definitions |
| **Rule factory** | Converts rule definitions into executable Rule objects |
| **AST serialization** | JSON schema for AST nodes (for UI builders or external tools) |

### Key Design Decisions

- The AST is the **canonical intermediate representation** — not a string, not a DSL
- Rule definitions are format-agnostic; YAML, JSON, database, API all produce the same definition object
- The factory pattern (from easy-rules) is preserved: `createSimpleRule()`, `createCompositeRule()`

### Research Questions

1. What does a JSON schema for AST nodes look like? How to represent functions, constants, and children?
2. Should the rule definition include the function registry configuration, or is that deployment-specific?
3. How to handle versioning of rule definitions?

---

## Use Case Mapping

| Use Case | How the AST Core Supports It |
|----------|------------------------------|
| **AML typology engine** | Typologies = composite rules with scoring functions. Trace output shows which typology rules matched and why. New typologies = new AST definitions + registered functions. |
| **Risk-rating with explanation** | AST evaluation returns a rich trace. The UI can render the trace as "Score = 85: base 50 + age factor 20 (age > 40) + income factor 15 (income > 100k)". No action coupling needed. |
| **Business rule expression** | Rules are just ASTs. Condition evaluates to boolean, or to a score, or to a category — whatever the domain needs. The engine doesn't care. |
| **Configurable decision logic** | Rules can be stored as JSON ASTs in a database. No recompilation, no redeployment. Just load and evaluate. |

---

## Comparison: This Approach vs. Existing Engines

| Concern | easy-rules | Drools | This Approach (AST-First) |
|---------|-----------|--------|--------------------------|
| Expression | Opaque string | DRL language | AST (structured, portable) |
| Evaluation result | boolean + side-effect | boolean + side-effect | Value + full trace |
| Extensibility | Via expression language | Via rule language | Via function registry |
| Trace | Listener only | Limited | Built into every evaluation |
| Scoring | Not native | Not native (agenda hack) | First-class: functions return numeric values |
| Action coupling | Condition → Action coupled | Condition → Action coupled | Condition and Action are both just ASTs |
| Integration burden | Must adapt to Condition/Action contract | Must adapt to DRL / KieSession | Integrate at the expression level only |
| Serialization | Strings | DRL text | JSON AST |

---

## Summary: What Makes This "Research"

The core library should prove that:

1. **Expressions (AST) are a sufficient primitive** — rules, conditions, actions, scoring, and traces can all be expressed as AST evaluation.
2. **The function registry makes it domain-agnostic** — AML, risk-rating, KYC, and generic business rules all share the same evaluation runtime.
3. **Rich trace is not a bolt-on** — it's a first-class output of every evaluation, enabling audit, explanation UI, and debugging without extra work.
4. **The stack is layered** — consumers can use just the AST evaluator (Layer 0–1) without committing to a full rule engine (Layer 2–4).

### Immediate Next Questions to Answer

1. Design the `Node` data model — single flexible class or sealed hierarchy?
2. Design the `EvaluationResult` — what fields must it have to support trace and UI rendering?
3. Design the function registry API — how does a consumer register a custom evaluator?
4. Design the evaluation loop — can it be a single generic method, or does it need pluggable strategies?

---

### Answered: Industry Practice Reference

Sources consulted:
- **Marble** (`/mnt/data/opensource/marble-backend/`): Production AML engine using AST evaluation. Primary reference.
- **easy-rules** (`/mnt/data/opensource/jeasy/easy-rules/`): Lightweight Java rule engine with expression language plugins (JEXL, MVEL, SpEL).
- **JEXL, MVEL, SpEL**: Well-known JVM expression language implementations. Used as secondary reference for AST representation patterns.

---

#### Q1: Node Data Model — Single Flexible Class vs. Sealed Hierarchy

| Approach | Who Uses It | Characteristics |
|----------|-------------|-----------------|
| **Single flexible class** | Marble (`Node` struct) | One class with optional `Constant`, `Children`, `NamedChildren`. `Function` enum discriminates constant vs. function node. |
| **Sealed hierarchy** | JEXL (`ASTNode` → `ASTAddNode`, `ASTAndNode`, ...), SpEL (`SpelNode` → `Literal`, `OpPlus`, ...), MVEL | Abstract base with typed subclasses per node type. |

**Marble's approach (reference implementation):**

```go
type Node struct {
    Index        int
    Function     Function          // FUNC_CONSTANT for constants, otherwise function ID
    Constant     any               // only used when Function == FUNC_CONSTANT
    Children     []Node            // positional children
    NamedChildren map[string]Node  // named children
}
```

Key design insight: a node is either a constant XOR a function — no node can be both.

**Recommendation: Single flexible class.**

Rationale (based on production evidence from Marble):
1. **Serialization is the primary requirement** — a flat class serializes trivially to JSON/YAML without polymorphism boilerplate. A sealed hierarchy requires type discriminators, `@JsonTypeInfo`, or a custom serializer. This conflicts with the goal of "AST as a portable intermediate representation."
2. **The AST is a data model, not a parser output** — in JEXL/SpEL, the sealed hierarchy maps to parser grammar rules. In our design, the AST is constructed programmatically or deserialized from a definition file. The single class is simpler for both sides.
3. **Function identity via enum/symbol** is already a discriminator — the `Function` field tells you what kind of node it is. Adding Java class hierarchy on top is redundant.
4. **The Marble engine proves this works at production scale** with 50+ function types (arithmetic, comparison, boolean, strings, time, database access, scoring, switching, aggregator, fuzzy match, etc.).

Trade-off acknowledged: type safety is weaker. A sealed hierarchy catches "added children to a constant node" at compile time. The single class catches it at runtime. Accept this trade-off in exchange for serialization simplicity.

---

#### Q2: EvaluationResult — Fields for Trace and UI Rendering

**Marble's model (reference):**

```go
type NodeEvaluation struct {
    Index          int
    EvaluationPlan NodeEvaluationPlan{Skipped bool, Cached bool, Took time.Duration}
    Function       Function
    ReturnValue    any
    Errors         []error
    Children       []NodeEvaluation
    NamedChildren  map[string]NodeEvaluation
}
```

Additionally, Marble derives aggregate views from the tree:
- `EvaluationStats` — aggregated counts per node: `SkippedCount`, `CachedCount`, `Nodes`, `Took`
- `FunctionStats` — per-function aggregation: how many times each function was called, how many skipped/cached, total time
- `FlattenErrors()` — collects all errors from the entire subtree into one list

**What these fields enable for UI rendering (e.g., risk-rating explanation):**

| Field | UI Purpose |
|-------|------------|
| `ReturnValue` | The actual value to display. For risk-rating: "Score = 85." For conditions: "true/false." |
| `EvaluationPlan.Skipped` | "This branch was not evaluated because an earlier branch matched." Crucial for explaining short-circuit behavior. |
| `EvaluationPlan.Cached` | "This result was reused from a previous evaluation." Explains performance, avoids confusion. |
| `EvaluationPlan.Took` | "This check took 2ms." Useful for performance debugging in the UI. |
| `Children` / `NamedChildren` | Recursive tree structure enables drill-down: "Score = 85. Expand → base 50, age factor 20, income factor 15." |
| `Errors` | "This check failed due to missing data." The UI can highlight failing nodes. |

**Recommended fields for Java `EvaluationResult`:**

```java
class EvaluationResult {
    FunctionId function;            // which function was evaluated
    Object returnValue;             // the computed value (null on error)
    List<EvaluationError> errors;   // structured errors, not raw Throwables
    List<EvaluationResult> children;           // positional child results
    Map<String, EvaluationResult> namedChildren; // named child results
    EvaluationTrace trace;          // metadata about the evaluation
}

class EvaluationTrace {
    boolean skipped;        // short-circuited / not evaluated
    boolean cached;         // result was from cache
    long durationNanos;     // wall-clock time for this node
    Instant evaluatedAt;    // whens evaluation happened
}

class EvaluationError {
    String code;            // machine-readable error code (e.g., "ARGUMENT_MUST_BE_INT")
    String message;         // human-readable description
    String source;          // which argument / named argument caused the error
}
```

Separate aggregate views (derived from the result tree, not stored):

```java
class EvaluationSummary {
    long totalDurationNanos;
    int totalNodes;
    int skippedNodes;
    int cachedNodes;
    Map<FunctionId, FunctionStats> perFunction;  // aggregated by function type
    List<EvaluationError> allErrors;             // flattened errors
}
```

Key design rule from Marble: **Trace is built into every evaluation, not opt-in.** This avoids the "we forgot to enable tracing in production" problem.

---

#### Q3: Function Registry API — Consumer Registration

**Marble's model (reference):**

Two key abstractions:

1. **Evaluator interface** — the contract every function must implement:

```go
type Evaluator interface {
    Evaluate(ctx context.Context, arguments ast.Arguments) (any, []error)
}
```

Where `Arguments` is:
```go
type Arguments struct {
    Args      []any
    NamedArgs map[string]any
}
```

2. **Environment** — the registry holding evaluators + configuration:

```go
type AstEvaluationEnvironment struct {
    availableFunctions       map[ast.Function]evaluate.Evaluator
    disableCostOptimizations bool
    disableCircuitBreaking   bool
}

func (env *AstEvaluationEnvironment) AddEvaluator(function ast.Function, evaluator evaluate.Evaluator) {
    // panics on duplicate registration (fail-fast)
}

func (env *AstEvaluationEnvironment) GetEvaluator(function ast.Function) (evaluate.Evaluator, error) {
    // returns error if function is not registered
}
```

**Usage pattern (from Marble's production code):**

```go
// 1. Create environment with built-in functions
env := NewAstEvaluationEnvironment()

// 2. Add domain-specific evaluators
env.AddEvaluator(ast.FUNC_SCORE_COMPUTATION, evaluate.ScoreComputation{})
env.AddEvaluator(ast.FUNC_SWITCH, evaluate.Switch{})

// 3. Use it
result, ok := EvaluateAst(ctx, cache, env, astNode)
```

**Key design decisions observed:**
- **Function identity is typed** (`Function` enum, not `String`) — prevents typos, enables IDE completion.
- **Evaluators receive already-evaluated values** — the evaluation loop pre-evaluates children; evaluators get `Arguments{[]any, map[string]any}` of resolved values.
- **Fail-fast on duplicate** — panic in Go, `IllegalStateException` in Java.
- **Evaluators are stateless** — they depend only on the arguments passed; any external dependency (DB, config) must come through the evaluation context, not the evaluator instance.
- **Environment carries optimization flags** — caching, circuit breaking, cost optimization are environment-level concerns, not per-evaluator.

**Recommended Java API (based on Marble pattern):**

```java
// Core contract
@FunctionalInterface
interface Evaluator {
    EvaluationOutcome evaluate(EvaluationContext ctx, Arguments arguments);
}

// Data passed to evaluator
record Arguments(List<Object> positional, Map<String, Object> named) {}

// What evaluator returns
record EvaluationOutcome(Object value, List<EvaluationError> errors) {}

// Registry
class FunctionRegistry {
    void register(FunctionId id, Evaluator evaluator);             // throws if duplicate
    void registerAll(Map<FunctionId, Evaluator> evaluators);
    Evaluator get(FunctionId id);                                  // throws if not found
    boolean contains(FunctionId id);
    Set<FunctionId> registeredFunctions();
}
```

**Extension patterns for consumers:**

```java
// Pattern 1: Programmatic registration
FunctionRegistry registry = new FunctionRegistry();
registry.register(FunctionId.of("CUSTOM_SCORE"), (ctx, args) -> {
    double score = (double) args.positional().get(0);
    if (score > 100) return new EvaluationOutcome(score * 2, List.of());
    return new EvaluationOutcome(score, List.of());
});

// Pattern 2: Built-in preset + domain extensions
FunctionRegistry registry = FunctionRegistry.withBuiltins();  // +, -, and, or, etc.
registry.register(FunctionId.of("AML_SCORE"), new AmlScoreEvaluator(database));

// Pattern 3: Discovery-based (optional convenience)
// Auto-discover @RegisterFunction annotated evaluators via classpath scanning
FunctionRegistry registry = FunctionRegistry.discover("com.myapp.rules");
```

---

#### Q4: Evaluation Loop — Single Generic Method vs. Pluggable Strategies

**Marble's model (reference):**

Marble uses a **single generic recursive method** (`EvaluateAst` in `usecases/ast_eval/evaluate_ast.go`). The pseudocode:

```
EvaluateAst(ctx, cache, environment, node):
  if constant → return NodeEvaluation{ReturnValue: node.Constant}
  
  hash = node.Hash()
  if cache hit → return cached copy (marked as Cached=true)
  
  children = evaluate each positional child (with short-circuit check after each)
  namedChildren = evaluate each named child (always, regardless of short-circuit)
  
  if child errors → return failed NodeEvaluation
  
  evaluator = environment.GetEvaluator(node.Function)
  value, errors = evaluator.Evaluate(ctx, Arguments(children, namedChildren))
  
  store in cache
  return NodeEvaluation{ReturnValue: value, Errors: errors, ...}
```

**Behavior variations are handled through parameterization, not loop replacement:**

| Variation | Mechanism |
|-----------|-----------|
| **Short-circuit** | `FuncAttributes.LazyChildEvaluation` callback — checked after each child evaluation. AND returns false to continue only if child is true; OR returns true to continue only if child is false. |
| **Cost-based reordering** | `WeightedNodes` — when parent is commutative, children are sorted by cost before evaluation. Cheaper nodes run first. |
| **Caching** | Optional `sync.Map` + `singleflight` group. If cache is `nil`, caching is skipped. |
| **Debug/simulation** | Environment has `disableCircuitBreaking`, `disableCostOptimizations` flags. The `WithoutOptimizations()` method returns a copy of the environment with both disabled. |

**When would you actually need a different loop?**
- Parallel evaluation (evaluate children concurrently)
- Distributed evaluation (each child on a different node)
- Record/replay (record every evaluation for replay testing)
- Incremental evaluation (re-evaluate only changed subtrees)

**Recommendation: Single generic method with well-defined extension points.**

The base evaluation loop should be a single method. Different "strategies" are implemented as:

1. **Context wrappers** (parallel evaluation → wrap children in `CompletableFuture.supplyAsync`)
2. **Decorator evaluators** (record/replay → evaluator wrapper that logs every call)
3. **Environment flags** (debug mode → add flags to `EvaluationContext`)
4. **Visitor pattern** (incremental evaluation → track node dependencies, re-evaluate only dirty nodes)

```java
// Core loop — single method
class AstEvaluator {
    EvaluationResult evaluate(Node node, EvaluationContext ctx) {
        // 1. Constant short-circuit
        if (node.isConstant()) {
            return EvaluationResult.constant(node.constant());
        }
        
        // 2. Cache check
        if (ctx.cacheEnabled()) { ... }
        
        // 3. Evaluate children (with short-circuit)
        var children = evaluateChildren(node.children(), ctx, node);
        var namedChildren = evaluateNamedChildren(node.namedChildren(), ctx);
        
        // 4. Look up and invoke evaluator
        var evaluator = ctx.registry().get(node.function());
        var outcome = evaluator.evaluate(ctx, new Arguments(children, namedChildren));
        
        // 5. Store in cache
        return EvaluationResult.function(node.function(), outcome, children, namedChildren);
    }
}

// Extension point for parallel evaluation — decorate context
class ParallelEvaluationContext extends EvaluationContext {
    @Override
    List<EvaluationResult> evaluateChildren(List<Node> children) {
        return children.parallelStream()
            .map(child -> evaluate(child, this))
            .toList();
    }
}
```

**Key principle (from Marble observation):** The evaluation loop itself is infrastructure. The variations are configuration and decoration, not loop replacement. This keeps the core simple while still supporting different execution modes. Only introduce a pluggable loop strategy if you have a concrete use case (like distributed evaluation) that cannot be handled by context decoration.
