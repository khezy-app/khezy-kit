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

## Building and testing

```sh
./gradlew :data-privacy-spring-ai:build     # compile + tests + checkstyle
```

Tests run with a canned-verdict `ChatModel` stub — no provider credentials required.