# Data Privacy Core — Java Library Design (v1)

> **Purpose of this doc:** the resolved, implementation-ready design for the Java
> data-privacy library. It answers the **five open questions of
> [`../docs/03-java-implementation.md`](../docs/03-java-implementation.md) §11**
> per the product decisions below, and reframes the library from
> "LLM guardrails" to a **general data privacy & security library for text**:
> usable in data processing pipelines, redact-before-logging, DLP scanning, and
> compliance — with LLM guardrails as _one optional use case_ on top.
>
> **Source docs:** theory [`../docs/01-principle-and-theory.md`](../docs/01-principle-and-theory.md),
> reference example [`../docs/02-guardrails-node-example.md`](../docs/02-guardrails-node-example.md),
> Java mapping [`../docs/03-java-implementation.md`](../docs/03-java-implementation.md).

---

## 1. Resolved decisions (answers to 03 §11)

| # | Open question (03 §11) | Decision | Where |
| --- | --- | --- | --- |
| 1 | Which LLM client is canonical? | **Spring AI** is the canonical/default implementation. The `LlmClassifier` SPI stays in core (zero Spring dependency); `SpringAiLlmClassifier` ships in a separate adapter module. | §11 |
| 2 | Redaction placeholder format? | **Exactly `<ENTITY>`**, where `ENTITY` is the name of the policy rule that matched — e.g. `<EMAIL_ADDRESS>`, `<US_SSN>`, `<SECRET_KEY>`, `<URL>`, `<customRegex-name>`. Fixed contract, **not configurable**. | §7 |
| 3 | Multi-region PII catalog? | **Full catalog, no subset.** Port 100% of the upstream `DEFAULT_PII_PATTERNS` table (the 33 entities enumerated in 03 §5.1, plus any additional entity the upstream table defines). | §8 |
| 4 | Stream input support? | **Yes.** The transformative path (detect + redact) is fully streaming over `Reader`/`Writer` with bounded memory. Classificatory checks (keywords, LLM) remain in-memory by design — rationale in §10.2. | §6, §10 |
| 5 | Compliance/masking policy guarantee? | The library is **pure logic: input text → output result** (verdicts + redacted text) for the caller to consume downstream. Guarantee scope is explicit (§3): deterministic exactness, complete redaction, fail-safe, zero side effects. Claims about LLM-as-judge accuracy, adversarial evasion resistance, or downstream handling are **not guaranteed** — they depend on implementation, configuration, and caller discipline. | §3 |

---

## 2. Positioning: a data privacy & security library, not an LLM library

The research docs framed the library as "gating/sanitizing text before/after LLM
calls". The product decision widens that: **the library protects text data
anywhere it flows**. LLM calls are one consumer of the sanitized output, not the
reason the library exists.

### 2.1 Use cases

| Use case | Operation | Checks involved | LLM needed? |
| --- | --- | --- | --- |
| **Redact before logging** | `redact` | pii, secretKeys, urls, customRegex | no |
| **Data processing pipeline** (stream sanitization, ETL) | `scan` + `redact` over `Reader`/`Writer` | pii, secretKeys, urls, customRegex | no |
| **DLP gateway / quarantine** (detect, count, route) | `scan` | pii, secretKeys, urls, keywords | no |
| **API request/response sanitization** (proxy, sidecar) | `redact` | pii, secretKeys, urls | no |
| **Compliance minimization** (GDPR/PDPA/PCI) | `scan` + `redact` | pii (region-tagged catalog), customRegex | no |
| **LLM guardrails** (pre/post LLM call) | `classify` / `sanitize` | all families, incl. LLM-as-judge | yes (optional) |

### 2.2 Design consequences of the reframing

1. **Deterministic checks are the product.** The core value is the PII catalog,
   secret scanning, URL validation, and typed redaction — all offline, exact,
   explainable. LLM checks are an optional extra family.
2. **Framework-agnostic core.** The core module has **zero runtime
   dependencies** (pure JDK 17). Spring AI lives in an adapter module.
3. **Streaming is a first-class citizen.** Pipelines and log redaction process
   arbitrarily large text with bounded memory.
4. **No side effects.** The core never logs input, never touches the network
   (unless an LLM check is explicitly configured), never persists anything.
5. **Pure function contract.** `text → outcome`; the caller decides routing,
   persistence, quarantine, or pass/fail branching.

---

## 3. Compliance / masking policy: guarantee scope

**The contract:** the library is a pure function

```
TextInput → [checks + redaction] → Outcome { passedChecks, failedChecks,
                                             maskEntities, redactedText,
                                             executionFailedAny }
```

It performs no side effects. Downstream consumers (loggers, LLM calls, storage,
notifications) act on the outputs the library hands them.

### 3.1 What the library guarantees

