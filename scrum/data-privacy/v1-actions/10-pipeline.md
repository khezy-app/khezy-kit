# Task 10 — Pipeline (in-memory): `pipeline/GuardrailPipeline` + `pipeline/StageResult` + `internal/ParallelStageRunner` + `api/Guardrails` facade

## Objective

Bring the deterministic checks + LLM checks into a **two-stage evaluator** (design §6) and give the
`api/Guardrails` facade its in-memory implementation: `scan(text)`, `redact(text)`,
`run(text, Operation)`. Stages and arrangement mirror design §6 (preflight transformative →
input classificatory), evaluation is parallel-per-stage, results stay deterministic, failures are
contained per `failOnlyOnErrors`.

## Hand-off context

- **Design doc:** §6 (two-stage pipeline, parallel evaluation §6.2, failOnlyOnErrors), §12 (facade
  signatures + Outcome shapes), §5 (types), §13 (threading & failure policy — MUST read).
- **From earlier tasks (in-repo):**
  - Task 02: `api/*` (all types), `Guardrails` facade **stub with `// Task 10/11` bodies** — fill
    in-memory methods NOW, keep `scan(Reader)`/`redact(Reader,Writer)` as Task 11.
  - Task 03: `redact/Redactor`.
  - Task 05/06/07/08: `SecretKeysCheck`, `UrlsCheck`, `KeywordsCheck`, `PiiCheck`, `CustomRegexCheck`,
    `LlmCheck`, `LlmClassifier`.
- **Design decisions pinned:**
  - Streamable checks come in already `toStream()`-wired from Task 09 — pipeline uses their
    in-memory `run(...)` here.
  - `GuardrailsConfig.builder()` is already built by Task 02; builder.toBuilder/classifier wiring
    APIs (`.withClassifier(LlmClassifier)`) are in Task 02's `Guardrails.Builder`.
  - Outcome constructors must never see `null` maps — empty `Map.of()`/`List.of()` defaults (the
    `List.copyOf`/`Map.copyOf` null gotcha — see `khezy-ast-evaluator-testing` skill).

## Files to create (under `.../dpriv/`)

### 1. `pipeline/StageResult.java`

```java
public record StageResult(String stageName, List<GuardrailResult> validations,
                          Map<String, List<String>> maskEntities, String cleanedValue,
                          boolean passed) {}
```
- Caches nothing mutable; `maskEntities` derived on construction (sum over `validations`, merged
  unique-first-seen); `cleanedValue` = last transformative check's `cleanedValue` (chain order).

### 2. `internal/ParallelStageRunner.java`

```java
public final class ParallelStageRunner {
    public StageResult run(String stageName, List<GuardrailCheck> checks, String input);
}
```
- Evaluate all checks **concurrently** (`CompletableFuture.supplyAsync`, common pool — library owns
  no threads), then **join + collect in stage order** (determinism of StageResult.validations).
- No reordering of results; `get()` unwraps ExecutionException; a thrown check → record error in
  StageResult via `StageResult` error field if needed (see design §13). Keep `failOnlyOnErrors`
  decision LOCAL to the facade, not the runner (runner always returns, callers decide).

### 3. `pipeline/GuardrailPipeline.java`

```java
public final class GuardrailPipeline {
    public GuardrailPipeline(GuardrailsConfig config);           // builds stage lists
    public GuardrailPipeline(GuardrailsConfig config, List<GuardrailCheck> preflight,
                             List<GuardrailCheck> classificatory, boolean failOnlyOnErrors);

    public StageResult preflight(String input);                  // Pii/Secret/Url/Keyword/CustomRegex
    public StageResult classify(String input);                   // LlmCheck instances (jailbreak/nsfw/topical)
    public String redact(String input);                          // preflight().cleanedValue() (no LLM)
}
```
- Stage-augmentation: `GuardrailsConfig` default stages = preflight(4 deterministic families) +
  classificatory(LLM families). Builder adds classifiers via `.withClassifier(...)` → appended to
  classificatory with `LlmCheck`.
- Deterministic stages NEVER throw; classificatory stage wraps classifier errors per §13 —
  capture into `StageResult` errors/messages, don't abort siblings.

### 4. `api/Guardrails.java` (fill the stub’s in-memory half)

```java
public ScanOutcome scan(String text);                 // preflight stage → ScanOutcome
public String redact(String text);                    // pipeline.redact
public GuardrailsOutcome run(String text, Operation op); // CLASSIFY: preflight+classificatory; SANITIZE: redact-semantics
```
- ScanOutcome derivation (design §12.3): `detected` from preflight passed flag;
  `entityTypes` = `maskEntities.keySet()`; `auditRecords` from StageResult.
- GuardrailsOutcome for `Operation.CLASSIFY`: merge preflight + classificatory results; `detected`
  if either stage flagged and (classificatory error → per failOnlyOnErrors).
- Both outcomes default to `List.of()/Map.of()` over `null`.

## Tests

- **Deterministic pipeline:** fixture with email+secret+url+keyword → preflight `maskEntities` has
  exactly the 4 families; `redact` replaces all; `scan` detects; `entityTypes()` matches keys.
- **Parallel is deterministic:** run `scan` 50× on a fixture with 4 checks + 3 LLM stubs (anonymous,
  no Mockito — see `khezy-ast-evaluator-testing`) → identical outcomes & observation order per stage.
- **Classificatory:** stub classifier flags → `run(text, CLASSIFY)` `detected=true`; disabled
  `LlmCheckConfig` short-circuits; SANITIZE runs WITHOUT calling any classifier (assert stub not invoked).
- **failOnlyOnErrors:** classifier stub throws → `failOnlyOnErrors=true` ⇒ detected/errored;
  `false` ⇒ treated as pass (no error, no crash).
- **Error containment:** a deterministic check won't throw at all; a throwing classifier doesn't
  block sibling checks.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green.
- `Guardrails.scan/redact/run` (in-memory) functional; `run` in SANITIZE mode called with `Operation.SANITIZE`
  never invokes LLM checks (asserted).
- No executor lifecycle/memory leaks: pipeline uses the common pool only, no owned threads.

## Hand-off to next task (log in 00-HANDOFF.md)

- `GuardrailPipeline`/`StageResult`/`ParallelStageRunner` signatures + the stage ordering +
  failOnlyOnErrors rule.
- `Guardrails.scan/redact/run` behavior contract + SANITIZE short-circuit note (Task 11 reuses for
  streaming paths).
- Any `StageResult` extra error-field you added (keep public contract stable for Task 11).