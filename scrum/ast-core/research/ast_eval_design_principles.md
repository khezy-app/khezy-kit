# AST Evaluation Design Principles for a Reusable Rule Engine Core

## Scope

This note documents the design principles behind Marble's AST-based evaluation engine, as seen in the repository under the ast_eval implementation. The goal is to turn this into a reusable core rule-engine design for Java that can be shared across multiple products and business domains.

## 1. Core Idea: Expressions as Trees

The engine represents rules and decision logic as an abstract syntax tree (AST):

- Each node is either:
  - a constant leaf, or
  - a function node that applies logic to its children.
- The tree is the canonical representation of an expression.
- The evaluator interprets the tree recursively.

This is a strong foundation for a reusable core library because it separates:

- the structure of the expression, from
- the runtime behavior of each function.

That separation makes the engine generic while still allowing domain-specific logic to be plugged in.

## 2. The Node Model

A node should be treated as a small, composable building block.

### Essential node fields

Each AST node has:

- Function: the semantic identity of the node.
- Constant: a leaf value for constants.
- Children: ordered positional arguments.
- NamedChildren: named arguments for structured expressions.

This design is important because it supports both:

- simple operators such as equality, arithmetic, and boolean logic, and
- richer domain-specific operations such as filters, aggregation, database access, scoring, or branching.

### Why both children and named children matter

The codebase uses positional children for conventional function arguments, but named children for cases where the semantic shape is richer and more explicit.

Examples:

- a comparison node may use positional children for left and right operands;
- a filter or database access node may use named children to describe table, field, path, threshold, or configuration.

For a Java core library, this means the AST node should support two argument forms:

1. positional arguments, for simple and compact expressions;
2. named arguments, for structured configuration and metadata.

## 3. Functions Are First-Class Concepts

The core design should not hardcode operations into the AST structure. Instead, the engine should define a function registry.

### Principle

A function is a semantic operation with:

- a symbolic identifier,
- a display name,
- optional argument metadata,
- optional evaluation behavior such as lazy execution or short-circuiting,
- optional cost/priority metadata for optimization.

This makes the engine extensible. New business rules can be introduced by registering new evaluators without changing the core runtime.

## 4. Evaluation Is Recursive and Generic

The central evaluator walks the AST recursively:

- evaluate each child node,
- collect the children’s results,
- pass them to the function evaluator,
- return a result.

This creates a clean execution model:

- structure is supplied by the AST;
- behavior is supplied by the evaluator implementation.

### Generic evaluation loop

The engine should follow this pattern:

1. Inspect the current node.
2. If it is a constant, return its value directly.
3. Otherwise, evaluate child nodes.
4. Build an argument bundle from positional and named children.
5. Ask the registered evaluator for that function to compute the result.
6. Return both the value and any errors.

## 5. The Runtime Should Separate Validation from AST Structure

The AST itself should remain relatively simple. It should not contain business-rule validation logic.

Instead, evaluators should be responsible for:

- argument adaptation,
- type checking,
- required-argument validation,
- error reporting.

This is a crucial principle for a reusable core library. It allows the core engine to remain generic while domain-specific evaluators implement the actual semantics.

## 6. Evaluation Results Should Be Traceable

The engine does not only return a final value. It also tracks evaluation details.

A result object should capture:

- the returned value,
- errors produced during evaluation,
- child evaluations,
- plan metadata such as whether the node was skipped, cached, or evaluated normally.

This is important because in rule engines, debugging and observability matter just as much as execution correctness.

For a reusable core library, this means the evaluation result should be rich enough to support:

- debugging,
- tracing,
- explanation generation,
- performance analysis.

## 7. Lazy Evaluation and Short-Circuiting Are First-Class Features

Some functions should not evaluate all children if the outcome is already known.

The implementation demonstrates this clearly with logical operators and scoring switches:

- AND can stop early when one child evaluates to false.
- OR can stop early when one child evaluates to true.
- a switch-like scoring structure can stop once a branch is triggered.

### Design principle

The runtime should support a pluggable short-circuit mechanism per function type.

This makes evaluation more efficient and allows the engine to model business semantics precisely.

## 8. Optimization Should Be Optional and Orthogonal

The engine includes optimization features such as:

- cost-based child reordering for commutative functions,
- caching of subtree evaluations,
- shared execution for repeated subtrees.

These should not be part of the core semantic model. Instead, they are runtime features that can be enabled or disabled depending on the execution environment.

### Design principle

The core should define semantics first, and optimization second.

That allows the same AST and evaluator model to be used in:

- simple local execution,
- offline batch evaluation,
- interactive user-facing decision evaluation,
- high-performance production engines.

## 9. The AST Should Be a Data Model, Not a Policy Layer

A strong design principle is that the AST should describe what the rule is, not how it is executed.

The AST should not contain:

- database access logic,
- external service calls,
- business-specific policy rules,
- runtime-specific transport concerns.

Instead, the evaluator layer should own those concerns.

This is essential for building a generic core library that can be reused across many domains.

## 10. Suggested Java Core Architecture

A Java implementation should mirror the same separation of concerns:

### A. AST model

- AstNode
  - function id
  - constant value
  - list of positional children
  - map of named children

### B. Function registry

- FunctionDefinition
- Evaluator interface
- registry that maps function ids to evaluator implementations

### C. Evaluation context

- environment or context object that provides:
  - evaluator registry
  - optional caching
  - optional short-circuit settings
  - optional tracing and telemetry

### D. Evaluation result

- value
- errors
- child results
- evaluation metadata

### E. Extension points

- custom evaluators for domain-specific operations
- custom argument adapters and validators
- custom optimization strategies

## 11. Recommended Design Rules for the Java Version

When translating this to Java, the following rules should guide the implementation:

1. Keep the AST structurally simple.
2. Treat functions as pluggable evaluators.
3. Support both positional and named children.
4. Make evaluation recursive and deterministic.
5. Make errors and traces explicit.
6. Keep short-circuit behavior configurable per function.
7. Keep optimization layers separate from business semantics.
8. Design the core to be reusable, not domain-specific.

## 12. Summary

The Marble ast_eval design is valuable because it combines:

- a simple, generic AST model,
- a pluggable evaluator architecture,
- explicit support for structured named arguments,
- recursive evaluation,
- rich result tracing,
- short-circuiting and optimization.

That makes it a strong reference for a reusable Java rule-engine core.

In practical terms, the core library should be built around one central idea:

> The AST describes the rule, and the evaluator runtime interprets it.

That division gives the library the flexibility needed to support multiple products, domains, and execution environments.