| # | Guarantee | Semantics |
| --- | --- | --- |
| G1 | **Deterministic exactness** | Regex/entropy/URL/keyword checks are pure functions of the input: same input ⇒ same result, always, on any JVM. No randomness, no time dependence. |
| G2 | **Complete redaction of detected tokens** | Every token a check reports in `maskEntities` is replaced; replacement is literal (never regex on the matched value) and longest-match ordered. The redacted text contains no detected raw token. |
| G3 | **Typed placeholders** | Every replacement is exactly `<ENTITY>` naming the policy rule (§7). The kind of data removed is preserved; its value is destroyed. |
| G4 | **Fail-safe** | An errored check is never a pass. `SANITIZE`/`redact` abort on error (fail-closed: never emit text that failed to be redacted). A rejected/malformed LLM verdict is an error, never a silent pass. |
| G5 | **Zero side effects** | The core never logs the input (raw or redacted), never persists, never makes network calls unless an LLM check is explicitly registered. |
| G6 | **Bounded memory streaming** | The streaming path peaks at O(window + match-set), never O(input). |
| G7 | **Reproducible audit data** | Audit records (when enabled) are returned as **data** in the outcome, not written to logs by the library; the caller persists them. |

### 3.2 What the library does NOT guarantee (claims, not guarantees)

| # | Non-guarantee | Why |
| --- | --- | --- |
| N1 | **LLM-as-judge accuracy** | A verdict is a stochastic model output, sensitive to prompt, model, temperature, and input wording. Any claim such as "jailbreak detection" is only as good as the specific model + prompt + threshold the caller configured. Treat LLM verdicts as **advisory signals**, never as a compliance guarantee. |
| N2 | **Exhaustive detection** | Regex catalogs are defeated by obfuscation (e.g. `j.o.h.n@x.com`), novel formats, or identifiers the catalog does not define. Detection is "what the catalog defines", nothing more. |
| N3 | **"Never logged" by the caller** | The library never logs, but it cannot control what the caller does with the outputs. The caller's discipline (log the redacted text, not the raw input) is outside the library. The library's contribution is that the redacted text is safe to log and the raw input is never emitted to any sink by the library itself. |
| N4 | **Downstream behavior** | The library does not guarantee what happens after output leaves it (e.g. that a model will not echo a placeholder, that a downstream system will honor the verdict). |
| N5 | **Zero false positives** | Entropy-based secret scanning and broad regexes trade precision for recall. Presets and catalog selection are the caller's precision/recall dial. |

> **Compliance reading:** in an audit, cite G1–G7 as the library's scope of
> assurance; present N1–N5 as explicit residual risks the deployment must
> manage (model choice, threshold tuning, caller-side logging policy, catalog
> selection).

---

## 4. Module & package layout

Two modules in the composite build. Core is standalone; the Spring AI adapter
depends on core + Spring AI. (This supersedes 03 §10's single-artifact
suggestion `com.khezylib:data-privacy-guardrails` — the split keeps the core
framework-agnostic, matching how the rest of `khezy-kit` structures opt-in
integrations.)

```
securities/data-privacy-core          io.github.khezyapp:data-privacy-core          v1.0.0
securities/data-privacy-spring-ai     io.github.khezyapp:data-privacy-spring-ai     v1.0.0
```

### 4.1 `securities/data-privacy-core` — build.gradle

```groovy
plugins {
    id("khezy.java-library")
}

group = "io.github.khezyapp"
version = "1.0.0"

mavenPublishing {
    pom {
        name = 'data-privacy-core'
        description = """Deterministic text-level data privacy engine: PII catalog, secret scanning, URL validation, typed redaction, streaming support, and a pluggable LLM-classifier SPI."""
    }
}
```

Package root: `io.github.khezyapp.dpriv` (hyphens stripped, per repo convention).

```
io.github.khezyapp.dpriv/
├── api/                    # public contract
│   ├── GuardrailResult.java     # uniform check result (theory §11 shape)
│   ├── GuardrailCheck.java      # @FunctionalInterface check over String
│   ├── StreamCheck.java         # streaming variant over Reader
│   ├── Guardrails.java          # façade: classify / sanitize / scan / redact
│   ├── GuardrailsConfig.java    # policy schema records (PiiConfig, SecretConfig, ...)
│   ├── RedactionPolicy.java     # what to scan + how to redact (log/pipeline entry)
│   ├── Operation.java           # CLASSIFY | SANITIZE
│   ├── GuardrailsOutcome.java   # classify/sanitize result
│   ├── ScanOutcome.java         # detect-only result (pipeline/DLP use)
│   └── LlmClassifier.java       # SPI, provider-agnostic (no Spring dep)
├── pipeline/
│   ├── GuardrailPipeline.java   # two-stage runner (in-memory)
│   ├── StreamPipeline.java      # two-pass streaming scanner + redactor
│   └── StageResult.java
├── redact/
│   ├── Redactor.java            # literal longest-match replacement (in-memory)
│   ├── StreamRedactor.java      # single-pass Aho–Corasick replacement
│   └── Placeholders.java        # <ENTITY> contract helpers
├── checks/
│   ├── PiiCheck.java
│   ├── SecretKeysCheck.java
│   ├── UrlsCheck.java
│   ├── KeywordsCheck.java
│   ├── CustomRegexCheck.java
│   └── LlmCheck.java            # generic Family-B check around LlmClassifier
├── policy/
│   ├── PiiEntity.java           # full catalog enum (§8)
│   ├── PiiPatterns.java         # entity → compiled patterns (single source of truth)
│   ├── SecretPresets.java       # STRICT / BALANCED / PERMISSIVE
│   ├── LlmPolicyPrompts.java    # jailbreak / nsfw / topicalAlignment / custom
│   └── LlmContract.java         # Verdict schema + validator + LLM_SYSTEM_RULES
├── stream/
│   ├── TextChunker.java         # windowed reader with overlap
│   ├── Tokenizer.java           # streaming whitespace tokenizer
│   └── MatchAccumulator.java    # dedupe + collect matches across windows
└── internal/
    ├── ParallelStageRunner.java # CompletableFuture stage runner + error wrap
    └── AhoCorasick.java         # literal multi-pattern matcher (longest-match)
```

