# Data Privacy Spring AI — ChatClient Advisors Design (v2)

> **Purpose of this doc:** the resolved, implementation-ready design for **v2** of
> `data-privacy-spring-ai` (currently v1.0.0, in development): a **pair of
> built-in Spring AI advisors** that plug into a `ChatClient`:
>
> - **`DataPrivacyAdvisor`** — the **mitigate-risk** pattern: deterministically
>   redacts sensitive text (PII, secrets, URLs, keywords, custom regex) before it
>   reaches the model — and optionally after, on the response.
> - **`GuardrailAdvisor`** — the **prevent** pattern: LLM-as-judge classification
>   (jailbreak, NSFW, topical, custom families) that **blocks** malicious input
>   and unsafe output before/after the model call.
>
> v1 shipped the engines: `data-privacy-core` (deterministic scan/redact
> pipeline, `<ENTITY>` placeholders, `Operation.CLASSIFY`/`SANITIZE`,
> fail-closed semantics) and `data-privacy-spring-ai` (the `LlmClassifier`
> adapter). **v2 adds the zero-effort integration points** — one advisor per
> pattern, one builder each, sensible defaults.
>
> **Source contract:** v1 design
> [`data_privacy_core_design.md`](./data_privacy_core_design.md) (guarantees
> G1–G7, placeholder contract, `Guardrails` facade, `LlmClassifier` SPI). This
> doc does not re-open v1 decisions; it layers the advisors on top.
>
> **Standards grounding:** OWASP GenAI Security Project — *LLM Top 10 for
> LLM/GenAI Applications (2025)*, and NIST AI RMF + GenAI Profile. §4 maps every
> advisor to the specific LLM risks it mitigates.

---

## 1. The problem v2 solves

Today, using `data-privacy-spring-ai` in an app means wiring `Guardrails` into
your `ChatClient` calls by hand:

```java
String safe = guardrails.redact(userInput);          // manual redaction
ChatClientResponse r = client.prompt().user(safe).call();
// ...and a second, different loop for classification:
GuardrailsOutcome o = guardrails.run(userInput, Operation.CLASSIFY);
if (o.detected()) throw new BlockedException(...);
```

Every developer re-implements the same loops: extract the user text, redact or
classify it, put it back, remember to do it for streaming too, forget to do it
in one place. The v2 advisors make this **automatic and structural** — the
framework's own advisor chain applies the control at the right point in every
request, whether `call()`, `stream()`, chat memory, or tool loops.

### 1.1 The simple use cases (v2 scope)

| # | Use case | Advisor | One-liner |
| --- | --- | --- | --- |
| 1 | **"Sensitive data in the user's message must never reach the model."** | `DataPrivacyAdvisor` | `.defaultAdvisors(DataPrivacyAdvisor.builder().build())` |
| 2 | **"Block jailbreak/injection attempts and unsafe content."** | `GuardrailAdvisor` | `.defaultAdvisors(GuardrailAdvisor.builder().config(cfg).build())` |
| 3 | **"Both."** (defense in depth) | both, defaults compose | two lines, ordering handled (§11) |

Everything else (reversible masking, un-redaction, tool-call gating, Boot
starters) is explicitly **out of scope** (§15).

---

## 2. The two security patterns (read this first)

The user-facing vocabulary in this design comes from two classic security
control patterns that the LLM Top 10 guidance expects applications to combine.
They answer a different question each:

| | **Mitigate risk (transform)** | **Prevent (gate)** |
| --- | --- | --- |
| Question | "How do I reduce the harm when sensitive data flows through my app?" | "How do I stop the attack before it reaches the model?" |
| Mechanic | **Sanitize**: replace the dangerous data with safe placeholders. The data is transformed so it cannot be abused, leaked, stored, or trained on. | **Block**: classify the content; if it violates policy, the request is rejected with an exception. The attacker gets no response. |
| Costs | Deterministic, cheap, no false positives that block legit users. Cannot *detect* an attack — it only cleans data. | Detects attacks (LLM-as-judge), but adds latency + model cost per call, and is stochastic (accuracy is model/prompt/threshold dependent — core N1). |
| Failure mode | Fail-closed: if the pipeline errors, do not send unredacted text. | Fail-closed: if the judge errors, do not silently let the content through. |
| OWASP anchor | **LLM02** Sensitive Information Disclosure | **LLM01** Prompt Injection, **LLM05** Improper Output Handling, **LLM07** System Prompt Leakage |
| v2 artifact | `DataPrivacyAdvisor` | `GuardrailAdvisor` |

**Defense in depth:** the two patterns compose. OWASP LLM01's mitigations
explicitly call for *"implement input and output filtering"* — and LLM02 calls
for both redaction of sensitive data *and* output filtering. The classic layering:

```
User input ──► [MITIGATE: redact PII] ──► [PREVENT: block attacks] ──► MODEL
                                                    │
Caller ◄── [MITIGATE: scrub output] ◄── [PREVENT: vet output] ◄───────┘
```

