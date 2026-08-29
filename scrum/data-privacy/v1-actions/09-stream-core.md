# Task 09 — Streaming core: `stream/TextChunker` + `stream/Tokenizer` + `redact/StreamRedactor` + stream variants & parity

## Objective

Implement the **streaming engine** per design §10 and wire the deterministic checks' `toStream()`
overrides: window the input, run the SAME detection logic per window, dedupe across windows via
`MatchAccumulator`, stitch redaction without breaking tokens. Prove **in-memory ≡ streaming** parity
with dedicated tests. This is the join point for tasks 03/05/06/07 — the trickiest slice.

## Hand-off context

- **Design doc:** §10 (engine: §10.1 chunk, §10.2 two-pass, §10.3 accumulator, §10.4 tokenizer),
  §7.3 (Aho-Corasick streaming redaction), §3 (G1 determinism, G2 redaction, N7 streaming scope).
- **From earlier tasks (in-repo, trust the handoff log tail):**
  - Task 03: `redact/Redactor.redact(String, Map<String,List<String>>)` — **the equality target**
    for streaming redaction; `internal/AhoCorasick` with `scan(CharSequence, MatchVisitor)` +
    longest-match/reset semantics; `redact/Placeholders`.
  - Task 05: `internal/SecretCandidateFilter` + tokenization boundary rules (adjacent-alnum rule).
  - Task 06: `UrlsCheck`/`KeywordsCheck` per-window logic.
  - Task 07: `PiiCheck.run` aggregation (family `"pii"`, per-type `maskEntities` keys);
    `CustomRegexCheck`.
  - Task 02: `api/StreamCheck.scan(Reader, MatchAccumulator)`, `stream/MatchAccumulator` (dedupe by
    value, first-seen order), `api/GuardrailCheck.toStream()`.
- **Design decisions already pinned:**
  - **Default window 64 KB, overlap 1 KB** (design §10 configurable; expose as ctor args, default
    those two).
  - **Streaming adheres to the SAME regex/checksum/entropy logic as in-memory**; the only difference
    is chunking + cross-window dedupe. A match longer than the overlap is out of scope (N-scope,
    design) — enforce that reasoning in a doc comment, do not paper over it.

## Files to create (under `.../dpriv/`)

### 1. `stream/TextChunker.java`

```java
public final class TextChunker implements Iterator<String> {
    public TextChunker(Reader reader);                       // defaults: 64KB window, 1KB overlap
    public TextChunker(Reader reader, int windowSize, int overlap);
    public boolean hasNext(); public String next();
}
```
- Reads `windowSize` chars per step, retaining the last `overlap` chars as the next window's prefix;
  the final window is whatever remains (may be shorter). Emits empty string when nothing left.
- Total windows never exceed `reader.length / (windowWindow - overlap) + 1`; overlap < windowSize
  enforced at ctor (IllegalArgumentException).

### 2. `stream/Tokenizer.java`

```java
public final class Tokenizer {
    public static List<String> tokens(CharSequence window, int maxLen);
        // maximal runs of [A-Za-z0-9], each truncated at maxLen (0 = unlimited)
    public static List<String> trailingBoundary(CharSequence window);
        // the run at the very end (used to decide whether a boundary-token may be partial)
}
```
- Reuse the **same boundary definition Task 05 logged** (adjacent-alnum) so secrets streaming and
  in-memory agree on what a candidate run is.

### 3. `redact/StreamRedactor.java`

```java
public final class StreamRedactor {
    public StreamRedactor(TextChunker chunker, Map<String, List<String>> maskEntities);
    public void redact(Writer out);   // or String redact() over buffered reader
}
```
- Streams redaction with `internal/AhoCorasick` over each window's `scan`; replaces longest-match
  tokens with `Placeholders.forEntityType(...)`.
- **Boundary rule:** for window 0 emit everything; for later windows emit only the region after the
  (windowSize − overlap) boundary, **held back until a complete token/placeholder** — never split a
  placeholder or a matched token. Placeholder overlap protection (same as `Redactor`).
- **Contract:** `StreamRedactor` on full `maskEntities` output must EQUAL
  `Redactor.redact(fullText, sameMaskEntities)` character-for-character (the parity test).

### 4. Streaming variants — `GuardrailCheck.toStream()` overrides

In **each** of `PiiCheck` (07), `SecretKeysCheck` (05), `UrlsCheck` (06), `KeywordsCheck` (06),
add a `toStream()` override backed by a small `stream/…StreamCheck` impl (one per check / reuse one
generic window-accumulator helper if cleaner):

```java
public StreamCheck toStream();  // returns a StreamCheck that per window:
    // 1. runs the SAME public in-memory detection over the window substring
    // 2. adds each (entityType, token) to the MatchAccumulator sink
    // 3. lets MatchAccumulator dedupe across windows
```
- Must not rely on a window sized < overlap; `Level: window code reuses the check's own
  `GuardrailResult.run` per window to guarantee identical logic (but only reads its `maskEntities`).
- `LlmCheck` (08) and anything non-streamable keeps the api default throwing `toStream()`.

## Tests — the parity suite (most important deliverable of the project)

- **RedactorParityTest:** 200 KB mixed fixture (emails, secrets, URLs, keywords) → StreamRedactor
  output `==` `Redactor.redact` (same maskEntities). Vary window size (e.g. 512) to force many
  boundaries.
- **CheckParityTest:** for each of Pii / Secret / URL / Keyword: same fixture → in-memory
  `run(text).maskEntities` == streaming result after feeding full text through the `toStream()`
  check into `MatchAccumulator.toMaskEntities()`. Assert sets equal, order preserved.
- **ChunkerTest:** window sizes/overlap values; final short window; empty input → no windows;
  IllegalArgumentException for overlap ≥ windowSize.
- **Boundary secret:** a 70-char secret right at a window edge of a 64 KB window is still detected
  (reliance on overlap; choose fixture length exactly bridging a window edge).
- **No-partial-placeholder:** StreamRedactor never emits a trailing half-placeholder.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green (parity suite is part of `test`).
- All five stream joins compile against existing signatures — no new public API beyond the listed
  files; zero third-party deps.
- `toStream()` streaming variants exist on exactly Pii/Secret/URL/Keyword checks; LlmCheck does not.

## Hand-off to next task (log in 00-HANDOFF.md)

- TextChunker/Tokenizer/StreamRedactor signatures + window/overlap defaults + boundary-hold rule.
- Confirmed parity results (test names + outputs) — Task 11's `StreamPipeline` reuses these and the
  in-memory pipeline's Accumulator→`GuardrailResult` assembly.
- List the per-check `toStream()` classes and how Task 10/11 obtains the streaming GuardrailResult
  (parity: streamed `GuardrailResult.entityType` family strings identical to in-memory per check).
- Any parity test that needed adjustment (log exact reason — e.g. overlap cap for max token length).