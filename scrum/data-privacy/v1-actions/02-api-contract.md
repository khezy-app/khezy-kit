# Task 02 — API contract: `api/`, `policy/` enums, `stream/MatchAccumulator`

## Objective

Define the **public API contract** of `data-privacy-core` per design §5 (types), §12.1 (facade),
and the resolved open questions. This task emits **types and signatures only** — every type compiles
in isolation (the runtime logic comes in tasks 03–11). After this task, `data-privacy-core` is the
module every other task compiles against.

## Hand-off context

- **Design doc:** §5.1 `GuardrailResult`, §5.2 `GuardrailCheck`/`StreamCheck`, §5.3 outcomes, §5.4
  config schema, §12.1 `Guardrails` facade signatures, §12.3 flow records, §5.6/§2 "no strictness"
  → **`PiiConfig` carries `boolean strict`** (design §8.2 vs §5.4 discrepancy — resolved: add field).
- Module scaffold ready from Task 01, package root `io.github.khezyapp.dpriv` (create sub-packages
  `api`, `policy`, `stream`, `internal`, `checks`, `pipeline`, `redact`).
- **Read design §5 + §12 content in the design doc before writing signatures.** Do not read the
  `docs/` folder or the reference implementations.
- Checkstyle gotchas apply (`final` everywhere, no unused imports, no `{ }` empty) — see
  `.opencode/skills/khezy-checkstyle-gotchas/SKILL.md`.

## Files to create (all under `securities/data-privacy-core/src/main/java/io/github/khezyapp/dpriv/`)

### `api/` — records, functional types, facade, config

1. **`api/GuardrailResult.java`** — immutable result of ONE check (design §5.1):
   ```java
   public record GuardrailResult(String entityType, boolean detected, String cleanedValue,
                                 Map<String, List<String>> maskEntities)
   ```
   - `entityType` design enum-alike "pii_credit_card" / "secret" / "link" / "keyword" / "jailbreak" / ...
     (string, matching the reference contract; never null).
   - `detected` = whether the input violated this check; `cleanedValue` = sanitized text (so
     `redact()` chains maskEntities from checks); `maskEntities` = `entityType -> non-windowed token
     list`. **Defaults:** `new GuardrailResult(type, false, input, Map.of())` via a static helper
     `static GuardrailResult pass(String entityType, String cleanedValue)` and
     `static GuardrailResult fail(String entityType, String cleanedValue, Map<String,List<String>> masks)`.
   - Add `boolean isPassed()` (not in the record header — derived).

2. **`api/GuardrailCheck.java`** — the deterministic checks SPI (design §5.2):
   ```java
   @FunctionalInterface
   public interface GuardrailCheck {
       GuardrailResult run(String input);
       default String name();            // returns getClass().getSimpleName() unless overridden
       default StreamCheck toStream();   // default throws UnsupportedOperationException
   }
   ```
   `toStream()` is the bridge to the streaming engine: checks that support streaming override it
   (tasks 03/05/06/07); `LlmCheck` and other non-streamable checks keep the throwing default (design §10).

3. **`api/StreamCheck.java`** — streaming engine entry (design §5.2, §10):
   ```java
   @FunctionalInterface
   public interface StreamCheck {
       void scan(Reader input, MatchAccumulator sink);
       default String name();
   }
   ```

4. **`api/GuardrailCheckFactory.java`** — how checks are registered **even when instantiation needs
   config** (design §5.2 note; used with reflection-free, preconfigured instances):
   ```java
   @FunctionalInterface
   public interface GuardrailCheckFactory<C> {
       GuardrailCheck create(C config);
   }
   ```

5. **`api/LlmClassifier.java`** — LLM check SPI (design §11.2):
   ```java
   public interface LlmClassifier {
       Verdict classify(String input);
       String beanName();   // unique per classifier, used as entityType ("jailbreak"/"nsfw"/...)
   }
   public static final record Verdict(boolean flagged, double confidence, String entityType) {}
   ```
   (The canonical Spring AI implementation lives in the adapter module — Task 12.)

