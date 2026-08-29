# Task 04 — Composition acceptance: end-to-end order, guarantee regression, README

## Objective

Prove the v2 guarantees (design §5) end to end: (1) each advisor works through a real
`ChatClient`; (2) **the composition order** — input: redact → gate; output: gate → redact — holds
when both advisors use their default orders; (3) the guarantee scope G8–G16 / non-guarantees
N6–N10 maps to passing tests; (4) module README documents the two quick-starts, the ordering rule,
and the exception reference. **This is the join point (INDEX dependency graph) — the trickiest
slice.**

## Hand-off context

- **Design doc:** §5 (guarantees G8–G16, non-guarantees N6–N10), §9 (reports), §11 (ordering &
  composition), §16 (quick-start).
- **From Task 02 (in-repo):** `DataPrivacyAdvisor` as built (builder, `CONTEXT_KEY`,
  before/after/adviseStream, `RedactionException`). Signatures in Task 02's handoff log entry.
- **From Task 03 (in-repo):** `GuardrailAdvisor` as built (builder incl. `classifier(...)`,
  `CONTEXT_KEY`, R3 interpretation, `PolicyViolationException`,
  `GuardrailEvaluationException`). Signatures in Task 03's handoff log entry.
- **From core (in-repo, pinned):** `Guardrails` facade + `GuardrailsConfig.DEFAULTS`,
  `LlmClassifier` SPI. **Test stubs:** anonymous `LlmClassifier`s + a hand-written
  `ChatModel` stub capturing the `Prompt` it receives — see `EndToEndSpringAiTest` (v1) for the
  established pattern; no Mockito.
- **Spring AI (pinned):** `ChatClient.builder().model(stub).defaultAdvisors(...).build()`;
  `client.prompt().user(text).call()` and `.stream()`; `ChatClientResponse.context()`.
- **Resolved decisions (INDEX, verbatim):** all — especially **R3** (violation vs judge-error
  reading), **R6** (`.collectList().block()` for stream assertions).

## Design notes

- **Chain mechanics (design §11.1) — the assertion core:** Spring AI executes `before` hooks in
  advisor order and `after` hooks in reverse (stack). With defaults (DPA `HIGHEST_PRECEDENCE`,
  GRA `+1`):
  - **input:** `DPA.before` (redact) runs first, so the GRA's judge receives **redacted** text and
    the stub model receives **redacted** text;
  - **output:** `GRA.after` (gate) runs first, so the judge sees the **raw** output; then
    `DPA.after` (redact) cleans whatever passed.
- The E2E tests below assert this by **capturing what each layer sees** (stub model records the
  prompt; GRA's classifier records classified text; final response text + context reports are
  inspected by the test). No timing/async assertions.
- Guarantee mapping: name one test per guarantee (G8–G16) so acceptance is auditable. N6–N10 are
  documented as **not** tested (they are explicit non-guarantees — assert only that README lists
  them).

## Files to create

Under `securities/data-privacy-spring-ai/src/test/java/io/github/khezyapp/dpriv/springai/`:

### 1. `EndToEndDataPrivacyAdvisorTest.java`

Real `ChatClient` + stubbed `ChatModel` (records the `Prompt` it receives; returns a canned
`ChatResponse`). Khmer test data.

- `chatClientRedactsUserMessageBeforeModelSeesIt` — `.defaultAdvisors(DPA.config(DEFAULTS))`;
  `client.prompt().user("my email is visal@example.com and phone 012 345 678").call()` → captured
  prompt user text contains `<EMAIL_ADDRESS>`/`<PHONE>` placeholders, no raw tokens (G8, call path).
- `chatClientRedactsStreamedUserMessageBeforeModelSeesIt` — same via `.stream()` → captured prompt
  redacted (G8, stream path).
- `outputScopeRedactsResponseForCaller` — scope `BOTH`, response text contains the email → caller's
  `content()` contains `<EMAIL_ADDRESS>` only (G9).