### 4.2 `securities/data-privacy-spring-ai` — build.gradle

```groovy
plugins {
    id("khezy.java-library")
}

group = "io.github.khezyapp"
version = "1.0.0"

dependencies {
    api(project(":data-privacy-core"))   // composite build dependency
    implementation("org.springframework.ai:spring-ai-bom:1.0.0")  // BOM, version managed
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

mavenPublishing {
    pom {
        name = 'data-privacy-spring-ai'
        description = """Spring AI adapter for data-privacy-core: the canonical LlmClassifier implementation using Spring AI structured output."""
    }
}
```

Package root: `io.github.khezyapp.dpriv.springai`.

```
io.github.khezyapp.dpriv.springai/
└── SpringAiLlmClassifier.java   # canonical LlmClassifier (§11)
```

> Spring AI version note: pin at implementation time to the current GA line
> (1.0+); the adapter only uses the stable `ChatClient` +
> `StructuredOutputConverter` API surface, which is stable across 1.x.

---

## 5. Core contract types

### 5.1 `GuardrailResult` — the uniform contract (theory §11, unchanged shape)

```java
public record GuardrailResult(
    String guardrailName,                    // which check
    boolean tripwireTriggered,               // verdict
    Double confidenceScore,                  // LLM checks only, 0..1, else null
    boolean executionFailed,                 // errored != passed
    String exceptionMessage,                 // optional, when executionFailed
    Map<String, List<String>> maskEntities,  // entityType -> matched tokens (redaction input)
    Map<String, Object> info                 // check-specific extras (blocked URLs, keywords, ...)
) {
    public static GuardrailResult passed(String name) { ... }
    public static GuardrailResult failed(String name) { ... }
    public static GuardrailResult errored(String name, String message) { ... }
}
```

Every check, deterministic or model-based, in-memory or streaming, returns this
exact shape. `maskEntities` keys are the **policy rule names** (§7).

### 5.2 Checks

```java
@FunctionalInterface
public interface GuardrailCheck {
    GuardrailResult check(String inputText);
}

@FunctionalInterface
public interface StreamCheck {
    void scan(Reader input, MatchAccumulator sink);   // streaming variant; fills sink
}
```

- A check implements at most one of the two: `GuardrailCheck` (in-memory) or
  `StreamCheck` (streaming). Checks that implement both are tested for
  **output parity** (§14). Streaming checks never see the whole input; they
  consume the chunked stream and push matches into the accumulator.
- `GuardrailCheckFactory<C>` (03 §2) remains the extensibility seam: config → check.

### 5.3 Outcome records

```java
public record GuardrailsOutcome(
    String guardrailsInput,             // raw (classify) or redacted (sanitize) text
    List<GuardrailResult> passed,
    List<GuardrailResult> failed
) {
    public boolean isPassed() { return failed.isEmpty(); }
}

public record ScanOutcome(
    Map<String, List<String>> maskEntities,   // entityType -> matched tokens
    Map<String, Integer> entityCounts,        // entityType -> match count
    boolean executionFailedAny,
    List<GuardrailResult> results
) {}

public record AuditRecord(
    String operation,                 // "classify" | "sanitize" | "scan" | "redact"
    String checkName,
    boolean triggered,
    boolean executionFailed,
    Double confidenceScore,
    List<String> entityTypes          // entity types of matched tokens, NO raw values
) {}
```

- `AuditRecord` is **data**, returned (opt-in) in the outcome; the library never
  writes it to a log itself (G5). It carries metadata only — never raw tokens.

### 5.4 Policy schema (03 §7, as records)

