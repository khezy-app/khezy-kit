# data-privacy-spring-ai

Spring AI adapter for the `data-privacy-core` engine. Turns a `ChatClient` into a
`data-privacy-core` `LlmClassifier` (the SPI for `run(text, CLASSIFY)`), so LLM-as-judge checks
(jailbreak, NSFW, topical) plug into the deterministic engine without any provider wiring.

---

## Introduction

`data-privacy-core` keeps Spring completely out of its classpath; it only defines the `LlmClassifier`
SPI. This module implements that SPI with Spring AI's `BeanOutputConverter`, deserializing the core
`LlmClassifier.Verdict` record (`flagged`, `confidence`) from the model's JSON response.

Use it to write code like:

```java
Guardrails guardrails = Guardrails.builder()
        .config(GuardrailsConfig.DEFAULTS)
        .withClassifier(SpringAiLlmClassifierFactory.jailbreak(chatClient, 0.7))
        .build();

GuardrailsOutcome outcome = guardrails.run(prompt, Operation.CLASSIFY);
if (outcome.detected()) {
    // entityType() == "jailbreak"; route for review
}
```

---

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.khezyapp</groupId>
    <artifactId>data-privacy-spring-ai</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation 'io.github.khezyapp:data-privacy-spring-ai:1.0.0'
}
```

---

## Usage

### Built-in families

`SpringAiLlmClassifierFactory` binds a `ChatClient` to the built-in families:

```java
SpringAiLlmClassifier jailbreak = SpringAiLlmClassifierFactory.jailbreak(chatClient, 0.7);
SpringAiLlmClassifier nsfw       = SpringAiLlmClassifierFactory.nsfw(chatClient, 0.7);
SpringAiLlmClassifier topical    = SpringAiLlmClassifierFactory.topical(chatClient, 0.7);
```

The `beanName` of a factory-built classifier is the family name — that is the `entityType` that surfaces
in the `GuardrailsOutcome`.

### Custom classifier

```java
SpringAiLlmClassifier mine = SpringAiLlmClassifier.builder()
        .chatClient(chatClient)
        .beanName("my_family")
        .build();
```

### Semantics to remember

- **Confidence gates, not the classifier**: the verdict's `confidence` is the model's opinion; the
  configured threshold (default `0.7`) in the core `LlmCheckConfig` decides whether it counts (non-guarantee N1).
- **Deterministic checks run first**: `scan`, `redact`, and `SANITIZE` never consult the model
  (`EndToEndSpringAiTest` locks this in).
- **Fail-safe**: a malformed/missing model verdict is an error, never a silent pass (G4).
- The LLM prompt and verdict contract live in `data-privacy-core` (`LlmPolicyPrompts`, `LlmContract`).

---

## Advisors

Two `BaseAdvisor`s turn a `ChatClient` into a privacy boundary:

- `DataPrivacyAdvisor` — **MITIGATE**: deterministically redacts USER-message text before the model
  call and (optionally) the model response after it.
- `GuardrailAdvisor` — **PREVENT**: LLM-as-judge gating of the last user message and/or the model
  response. It never transforms the request — it allows it or blocks it (`PolicyViolationException`).

### Quick-start 1 — redaction only (MITIGATE)

```java
DataPrivacyAdvisor privacy = DataPrivacyAdvisor.builder()
        .config(GuardrailsConfig.DEFAULTS)
        .build();

ChatClient client = ChatClient.builder(model).defaultAdvisors(privacy).build();
String answer = client.prompt().user("my email is visal@example.com").call().content();
// the model saw: "my email is <EMAIL_ADDRESS>"
```

### Quick-start 2 — redaction + gating (MITIGATE + PREVENT)

```java
DataPrivacyAdvisor privacy = DataPrivacyAdvisor.builder()
        .config(GuardrailsConfig.DEFAULTS)
        .build();

Guardrails guardrails = Guardrails.builder()
        .withClassifier(SpringAiLlmClassifierFactory.jailbreak(chatClient, 0.7))
        .build();
GuardrailAdvisor gate = GuardrailAdvisor.builder().guardrails(guardrails).build();