- `reportIsReadableFromResponseContext` — after `call()`, `response.context()` (or the content
  path's `ChatClientResponse`) carries `RedactionReport` with `redacted=true` (G13/observability).

### 2. `EndToEndGuardrailAdvisorTest.java`

- `flaggedInputBlocksRequestBeforeModel` — `.defaultAdvisors(GRA.classifier(flagged))`;
  `assertThatThrownBy(() -> client.prompt().user("...").call())` is `PolicyViolationException`
  (entityType `jailbreak`); **stub model never invoked** (flag on the stub) (G14).
- `cleanInputReachesModelUnchanged` — clean classifier → model called with the original text,
  request identical (G11/G16).
- `flaggedOutputIsBlockedFromCaller` — scope `OUTPUT`, flagged classifier, model returns a canned
  response → `PolicyViolationException` with `scope()==OUTPUT` (G14/LLM05).

### 3. `CompositionOrderTest.java` — the parity/composition slice (design §11)

Setup: DPA (defaults) + GRA (defaults) via `defaultAdvisors(privacy, gate)`; GRA's classifier stub
**records every text it classifies**; the `ChatModel` stub records the prompt it receives; both
advisors' context reports are read from the final response.

- `inputPathIsRedactThenGate` — user text with PII **and** a benign prompt:
  `client.prompt().user("my email is visal@example.com. tell me a joke").call()` →
  (1) GRA's recorded classified text **contains `<EMAIL_ADDRESS>` and not the raw email**;
  (2) stub model's prompt also contains the placeholder only (G8 + §11 order).
- `outputPathIsGateThenRedact` — scope `BOTH` on both advisors; model returns text containing the
  email → (1) GRA's classified output text **contains the raw email** (gate sees raw output);
  (2) caller's `content()` contains `<EMAIL_ADDRESS>` only (G9 + §11 reverse order).
- `cleanThroughputKeepsBothReports` — fully clean round-trip → response context carries
  `RedactionReport(redacted=false, ...)` and `GuardrailReport(passed=true, ...)` (G13).
- `flaggedInputBypassesModelAndReturnsNoRedaction` — flagged input → `PolicyViolationException`;
  model stub not invoked; no `RedactionReport` on the exception path.

### 4. `GuaranteeScopeAdvisorTest.java` — G8–G16 regression (design §5.1)

One test per guarantee, reusing the above fixtures (names pin the guarantee):

- `g8InputNeverLeaksRaw` — INPUT scope; model prompt has no raw matched token.
- `g9OutputScrubbedOnWayBack` — OUTPUT scope; caller output has no raw matched token.
- `g10RedactionFailClosed` — R4 config (see Task 02) + `failOnError=true` → `RedactionException`;
  model stub not invoked.
- `g11NonInterference` — system message + media + metadata byte-identical after a redacted
  round-trip.
- `g12IdempotentRedaction` — redacting the already-redacted text yields identical text
  (placeholder not re-detected).
- `g13ZeroSideEffects` — no logger invocations: assert the advisor classes contain no
  `Logger`/`log(` usage (grep-style assertion via source scan is acceptable, or
  `assertThat(loggerIsAbsent)` helper reading the class bytes for `org.slf4j` references).
- `g14ViolationsAlwaysBlocked` — `failOnError=false` + flagged → still `PolicyViolationException`.
- `g15JudgeFailClosed` — broken classifier + default → `GuardrailEvaluationException`; model stub
  not invoked.
- `g16InputGatingTargetsNewInput` — GRA classifies only the last user message (recorded inputs
  assert exactly one classification, equal to the last user text).

### 5. README update — `securities/data-privacy-spring-ai/README.md`

Edit (append or restructure — match existing style): two quick-start snippets from design §16
(DPA only; DPA+GRA combined), the ordering rule (design §11.2 guard rule verbatim), the scope/mode
matrix (design §10), and the exception reference table (design §8.9). Document N6–N10 as
non-guarantees. **No logging/emoji; keep it dry.**

## Tests summary / acceptance criteria

- `./gradlew :data-privacy-spring-ai:build` → BUILD SUCCESSFUL (compile + tests + Checkstyle).
- No `build.gradle` edits; version stays `1.0.0`; no new production files (only tests + README).
- Every guarantee G8–G16 has a named passing test; README updated.

## Hand-off to next task (log in 00-HANDOFF.md)

- The full verified guarantee → test mapping (one line per G).
- The confirmed composition behavior as-built (any deviation from design §11 — e.g. if Spring AI's
  actual chain reverse-order differed, log it).
- README diff summary; final acceptance statement for the whole v2 plan (all 4 tasks green).
- Run `graphify update .` at the repo root and log the result.