```java
public record PiiConfig(PiiCoverage coverage, Set<PiiEntity> entities) {
    // PiiCoverage.ALL -> every catalog entity; PiiCoverage.SELECTED -> entities
    public static PiiConfig all()      { return new PiiConfig(PiiCoverage.ALL, Set.of()); }
    public static PiiConfig selected(PiiEntity... entities) { ... }
}

public record SecretConfig(SecretPreset preset, List<Pattern> customPatterns) {}
// SecretPreset: STRICT | BALANCED | PERMISSIVE  (tuple table §9.2)

public record UrlsConfig(Set<String> allowedSchemes, Set<String> allowedHosts,
                         boolean allowSubdomains, boolean blockUserinfo) {
    // defaults: allowedSchemes=[https], empty hosts => deny-by-default (block all)
}

public record KeywordsConfig(List<String> keywords) {}

public record CustomRegexConfig(String name, List<Pattern> patterns) {}

public record LlmCheckConfig(String name, String policyPrompt,
                             double threshold /* default 0.7 */) {}
```

`GuardrailsConfig` composes them; every section is optional with a sensible
default. `RedactionPolicy` is the slim entry for log/pipeline use:

```java
public record RedactionPolicy(PiiConfig pii, SecretConfig secrets,
                              UrlsConfig urls, List<CustomRegexConfig> customRegex) {
    public static RedactionPolicy defaults() { ... }   // pii=all, secrets=balanced, urls=defaults
}
```

---

## 6. Pipeline

The two-stage structure from the theory (§3) and 03 §3 is preserved exactly for
the in-memory path and mirrored by a streaming path for transformative work.

### 6.1 In-memory path (classify / sanitize)

```
run(String rawText, Operation op, GuardrailsConfig cfg, LlmClassifier llm)
  Stage 1: preflight (pii, customRegex, secretKeys, urls) in parallel, failOnlyOnErrors = (op == SANITIZE)
  if any stage-1 failure -> return GuardrailsOutcome.failed(rawText, stage1.failed)   // short-circuit
  masked = Redactor.apply(rawText, stage1.passed)     // literal, longest-match, <ENTITY>
  if op == SANITIZE -> return GuardrailsOutcome.passed(masked, stage1.passed)
  Stage 2: input checks (keywords, llm*) on MASKED text, in parallel
  return failed.isEmpty() ? passed(masked, concat) : failed(masked, stage2.failed)
```

Stage runner: `CompletableFuture` per check on a bounded executor; every thrown
exception is wrapped into `GuardrailResult.errored(...)` — never dropped,
never a pass (G4). Grouping rule (from 02 §4, verbatim semantics):

```
failed  = rejected || executionFailed || (classify-stage && tripwireTriggered)
```

### 6.2 Streaming path (detect + redact — the data-pipeline/log use case)

```
scan(Reader in, RedactionPolicy policy) -> ScanOutcome          // detect only
redact(Reader in, Writer out, RedactionPolicy policy) -> void   // detect + replace
```

Both are **two-pass over the stream**:

1. **Pass 1 — scan:** run the configured `StreamCheck`s over the chunked input
   (windowed `TextChunker`, §10.1), collecting `maskEntities` with cross-window
   dedupe.
2. **Pass 2 — redact:** rebuild a literal longest-match matcher
   (Aho–Corasick, §7.2) from `maskEntities` and stream the input again,
   writing replaced text to the `Writer`.

Peak memory: O(window size + matched-token set), never O(input) (G6).
`redact` is fail-closed: if any check errored during pass 1, throw before
writing a single byte (G4).

### 6.3 Why classification (incl. LLM) is not streaming (decision §1.4)

- **Model checks must ship the text to a model API** with a bounded context
  window anyway — materializing a bounded `String` is unavoidable and honest.
- **Keywords** run on the masked text; the masked text is already materialized
  in the classify path, so a streaming keyword matcher would buy nothing.
- The streaming API therefore covers the **transformative path** (detect +
  redact), which is exactly what pipelines and pre-log redaction need.
- Future work (not v1): chunked classify for very large inputs with
  sentence-boundary chunking — deferred, see §16.

---

## 7. Redactor & the `<ENTITY>` placeholder contract

### 7.1 Placeholder contract (decision §1.2)

- Format is **exactly `<ENTITY>`** — fixed, documented, not configurable.
  Downstream consumers can rely on the literal format and on the fact that a
  placeholder never appears unless the named policy rule matched.
- `ENTITY` = the **policy rule name** that produced the match:

| Rule source | Entity name | Example |
| --- | --- | --- |
| PII catalog entity | `PiiEntity.name()` | `john@x.com` → `<EMAIL_ADDRESS>` |
| Secret scanning | constant `SECRET_KEY` | `sk-abc…` → `<SECRET_KEY>` |
| URL validation | constant `URL` | `https://evil.example` → `<URL>` |
| Custom regex | the user-supplied rule name | `<CUSTOM_NAME>` (uppercased) |

- Nested/overlapping protection: replacement is longest-match ordered (theory
  §8) — a large token is never partially replaced by a substring rule, and a
  placeholder is never matched by a later rule (placeholders themselves are
  excluded from scanning when the pipeline reuses redacted text).
