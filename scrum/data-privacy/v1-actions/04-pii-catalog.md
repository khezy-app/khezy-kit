# Task 04 — PII catalog: `policy/PiiPatterns` (33 patterns) + `policy/ChecksumValidators`

## Objective

Implement the **PII detection catalog** per design §8: a precompiled regex table for **exactly the
33 entities** listed in design §8 (Global 9 · US 5 · UK 2 · ES 2 · IT 2 · PL 1 · SG 2 · AU 4 · IN 5 ·
FI 1), plus the opt-in checksum validators (Luhn for CREDIT_CARD, mod-97 for IBAN_CODE, Verhoeff for
IN_AADHAAR) that `strict` mode uses to reject syntactically-but-not-numerically-valid matches.

## Hand-off context

- **Design doc:** §8 (table of all 33 entities, their `type()` strings, greedy vs strict expressions).
  **The §8 table is the contract for names AND patterns** — reproduce it exactly.
- **From Task 02 handoff (read the log tail, then verify in source):** `policy/PiiEntity` enum
  (33 constants, `type()` = `pii_`-prefixed contract string). This task must NOT re-define the enum —
  it must match Task 02's constants 1:1; add a **self-check test** asserting
  `EnumSet.allOf(PiiEntity.class)` has exactly the 33 names from §8 and every constant's
  `type()` is `pii_` + UPPER_SNAKE rule name.
- **From Task 03:** none needed here. Core stays zero-dependency.
- **Attribution §17:** ported patterns must go from the **OpenAI Guardrails JS** and **n8n**
  pattern tables → re-implemented, ADDITION row added to `CREDITS.md` (in-repo from Task 01).

## Files to create (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`)

### 1. `policy/PiiPatterns.java`

```java
public final class PiiPatterns {
    public static Pattern forEntity(PiiEntity entity);           // precompiled singleton
    public static Map<PiiEntity, Pattern> all();                 // unmodifiable, all 33
    public static boolean isStrictMatch(PiiEntity entity, String token); // pattern AND valid checksum
    public static boolean isNonStrictMatch(PiiEntity entity, String token); // pattern only
}
```
- Build the table lazily in a holder (init-on-demand) so the JVM doesn't precompile all regexes on
  class load. Compile once; reuse.
- Patterns come from design §8 — including any **embedded flags** (`(?i)` case-insensitivity,
  `\b` word boundaries) exactly as §8 lists them. Where §8 distinguishes "strict pattern requires
  checksum", encode that here as the `isStrictMatch` vs `isNonStrictMatch` distinction (design §8.2).
- Keep each pattern as a `private static final String` regex const named after the entity — the task
  source must be readable against §8 so a reviewer can diff the table.

### 2. `policy/ChecksumValidators.java`

```java
public final class ChecksumValidators {
    public static boolean luhn(String digits);      // Luhn mod-10 (CREDIT_CARD)
    public static boolean mod97(String iban);       // ISO 7064 mod-97-10, IBAN normalization
    public static boolean verhoeff(String digits);  // Verhoeff dihedral (IN_AADHAAR)
}
```
- Return `false` on non-numeric/scrambled input rather than throwing (a malformed token must fail
  the checksum, never crash a check — design §8.3 robustness).
- `luhn`: standard Luhn, double/digit-sum from rightmost; accept classic 13–19 digit card forms.
- `mod97`: strip `IBAN` prefix handling + country check-digit positions per §8 (validate the
  full IBAN, not just numeric slice).
- `verhoeff`: dihedral/permutation/inverse tables; implement cleanly, no magic arrays hiding
  semantics (name the three tables `D_TABLE`, `P_TABLE`, `INV_TABLE`).
- `strict` mode wiring: `PiiPatterns.isStrictMatch` calls the relevant validator; `isNonStrictMatch`
  does not. PiiCheck (Task 07) passes `PiiConfig.strict()` through — this task only exposes the two
  predicates.

## Tests

- Every one of the 33 entities: §8's canonical true example matches; a `\b`-needing negative (e.g.
  a plain number padded with letters) does not. Use §8 example values.
- Checksums: known-good Luhn (`visa 4111111111111111`, `mastercard 5555555555554444`) true;
  toggled digit false. IBAN `GB82 WEST 1234 5698 7654 32` true; corrupted false. Aadhaar 12-digit
  Verhoeff example from §8 true; altered false.
- Gale-breaking `strict=false` case: an 16-digit number passing pattern but failing Luhn → true via
  `isNonStrictMatch`, false via `isStrictMatch`.
- Self-check: `PiiPatterns.all().size() == 33` and exactly §8's names; `PiiEntity` constant set equal.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` green.
- Patterns + checksums live only in these two files; `PiiPatterns.all()` immutable.
- Self-check test guards **constant-name ≤→ pattern** drift between Tasks 02 and 04.

## Hand-off to next task (log in 00-HANDOFF.md)

- Summary of any §8 table ambiguities you hit and how you resolved them (kept source-line of §8
  reference). Exact table of the 33 `type()` strings.
- `PiiPatterns`/`ChecksumValidators` signatures.
- **Next task (07) needs:** `PiiPatterns.forEntity` + `isStrictMatch`/`isNonStrictMatch` +
  `PiiEntity` iteration order, and confirmation that `PiiConfig.strict()` wires to these two
  predicates (Task 07 implementation).
- CREDITS.md row added for the pattern table sources.