6. **`api/Guardrails.java`** — the facade. **Only signatures this task; implementation in tasks 10/11.**
   For the contract, declare the class + the public methods and leave `// Task 10/11` bodies; that is
   enough for the module to compile and for every other task to compile against it:
   ```java
   public final class Guardrails {
       public static Guardrails.Builder builder();
       public GuardrailsOutcome run(String text, Operation op);            // Task 10 (in-memory)
       public ScanOutcome scan(String text);                                // Task 10
       public String redact(String text);                                   // Task 10
       public ScanOutcome scan(Reader input);                               // Task 11 (streaming)
       public void redact(Reader input, Writer out);                        // Task 11 (streaming)
   }
    public static final class Builder {           // fluent, documented in design §12.4
         public Builder config(GuardrailsConfig config);
        public Builder failOnlyOnErrors(boolean value);
        public Builder withClassifier(LlmClassifier classifier);
        public Guardrails build();
   }
   ```
   `run(String, Operation)` semantics (design §12.2): `Operation.CLASSIFY` = constrained
   LLM-as-judge annotate/validate; `Operation.SANITIZE` = deterministic redaction only.

### `api/` — configuration schema (design §5.4 schema; §8 default table)

Keep each config record `static`-pure, no logic. `GuardrailsConfig` is the composition root with a
**builder** (design §9.1 `GuardrailsConfig.builder().pii(...).secrets(...).urls(...).keywords(...)
.customRegexes(...).llm(...).jailbreak(...).nsfw(...).topical(...).build()`).

```java
public record PiiConfig(PiiCoverage coverage, Set<PiiEntity> entities,
                        List<CustomRegexConfig> customRegexes, boolean strict)
    { public static final PiiConfig DEFAULTS = new PiiConfig(ALL, Set.of(), List.of(), true); }

public enum PiiCoverage { ALL, SELECTED }

public record SecretConfig(SecretPreset preset, Map<String, List<Pattern>> customPatterns)
    { public static final SecretConfig DEFAULTS =
        new SecretConfig(SecretPreset.BALANCED, Map.of()); }

public record UrlsConfig(List<String> allowedSchemes, List<String> allowedHosts)
    // defaults IRL: [ "http","https" ], empty allow-list
    { public static final UrlsConfig DEFAULTS = new UrlsConfig(List.of("http","https"), List.of()); }

public record KeywordsConfig(boolean toMask, List<String> keywords) {}

public record CustomRegexConfig(String name, List<Pattern> patterns) {}

public record LlmCheckConfig(boolean enabled, double threshold)   // default 0.7
    { public static final LlmCheckConfig DEFAULTS = new LlmCheckConfig(true, 0.7); }

public record GuardrailsConfig(PiiConfig pii, SecretConfig secrets, UrlsConfig urls,
                               KeywordsConfig keywords, List<CustomRegexConfig> customRegexes,
                               LlmCheckConfig llm,
                               LlmCheckConfig jailbreak, LlmCheckConfig nsfw,
                               LlmCheckConfig topical, Map<String, Boolean> booleanOptions) {
    public static Builder builder() { ... }   // design §9.1 builder
    public static final GuardrailsConfig DEFAULTS = builder().build();
}
```

Maintain configuration **defaults exactly as the design schema / resolved table** (BALANCED,
LlmCheck enabled=true threshold 0.7, small `GuardrailsConfig.builder()` chainable). Note the design
doc keeps `booleanOptions` in the schema but does **not** specify keys — document it as "reserved"
and keep it `Map.of()`.

### `policy/` — enums needed by the config

7. **`policy/PiiEntity.java`** — enum ONLY, with the **33 design names** as constants (values from
   design §8 list — use the exact constant names in the design):
   `GLOBAL_PHONE/NUMBER/EMAIL_ADDRESS/PERSON_NAME/GEO_LOCATION/DATE_TIME/AGE/CURRENCY/NATIONALITY`,
   regional subsets (US, UK, ES, IT, PL, SG, AU, IN, FI). Add `private final String typeString` +
   `public String type()`, e.g. `EMAIL_ADDRESS.type() == "pii_email"` — the `pii_`-prefixed contract
   string family from design §5.1. Keep the enum body free of regex/pattern logic (that is Task 04).