- `Placeholders.of(entityType)` = `"<" + entityType + ">"`, with validation:
  entity names are `[A-Z0-9_]+`.

### 7.2 Replacement engine

In-memory (reference semantics, verbatim from 03 §4): literal `String.replace`
over a longest-first sorted mapping.

Streaming: the **same semantics, single pass** — an Aho–Corasick automaton over
the matched tokens with longest-match tie-breaking at each position. For any
position it emits the placeholder of the longest pattern ending there and skips
past it. This yields the same output as the sorted-longest-first literal
replacement (nested and same-start overlaps resolve identically; token
occurrences are disjoint and fully consumed). A `StreamRedactor` wraps the
automaton in a buffered character loop so output is written incrementally to a
`Writer`.

Both engines are unit-tested for **parity**: for any input, in-memory and
streaming redaction must produce identical output (§14).

---

## 8. PII catalog — the full entity list (decision §1.3)

**Decision: port the complete upstream catalog — no subset.** The enum below is
the canonical list (33 entities, region-tagged, from 03 §5.1); `PiiPatterns`
carries the per-entity compiled patterns as the single source of truth and
**must contain every entity the upstream `DEFAULT_PII_PATTERNS` table defines**
(ported 1:1, MIT attribution kept in the module LICENSE/CREDITS file).

| Region | Entity | Notes |
| --- | --- | --- |
| Global | `CREDIT_CARD` | Luhn-validatable (see §8.2) |
| Global | `CRYPTO` | BTC/ETH wallet address patterns |
| Global | `EMAIL_ADDRESS` | |
| Global | `IP_ADDRESS` | IPv4 + IPv6 |
| Global | `PHONE_NUMBER` | international formats |
| Global | `IBAN_CODE` | mod-97 validatable (§8.2) |
| Global | `LOCATION` | |
| Global | `DATE_TIME` | |
| Global | `MEDICAL_LICENSE` | |
| US | `US_BANK_NUMBER` | routing/account |
| US | `US_DRIVER_LICENSE` | |
| US | `US_ITIN` | |
| US | `US_PASSPORT` | |
| US | `US_SSN` | |
| UK | `UK_NHS` | |
| UK | `UK_NINO` | |
| ES | `ES_NIF` | |
| ES | `ES_NIE` | |
| IT | `IT_FISCAL_CODE` | 16-char alphanumeric |
| IT | `IT_VAT_CODE` | |
| PL | `PL_PESEL` | |
| SG | `SG_NRIC_FIN` | |
| SG | `SG_UEN` | |
| AU | `AU_ABN` | |
| AU | `AU_ACN` | |
| AU | `AU_TFN` | |
| AU | `AU_MEDICARE` | |
| IN | `IN_PAN` | |
| IN | `IN_AADHAAR` | Verhoeff-validatable (§8.2) |
| IN | `IN_VEHICLE_REGISTRATION` | |
| IN | `IN_VOTER` | |
| IN | `IN_PASSPORT` | |
| FI | `FI_PERSONAL_IDENTITY_CODE` | |

### 8.1 Analyzer engine

- `PiiPatterns` precompiles every entity's patterns at class load
  (`EnumMap<PiiEntity, Pattern[]>`, `Pattern.CASE_INSENSITIVE | UNICODE_CASE`
  where the upstream table is case-insensitive). **Never compile per call.**
