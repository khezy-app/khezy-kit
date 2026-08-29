# Task 02 — `DataPrivacyAdvisor` (mitigate pattern): builder, before/after, streaming

## Objective

Deliver the **`DataPrivacyAdvisor`** (design §7, §8.1–8.4): a `BaseAdvisor` that redacts
USER-message text via `Guardrails.run(text, Operation.SANITIZE)` before the model call (scope
`INPUT`/`BOTH`) and optionally redacts the model response (scope `OUTPUT`/`BOTH`), with the fluent
builder, `RedactionException` fail-closed semantics, context report, and the `adviseStream`
override. Unit + stream tests.

## Hand-off context

- **Design doc:** §7 (API surface), §8.1–8.4 (behavior), §9 (report), §11 (ordering).
- **From Task 01 (in-repo):** `ProtectionScope`, `RedactMode`, `RedactionReport`,
  `exception/DataPrivacyException`, `exception/RedactionException` — signatures verbatim in
  Task 01's handoff log entry; do not re-read Task 01.
- **From core (in-repo, compile against, do NOT re-derive):** `Guardrails`
  (`builder().config(GuardrailsConfig).failOnlyOnErrors(boolean).withClassifier(LlmClassifier).build()`,
  `run(String, Operation)`, `redact(String)`), `GuardrailsOutcome(text, entityType, detected,
  validations, maskEntities, auditRecords, messages)`, `Operation.SANITIZE`, `GuardrailsConfig`
  (+`DEFAULTS`), `LlmClassifier` SPI.
- **From Spring AI 2.0.1 (framework API, pinned):** `BaseAdvisor` (`before(ChatClientRequest,
  AdvisorChain)`, `after(ChatClientResponse, AdvisorChain)`, `getScheduler()`), `ChatClientRequest(
  prompt, context)` with `mutate().prompt(...).build()`, `ChatClientResponse(chatResponse,
  context)` with `mutate().chatResponse(...).build()`, `Prompt.getInstructions()`,
  `Prompt.getUserMessages()`, `Prompt.getLastUserOrToolResponseMessage()`, `Prompt.mutate()
  .messages(List<Message>).build()`, `UserMessage.mutate().text(String).build()` (preserves
  media + metadata), `AssistantMessage.mutate().text(String)`, `Generation(AssistantMessage)`,
  `ChatResponse.builder().generations(List<Generation>).metadata(ChatResponseMetadata).build()`,
  `ChatClientMessageAggregator().aggregateChatClientResponse(Flux<ChatClientResponse>,
  Consumer<ChatClientResponse>)`, `BaseAdvisor.DEFAULT_SCHEDULER`, `MessageType.USER`.
- **Resolved decisions (INDEX, apply verbatim):** **R4** (RedactionException trigger config),
  **R5** (duplicate the stream template — no shared helper), **R6** (`.collectList().block()` in
  stream tests), **R7** (LAST_ONLY fallback), **R8** (response rebuild), **R9** (chain param may
  be `null` in unit tests).

## Design notes

- `build()` **fails fast** with `IllegalStateException` if neither `guardrails(...)` nor
  `config(...)` was supplied (message: "guardrails(...) or config(...) is required").
- `config(...)` convenience: `this.guardrails = Guardrails.builder().config(cfg).build()`.
  Last call wins if both setters used (repo builder convention).
- `before(...)`/`after(...)` never call the chain parameter (R9).
- Defaults: `scope=ProtectionScope.INPUT`, `mode=RedactMode.ALL`, `failOnError=true`,
  `order=Ordered.HIGHEST_PRECEDENCE` (`org.springframework.core.Ordered`).
- **Scope→behavior mapping:** `INPUT` → redact in `before`, return unchanged in `after`;
  `OUTPUT` → unchanged in `before`, redact in `after`; `BOTH` → both.
- The report is **always** written on the redacting path: `RedactionReport(redacted,
  entityTypes)` with `redacted=true` only when the text actually changed; use `RedactionReport.NONE`
  when no message was redacted.

## Files to create

Under `securities/data-privacy-spring-ai/src/main/java/io/github/khezyapp/dpriv/springai/`:

### 1. `DataPrivacyAdvisor.java`