8. **`policy/SecretPreset.java`** — enum carrying the resolved preset tuple:
   ```java
   public enum SecretPreset {
       STRICT(10, 3.0, 2, true),      // design §9.2 STRICT
       BALANCED(10, 3.8, 3, false),   // default
       PERMISSIVE(30, 4.0, 2, false);
   }
   public record SecretPresetParams(int minLength, double minEntropy, int minDiversity, boolean strictMode) {}
   ```
   Signature order and `strictMode` naming are pinned in the handoff log; Task 05 consumes it.

### `stream/` — the sink every streamed check writes to (design §10.3)

9. **`stream/MatchAccumulator.java`** — dedupes by `(entityType, token)` **value** (design §10.3);
   token order = first-seen order (insertion order of a LinkedHashSet per type):
   ```java
   public final class MatchAccumulator {
       public void add(String entityType, String token);                    // dedupe insert
       public void addAll(String entityType, List<String> tokens);          // dedupe, preserve order
       public Set<String> entityTypes();
       public List<String> tokens(String entityType);                       // unmodifiable
       public Map<String, List<String>> toMaskEntities();                   // unmodifiable deep-ish
       public int entityTypeCount();  // convenience for tests/parity
   }
   ```

### `api/` — outcome records (design §5.3)

10. **`api/GuardrailsOutcome.java`**
    ```java
    public record GuardrailsOutcome(String text, String entityType, boolean detected, List<GuardrailResult> validations, Map<String, List<String>> maskEntities, List<AuditRecord> auditRecords, List<String> messages)
        { public boolean isPassed(); }
    ```
11. **`api/ScanOutcome.java`**
    ```java
    public record ScanOutcome(String text, String entityType, boolean detected,
                              List<String> errorMessages, Map<String, List<String>> maskEntities,
                              List<AuditRecord> auditRecords)
        { public boolean isPassed(); }
    ```
12. **`api/AuditRecord.java`**
    ```java
    public record AuditRecord(String entityType, List<GuardrailResult> validations, String rawText) {}
    ```
    Design §5.3 vs §12.3 note: outcomes expose `auditRecords()` as derived convenience (Task 10
    derivation — resolved in INDEX). Also add `ScanOutcome.entityTypes()` convenience (`maskEntities
    .keySet()` — design §12.3 usage.)

### `api/` — page/token budget & `Guardrails` placeholder

Do **not** invent extra public types beyond the above. The facade class body is a Task 10/11 slot;
keep it compiling with stub `// TODO Task 10/11` throws so the module never breaks downstream.

## Acceptance criteria

- Every listed public type + signature exists at the listed path; `./gradlew :data-privacy-core:build`
  green.
- All fields `final`/immutable; config records have the documented static `DEFAULTS`; GuardrailsConfig
  builder chainable; `Guardrails` facade compiles with stub bodies.
- No third-party imports beyond `java.util`, `java.util.regex`, `java.io.Stream`-family types (core is
  zero-dependency).

## Hand-off to next task (log in 00-HANDOFF.md)

- Full file list with paths + every public signature added (so tasks 03–13 never re-read this file).
- The `pii_`-prefix `type()` values you chose for each `PiiEntity` (they drive `entityType` strings
  in GuardrailResult). If Spring AI is not resolved yet, note it; Task 12 owns it.
- Next tasks depend on: 03 (redaction) needs `GuardrailResult`/`GuardrailCheck` defaults; 04 needs
  `PiiEntity`; 05 needs `SecretPreset` + `SecretConfig` + `GuardrailResult`; 06 needs `UrlsCheck`-contract
  records (`UrlsConfig`, `KeywordsConfig`, `GuardrailResult`); 07 needs `PiiConfig`/`CustomRegexConfig`;
  08 needs `LlmClassifier` + `LlmCheckConfig` + `GuardrailResult` + `GuardrailsConfig.builder()`.