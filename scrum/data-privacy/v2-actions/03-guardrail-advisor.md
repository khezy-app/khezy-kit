# Task 03 — `GuardrailAdvisor` (prevent pattern): builder, before/after, streaming

## Objective

Deliver the **`GuardrailAdvisor`** (design §7, §8.5–8.8): a `BaseAdvisor` that **gates** the last
user message (scope `INPUT`/`BOTH`) and/or the model response (scope `OUTPUT`/`BOTH`) via
`Guardrails.run(text, Operation.CLASSIFY)`. It never modifies the request — it allows it or blocks
it (`PolicyViolationException`); judge failures abort via `GuardrailEvaluationException` unless
`failOnError=false`. Fluent builder incl. the `classifier(...)` convenience, context report, and
the `adviseStream` override. Unit + stream tests.

## Hand-off context

- **Design doc:** §7 (API surface), §8.5–8.8 (behavior), §9 (report), §11 (ordering).
- **From Task 01 (in-repo):** `ProtectionScope`, `GuardrailReport`,
  `exception/DataPrivacyException`, `exception/PolicyViolationException`,
  `exception/GuardrailEvaluationException` — signatures verbatim in Task 01's handoff log entry.
  **No dependency on Task 02 code** (R5).
- **From core (in-repo, pinned):** `Guardrails` facade + `Guardrails.Builder`
  (`config(GuardrailsConfig)`, `failOnlyOnErrors(boolean)` **default true**,
  `withClassifier(LlmClassifier)`, `build()`); `GuardrailsOutcome(text, entityType, detected,
  validations, maskEntities, auditRecords, messages)` — **`messages` carries ONLY errors**;
  `Operation.CLASSIFY`; `GuardrailsConfig` + `DEFAULTS`; `LlmClassifier`
  (`classify(String) → Verdict(boolean flagged, double confidence)`, `beanName()`). Threshold rule
  lives in core (`LlmContract`: triggered = flagged ∧ confidence ≥ threshold; `beanName`
  `"jailbreak"` → `config.jailbreak()` etc., unknown → `LlmCheckConfig.DEFAULTS`).
- **From Spring AI 2.0.1 (framework API, pinned):** as Task 02's list (same mutation/rebuild
  surface; this advisor uses `ChatClientRequest.context()`, `Prompt.getLastUserOrToolResponseMessage()`,
  `Prompt.getUserMessages()`, `MessageType.USER`, `ChatClientMessageAggregator`,
  `BaseAdvisor.DEFAULT_SCHEDULER`).
- **Resolved decisions (INDEX, apply verbatim):** **R1** (no confidence — report/exception carry
  `entityType` only), **R2** (`classifier(...)` convenience + build assembly), **R3** (judge-error
  interpretation table — the core of this task), **R5** (duplicate stream template), **R6**
  (`.collectList().block()` in stream tests), **R7** (target selection fallback), **R9** (chain
  param may be `null` in unit tests).

## Design notes

- **The gate never transforms** (design §2, G11): `before` returns the request unchanged on pass.
- **R3 interpretation table — implement exactly** (verified against core semantics):
  ```
  outcome = guardrails.run(targetText, Operation.CLASSIFY)
  detected == false                              → PASS (write GuardrailReport)
  detected == true  && messages.isEmpty()        → VIOLATION → throw PolicyViolationException
  detected == true  && !messages.isEmpty()       → JUDGE/CHECK ERROR:
                                                     failOnError == true  → throw GuardrailEvaluationException
                                                     failOnError == false → PASS (documented weakening)
  ```
  (With `failOnlyOnErrors=true` core folds errors into `detected=true` + `messages`; a real
  violation leaves `messages` empty. With `failOnlyOnErrors=false` errors yield `detected=false`,
  so the first row already passes them — the convenience path forwards `failOnError` into
  `failOnlyOnErrors` to keep both layouts consistent.)
- **R2 build assembly:** if `guardrails(...)` was set → it wins, ignore `config`/`classifier`.
  Else assemble: `Guardrails.builder().config(cfg != null ? cfg : GuardrailsConfig.DEFAULTS)
  .failOnlyOnErrors(failOnError)` + one `.withClassifier(c)` per classifier (requireNonNull each).
  Fail fast with `IllegalStateException` if `guardrails == null && classifiers.isEmpty()`.
- Defaults: `scope=ProtectionScope.INPUT`, `failOnError=true`,
  `order=Ordered.HIGHEST_PRECEDENCE + 1` (design §11 — after `DataPrivacyAdvisor`).