```java
package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.api.Guardrails;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.springai.exception.RedactionException;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MITIGATE pattern (design §2, §7): deterministic redaction of USER-message text before the
 * model call (INPUT/BOTH) and of the model response after (OUTPUT/BOTH). Never blocks; never
 * modifies anything but USER text. Fail-closed via RedactionException.
 */
public final class DataPrivacyAdvisor implements BaseAdvisor {

    public static final String CONTEXT_KEY = "io.github.khezyapp.dpriv.springai.redactionReport";

    private final Guardrails guardrails;
    private final ProtectionScope scope;
    private final RedactMode mode;
    private final boolean failOnError;
    private final int order;

    private DataPrivacyAdvisor(Builder builder) { /* requireNonNull + assign; guardrails required */ }

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
    public String getName();   // "DataPrivacyAdvisor"
    @Override
    public int getOrder();

    public static final class Builder {
        public Builder guardrails(Guardrails value);   // canonical
        public Builder config(GuardrailsConfig value); // convenience → builds Guardrails
        public Builder scope(ProtectionScope value);   // default ProtectionScope.INPUT
        public Builder mode(RedactMode value);         // default RedactMode.ALL
        public Builder failOnError(boolean value);     // default true (fail-closed)
        public Builder order(int value);               // default Ordered.HIGHEST_PRECEDENCE
        public DataPrivacyAdvisor build();
    }
}
```

### Behavior spec (design §8.1–8.4, pinned)

**`before(...)`** (only when scope includes `INPUT`; else return `request` unchanged):
1. Select candidates: `mode == ALL` → `prompt.getUserMessages()`; `mode == LAST_ONLY` →
   `getLastUserOrToolResponseMessage()`, used only when `getMessageType() == USER`, else fall back
   to the last entry of `getUserMessages()` (R7). Skip any candidate with `getText() == null`.
2. Per candidate: `outcome = guardrails.run(text, Operation.SANITIZE)`. **On
   `IllegalStateException`** (core's fail-closed) → `failOnError ? throw new RedactionException(
   "redaction failed: " + e.getMessage(), e) : keep original text`.
3. Rebuild changed messages via `((UserMessage) msg).mutate().text(redacted).build()` (media +
   metadata preserved). Rebuild the full instructions list from `prompt.getInstructions()`,
   replacing only the changed USER messages in place, keeping every other message instance.
4. Rebuild request: `request.mutate().prompt(request.prompt().mutate().messages(redactedList)
   .build()).build()`.
5. `processed.context().put(CONTEXT_KEY, report)` — `RedactionReport(true, outcome.maskEntities()
   .keySet())` when any text changed, else `RedactionReport.NONE` (still put).

**`after(...)`** (only when scope includes `OUTPUT`; else return `response` unchanged):
1. `response.chatResponse() == null` → unchanged. Output `AssistantMessage` text `null`
   (tool-call-only turn) → unchanged.
2. `outcome = guardrails.run(text, Operation.SANITIZE)`; on `IllegalStateException` →
   `failOnError ? throw RedactionException : return response unchanged`.
3. Rebuild: `ChatResponse.builder().generations(List.of(new Generation(
   (AssistantMessage) msg.mutate().text(outcome.text()).build()))).metadata(original.metadata())
   .build()` (R8); rebuild `response` via `response.mutate().chatResponse(redacted).build()`.
4. Merge report into `response.context().put(CONTEXT_KEY, report)`.

**`adviseStream(...)`** (R5 — duplicate this exact template):
```java
return Mono.just(request)
        .publishOn(getScheduler())
        .map(r -> this.before(r, chain))
        .flatMapMany(chain::nextStream)
        .transform(flux -> new ChatClientMessageAggregator()
                .aggregateChatClientResponse(flux, aggregated -> this.after(aggregated, chain)));