ChatClient client = ChatClient.builder(model).defaultAdvisors(privacy, gate).build();
try {
    String answer = client.prompt().user("my email is visal@example.com").call().content();
} catch (PolicyViolationException e) {
    // security event: e.entityType(), e.scope()
}
```

### Ordering rule

Keep the privacy advisor first (lowest order) so raw text is not visible to anything downstream.
When composing the pair, use the defaults: `DataPrivacyAdvisor` at `HIGHEST_PRECEDENCE`,
`GuardrailAdvisor` at `HIGHEST_PRECEDENCE + 1`.

Spring AI runs `before` hooks in advisor order and `after` hooks in reverse (stack semantics), so
the pair composes as:

```
input:  DPA.redact  ->  GRA.classify  ->  model
output: model  ->  GRA.classify  ->  DPA.redact  ->  caller
```

The judge classifies **redacted** input (it never sees raw PII either); the output is vetted
**raw** first (redaction could destroy attack evidence) and whatever passes is then scrubbed. A
flagged output throws before `DPA.after` runs, so no redacted half-blocked output reaches the
caller.

### Scope and mode reference

| Advisor | Setter | Options | Default |
| --- | --- | --- | --- |
| both | `scope(...)` | `INPUT` / `OUTPUT` / `BOTH` | `INPUT` |
| `DataPrivacyAdvisor` | `mode(...)` | `ALL` / `LAST_ONLY` | `ALL` |
| both | `failOnError(...)` | `true` / `false` | `true` |
| both | `order(...)` | int | DPA `HIGHEST_PRECEDENCE`, GRA `HIGHEST_PRECEDENCE + 1` |

`failOnError=true` is fail-closed: a pipeline/judge error aborts the request instead of passing
unredacted or unvetted content through. A detected violation is never bypassable (`failOnError`
does not affect it).

### Streaming behavior

An `INPUT`-only advisor streams token-by-token: raw deltas pass straight through with full
time-to-first-token. When an advisor protects the output (`OUTPUT` or `BOTH` scope),
`.stream()` buffers the whole response and emits a single aggregated result — output redaction and
gating must see the complete text to be correct, because a PII token or an attack can straddle
chunk boundaries.

| Scope | `.stream()` |
| --- | --- |
| `INPUT` | True streaming — raw deltas passed through |
| `OUTPUT` / `BOTH` | Buffered — one aggregated response after completion |

### Exception reference

| Exception | Means | Consumer reaction |
| --- | --- | --- |
| `PolicyViolationException` | A security event: input or output blocked. Carries `entityType`, `scope`. | Log as a security event; return a polite refusal; **do not** retry the same input. |
| `RedactionException` | The SANITIZE pipeline failed; the request was aborted before the model call. | Surface an error; retry may be OK once the check issue is fixed. |
| `GuardrailEvaluationException` | The judge failed (infra); the request was aborted. | Backend/health alert; retry with backoff may be appropriate. |
| `DataPrivacyException` (base) | Any of the above, uniformly. | Generic handler / 5xx mapping. |

### Observability

Both advisors write a report into the request/response context, never logs or storage.

```java
RedactionReport redaction = (RedactionReport) response.context().get(DataPrivacyAdvisor.CONTEXT_KEY);
GuardrailReport  guardrail  = (GuardrailReport)  response.context().get(GuardrailAdvisor.CONTEXT_KEY);
```

### Non-guarantees

The following are explicitly **not** guaranteed (accepted residual risk):

- **N6** — no model echo: the model may repeat placeholders or synthesize sensitive-looking text.
- **N7** — no data-loss recovery: placeholders are irreversible; there is no un-redaction.
- **N8** — judge accuracy: a verdict is stochastic and threshold-dependent; blocking quality is
  bounded by classifier quality.
- **N9** — no detection of indirect injection: content from RAG/tools/websites does not enter
  through the user-message boundary this advisor watches.
- **N10** — historical data outside the request: data persisted before the advisors were added is
  only protected when it flows through this request's prompt.

---

## Building and testing

```sh
./gradlew :data-privacy-spring-ai:build     # compile + tests + checkstyle
```

Tests run with a canned-verdict `ChatModel` stub — no provider credentials required.