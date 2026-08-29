# Data Privacy — v1 Implementation Action Plan

Actionable, dependency-ordered tasks for implementing the Java data-privacy library described in
[`../design/data_privacy_core_design.md`](../design/data_privacy_core_design.md) (**the design doc**).
Each task is a self-contained vertical slice: build it, verify it compiles + tests + Checkstyle,
**append a handoff entry to [`00-HANDOFF.md`](./00-HANDOFF.md)**, then stop. The next task reads the
handoff log tail instead of re-exploring the codebase.

## Modules & conventions (read first)

- **Module 1:** `securities/data-privacy-core` → `io.github.khezyapp:data-privacy-core` v1.0.0
  (pure JDK 17, **zero runtime dependencies**).
- **Module 2:** `securities/data-privacy-spring-ai` → `io.github.khezyapp:data-privacy-spring-ai`
  v1.0.0 (Spring AI adapter, depends on core by **coordinate**, not `project(...)`).
- **Package roots:** `io.github.khezyapp.dpriv` (core), `io.github.khezyapp.dpriv.springai` (adapter).
  Sub-packages: `api`, `checks`, `internal`, `pipeline`, `policy`, `redact`, `stream`.
- **Both modules** use the `khezy.java-library` convention plugin (aggregates JUnit 5 +
  maven-publish + Checkstyle). **Core = no Lombok** (records only, pure JDK 17). **Adapter =
  `khezy.java-lombok`** (uses `@Builder`/`@Getter` etc. for the `SpringAiLlmClassifier` builder, no
  hand-written getters/setters or manual builder class).
- **Commands** (from repo root; composite build):
  ```sh
  ./gradlew :data-privacy-core:build             # compile + test + checkstyle + assemble
  ./gradlew :data-privacy-core:test              # tests only
  ./gradlew :data-privacy-core:checkstyleMain
  ./gradlew :data-privacy-spring-ai:build
  ```
- **Design doc sections are the contract** — cited per task. When a task and the design doc
  disagree, the task file's "Design notes" section resolves it (they are the resolved interpretation).
- **Coding style:** `.opencode/skills/khezy-coding-style/SKILL.md` (`final` params, `final var`
  locals, records for data carriers, Egyptian braces, 120-char lines, no Javadoc needed).
- **Checkstyle gotchas:** `.opencode/skills/khezy-checkstyle-gotchas/SKILL.md`. Recurring:
  `final` everywhere, method length < 150, no unused imports, opening brace last char on its line,
  no `{}` empty blocks (use `{ }`).
- **Attribution:** PII/URL/keyword regex tables are ported from **OpenAI Guardrails JS** and the
  **n8n Guardrails** node (both MIT). `CREDITS.md` is created in Task 01 and extended whenever
  patterns are ported. Do not copy code verbatim — re-implement from contracts/patterns.

## Dependency graph

Tasks are strictly ordered; a task may only depend on artifacts of earlier tasks.

```
01 scaffold ─► 02 api ─┐
              └► 04 pii-catalog ─► 07 pii-check ─┐
02 api ├► 03 redaction ─► 09 stream-core ─────────┤
       ├► 05 secret-keys ────────────────────────►┤
       ├► 06 urls-keywords ──────────────────────►┤
       ├► 08 llm-core ───────────────────────────►┼► 10 pipeline ─► 11 stream-pipeline ─► 13 acceptance
03 redaction ──► 09 stream-core ──────────────────►┘ 08 llm-core ─► 12 spring-ai-adapter ─► 13 acceptance
```