- Scope→behavior: `INPUT` → gate in `before`, unchanged in `after`; `OUTPUT` → gate in `after`,
  unchanged in `before`; `BOTH` → both.
- Report on pass: `context().put(CONTEXT_KEY, new GuardrailReport(true, outcome.entityType()))` —
  `entityType` is the primary detected family or `null` when clean. On violation/error: **no
  report** — the exception is the report (design §9).
- `after(...)`: the model call already happened (cost incurred); the gate stops the output from
  reaching the caller (design §8.6). `PolicyViolationException` gets `scope=OUTPUT` on this path.
- `before(...)`: `entityType` from the outcome; `scope=INPUT` on this path.

## Files to create

Under `securities/data-privacy-spring-ai/src/main/java/io/github/khezyapp/dpriv/springai/`:

### 1. `GuardrailAdvisor.java`

```java
package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.api.Guardrails;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.LlmClassifier;
import io.github.khezyapp.dpriv.springai.exception.GuardrailEvaluationException;
import io.github.khezyapp.dpriv.springai.exception.PolicyViolationException;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * PREVENT pattern (design §2, §7): LLM-as-judge gating of the last user message (INPUT) and/or
 * the model response (OUTPUT) via Operation.CLASSIFY. Never modifies the request: it allows it
 * or blocks it (PolicyViolationException). Judge failures fail-closed via
 * GuardrailEvaluationException unless failOnError=false (G14, G15).
 */
public final class GuardrailAdvisor implements BaseAdvisor {

    public static final String CONTEXT_KEY = "io.github.khezyapp.dpriv.springai.guardrailReport";

    private final Guardrails guardrails;
    private final ProtectionScope scope;
    private final boolean failOnError;
    private final int order;

    private GuardrailAdvisor(Builder builder) { /* requireNonNull + assign */ }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain);
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain);
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain);
    @Override
    public String getName();   // "GuardrailAdvisor"
    @Override
    public int getOrder();

    public static final class Builder {
        public Builder guardrails(Guardrails value);        // canonical; wins if set
        public Builder config(GuardrailsConfig value);      // convenience (R2)
        public Builder classifier(LlmClassifier... value);  // convenience, varargs (R2)
        public Builder scope(ProtectionScope value);        // default ProtectionScope.INPUT
        public Builder failOnError(boolean value);          // default true (fail-closed)
        public Builder order(int value);                    // default Ordered.HIGHEST_PRECEDENCE + 1
        public GuardrailAdvisor build();
    }
}
```

### Behavior spec (design §8.5–8.8, pinned)

**`before(...)`** (scope includes `INPUT`; else return `request` unchanged):
1. Target: `prompt.getLastUserOrToolResponseMessage()` — use only when `getMessageType() == USER`
   and `getText() != null`; else the last entry of `getUserMessages()` with non-null text
   (R7). No user text at all → return `request` unchanged (no report).
2. `outcome = guardrails.run(targetText, Operation.CLASSIFY)` — exceptions thrown by the facade
   itself (none expected on CLASSIFY; if one occurs) propagate as-is.
3. Apply the R3 table. On PASS → `request.context().put(CONTEXT_KEY, new GuardrailReport(true,
   outcome.entityType()))` and return `request` (same instance).

**`after(...)`** (scope includes `OUTPUT`; else return `response` unchanged):
1. `response.chatResponse() == null` or output text `null` → unchanged.
2. `outcome = guardrails.run(outputText, Operation.CLASSIFY)`; apply the R3 table with
   `PolicyViolationException(entityType, ProtectionScope.OUTPUT)`.
3. On PASS → merge report into `response.context()` and return `response` unchanged.

**`adviseStream(...)`** (R5 — duplicate the Task 02 template exactly; `getScheduler()` default):
```java
return Mono.just(request)
        .publishOn(getScheduler())
        .map(r -> this.before(r, chain))
        .flatMapMany(chain::nextStream)
        .transform(flux -> new ChatClientMessageAggregator()
                .aggregateChatClientResponse(flux, aggregated -> this.after(aggregated, chain)));
```

## Tests

Files: `src/test/java/io/github/khezyapp/dpriv/springai/GuardrailAdvisorTest.java` and
`GuardrailAdvisorStreamTest.java`. No Mockito — **anonymous `LlmClassifier` stubs** (repo style;
see `EndToEndSpringAiTest` precedent):

