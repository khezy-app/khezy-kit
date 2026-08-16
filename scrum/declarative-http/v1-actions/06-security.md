# Task 06 — Security-first behaviors (`security/`)

## Objective

Implement the **non-negotiable security contract** (§7 / R12): SSRF guard, domain allow-list,
cross-origin redirect policy, and secret redaction. These run inside the transport/engine path so
every request is safe by default.

## Hand-off context

- **Design doc:** §2 (`security/` tree), §7 (contract items 1–4), §5 R12.
- **Already done (prior tasks):** none — leaf task. Task 02 transports a `SecurityPolicy`/domain
  list; Task 13 (engine) invokes these guards before send. This task defines the guards only.
- **Package:** `io.github.khezyapp.dhttp.security`.
- **Conventions:** `final` classes, pure functions. Read `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/security/`)

1. `SsrfGuard.java` — `public final class`:
   - `void validate(String url, List<String> allowedDomains, boolean allowIpLiteral)` — checks the
     URL host against allowed domains; validates direct IP literals without DNS. Throws
     `HttpApiException` (from Task 05) on violation.
   - A `boolean allows(String host, List<String> allowedDomains)` helper.
   - Do DNS resolution check: resolve the host and verify every resolved address is within the
     allow-list. Direct IP literal is validated directly (no DNS).
2. `DomainAllowList.java` — `public final class`: `boolean isAllowed(String host, List<String> domains)`
   (exact + subdomain matching); `void requireAllowed(...)`.
3. `RedirectPolicy.java` — `public final class`:
   - `boolean shouldStripCredentials(String originalUrl, String redirectUrl)` — true when the
     redirect is cross-origin (scheme/host/port differ) and cross-origin stripping is the default.
   - Honors an `allowCrossOriginCredentials` opt-in flag (strip unless explicitly opted in).
4. `SecretRedactor.java` — `public final class`:
   - `String redact(String text, List<String> secrets)` — replaces each secret occurrence with `***`.
   - Convenience to extract secret-like values from a map (e.g. `password`, `token`, `Authorization`)
     so messages/logs never leak them.
   - `public static SecretRedactor get()` singleton.

## Design notes

- **SSRF (contract 1):** validate on URL AND every resolved DNS address; direct IP literals
  validated without DNS. This is the strictest part — implement host parsing via `java.net.URI`.
- **Domain allow-list (contract 2):** enforced before send, from the resolved credential's
  `allowedDomains` (Task 01 `SecurityPolicy`).
- **Redirect (contract 3):** cross-origin strips credentials unless explicitly opted in; default off
  for cross-origin.
- **Redaction (contract 4):** Authorization headers, credential fields, OAuth2 access/refresh tokens,
  and `sensitiveOutputFields` are matched by `SecretRedactor` in logs and exceptions.
- Pure/JDK-only; no third-party deps.

## Acceptance criteria

- Compiles + Checkstyle green.
- `SsrfGuardTest`: allowed host passes; disallowed host throws; IP literal inside allow-list passes,
  outside throws.
- `DomainAllowListTest`: exact + subdomain matching.
- `RedirectPolicyTest`: same-origin does NOT strip; cross-origin strips by default; explicit opt-in
  does not strip.
- `SecretRedactorTest`: a message containing an access token is redacted to `***`; plain text
  without secrets is unchanged.
- No unused imports; `final` everywhere; method length < 150.

## Hand-off to next task

Task 13 (engine) invokes `SsrfGuard`/`DomainAllowList`/`RedirectPolicy` before `transport.send` and
wraps transport/error strings with `SecretRedactor`. Keep method names obvious (`validate`, `redact`,
`shouldStripCredentials`, `isAllowed`).
