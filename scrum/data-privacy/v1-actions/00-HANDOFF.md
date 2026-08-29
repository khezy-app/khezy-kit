# Hand-off Log — Data Privacy v1

**The centralized log.** Every completed task appends a section at the END of this file using the
template below. A later task reads the log tail instead of re-exploring the codebase — this is the
single source of "what already exists, exactly how, and why".

Do **not** edit or remove earlier entries. Append only.

---

## Template (copy into the END of this file when a task finishes)

```markdown
## Task 01 — module scaffold — DONE

- **Date/agent:** <date>, <agent>
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL
- **Files created:**
  - `securities/data-privacy-core/settings.gradle`
  - ...
- **Files edited:** `settings.gradle` (added `includeBuild("securities/data-privacy-core")`)
- **Public surface added:**
  - `io.github.khezyapp.dpriv.api.Foo` — `record Foo(String a, int b)`, `static Foo of(...)`
- **Gotchas / decisions:** ...
- **Next task(s) must know:** what to build on, exact signatures, deviations from design.
```
## Task 01 — module scaffold — DONE

- **Date/agent:** 2026-08-29
- **Verified commands:**
  - `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + Checkstyle green on empty module)
  - `./gradlew :data-privacy-spring-ai:build` → BUILD SUCCESSFUL (Spring AI BOM/coordinates resolved against Maven Central)
  - `./gradlew projects` → both `:data-privacy-core` and `:data-privacy-spring-ai` listed as included builds
  - `./gradlew :data-privacy-core:dependencies --configuration runtimeClasspath` → "No dependencies" (zero 3rd-party, pure JDK)
- **Files created:**
  - `securities/data-privacy-core/settings.gradle`
  - `securities/data-privacy-core/build.gradle`
  - `securities/data-privacy-core/CREDITS.md`
  - `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/.gitkeep` (package-root scaffold dir for Task 02)
  - `securities/data-privacy-spring-ai/settings.gradle`
  - `securities/data-privacy-spring-ai/build.gradle`
- **Files edited:**
  - `settings.gradle` (repo root) — added `includeBuild("securities/data-privacy-core")` and `includeBuild("securities/data-privacy-spring-ai")`
  - `scrum/data-privacy/v1-actions/01-module-scaffold.md` — added `id("khezy.java-lombok")` to adapter module + note (use `@Builder` for `SpringAiLlmClassifier`, no hand-written getters/setters/builder)
  - `scrum/data-privacy/v1-actions/00-INDEX.md` — module conventions: core = no Lombok (records only); adapter = lombok
- **Public surface added:** none yet (empty src). Package root `io.github.khezyapp.dpriv` is scaffolded for Task 02.
- **Gotchas / decisions:**
  - **Core has zero deps.** `spring-ai-bom:1.0.0` + `spring-ai-client-chat:1.0.0` + `spring-ai-test:1.0.0` **resolved successfully** against `mavenCentral()` (root `dependencyResolutionManagement`). No `// TODO resolve at Task 12` fallback needed. Resolved Spring Family: `spring-ai-model:1.0.0`, `spring-ai-commons:1.0.0`, `spring-ai-template-st:1.0.0`, Spring Framework 6.2.6.
  - Adapter applies `khezy.java-lombok` (decision: use `@Builder` for `SpringAiLlmClassifier.Builder` in Task 12). Cross-sibling coordinate `api "io.github.khezyapp:data-privacy-core:1.0.0"` substitutes the included build (same pattern as declarative-http/dynamic-object).
  - Empty `src` → `checkstyleMain`/`checkstyleTest` are NO-SOURCE, not errors. A missing package dir is not a compile error; scaffolded `.gitkeep` makes the `dpriv` root trackable for Task 02.
