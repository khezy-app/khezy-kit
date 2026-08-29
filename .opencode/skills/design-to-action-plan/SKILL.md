---
name: design-to-action-plan
description: "Translate a design document into a dependency-ordered action plan of self-contained task files (00-INDEX.md, 00-HANDOFF.md, 01-..., 02-...). Embed contract context so coding agents never re-explore the repo. Optionally scope the plan or emit implementation-ready context for ONE specific task/component. Use when the user says 'make a plan from the design doc', 'create tasks for <design>', 'plan v1-actions', or names a component and wants its task base."
license: MIT
compatibility: opencode
metadata:
  audience: developers
  project: khezy-kit
---

# Design doc → action plan

Turn a resolved design document into an **actionable, dependency-ordered plan of vertical-slice
tasks** that coding agents (or sub-agents) can execute independently. Each task file is a
self-contained contract — it embeds every file path, public signature, pinned decision and §-ref it
needs, so the executing agent does **not** re-explore the codebase (token-spend reduction is the
whole point). This is the generalization of the `scrum/data-privacy/v1-actions/` plan — that folder
is the **reference example**; read it first when unsure how deep a task file should go.

## When to use

- User asks to plan an implementation from a design doc (usually `scrum/<feature>/design/*.md`).
- User names a specific component/section and wants its task base produced (focus mode).
- User asks to re-shape an existing plan or add one task to an existing plan.

## Inputs

| Input | Required | Meaning |
|---|---|---|
| `doc` | **yes** | Path to the design doc (markdown). |
| `focus` | no | Dialog-scope switch. See **Focus mode** below. |
| `outDir` | no | Where to write the plan. Default: the design doc's directory + `/v1-actions` (matches the `scrum/*/v1-actions` convention). |

### Focus mode (`focus` argument)

Default (no `focus`) → a **full plan** for the whole doc. With `focus`, restrict deliverables so the
plan (or its base) is built for ONE target instead of everything:

| `focus` value | Deliverable |
|---|---|
| `"<component>"` (e.g. `StreamRedactor`, `PiiCheck`) | A **single deep task** file `01-<component>.md` scoped to that component, PLUS a one-page `00-CONTEXT.md` excerpt of every design § + in-repo type the component touches. Production-ready base, not a sketch. |
| `"task:<N>"` | If `v1-actions/` already exists: **expand/re-write task `N` to implementation-ready depth**, or if it exists and is final, emit the base-objective context excerpt only. If no plan exists yet: produce a minimal plan where task `<N>` is fully specced and the other tasks are stubs with dependency edges. |
| `"section:<N>"` or `"§<N>"` | A **subgraph plan**: only the tasks reachable from design §N (mini-INDEX + their task files), with a note of what they depend on from outside the scope. |

## Workflow

### Phase 0 — Read the design doc (required, no shortcuts)

1. Read the whole doc (all sections). Build a mental model of: scope/guarantees, module layout,
   packages, public types, algorithms, open questions, non-goals.
2. Make a note of **ambiguities or design-doc self-contradictions** (e.g. a record definition in one
   section that disagrees with a table in another). You will resolve them explicitly — see Phase 2.
3. If the doc references other docs (decisions, companion designs), read only what Phase 1 proves
   necessary.

### Phase 1 — ONE exploration pass for precedents (then stop exploring)

Do a **single, batched** exploration of repo conventions and precedent, then embed findings into the
INDEX/tasks so no later agent re-does it:

- Where similar plans/tasks already live (`scrum/*/v1-actions/`) — the format precedent.
- The module scaffold precedent for the target module type (nearest sibling: `settings.gradle`,
  `build.gradle`, plugin id, group/version, package naming) — copy from one example, don't re-derive.
- Root composite-build wiring (`settings.gradle` `includeBuild(...)` lines) and whether the sibling
  dependency should be by **coordinate** (`api "io.github.khezyapp:x:1.0.0"` with Gradle substitution)
  or `project(...)` — record which.
- Repo style skills exist to reference: `.opencode/skills/khezy-coding-style/SKILL.md` and
  `.opencode/skills/khezy-checkstyle-gotchas/SKILL.md`.
- Anything the design doc asserts that is factually wrong for this repo (coordinates, plugin names)
  — record the correction as a "resolved decision".

Stop exploring after this pass. Everything downstream follows the precedent you captured.

### Phase 2 — Resolve ambiguities (the INDEX's job)

Every ambiguity/self-contradiction found in Phase 0 gets a **resolved decision**, written into the
INDEX under a "Resolved decisions (design vs implementation)" heading in the form
`design §X says A; implemented as B (reason)`. A later task MUST NOT re-litigate a resolved decision —
task files should cite the resolution instead of reopening it. If a resolution only concerns one
task, put it in that task's "Design notes" subsection.

### Phase 3 — Emit deliverables in dependency order

Create files in this order (see Deliverable specs below):

1. `00-INDEX.md` — the plan's spine (modules, conventions, resolved decisions, dependency graph,
   task table, sequencing, handoff protocol, acceptance guardrails).
2. `00-HANDOFF.md` — the append-only execution log with template.
3. Task files **in dependency order** — each one fully specced before you move on; a task may only
   reference public types established by earlier tasks.

### Phase 4 — (focus mode) restrict scope

If `focus` was given, apply the Focus-mode table: skip unaffected deliverables, but always keep
`00-HANDOFF.md` (any single task still logs into it).

## Deliverable specs

### `00-INDEX.md`

Sections (mirror the reference example `scrum/data-privacy/v1-actions/00-INDEX.md` exactly):