- `PiiCheck` iterates the configured coverage (ALL = every enum value,
  SELECTED = caller's set) and runs `Matcher.find()` with region advancement
  (the Java equivalent of the JS `/g` flag).
- Result: `maskEntities = { entityName -> [matched snippets] }`;
  `tripwireTriggered = !matches.isEmpty()`.
- `CustomRegexCheck` reuses the same analyzer with user patterns — the
  extensibility seam (02 §5.1).

### 8.2 Optional checksum validation (design addition, opt-in `strict` flag)

Pure regex catalog detection is exact for formats but noisy for
checksum-validated identifiers. Add an **optional validation layer** behind a
`ChecksumValidator` SPI, enabled per entity via `PiiConfig.strict`:

| Entity | Validator |
| --- | --- |
| `CREDIT_CARD` | Luhn |
| `IBAN_CODE` | country length + mod-97 |
| `IN_AADHAAR` | Verhoeff |

When enabled, a regex match that fails the checksum is reported in `info`
(`maskEntities` keeps only validated matches). Default: **off** (parity with
upstream behavior); the flag is the precision/recall dial for audits.

---

## 9. Family A checks (deterministic)

### 9.1 `PiiCheck` — §8 above.

### 9.2 `SecretKeysCheck` — entropy + heuristics (theory §5, 02 §5.2)

Decision procedure per whitespace-delimited token (streaming via `Tokenizer`):

```
token → (strict ? skip nothing : skip if URL or allowed file extension)
      → length + char-diversity gate
      → known prefix ⇒ secret (unconditional)
      → shannonEntropy(token) >= minEntropy ⇒ secret
```

Presets map 1:1 to the reference table:

| Preset | min_length | min_entropy | min_diversity | strict_mode |
| --- | --- | --- | --- | --- |
| `STRICT` | 10 | 3.0 | 2 | true |
| `BALANCED` | 10 | 3.8 | 3 | false |
| `PERMISSIVE` | 30 | 4.0 | 2 | false |

Entity name for redaction: `SECRET_KEY`. Known prefixes (`sk-`, `sk_`, `ghp_`,
`AKIA`, `xox`, `SG.`, `hf_`, `api-`, `Bearer `, …) ported verbatim from the
reference.

### 9.3 `UrlsCheck` — staged validator (theory §7, 02 §5.3)

```
detect (3 passes: scheme-ful, scheme-less domain, bare IP; trailing-punctuation
        cleanup; cross-pass dedupe)
  → parse (java.net.URI; single-colon special schemes handled manually)
  → scheme allowlist (default [https])
  → userinfo block (blockUserinfo, default true)
  → host allowlist: exact | subdomain | CIDR  (empty ⇒ block all, deny-by-default)
```

Entity name for redaction: `URL`. Matched (blocked) URLs also surface in
`info.blockedUrls` with the rejection reason (scheme/userinfo/host).

### 9.4 `KeywordsCheck` — unicode-aware filter (02 §5.4)

Case-insensitive matching with `\p{L}|\p{N}|_` boundaries (not `\b`, which is
ASCII-only), punctuation-adjacent matches supported, unique case-folded matches
in `info.matchedKeywords`. Classificatory only — no `maskEntities`.

### 9.5 `CustomRegexCheck`

User-named patterns; runs on the `PiiCheck` analyzer; entity name = the user's
rule name (uppercased); same streaming support.

---

## 10. Streaming engine (decision §1.4)

### 10.1 `TextChunker` — windowed reader with overlap

- Reads the `Reader` in fixed-size chunks (`windowSize`, default 64 KB) with a
  configurable `overlap` (default 1 KB) carried across chunk boundaries.
- Overlap exists so a match straddling a chunk boundary is found whole in the
  next window. Overlap ≥ the longest pattern the check set can produce
  (patterns are bounded; for user regexes the caller can raise overlap).
- Each chunk + overlap is presented to the checks; the accumulator dedupes
  matches that appear in two consecutive windows (by token value + entity).
- `scan(Reader, StreamCheck[], sink)`: loops windows until EOF, then flushes.

### 10.2 Bounded memory guarantee

Peak memory = `windowSize + overlap + |matchSet|`. The match set is bounded by
the number of *detected* tokens, not by input size; the input itself is never
materialized. This is the G6 guarantee and the contract of the streaming API.

### 10.3 Streaming variants per check

| Check | Streaming strategy |
| --- | --- |
| `PiiCheck` | windowed regex scan (`TextChunker`), overlap-aware |
| `CustomRegexCheck` | same engine |
| `SecretKeysCheck` | streaming `Tokenizer` (whitespace) + per-token decision |
| `UrlsCheck` | windowed URL detection; parse/validate per detected candidate |
| `KeywordsCheck` | windowed unicode-boundary matcher (not used in streaming redact path) |
| LLM checks | n/a — in-memory only (§6.3) |

Parity rule: every check implementing both `GuardrailCheck` and `StreamCheck`
must produce identical `maskEntities` for the same input (§14.5).

---

## 11. Family B: LLM-as-judge checks — Spring AI is the default (decision §1.1)

### 11.1 The SPI stays in core (zero Spring dependency)

```java
public interface LlmClassifier {
    record Verdict(double confidenceScore, boolean flagged) {}

    Verdict classify(String policyPrompt, String systemRules, String inputText);
}
```

- Core ships `LlmCheck` (name, policy prompt, threshold) built on this SPI +
  the `LlmContract` validator, so the whole Family-B machinery (threshold
  semantics, fail-safe parse handling, prompt assembly) is tested without any
  model (FakeLlmClassifier in tests).
- Providers: `SpringAiLlmClassifier` (canonical, adapter module), plus the
  seam for LangChain4j / direct clients later.

### 11.2 The contract (unchanged from theory §4 / 02 §6)

```
model(system = policyPrompt + "\n" + formatInstructions + "\n" + LLM_SYSTEM_RULES,
      user   = maskedInputText)
  → { "confidenceScore": 0..1, "flagged": boolean }
triggered = flagged && confidenceScore >= threshold        // threshold default 0.7
parse/validation failure  → GuardrailResult.errored(...)   // fail-safe (G4), never a pass
```

`LlmContract` validates the parsed verdict (range, NaN, missing fields) — the
Jackson deserialization in the adapter is only the first gate; core validation
is the authoritative one.

### 11.3 `SpringAiLlmClassifier` — the canonical implementation

```java
public final class SpringAiLlmClassifier implements LlmClassifier {

    private final ChatClient chatClient;
    private final StructuredOutputConverter<Verdict> converter;

    public SpringAiLlmClassifier(final ChatClient chatClient) {
        this.chatClient = chatClient;
        this.converter = new BeanOutputConverter<>(Verdict.class);
    }

    @Override
    public Verdict classify(final String policyPrompt, final String systemRules,
                            final String inputText) {
        final var fullSystem = policyPrompt + "\n" + FORMAT_INSTRUCTIONS + "\n" + systemRules;
        final var response = chatClient.prompt()
            .system(fullSystem)
            .user(inputText)
            .call();
        return converter.convert(response.getContent());
    }
}
```

- `Verdict` is a Jackson-mapped record (`@JsonIgnoreProperties(ignoreUnknown =
  true)` on the record or the mapper config; strictness on required fields via
  the core validator).
- Constructor takes a `ChatClient`; **a null/unconfigured client fails at
  construction** with `"LLM classifier requires a ChatClient"` — the Java
  analog of the reference's "Chat Model is required", surfaced early, not at
  request time.
- Threshold and prompt assembly live in core (`LlmCheck`), so the adapter
  stays a thin transport.

### 11.4 Built-in policy prompts (ported from 02 §6)

| Check | Policy prompt intent |
| --- | --- |
| `jailbreak` | adversarial manipulation of safety constraints (circumvention, roleplay-unrestricted, indirect phrasing, prompt injection, obfuscation) — distinct from merely harmful content |
| `nsfw` | content-moderation taxonomy: sexual, hate, harassment, violence, self-harm, profanity, illegal activity, adult themes, extremism, exploitation, graphic medical |
| `topicalAlignment` | "BUSINESS SCOPE: [placeholder] — stays/strays" |
| `custom` | caller-provided semantic policy |

All four share `LlmCheck` + `LlmContract` with a different prompt + name.

### 11.5 Claims, not guarantees (N1)

LLM verdicts are advisory. The design documents in the module Javadoc and README:
*"LLM-as-judge checks are best-effort semantic signals; they are not a
compliance guarantee. Calibrate the threshold, choose the model deliberately,
and treat triggered checks as routing hints — never as the sole basis for a
compliance claim."* Deterministic checks (G1–G3) are the only basis for
compliance assertions.

---

## 12. Public API & usage scenarios

`Guardrails` façade (03 §7) plus the streaming entry points:

```java
public final class Guardrails {

    // --- in-memory, full pipeline ---
    public static GuardrailsOutcome run(String text, Operation op,
                                        GuardrailsConfig config, LlmClassifier classifier);

    // --- detect only (DLP / quarantine) ---
    public static ScanOutcome scan(String text, RedactionPolicy policy);
    public static ScanOutcome scan(Reader in, RedactionPolicy policy) throws IOException;

    // --- redact (logs, pipelines, API sanitization) ---
    public static String redact(String text, RedactionPolicy policy);
    public static void redact(Reader in, Writer out, RedactionPolicy policy) throws IOException;
}
```

### 12.1 Redact before logging

```java
final var policy = RedactionPolicy.defaults();
LOGGER.info(Guardrails.redact(userMessage, policy));   // <EMAIL_ADDRESS>, <US_SSN>, ...
```

### 12.2 Data pipeline sanitization (streaming)

```java
try (final var in = Files.newBufferedReader(sinkFile);
     final var out = Files.newBufferedWriter(targetFile)) {
    Guardrails.redact(in, out, policy);          // bounded memory, any size
}
```

### 12.3 DLP scan & quarantine

```java
final var outcome = Guardrails.scan(in, policy);
if (outcome.executionFailedAny) { routeToQuarantine(); }          // fail-safe
if (!outcome.maskEntities.isEmpty()) {                           // counts for compliance
    final var counts = outcome.entityCounts;                     // {EMAIL_ADDRESS: 3, US_SSN: 1}
    notifyDlp(outcome.entityTypes());                            // metadata only, no raw values
}
```

### 12.4 LLM guardrails (one use case among several)

```java
final var config = GuardrailsConfig.builder()
    .pii(PiiConfig.all())
    .secrets(SecretConfig.balanced())
    .jailbreak(new LlmCheckConfig("jailbreak", LlmPolicyPrompts.JAILBREAK, 0.7))
    .build();

final var outcome = Guardrails.run(userInput, Operation.CLASSIFY,
                                   config, new SpringAiLlmClassifier(chatClient));
final var textToSend = outcome.isPassed() ? outcome.guardrailsInput() : fallback();
```

---

## 13. Threading & failure policy (03 §8, confirmed)

- Stage checks run on a shared bounded executor; model checks dominate cost —
  size for them; deterministic checks are cheap.
- `CompletableFuture.allOf` + per-future error wrap ⇒ `executionFailed` (G4).
- `SANITIZE`/`redact` throw on any check error (fail-closed, G4).
- No logging by the library; if audit data is requested it is returned in the
  outcome (`AuditRecord`, §5.3), never written by the library (G5).

---

## 14. Testing strategy

JUnit 5 + AssertJ, no network in core tests. Test data uses Khmer names and
Cambodia locations per repo convention (e.g. `visal@example.com`,
`SOK`, `Phnom Penh`).

1. **PII:** every entity's pattern on positive/negative fixtures (one test per
   entity, parameterized); ALL vs SELECTED coverage; strict checksum layer
   (Luhn/mod-97/Verhoeff) on/off; redaction produces `<ENTITY>` placeholders.
