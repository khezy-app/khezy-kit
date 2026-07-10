---
description: "Capture session learnings as reusable skill, command, or AGENTS.md update"
---

You are at the end of a work session. Review the conversation history and extract any reusable knowledge that future sessions should not need to re-discover.

## Steps

### 1. Identify what was learned

Scan the conversation for:
- **Project structure** — new modules, build config, package layout discovered
- **Coding conventions** — formatting rules, naming patterns, architectural decisions
- **Workflows** — multi-step tasks done by hand that could be automated
- **Gotchas** — non-obvious constraints, known issues, workarounds
- **Tooling** — specific commands, test runners, lint/format commands

### 2. Decide artifact type

Create or update the most appropriate artifact(s):

| Artifact | Location | When to use | Update rule |
|---|---|---|---|
| **Skill** | `.opencode/skills/<name>/SKILL.md` | General knowledge — conventions, architecture, patterns, design decisions a new agent must follow. | **NEVER overwrite.** Read existing file first, then APPEND new sections at the end. Keep all original content intact. Create new only if no match exists. |
| **Command** | `.opencode/commands/<name>.md` | Repeatable task — "run this workflow", "create that component", "do X with these steps". | If a matching command exists, skip (commands are for fixed workflows, not accumulated knowledge). |
| **AGENTS.md** | `AGENTS.md` (project root) | Project-level facts — module locations, build commands, critical paths, env setup. | **NEVER overwrite.** Read existing file first, then MERGE new info into relevant sections. Preserve all existing content. |

### 3. Create or update

**CRITICAL: NEVER overwrite an existing file.** Always read the file first, then append or merge new content. If a file already exists, add a new section to it rather than creating a separate artifact.

For each artifact chosen:
- **Skill**: Read existing SKILL.md first. Append new sections at the end with a clear heading. Preserve original frontmatter unchanged. Content should be concise, example-driven, and structured for quick reading by a new agent.
- **Command**: Use markdown format with frontmatter (`description`, optional `agent`/`model`). The template should tell the agent exactly what steps to perform. Support `$ARGUMENTS` where appropriate.
- **AGENTS.md**: Read existing file first. Insert new info into the appropriate section. Preserve all original content.

Follow these quality rules:
- **No duplicate knowledge** — check existing artifacts before adding
- **Be specific** — prefer concrete examples over vague principles
- **One concern per artifact** — don't mix "how to write tests" with "build command reference"
- **Verify discoverability** — every artifact must be loadable by its tool (skill by `skill()`, command by `/` in TUI)

### 4. Report

Tell the user what you created/updated and why. If you chose not to capture anything, explain why (e.g., "this was a one-off fix, no general knowledge to extract").

## Examples of good captures

**Skill candidate**: "This project uses `final var` everywhere, `final` on all params, and records for DTOs with compact constructors for validation" → update or create `khezy-coding-style` skill.

**Command candidate**: "We ran `./gradlew :pluginlib:test` followed by `./gradlew :pluginlib:checkstyleMain` repeatedly to verify changes" → create `test-module` command.

**AGENTS.md candidate**: "We discovered the module directory is `utils/pluginlib` but the package is `io.github.khezyapp.pluginlib`" → already in AGENTS.md, skip.