- **Prevent what you can detect** (a jailbreak attempt), **sanitize what you
  can transform** (an email address in a benign message). Neither subsumes the
  other: redaction cannot stop an attack; classification cannot protect data
  the model was already given.
- A key consequence (§8.5): `GuardrailAdvisor` **never modifies the request** —
  it only allows or blocks. If it passes raw text through, the model sees raw
  text. Pairing it with `DataPrivacyAdvisor` is what actually protects the data
  (and v1's `CLASSIFY` pipeline already redacts internally for the *judge's*
  eyes only — not for the main model).

---

## 3. Resolved decisions

| # | Question | Decision | Where |
| --- | --- | --- | --- |
| 1 | How many advisors? | **Two, one per pattern**: `DataPrivacyAdvisor` (mitigate/redact) + `GuardrailAdvisor` (prevent/gate). Separate classes, shared vocabulary types. | §2, §6, §7 |
| 2 | Which advisor SPI? | **`BaseAdvisor`** (`org.springframework.ai.chat.client.advisor.api`). Implement `before(ChatClientRequest, AdvisorChain)` + `after(ChatClientResponse, AdvisorChain)`; the framework's default `adviseCall` handles the call path. | §8 |
| 3 | Where does each advisor apply? | Shared **`ProtectionScope`** (`INPUT` / `OUTPUT` / `BOTH`), default **`INPUT`** for both: input protection is the product; output is opt-in (leak / unsafe-content mitigation). | §7, §10 |
| 4 | Which messages does `DataPrivacyAdvisor` redact? | **`MessageType.USER` messages only**, text content only (media parts untouched). `RedactMode` = `ALL` (default — every user message, incl. history) vs `LAST_ONLY`. Uses the framework's own `Prompt` helpers: `Prompt.getUserMessages()` (ALL) and `Prompt.getLastUserOrToolResponseMessage()` (LAST_ONLY, with a USER-type check). | §7, §8.1 |
| 5 | Which `Guardrails` entry point for redaction? | **`Guardrails.run(text, Operation.SANITIZE)`**, not bare `redact()`. Same pipeline, same fail-closed semantics, but returns the `GuardrailsOutcome` with `maskEntities` + `auditRecords` in the same single pass — needed for the observability report (§9). | §8.1 |
| 6 | Which `Guardrails` entry point for gating? | **`Guardrails.run(text, Operation.CLASSIFY)`** on the last user message (the new input; history was vetted when it entered). Errored judge ≠ pass (core fail-safe) → see #8. | §8.5 |
| 7 | Failure semantics — redaction? | **Fail-closed default** (`failOnError=true`): check error ⇒ throw **`RedactionException`**, request aborted, unredacted text never sent. `false` = best-effort pass-through (explicit weakening). | §8.4, §5 |
| 8 | Failure semantics — gating? | **Fail-closed always for violations**: a flagged verdict always throws **`PolicyViolationException`** (that is the prevent pattern's contract). Judge *infra* errors throw **`GuardrailEvaluationException`** by default (`failOnError=true`); `false` treats them as pass (documented weakening). | §8.8, §5 |
| 9 | Custom exceptions? | **Yes — three, with one base**: `DataPrivacyException` → `RedactionException`, `PolicyViolationException`, `GuardrailEvaluationException`. Consumers catch one type per reaction scenario (§8.9). | §7, §8.9 |
| 10 | Default advisor order? | `DataPrivacyAdvisor` = **`Ordered.HIGHEST_PRECEDENCE`**, `GuardrailAdvisor` = `HIGHEST_PRECEDENCE + 1`. Resulting composition (chain mechanics in §11): input = **redact → classify**, output = **classify → redact**. | §11 |
| 11 | Streaming output handling? | Both advisors override `adviseStream` using the framework's **`ChatClientMessageAggregator`** pattern (like `MessageChatMemoryAdvisor` / `SimpleLoggerAdvisor`): stream chunks carry deltas, so the finish-reason response alone does not hold the full text — aggregation guarantees `after` sees the complete output. | §8.3, §8.7 |
| 12 | Reversible masking / un-redaction? | **No.** v1 fixed the irreversible `<ENTITY>` placeholder contract (core G3). Un-redaction is a re-identification risk and a non-goal. | §5, §15 |
| 13 | Factory classes? | **No `...Factory`.** Both advisors are model-agnostic (wrap `Guardrails`); a fluent builder each is the whole public surface. The v1 `SpringAiLlmClassifierFactory` already covers the ChatClient-binding concern. | §7 |
| 14 | Version bump? | **None.** Module stays at `1.0.0` — the data-privacy modules are still in development (not yet released); v2 ships inside the same pre-release version. | §13 |
| 15 | Observability? | Each advisor writes its report (`RedactionReport`, `GuardrailReport`) into the request `context` under a namespaced key. No logging, ever (core G5). | §9 |

---

## 4. OWASP LLM Top 10 (2025) mapping

Both advisors are text-boundary controls: they protect the *in/out edges* of the
model call. The table below shows which OWASP risks each advisor addresses and
which are deliberately out of scope.

| OWASP 2025 risk | Addressed? | Advisor + scope | Mechanism |
| --- | --- | --- | --- |
| **LLM01 Prompt Injection** (direct, indirect, jailbreak) | ✅ partial | `GuardrailAdvisor` `INPUT` (jailbreak family, custom families) + `OUTPUT` (detect model complying with injected instructions) | LLM-as-judge, threshold-gated, fail-closed. Indirect injection (RAG/doc content) is *not* covered — that content does not flow through the advisor chain; note in §15. |
| **LLM02 Sensitive Information Disclosure** | ✅ | `DataPrivacyAdvisor` `INPUT` (redact before model) + `OUTPUT` (scrub leaked/echoed data) | Deterministic PII/secrets/URLs/keywords redaction → `<ENTITY>` placeholders. Model never receives raw matched tokens (G8); output scrub catches echoes (G9). |
| **LLM05 Improper Output Handling** | ✅ | `GuardrailAdvisor` `OUTPUT` | Model output is validated by classification (NSFW / topical / custom) before the caller receives it; violation ⇒ `PolicyViolationException(scope=OUTPUT)`. |
| **LLM07 System Prompt Leakage** | ✅ via custom classifier | `GuardrailAdvisor` `OUTPUT` | Consumers register a custom `LlmClassifier` family tuned to detect system-prompt fragments; the advisor blocks/fails the response. |
| **LLM06 Excessive Agency** | ❌ future | — | Requires tool-call interception (vetting *actions*, not text). v3 candidate. |
| **LLM03 Supply Chain / LLM04 Data & Model Poisoning / LLM08 Vector & Embedding / LLM09 Misinformation / LLM10 Unbounded Consumption** | ❌ out of scope | — | Not text-boundary controls; outside the advisor's lane (documented in §15). |

> **Compliance reading:** this module is the *runtime input/output filtering*
> layer of an LLM security program — it implements OWASP LLM01 mitigation #3
> ("implement input and output filtering") and the input-redaction + output
> scrubbing controls for LLM02. It does **not** cover model-level safety, RAG
> content segregation, or agent permissions (LLM01 mitigations #1/#4/#5/#6).

---

## 5. Guarantee scope (v2 — advisor layer)

Extends v1 guarantees G1–G7 (core engine) with each advisor's own contract. The
v1 non-guarantees N1–N5 (LLM accuracy, exhaustive detection, caller discipline,
downstream behavior, false positives) **carry over unchanged**.

### 5.1 What v2 guarantees

| # | Guarantee | Advisor | Semantics |
| --- | --- | --- | --- |
| G8 | **Input never leaks raw** | `DataPrivacyAdvisor` | With scope `INPUT`/`BOTH`, text sent to the model for every USER message matches the configured pipeline exactly — raw matched tokens replaced before the call (call and stream paths). |
| G9 | **Output scrubbed on the way back** | `DataPrivacyAdvisor` | With scope `OUTPUT`/`BOTH`, the final response text the caller receives has matched tokens replaced (full-text aggregation on stream). |
| G10 | **Redaction fail-closed** | `DataPrivacyAdvisor` | `failOnError=true`: any check error aborts the request before the model call — no unredacted fallback. |
| G11 | **Non-interference** | both | System messages, tool definitions, tool-call/tool-response messages, media parts, metadata, and context are never modified; only USER-message text is transformed (by `DataPrivacyAdvisor`). |
| G12 | **Idempotent redaction** | `DataPrivacyAdvisor` | Redacting already-redacted text is a no-op: `<ENTITY>` placeholders are not re-detected; safe with memory advisors and repeated application. |
| G13 | **Zero side effects** | both | Never log input (raw or redacted), never persist, never call the network (except configured LLM classifiers). Observability is data in the request context (§9). |
| G14 | **Violations always blocked** | `GuardrailAdvisor` | A flagged verdict (≥ threshold, any family) always throws `PolicyViolationException` — there is no configuration that silently lets a detected violation through. |
| G15 | **Judge fail-closed** | `GuardrailAdvisor` | `failOnError=true`: an errored judge aborts the request (`GuardrailEvaluationException`) — errored ≠ pass (inherits core G4). |
| G16 | **Input gating on the new input** | `GuardrailAdvisor` | `INPUT` classification targets the last user message (not the whole history); history was vetted when it entered the conversation. |

### 5.2 What v2 does NOT guarantee

| # | Non-guarantee | Advisor | Why |
| --- | --- | --- | --- |
| N6 | **No model echo** | `DataPrivacyAdvisor` | Cannot force a model not to repeat placeholders or synthesize sensitive-looking text. `OUTPUT` scrubbing mitigates; it cannot prove absence (inherits N4). |
| N7 | **No data loss recovery** | `DataPrivacyAdvisor` | Placeholders are irreversible by design; no un-redaction (re-identification risk). |
| N8 | **Judge accuracy** | `GuardrailAdvisor` | A verdict is stochastic — model/prompt/threshold dependent (inherits N1). Blocking quality is bounded by classifier quality; false positives block legit users, false negatives let attacks through. |
| N9 | **No detection of indirect injection** | `GuardrailAdvisor` | Content injected via RAG/tools/websites does not enter through the user-message boundary this advisor watches (LLM01 #4/#6 mitigations are the caller's architecture). |
| N10 | **Historical data outside the request** | both | Data persisted *before* the advisors were added is only protected when it flows through this request's prompt. Backfill is the caller's job. |

> **Compliance reading:** G8 + G10 are the audit-citable redaction pair
> ("raw matched text is replaced before the model call; failures abort the
> call"). G14 + G15 are the gating pair ("detected violations are always
> blocked; judge failures are never a silent pass"). N8 is the residual risk to
> accept: the gate is only as good as the configured classifier.

---

## 6. Module & package layout

No new module, no new dependencies. Everything lives in the existing
**`data-privacy-spring-ai`** module (version stays `1.0.0`, §13), flat package
matching v1:

```
src/main/java/io/github/khezyapp/dpriv/springai/
├── DataPrivacyAdvisor.java          new — mitigate pattern (BaseAdvisor)
├── GuardrailAdvisor.java            new — prevent pattern (BaseAdvisor)
├── ProtectionScope.java             new — enum INPUT | OUTPUT | BOTH (shared)
├── RedactMode.java                  new — enum ALL | LAST_ONLY (DataPrivacyAdvisor)
├── RedactionReport.java             new — record: redacted? + entityTypes
├── GuardrailReport.java             new — record: passed? + entityType + confidence
└── exception/
    ├── DataPrivacyException.java        new — base RuntimeException
    ├── RedactionException.java          new — SANITIZE pipeline failed
    ├── PolicyViolationException.java    new — classifier flagged (input or output)
    └── GuardrailEvaluationException.java new — judge failed (infra, not a violation)
```

Build: `api data-privacy-core:1.0.0` + `spring-ai-client-chat` — both already
present. **No changes to `build.gradle`.**

---

## 7. Public API surface

```java
package io.github.khezyapp.dpriv.springai;

/**
 * ProtectionScope — where an advisor applies (shared by both advisors).
 */
public enum ProtectionScope {
    INPUT,    // user messages only (default for both advisors)
    OUTPUT,   // model response only
    BOTH      // both directions
}

/**
 * RedactMode — which user messages DataPrivacyAdvisor redacts in before().
 */
public enum RedactMode {
    ALL,        // every USER message in the prompt, incl. history (default)
    LAST_ONLY   // only the last USER message (perf opt-in)
}

/**
 * RedactionReport — observability payload from DataPrivacyAdvisor.
 */
public record RedactionReport(boolean redacted, Set<String> entityTypes) {
    public static final RedactionReport NONE = new RedactionReport(false, Set.of());
}

/**
 * GuardrailReport — observability payload from GuardrailAdvisor (pass path only;
 * a violation is carried by PolicyViolationException instead).
 */
public record GuardrailReport(boolean passed, String entityType, double confidence) {}
```

```java
package io.github.khezyapp.dpriv.springai;

/**
 * DataPrivacyAdvisor — MITIGATE pattern. Deterministic redaction of USER-message
 * text (and optionally model output). Never blocks; never modifies anything
 * but USER text. Fail-closed via RedactionException.
 */
public final class DataPrivacyAdvisor implements BaseAdvisor {

    public static final String CONTEXT_KEY = "io.github.khezyapp.dpriv.springai.redactionReport";

    public static Builder builder();

    @Override public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain);
    @Override public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain);
    @Override public Flux<ChatClientResponse> adviseStream(ChatClientRequest request,
                                                           StreamAdvisorChain chain);
    @Override public String getName();   // "DataPrivacyAdvisor"
    @Override public int getOrder();

    public static final class Builder {
        public Builder guardrails(Guardrails value);       // canonical — prebuilt instance
        public Builder config(GuardrailsConfig value);     // convenience — builds Guardrails internally
        public Builder scope(ProtectionScope value);       // default ProtectionScope.INPUT
        public Builder mode(RedactMode value);             // default RedactMode.ALL
        public Builder failOnError(boolean value);         // default true (fail-closed)
        public Builder order(int value);                   // default Ordered.HIGHEST_PRECEDENCE
        public DataPrivacyAdvisor build();
    }
}
```

```java
package io.github.khezyapp.dpriv.springai;

/**
 * GuardrailAdvisor — PREVENT pattern. LLM-as-judge gating of the last user
 * message (INPUT) and/or the model response (OUTPUT) via Operation.CLASSIFY.
 * Never modifies the request: it allows it or blocks it (PolicyViolationException).
 */
public final class GuardrailAdvisor implements BaseAdvisor {

    public static final String CONTEXT_KEY = "io.github.khezyapp.dpriv.springai.guardrailReport";

    public static Builder builder();

    @Override public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain);
    @Override public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain);
    @Override public Flux<ChatClientResponse> adviseStream(ChatClientRequest request,
                                                           StreamAdvisorChain chain);
    @Override public String getName();   // "GuardrailAdvisor"
    @Override public int getOrder();

    public static final class Builder {
        public Builder guardrails(Guardrails value);       // canonical — must include ≥1 LlmClassifier
        public Builder config(GuardrailsConfig value);     // convenience — GuardrailsConfig + built-in families
        public Builder scope(ProtectionScope value);       // default ProtectionScope.INPUT
        public Builder failOnError(boolean value);         // default true (judge error → GuardrailEvaluationException)
        public Builder order(int value);                   // default HIGHEST_PRECEDENCE + 1
        public GuardrailAdvisor build();
    }
}
```

```java
package io.github.khezyapp.dpriv.springai.exception;

/**
 * Base class for all advisor failures. Consumers may catch this type to handle
 * any data-privacy failure uniformly.
 */
public class DataPrivacyException extends RuntimeException {
    public DataPrivacyException(String message) { super(message); }
    public DataPrivacyException(String message, Throwable cause) { super(message, cause); }
}

/**
 * The SANITIZE pipeline failed (a check errored) and failOnError=true.
 * Reaction: the request was aborted — do NOT retry the same payload blindly.
 */
public final class RedactionException extends DataPrivacyException {
    public RedactionException(String message, Throwable cause) { ... }
}

/**
 * A classifier flagged the input or output (detected && confidence >= threshold).
 * Reaction: log as a security event, respond to the user, do not proceed.
 */
public final class PolicyViolationException extends DataPrivacyException {
    public PolicyViolationException(String entityType, double confidence,
                                    ProtectionScope scope) { ... }
    public String entityType();            // e.g. "jailbreak", "nsfw", custom
    public double confidence();            // 0..1
    public ProtectionScope scope();        // where it was caught: INPUT or OUTPUT
}

/**
 * The judge itself failed (LLM unreachable, malformed verdict) and failOnError=true.
 * Reaction: infrastructure problem — retry with backoff may be appropriate.
 */
public final class GuardrailEvaluationException extends DataPrivacyException {
    public GuardrailEvaluationException(String message, Throwable cause) { ... }
}
```

Design notes:

- **`guardrails(...)` vs `config(...)`** (both advisors): `guardrails(Guardrails)`
  is canonical (custom checks, classifiers, streaming config). `config(...)` is
  the simple-use-case convenience that builds the instance internally. Last one
  wins in the builder (repo convention).
- **No Lombok requirement.** Hand-written final classes + fluent builders, same
  style as `SpringAiLlmClassifier.Builder`.
- **Context keys are namespaced** to avoid collisions with other advisors.
- `RedactionReport`/`GuardrailReport` live in the same package as the advisors
  (v1 keeps the `springai` package flat).

---

## 8. Behavior specification

### 8.1 `DataPrivacyAdvisor.before(...)` — input redaction (scope INPUT | BOTH)

1. Select candidate messages using the framework's own helpers:
   - `mode == ALL` → `request.prompt().getUserMessages()` (already USER-filtered);
   - `mode == LAST_ONLY` → `request.prompt().getLastUserOrToolResponseMessage()`
     — if its type is `USER`, use it; otherwise fall back to the last entry of
     `getUserMessages()`;
   - skip candidates whose `getText()` is null (pure-media message).
2. For each candidate, run the pipeline once:
   ```java
   GuardrailsOutcome outcome = guardrails.run(userText, Operation.SANITIZE);
   String redacted  = outcome.text();
   Set<String> types = outcome.maskEntities().keySet();   // entity types found
   ```
   `SANITIZE` throws on check error → `failOnError=true` propagates as
   **`RedactionException`** (wrapping the cause); `false` passes the original
   text through.
3. Rebuild each changed message preserving everything else:
   ```java
   Message redactedMessage = ((UserMessage) msg).mutate().text(redacted).build();
   // media + metadata preserved
   ```
   (or `UserMessage.fromText`-style rebuild of the full message list keeping
   order — unchanged messages keep their original instances).
4. Rebuild the request:
   ```java
   ChatClientRequest processed = request.mutate()
           .prompt(request.prompt().mutate().messages(redactedList).build())
           .build();
   ```
5. Write `RedactionReport` to `processed.context().put(CONTEXT_KEY, report)` —
   `redacted=true` only when some text actually changed.
6. Return `processed`. The advisor never reads `context()` for control flow.

### 8.2 `DataPrivacyAdvisor.after(...)` — output redaction (scope OUTPUT | BOTH)

1. `response.chatResponse()` null (aborted/cancelled) → return unchanged.
2. `chatResponse.getResult().getOutput()` → `AssistantMessage`; text null
   (tool-call-only turn) → return unchanged.
3. Run `SANITIZE`, rebuild the assistant message (`mutate().text(redacted)`),
   rebuild the `ChatResponse` with the new `Generation` (metadata unchanged).
4. Merge the report into `response.context()`.
5. Return the mutated response.

### 8.3 `DataPrivacyAdvisor.adviseStream(...)`

Override using the framework's own aggregation pattern (`MessageChatMemoryAdvisor`
/ `SimpleLoggerAdvisor`): stream chunks carry **deltas**, so the finish-reason
response alone does not hold the full text:

```java
@Override
public Flux<ChatClientResponse> adviseStream(ChatClientRequest request,
                                             StreamAdvisorChain chain) {
    return Mono.just(request)
        .publishOn(getScheduler())
        .map(r -> this.before(r, chain))
        .flatMapMany(chain::nextStream)
        .transform(flux -> new ChatClientMessageAggregator()
            .aggregateChatClientResponse(flux, aggregated ->
                this.after(aggregated, chain)));
}
```

`getScheduler()` stays `BaseAdvisor.DEFAULT_SCHEDULER`. For scope `INPUT`,
`after` is a no-op and the aggregated path is harmless.

### 8.4 `DataPrivacyAdvisor` failure semantics

| `failOnError` | Check error in `before` | Check error in `after` |
| --- | --- | --- |
| `true` (default) | **`RedactionException`** → model call never happens. Unredacted text never sent. | **`RedactionException`** → caller gets the error instead of unredacted output. |
| `false` | Original text passes through to the model (weakened privacy — documented). | Original response passes through to the caller. |

### 8.5 `GuardrailAdvisor.before(...)` — input gating (scope INPUT | BOTH)

The prevent pattern is **binary: allow or block**. `before` never transforms.

1. Select the target: the last user message via
   `prompt.getLastUserOrToolResponseMessage()` (USER-type check; fall back to
   last of `getUserMessages()`). History is not re-classified (G16).
2. Run the gate:
   ```java
   GuardrailsOutcome outcome = guardrails.run(userText, Operation.CLASSIFY);
   ```
   v1's `CLASSIFY` pipeline internally runs the deterministic preflight first —
   the judge sees **masked** text (raw PII never reaches the judge model), then
   all registered `LlmClassifier` families with their thresholds (`LlmContract`).
3. Interpret:
   - `outcome.detected()` (flagged ∧ ≥ threshold) → **throw
     `PolicyViolationException(outcome.entityType(), confidence, INPUT)`**.
   - outcome reports an errored judge (fail-safe: errored ≠ pass) →
     `failOnError=true` → **`GuardrailEvaluationException`**; `false` → treat as
     pass (documented weakening).
   - otherwise → **return the request unchanged** (the advisor's only valid
     "pass" outcome).
4. On pass, write `GuardrailReport` into the request context (do not write on
   violation — the exception is the report).

> **Important (defense in depth):** `CLASSIFY`'s internal preflight protects
> only the *judge's* context. The main model still receives the **raw** user
> text when this advisor runs alone — gating does not sanitize. To keep PII
> away from the main model, register `DataPrivacyAdvisor` too (§11).

### 8.6 `GuardrailAdvisor.after(...)` — output gating (scope OUTPUT | BOTH)

1. `response.chatResponse()` null → return unchanged.
2. Output text null (tool-call-only turn) → return unchanged.
3. Run `run(outputText, Operation.CLASSIFY)`:
   - detected → **`PolicyViolationException(..., scope=OUTPUT)`** — the caller
     never receives the unsafe/leaked output (LLM05/LLM07);
   - errored judge → per `failOnError` (throw `GuardrailEvaluationException` /
     pass);
   - clean → return the response unchanged.
4. Note: the model call already happened at this point (cost incurred); the
   gate's value is stopping the *output* from reaching the caller, tools, or UI.

### 8.7 `GuardrailAdvisor.adviseStream(...)`

Same override as §8.3 — full-text aggregation is mandatory for correct output
gating (a delta fragment cannot be classified meaningfully).

### 8.8 `GuardrailAdvisor` failure semantics

| Situation | Default | `failOnError=false` |
| --- | --- | --- |
| Verdict flagged (≥ threshold) | **`PolicyViolationException`** — always, no opt-out (G14) | same — violations are never silently passed |
| Judge errored (LLM down, malformed verdict) | **`GuardrailEvaluationException`** (fail-closed) | treated as pass (documented weakening) |

### 8.9 Exception reference (consumer reactions)

| Exception | Means | Consumer reaction |
| --- | --- | --- |
| `PolicyViolationException` | A security event: input or output blocked. Carries `entityType`, `confidence`, `scope`. | Log as security event; return a polite refusal to the user; **do not** retry the same input. |
| `RedactionException` | SANITIZE pipeline failed; request aborted before the model call. | Surface an error; retry may be OK once the underlying check issue is fixed. |
| `GuardrailEvaluationException` | The judge failed (infra); request aborted. | Backend/health alert; retry with backoff may be appropriate. |
| `DataPrivacyException` (base) | Any of the above, uniformly. | Generic handler / 5xx mapping. |

---

## 9. Observability — reports in the request context

| Advisor | Key | Payload | Written when |
| --- | --- | --- | --- |
| `DataPrivacyAdvisor` | `...redactionReport` | `RedactionReport(redacted, entityTypes)` | Always (pass path); `NONE` when nothing changed |
| `GuardrailAdvisor` | `...guardrailReport` | `GuardrailReport(passed, entityType, confidence)` | Only on pass — a violation *is* the `PolicyViolationException` |

```java
RedactionReport report = (RedactionReport)
        response.context().get(DataPrivacyAdvisor.CONTEXT_KEY);
```

Audit records stay in the core `GuardrailsOutcome` when enabled — advisors
never persist or log anything (G13).

---

## 10. Config reference — "when to apply the control"

| Question | Config | Options | Default | Why |
| --- | --- | --- | --- | --- |
| Where does the control apply? | `scope(...)` (both) | `INPUT` / `OUTPUT` / `BOTH` | `INPUT` | Input protection is the product; output is opt-in (leak/unsafe-output mitigation). |
| Which user messages (redaction)? | `mode(...)` | `ALL` / `LAST_ONLY` | `ALL` | `ALL` is idempotent (G12) and covers pre-existing raw history; `LAST_ONLY` is the perf trade-off. |
| What happens on pipeline error (redaction)? | `failOnError(...)` | `true` / `false` | `true` | Fail-closed inherits core G4; `false` is the explicit weakening. |
| What happens on judge error (gating)? | `failOnError(...)` | `true` / `false` | `true` | Errored ≠ pass (core G4); `false` weakens to fail-open. |
| Can a detected violation be bypassed? | — (no config) | — | never | G14: `PolicyViolationException` always. |
| When does redaction actually trigger? | — (no config) | — | always scan | The pipeline is conditional: no matches ⇒ text unchanged. No "only if detected" flag needed. |
| Chain position? | `order(...)` | int | DPA `HIGHEST_PRECEDENCE`, GRA `+1` | Composition order (§11). |

No Spring Boot properties — plain library module, consistent with `khezy-kit`.

---

## 11. Ordering & composition

### 11.1 Chain mechanics (why the defaults compose correctly)

Spring AI advisor chains execute **`before` hooks in order** and **`after` hooks
in reverse order** (stack semantics: the first advisor wraps the rest). With the
default orders:

```
order HIGHEST_PRECEDENCE      DataPrivacyAdvisor   ─┐
order HIGHEST_PRECEDENCE + 1  GuardrailAdvisor     ─┤
                                                     │
BEFORE (in order):   DPA.redact ─► GRA.classify ─► model
AFTER  (reverse):    model ─► GRA.classify ─► DPA.redact ─► caller
```

Result — **input = redact then classify; output = classify then redact**:

- The judge classifies **redacted** input (consistent with v1's "classify on
  masked text" pipeline; the judge never sees raw PII either).
- The output is vetted **raw** first (best detection precision — redaction
  could destroy attack evidence), and whatever passes is then scrubbed.
- If the output is flagged, `PolicyViolationException` propagates before
  `DPA.after` — no redacted half-blocked output reaches the caller.

### 11.2 Interplay table

| Advisor | Order relative to the pair | Effect |
| --- | --- | --- |
| Chat memory (`MessageChatMemoryAdvisor`, `HIGHEST + 1000`) | after | Redacted text is what gets stored. Raw PII never persists. |
| Logger advisors (`SimpleLoggerAdvisor`) | after | Logs capture redacted requests. |
| User custom advisors | caller's choice | Anything running before DPA may see raw text; anything between DPA and GRA sees redacted-but-unvetted text. |
| Tool-call advisors | unaffected | Tool definitions/messages untouched (G11); gating covers USER text + model output only. |

**README guard rule:** *"Keep the privacy advisor first (lowest order) so raw
text is not visible to anything downstream. When composing the pair, use the
defaults: DataPrivacyAdvisor at `HIGHEST_PRECEDENCE`, GuardrailAdvisor at
`HIGHEST_PRECEDENCE + 1`."*

---

## 12. Testing strategy

Repo conventions: **no Mockito** — hand-written stubs; JUnit 5 + AssertJ;
Khmer/Cambodia test data (e.g. `visal@example.com`, `SOK`, `Phnom Penh`);
Checkstyle green; `./gradlew :data-privacy-spring-ai:build`.

| Test class | Covers |
| --- | --- |
| `DataPrivacyAdvisorTest` | `before`: user text redacted (`<EMAIL_ADDRESS>`), system message untouched, media/metadata preserved; `RedactMode.ALL` vs `LAST_ONLY` (incl. `getLastUserOrToolResponseMessage` fallback); report in context; `redacted=false` when clean; `failOnError=true` → `RedactionException` from a throwing custom `GuardrailCheck`; `false` → pass-through; `after`: response redacted, tool-call-only untouched; `BOTH`; `config(...)` ≡ `guardrails(...)`. |
| `GuardrailAdvisorTest` | `before` with stubbed classifier (via `Guardrails` + anonymous `LlmClassifier` per repo style): flagged → `PolicyViolationException` with correct `entityType`/`confidence`/`scope`; clean → request unchanged + `GuardrailReport` in context; judge error → `GuardrailEvaluationException` (default) vs pass (`failOnError=false`); target = last user message only; `after` (OUTPUT/BOTH): flagged output → exception; tool-call-only → untouched. |
| `DataPrivacyAdvisorStreamTest` / `GuardrailAdvisorStreamTest` | Chunked `ChatModel` stubs: final aggregated output redacted / vetted; INPUT scope streams unchanged content; error mid-stream propagates. |
| `EndToEndDataPrivacyAdvisorTest` / `EndToEndGuardrailAdvisorTest` | Full `ChatClient.builder().model(stub).defaultAdvisors(...)`; combined test proves the composition order: model receives redacted text, judge sees redacted text, output vetted then scrubbed (assert via captured prompts + context reports). Mirrors `EndToEndSpringAiTest`. |

---

## 13. Build & release plan

| Item | Value |
| --- | --- |
| Module | `securities/data-privacy-spring-ai` |
| Version | **unchanged `1.0.0`** (still in development; no bump — decision #14) |
| New sources | 11 files (§6) |
| Dependencies | none added |
| Verification | `./gradlew :data-privacy-spring-ai:build` green; `graphify update .` |
| Release | at the existing manual release point, with the module's current version |

---

## 14. Suggested implementation order (task sketch)

1. **T1 — shared types**: `ProtectionScope`, `RedactMode`, `RedactionReport`,
   `GuardrailReport`, `exception/` package (4 exceptions).
2. **T2 — `DataPrivacyAdvisor`**: builder + `before` (ALL/LAST_ONLY, media-safe
   rebuild, report, `RedactionException`) + unit tests.
3. **T3 — `DataPrivacyAdvisor` output + streaming**: `after`,
   `adviseStream` (aggregator), stream tests.
4. **T4 — `GuardrailAdvisor`**: builder + `before`/`after` gating,
   `PolicyViolationException`/`GuardrailEvaluationException`, `failOnError`,
   unit tests.
5. **T5 — streaming + composition acceptance**: GRA stream path; end-to-end
   combined test (redact → classify → model → classify → redact); README update
   (two quick-starts, ordering rule, scope/mode matrix, exception reference).

Each task: build green + handoff log entry, per the v1 action plan convention.

---

## 15. Non-goals & future work (explicitly not in v2)

| Item | Status | Notes |
| --- | --- | --- |
| **Tool-call / agency gating** (LLM06) | future (v3 candidate) | Requires intercepting tool invocations, not text; would be a third advisor pattern ("restrict"). |
| **Indirect prompt injection defense** (RAG/website content) | future | Content does not flow through the advisor chain; needs RAG-side handling (OWASP LLM01 #6: segregate + identify external content). |
| **Un-redaction / reversible masking** | never by default | Re-identification risk; placeholders are the v1 contract. |
| **`ViolationAction.REPLACE`** (swap unsafe output with a safe message) | future | v2 throws only (G14); replacement is easy to add later without breaking the contract. |
| **Redacting TOOL-response messages** | future option | Tool outputs may echo sensitive data; v2 scopes to USER text (G11). A `predicate` extension point is a clean future add. |
| **Spring Boot starter / auto-config** | future | khezy-kit has no starters today; builders are the integration point. |
| **Synthetic data replacement** | future | Different product (data generation); v1 placeholders stay. |
| **Per-call opt-out** (context-key override) | future | `request.context()` is the natural channel; v2 keeps context read-only for control flow. |

---

## 16. Quick-start (design preview for README)

```java
// Use case 1 — MITIGATE: deterministic redaction, defaults (INPUT, ALL, fail-closed)
DataPrivacyAdvisor privacy = DataPrivacyAdvisor.builder()
        .config(GuardrailsConfig.DEFAULTS)
        .build();

// Use case 2 — PREVENT: LLM-as-judge gating (jailbreak + nsfw families, threshold 0.7)
Guardrails guardrails = Guardrails.builder()
        .withClassifier(SpringAiLlmClassifierFactory.jailbreak(chatClient, 0.7))
        .withClassifier(SpringAiLlmClassifierFactory.nsfw(chatClient, 0.7))
        .build();
GuardrailAdvisor gate = GuardrailAdvisor.builder().guardrails(guardrails).build();

// Use case 3 — defense in depth: both; defaults compose (redact → gate → model → gate → redact)
ChatClient client = ChatClient.builder()
        .model(openAiChatModel)
        .defaultAdvisors(privacy, gate)     // order from defaults
        .build();

try {
    String answer = client.prompt().user("my email is visal@example.com").call().content();
    // model received: "my email is <EMAIL_ADDRESS>"   (and nothing was flagged)
} catch (PolicyViolationException e) {
    // security event: e.entityType(), e.confidence(), e.scope()
}
```

_Last updated: 2026-08-29 (v2 design, not yet implemented)._