2. **SecretKeys:** entropy math (known high/low entropy strings); each preset;
   prefix hits; URL/file-extension denylist honored only when not strict.
3. **URLs:** scheme block, userinfo block, exact/subdomain/CIDR allowlist,
   empty allowlist ⇒ block all, special schemes (`data:`, `mailto:`, …).
4. **Keywords:** unicode boundaries; punctuation-adjacent matches.
5. **Redactor:** literal replacement (no regex injection: token `$` `\` `+`
   etc.); longest-match ordering; **streaming/in-memory parity** — same input,
   byte-identical output; placeholders never re-matched.
6. **Streaming:** chunk-boundary straddling matches (match split across two
   windows is found once); overlap correctness; bounded-memory assertion
   (process a large generated input with a tiny window); fail-closed on error
   before any output byte is written.
7. **Pipeline:** preflight short-circuit; masking before input stage (fake LLM
   receives masked text); classify pass/fail routing; sanitize single-output +
   fail-closed on error.
8. **LLM checks (core):** `FakeLlmClassifier` — threshold semantics
   (`triggered = flagged && confidence >= threshold`), out-of-range/NaN verdict
   ⇒ `executionFailed`, malformed JSON ⇒ `executionFailed`.
9. **Spring AI adapter:** `ChatClient` mocked (Spring AI's test
   `MockedChatClient`/`ChatClientAutoConfiguration` test support) — assert
   prompt assembly (system = policy + format + rules, user = input), converter
   mapping, and that a null `ChatClient` fails construction.
10. **Guarantee-scope regression:** tests that lock G1 (same input ⇒ same
    output), G4 (error ⇒ never pass), G5 (no output written to stderr/stdout by
    the library on any path).

---

## 15. Relation to existing modules

| Module | Relationship |
| --- | --- |
| `data-masker` (`securities/data-masker`, `io.github.khezyapp.datamasker`) | **Complementary, compose cleanly.** `data-masker` masks *structured objects* (annotations, key patterns on Maps/POJOs). `data-privacy-core` scans *free text*. Composition: mask a POJO field whose value is free text by running `Guardrails.redact` inside a `SensitiveMaskerStrategy` — structured masking for containers, text-level detection for content. A follow-up task may ship this bridge strategy. |
| `clone-util`, `dynamic-object`, `string-util`, etc. | Not involved. Core is dependency-free (pure JDK). |
| `declarative-http` | Potential consumer: redact request/response bodies before logging in `JdkHttpTransport` traces — out of scope here, noted as an integration example for the README. |

---

## 16. Non-goals & future work

- **Chunked LLM classification** of very large inputs (sentence-boundary
  chunking + per-chunk verdict) — deferred; classify is in-memory in v1 (§6.3).
- **Structured-object scanning** (nested JSON/POJO traversal) — `data-masker`
  covers object graphs; a JSON-body variant of the PII scan may be a separate
  artifact later.
- **Persistence, log sinks, notification** — caller's job; the library emits
  data, never acts.
- **Placeholder configurability** — deliberately rejected (§7.1): a fixed
  `<ENTITY>` contract keeps downstream parsing stable and audits deterministic.
- **Guaranteed-evasion-proof detection** — impossible (N2); documented as a
  claim boundary, not a feature.

---

## 17. Standards & attribution

- Controls traced to standards (theory §1): **OWASP LLM01** (jailbreak/custom),
  **OWASP LLM02** (PII/secretKeys/urls), **OWASP LLM05** (sanitize),
  **NIST AI RMF** (MEASURE/MANAGE), **OWASP SSRF Prevention Cheat Sheet**
  (URL staged validator), DLP analyzer pattern (Presidio/Cloud DLP/Macie),
  entropy secret scanning (TruffleHog/detect-secrets lineage).
- Reference implementation: **n8n Guardrails** node (MIT) — contract semantics
  ported, not code copied.
- PII/URL/keyword regex tables derived from **OpenAI Guardrails JS** (MIT) —
  full table port with attribution kept in `CREDITS.md` of
  `data-privacy-core` (per the repo's research docs note).
- GDPR/PDPA/PCI relevance: typed redaction + region-tagged catalog + audit
  metadata data path are the compliance-facing surfaces (G1–G7 scope, §3).

---

_Back to index: [`../README.md`](../README.md)._