```java
final var flagged = new LlmClassifier() {
    @Override public Verdict classify(String input) { return new Verdict(true, 0.95d); }
    @Override public String beanName() { return "jailbreak"; }
};
final var clean = new LlmClassifier() { /* Verdict(false, 0.1d), beanName "jailbreak" */ };
final var broken = new LlmClassifier() { /* classify throws new IllegalStateException("judge down") */ };
```
Guardrails: `Guardrails.builder().withClassifier(<stub>).build()` (default `failOnlyOnErrors=true`).
Chain argument `null` (R9).

- `flaggedInputThrowsPolicyViolationWithScope` — `before` with `flagged` → `PolicyViolationException`,
  `entityType()=="jailbreak"`, `scope()==ProtectionScope.INPUT`, message contains `"jailbreak"`.
- `cleanInputPassesRequestUnchanged` — `before` with `clean` → **same request instance** returned;
  context `CONTEXT_KEY` → `GuardrailReport(passed=true, entityType=null)`.
- `violationIsNeverBypassedWithFailOnErrorFalse` — `flagged` + `failOnError=false` → still
  `PolicyViolationException` (G14).
- `judgeErrorThrowsGuardrailEvaluationException` — `broken` classifier → `before` →
  `GuardrailEvaluationException` (R3: `detected=true` + non-empty `messages`; default
  `failOnError=true`).
- `judgeErrorPassesWhenFailOnErrorFalse` — `broken` + `failOnError=false` → `before` returns
  request unchanged (R3 table; convenience path sets `failOnlyOnErrors(false)`).
- `gatesOnlyLastUserMessage` — prompt with two user messages + `clean` stub recording classified
  text → classified input equals the **last** user text only.
- `skipsGatingWhenLastMessageIsToolResponseWithNoUserText` — only tool messages → request
  unchanged, no report.
- `outputScopeGatesOnlyResponse` — scope `OUTPUT`: `before` unchanged; `after` with flagged
  output → `PolicyViolationException` `scope()==OUTPUT`.
- `afterIgnoresNullChatResponseAndToolCallOnly` — null `chatResponse` / null output text →
  unchanged.
- `passWritesReportToResponseContext` — scope `OUTPUT`, clean classifier, real response → response
  context `CONTEXT_KEY` → `GuardrailReport(true, null)`.
- `inputScopeLeavesAfterUntouched` — scope `INPUT` → `after` returns response unchanged.
- `configPlusClassifiersConvenienceBuildsGate` — `builder().config(GuardrailsConfig.DEFAULTS)
  .classifier(flagged).build()` → `before` throws `PolicyViolationException` (R2).
- `buildFailsFastWithoutGuardrailsOrClassifier` — `builder().build()` → `IllegalStateException`.
- `explicitGuardrailsWinsOverConvenience` — `guardrails(clean-built)` + `classifier(flagged)` →
  clean behavior (explicit wins).
- `defaultsAreInputFailClosedSecondPrecedence` — `getOrder() == Ordered.HIGHEST_PRECEDENCE + 1`,
  `getName().equals("GuardrailAdvisor")`.

**Stream tests** (`GuardrailAdvisorStreamTest`, R6 — `.collectList().block()`):
- `streamGatesOutputOnFinishReason` — scope `BOTH`, chunked stub `ChatModel`, flagged classifier →
  `stream().collectList().block()` → single `PolicyViolationException` surfaces (assert
  `assertThatThrownBy` around the blocking collect).
- `streamPassesCleanOutputThrough` — scope `BOTH`, clean classifier → aggregated output equals the
  stub's full text; `GuardrailReport` in the final response context.

## Acceptance criteria

- `./gradlew :data-privacy-spring-ai:build` → BUILD SUCCESSFUL (compile + tests + Checkstyle).
- No `build.gradle` edits; version stays `1.0.0`.
- Public surface: `GuardrailAdvisor` + nested `Builder` only.

## Hand-off to next task (log in 00-HANDOFF.md)

- As-built `GuardrailAdvisor`/`Builder` signatures; the exact R3 interpretation code (how
  `messages().isEmpty()` distinguishes violation from judge error — confirm against a real
  `GuardrailsOutcome`).
- Confirmed behaviors Task 04's composition test relies on: on PASS the request/response instance
  is untouched (report via context only); violations throw before any `after`-of-DPA runs.
- Any Spring AI API surprises in `getLastUserOrToolResponseMessage()` typing.