```
`getScheduler()` = `BaseAdvisor.DEFAULT_SCHEDULER` (no override).

## Tests

Files: `src/test/java/io/github/khezyapp/dpriv/springai/DataPrivacyAdvisorTest.java` and
`DataPrivacyAdvisorStreamTest.java`. No Mockito — hand-written stubs; build prompts via
`Prompt.builder()`/`Prompt(List<Message>)` and requests via `ChatClientRequest.builder()
.prompt(...).context(Map.of()).build()`; the chain argument is `null` (R9).

Guardrails for most tests: `Guardrails.builder().config(GuardrailsConfig.DEFAULTS).build()`
(PII `ALL` coverage detects `visal@example.com` as `EMAIL_ADDRESS`).

- `beforeRedactsUserMessageInPlace` — prompt `[user: "my email is visal@example.com"]` → returned
  request's last user text contains `<EMAIL_ADDRESS>` and not `visal@example.com`; other messages
  untouched.
- `beforeLeavesSystemMessageUntouched` — `[system: "...", user: "email visal@example.com"]` →
  system text identical instance.
- `beforePreservesMediaAndMetadata` — `UserMessage` with `media` + `metadata` and redactable text →
  redacted text, media list equal, metadata map equal.
- `allModeRedactsEveryUserMessage` — two user messages → both redacted.
- `lastOnlyModeRedactsOnlyLastUserMessage` — two user messages → only the last redacted.
- `lastOnlyFallsBackToLastUserWhenLastIsToolResponse` — last message is a `ToolResponseMessage` →
  last USER message redacted (R7).
- `beforeSkipsPureMediaUserMessage` — `UserMessage` with `null` text → unchanged, no report
  mismatch.
- `beforeWritesReportWithEntityTypes` — response request context `CONTEXT_KEY` →
  `RedactionReport` with `redacted()==true`, `entityTypes()` contains `EMAIL_ADDRESS`.
- `beforeWritesNoneReportOnCleanText` — clean text → `RedactionReport.NONE` in context.
- `beforeFailClosedThrowsRedactionException` — Guardrails built with **R4 config**:
  `Guardrails.builder().config(GuardrailsConfig.builder().pii(new PiiConfig(PiiCoverage.SELECTED,
  null, List.of(), true)).build()).build()`; `before(...)` → `RedactionException` with
  `IllegalStateException` cause; **nothing written to context**.
- `beforeFailOpenPassesOriginalText` — same R4 config but `failOnError=false` → request passes
  through with original text.
- `afterRedactsResponseText` — scope `OUTPUT`; response with assistant text containing
  `visal@example.com` → redacted text contains `<EMAIL_ADDRESS>`; metadata preserved.
- `afterIgnoresToolCallOnlyResponse` — assistant message with `null` text → response unchanged.
- `afterFailClosedThrowsRedactionException` — R4 Guardrails + scope `OUTPUT` → `RedactionException`.
- `outputScopeLeavesBeforeUntouched` — scope `OUTPUT` → `before` returns same request (identity).
- `bothScopeRedactsInputAndOutput` — scope `BOTH` → both directions redacted.
- `configConvenienceEqualsGuardrailsInstance` — advisor built with `config(DEFAULTS)` behaves
  identically to advisor built with the equivalent `guardrails(...)` instance (same redaction).
- `defaultsAreInputAllFailClosedHighestPrecedence` — `builder().config(DEFAULTS).build()` →
  `getOrder() == Ordered.HIGHEST_PRECEDENCE`, `getName().equals("DataPrivacyAdvisor")`,
  INPUT scope, ALL mode, fail-closed.
- `buildFailsFastWithoutGuardrailsOrConfig` — `builder().build()` → `IllegalStateException`.

**Stream tests** (`DataPrivacyAdvisorStreamTest`, R6 — `.collectList().block()` + AssertJ):
- `streamAggregatesAndRedactsFullOutput` — scope `BOTH`; chunked stub `ChatModel` emitting 3
  deltas ("my email is ", "visal@", "example.com") via a real `ChatClient.builder().model(stub)
  .defaultAdvisors(advisor).build()`; `client.prompt().user("...").stream().collectList().block()`
  → the single aggregated response's text contains `<EMAIL_ADDRESS>`, not the raw email.
- `streamInputScopeLeavesOutputUnchanged` — scope `INPUT` → streamed output equals the stub's
  raw text.

## Acceptance criteria

- `./gradlew :data-privacy-spring-ai:build` → BUILD SUCCESSFUL (compile + tests + Checkstyle).
- No `build.gradle` edits; version stays `1.0.0`.
- Public surface: `DataPrivacyAdvisor` + nested `Builder` only — nothing else public.

## Hand-off to next task (log in 00-HANDOFF.md)

- As-built `DataPrivacyAdvisor`/`Builder` signatures; the exact exception-wrap code used
  (message formats); the report-write points.
- Confirmed behaviors Task 04's composition test relies on: `before` writes the report BEFORE
  later advisors run; `after` merges the report into response context.
- Any Spring AI 2.0.1 API surprises found while rebuilding `ChatResponse`/messages.
