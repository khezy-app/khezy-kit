# Task 05 — Error model (`error/`)

## Objective

Implement the **structured error model** (R13): the runtime `HttpApiException`, a `HttpErrorFactory`
for building them, and the config-time `OAuth2NotConfiguredException`. Every other task throws and
catches these, so this is a foundational leaf.

## Hand-off context

- **Design doc:** §2 (`error/` tree), §5 R13, §6.6 (`OAuth2NotConfiguredException` config-time
  contract), §7 (secrets redacted in exceptions).
- **Already done (prior tasks):** none — leaf task. Task 02's `HttpTransport.send` signature throws
  `HttpApiException`; Task 08/10 use `OAuth2NotConfiguredException`; Task 13 wraps pipeline errors.
- **Package:** `io.github.khezyapp.dhttp.error`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/error/`)

1. `HttpApiException.java` — `public class HttpApiException extends RuntimeException` carrying:
   - `int status` (HTTP status, or a sentinel e.g. `-1` for non-HTTP),
   - `String operationId`,
   - `int itemIndex`,
   - the cause (`Throwable`).
   Provide overloaded constructors + accessors. Structured fields enable R13.
2. `HttpErrorFactory.java` — `public final class` (static factory) building `HttpApiException`
   instances:
   - `of(int status, String operationId, int itemIndex, String message)`
   - `of(int status, String operationId, int itemIndex, String message, Throwable cause)`
   - `http(String operationId, int itemIndex, HttpResult result)` — derive status + message from a
     non-2xx `HttpResult` (import from Task 02; build 02 first or reference it).
   Center the redaction hook here so exception messages never leak Authorization/tokens.
3. `OAuth2NotConfiguredException.java` — `public final class OAuth2NotConfiguredException extends RuntimeException`
   with `String credentialId`; default message:
   `"oauth2 credential '<id>' is not yet configured"`. Used by `engine.validate` / `OAuth2AuthorizationFlow.validate`.

## Design notes

- **Redaction:** exceptions may embed request context; strip any `Authorization` header / token
  values before putting them in messages (defer full `SecretRedactor` to Task 06, but keep the
  factory the single place errors are built so redaction can be applied centrally).
- Runtime exception (unchecked) — transport/engine throw it without checked-exception ceremony.
- Keep it small; no external deps.

## Acceptance criteria

- Compiles + Checkstyle green.
- `HttpErrorFactoryTest`:
  - `of(401, "op", 3, "msg")` yields an exception whose `getStatus()==401`, `getOperationId()=="op"`,
    `getItemIndex()==3`.
  - `http(...)` from a `HttpResult` with status `404` yields a message containing the status and a
    non-2xx flag.
  - `OAuth2NotConfiguredException` default message contains the credentialId; accessor returns it.
- No unused imports; `final` everywhere.

## Hand-off to next task

All later tasks `throw new HttpApiException`/`HttpErrorFactory.of(...)`. `HttpTransport.send`
declares `throws HttpApiException`. `OAuth2NotConfiguredException` is thrown by config-time validate.