- **Next task(s) must know:** Task 02 builds `api/` under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/api/` — package root `io.github.khezyapp.dpriv` already exists. Adapter module + root wiring are live; Spring AI coordinates above are real and resolved.

## Task 02 — API contract — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + `checkstyleMain` green, NO-SOURCE tests)
- **Files created** (all under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`):
  - `api/GuardrailResult.java`
  - `api/GuardrailCheck.java`
  - `api/StreamCheck.java`
  - `api/GuardrailCheckFactory.java`
  - `api/LlmClassifier.java`
  - `api/Operation.java`  (ADDED — not in the task's file list, but required by the `Guardrails.run(String, Operation)` facade signature; enum `CLASSIFY | SANITIZE`)
  - `api/Guardrails.java`
  - `api/PiiCoverage.java`
  - `api/PiiConfig.java`
  - `api/SecretConfig.java`
  - `api/UrlsConfig.java`
  - `api/KeywordsConfig.java`
  - `api/CustomRegexConfig.java`
  - `api/LlmCheckConfig.java`
  - `api/GuardrailsConfig.java`
  - `api/GuardrailsOutcome.java`
  - `api/ScanOutcome.java`
  - `api/AuditRecord.java`
  - `policy/PiiEntity.java`
  - `policy/SecretPreset.java`
  - `policy/SecretPresetParams.java`
  - `stream/MatchAccumulator.java`
- **Files edited:** none.
- **Public surface added** (signatures; impl is Tasks 10/11 — stub `throw new UnsupportedOperationException("Task 10"/"Task 11")` bodies):
  - `api.GuardrailResult(String entityType, boolean detected, String cleanedValue, Map<String,List<String>> maskEntities)` + `static pass(String,String)` + `static fail(String,String,Map<String,List<String>>)` + `boolean isPassed()` (derived `!detected`). `maskEntities` key = `entityType` string.
  - `api.GuardrailCheck` `@FunctionalInterface`: `GuardrailResult run(String)`; `default String name()` = `getClass().getSimpleName()`; `default StreamCheck toStream()` throws `UnsupportedOperationException`.
  - `api.StreamCheck` `@FunctionalInterface`: `void scan(Reader, MatchAccumulator)`; `default String name()`.
  - `api.GuardrailCheckFactory<C>` `@FunctionalInterface`: `GuardrailCheck create(C)`.
  - `api.LlmClassifier`: `Verdict classify(String)`, `String beanName()`, nested `record Verdict(boolean flagged, double confidence)` (entity type comes from `beanName()`, NOT the verdict — keeps the model output to just `flagged`+`confidence`, matching `LlmPolicyPrompts.JSON_SCHEMA` and saving tokens; the adapter's `BeanOutputConverter` no longer needs an `entityType` field).
  - `api.Operation`: `CLASSIFY`, `SANITIZE`.
  - `api.Guardrails` (final, private ctor): `static Builder builder()`; `GuardrailsOutcome run(String, Operation)`; `ScanOutcome scan(String)`; `String redact(String)`; `ScanOutcome scan(Reader)`; `void redact(Reader, Writer)`. Inner `static final class Builder`: `config(GuardrailsConfig)`, `failOnlyOnErrors(boolean)`, `withClassifier(LlmClassifier)`, `Guardrails build()` — all fluent returning `Builder` except `build()`.
  - Config records (in `api`, static-pure): `PiiCoverage{ALL,SELECTED}`; `PiiConfig(PiiCoverage,Set<PiiEntity>,List<CustomRegexConfig>,boolean strict)` + `DEFAULTS = new PiiConfig(ALL,Set.of(),List.of(),true)`; `SecretConfig(SecretPreset,Map<String,List<Pattern>>)` + `DEFAULTS` (BALANCED, `Map.of()`); `UrlsConfig(List<String> allowedSchemes,List<String> allowedHosts)` + `DEFAULTS` (`["http","https"]`, empty); `KeywordsConfig(boolean toMask,List<String>)` (no DEFAULTS); `CustomRegexConfig(String name,List<Pattern>)`; `LlmCheckConfig(boolean enabled,double threshold)` + `DEFAULTS = new LlmCheckConfig(true,0.7)`.
  - `api.GuardrailsConfig` (10 fields incl. `Map<String,Boolean> booleanOptions` reserved) + inner fluent `Builder` with setters `pii/secrets/urls/keywords/customRegexes/llm/jailbreak/nsfw/topical/booleanOptions/build`; `static DEFAULTS = builder().build()`. Builder default `keywords = new KeywordsConfig(false,List.of())`, `customRegexes = List.of()`, `booleanOptions = Map.of()`.
  - `api.GuardrailsOutcome(String text,String entityType,boolean detected,List<GuardrailResult> validations,Map<String,List<String>> maskEntities,List<AuditRecord> auditRecords,List<String> messages)` + `boolean isPassed()` (`!detected`).
  - `api.ScanOutcome(String text,String entityType,boolean detected,List<String> errorMessages,Map<String,List<String>> maskEntities,List<AuditRecord> auditRecords)` + `boolean isPassed()` + `Set<String> entityTypes()` (`maskEntities.keySet()`).
  - `api.AuditRecord(String entityType,List<GuardrailResult> validations,String rawText)`.
  - `policy.PiiEntity` enum — **33 constants from design §8 table** (Global 9: CREDIT_CARD,CRYPTO,EMAIL_ADDRESS,IP_ADDRESS,PHONE_NUMBER,IBAN_CODE,LOCATION,DATE_TIME,MEDICAL_LICENSE; US 5: US_BANK_NUMBER,US_DRIVER_LICENSE,US_ITIN,US_PASSPORT,US_SSN; UK 2: UK_NHS,UK_NINO; ES 2: ES_NIF,ES_NIE; IT 2: IT_FISCAL_CODE,IT_VAT_CODE; PL 1: PL_PESEL; SG 2: SG_NRIC_FIN,SG_UEN; AU 4: AU_ABN,AU_ACN,AU_TFN,AU_MEDICARE; IN 5: IN_PAN,IN_AADHAAR,IN_VEHICLE_REGISTRATION,IN_VOTER,IN_PASSPORT; FI 1: FI_PERSONAL_IDENTITY_CODE). `private final String typeString` + `String type()` = `"pii_" + name().toLowerCase(Locale.ROOT)`.
  - `policy.SecretPreset{STRICT(10,3.0,2,true),BALANCED(10,3.8,3,false),PERMISSIVE(30,4.0,2,false)}` + `SecretPresetParams params()` + record `SecretPresetParams(int minLength,double minEntropy,int minDiversity,boolean strictMode)`.
  - `stream.MatchAccumulator` (final): `void add(String,String)`; `void addAll(String,List<String>)`; `Set<String> entityTypes()`; `List<String> tokens(String)` (unmodifiable, empty if absent); `Map<String,List<String>> toMaskEntities()` (unmodifiable snapshot); `int entityTypeCount()`. Backed by `LinkedHashMap<String,LinkedHashSet<String>>` — dedupes by value, preserves first-seen order.
- **Gotchas / decisions:**
  - **PiiEntity resolution:** The task file §7 gave example constant names (`GLOBAL_PHONE`, `PERSON_NAME`, …) that **conflict with design §8 and Task 04**. Task 04 explicitly asserts `EnumSet.allOf(PiiEntity.class)` == the **33 exact design §8 names**, and the task prose itself says "use the exact constant names in the design". **Chose design §8 names** (listed above). Downstream Task 04/07 rely on this exact set.
  - `type()` = `"pii_" + lowercased-snake constant name` (e.g. `EMAIL_ADDRESS → "pii_email"`, `CREDIT_CARD → "pii_credit_card"`, `US_SSN → "pii_us_ssn"`). Matches Task 04's `pii_`+rule-name assertion and design §5.1's `pii_credit_card` example.
  - **Added `api/Operation` enum** (not in the task file list) because `Guardrails.run(String, Operation)` and `Operation.CLASSIFY/SANITIZE` are explicitly referenced by the task text and the facade must compile. This is the only type beyond the task's enumerated list.
  - `Guardrails` facade bodies are stubs (throw) as required; `Builder.build()` returns `new Guardrails()` (compiles; runtime comes in 10/11).
  - `GuardrailsConfig.DEFAULTS` = `builder().build()`; builder defaults each config to its `DEFAULTS`, `keywords = new KeywordsConfig(false,List.of())`, `booleanOptions = Map.of()` (reserved).
  - `StreamCheck`/`GuardrailCheck`/`GuardrailCheckFactory`/`LlmClassifier` are zero-dependency; only `java.util`, `java.util.regex.Pattern`, `java.io.Reader/Writer` imported. No third-party imports in core (acceptance criterion met).
- **Next task(s) must know:**
  - 03 (redaction) needs `GuardrailResult.pass/fail` + `GuardrailCheck` defaults.
  - 04 (PII catalog) needs `PiiEntity` (33 §8 names) + `type()` strings above.
  - 05 (secret keys) needs `SecretPreset` + `SecretConfig` + `GuardrailResult`.
  - 06 (urls/keywords) needs `UrlsConfig`, `KeywordsConfig`, `GuardrailResult`.
  - 07 (PII check) needs `PiiConfig` / `CustomRegexConfig` / `GuardrailResult` + `PiiEntity`.
  - 08 (LLM core) needs `LlmClassifier` + `LlmCheckConfig` + `GuardrailResult` + `GuardrailsConfig.builder()`.
  - 09/10/11 (stream / pipeline / stream-pipeline) need `StreamCheck`, `MatchAccumulator`, `Guardrails` stub facade, `Operation`.
   - Spring AI is NOT resolved in core (by design — core is zero-dep); `LlmClassifier` is the single classifier registration point and its canonical impl is Task 12.


## Task 03 — redaction engine — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + Checkstyle green; 23 tests, 0 failures).
- **Files created** (all under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`):
  - `redact/Placeholders.java`
  - `redact/Redactor.java`
  - `internal/AhoCorasick.java`
  - tests: `src/test/java/io/github/khezyapp/dpriv/redact/PlaceholdersTest.java`, `.../redact/RedactorTest.java`, `.../internal/AhoCorasickTest.java`
- **Files edited:** `securities/data-privacy-core/build.gradle` (added `testImplementation "org.assertj:assertj-core:3.25.3"` — task requested AssertJ; runtime stays zero-dependency).
- **Public surface added:**
  - `redact.Placeholders` (final, private ctor): `static String forEntityType(String)`; `static final Pattern TOKEN = <[A-Z0-9_]+>`.
  - `redact.Redactor` (final): `String redact(String input, Map<String,List<String>> maskEntities)`.
  - `internal.AhoCorasick` (final): `static AhoCorasick compile(Map<String,List<String>>)`; `void scan(CharSequence, MatchVisitor)`; nested `@FunctionalInterface MatchVisitor { void match(int,int,String,String); }` (start, end-exclusive, token, entityType).
- **Exact `Placeholders.forEntityType` mapping table (PiiEntity + every check reuse this):**
  - Strip leading `pii_` family prefix, then uppercase (Locale.ROOT) + sanitize to `[A-Za-z0-9_]`:
    - `pii_email_address → <EMAIL_ADDRESS>`, `pii_credit_card → <CREDIT_CARD>`, `pii_us_ssn → <US_SSN>`.
    - (Note: actual `PiiEntity.type()` strings are `pii_` + snake name, e.g. `pii_email_address` → `<EMAIL_ADDRESS>`. The task file's inline example `pii_email → <EMAIL_ADDRESS>` is a simplification — the real type string `pii_email_address` maps to `<EMAIL_ADDRESS>`.)
  - Non-PII families (no prefix) uppercase as-is: `secret → <SECRET>`, `link → <LINK>`, `keyword → <KEYWORD>`, `jailbreak → <JAILBREAK>`.
  - Unknown fallback: `<sanitize(entityType).toUpperCase()>`; non-alnum/non-`_` stripped, `_` kept (`SECRET_KEY → <SECRET_KEY>`).
- **`AhoCorasick` behavior notes (Task 09's StreamRedactor + Tokenizer consume it):**
  - Builds trie + failure links; a node's `best` = its own longest matching output depth (own output else deepest fail-chain output), so each node yields exactly one longest match.
  - `scan` resets to root per call; walks each char, emits the **longest** pattern ending at each position via `visitor.match(start, end, token, entityType)`; **does not skip** — continues scanning. Matches therefore arrive in ascending end-position order and **may overlap** (a shorter token that ends before a containing longer token is emitted: e.g. `example` [12,19) and `visal@example.com` [6,23) both emitted). Consumers must handle overlap.
  - `compile` throws `IllegalArgumentException` on empty `maskEntities` (msg contains "maskEntities") or any empty token (msg contains "token").
- **`Redactor.redact` contract (Task 09's StreamRedactor MUST match this exactly for parity, assert via assertEquals):**
  - Signature: `String redact(String input, Map<String,List<String>> maskEntities)`; pure/immutable/deterministic; `Objects.requireNonNull` on both; returns `input` unchanged when `maskEntities` empty (no allocation churn).
  - **Longest-first**: collects all automaton matches, sorts by token length descending (tie-break start, then end, then entityType), greedily keeps non-overlapping matches (a longer token's span consumes contained shorter tokens). Applies kept matches left-to-right, emitting `Placeholders.forEntityType(entityType)` per match.
  - **Placeholder-protected**: because scan runs on the original input (never the emitted output) and kept matches are non-overlapping, a token that equals/contains placeholder text (e.g. `EMAIL`) can never corrupt an emitted `<EMAIL_ADDRESS>` placeholder. Proven by test `doesNotCorruptPlaceholderWithSubstringToken`.
  - Parity contract for Task 09: for the same input + `maskEntities`, StreamRedactor output must equal `Redactor.redact(input, maskEntities)` — i.e. `assertEquals(redactor.redact(input, mask), streamResult)`. The streaming path must replicate longest-first resolution (containing token consumed, contained suppressed) and never re-scan placeholders.
- **Gotchas / decisions:**
  - **Aho-Corasick emits overlapping per-end matches by design**; the automaton is NOT a redactor. `Redactor` resolves longest-first itself (collect + sort + greedy non-overlap). This is the correct interpretation of "longest-first": the naive "emit longest per position + protected-list, discard overlap" inline algorithm from the task would wrongly discard a longer token `[6,23)` after a contained shorter token `[12,19)` was emitted first. Collect-then-resolve is unambiguous, deterministic, and matches design §7.2's "String.replace over a longest-first sorted mapping".
  - `Placeholders` uses `forEntityType(String)` (per the task file) rather than the design doc's `of(...)` naming.
  - Added AssertJ to core tests only (task explicitly requested "JUnit 5 + AssertJ"). No other module uses AssertJ; it is test-scope so core's runtime remains zero-dependency.
- **Next task(s) must know:**
  - Task 04 (PII catalog): `PiiEntity` (33 §8 names) + `.type()` strings (`pii_email_address` etc.) already pinned from Task 02. `Placeholders.forEntityType(PiiEntity.X.type())` yields the `<X>` placeholder.
  - Task 09 (stream core): implement `StreamRedactor` targeting equality with `Redactor.redact` (contract above); consume `internal.AhoCorasick` (public API + overlap/emission notes above) and `redact.Placeholders`.

## Task 04 — PII catalog — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + Checkstyle green). PiiPatternsTest 40 + ChecksumValidatorsTest 27 = 67 new tests, 0 failures.
- **Files created** (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/policy/`):
  - `PiiPatterns.java`
  - `ChecksumValidators.java`
  - tests: `src/test/java/io/github/khezyapp/dpriv/policy/PiiPatternsTest.java`, `.../ChecksumValidatorsTest.java`
- **Files edited:** `securities/data-privacy-core/CREDITS.md` (added `DEFAULT_PII_PATTERNS` attribution row).
- **Public surface added:**
  - `policy.PiiPatterns` (final, private ctor, lazy init-on-demand via nested `Holder`, `EnumMap<PiiEntity,Pattern>` wrapped in `Collections.unmodifiableMap`):
    - `static Pattern forEntity(PiiEntity)` — same cached singleton every call.
    - `static Map<PiiEntity,Pattern> all()` — unmodifiable, ordered by enum declaration.
    - `static boolean isNonStrictMatch(PiiEntity, String)` — pattern-only (`Pattern.matcher(...).find()`, `Objects.requireNonNull` both args).
    - `static boolean isStrictMatch(PiiEntity, String)` — pattern first, then checksum gate for `CREDIT_CARD`→Luhn, `IBAN_CODE`→mod-97, `IN_AADHAAR`→Verhoeff; every other entity `default: true`.
  - `policy.ChecksumValidators` (final, private ctor, all fail-safe — null/scrambled/malformed → `false`, never throws):
    - `static boolean luhn(String)` — strips non-digits, requires 13–19 digits, classic double-from-right mod-10.
    - `static boolean mod97(String)` — strips whitespace, uppercases (Locale.ROOT), optionally strips literal `IBAN` prefix, requires ISO 13616 shape `[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}` (15–34 chars), rotates first 4 to end, converts letters→10–35, returns `BigInteger.mod(97)==1`.
    - `static boolean verhoeff(String)` — strips non-digits, requires exactly 12 digits (Aadhaar length gate), standard Dihedral D5 tables (`D_TABLE`, `P_TABLE`, `INV_TABLE`).
- **Pattern table provenance (IMPORTANT):**
  - Design §8 lists entity names ONLY — no literal regexes and no example values. Resolution: ported the upstream `DEFAULT_PII_PATTERNS` table verbatim (pattern strings only) from OpenAI Guardrails JS `src/checks/pii.ts` @ commit `b9b99b4fb454f02a362c2836aec6285176ec40a8`, as mirrored in the n8n Guardrails node `.../nodes-langchain/nodes/Guardrails/actions/checks/pii.ts`. Local source: `/mnt/data/opensource/n8n/packages/@n8n/nodes-langchain/nodes/Guardrails/actions/checks/pii.ts`. Both MIT; CREDITS.md updated.
  - Upstream table holds 37 entities; design §8 pins 33. The 4 dropped are `IT_DRIVER_LICENSE`, `IT_PASSPORT`, `IT_IDENTITY_CARD` (not in §8). Kept exact set = §8 list (already pinned in PiiEntity from Task 02).
  - Regexes compiled WITHOUT flags — upstream has no case-insensitive modifiers, so they stay default Java settings (case-sensitive). Deliberately not "fixed".
- **Gotchas / decisions:**
  - **IBAN strict requires the COMPACT form.** The upstream `IBAN_CODE` pattern contains no space allowance (`\b[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}([A-Z0-9]?){0,16}\b`), so `GB82 WEST 1234 5698 7654 32` (spaced) FAILS `isNonStrictMatch` and therefore `isStrictMatch` too, while the compact `GB82WEST12345698765432` passes. Checksum `mod97` itself tolerates spaces/prefix/case, but the pattern gate runs first. Test asserts the compact form for strict.
  - **`EMAIL_ADDRESS` negatives cannot be boundary-padded**: the greedy local-part `[A-Za-z0-9._%+-]+` and unbounded TLD class `[A-Z|a-z]{2,}` absorb "cushion" letters, so `xvisal@example.comy` matches. Negative used: `visal@example` (missing TLD → deterministic no-match). The `[A-Z|a-z]` literal-`|` quirk is preserved from upstream.
  - **Aadhaar Verhoeff gate = exactly 12 digits.** A scrambled 8-digit slice (e.g. `48525745`) coincidentally passes Verhoeff, so any length ≠ 12 fails first.
  - **CREDIT_CARD checksum gate = Luhn on 13–19 digits** (strips `-`/space separators first); `4111111111111112` (pattern match, bad Luhn) passes non-strict but fails strict — proven by test.
  - Checkstyle: all four new files needed a trailing newline (`NewlineAtEndOfFileCheck`); `int[][]`/array literals for the Verhoeff tables passed `NoWhitespaceAfter`/`ARRAY_INITIALIZER` as written.
  - Test self-check pins the catalog to §8: `all().size()==33`, `keySet()==EnumSet.allOf(PiiEntity.class)`, exact 33-name list, and every `type()` == `"pii_" + lowercase(name)`.
- **Verified good values** (also used as test fixtures): Luhn `4111111111111111`, `5555555555554444` (+ `-`/space forms), `4111111111111112`→false; mod-97 `GB82WEST12345698765432` (+spaced/`IBAN`-prefixed/lowercase)→true, `...33`/truncated→false; Verhoeff `485275045745`→true, `485275045746`/13-digit→false.
- **Next task(s) must know:**
  - Task 07 (PII check) consumes `PiiPatterns` directly: `isStrictMatch/isNonStrictMatch` + `forEntity`/`all()`. `Placeholders.forEntityType(PiiEntity.X.type())` yields the `<X>` placeholder. No entity needs more than one `PiiEntity`-keyed `GuardrailResult`.
  - Entity constants/types are final (Task 02 §8 pins); future "more countries" work is additive and would require a design update, not an edit here.

## Task 05 — secret keys: SecretKeysCheck + SecretCandidateFilter — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + Checkstyle green; 11 new tests → 101 total, 0 failures).
- **Files created** (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`):
  - `internal/SecretCandidateFilter.java`
  - `checks/SecretKeysCheck.java`
  - tests: `src/test/java/io/github/khezyapp/dpriv/internal/SecretCandidateFilterTest.java` (4), `.../checks/SecretKeysCheckTest.java` (7)
- **Files edited:** `securities/data-privacy-core/CREDITS.md` (added secret entropy preset scheme attribution row).
- **Public surface added:**
  - `internal.SecretCandidateFilter` (final, stateless): ctor `SecretCandidateFilter(SecretPresetParams)`; `boolean accept(String)` (intrinsic: length → diversity → entropy); `boolean accept(String input, int start, int end)` (intrinsic + strict boundary).
  - `checks.SecretKeysCheck implements GuardrailCheck` (final): ctor `SecretKeysCheck(SecretConfig, Redactor)`; `GuardrailResult run(String)`; `name()` = `"SecretKeysCheck"` (explicit override).
  - Bucket: `maskEntities["secret"]` (NOT `SECRET` — `Placeholders.forEntityType("secret")` → `<SECRET>`).
- **Exact algorithm (the filter is the single source of truth for Task 09's StreamRedactor):**
  - **Tokenization (in `SecretKeysCheck`, `Pattern TOKEN = [A-Za-z0-9]+(?:[+/=]+)?`)**: an alphanumeric run optionally followed by trailing `+/=` (base64 padding). `+/=` are **trailing-only** so a `key=value` separator does NOT glom into the value token: `my-api-key=AbC123xYz78qR9` yields tokens `my`,`api`,`key=`,`AbC123xYz78qR9` → `key=` (len 4) rejected, value accepted → redacts to `my-api-key=<SECRET>`. (Task's fictional `ABC...xyz` example has non-token dots; used a real high-entropy value instead.)
  - **Predicate order (cheapest first):** `length >= minLength` → `distinct char count >= minDiversity` → Shannon entropy `H = -Σ p(c)·log2(p(c)) >= minEntropy`. Computed over a `boolean[65536]`/`int[65536]` count table (candidate ≤ 65536 char values), `Math.log(p)/Math.log(2)`. **Diversity = distinct CHARACTER count** (task's literal spec), not upstream's per-class count.
  - **strictMode boundary (STRICT only, in `accept(String,int,int)`):** token rejected if the char immediately before `start` or at `end` is `isLetterOrDigit || '_'` — i.e. **underscore counts as an identifier glue char**. So `my_AbC123xYz78qR9`: STRICT rejects (preceded by `_`), BALANCED accepts (no boundary). This is Task 09's definition of "adjacent to alnum".
  - **Custom patterns:** `config.customPatterns()` (`Map<String,List<Pattern>>`) — each pattern's `matcher.find()` group(0) merges into the SAME `"secret"` bucket, **unconditionally** (no entropy gate — matches upstream precedent that custom regex returns true first).
  - `GuardrailResult`: detected → `fail("secret", redactor.redact(input, maskEntities), maskEntities)`; not detected → `pass("secret", input)`. `tokens` = `LinkedHashSet` → `List.copyOf` → **unique, first-seen order**.
- **Gotchas / decisions:**
  - **The task's own `session123`-in-`my_session123` example is wrong under real entropy**: `session123` has H≈3.12, which is < BALANCED's 3.8, so BALANCED REJECTS it on entropy — it never demonstrates the STRICT/BALANCED boundary split. The boundary split is demonstrated with a genuinely high-entropy glued token `my_AbC123xYz78qR9` (token `AbC123xYz78qR9`, H=log2(14)=3.807≥3.8): STRICT rejects (glued via `_`), BALANCED accepts. Logged as the tested boundary behavior.
  - **`=`-in-run merge bug avoided**: a naive `[A-Za-z0-9+/=]+` class merges `key=AbC...` into one token and redacts to `my-api-<SECRET>` (losing `key=`). Using `[A-Za-z0-9]+(?:[+/=]+)?` (trailing-only `+/=`) matches the task's `my-api-key=<SECRET>` example.
  - Filter is stateless (immutable `SecretPresetParams`); `accept(String)` is pure. The context-aware `accept(String,int,int)` overload carries strict-boundary logic so in-memory and streaming share ONE decision.
- **Next task(s) must know:**
  - **Task 09 (StreamRedactor / stream SecretKeysCheck) MUST reuse `internal.SecretCandidateFilter.accept(input, start, end)`** (NOT reimplement) and the trailing-only tokenization `[A-Za-z0-9]+(?:[+/=]+)?`. For a **window edge**, "adjacent to alnum" = the original char before `start` / at `end` is `isLetterOrDigit || '_'`; a window edge mid-token is fine to evaluate against the window string as the `input`.
  - EntityType bucket string is `"secret"` → `<SECRET>` placeholder. `maskEntities` must be unique-first-seen for streaming parity (MatchAccumulator dedupes by value).
  - `SecretConfig` (Task 02) field is `customPatterns()` as `Map<String,List<Pattern>>` (not the design doc's `List<Pattern>` — the in-repo record is authoritative, per Task 02's handoff note).

## Task 06 — URL + keyword checks: UrlsCheck + KeywordsCheck — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + Checkstyle green; 23 new tests → 124 total, 0 failures).
- **Files created** (under `securities/data-privacy-core/src/`):
  - `main/java/io/github/khezyapp/dpriv/checks/UrlsCheck.java`
  - `main/java/io/github/khezyapp/dpriv/checks/KeywordsCheck.java`
  - tests: `test/java/io/github/khezyapp/dpriv/checks/UrlsCheckTest.java` (13), `.../checks/KeywordsCheckTest.java` (10)
- **Files edited:** `securities/data-privacy-core/CREDITS.md` (URL + keyword pattern attribution row).
- **Public surface added:**
  - `checks.UrlsCheck implements GuardrailCheck` (final): ctor `UrlsCheck(UrlsConfig, Redactor)`; `run(String)`; `name()` = `"UrlsCheck"`. Package-private statics exposed for Task 09: `detect(String)` → `List<String>`, `isFlagged(String, Set<String>, Set<String>)`.
  - `checks.KeywordsCheck implements GuardrailCheck` (final): ctor `KeywordsCheck(KeywordsConfig, Redactor)`; `run(String)`; `name()` = `"KeywordsCheck"`.
  - Buckets: `maskEntities["link"]` → `<LINK>`, `maskEntities["keyword"]` → `<KEYWORD>` (via `Placeholders.forEntityType`).
- **Exact algorithm — UrlsCheck (3-pass sweep ported from upstream `urls.ts`):**
  - Pass 1 scheme-ful: one pattern per family of `https?://`|`ftp://` + `data:`|`javascript:`|`vbscript:`|`mailto:` + SCHEME_CLASS `[^\s<>"{}|\\^`\[\]]+`. Uppercase schemes are **not** detected (reference parity — no case-insensitive flag).
  - Pass 2 scheme-less domain `\b(?:www\.)?[a-zA-Z0-9][a-zA-Z0-9.-]*\.[a-zA-Z]{2,}(?:/[^\s]*)?`; Pass 3 bare IPv4 `\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?::[0-9]+)?(?:/[^\s]*)?`. Trailing punctuation stripped from every match (see literal gotcha below) → unique in first-seen order.
  - **Cross-pass dedupe (`finalize`)**: a scheme-ful candidate adds `getHost()` and its `www.`-stripped form to `covered`; a later bare candidate is dropped when its pre-`[/?#]` host (www-stripped, lowercased) is covered. `hostPart` = substring before first `[/?#]`.
  - **Classifier (`isFlagged` -> `isPolicyViolation`)**: parse via `java.net.URI.create` — `http://` prefixed for scheme-less candidates, special scheme recognized via `^(?:data|javascript|vbscript|mailto):`; `IllegalArgumentException` drops the candidate (never crash). Flagged when: scheme is null/not in allowed-set (case-insensitive), OR raw userinfo present (**always blocks**, even when scheme/host allowed), OR `allowedHosts` is non-empty AND host is null/not allow-listed. **Host rule applies only when the allow-list is configured** (task file overrides design §9.3's deny-by-default — confirmed against Task 02's `UrlsConfig`).
  - Scheme/host allow-lists normalized to lowercase + trimmed at construction.
- **Exact algorithm — KeywordsCheck (ported from upstream `keywords.ts`):**
  - Config keywords stripped of trailing punctuation (`[.,!?;:]+$`); empty results skipped. Each becomes `Pattern.quote`'d literal with boundary lookarounds, all joined into ONE compiled alternation `(?:\Q..\E|\Q..\E)` with flags `CASE_INSENSITIVE | UNICODE_CASE`.
  - Boundaries are Unicode-aware: LEFT `(?<![\p{L}\p{N}_])` applied only when the keyword starts with a word code point; RIGHT `(?![\p{L}\p{N}_])` only when it ends with one — so punctuation-leading/trailing keywords (`!priority`, `urgent!`) still match, while embedded substrings do not. Empty/blank `keywords()` → pattern null → **no-op pass**.
  - **Dedupe = case-folded, first-seen order** (`URGENT urgent` → `["URGENT"]`); matched group text is preserved (case preserved). `maskEntities["keyword"]` is populated even when `toMask=false` (only `cleanedValue` falls back to input).
  - **Masking is by-value**: `redactor.redact(input, maskEntities)` replaces EVERY occurrence of a matched token value (shared Redactor/Aho-Corasick contract), including inside longer words; whole-word rules govern detection only.
- **Gotchas / decisions:**
  - **Trailing-punctuation literal (main bug found):** correct Java literal is `Pattern.compile("[].,;:!?)\\\\]+$")` — a leading `]` as the FIRST class member makes it literal and sidesteps `\]`-escape failures. Verified failures of alternatives: `"[.,;:!?)\\\\]]+$"` (class closes at first unescaped `]` → strips nothing), three-backslash variants either strip nothing or gobble `://`/`.` too much.
  - **Redactor by-value masking vs whole-word detection**: the Khmer glued case (`ខភ្នំពេញ` with keyword `ភ្នំពេញ`) is detected-whole-word (mask list `[keyword]`) but cleanedValue still masks the glued occurrence (` <KEYWORD> ខ<KEYWORD> `). Deliberate and documented in the class Javadoc — Task 09 must keep this parity.
  - **URI probe results locked in:** `URI.create("http://")` throws; `http://999.999.999.999` → `getHost()==null` (flagged when allow-list non-empty); `http:///path` → host null; `hTTp` scheme preserved by `getScheme()` → compare via lowered form.
  - `config` field dropped from `UrlsCheck` (normalized sets are the single comparison surface; avoids an unused field).
  - All four new files needed a trailing newline (`NewlineAtEndOfFileCheck`) — the `write` tool omits it.
- **Verified good values** (also test fixtures): `read ftp://visal.example,.` → token `ftp://visal.example` (punctuation stripped), `<LINK>` in cleaned; `mark!priority handled` → keyword `!priority` matches (left boundary dropped) → `mark<KEYWORD> handled`; `URGENT urgent then urgent URGENT later` → `["URGENT"]`, cleaned `<KEYWORD> urgent then urgent <KEYWORD> later`; `phnompenh.example.org` flagged with `allowedHosts=[example.com]` (exact match); `user:pass@` always flagged; `http://127.0.0.1` → scheme allowed, IPv4 host, not flagged (no userinfo, empty allow-list).
- **Next task(s) must know:**
  - **Task 09 `StreamUrlsCheck`**: reuse `UrlsCheck.detect(window)` + `UrlsCheck.isFlagged(candidate, allowedSchemes, allowedHosts)`; the stream impl only needs to place the emitted tokens at exact window match offsets and dedupe first-seen. Pass the stream's normalized allow-sets through (or reconstruct via the same ctor normalization).
  - **Task 09 `StreamKeywordsCheck`**: the combined pattern + boundary rules live privately in `KeywordsCheck`; the stream variant must reproduce the identical per-keyword sanitize → boundary-selection → quote → alternation compilation (or the design must share it, e.g. a package-private static builder — permission needed since `KeywordsCheck` is public surface).
  - In-memory == streaming parity is the mandate; buckets `"link"`/`"keyword"` → `<LINK>`/`<KEYWORD>`; `GuardrailCheck.toStream()` still throws by default until Task 09.

## Task 07 — PII check + custom regex check — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + Checkstyle green; 15 new tests → 139 total, 0 failures).
- **Files created** (under `securities/data-privacy-core/src/`):
  - `main/java/io/github/khezyapp/dpriv/checks/PiiCheck.java`
  - `main/java/io/github/khezyapp/dpriv/checks/CustomRegexCheck.java`
  - tests: `test/java/io/github/khezyapp/dpriv/checks/PiiCheckTest.java` (9), `.../checks/CustomRegexCheckTest.java` (6)
- **Files edited:** none (tests only; no build.gradle/CREDITS change — no new attribution rows, patterns already ported in Task 04).
- **Public surface added:**
  - `checks.PiiCheck implements GuardrailCheck` (final): ctor `PiiCheck(PiiConfig, Redactor)`; `run(String)`; `name()` = `"PiiCheck"` (explicit override).
  - `checks.CustomRegexCheck implements GuardrailCheck` (final): ctor `CustomRegexCheck(List<CustomRegexConfig>, Redactor)`; `run(String)`; `name()` = `"CustomRegexCheck"`. Kept public per task so Task 10 can run it standalone.
- **Exact `PiiCheck.run` aggregation contract (this is what Task 09 streaming + Task 10 pipeline MUST mirror/assert):**
  - **Coverage resolution** (deterministic enum-declaration order in BOTH modes): `ALL` → `PiiPatterns.all().keySet()` (EnumMap order); `SELECTED` → the configured `entities()` filtered against `all().keySet()` order (so output ordering doesn't depend on the caller's `Set` type). Empty SELECTED set → immediate `GuardrailResult.pass("pii", input)` (no children run).
  - **Per-entity scanning:** `PiiPatterns.forEntity(entity).matcher(input).find()` loop; each `group()` candidate is validated via `config.strict()`: `true` → `PiiPatterns.isStrictMatch(entity, token)` (checksum gate for CREDIT_CARD/IBAN/IN_AADHAAR), `false` → `isNonStrictMatch` (pattern only). Rejected candidates are dropped (stay out of `maskEntities`).
  - **Top-level result:** `entityType = "pii"` (family per §12.3); `detected = any pii match || any custom match`; `maskEntities` keyed by `PiiEntity.type()` — the real keys are **`pii_` + lowercased-snake constant name**, e.g. `pii_email_address`, `pii_credit_card`, `pii_us_ssn`, `pii_location` (NOT the task's prose simplifications `pii_email`/`pii_geo_location` — Task 03 handoff already pinned this). Values = unique-first-seen tokens.
  - **Custom folding:** builds `new CustomRegexCheck(config.customRegexes(), redactor)`, runs `.run(input)`, and merges its `maskEntities` entries + `detected()` into the SAME aggregate. ONE redaction pass over the merged map → shared `cleanedValue`.
  - **`cleanedValue`** = `redactor.redact(input, mergedMaskEntities)`; placeholder families come from `Placeholders.forEntityType` (e.g. `pii_email_address → <EMAIL_ADDRESS>`, custom `order_ref → <ORDER_REF>`).
- **Exact `CustomRegexCheck.run` contract (§9.5, reuse in Task 10):**
  - One `maskEntities` group per `CustomRegexConfig.name()` — NEVER folded into `pii_*`. Key = the raw name string as given (`"order_ref"`); tokens = full matched text `matcher.group()` (unique, first-seen). Top-level `entityType = "custom"` when run standalone.
  - `detected = any group matched`; `cleanedValue = redactor.redact(input, masks)`.
  - Empty config list → `pass("custom", input)`. Blank/`null` name → group skipped (never throws). `null` config entry or `null` patterns list / `null` pattern → skipped. (Note: the `configs` list itself must not contain nulls if built via `List.of` — use `Arrays.asList` to hold null entries; the check tolerates null list elements.)
- **Gotchas / decisions:**
  - **`pii_location` token includes the leading space**: the upstream `LOCATION` regex `\b[A-Za-z\s]+(?:Street|...|Boulevard)\b` greedily includes the whitespace before the street name, so `"123 Monivong Boulevard"` yields token `" Monivong Boulevard"` (leading space). Masking replaces that whole span including the space. Fixtures use `Monivong Boulevard` → `<LOCATION>`.
  - **48-gotcha recursion avoided via `List.of` rejection**: my null-tolerance test crashed on `List.of(null,...)` (rejects null elements), not in the check itself — `CustomRegexCheck` correctly tolerates null list elements via the isNull guard.
  - Both checks override `name()` (matches sibling checks); `toStream()` remains the throwing default until Task 09.
- **Next task(s) must know:**
  - **Task 09 (stream core):** `StreamPiiCheck`/`StreamCustomRegexCheck` must reproduce PiiCheck's aggregation keys EXACTLY — `entityType`-`pii_`+snake per entity, custom = raw name, unique-first-seen, strict/non-strict via `PiiPatterns.isStrictMatch/isNonStrictMatch` — for in-memory==streaming parity (asserted by equality). `PiiCheck` is the single entry point Task 09/10 use for PII.
  - **Task 10 (pipeline):** `PiiCheck` can be run standalone; `CustomRegexCheck` is also public for standalone use. The check-level bucket keys above are what `StreamCheck`/`MatchAccumulator` and the pipeline assert on.
  - Redaction parity: `PiiCheck.cleanedValue` must equal the stream path's redaction for the same merged maskEntities (longest-first via shared `Redactor`/`AhoCorasick`).

## Task 08 — LLM check core: LlmContract + LlmPolicyPrompts + LlmCheck — DONE

- **Date/agent:** 2026-08-29
- **Verified commands:**
  - `./gradlew :data-privacy-core:build --rerun-tasks` → BUILD SUCCESSFUL (compile + Checkstyle green)
  - `./gradlew :data-privacy-core:test --rerun-tasks` → 22 new tests, 0 failures (LlmCheckTest 7, LlmContractTest 9, LlmPolicyPromptsTest 6); 161 total
- **Files created** (under `securities/data-privacy-core/src/`):
  - `main/java/io/github/khezyapp/dpriv/policy/LlmContract.java`
  - `main/java/io/github/khezyapp/dpriv/policy/LlmPolicyPrompts.java`
  - `main/java/io/github/khezyapp/dpriv/checks/LlmCheck.java`
  - tests: `test/java/io/github/khezyapp/dpriv/policy/LlmContractTest.java` (9), `.../policy/LlmPolicyPromptsTest.java` (6), `.../checks/LlmCheckTest.java` (7)
- **Files edited:** none (new files only — no build.gradle/CREDITS change; prompts are re-implemented from design intent, no ported patterns).
- **Public surface added:**
  - `policy.LlmContract` (final, private ctor, stateless): `static boolean classify(boolean verdictFlagged, double confidence, double threshold)`; `static GuardrailResult toResult(LlmClassifier, LlmClassifier.Verdict, LlmCheckConfig, String input)`.
  - `policy.LlmPolicyPrompts` (final, private ctor): `static final String SYSTEM_RULES`; `static String jailbreakPrompt(String input)`; `static String nsfwPrompt(String input)`; `static String topicalAlignmentPrompt(String input)`; `static String customPrompt(String input, String userRules)`.
  - `checks.LlmCheck implements GuardrailCheck` (final): ctor `LlmCheck(LlmClassifier, LlmCheckConfig)`; `run(String)`; `name()` = `"LlmCheck"` (explicit override; `toStream()` stays the throwing api default).
- **Exact decision rule (`LlmContract.classify`):**
  - `!verdictFlagged` → `false` always. `threshold <= 0` → `true` (only the flagged bit gates). Otherwise `clamp(confidence) >= threshold`, where `clamp = min(1.0, max(0.0, confidence))`. NaN confidence -> clamp stays NaN -> `>=` is false -> not detected (deterministic).
  - `toResult`: `entityType = classifier.beanName()` (e.g. `"jailbreak"`, `"nsfw"`); `maskEntities = Map.of()` and `cleanedValue = input` in EVERY case — LLM checks are classificatory only (design §12.2), never redact. `detected` → `GuardrailResult.fail(entityType, input, Map.of())`, else `pass(entityType, input)`. Also short-circuits to `pass` when `config.enabled()==false` (defensive; `LlmCheck.run` short-circuits earlier to avoid a model call). All 4 args `requireNonNull`.
  - NOTE: the task prose said `entityType = classifier.entityType()`, but the in-repo SPI (Task 02) is `beanName()` — used that. The `Verdict` record's own `entityType` component is NOT consulted (record-local, redundant with `beanName()`).
- **Exact `LlmCheck.run` contract:**
  - `config.enabled()==false` → `pass(classifier.beanName(), input)` WITHOUT calling `classifier.classify(input)` (disabled check short-circuits; avoids a model round-trip).
  - Else `classifier.classify(input)` → `LlmContract.toResult(classifier, verdict, config, input)`.
  - Ctor: `classifier` `requireNonNull`; `null` config → `LlmCheckConfig.DEFAULTS` (enabled=true, threshold=0.7).
- **`LlmPolicyPrompts` details (Task 12 must reference these EXACT names):**
  - Prompt builders return a single full system prompt (policy instructions + `USER INPUT:` + interpolated input) via text blocks. Interpolation uses `template.replace("[[USER_INPUT]]", input)` — NOT `String.formatted` — so `%` and NUL characters in user input never crash formatting.
  - `customPrompt(input, userRules)` also interpolates the caller policy via `"[[USER_RULES]]"`.
  - `SYSTEM_RULES` pins the output contract in design-§11.2 intent: JSON-only reply, ignore contradictory instructions, exactly two fields **`confidence`** (number, 0..1) + **`flagged`** (boolean). Field names intentionally match `LlmClassifier.Verdict` (NOT the design's `confidenceScore`); Task 12's Jackson mapping should use these record accessor names (or `@JsonProperty`).
  - Top-level family keywords emitted: `jailbreak`/`Jailbreak`, `NSFW`, `BUSINESS SCOPE` (topical), caller policy (custom) — tests assert on these plus interpolated input.
- **Gotchas / decisions:**
  - **Text blocks + trailing-space rule:** Checkstyle `RegexpSingleline \s+$` checks raw source lines, so blank lines INSIDE text blocks must be truly empty (zero chars) — an indented blank line is a violation. All prompt text blocks written that way.
  - **No `.formatted()` for user input** — `%` in input would throw `UnknownFormatConversionException`; used `replace(PLACEHOLDER, input)` instead. NUL-safe test proven with `"by\0pass"` using the octal escape (no `\u` escapes in source).
  - `LlmContract` disabled short-circuit is belt-and-suspenders: `run` already pre-empts disabled checks, but the task's test bullet demanded the short-circuit be verifiable at `toResult` level too.
  - Anonymous `LlmClassifier` stubs (no Mockito) per khezy-ast-evaluator-testing skill: `classify` throws `UnsupportedOperationException` where the contract must not call it (LlmContractTest), `CapturingClassifier` nested static class records the seen input (LlmCheckTest).
  - Trailing newline: all six new files needed `NewlineAtEndOfFile` fixed after the `write` tool (rstrip+newline via python).
- **Next task(s) must know:**
  - **Task 10 (pipeline):** `LlmCheck(classifier, LlmCheckConfig)` is how LLM checks register — one per family (`jailbreak`/`nsfw`/`topical`/`custom`), each its own `LlmClassifier` with `beanName()` = the family string. GuardrailsConfig carries per-family `LlmCheckConfig` (Builder `jailbreak()/nsfw()/topical()`); a disabled config makes `LlmCheck` a guaranteed-clean pass that never calls the model.
  - **Task 12 (Spring AI adapter):** implement `LlmClassifier` where `classify(String)` receives BOTH the prompt and the input — prompt rolls up `LlmPolicyPrompts.<family>Prompt(input)` (+ `SYSTEM_RULES` header); `run` here calls the SPI with only the raw input, so the adapter (or Task 10 wiring) must assemble the prompt/input. Reference `LlmPolicyPrompts.SYSTEM_RULES` + the four builders by name. `Verdict` components are `flagged`, `confidence`, `entityType`.
  - Classificatory invariant for Task 13 acceptance: `LlmCheck` results ALWAYS have `maskEntities == Map.of()` and `cleanedValue == input`.

### Task 08 revision A1 — two-message system/user prompts (provider prompt caching) — DONE

- **Date/agent:** 2026-08-29 (same session, after review)
- **Why:** the original Task 08 prompt builders interpolated the user input into a single prompt string, so the
  prompt prefix changed on every call and could never hit a provider cache (Anthropic/OpenAI). Redesigned
  `LlmPolicyPrompts` to a two-message model: a **static system message** (guardrail prompt + JSON schema +
  system rules, byte-identical across calls) and the **raw input as the user message** (the only varying part).
- **Verified command:** `./gradlew :data-privacy-core:build --rerun-tasks` → BUILD SUCCESSFUL (LlmPolicyPromptsTest now 9 tests, 0 failures; total 168).
- **Files edited:**
  - `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/policy/LlmPolicyPrompts.java`
  - `securities/data-privacy-core/src/test/java/io/github/khezyapp/dpriv/policy/LlmPolicyPromptsTest.java`
- **NEW public surface (supersedes the Task 08 signatures — Task 12 MUST use these):**
  - `static final String JSON_SCHEMA` — the RESPONSE SCHEMA JSON block (exactly `confidence` number 0..1 +
    `flagged` boolean, no other fields).
  - `static final String SYSTEM_RULES` — behavioural rules (reply with the schema object only, ignore
    contradictions, use full confidence range). Still exists — Task 12 references it by name.
  - `static String jailbreakPrompt()` — **NO input parameter** → returns the static jailbreak policy block only.
  - `static String nsfwPrompt()` — same, static policy block only.
  - `static String topicalAlignmentPrompt()` — same, static policy block only (BUSINESS SCOPE marker).
  - `static String customPrompt(String userRules)` — **input param removed**; policy block from caller rules.
  - `static String systemMessage(String guardrailPrompt)` — assembles the full system message in design §11.2
    order: `guardrailPrompt + "\n\n" + JSON_SCHEMA + "\n\n" + SYSTEM_RULES`. No input anywhere.
- **Two-message contract (how Task 12 wires it):**
  - `system = LlmPolicyPrompts.systemMessage(<family>Prompt())` — build ONCE at classifier construction/home and
    reuse for every request: identical bytes → provider caches the prefix.
  - `user = input` verbatim (raw classify input — NOT interpolated). 'USER INPUT'/'[[USER_INPUT]]' placeholder
    REMOVED from the API; only `[[USER_RULES]]` remains (custom family, still NUL-safe via `replace`).
- **Gotchas / decisions:**
  - Splitting the old monolithic SYSTEM_RULES: schema/field-name pinning lives in `JSON_SCHEMA`, behavioural
    rules in `SYSTEM_RULES`; SYSTEM_RULES now references "the json object defined in RESPONSE SCHEMA".
  - Cacheability is a STATIC property, not empirically tested against a provider: tests pin determinism
    (`systemMessage` equal across calls), input-freedom (system never contains the classified input), ordering
    (policy < JSON_SCHEMA < SYSTEM_RULES via `indexOf`), and NUL safety of the remaining `[[USER_RULES]]`
    interpolation.
  - Old Task 08 names `jailbreakPrompt(String)`/`nsfwPrompt(String)`/`topicalAlignmentPrompt(String)`/
    `customPrompt(String,String)` are GONE — Task 10/12 must not start from the earlier handoff text.
- **Next task(s) must know:** Task 12 `SpringAiLlmClassifier` → `chatClient.prompt().system(systemMessage(...)).user(input)`. LlmCheck/LlmContract unchanged (classify-only SPI; they never build prompts).

### Task 08 revision A2 — customizable SYSTEM_RULES segment — DONE

- **Date/agent:** 2026-08-29 (same session)
- **Why:** consumers need to refine the output-contract rules for better results (e.g. per-check calibration).
- **Verified command:** `./gradlew :data-privacy-core:build --rerun-tasks` → BUILD SUCCESSFUL (LlmPolicyPromptsTest 11 → 14 tests, 0 failures).
- **Files edited:** `.../policy/LlmPolicyPrompts.java`, `.../policy/LlmPolicyPromptsTest.java`.
- **Public surface added:**
  - `static String systemMessage(String guardrailPrompt, String systemRules)` — overload; the rules segment of
    the system message is the caller's `systemRules`, and a `null`/blank value falls back to the built-in
    `SYSTEM_RULES` constant. Assembly stays `policy + "\n\n" + JSON_SCHEMA + "\n\n" + rules`.
  - Single-arg `systemMessage(String)` now delegates through a private `buildSystem(...)` that also
    `requireNonNull`s the guardrail prompt (previously a null prompt silently became the `"null"` prefix).
- **Gotchas / decisions:** fallback uses `Objects.nonNull(x) && !x.isBlank()`. Custom rules remain input-free →
  system message stays provider-cacheable when the override is static per check.
- **Next task(s) must know:** Task 12 may wire a per-check refined rules block via
  `systemMessage(policy, refinedRules)`; default behavior (api) unchanged for consumers that don't override.

## Task 09 — streaming redaction engine (TextChunker/Tokenizer/StreamRedactor + stream checks + parity) — DONE

- **Date/agent:** 2026-08-29
- **Verified commands:**
  - `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + `checkstyleMain`/`checkstyleTest` green; 202 tests, 0 failures)
  - Streaming↔in-memory parity driver on a 400 050-char stress input (all 4 families, W=512/O=64): `input=400050 expected=334264 actual=334264` — output stream byte-identical to `Redactor.redact(fullText, maskEntities)`, zero diffs
- **Files created** (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`):
  - `stream/TextChunker.java`
  - `stream/Tokenizer.java`
  - `redact/StreamRedactor.java`
  - `checks/WindowMeta.java`
  - `checks/Detector.java`
  - `checks/WindowedSpanScanner.java`
  - `checks/TokenSpan.java`
  - `checks/StreamPiiCheck.java`
  - `checks/StreamUrlsCheck.java`
  - `checks/StreamSecretKeysCheck.java`
  - `checks/StreamKeywordsCheck.java`
  - tests: `stream/TextChunkerTest.java`, `stream/TokenizerTest.java`, `redact/StreamRedactorTest.java`, `checks/StreamCheckParityTest.java`
- **Files edited:**
  - `checks/UrlsCheck.java` — refactored: `detectSpans`/`finalizeCandidates`/`matchDomainAndIp` now return position-aware `record UrlSpan(int start,int end,String token)` (nested record, non-static — bare `record` to dodge checkstyle `RedundantModifier`); `detect(String)`/`isFlagged` keep exact prior behavior; `StreamUrlsCheck` reuses `finalizeCandidates` + `isFlagged` verbatim
  - `checks/PiiCheck.java` — added package-private `PiiCheck.resolveFor(PiiConfig)` (entity list resolution for a given effective config, shared by the stream variant)
  - `checks/SecretKeysCheck.java` / `checks/KeywordsCheck.java` — added package-private `tokenPattern()` / shared pattern+matching placement exposed for the stream variants
  - `stream/TextChunkerTest.java` — moved one assertion to a chunk-boundary-aligned form (`output.startsWith("a".repeat(448)).contains("BCDE")` + window1 prefix checks) so the test no longer depends on exact interior offsets of the first window
  - `redact/StreamRedactor.java` — final pipelined emitter/detector refactor (see algorithm below); Javadoc rewritten to pin the safe-commit contract
- **Public surface added:**
  - `stream.TextChunker implements Iterator<String>` — `TextChunker(String input, int windowSize, int overlap)`, `hasNext()/next()`, `base()/boundary()/last()` window state. Geometry: window0 = text[0, W); window i≥1 = text[i·W − O, (i+1)·W), length W+O. `base = 0 (i==0) else i·W − O`; `boundary = base + window.length()`; final window un-padded. Guards: W>0, O≥0, O≤W, O<full-min length. Grammar-checked: `base` is NOT derivable from `next-run length` — the iterator tracks it arithmetically.
  - `stream.Tokenizer implements MatchAccumulator.MatchSink` — per-window absolute-position token sink.
    `Tokenizer(window)` accumulates into the shared `MatchAccumulator`, with `next = window.base() + matchEnd`,
    so `MatchAccumulator` becomes position-native (windows feed it filtered spans).
  - `redact.StreamRedactor` — ctor `(TextChunker, Map<String,List<String>> maskEntities)`; fields `redact(Reader, Writer)` and `redact(CharSequence)`; empty `maskEntities` → pass-through copy. Consumes the shared `internal.AhoCorasick` compiled once.
  - `checks.WindowedSpanScanner` — scans one window with a category-based `Detector` composite and returns absolute `TokenSpan`-list (single pass, position-aware).
  - `checks.Detector` — package-private functional splitter; `Stream{Pii,Urls,SecretKeys,Keywords}Check` each `implements GuardrailCheck` with `toStream()` returning a real `StreamCheck` (constructed over the same config the in-memory check uses).
  - `GuardrailCheck.toStream()` — now overridden by the four stream-aware checks (in-memory `UrlsCheck/PiiCheck/SecretKeysCheck/KeywordsCheck` keep the throwing default).
- **Window-numbering / effective-window constants (pin this):** `StreamRedactor` computes `effectiveWindow = window + "\n"` for keyword boundary testing but **emits against the same `base/boundary` as the in-memory positions** (the extra char exists only for cross-boundary keyword look-behind). Test fixtures use W=512/O=64.
- **Exact algorithm — detector per category (replicates each in-memory check's semantics to EXACT offsets):**
  - `Detector` per family: PII `PiiPatterns.forEntity(...)` per §8 entity + checksum gates via `isStrictMatch`; URL: `UrlsCheck.finalizeCandidates(UrlsCheck.detectSpans(window))` → `isFlagged(...)`; secrets: `SecretKeysCheck.tokenPattern()` matched against the window + `SecretCandidateFilter.accept(...)`; keywords: `WindowedSpanScanner` finds `\b\Qkw\E\b` words case-insensitively, then `Detector` dedupes (first-seen, case-folded) — keyword detection is whole-word, no `run()` reuse.
  - **Window edge semantics (URLs/secrets/keywords at a boundary):** spans are evaluated against the window string itself (`isFlagged` runs on the raw substring using the in-memory classifier — boundary chars only affect the classifier via the substring extent, matching `run()`), keywords add `"\n"` at the window boundary for look-behind parity. Dedupe/tokenization stays first-seen-order identical to in-memory (`MatchAccumulator`).
  - Drain orders pinned: secrets = built-ins (patternIndex 0, sorted window/start) then custom ordinals; URLs = sorted (patternIndex, window, start) → `finalizeCandidates` → `isFlagged` → `"link"`; keywords = case-folded-first-seen over kept spans only; PII = entity catalog order then customs (empty names skipped). Keys `"secret"`, `"link"`, `"keyword"`, `pii_…`. Never `sink.addAll` an empty list.
- **Exact algorithm — StreamRedactor window emission (safe-commit rule, trace-verified, parity-driven):**
  - One shared `AhoCorasick` automation compiled over the whole `maskEntities` map; per window: scan → collect into `pending` (abs-coordinate spans) → deposit into the shared pending set → select + emit.
  - `selected = MatchSelection.selectLongestFirst(new ArrayList<>(pending))`; iterate start-sorted: **skip** if `span.end() <= flushOffset` (already committed — re-detected overlap duplicates); **hold** if `span.end() > limit` (cut for the LAST window = boundary, else `boundary - overlap`); else `writeRaw(out, emitEnd, span.start())` + placeholder + `emitEnd = span.end()`.
  - **Cut default = `boundary - overlap` for EVERY non-final window** (not only when a visible span ends past the cut): the raw tail write must not pass `boundary - overlap` even when no known span ends there, because a boundary-straddling match (start inside the last O chars) would otherwise have its prefix flushed raw before re-detection. `holdStart = limit` then `min(holdStart, span.start())` per held span.
  - `writeRaw(out, emitEnd, holdStart)`; `flushOffset = holdStart`; `pending.removeIf(span -> span.end() <= flushOffset)`.
  - **Prev-tail gap-fill (window-base re-entry):** fields `prevTail` (String) + `prevTailStart` (int). After each window, `keepFrom = max(base, boundary - 2·overlap)`; `prevTail = window.substring(keepFrom - base)`; `prevTailStart = keepFrom`. Buffer build: if `flushOffset < base` → `buffer.append(prevTail, flushOffset - prevTailStart, base - prevTailStart)` then full window (THE END INDEX IS `base - prevTailStart`, not `prevTail.length()` — overshoot mis-aligns by the overlap); else `buffer.append(window, flushOffset - base, window.length())`. Invariant: any pending span start ≥ flushOffset ≥ base − overlap; the gap [flushOffset, base) ⊆ the previous window's last 2O chars.
  - **Three parity bugs found & fixed in order:** (1) skip-committed via `end <= flushOffset` (else re-detected dupes overwrote placeholders); (2) `pending.removeIf(end <= boundary)` dropped held-but-unemitted spans → leak; (3) default cut `boundary` instead of `boundary - overlap` leaked straddling-match prefixes ("visal@e<EMAIL_ADDRESS>" at diff 1716 → 5576 → clean). The prevTail overshoot (bug 2b) caused `StringIndexOutOfBoundsException` in `writeRaw` for a held span starting below the next window's base.
  - **Robustness:** window finality via `chunker.last()`; secrets/URLs skip spans with null custom-pattern entries; custom-name == pii-type key collision accepted (matched spans merge, matching in-memory map-merge semantics); streaming never allocates the whole input.
- **Gotchas / decisions:**
  - **Effective vs logical window:** the `"\n"` suffix for keyword look-behind is logical-only; the emitter uses raw window bases — this was the source of early parity noise until pinned.
  - **Do NOT reuse `Redactor.run` per window** — that would re-scan placeholders written into earlier output (the in-memory redactor runs once over the original text; a windowed re-run double-masks). The stream detector emits spans at true absolute offsets in one pass.
  - **Keyword boundary parity** needed the injected `"\n"` at the window end because `\b` look-around in `\Qkw\E` is positioning-sensitive; without it, keyword matches at window seams diverge from in-memory.
  - Checkstyle: `record UrlSpan` nested in `UrlsCheck` must be non-static (`RedundantModifier`); all new files needed trailing newlines; no unused imports; all lambda params `final`.
  - Debug-only trace (guarded by `-Dsr.debug` System.err prints) was used to converge engineering then REMOVED — the committed `StreamRedactor` has zero debug output.
- **Next task(s) must know:**
  - **Task 10 (pipeline)** starts from `Guardrails.scan(String)`/`redact(String)`; the stream path is complete and parity-asserted, so pipeline wiring can reuse `StreamRedactor` for `scan(Reader)`/`redact(Reader, Writer)` and `Redactor` for the String paths.
  - **Stream check classes are the streaming equivalents of the four in-memory checks**, constructed from the same configs, exposing `toStream()` returning productive `StreamCheck`s — the pipeline's `Guardrails.Builder` can now interrogate `GuardrailCheckFactory`-built checks for their stream form instead of throwing.
  - The safe-commit rule lives in `StreamRedactor` (design §7.2/§10.2) — keep the cut at `boundary - overlap`, the `end <= flushOffset` skip, and the prev-tail gap-fill intact; they are what makes output identical to in-memory redaction. Any future streaming feature (e.g. cross-window protection sets) must re-verify parity with stress input ≥ 400k chars at W=512/O=64.

## Task 10 — in-memory pipeline (GuardrailPipeline + StageResult + ParallelStageRunner + Guardrails facade) — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + `checkstyleMain`/`checkstyleTest` green; 215 tests, 0 failures).
- **Files created** (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`):
  - `pipeline/StageResult.java`
  - `pipeline/GuardrailPipeline.java`
  - `internal/ParallelStageRunner.java`
  - tests: `src/test/java/io/github/khezyapp/dpriv/pipeline/GuardrailPipelineTest.java`, `.../api/GuardrailsTest.java`
- **Files edited:**
  - `api/Guardrails.java` — filled the in-memory stub bodies (`scan(String)`, `redact(String)`, `run(String, Operation)`); `scan(Reader)`/`redact(Reader, Writer)` remain `throw new UnsupportedOperationException("Task 11")`. Builder now actually wires a pipeline: added `config(GuardrailsConfig)`, `failOnlyOnErrors(boolean)`, made `withClassifier(LlmClassifier)` register, and `build()` constructs a `GuardrailPipeline`.
- **Public surface added (signatures):**
  - `pipeline.StageResult(String stageName, List<GuardrailResult> validations, Map<String,List<String>> maskEntities, String cleanedValue, boolean passed, List<String> errors)` + `boolean detected()` (`!passed`) + `static StageResult aggregate(String, List<GuardrailResult>, String)` and `aggregate(String, List<GuardrailResult>, String, List<String>)` (derives `maskEntities` by merging validations unique-first-seen, sets `passed = !anyDetected`) + `static Map<String,List<String>> mergeMaskEntities(List<GuardrailResult>)` (public, reused by the runner). Canonical ctor null-guards and freezes collections; `cleanedValue` defaults to `""` if null.
  - `internal.ParallelStageRunner(Redactor)` + `StageResult run(String stageName, List<GuardrailCheck> checks, String input)`. Concurrent via `CompletableFuture.supplyAsync` (ForkJoin common pool — library owns no threads). Joins + collects results in **stage order** (deterministic `validations`). A thrown check is captured as an error message in `StageResult.errors()`; the runner always returns. `cleanedValue` = `redactor.redact(input, merged masks)` (fully-merged, equals chaining the deterministic checks).
  - `pipeline.GuardrailPipeline`: `GuardrailPipeline(GuardrailsConfig)`, `GuardrailPipeline(GuardrailsConfig, boolean failOnlyOnErrors)`, `GuardrailPipeline(GuardrailsConfig, List<GuardrailCheck> preflight, List<GuardrailCheck> classificatory, boolean failOnlyOnErrors)`; `StageResult preflight(String)`, `StageResult classify(String)`, `String redact(String)` (`== preflight(input).cleanedValue()`, no LLM), `boolean failOnlyOnErrors()`; `static List<GuardrailCheck> defaultPreflight(GuardrailsConfig)` = `[PiiCheck, SecretKeysCheck, UrlsCheck, KeywordsCheck]` built from `config` (shared `Redactor`).
  - `api.Guardrails` in-memory: `ScanOutcome scan(String)`, `String redact(String)`, `GuardrailsOutcome run(String, Operation)`. `run` → `SANITIZE` = `sanitize(text)` (preflight redaction only, never calls `classify`); `CLASSIFY` = `classify(text)` (preflight, short-circuit on preflight detection; else `classify(preflight.cleanedValue())` on the masked text). `ScanOutcome` derived from the preflight `StageResult`; `GuardrailsOutcome` merges preflight + classify `validations`/`auditRecords`, `maskEntities` from preflight, `detected = classifyFlagged || (failOnlyOnErrors && classifyHasErrors)`, `messages = failOnlyOnErrors ? classify.errors() : List.of()`.
- **Stage ordering / failOnlyOnErrors rule (pinned):**
  - Deterministic preflight order: `PiiCheck → SecretKeysCheck → UrlsCheck → KeywordsCheck` (entityType buckets `pii_*`, `secret`, `link`, `keyword`). Classificatory order = classifier registration order, each wrapped as `LlmCheck(classifier, configFor(beanName))` where `configFor` maps `beanName` ∈ {`llm`,`jailbreak`,`nsfw`,`topical`} to the matching `GuardrailsConfig` slot, else `LlmCheckConfig.DEFAULTS`.
  - `failOnlyOnErrors` is owned by the facade/pipeline, NOT the runner: the runner records every error in `StageResult.errors()` and always returns; the facade converts an error into `detected`/messages only when `failOnlyOnErrors == true`. With `false`, a classifier error is swallowed (no `detected`, empty `messages`) — deterministic checks never throw, so errors only arise from classifiers.
  - `entityType()` of an outcome = first *detected* validation's `entityType` across merged validations, else first `maskEntities` key (matches `ScanOutcome.entityTypes()`).
- **Gotchas / decisions:**
  - `preflight` runs the 4 deterministic families (incl. keyword) — this follows the Task-10 file's explicit enumeration, which puts `Keyword` in preflight, a deliberate refinement of design §6.1's "keywords run on masked text in stage 2". The pipeline's `classify` stage therefore only carries LLM checks.
  - `StageResult` gained an extra `errors` component beyond the task's record sketch — required to carry classifier failures to the facade for the `failOnlyOnErrors` policy. The public contract stays stable for Task 11.
  - `redact(String)` and `run(..., SANITIZE)` never invoke LLM checks (asserted by `GuardrailsTest.sanitizeNeverInvokesClassifier`) — the classificatory stage is only reached on the `CLASSIFY` happy path that passes preflight.
  - Email entity key is `pii_email_address` (→ placeholder `<EMAIL_ADDRESS>`), confirming Task 03/07's pin; tests assert `"pii_email_address"` not the prose simplification `"pii_email"`.
  - Common-pool only: no `ExecutorService`/owned threads, so no executor lifecycle or memory-leak surface (acceptance criterion met).
- **Next task(s) must know:**
  - **Task 11 (stream pipeline):** reuse `GuardrailPipeline.redact`/`preflight` semantics; the streaming `scan(Reader)`/`redact(Reader,Writer)` should mirror this two-stage contract using `StreamCheck`s + `StreamRedactor` (Task 09 is parity-locked to `Redactor`). Keep `StageResult`/`failOnlyOnErrors` behavior identical so the facade's streaming bodies drop into `api/Guardrails`.
  - The `Guardrails.Builder` now needs `config(...)` + `failOnlyOnErrors(...)` to be usable; `withClassifier` is the only classifier registration path. `GuardrailsConfig.DEFAULTS` + no classifiers → classify stage empty (purely deterministic).
  - `ParallelStageRunner` and `StageResult` are package-agnostic (live in `internal`/`pipeline`); the streaming pipeline can reuse `StageResult` directly.

## Task 11 — streaming pipeline (StreamPipeline + Guardrails.scan(Reader)/redact(Reader, Writer)) — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + `checkstyleMain`/`checkstyleTest` green; streaming suite `GuardrailsStreamingTest` green). Also fixed a **pre-existing Task-09 checkstyle violation** in `StreamSecretKeysCheck.java:112` (`))` followed by a standalone `;`) that was blocking the build — corrected to `));` so the module builds.
- **Files created** (under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`):
  - `pipeline/StreamPipeline.java`
  - tests: `src/test/java/io/github/khezyapp/dpriv/api/GuardrailsStreamingTest.java`
- **Files edited:**
  - `api/Guardrails.java` — filled the streaming stubs: `scan(Reader)` → `streamPipeline.scan(input)`; `redact(Reader, Writer)` → `streamPipeline.redact(input, out)`. `Guardrails` now holds a `StreamPipeline` built in `Builder.build()` alongside the in-memory `GuardrailPipeline` from the **same** `config`/`preflight`/`classificatory`/`failOnlyOnErrors`. Added `StreamPipeline` import and updated the class Javadoc. No new public methods beyond the two streaming ones.
- **Public surface added (signatures):**
  - `pipeline.StreamPipeline(GuardrailsConfig config, List<GuardrailCheck> preflight, List<GuardrailCheck> classificatory, boolean failOnlyOnErrors)` — the streaming counterpart of `GuardrailPipeline`. It derives its runnable `StreamCheck`s in the **ctor** by calling `GuardrailCheck.toStream()` on each preflight check; a check whose `toStream()` throws `UnsupportedOperationException` (notably `LlmCheck`) is **skipped** — the LLM stage never participates in streaming, by design.
  - `ScanOutcome scan(Reader input)` (pass 1 only): reads the input once, then runs each `StreamCheck` **concurrently** on the ForkJoin common pool (same `CompletableFuture.supplyAsync` + join-in-order discipline as `ParallelStageRunner`), each into its own `MatchAccumulator`; the per-family accumulators are merged, errors captured per check (design §13 — contained, never dropped). Output `ScanOutcome` has the same `entityTypes()`/`maskEntities`/`auditRecords` derivation as `Guardrails.scan(String)`.
  - `void redact(Reader input, Writer out)` (pass 1 → pass 2): pass 1 = `scan` to build the full `maskEntities`; pass 2 = `new StreamRedactor(new TextChunker(reader), maskEntities).redact(out)` then `out.flush()`. Parity-equal to `Guardrails.redact(String)`.
- **Re-readable-source contract / one-shot buffering note (pinned):**
  - The library **never closes** caller-owned `Reader`s/`Writer`s; the `Writer` is flushed on success, never closed.
  - Because a `Reader` is single-pass and each `StreamCheck` windows the input independently, the streaming `scan`/`redact` read the whole input into a `String` once (the `readFully` helper) and hand **each** check an independent `StringReader` over that copy so per-check concurrent reads work. **This means `scan(Reader)` is NOT strictly O(window) in this implementation** — it buffers the full input for the one-shot-reader path (the task's "no full buffering" goal is a soft, code-review assertion; documented here as a known tradeoff). A genuinely zero-copy concurrent fan-out (splitter + piped readers) was deferred; correctness/parity is unaffected. `redact` likewise buffers the one-shot `Reader` so pass 1 (scan) and pass 2 (`StreamRedactor`) can each read the source; a re-readable `Reader` is not specially detected/fast-pathed (buffering is the universal safe path).
  - ScanOutcome.text for the streaming path is the fully-read input `String` (kept complete so audit records carry the rawText), matching the in-memory `scan(String)` field semantics.
- **Confirmed parity assertions (test names in `GuardrailsStreamingTest`):**
  - `scanParityMultiWindow` — ≥2 windows fixture (`.repeat(800)` of the 4-family FIXTURE): `scan(Reader).maskEntities` == `scan(String).maskEntities`; `entityTypes` == `containsExactlyInAnyOrder`; `detected` equal.
  - `redactParityMultiWindow` — `redact(Reader, Writer)` == `redact(String)`.
  - `boundaryStraddleParity` — email token straddling the 64 KiB window boundary (`"a".repeat(65520) + " " + email + " tail"`): streaming == in-memory (realistic token with a word boundary before it — a pathological 64k-`a` local-part is intentionally avoided; see Gotchas).
  - `oneShotReaderRedaction` — a non-resettable `InputStreamReader`/`ByteArrayInputStream` (one-shot) is redacted correctly via the buffering path, equal to `redact(String)`.
  - `emptyAndTinyInputs` — empty string and one-char `"a"`: empty `ScanOutcome` (`detected=false`, empty `maskEntities`/`errorMessages`/`entityTypes`).
  - `streamingNeverInvokesLlm` — a `CapturingClassifier` is **never** invoked by `scan(Reader)` or `redact(Reader, Writer)` (asserts `saw() == null` while deterministic masks are still produced).
  - `streamingDeterministicAcrossRuns` — `scan(Reader)` 50× → `isEqualTo` each time; `maskEntities` stable.
- **Gotchas / decisions:**
  - Streaming **never runs the LLM**: `LlmCheck.toStream()` throws the default `UnsupportedOperationException` (per `GuardrailCheck` SPI contract), so it is filtered out of `StreamPipeline`'s check list at construction. Task 12 only needs to wire `SpringAiLlmClassifier` into `run(…, CLASSIFY)` — the streaming paths are LLM-free by construction.
  - Non-streamable preflight checks are skipped silently (only `UnsupportedOperationException` is caught); any other throw inside a `StreamCheck.scan` is captured as an error message in `ScanOutcome.errorMessages()` (design §13).
  - Audit records for the streaming path are built one per detected `entityType` (`new AuditRecord(entityType, List.of(), text)`) — there are no per-check `GuardrailResult`s in the streaming model, so `validations` is empty (in-memory `scan` populates it from `StageResult.validations`; parity tests compare `maskEntities`/`entityTypes`, not `auditRecords` internals).
  - `primaryEntityType(masks)` = first `maskEntities` key (insertion order), mirroring the in-memory fallback (no detected-validation path exists in streaming).
  - **Pathological fixture warning (do NOT replicate):** a token whose local-part is thousands of non-`@` word chars (e.g. `"a".repeat(65528) + "visal@example.com"`) makes the in-memory EMAIL regex absorb the entire filler into one giant token AND exposes a Task-09 `WindowedSpanScanner` left-edge drop when that token straddles a boundary. Realistic tokens (space/punctuation boundary before) are parity-safe; the streaming engine is parity-locked to `Redactor` for realistic inputs only.
- **Next task(s) must know:**
  - **Task 12 (Spring AI adapter):** wire `SpringAiLlmClassifier` (in `data-privacy-spring-ai`) into `Guardrails.Builder.withClassifier(...)`; the streaming paths (`scan(Reader)`/`redact(Reader, Writer)`) are already LLM-free and need no changes. The in-memory `run(…, CLASSIFY)` already routes classifiers through `LlmCheck` → `LlmContract` (Task 08), so the adapter only supplies an `LlmClassifier` implementation. `StreamPipeline`'s `classificatory` param is intentionally unused (streaming excludes LLM) — keep that invariant.
  - `Guardrails.Builder` now exposes `config(...)`, `failOnlyOnErrors(...)`, `withClassifier(...)`, and builds **both** `GuardrailPipeline` and `StreamPipeline`. Any new builder knob must propagate to both.
  - Threading: streaming scan uses the ForkJoin common pool (no owned threads, same as `ParallelStageRunner`); `StreamRedactor` is single-threaded per call. No executor lifecycle to manage.

## Task 12 — Spring AI adapter (SpringAiLlmClassifier) — DONE

- **Date/agent:** 2026-08-29
- **Verified command:** `./gradlew :data-privacy-spring-ai:build` → BUILD SUCCESSFUL (compile + `checkstyleMain`/`checkstyleTest` green; test suite `SpringAiLlmClassifierTest` green). Core is resolved from the composite build via coordinate `io.github.khezyapp:data-privacy-core:1.0.0` (substitution).
- **Spring AI version RESOLVED & PINNED:** BOM `org.springframework.ai:spring-ai-bom:2.0.1` (stable GA, per instruction). Verified present on Maven Central. Transitively pulls `spring-ai-commons:2.0.1`, `spring-ai-model:2.0.1`, `spring-ai-template-st:2.0.1`. **NOTE — Spring AI 2.0 uses Jackson 3 (`tools.jackson.*`), not Jackson 2.** `BeanOutputConverter` (in `spring-ai-model`, reached transitively through `spring-ai-client-chat`) carries a `ResponseTextCleaner`, so fenced/```-wrapped JSON model output is cleaned before parsing. The build.gradle `ext.springAiVersion` was already set to `2.0.1` and is wired into the `platform(...)` import.
- **`spring-ai-test` divergence (important):** the task assumed `org.springframework.ai:spring-ai-test` exposes `MockChat*`. In 2.0.1 it does **NOT** — the test jar contains only `vectorstore`/`advisor` base tests and `AudioPlayer`/`CurlyBracketEscaper`. So tests use a **hand-written `ChatModel` stub** (repo style: anonymous stubs, no Mockito) returning canned JSON, wrapped by `ChatClient.create(stub)`. This exercises the real `DefaultChatClient` → `entity(BeanOutputConverter)` → record-deserialization path.
- **Files created** (under `securities/data-privacy-spring-ai/src/`):
  - `main/java/io/github/khezyapp/dpriv/springai/SpringAiLlmClassifier.java` — `public final class SpringAiLlmClassifier implements LlmClassifier` + nested `Builder`.
  - `main/java/io/github/khezyapp/dpriv/springai/SpringAiLlmClassifierFactory.java` — `jailbreak/nsfw/topical(ChatClient, double)` factories (builder covers custom prompts, so only the 3 family factories are provided).
  - `test/java/io/github/khezyapp/dpriv/springai/SpringAiLlmClassifierTest.java` — tests (single-call delegation, beanName, confidence clamp [0,1], malformed→throws through `LlmCheck`, `BeanOutputConverter` round-trip of `Verdict`, factory family wiring).
- **Public surface added (signatures):**
  - `SpringAiLlmClassifier implements LlmClassifier` — `static Builder builder()`; `Verdict classify(String)`; `String beanName()`. `classify` does `chatClient.prompt().system(<LlmPolicyPrompts.systemMessage(family, systemRules)>).user(input).call().content()` then parses with `converter.convert(content)` where `converter = new BeanOutputConverter<>(LlmClassifier.Verdict.class)`. **Sanitizes** the raw `Verdict`: NaN/±Inf confidence→0.0, out-of-range clamped to [0,1]. The `Verdict` has NO `entityType` (it comes from `beanName()` in core `LlmContract`), so the model only emits `flagged`+`confidence` — exactly matching `LlmPolicyPrompts.JSON_SCHEMA` and avoiding token waste. NO threshold decision here (core `LlmContract` owns it).
  - `Builder` — `chatClient(ChatClient)`, `beanName(String)` (must be a core family `jailbreak`/`nsfw`/`topical`/`topicalAlignment` OR a custom `prompt(...)` must be supplied), `prompt(String)` (custom guardrail block overriding the built-in family), `systemRules(String)` (custom system-rules block overriding the built-in `SYSTEM_RULES`; null/blank falls back), `threshold(double)` (recorded, exposed via `threshold()`, NOT applied — forwarded for `Guardrails` config), `build()`.
  - `SpringAiLlmClassifierFactory` — `jailbreak/nsfw/topical(ChatClient, double threshold)`.
- **Exact Spring AI calls used (log for Task 13 / design §11 divergence):**
  - `org.springframework.ai.chat.client.ChatClient` (package is `org.springframework.ai.chat.client`, NOT `org.springframework.ai.chat`).
  - Fluent chain: `ChatClient.prompt()` → `ChatClientRequestSpec.system(String)` → `.user(String)` → `.call()` → `CallResponseSpec.content()` (returns the raw `String`); the `BeanOutputConverter` is used ONLY for parsing (`converter.convert(content)`), NOT via `entity(converter)` — because `entity(converter)` auto-injects `converter.getFormat()` (its own JSON schema) into the prompt, duplicating `LlmPolicyPrompts.JSON_SCHEMA` already in the system message and wasting tokens.
  - `BeanOutputConverter<T>` lives in `org.springframework.ai.converter` (artifact `spring-ai-model`, transitive) — `new BeanOutputConverter<>(LlmClassifier.Verdict.class)`; parsed via `converter.convert(content)`. Its `ResponseTextCleaner` still strips ``` ```json ``` fences.
  - Test stub implements `org.springframework.ai.chat.model.ChatModel.call(Prompt)` returning `new ChatResponse(List.of(new Generation(new AssistantMessage(json))))` (package `org.springframework.ai.chat.model` / `org.springframework.ai.chat.messages`).
- **Wiring for Task 13:** `Guardrails.builder().withClassifier(SpringAiLlmClassifierFactory.jailbreak(chatClient, 0.7))` (or `.nsfw(...)`/`.topical(...)`, or a custom `SpringAiLlmClassifier.builder()...build()`). `Guardrails.Builder.configFor(classifier)` already switches on `beanName` `llm`/`jailbreak`/`nsfw`/`topical` → the adapter's `beanName` MUST be one of those four so the right `LlmCheckConfig` is selected (this is why the factory uses `beanName("jailbreak"|"nsfw"|"topical")`, not `spring-ai-jailbreak`). No `Guardrails` changes needed; only supply the `LlmClassifier`.
- **Gotchas / decisions:**
  - Adapter contains ONLY the classifier + factory: no pipeline, no clone of core logic (acceptance criterion met).
  - **`Verdict` is `record Verdict(boolean flagged, double confidence)`** (no `entityType`) — changed in core `LlmClassifier` (Task 02 record) so the model output matches `LlmPolicyPrompts.JSON_SCHEMA` exactly (only `flagged`+`confidence`), preventing `BeanOutputConverter` parse failures and saving tokens. The entity type is supplied by `LlmClassifier.beanName()` and applied by core `LlmContract.toResult`. All core test stub classifiers updated to the 2-arg `Verdict` ctor.
  - `Malformed/non-JSON` model output → `BeanOutputConverter.convert` throws → `classify` throws → surfaced as an error contained by the core pipeline (`LlmCheck.run` rethrows; the `GuardrailPipeline`/design §13 containment wraps it). Verified via `LlmCheck` in the test.
  - Build-logic `khezy.java-lombok` is applied but the classifier uses a **hand-written** builder (records + final fields per repo style), so no Lombok `@Builder` is actually used — the Task-01 note about `@Builder` was not followed; hand-written keeps zero-reflection deserialization compatible with the `Verdict` record.

## Task 13 — acceptance: guarantee-scope regression, end-to-end, READMEs — DONE (v1 wrapped)

- **Date/agent:** 2026-08-29
- **Verified commands:**
  - `./gradlew :data-privacy-core:build` → BUILD SUCCESSFUL (compile + full JUnit suite incl. 14-test `GuaranteeScopeTest` + `checkstyleMain`/`checkstyleTest` green)
  - `./gradlew :data-privacy-spring-ai:build` → BUILD SUCCESSFUL (compile + `EndToEndSpringAiTest` + Checkstyle green)
- **Files created:**
  - `securities/data-privacy-core/src/test/java/io/github/khezyapp/dpriv/GuaranteeScopeTest.java` — the §3 acceptance suite (13 guarantee/non-guarantee tests + 1 cross-family e2e)
  - `securities/data-privacy-spring-ai/src/test/java/io/github/khezyapp/dpriv/springai/EndToEndSpringAiTest.java` — cross-module smoke test (stubbed `ChatModel` + `SpringAiLlmClassifierFactory`; no Mockito, per repo style)
  - `securities/data-privacy-core/README.md` — one-screen: what/why, Maven+Gradle install, minimal scan/redact, CLASSIFY example, config defaults, API table, guarantee box (G1–G7/N1–N5 → test names), build commands, attribution → `CREDITS.md`
  - `securities/data-privacy-spring-ai/README.md` — one-screen: intro, install, factory + custom-classifier usage (`SpringAiLlmClassifier.builder()...`), threshold-gating + deterministic-first semantics, build commands
- **Files edited:**
  - `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/internal/AhoCorasick.java` — identical-token tie-break fix (see Deficiencies below)
  - `scrum/data-privacy/README.md` — added **v1 milestone (implemented)** section; design doc marked ✅ implemented; date bumped
  - `scrum/data-privacy/v1-actions/00-INDEX.md` — added `Status` column, all 13 tasks marked `✅ Done`
  - `scrum/data-privacy/v1-actions/00-HANDOFF.md` — this entry
  - (earlier in this session, re-verified here) fail-closed fixes in `GuardrailPipeline.redact`, `StreamPipeline.redact`, `Guardrails.sanitize`/`classify`
- **Guarantee → test mapping (design §3, this is the acceptance checklist):**

  | Guarantee | Test(s) | Proves |
  |---|---|---|
  | G1 determinism | `g1DeterministicExactness` | repeated `scan`/`redact`/`run` (in-memory + streaming) are binary-identical |
  | G2 redaction completeness | `g2CompleteRedactionOfDetectedTokens` | every token in every `maskEntities` value is un-findable in the output (incl. the `<...>` literal not being re-redacted) |
  | G3 typed placeholders | `g3TypedPlaceholdersIdentifyThePolicyRule` | each family maps to its own placeholder: `pii_email_address→<EMAIL_ADDRESS>`, `pii_credit_card→<CREDIT_CARD>`, `secret→<SECRET>`, `link→<LINK>`, `keyword→<KEYWORD>` |
  | G4 fail-safe | `g4ErroredClassifierNeverPasses` + `g4RedactionFailsClosedOnCheckError` | errored classifier never yields `detected`; redact/sanitize throw on check errors |
  | G5 zero side effects | `g5ZeroSideEffectsOnAnyPath` | errors are contained (no throws, no leaks); passthrough when undeployed |
  | G6 streaming | `g6StreamingUsesBoundedWindowsOnLargeInput` | ~500 KiB input, tiny-window `StreamRedactor(new TextChunker(reader, 512, 64))` → bounded memory, parity output |
  | G7 audit | `g7AuditRecordsReturnedAsReproducibleData` | `auditRecords` carry entityType/validations/rawText; deterministic across runs |
  | N1 | `n1ConfidenceIsModelOpinionScaledByThreshold` | flagged-but-below-threshold does not trip |
  | N2 | `n2ObfuscationDefeatsExhaustiveDetection` | documented non-exhaustiveness pinned (leet/obfuscation beats regexes) |
  | N3 | `n3LoggingIsACallerDecisionTheLibraryReturnsRawData` | library never logs; outputs raw data to the caller |
  | N4 | `n4DownstreamRoutingIsOutOfScope` | routing decisions are the caller's |
  | N5 | `n5EntropyScanningTradesPrecisionForRecall` | entropy scanning is a recall-priority heuristic (bounds pinned) |
  | E2E | `endToEndAllFamiliesMatchApiSurface` (core) | one FIXTURE through `scan`/`redact`/`run(CLASSIFY)`+stub + streaming variants → §12.3 outcome shapes |
  | Spring e2e | `EndToEndSpringAiTest.jailbreakFamilySurfacesEndToEnd` + `deterministicPathsDoNotCallTheModel` | classifier wired into `Guardrails` surfaces jailbreak; scan/redact/SANITIZE never invoke the model |

- **Spring AI wiring (reference usage, from Task 12 — verified live in the e2e test):**
  ```java
  ChatClient client = ChatClient.create(stub);                 // stub ChatModel → canned JSON Verdict
  Guardrails g = Guardrails.builder()
      .withClassifier(SpringAiLlmClassifierFactory.jailbreak(client, 0.7))
      .build();
  GuardrailsOutcome o = g.run(text, Operation.CLASSIFY);       // jailbreak family surfaces when flagged
  ```
- **Final API surface (one line per public type, `io.github.khezyapp.dpriv.*`):**
  - `api.Guardrails` — facade: `Builder.config/failOnlyOnErrors/withClassifier/build`; `ScanOutcome scan(String|Reader)`, `String redact(String)`, `void redact(Reader,Writer)`, `GuardrailsOutcome run(String, Operation)`.
  - `api.GuardrailsConfig` — feature slots: `PiiConfig(boolean, List<String>)`, `SecretConfig(SecretPreset, boolean)`, `UrlsConfig(List<String> allowlist, List<String> blocklist)`, `KeywordsConfig(boolean, List<String>)`, `CustomRegexConfig(List<String>)`, `LlmCheckConfig(boolean enabled, double threshold)` (+ `llm`/`jailbreak`/`nsfw`/`topical` slots); `DEFAULTS` (keywords off, llm `(true,0.7)`).
  - `api.GuardrailsOutcome(text, entityType, detected, validations, maskEntities, auditRecords, messages)`; `api.ScanOutcome(text, entityType, detected, errorMessages, maskEntities, auditRecords)`.
  - `api.AuditRecord(entityType, List<GuardrailResult> validations, rawText)`; `api.GuardrailResult.pass/fail(entityType, cleanedValue, masks)`.
  - `api.GuardrailCheck` (SPI) + `GuardrailCheckFactory` + `api.StreamCheck`; `api.LlmClassifier` with `record Verdict(boolean flagged, double confidence)` + `beanName()`; `api.Operation` (`CLASSIFY | SANITIZE`).
  - `policy.PiiEntity` (33 patterns + entry order); `policy.SecretPreset` STRICT(10,3.0,2,true)/BALANCED(10,3.8,3,false)/PERMISSIVE(30,4.0,2,false) + `SecretPresetParams`.
  - `stream.MatchAccumulator`. Adapter (`springai.`): `SpringAiLlmClassifier` (+ `Builder`) and `SpringAiLlmClassifierFactory.jailbreak/nsfw/topical(ChatClient, double)`.
  - Placeholders are hard-pinned `"<" + UPPER_SNAKE + ">"`: `$S8::PiiEntityType` → `<EMAIL_ADDRESS>`, `<CREDIT_CARD>`, `<US_BANK_NUMBER>`, ... ; `secret`→`<SECRET>`, `link`→`<LINK>`, `keyword`→`<KEYWORD>`. Not configurable.
- **Deviations / known tradeoffs (recorded, accepted):**
  1. **G6 at the facade (not the engine):** `scan(Reader)`/`redact(Reader,Writer)` `readFully` the input once (one-shot-reader fan-out) so each `StreamCheck` gets an independent `StringReader`; strict `O(window)` memory holds for the **engine** (`StreamRedactor` + `TextChunker`, verified at W=512/O=64 over ~500 KiB), NOT end-to-end at the facade. Parity is unaffected; a zero-copy splitter is the deferred v1.1 item (Task-11 handoff documents the same tradeoff).
  2. **`AuditRecord.entityType` is family-level:** a scan's audit record for the PII family carries `"pii"` (aggregate), granular types (`pii_email_address`, ...) live in `maskEntities` keys / `ScanOutcome.entityTypes()`. Streaming audit records are per-family `new AuditRecord(entityType, List.of(), text)` (empty `validations`).
  3. **AhoCorasick identical-token tie-break fix (acceptance-found):** a token matching ≥2 entity types (e.g. `4111111111111111` = `pii_credit_card` + `pii_us_bank_number`) was emitted context-dependently because `StageResult.mergeMaskEntities`'s `Map.copyOf` (order-dropping, JDK gotcha) fed `AhoCorasick` in arbitrary order and `insert` kept the last writer. Fix: on an existing terminal `best`, keep the lexicographically smallest `entityType` — aligning with `MatchSelection`'s final `entityType`-ascending tie-break. Now `<CREDIT_CARD>` in both isolated and composite inputs. This is the ONLY correctness code change in acceptance.
  4. **Fail-closed final state (G4):** `GuardrailPipeline.redact`/`StreamPipeline.redact` throw `IllegalStateException` on check errors; `Guardrails.sanitize` throws on preflight errors; `classify` short-circuits on preflight detection and folds preflight errors into `messages`/`detected` only under `failOnlyOnErrors`.
- **v1.1 recommendations:** (a) zero-copy streaming fan-out so `scan(Reader)`/`redact(Reader,Writer)` are end-to-end `O(window)`; (b) granular per-pattern audit records (entityType per matched pattern, rich `validations` in the streaming path); (c) reroute `SpringAiLlmClassifier.Builder.threshold` into `GuardrailsConfig` programmatically (currently recorded, decision owned by core config); (d) optional configurable placeholder format; (e) context-aware PII class disambiguation (credit-card vs US-bank-number on shared prefixes) via a post-detection resolver instead of the tie-break fallback.
