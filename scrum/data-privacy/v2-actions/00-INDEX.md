# Data Privacy Spring AI — v2 Advisor Action Plan

Actionable, dependency-ordered tasks for implementing the ChatClient advisors described in
[`../design/data_privacy_advisor_design.md`](../design/data_privacy_advisor_design.md) (**the design doc**).
Each task is a self-contained vertical slice: build it, verify it compiles + tests + Checkstyle,
**append a handoff entry to [`00-HANDOFF.md`](./00-HANDOFF.md)**, then stop. The next task reads the
handoff log tail instead of re-exploring the codebase.

## Modules & conventions (read first)

- **Module (no new module):** `securities/data-privacy-spring-ai` → `io.github.khezyapp:data-privacy-spring-ai`
  **v1.0.0 — version unchanged** (still in development; no bump per design §3 decision #14).
  Depends on `io.github.khezyapp:data-privacy-core:1.0.0` (composite-build substitution, already wired).
- **No `build.gradle` / `settings.gradle` changes in any task.** All dependencies already present
  (`api data-privacy-core`, `spring-ai-client-chat` via BOM 2.0.1, `spring-ai-test` for tests).
- **Package roots:** `io.github.khezyapp.dpriv.springai` (advisors, enums, reports),
  `io.github.khezyapp.dpriv.springai.exception` (exceptions).
- **Commands** (from repo root; composite build):
  ```sh
  ./gradlew :data-privacy-spring-ai:build             # compile + test + checkstyle + assemble
  ./gradlew :data-privacy-spring-ai:test
  ./gradlew :data-privacy-spring-ai:checkstyleMain
  ```
- **Design doc sections are the contract** — cited per task. When a task and the design doc disagree,
  the task file's "Design notes" section resolves it (they are the resolved interpretation).
- **Coding style:** load `.opencode/skills/khezy-coding-style/SKILL.md` + `.opencode/skills/khezy-checkstyle-gotchas/SKILL.md`.
  `final` params, `final var` locals, records for data carriers, Egyptian braces, no unused imports,
  method length < 150.
- **Test data:** Khmer names + Cambodia locations (`visal@example.com`, `SOK`, `Phnom Penh`, `Siem Reap`).
- **No logging by the library** (design G13). Observability = context reports only.

## Resolved decisions (design vs implementation)

| # | Design § says | Implemented as | Reason |
|---|---|---|---|
| R1 | §7 `PolicyViolationException`/`GuardrailReport` carry `double confidence()` | **Drop `confidence` from both types.** `PolicyViolationException(String entityType, ProtectionScope scope)`; `GuardrailReport(boolean passed, String entityType)` | Verified core: `GuardrailsOutcome`/`GuardrailResult` expose only `entityType`/`detected`/`messages`; per-verdict confidence stays inside `LlmClassifier.Verdict` (core `Guardrails.java`, `LlmContract.java`). |
| R2 | §7 `GuardrailAdvisor.Builder.config(...)` alone | **Add `classifier(LlmClassifier...)`.** `build()` assembles `Guardrails.builder().config(cfg ?? DEFAULTS).withClassifier(each).failOnlyOnErrors(failOnError).build()` when `guardrails(...)` absent; fail-fast `IllegalStateException` if neither `guardrails` nor `classifier` given. | `GuardrailsConfig` cannot register classifiers (facade wires them via `withClassifier`; `configFor` maps `beanName` → `LlmCheckConfig`). `config(...)` alone yields zero LLM checks → gate always passes. |
| R3 | §8.5/§8.8 "outcome reports an errored judge" | **Pinned interpretation** (verified core `Guardrails.java` §run(CLASSIFY) + `LlmCheck`): with `failOnlyOnErrors=true`, judge error ⇒ `detected=true` **and** non-empty `messages`; real violation ⇒ `detected=true` **and** empty `messages` (messages carry only errors). Table: `detected && messages.isEmpty()` → `PolicyViolationException` (never bypassed, G14); `detected && !messages.isEmpty()` → `GuardrailEvaluationException` when `failOnError=true`, else pass. | `ParallelStageRunner` captures check exceptions into `StageResult.errors()`; facade folds them into `detected` + `messages`. |
| R4 | §8.4 `RedactionException` on SANITIZE failure | **Trigger mechanism for tests:** `new PiiConfig(PiiCoverage.SELECTED, null, List.of(), true)` is constructible (records don't validate) and builds fine, but NPEs **at run time** in `PiiCheck.resolveFor` (`chosen.contains`) → preflight error → `Guardrails.run(text, SANITIZE)` throws `IllegalStateException("sanitize aborted on check error: ...")`. `RedactionException` wraps it. | Verified `PiiCheck.java` (ctor `requireNonNull` only; entity iteration deferred to `run`) + `Guardrails.java` line 111. |
| R5 | §8.3/§8.7 `adviseStream` override | **Duplicated ~10-line template in both advisors** (no shared helper). | Spring AI's own precedent: `SimpleLoggerAdvisor`/`MessageChatMemoryAdvisor` each implement `adviseStream` independently; keeps tasks self-contained. |
| R6 | stream tests | **Use `Flux.collectList().block()` + AssertJ**, not `StepVerifier`. | No `reactor-test`/`StepVerifier` precedent in repo; no new dependencies allowed. |
| R7 | §8.1/§8.5 LAST_ONLY + GRA target | `getLastUserOrToolResponseMessage()` returns `Message` — use only if `getMessageType() == USER`, else fall back to the last entry of `getUserMessages()`. | Verified `Prompt.java`: `getUserMessages()` returns `List<UserMessage>`; `getLastUserOrToolResponseMessage()` returns `Message`. |
| R8 | response rebuild | `ChatResponse.builder().generations(List.of(new Generation(redactedAssistantMessage))).metadata(original.metadata()).build()`; message rebuild via `AssistantMessage.mutate().text(...)`. | Verified `ChatResponse.java` + `Generation.java` (spring-ai-model). |
| R9 | unit-test ergonomics | `before(...)`/`after(...)` never invoke the `AdvisorChain` parameter — tests may pass `null` as the chain argument. | Both advisors are pure transformers/gates at the `before`/`after` level; chain use is `BaseAdvisor.adviseCall/adviseStream`'s job. |

## Dependency graph

```
01 shared-types ─► 02 data-privacy-advisor ─┐
                └► 03 guardrail-advisor ────┼► 04 composition-acceptance
```

Legend: `─►` = dependency edge (downstream needs upstream's public types). **02 and 03 are parallel
leaves after 01** — dispatchable to sub-agents. **04 is the join point** (composition/parity tests +
README) — the trickiest slice.

## Task list

| # | Task file | Builds | Depends on | Status |
|---|-----------|--------|-----------|--------|
| 00 | `00-INDEX.md` | plan (this) | — | ✅ Done |
| 00b | `00-HANDOFF.md` | centralized handoff log (append after each task) | — | ✅ Done |
| 01 | `01-shared-types.md` | `ProtectionScope`, `RedactMode`, `RedactionReport`, `GuardrailReport`, `exception/` (4 types) | — | ⬜ Pending |
| 02 | `02-data-privacy-advisor.md` | `DataPrivacyAdvisor` + unit + stream tests | 01 | ⬜ Pending |
| 03 | `03-guardrail-advisor.md` | `GuardrailAdvisor` + unit + stream tests | 01 | ⬜ Pending |
| 04 | `04-composition-acceptance.md` | end-to-end tests (individual + combined order), guarantee regression (G8–G16, N6–N10), README | 02, 03 | ✅ Done |

## Sequencing notes

- **01 → 02/03** is the critical path: shared types first, then the two advisors as parallel leaves.
- **04 (composition)** is the join point — it holds the **composition-order parity tests** (input:
  redact → classify; output: classify → redact; judge sees redacted input, raw output). Budget time.
- 02 and 03 are fully independent of each other (both compile against 01 only) — run in parallel with
  sub-agents. 03 depends on **none** of 02's code (R5: the `adviseStream` template is duplicated).
- After any task, keep the graph fresh: run `graphify update .` at the repo root.

## Hand-off protocol (required)

1. After each task's build/tests/Checkstyle are green, append a section to `00-HANDOFF.md`
   using the template at the top of that file.
2. The entry must name every file created/edited (absolute-in-repo path), the public types +
   signatures added, gotchas/decisions, and the verified command + result.
3. Do not rewrite or remove earlier handoff entries. The log tail is the only context a later
   agent is allowed to assume.
4. Each task file's "Hand-off to next task" section tells you exactly what the following task(s)
   depend on — log that.

## Cross-cutting acceptance guardrails

Each task's "Done" bar: **compiles clean, JUnit 5 + AssertJ tests pass, Checkstyle green, no
unused imports, `final` everywhere, records for the data model, no logging by the library (G13),
no new files outside the task's declared paths, no `build.gradle`/`settings.gradle` edits, version
stays 1.0.0.**
