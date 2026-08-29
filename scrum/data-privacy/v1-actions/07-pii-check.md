# Task 07 — PII check + custom regex check: `checks/PiiCheck` + `checks/CustomRegexCheck`

## Objective

Assemble the PII + custom-regex **guarded check** per design §9.1 and §9.5: run the 33-entity catalog
(`ALL` coverage or an explicit `SELECTED` entity list) honoring `PiiConfig.strict()` (checksum
validation on/off from §8.2), fold in named custom regex groups, and emit ONE aggregated
`GuardrailResult`. This is Task 04's consumer and the biggest of the "detect" checks.

## Hand-off context

- **Design doc:** §9.1 (PiiCheck — coverage, customGroups, strict), §9.5 (CustomRegexCheck),
  §5.4 (`PiiConfig`, `CustomRegexConfig`), §7.2 (cleanedValue via Redactor), §12.3 (entityType
  naming in outcomes).
- **From Task 02 (in-repo):** `api/PiiConfig` (`coverage`, `entities`, `customRegexes`, `strict`),
  `api/CustomRegexConfig` (`name`, `patterns`), `api/GuardrailResult`+helpers.
- **From Task 03:** `redact/Redactor` (`Redactor.redact(input, maskEntities)`), `Placeholders`
  (per-family mapping).
- **From Task 04:** `policy/PiiPatterns` — `forEntity`, `isStrictMatch`, `isNonStrictMatch`,
  `all()`; `policy/ChecksumValidators` if needed for direct test assertions.
- **Dedupe parity (pinned):** `maskEntities` unique-first-seen per entityType. `GuardrailResult`
  masking must survive redaction through `Placeholders` (post-checksum tokens redacted by family).

## Files to create (under `.../dpriv/checks/`)

### 1. `checks/CustomRegexCheck.java`

```java
public final class CustomRegexCheck {
    public CustomRegexCheck(List<CustomRegexConfig> configs, Redactor redactor);
    public GuardrailResult run(String input);
}
```
- One match group per `CustomRegexConfig.name()`; each `name` → its own `maskEntities` key
  (NOT folded into `"pii"`). Tokens = full matched text (unique-first-seen).
- `detected = any group matched`; `cleanedValue = redactor.redact(input, maskEntities)`.
- Empty configs → identity result (`pass`); malformed/empty `name` → skip that group (never throw).

### 2. `checks/PiiCheck.java`

```java
public final class PiiCheck {
    public PiiCheck(PiiConfig config, Redactor redactor);
    public GuardrailResult run(String input);
}
```
- Coverage resolution: `ALL` → `PiiPatterns.all().keySet()`; `SELECTED` → `config.entities()`.
- Per matched entity: run `PiiPatterns.forEntity(entity)` matcher over input; for each candidate
  match hit, validate via `PiiConfig.strict()`:
  - `strict=true`  → `PiiPatterns.isStrictMatch(entity, token)` (regex + checksum for the
    checksum-backed entities).
  - `strict=false` → `PiiPatterns.isNonStrictMatch(entity, token)` (regex only).
- **Dedupe:** accumulate unique-first-seen tokens under the entity's `type()` key. Aggregated result:
  - `entityType = "pii"` (family name per §12.3), `detected = any matched`,
    `maskEntities` = { `"pii_email"`: [...] , `"pii_credit_card"`: [...], ... `"pii_us_ssn"`: ... },
    `cleanedValue = redactor.redact(input, maskEntities)`.
- Also run `CustomRegexCheck` over `config.customRegexes()` and MERGE its `maskEntities` + detect
  state into the same aggregated `GuardrailResult` (design §9.1: customGroups are part of PiiCheck).
- Empty `SELECTED` entity set → returns `pass` (no children run).

## Tests

- `ALL` coverage on a Khmer-contact fixture (`visal@example.com · SOK · Phnom Penh`) → aggregated
  `maskEntities` contains `pii_email` (+ geo/person if patterns fire; assert whichever §8 yields —
  build fixtures to hit at least EMAIL + GEO_LOCATION deterministically).
- `SELECTED={EMAIL_ADDRESS}` only → `pii_email` present, `pii_geo_location` absent; `detected` false
  for a fixture with no email.
- strict on/off split: 16-digit number passing pattern + failing Luhn → strict: rejected,
  non-strict: accepted (uses Task 04's pair).
- Custom group `"order_ref"` appears as its own `maskEntities` key alongside `pii_*` keys;
  `detected=true` when only a custom match exists.
- `cleanedValue` shows `<EMAIL_ADDRESS>` and `<ORDER_REF>` families, and no raw token survives
  (iterate every `maskEntities` value and assert none is a substring of `cleanedValue`).
- No-match prose → `pass`/`isPassed()` true, `cleanedValue == input`.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green.
- PiiCheck is the **single** entry point Task 09 (streaming) and Task 10 (pipeline) use for PII.
- custom regex semantics exactly as §9.5/§9.1 (own group keys, never inside `pii_*`).

## Hand-off to next task (log in 00-HANDOFF.md)

- `PiiCheck.run` aggregation contract (family `"pii"`, per-type `maskEntities` keys, custom folds),
  strict/non-strict wiring.
- The exact entity-type→`maskEntities` keys PiiCheck emits (from Task 04 + this task), because
  Task 09's streaming and Task 10's pipeline assert on them.
- Reuse contracts for `CustomRegexCheck` (kept public for Task 10 to run standalone if needed).