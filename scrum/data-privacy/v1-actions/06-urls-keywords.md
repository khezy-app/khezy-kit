# Task 06 — URL + keyword checks: `checks/UrlsCheck` + `checks/KeywordsCheck`

## Objective

Implement the two remaining "detect + sanitize" Family A checks per design §9.3 (URLs) and §9.4
(keywords). Both emit a `GuardrailResult`; both are streamable per design §10 (Task 09 builds the
stream variants — this task keeps them in-memory with a `toStream()` hook).

## Hand-off context

- **Design doc:** §9.3 (URL check — 3-pass algorithm, `maskEntities["link"]`), §9.4 (keywords —
  `toMask` behavior, `maskEntities["keyword"]`), §5.4 (`UrlsConfig`, `KeywordsConfig`).
- **From Task 02 (in-repo):** `api/UrlsConfig` (`allowedSchemes=["http","https"]`, `allowedHosts=[]`),
  `api/KeywordsConfig` (`toMask`, `keywords`), `api/GuardrailResult`+helpers, `api/GuardrailCheck`.
- **From Task 03:** `redact/Redactor` for `cleanedValue`; `redact/Placeholders` (`link` → `<LINK>`,
  `keyword` → `<KEYWORD>` table already built there).
- **Parity & dedupe (pinned):** unique-first-seen token lists in `maskEntities`; streaming == in-memory.

## Files to create (under `.../dpriv/checks/`)

### 1. `checks/UrlsCheck.java`

```java
public final class UrlsCheck {
    public UrlsCheck(UrlsConfig config, Redactor redactor);
    public GuardrailResult run(String input);
}
```
- Follow the design §9.3 **3-pass pipeline**:
  1. **Detect:** regex over candidate URLs (`https?://...`, `www.` optional forms as §9.3 defines).
  2. **Parse:** `java.net.URI` per candidate (fail → drop candidate, do NOT throw — malformed input
     is a detection artifact, never a crash).
  3. **Policy classify:** flag a link if **(a)** its scheme is not in `config.allowedSchemes()`;
     **(b)** it contains `userinfo` (`user:pass@host`) — **always blocked** regardless of schemes
     (design decision); **(c)** if `allowedHosts()` is non-empty, host not in the allow-list.
- `maskEntities = Map.of("link", <unique flagged URLs>)`; `detected = !list.isempty()`;
  `cleanedValue = redactor.redact(input, maskEntities)`.
- Do not treat every present URL as a violation — only policy-violating ones (a clean https URL with
  no userinfo passes; `detected=false`, `cleanedValue=input` unchanged).
- Expose a package-visible `toStream()` support hook (empty or documented for Task 09) — streaming
  must re-run the same passes over windowed text with overlap, so keep the per-URL classifier as a
  small private static method Task 09 can mirror.

### 2. `checks/KeywordsCheck.java`

```java
public final class KeywordsCheck {
    public KeywordsCheck(KeywordsConfig config, Redactor redactor);
    public GuardrailResult run(String input);
}
```
- Case-insensitive, whole-word matching against `config.keywords()`. Empty keyword list → no-op.
- `maskEntities["keyword"]` = unique case-preserved first-seen keyword instances found.
- If `config.toMask()` is true → `cleanedValue = redactor.redact(input, maskEntities)`;
  else `cleanedValue = input` and `detected = !list.isempty()`.
- Keep the matcher trivial (case-insensitive literal search); Task 09 streams it by scanning each
  window with the same matching rules.

## Tests (JUnit 5 + AssertJ, Khmer/Cambodia fixtures)

- URLs: `http://visal.example` with `allowedSchemes=[https]` → flagged; `https://example.com` clean →
  not flagged; `ftp://example.com` flagged; `https://user:pass@example.com` flagged (userinfo);
  `allowedHosts=[example.com]` → `https://phnompenh.example.org` flagged; malformed URL text
  (`http://`) ignored, no exception.
- Keywords: `toMask=true` → keyword shows as `<KEYWORD>` in `cleanedValue`; `toMask=false` → text
  unchanged but `detected=true`; empty config no-op; uniqueness.
- Both checks return `isPassed()==false` only when `detected`.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green.
- URL policy rules exactly as §9.3 (scheme, userinfo-always-block, host allow-list when non-empty).
- Streaming-ready: no per-check static mutable state; both classes hold only immutable config +
  Redactor.

## Hand-off to next task (log in 00-HANDOFF.md)

- `UrlsCheck` / `KeywordsCheck` signatures + the per-URL classifier rule table.
- WordPress-free `toStream()` hint needed by Task 09 (window + overlap boundary behavior for URL and
  keyword scans).
- CREDITS.md row for URL regex + keyword check patterns (ported from reference tables).