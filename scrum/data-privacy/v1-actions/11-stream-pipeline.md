# Task 11 — Streaming pipeline: `pipeline/StreamPipeline` + `Guardrails.scan(Reader)` / `redact(Reader, Writer)`

## Objective

Complete the **streaming half** of the `Guardrails` facade (design §10.2, §12.1): a `ScanOutcome`
from `scan(Reader)` and a line-buffered `redact(Reader, Writer)` via the two-pass model
(pass 1: scan all streamable checks; pass 2: `StreamRedactor`). This reuses Task 09's
`toStream()` checks + Task 10's outcome assembly; no new detection logic.

## Hand-off context

- **Design doc:** §10.2 (two-pass architecture), §12.1 (facade streaming signatures),
  §13 (threading), §5.3 (ScanOutcome).
- **From Task 09 (handoff log tail):** `stream/TextChunker`, `stream/Tokenizer`,
  `redact/StreamRedactor`, the per-check `toStream()` classes proving parity in-memory==streaming.
  Streaming check entities/families identical to in-memory outcomes.
- **From Task 10:** `GuardrailPipeline` stage ordering, `ScanOutcome` derivation rules,
  `failOnlyOnErrors` behavior, Outcome no-null-map/`List.of()` defaults.
- **Resolved decisions:**
  - **Two-pass requires a re-readable source for `redact(Reader, Writer)`.** For one-shot
    `Reader`s, buffer the input to memory (document this; scan-only path MUST stay O(window) memory —
    no full buffering).
  - Streaming evaluator reuses the **same stage classification**: only streamable deterministic
    checks (Pii/Secret/Url/Keyword/CustomRegex) run streaming; `LlmCheck` never participates in
    streaming (`run(…, CLASSIFY)` stays in-memory on `String`).

## Files to create / edit (under `.../dpriv/`)

### 1. `pipeline/StreamPipeline.java`

```java
public final class StreamPipeline {
    public StreamPipeline(GuardrailsConfig config, List<GuardrailCheck> preflight,
                          List<GuardrailCheck> classificatory, boolean failOnlyOnErrors);
    public ScanOutcome scan(Reader input);      // pass 1 only
    public void redact(Reader input, Writer out); // pass 1 (scan) → pass 2 (StreamRedactor)
}
```
- `scan(Reader)`: run each streamable preflight check's `toStream()` concurrently (reuse
  `ParallelStageRunner` join discipline — common pool), each writing into its own `MatchAccumulator`;
  merge per family; assemble a `ScanOutcome` with the same `entityTypes()`/`maskEntities`/
  `auditRecords` derivation Task 10 logged.
- `redact(Reader, Writer)`: pass 1 scans to build full `maskEntities`; pass 2 redirects via
  `StreamRedactor(chunker(maskEntities))` → `Writer`. Deterministic and parity-equal to the
  in-memory `redact(String)` (Task 09 ensured).
- Both must close nothing the caller opened (library never closes caller-owned Readers/Writers);
  flush the Writer on success.

### 2. `api/Guardrails.java` (fill the stub’s streaming half)

```java
public ScanOutcome scan(Reader input);       // → StreamPipeline.scan
public void redact(Reader input, Writer out);// → StreamPipeline.redact
```
- Share config with the in-memory `Guardrails` from Task 10; prefer re-readable input, buffer a
  one-shot Reader for `redact` only.

## Tests

- **Parity (streaming == in-memory):** same big fixture (≥ 2 windows) →
  `scan(reader).maskEntities` == `scan(string).maskEntities`; `redact(reader, writer)` ==
  `redact(string)`.
- **One-shot reader:** pass a fixed-source one-shot Reader → `redact` still correct (buffered path).
- **Empty / tiny input:** one-char, empty string → empty ScanOutcome, no `match` errors.
- **Streaming never runs LLM:** a stub classifier is NOT invoked by `scan(Reader)`/`redact(Reader,…)`.
- **Concurrent stream scans deterministic:** run `scan(Reader)` 50× → same outcome; window-boundary
  fixture (token exactly at edge) produces same result as in-memory.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green.
- `scan` doesn't buffer whole input (assert only if feasible — code-review assertion in handoff log).
- Streaming methods match the `Guardrails` facade surface promised in Task 02 (no new public
  methods beyond those two).

## Hand-off to next task (log in 00-HANDOFF.md)

- `StreamPipeline` signatures + the re-readable-source contract + one-shot buffering note.
- Confirmed streaming vs in-memory parity assertions (test names).
- Guardrails config/threading summary so Task 12 can wire `SpringAiLlmClassifier` into the same
  facade (Task 12 targets `run(…, CLASSIFY)` only).