1. **Purpose** — 2 sentences: what the plan is + the handoff contract.
2. **Modules & conventions (read first)** — every module path + coordinate + version; package roots;
   plugin/plugin-version rules; the exact build commands; the "design doc is the contract; task
   file's Design notes resolve disagreement" convention; which style skills to load.
3. **Resolved decisions** — from Phase 2.
4. **Dependency graph** — ASCII graph of `─►` edges; legend naming which tasks are **parallel
   leaves** after each join point.
5. **Task list** — table `| # | Task file | Builds | Depends on |`. `00`/`00b` rows for INDEX/HANDOFF.
6. **Sequencing notes** — critical path, first join point (that's where parity/contract tests live),
   which tasks run in parallel, final acceptance task.
7. **Hand-off protocol** — rules: append-only, log tail is the only assumed context, template use.
8. **Cross-cutting acceptance guardrails** — the global "Done" bar (compiles, tests pass, Checkstyle
   green, no logging, no files outside declared paths, `final` everywhere, records for data model).

### `00-HANDOFF.md`

Header explains the log; a fenced **template** with `## Task <N> — <name> — DONE` and fields:
date/agent, verified command + result, files created, files edited, public surface added (types +
signatures), gotchas/decisions, "Next task(s) must know". **Append-only**; never rewrite earlier
entries; user requirement, not optional.

### Task files (`<NN>-<kebab-slug>.md`)

Every task file has these sections, in order:

1. **Objective** — the vertical slice in one paragraph (what behavior this task delivers).
2. **Hand-off context** — which design §s are the contract; **which earlier task artifacts (types,
   signatures) this task compiles against** — cite them by task number + file path, not "whatever
   exists"; what the agent is explicitly told NOT to re-read/re-derive.
3. **Design notes** — this task's resolved decisions (only if any).
4. **Files to create / edit** — every path (module-relative), plus the **public signatures as Java
   skeleton blocks** for the key types. Not illustrative prose — actual signatures the next task
   will compile against. For behavior, point at the design § (the spec) and pin only the
   decisions/bounds that must not drift.
5. **Tests** — concrete JUnit 5 + AssertJ cases, named after behavior they lock (loose wording not
   allowed); identity/parity tests where a second code path must equal a first.
6. **Acceptance criteria** — the exact `./gradlew ...:build` command + qualifiers ("green",
   "no new public API", "zero third-party deps").
7. **Hand-off to next task** — what this task MUST log into `00-HANDOFF.md` so the next task needs
   zero exploration: signatures as-built, gotchas, the contract edge the next task builds on.

Length guidance: enough that the executor never opens another file for context except the two style
skills. The reference example averages ~60–90 lines/task with 5–9 files/project.

## Task decomposition rules

- **Vertical slices, ordered by public-type dependency.** Task N's artifacts are only the public
  types tasks N+1 need; a type required by two downstream tasks is defined ONCE, in the earliest
  task that needs it (move it there, don't duplicate).
- **Dependency edge = "needs upstream's public types".** Express as the table in INDEX; leave-tasks
  (no dependents yet) are parallelizable → call that out so sub-agents can be dispatched.
- **Contract-first ordering.** Put the API/type-contract task right after scaffold; everything else
  becomes leaves off it.
- **Reserve the join point** (first task consuming many leaves) for the **parity/consistency test
  suite** — that's where two implementations must produce identical output — and warn it's the
  tricky slice.
- **Embed, don't explore.** Every signature a later task needs appears verbatim in the task file or
  in the HANDOFF log entry it reads. No task file contains instructions that require opening another
  module's source.
- **One module scaffold task** creates BOTH module skeletons + root `includeBuild` wiring + the
  CREDITS/attribution file when patterns are ported.

## Hand-off protocol (write into INDEX verbatim)

1. After each task's build/tests/Checkstyle are green, append a section to `00-HANDOFF.md` using the
   template at its top.
2. The entry must name every file created/edited (absolute-in-repo path), the public types +
   signatures added, gotchas/decisions, and the verified command + result.
3. Do not rewrite or remove earlier handoff entries. The log tail is the only context a later agent
   is allowed to assume.
4. Each task file's "Hand-off to next task" section tells you exactly what the following task(s)
   depend on — log that.

## Cross-cutting acceptance guardrails (write into INDEX)

Each task's "Done" bar: compiles clean, JUnit 5 + AssertJ tests pass, Checkstyle green, no unused
imports, `final` everywhere (params + locals), records for the data model, no logging by the library
(if the design says so), no new files outside the task's declared paths.

## Repo style checklist (bake into every task's Hand-off context)

- Composite build; sibling modules depend **by coordinate** unless proven otherwise.
- Plugin id `khezy.java-library` always; `khezy.java-lombok` opt-in (skip unless the design demands
  Lombok/SLF4J).
- Package base `io.github.khezyapp.<prefix>`; module-dir hyphen ≠ package hyphen (strip).
- Test data convention for this repo: Khmer names + Cambodia locations.
- Load `.opencode/skills/khezy-coding-style/SKILL.md` + `.opencode/skills/khezy-checkstyle-gotchas/SKILL.md`.
- After tasks execute, run `graphify update .` at repo root to refresh the knowledge graph.

## Exit criteria

- All deliverables ship in `outDir`; INDEX's dependency graph is acyclic and matches the table.
- Every task file ends with a filled "Hand-off to next task" (the fields it must populate).
- Focus mode honored exactly; unaffected deliverables untouched.
- Point the user at the reference example (`scrum/data-privacy/v1-actions/`) for a live sample, and
  remind them to restart opencode if this skill is new (skills load at startup).