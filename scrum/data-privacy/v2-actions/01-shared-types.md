# Task 01 — Shared types: scopes, modes, reports, exceptions

## Objective

Deliver the vocabulary the two advisors (Tasks 02/03) compile against: the `ProtectionScope` and
`RedactMode` enums, the `RedactionReport` / `GuardrailReport` observability records, and the
`exception/` package (base `DataPrivacyException` + three subclasses). This is the contract task —
everything downstream depends on these exact signatures.

## Hand-off context

- **Design doc:** §6 (module layout), §7 (public API surface), §8.9 (exception reference),
  §9 (observability).
- **From earlier tasks:** none — this is the first task. Module + deps already exist (INDEX
  "Modules & conventions"); **do not touch `build.gradle`/`settings.gradle`/version (1.0.0)**.
- **Resolved decisions (INDEX, apply verbatim):** **R1** — no `confidence` field anywhere (core
  does not expose it). **R9** — not applicable here (no chain param in these types).
- **Design notes:**
  - `RedactionReport.NONE` is the only factory constant (design §7 shows no `of(...)` factory —
    the executor builds reports from `GuardrailsOutcome` fields in Task 02).
  - Exceptions live in the **`exception` sub-package** (design §6); reports/enums stay flat in
    `io.github.khezyapp.dpriv.springai` (design §7 note).
  - `DataPrivacyException` is a **non-final** base class; the three subclasses are `final`.
  - Exception messages: derive human-readable text inside the constructors (no externalized
    strings needed; library does not log).

## Files to create

All under `securities/data-privacy-spring-ai/src/main/java/io/github/khezyapp/dpriv/springai/`:

### 1. `ProtectionScope.java`

```java
package io.github.khezyapp.dpriv.springai;

/**
 * Where an advisor applies (design §7). Shared by both advisors.
 */
public enum ProtectionScope {
    INPUT,    // user messages only (default for both advisors)
    OUTPUT,   // model response only
    BOTH      // both directions
}
```

### 2. `RedactMode.java`

```java
package io.github.khezyapp.dpriv.springai;

/**
 * Which user messages DataPrivacyAdvisor redacts in before() (design §7).
 */
public enum RedactMode {
    ALL,        // every USER message in the prompt, incl. history (default)
    LAST_ONLY   // only the last USER message (perf opt-in)
}
```

### 3. `RedactionReport.java`

```java
package io.github.khezyapp.dpriv.springai;

import java.util.Set;

/**
 * Observability payload written by DataPrivacyAdvisor to the request context (design §7, §9).
 */
public record RedactionReport(boolean redacted, Set<String> entityTypes) {
    public static final RedactionReport NONE = new RedactionReport(false, Set.of());
}
```

### 4. `GuardrailReport.java`

```java
package io.github.khezyapp.dpriv.springai;

/**
 * Observability payload written by GuardrailAdvisor on the pass path only (design §7, §9).
 * A violation is carried by PolicyViolationException instead. No confidence — see INDEX R1.
 */
public record GuardrailReport(boolean passed, String entityType) {}
```

### 5. `exception/DataPrivacyException.java`

```java
package io.github.khezyapp.dpriv.springai.exception;

/**
 * Base class for all advisor failures (design §8.9). Consumers may catch this type to handle
 * any data-privacy failure uniformly.
 */
public class DataPrivacyException extends RuntimeException {
    public DataPrivacyException(String message) {
        super(message);
    }

    public DataPrivacyException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 6. `exception/RedactionException.java`

```java
package io.github.khezyapp.dpriv.springai.exception;

/**
 * The SANITIZE pipeline failed (a check errored) and failOnError=true: the request was aborted
 * before the model call — unredacted text was never sent (design §8.4, G10).
 */
public final class RedactionException extends DataPrivacyException {
    public RedactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 7. `exception/PolicyViolationException.java`

```java
package io.github.khezyapp.dpriv.springai.exception;

import io.github.khezyapp.dpriv.springai.ProtectionScope;

/**
 * A classifier flagged the input or output (detected, above threshold) (design §8.9, G14).
 * Never bypassable: there is no configuration that lets a detected violation through.
 */
public final class PolicyViolationException extends DataPrivacyException {

    private final String entityType;
    private final ProtectionScope scope;

    public PolicyViolationException(String entityType, ProtectionScope scope) {
        super("policy violation detected: " + entityType + " (scope=" + scope + ")");
        this.entityType = entityType;
        this.scope = scope;
    }

    public String entityType() {
        return entityType;
    }

    public ProtectionScope scope() {
        return scope;
    }
}
```

### 8. `exception/GuardrailEvaluationException.java`

```java
package io.github.khezyapp.dpriv.springai.exception;

/**
 * The judge itself failed (LLM unreachable, malformed verdict) and failOnError=true (design §8.8,
 * G15). Infra problem — retry with backoff may be appropriate.
 */
public final class GuardrailEvaluationException extends DataPrivacyException {
    public GuardrailEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## Tests (JUnit 5 + AssertJ, no Mockito)

File: `src/test/java/io/github/khezyapp/dpriv/springai/AdvisorTypesTest.java` (+
`exception/AdvisorExceptionsTest.java` if preferred — split is the executor's choice, both paths
declared here are allowed).

- `protectionScopeHasExactlyInputOutputBoth` — enum values `INPUT`, `OUTPUT`, `BOTH` in that order.
- `redactModeHasExactlyAllLastOnly` — enum values `ALL`, `LAST_ONLY`.
- `redactionReportNoneIsCleanEmptyReport` — `RedactionReport.NONE` → `redacted()==false`,
  `entityTypes()` empty and immutable (immutability of `Set.of()` — adding throws
  `UnsupportedOperationException`).
- `policyViolationExceptionCarriesEntityTypeAndScope` — `new PolicyViolationException("jailbreak",
  ProtectionScope.INPUT)` → `entityType()=="jailbreak"`, `scope()==ProtectionScope.INPUT`,
  `getMessage()` contains both `"jailbreak"` and `"INPUT"`; instance-of `DataPrivacyException`.
- `redactionExceptionIsDataPrivacyException` — `RedactionException` is catchable as
  `DataPrivacyException`; message + cause round-trip.
- `guardrailEvaluationExceptionIsDataPrivacyException` — same for `GuardrailEvaluationException`.

## Acceptance criteria

- `./gradlew :data-privacy-spring-ai:build` → BUILD SUCCESSFUL (compile + tests + Checkstyle green).
- No `build.gradle`/`settings.gradle` edits; version still `1.0.0`.
- Public surface is exactly the 8 types above — no extra public members.

## Hand-off to next task (log in 00-HANDOFF.md)

- As-built signatures of all 8 types (paste the final declarations).
- Confirm the two packages (`io.github.khezyapp.dpriv.springai`, `...springai.exception`) compile
  under the module's existing Checkstyle rules — flag any rule surprises (e.g. record Javadoc).
- State that Tasks 02/03 may compile against these types as-is.
