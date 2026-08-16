# Task 09 — Authenticator SPI + generic auth (`auth/`)

## Objective

Implement the **authentication injection** SPI (R10 / §6.3): the `Authenticator` interface,
`AuthResult`, and the `GenericAuthenticator` that injects header/query/basic auth from a resolved
credential's fields. This is the auth branch of the pipeline (§4) for non-OAuth2 credentials.

## Hand-off context

- **Design doc:** §2 (`auth/` tree), §6.3 (`Authenticator` + `GenericAuthenticator`), §4 (pipeline
  "generic credential → GenericAuthenticator"), §5 R10.
- **Already done (prior tasks):**
  - 01 `spec/`: `CredentialRef`.
  - 02 `transport/`: `HttpRequest` + builder.
  - 05 `error/`: `HttpApiException`.
  - 08 `auth/credential/`: `DecryptedCredential`, `CredentialStore`.
- **Package:** `io.github.khezyapp.dhttp.auth`.
- **Conventions:** `@FunctionalInterface` for single-method; `final` classes. Read
  `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/auth/`)

1. `Authenticator.java` — `public interface` (per §6.3):
   `HttpRequest apply(DecryptedCredential credential, HttpRequest request, AuthResult out)`.
   (Mark as `@FunctionalInterface` if single abstract method.)
2. `AuthResult.java` — small mutable carrier for side-channel info the authenticator sets (e.g.
   whether auth was applied, the credential id used, token expiry). Provide
   `boolean applied()`, `String credentialId()`, `Instant tokenExpiresAt()`.
3. `GenericAuthenticator.java` — `public final class implements Authenticator` (R10):
   - **header:** if credential fields contain `headerName` + `value` (e.g. `api-key` type → inject
     `headerName: value`), set the header.
   - **basic:** if fields contain `username` + `password` (e.g. `basic-auth`), set
     `Authorization: Basic base64(username:password)` (via `HttpRequest.Auth.BasicAuth`).
   - **http-header:** if fields is a `headers` map (`http-header` type), merge all headers.
   - Any credential type is supported via field inspection — no hardcoded type→behavior mapping beyond
     the typed config conventions from Task 08.

## Design notes

- **No secrets in spec:** auth values always come from `DecryptedCredential.fields()`/`data`, never
  from the request spec (R12/§7.5).
- Reuse `HttpRequest.Builder`/`toBuilder()` to add auth/headers and return a new request (immutable).
- Support the three built-in config records from Task 08: `HeaderApiKeyCredentials`,
  `BasicAuthCredentials`, `HttpHeaderCredentials`. The `oauth2` type is handled by Task 10.
- Do not log the secret values.

## Acceptance criteria

- Compiles + Checkstyle green.
- `GenericAuthenticatorTest`:
  - `api-key` credential → request gets `headerName: value`.
  - `basic-auth` credential → request gets `Authorization: Basic <base64>`.
  - `http-header` credential → all headers merged.
  - `AuthResult.applied()==true` and `credentialId` set.
  - No secret value appears in `request.toString()`/headers log output (assert redaction or absence).
- No unused imports; `final` everywhere.

## Hand-off to next task

Task 10 (OAuth2) provides `OAuth2RequestAuthenticator implements Authenticator` and the pipeline
branch that detects an `oauth2` credential type and delegates to it. Task 13 wires the `Authenticator`
selection: none / generic / oauth2. Keep `apply(credential, request, out)` signature stable.
