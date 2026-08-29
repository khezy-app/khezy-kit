# Task 12 — Spring AI adapter: `securities/data-privacy-spring-ai` + `SpringAiLlmClassifier`

## Objective

Deliver the **Spring AI canonical `LlmClassifier`** (design §11) in the adapter module: a
`ChatClient`-backed implementation using `BeanOutputConverter` to deserialize the core `Verdict`
record, wired to `LlmContract`/`LlmPolicyPrompts` from core. Adapter depends on core **by coordinate**
(composite-build substitution). This task is fully parallel-safe with tasks 09–11.

## Hand-off context

- **Design doc:** §11 (LLM classifier), §4.2 (adapter module), §12 (facade integration).
- **From Task 01 (in-repo):** adapter module `securities/data-privacy-spring-ai` already scaffolded
  with a `// TODO resolve at Task 12` deps block. Root `settings.gradle` already `includeBuild`s it.
- **From Task 08 (in-repo):** `LlmClassifier` SPI (nested `record Verdict(boolean flagged, double
  confidence, String entityType)`), `LlmContract` (threshold rule + `toResult`), `LlmPolicyPrompts`
  (jailbreak/nsfw/topical/custom templates + `SYSTEM_RULES`), `LlmCheck` (binds classifier →
  GuardrailResult).
- **Resolved decisions:**
  - **Coordinate dependency, not `project(...)`:** `api "io.github.khezyapp:data-privacy-core:1.0.0"`
    (composite-build substitution; declarative-http uses the same identity for `dynamic-object`).
  - **Spring AI version:** RESOLVED & PINNED to stable GA **`org.springframework.ai:spring-ai-bom:2.0.1`**
    (verified present on Maven Central). The `build.gradle` `ext.springAiVersion` is set to `2.0.1` and wired
    into the `platform(...)` import. Note Spring AI 2.0 uses Jackson 3 (`tools.jackson.*`); `BeanOutputConverter`
    (artifact `spring-ai-model`, transitive) carries a `ResponseTextCleaner` so fenced JSON is handled. Final
    coordinates logged in `00-HANDOFF.md` (Task 12 section).
  
## Installations

- In `securities/data-privacy-spring-ai/build.gradle` (fill the Task 01 TODO):
  ```groovy
  dependencies {
      api "io.github.khezyapp:data-privacy-core:1.0.0"
      implementation platform("org.springframework.ai:spring-ai-bom:<resolved-GA>")
      implementation "org.springframework.ai:spring-ai-client-chat"
      testImplementation "org.springframework.ai:spring-ai-test"   // MockChat*, stubs
  }
  mavenPublishing { pom { name = 'data-privacy-spring-ai'; description = '...' } }
  ```

## Files to create (under `securities/data-privacy-spring-ai/src/main/java/io/github/khezyapp/dpriv/springai/`)

### 1. `SpringAiLlmClassifier.java`

```java
public final class SpringAiLlmClassifier implements LlmClassifier {
    public static Builder builder();
    public static class Builder {   // chatClient(...), entityType(...), prompt(...), threshold(...) }
    public Verdict classify(String input);
    public String beanName();
}
```
- `classify`: call `this.chatClient.prompt()...user(<template with input>)...call().entity(Verdict.class)`
  via `BeanOutputConverter<Verdict>` (which carries a JSON schema); map NaN/negative confidence to
  0.0; verify `Verdict` sanity (boolean flag, 0..1 confidence, entityType fallback = `beanName()`).
- **No decision logic here** — `LlmContract` owns the threshold rule; classifier just returns the raw
  `Verdict` (Tasks 08/10 hold the classification semantics). Single-responsibility keeps core testable.
- `beanName()` = fixed unique identifier (e.g. `"spring-ai-jailbreak"`) — used as
  `GuardrailResult.entityType` family.
- Prompts from `LlmPolicyPrompts` default (per built-in family) unless the builder's `prompt`
  supplies a custom one.

### 2. `SpringAiLlmClassifierFactory.java` (optional convenience)

```java
public final class SpringAiLlmClassifierFactory {
    public static SpringAiLlmClassifier jailbreak(ChatClient client, double threshold);
    public static SpringAiLlmClassifier nsfw(ChatClient client, double threshold);
    public static SpringAiLlmClassifier topical(ChatClient client, double threshold);
}
```
- Maps to `LlmPolicyPrompts`. Skip this file if the builder covers it (log which).

## Tests (JUnit 5; spring-ai-test Mock model preferred — anonymous stubs per repo style)

- `classify` calls the ChatClient prompt exactly once for a fixed input (verify with a stub
  `ChatClient` returning a canned `Verdict`); `BeanOutputConverter` schema round-trips `Verdict`.
- EntityType fallback: verdict with empty `entityType` → `beanName()`.
- Non-JSON / malformed model response → no `Verdict` (classifier throws); the ROBUST path still
  surfaces as an error contained by the core pipeline (design §13) — assert via `LlmCheck` wrapper.
- Threshold/flag decisions stay in core: adapter returns the raw `Verdict` regardless of threshold.

## Acceptance criteria

- `./gradlew :data-privacy-spring-ai:build` green (resolves core from composite build).
- Adapter contains ONLY the classifier + optional factory: no pipeline/clone of core logic.
- Handoff log records the **resolved Spring AI GA version** + any build-toolchain notes.

## Hand-off to next task (log in 00-HANDOFF.md)

- Final Spring AI BOM/GA version pinned in `build.gradle` (+ resolution evidence).
- `SpringAiLlmClassifier` / factory signatures + how Task 13 wires `SpringAiLlmClassifier` into
  `Guardrails` (via `Guardrails.builder().withClassifier(...)`).
- Any divergence from design §11 (e.g. `ChatClient` method names that changed across Spring AI
  versions — log the exact calls used).