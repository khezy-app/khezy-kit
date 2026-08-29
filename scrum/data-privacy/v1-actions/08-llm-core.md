# Task 08 — LLM check core: `policy/LlmContract` + `policy/LlmPolicyPrompts` + `checks/LlmCheck`

## Objective

Implement the **LLM-as-judge contract logic in core** (design §11), fully decoupled from Spring AI:
the confidence-threshold decision rule, the prompt templates, and `LlmCheck` — a `GuardrailCheck`
binding any `LlmClassifier` SPI to the core GuardrailResult contract. The concrete Spring citizen
(`SpringAiLlmClassifier`) lands in the adapter module in Task 12.

## Hand-off context

- **Design doc:** §11 (LLM classifier — §11.2 SPI + `LLM_SYSTEM_RULES`, §11.4 four built-in prompts,
  default threshold **0.7**), §12.2 (CLASSIFY vs SANITIZE), §5.4 (`LlmCheckConfig`).
- **From Task 02 (in-repo):** `api/LlmClassifier` (with nested `record Verdict(boolean flagged,
  double confidence, String entityType)` and `beanName()`), `api/LlmCheckConfig` (`LlmCheckConfig
  .DEFAULTS = enabled=true, threshold=0.7`), `api/GuardrailResult`+helpers.
- **No LLM library in core.** Every type here is pure logic over `Verdict`s and `String`s.
- **Prompts are templates, not magic:** design §11.4 gives the jailbreak / nsfw / topical-alignment /
  custom prompt skeletons. Reproduce their intent; the exact wording ships in `LlmPolicyPrompts`.

## Files to create (under `.../dpriv/`)

### 1. `policy/LlmContract.java` — decision logic (testable without any LLM)

```java
public final class LlmContract {
    public static final boolean classify(boolean verdictFlagged, double confidence, double threshold);
        // true  iff verdictFlagged && confidence >= threshold  (design §11.2)
    public static GuardrailResult toResult(LlmClassifier classifier, LlmClassifier.Verdict verdict,
                                           LlmCheckConfig config, String input);
}
```
- `toResult`: `detected = classify(verdict.flagged(), verdict.confidence(), config.threshold())`;
  `entityType = classifier.entityType()` (e.g. `"jailbreak"`); `maskEntities = Map.of()` (LLM checks
  never redact — classificatory only, design §12.2); `cleanedValue = input`.
- Confidence outside `[0,1]` → clamp for the comparison (still deterministic); threshold `<=0` →
  only `verdict.flagged` gates detection.

### 2. `policy/LlmPolicyPrompts.java` — bundled prompt templates

```java
public final class LlmPolicyPrompts {
    public static final String SYSTEM_RULES;            // design §11.2 LLM_SYSTEM_RULES intent
    public static String jailbreakPrompt(String input); // §11.4
    public static String nsfwPrompt(String input);
    public static String topicalAlignmentPrompt(String input);
    public static String customPrompt(String input, String userRules);
}
```
- All return full prompt text with the user input interpolated. No I/O, no state.

### 3. `checks/LlmCheck.java`

```java
public final class LlmCheck {
    public LlmCheck(LlmClassifier classifier, LlmCheckConfig config);
    public GuardrailResult run(String input);
}
```
- Implements `GuardrailCheck` (`name()` = `"LlmCheck"`, `toStream()` stays the api throwing default —
  LLM checks are non-streamable, design §10).
- `guardrails`: `config.enabled()==false` → `pass` (design: disabled check short-circuits).
  Otherwise `classifier.classify(input)` → `LlmContract.toResult(...)`.
- Constructor validates `classifier != null`; `config` may be null → treat as `LlmCheckConfig.DEFAULTS`.

## Tests

- `LlmContract`: threshold boundary (flagged@0.7 == threshold → detected; flagged@0.69 → not; unflagg
  ed → not), clamp path, disabled config short-circuit.
- `LlmCheck` with an anonymous `LlmClassifier` stub (NO Mockito — see
  `.opencode/skills/khezy-ast-evaluator-testing/` for anonymous-stub style): threshold low → detected,
  verdict patterns captured on entityType `"jailbreak"`.
- `LlmPolicyPrompts`: each builder emits a non-empty multiline string containing the interpolated
  input and its rule keyword (e.g. `"jailbreak"`), no crashing on NUL chars.
- `GuardrailResult` from a stub verdict has `maskEntities == Map.of()` and `cleanedValue == input`.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green — llm checks compile against ONLY `java.*`, `api/`,
  `policy/`, `checks/`. Adapter-visible hook: `LlmContract`/`LlmCheck`/`LlmPolicyPrompts` are public
  so Task 12 can wire `SpringAiLlmClassifier` into them without touching core.

## Hand-off to next task (log in 00-HANDOFF.md)

- `LlmContract.LlmContract(classify/toResult)` signatures + exact threshold rule.
- `LlmPolicyPrompts` constant + method names (Task 12's adapter + acceptance tests reference them).
- `LlmCheck` ctor (`LlmClassifier, LlmCheckConfig`) — how Task 10's pipeline registers LLM checks
  (`jailbreak`/`nsfw`/`topical` families, each its own classifier).
- "Edited so far" reminder: none — new files only.