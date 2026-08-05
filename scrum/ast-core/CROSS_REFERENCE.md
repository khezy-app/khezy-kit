# Cross-Reference Map: AST Expression Core

## File Index

| File | Type | Covers |
|------|------|--------|
| `design/ast_expression_core_design.md` | **Authoritative design** | Current implementation: Node, Evaluator, Registry, Errors, SQL data-access layer with updated DbQuery model, JOINs, FunctionExpr, DbOrder |
| `design/dynamic_index_creation_algorithm.md` | Design doc (future) | Concrete algorithm: extraction, projection, coverage check, minimization |
| `design/index_detection_impl_plan.md` | Implementation plan (future) | Package structure, algorithm phases, testing strategy for index detection module |
| `research/research_goals_structure.md` | Research (goals) | Full 4-layer architecture, use cases, industry comparisons (Marble, easy-rules, Drools) |
| `research/ast_eval_design_principles.md` | Research (principles) | Marble's AST evaluation model: Node, evaluator, function registry, trace |
| `research/easy_rules_design_principles.md` | Research (principles) | easy-rules design: Rule, Condition, Action, Facts, engine strategies, composite rules |
| `research/dynamic_index_creation_principles.md` | Research (principles) | Index derivation from AST: pipeline, B-tree fundamentals, IndexFamily model, refinement |
| `reference/postgresql_index_reference.md` | Reference | PostgreSQL index types, B-tree mechanics, multicolumn rules, INCLUDE, partial indexes |

## Reading Guide

| Goal | Start here |
|------|------------|
| Understand the current implementation | `design/ast_expression_core_design.md` |
| Understand the 4-layer architecture | `research/research_goals_structure.md` |
| Understand AST Node / evaluation design principles | `research/ast_eval_design_principles.md` |
| Understand easy-rules comparison | `research/easy_rules_design_principles.md` |
| Understand dynamic index creation (future work) | `research/dynamic_index_creation_principles.md` then `design/dynamic_index_creation_algorithm.md` |
| PostgreSQL index reference | `reference/postgresql_index_reference.md` |

## Concept Map

### Core AST (implemented)

| Concept | Document |
|---------|----------|
| Node model, FunctionId, CoreFunctions | `design/ast_expression_core_design.md` §Core AST Model |
| Evaluation runtime (Evaluator, AstEvaluator, EvaluationContext) | `design/ast_expression_core_design.md` §Evaluation Runtime |
| Function system (FunctionDefinition, FunctionRegistry) | `design/ast_expression_core_design.md` §Function System |
| Results & Errors (EvaluationOutcome, EvaluationResult) | `design/ast_expression_core_design.md` §Results & Errors |
| SQL data-access layer (DbQuery, DbFilter, FilterValue, JOINs, DbOrder) | `design/ast_expression_core_design.md` §SQL Data-Access Layer |
| Evaluators (DbAccess, DbAggregator, DbFieldAccess, FilterBuilder) | `design/ast_expression_core_design.md` §7.6 |
| Builtin evaluators (string, arithmetic, boolean) | `design/ast_expression_core_design.md` §Builtin Evaluators |
| Design principles (Marble reference) | `research/ast_eval_design_principles.md` |

### Future / Not Implemented

| Concept | Document |
|---------|----------|
| Dynamic index creation algorithm | `research/dynamic_index_creation_principles.md`, `design/dynamic_index_creation_algorithm.md` |
| Index detection implementation plan | `design/index_detection_impl_plan.md` |
| Rule abstraction (Layer 2), Engine strategies (Layer 3), Integration (Layer 4) | `research/research_goals_structure.md` |

## Concept Relationships

```
AST Node
  └── is evaluated by → Recursive Evaluator (AstEvaluator)
        └── uses → FunctionRegistry, EvaluationContext
        └── produces → EvaluationResult + EvaluationTrace
              └── SQL evaluators → DbQuery → SqlDialect → SQL
                    └── JooqSqlDialect (default implementation)
                    └── FilterValue.FunctionExpr for function-based filters
                    └── DbJoin for JOIN support
```
