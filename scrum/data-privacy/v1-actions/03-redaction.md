# Task 03 — Redaction engine: `redact/Placeholders` + `redact/Redactor` + `internal/AhoCorasick`

## Objective

Implement the **redaction core** per design §7: the placeholder contract (§7.1), the in-memory
literal longest-first redactor (§7.2), and the Aho-Corasick automaton that powers the streaming
variant in Task 09 (§7.3, `internal/`). This is the "transformative" half of the pipeline — the
piece that produces `GuardrailResult.cleanedValue` (design §5.1/§12.3) and the output of
`Guardrails.redact(...)`.

## Hand-off context

- **Design doc:** §7 (redaction), especially §7.1 placeholder contract and §7.2/§7.3 algorithms.
- **From Task 02 (available now, in-repo):** `api/GuardrailResult` (record with `maskEntities`),
  `api/GuardrailCheck`, `stream/MatchAccumulator` (`(entityType, token)` dedupe, first-seen order).
  Read them in `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/api/` — the
  signatures you depend on are pinned there; don't read other modules.
- **Semantics already resolved:**
  - Placeholder = **`<ENTITY>`**, `ENTITY` = the **policy rule name**, e.g. `<EMAIL_ADDRESS>`
    (design §7.1; resolved in design decisions — NOT `[REDACTED]`).
  - `maskEntities` maps `entityType` → list of **unique tokens** (first-seen order). Each token's
    EVERY occurrence in the text is replaced; a token already inside an emitted placeholder must be
    skipped (never corrupt a placeholder).
  - Core stays zero-dependency (`java.util` + `java.util.regex` only).

## Files to create (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`)

### 1. `redact/Placeholders.java`

```java
public final class Placeholders {
    public static String forEntityType(String entityType); // "pii_email"        -> "<EMAIL_ADDRESS>"
    public static final Pattern TOKEN = ...;               // <[A-Z0-9_]+>  (used to skip protected regions)
}
```
- `forEntityType`: strip a leading `pii_` family prefix and uppercase the rule name; known non-PII
  families map to their `entityType.toUpperCase()` name (`secret` → `<SECRET>`, `link` → `<LINK>`,
  `keyword` → `<KEYWORD>`, `jailbreak` → `<JAILBREAK>`). The exact mapping table lives here —
  document it in the handoff log since `PiiEntity`/checks reuse it.
- Unknown types fall back to `"<" + sanitize(entityType).toUpperCase() + ">"` (alnum/underscore only).

### 2. `internal/AhoCorasick.java`

Standard Aho-Corasick automaton with failure links + longest-match emission (design §7.3):

```java
public final class AhoCorasick {
    public static AhoCorasick compile(Map<String, List<String>> maskEntities);
    public void scan(CharSequence input, MatchVisitor visitor); // resets to root per call
}
public interface MatchVisitor { void match(int start, int end, String token, String entityType); }
```
- Build nodes from each `(entityType, token)`; a node may carry the outputs of multiple
  `(token, entityType)` pairs. Emit the **longest** match ending at each position
  (compare by output length), then continue scanning (do not skip).
- Matches are emitted ordered by end position; visitors that only keep "no overlap" windows handle
  overlap.
- `compile` must throw `IllegalArgumentException` on empty `maskEntities` or empty token text
  (validation test).

### 3. `redact/Redactor.java`

In-memory, whole-input redactor (design §7.2 — "literal longest-first"):

```java
public final class Redactor {
    public String redact(String input, Map<String, List<String>> maskEntities);
}
```
- Replace every occurrence of every token with `Placeholders.forEntityType(entityType)`.
- **Longest-first:** when token A is a substring of token B, B wins → emit B, then the region B
  covers is consumed (A never applies inside B's span).
- **Placeholder protection:** after emitting `<X>`, no later (shorter) token may match inside it.
  Recommended approach: single pass with the Aho-Corasick automaton (`internal/AhoCorasick`) +
  a "protected regions" list; on each longest match, discard it if it overlaps an emitted
  placeholder, else emit the placeholder and append its span to the protected list. This keeps the
  in-memory and streaming code paths behaviorally identical (parity requirement, design §10.3).
- Single matches crossing no boundaries: `redact` is pure (immutable input), deterministic.
- If `maskEntities` is empty, return `input` unchanged (no allocation churn).

## Tests (`securities/data-privacy-core/src/test/java/io/github/khezyapp/dpriv/...`)

JUnit 5 + AssertJ (see `khezy-coding-style` skill for test layout). Cambodia-typed fixtures:
`visal@example.com`, `SOK`, `Phnom Penh`, `Siem Reap`.
- Redactor replaces `visal@example.com` → `<EMAIL_ADDRESS>`; a longer token wins over contained
  shorter token; a token equal to or containing `<EMAIL_ADDRESS>` text does not corrupt/self-match
  (use a secret named `EMAIL` to prove placeholder protection).
- Determinism: same input twice → identical output (G1 pre-check).
- `AhoCorasick` parity: multi-token map, overlap, longest-match via visitor on a windowed input;
  `compile` validation paths throw.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green (compile + tests + Checkstyle).
- `<ENTITY>` placeholder format observable in Redactor output; `Placeholders` mapping table present.
- Parity rule: Redactor output == what the streaming path must produce for the same tokens (the
  streaming `StreamRedactor` in Task 09 targets `Redactor.redact` equality — log the exact contract
  below so Task 09 can assert `assertEquals`).

## Hand-off to next task (log in 00-HANDOFF.md)

- Exact `Placeholders.forEntityType` mapping table (every family prefix → placeholder string).
- `AhoCorasick` public API + behavior notes (reset semantics, longest-match, overlap emission),
  since Task 09's `StreamRedactor` + `Tokenizer` consume it.
- The `Redactor.redact` signature + "longest-first, placeholder-protected" contract that Task 09
  must match exactly in streaming mode.
- The worker hint for Task 04 (Pii catalog): `PiiEntity` constants + `type()` strings are already
  pinned from Task 02's handoff entry.