---
description: "Extract session learnings into focused, non-duplicative artifacts. Prefer separate skills over bloated appends."
---

Analyze the conversation and extract reusable knowledge. The goal is to make future sessions faster and more correct — not to dump everything into one master skill.

## 0. First, audit this session's mistakes

Before extracting knowledge, identify process failures that cost time or tokens:

- Was a relevant skill or command available but not loaded/used by the agent? → That's a **discoverability problem**, not a knowledge gap. Consider whether the artifact needs a more descriptive name, or was hard to find.
- Did the agent repeatedly violate a convention that was documented in an existing skill? → The skill may be too long or buried. **Consider splitting it into smaller, focused skills** rather than appending more.
- Was the same fix applied multiple times (e.g., same Checkstyle rule violated across many files)? → That's a **workflow problem**. Consider a command like `/fix-checkstyle` that runs a targeted fix.
- Did the agent make incorrect assumptions not covered by any existing artifact? → That's a **knowledge gap**. Capture it.

Document up to 3 key mistakes and what process change would prevent them.

## 1. Identify what was learned

Scan the conversation for:

| Signal | Likely artifact |
|---|---|
| New module, build config, package layout | **AGENTS.md** — module table or commands section |
| Formatting/naming pattern the agent got wrong | **Skill** — but only if existing skill didn't already cover it |
| Multi-step manual workflow done 2+ times | **Command** — automate the workflow |
| Non-obvious constraint that caused a bug | **Skill** (gotchas section) or **separate gotcha skill** |
| Specific shell commands, test runner flags | **AGENTS.md** — commands section |

## 2. Decide artifact type and scope

**CRITICAL RULE: NEVER append to a skill if the new knowledge belongs in its own focused skill.**

### Is the existing skill already too long?

If the target skill exceeds ~200 lines, **do not append**. Either:
- Create a new focused skill (e.g., `khezy-checkstyle-gotchas` instead of appending to `khezy-coding-style`)
- Use a command instead (for procedural workflows)
- Put it in AGENTS.md (for project facts only)

### Artifact decision matrix

| Situation | Action |
|---|---|
| New project structure / build fact | Update AGENTS.md |
| Convention the agent repeatedly violated, and existing skill already covers the area | **Don't append.** The skill was already there — the agent just didn't use it. Consider if discoverability is the issue (rename? split?). |
| Convention the agent violated, and existing skill does NOT cover it | Create a **new focused skill** (e.g., `khezy-checkstyle-gotchas`) or append only if the existing skill is short and topically adjacent. |
| Workflow done 2+ times by hand | Create a **command** |
| Bug pattern / gotcha | Create a **gotcha skill** or add to a dedicated gotcha file |
| One-off fix, no general pattern | **Skip** — not all learnings need capture |

### Allowed actions by artifact

| Artifact | Create? | Update? | Constraint |
|---|---|---|---|
| **Skill** (`skills/<name>/SKILL.md`) | Yes, when no existing skill covers the topic | Yes, but only if skill is **under 200 lines** and knowledge is closely related | **Never append past 200 lines.** Split instead. |
| **Command** (`commands/<name>.md`) | Yes, for any repeatable workflow | No — commands are fixed workflows | One workflow per command |
| **AGENTS.md** | Never (project root) | Yes — merge new facts into existing sections | Preserve all existing content. Never overwrite. |

## 3. Create or update

For each artifact chosen:

### Creating a new skill
- YAML frontmatter: `name`, `description` required
- Keep it focused on one concern (e.g., "checkstyle rules unique to this project", not "everything about Java")
- First line should state: "Use this skill when [specific trigger condition]"
- Content: concise, example-driven, structured for quick scanning
- Target: 50–150 lines

### Updating an existing skill (only if under 200 lines)
- Read the full file first
- Check if the knowledge is already present — skip if duplicate
- If the skill would exceed 200 lines after adding → **split instead**
- Insert the new section in a logical place (not just appended to the end)
- Preserve all original frontmatter and content

### Creating a command
- YAML frontmatter: `description` required
- The template should tell the agent exactly what steps to perform
- Support `$ARGUMENTS` where appropriate
- Keep it procedural and repeatable

### Updating AGENTS.md
- Read the full file first
- Insert new info into the appropriate section (module table, commands, conventions)
- Update stale numbers (e.g., "8 independent builds" → "10")
- Preserve all existing content

## 4. Report

Summarize what was created, updated, and why. If you chose not to capture something that seems learnable, explain why (e.g., "this was a one-off fix, no general pattern").

## Examples of good captures

**New skill (khezy-ast-checkstyle-gotchas)**: "Checkstyle `LeftCurly` requires expanding compact methods — `{ return x; }` → multi-line. The `khezy-coding-style` skill mentions braces but doesn't detail this specific rule, causing repeated violations." → Create separate skill because adding to existing skill would bloat it past 200 lines.

**New skill (khezy-evaluator-test-patterns)**: "Testing evaluators with external dependencies uses anonymous `DbAccessor` stubs (no Mockito). Register custom evaluators manually with `FunctionRegistry.empty()` + `registry.register()`." → Create separate skill; distinct concern from general coding style.

**Command (fix-leftcurly)**: "Run a script to expand all compact methods `{ return x; }` to multi-line format in a given file or directory." → Automates a repeated manual fix.

**AGENTS.md update**: "Added `ast-expression-core` to module table with package `io.github.khezyapp.ast.core` and build commands." → Project fact, belongs in AGENTS.md.

**Skip**: "We fixed imports in 5 files by removing unused static imports." → Too specific; the general rule (no unused imports) is already covered by Checkstyle.

**Mistake capture**: "Agent ignored `khezy-coding-style` skill when implementing from reference, resulting in 121 Checkstyle violations. Fix: `/learn` should highlight at session start which skills are applicable, and new code should always be generated with `skill('khezy-coding-style')` loaded."
