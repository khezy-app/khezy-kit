# Task 05 — Secret keys: `checks/SecretKeysCheck` + `internal/SecretCandidateFilter`

## Objective

Implement deterministic **secret/key/token detection** per design §9.2 + §5.4. Emit a
`GuardrailResult` whose `maskEntities["secret"]` lists flagged tokens, `cleanedValue` is the
secret-redacted text, and `detected` = any secret found. Reuse the experimental **preset tuples**
already pinned in `policy/SecretPreset`: STRICT(10, 3.0, 2, true) · BALANCED(10, 3.8, 3, false) ·
PERMISSIVE(30, 4.0, 2, false) — `SecretConfig.DEFAULTS` uses BALANCED.

## Hand-off context

- **Design doc:** §9.2 (algorithm + table), §5.4 (`SecretConfig` fields), §7.2 (redaction hook),
  §10.2 (why this must stay deterministic + reusable in streaming).
- **From Task 02 (in-repo now):** `api/SecretConfig`, `policy/SecretPreset` (tuple params via
  `SecretPresetParams`), `api/GuardrailResult` + its `pass(...)`/`fail(...)` helpers, `api/GuardrailCheck`,
  `stream/MatchAccumulator`. Read the `api/` + `policy/` sources — signatures pinned there.
- **From Task 03:** `redact/Placeholders` + `redact/Redactor` for `cleanedValue`; `internal/AhoCorasick`
  exists for later streaming reuse but is NOT required in-memory here (literal loop is fine).
- **Parity decision (index/pinned):** every check dedupes `maskEntities` by value — streaming and
  in-memory MUST produce identical unique-token lists. So `maskEntities["secret"]` = **first-seen
  unique** secret tokens; `detected = !list.isempty()`.

## Files to create (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`)

### 1. `internal/SecretCandidateFilter.java` (shared predicate, package-visible)

The **single source of truth** for "is this string a high-entropy secret?" so in-memory (this task)
and streaming (Task 09) can't drift:

```java
public final class SecretCandidateFilter {
    public SecretCandidateFilter(SecretPresetParams params);
    public boolean accept(String candidate);
}
```
- **Candidate extraction** (in-memory helper too, in `SecretKeysCheck`): consecutive runs of
  `[A-Za-z0-9]` (base64/hex safe) plus optional `+/=` for base64 tokens.
- **Length:** `candidate.length() >= params.minLength`.
- **Entropy:** Shannon entropy `H = -Σ p(c)·log₂(p(c))` over per-character frequency of the run;
  `accept` iff `H >= params.minEntropy`.
- **Diversity:** distinct character count `>= params.minDiversity`.
- **strictMode=true (STRICT):** additionally require token is NOT directly adjacent to other
  alphanumerics (context boundary check) — reduces false positives inside identifiers/words.
- Order of checks: length → diversity → entropy (cheapest first). Pure function, no state.

### 2. `checks/SecretKeysCheck.java`

```java
public final class SecretKeysCheck {
    public SecretKeysCheck(SecretConfig config, Redactor redactor);
    public GuardrailResult run(String input);
}
```
- Implements `GuardrailCheck` (extend the functional type, keep `name()` = `"SecretKeysCheck"`).
- Tokenize input into candidate runs; `SecretCandidateFilter.accept` each; collect accepted
  **unique-first-seen**; build `maskEntities = Map.of("secret", List.copyOf(tokens))`.
- Also match `config.customPatterns()` (`Map<String, List<Pattern>>`) — each custom pattern's named
  category merges matches into the SAME `"secret"` bucket (reference contract); do not create extra
  entityTypes.
- `cleanedValue = redactor.redact(input, maskEntities)` (Task 03 `Redactor`), `detected = !tokens.isEmpty()`.
- No LLM, no logging. Must be usable from `toStream()` composition in Task 09 — keep a
  package-visible static/instance handle to `SecretCandidateFilter` + the custom patterns for reuse.

## Tests

- Preset behavior: BALANCED flags `sk-abcdefghijklmnop1234567890`-style token and rejects
  `session123`, `hello`; PERMISSIVE's 30-char min rejects the same; STRICT rejects a token glued
  into `my_session123` (boundary check) while BALANCED accepts it — assert STRICT/BALANCED split.
- `cleanValue`: `my-api-key=ABC...xyz` → the reported secret shows as `<SECRET>`.
- `maskEntities` uniqueness + first-seen order; `detected` false on plain prose.
- Custom pattern `Map.of("api_doc", List.of(Pattern.compile(...)))` adds its match to `"secret"`.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green; no `boolean`/mutable state in `SecretCandidateFilter`
  leak (stateless filter, immutable `SecretPresetParams`).
- `SecretKeysCheck` is the only checks-class in this task; `internal/SecretCandidateFilter` is the
  shared predicate Task 09 must reuse.

## Hand-off to next task (log in 00-HANDOFF.md)

- `SecretCandidateFilter` signature + false-positive rules captured AS TESTED (boundary behavior);
  the exact entropy/diversity formula.
- `SecretKeysCheck` ctor shape + `maskEntities` bucket `"secret"` for both preset + custom patterns.
- **Task 09 needs:** the filter handle + tokenization boundary rules so streaming windows reuse the
  same predicate; note in the log what "adjacent to alnum" means for a window edge.
- CREDITS.md: add a row for the entropy preset scheme (ported contract from n8n Guardrails options).