Legend: `─►` = dependency edge (downstream needs upstream's public types). Tasks 04/05/06/08 are
leaves after 02 and can run in parallel with sub-agents.

## Task list

| # | Task file | Builds | Depends on | Status |
|---|-----------|--------|-----------|--------|
| 00 | `00-INDEX.md` | plan (this) | — | ✅ Done |
| 00b | `00-HANDOFF.md` | centralized handoff log (append after each task) | — | ✅ Done |
| 01 | `01-module-scaffold.md` | module skeletons + root wiring + `CREDITS.md` | — | ✅ Done |
| 02 | `02-api-contract.md` | `api/` records, SPIs, config schema, `policy/PiiEntity`, `policy/SecretPreset`, `stream/MatchAccumulator` | 01 | ✅ Done |
| 03 | `03-redaction.md` | `redact/` `Placeholders` + `Redactor` + `internal/AhoCorasick` | 02 | ✅ Done |
| 04 | `04-pii-catalog.md` | `policy/PiiPatterns` (33 patterns) + checksum validators (uses `PiiEntity` from 02) | 02 | ✅ Done |
| 05 | `05-secret-keys.md` | `checks/SecretKeysCheck` + `internal/SecretCandidateFilter` (consumes `SecretPreset` from 02) | 02 | ✅ Done |
| 06 | `06-urls-keywords.md` | `checks/UrlsCheck` + `checks/KeywordsCheck` | 02 | ✅ Done |
| 07 | `07-pii-check.md` | `checks/PiiCheck` + `checks/CustomRegexCheck` | 02, 04 | ✅ Done |
| 08 | `08-llm-core.md` | `policy/LlmContract` + `LlmPolicyPrompts` + `checks/LlmCheck` | 02 | ✅ Done |
| 09 | `09-stream-core.md` | `stream/` `TextChunker` `Tokenizer` `StreamRedactor` + stream variants of checks | 03, 05, 06, 07 | ✅ Done |
| 10 | `10-pipeline.md` | `pipeline/GuardrailPipeline` + `StageResult` + `internal/ParallelStageRunner` + `api/Guardrails` (in-memory) | 03, 05, 06, 07, 08 | ✅ Done |
| 11 | `11-stream-pipeline.md` | `pipeline/StreamPipeline` + `Guardrails.scan(Reader)` / `redact(Reader,Writer)` | 09, 10 | ✅ Done |
| 12 | `12-spring-ai-adapter.md` | adapter module + `SpringAiLlmClassifier` + root wiring | 01 (adapter scaffold), 08 | ✅ Done |
| 13 | `13-acceptance.md` | guarantee-scope regression (G1–G7), end-to-end parity, READMEs | 11, 12 | ✅ Done |

> **v1 is implemented and accepted.** All 13 tasks complete; the design doc
> `data_privacy_core_design.md` is the implemented contract. See the Task 13 entry in `00-HANDOFF.md`
> for the guarantee→test mapping, the final API surface, and the recorded deviations.

## Sequencing notes

- **01 → 02** is the critical path (scaffold then contract). After 02, tasks **03/04/05/06/08**
  are independent leaves — parallelizable. 07 needs the catalog (04).
- **09 (stream-core)** is the first join point (needs checks 05/06/07 + redaction 03). It includes
  the in-memory ↔ streaming **parity tests** — the trickiest slice; budget time.
- **10 (pipeline)** wires deterministic checks + LLM checks (08) into the staged runner and the
  `Guardrails` facade. **11** adds the streaming two-pass path.
- **12 (Spring AI adapter)** is fully independent of 09/10/11 — can run in parallel with 09–11.
- **13 (acceptance)** proves the guarantee scope (design §3 G1–G7) end to end.
- After any task, keep the graph fresh: run `graphify update .` at the repo root.

## Hand-off protocol (required)

1. After each task's build/tests/Checkstyle are green, append a section to `00-HANDOFF.md`
   using the template at the top of that file.
2. The entry must name **every file created/edited** (absolute-in-repo path), the **public types +
   signatures** added, **gotchas/decisions**, and the **verified command + result**.
3. Do not rewrite or remove earlier handoff entries. The log tail is the only context a later
   agent is allowed to assume.
4. Each task file's "Hand-off to next task" section tells you exactly what the following task(s)
   depend on — log that.

## Cross-cutting acceptance guardrails

Each task's "Done" bar: **compiles clean, JUnit 5 + AssertJ tests pass, Checkstyle green, no
unused imports, `final` everywhere, records for the data model, no logging by the library (G5),
no new files outside the task's declared paths.**

Test data uses **Khmer names and Cambodia locations** (repo convention): e.g. `visal@example.com`,
`SOK`, `Phnom Penh`, `Siem Reap`.