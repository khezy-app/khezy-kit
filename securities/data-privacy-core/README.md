# data-privacy-core

A deterministic, text-level data privacy engine for the JVM. Scans free text for **PII, secrets, URLs,
and keywords**, reports what was found, and produces **typed redaction** (`<EMAIL_ADDRESS>`, `<CREDIT_CARD>`,
`<SECRET>`) so the kind of data removed survives while its value is destroyed. Ships an in-memory and a
bounded-memory streaming path, plus a pluggable **LLM-as-judge** SPI (see `data-privacy-spring-ai` for the
Spring AI adapter).

---

## Introduction

`data-privacy-core` answers three questions about any text before it is logged, stored, or sent to a model:

1. **Is there sensitive content in this text?** (`scan`)
2. **Give me a log-safe copy.** (`redact` / `run(SANITIZE)`)
3. **Should this text be routed for review?** (`run(CLASSIFY)`, optionally with an LLM classifier)

Everything is deterministic, side-effect free, and regression-locked against a guarantee scope
(see [Guarantees](#guarantees)).

---

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.khezyapp</groupId>
    <artifactId>data-privacy-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation 'io.github.khezyapp:data-privacy-core:1.0.0'
}
```

---

## Quick start

```java
import io.github.khezyapp.dpriv.api.Guardrails;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.KeywordsConfig;
import io.github.khezyapp.dpriv.api.Operation;
import io.github.khezyapp.dpriv.api.UrlsConfig;
import io.github.khezyapp.dpriv.api.ScanOutcome;

Guardrails guardrails = Guardrails.builder().config(GuardrailsConfig.DEFAULTS).build();

ScanOutcome scan = guardrails.scan(
        "Email visal@example.com token wJalrXUtnFEMIK7p2x1qK visit https://example.com/page");

String redacted = guardrails.redact(
        "Email visal@example.com token wJalrXUtnFEMIK7p2x1qK visit https://example.com/page");
// "Email <EMAIL_ADDRESS> token <SECRET> visit <LINK>"
```

### Enabling keywords and URL validation

URLs are only flagged for the schemes you allow (empty `allowedSchemes` = flag every URL). Keywords are off
by default.

```java
Guardrails guardrails = Guardrails.builder()
        .config(GuardrailsConfig.builder()
                .urls(new UrlsConfig(java.util.List.of(), java.util.List.of()))
                .keywords(new KeywordsConfig(true, java.util.List.of("confidential", "urgent")))
                .build())
        .build();
```

### Classification with an LLM classifier (Spring AI)

`run(text, Operation.CLASSIFY)` returns the verdict as data; deterministic checks always run first and a
classifier is only consulted when the preflight stage stayed clean.

```java
Guardrails guardrails = Guardrails.builder()
        .config(GuardrailsConfig.DEFAULTS)
        .withClassifier(SpringAiLlmClassifierFactory.jailbreak(chatClient, 0.7)) // from data-privacy-spring-ai
        .build();

GuardrailsOutcome outcome = guardrails.run(prompt, Operation.CLASSIFY);
boolean needsReview = outcome.detected();        // true when flagged or a check errored
String entity = outcome.entityType();            // e.g. "jailbreak", "pii", "secret", ...
```

---

## API surface

| Method | What it does |
|---|---|
| `scan(String)` / `scan(Reader)` | Detect-only: `ScanOutcome(text, entityType, detected, errorMessages, maskEntities, auditRecords)` |
| `redact(String)` / `redact(Reader, Writer)` | Replace every detected token with its `<ENTITY>` placeholder |
| `run(String, Operation.CLASSIFY)` | Detect + consult registered classifiers; verdict is data (`GuardrailsOutcome`) |
| `run(String, Operation.SANITIZE)` | Redact + report; never calls classifiers |
| `Guardrails.builder().config(...).withClassifier(...).failOnlyOnErrors(...)` | Composition root |

**Key behaviours**

- `maskEntities` is `entityType -> matched tokens` — the *redaction input* and the *audit input*, as data.
- Placeholders are exactly `<ENTITY>` where `ENTITY` is the uppercased policy rule name
  (`pii_email_address` → `<EMAIL_ADDRESS>`, `secret` → `<SECRET>`). Format is fixed, not configurable.
- Fail-safe: if any check errors, `redact`/`SANITIZE` **throw** (never emit under-redacted text) and
  `CLASSIFY` fails the input (`failOnlyOnErrors`, default `true`).
- The facade never logs, persists, or calls the network. Streaming `scan(Reader)`/`redact(Reader, Writer)`
  buffer the input; the engine itself (`StreamRedactor` + `TextChunker`, default 64 KiB window / 1 KiB
  overlap) is bounded-memory — see [Guarantees](#guarantees) G6.

---

## Guarantees

The design (see `scrum/data-privacy/design/data_privacy_core_design.md` §3) locks the following contract.
`GuaranteeScopeTest` regression-tests every row.

| ID | Guarantee | Test |
|---|---|---|
| G1 | Deterministic exactness: same input ⇒ same result, on any JVM | `g1DeterministicExactness` |
| G2 | Complete redaction: no detected token survives | `g2CompleteRedactionOfDetectedTokens` |
| G3 | Typed placeholders: every replacement exactly names the policy rule | `g3TypedPlaceholdersIdentifyThePolicyRule` |
| G4 | Fail-safe: an errored check is never a pass; redaction aborts | `g4ErroredClassifierNeverPasses`, `g4RedactionFailsClosedOnCheckError` |
| G5 | Zero side effects: no logging, no output to any sink | `g5ZeroSideEffectsOnAnyPath` |
| G6 | Bounded-memory streaming: O(window + match-set), never O(input) | `g6StreamingUsesBoundedWindowsOnLargeInput` |
| G7 | Reproducible audit data: records returned as data, never logged | `g7AuditRecordsReturnedAsReproducibleData` |
| N1 | LLM confidence is the model's opinion; the configured threshold gates it | `n1ConfidenceIsModelOpinionScaledByThreshold` |
| N2 | Detection is catalog-bounded: obfuscation defeats the regexes | `n2ObfuscationDefeatsExhaustiveDetection` |
| N3 | Logging is the caller's decision; the library returns raw + log-safe forms | `n3LoggingIsACallerDecisionTheLibraryReturnsRawData` |
| N4 | Downstream behavior (model echo, routing) is out of scope | `n4DownstreamRoutingIsOutOfScope` |
| N5 | Entropy scanning trades precision for recall; presets are the dial | `n5EntropyScanningTradesPrecisionForRecall` |

**Compliance reading:** cite G1–G7 as the library's scope of assurance; present N1–N5 as explicit residual
risks the deployment must own.

---

## Building and testing

```sh
./gradlew :data-privacy-core:build        # compile + tests + checkstyle
./gradlew :data-privacy-core:test         # JUnit 5
```

---

## Credits

Pattern tables and contract semantics are ported from MIT-licensed open-source projects (n8n Guardrails,
OpenAI Guardrails JS). Attribution in [CREDITS.md](CREDITS.md).