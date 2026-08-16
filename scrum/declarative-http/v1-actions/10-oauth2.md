# Task 10 — OAuth2 two-phase config + token lifecycle (`auth/oauth2/`)

## Objective

Implement the **OAuth2 client** (R10 / §6.6): two-phase configuration (Phase A credential flow +
Phase B spec references with config-time validation) and the request-time token lifecycle
(acquire, persistent `TokenStore`, refresh-on-expiry, refresh-on-401 with single-flight). This is
the most complex single vertical slice.

## Hand-off context

- **Design doc:** §2 (`auth/oauth2/` tree), §6.6 (entire section: two phases, state diagram,
  component responsibilities, `OAuth2AuthorizationFlow`, `OAuth2RequestAuthenticator`, `TokenStore`,
  concurrency/safety), §5 R10, §7 (tokens always redacted).
- **Already done (prior tasks):**
  - 02 `transport/`: `HttpRequest`/builder, `HttpResult`, `Body`, `HttpTransport` — **reused for raw
    token-endpoint calls** so SSRF/TLS stay consistent.
  - 03 `json/`: `JsonMapper` (parse token JSON response).
  - 05 `error/`: `OAuth2NotConfiguredException`, `HttpApiException`.
  - 08 `auth/credential/`: `OAuth2Credentials` (create/refine it here), `DecryptedCredential`,
    `CredentialStore`, `EncryptedPayload`.
  - 09 `auth/`: `Authenticator`, `AuthResult`.
- **Package:** `io.github.khezyapp.dhttp.auth.oauth2`.
- **Conventions:** `final` classes, immutable token record, per-credential lock. Read
  `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/auth/oauth2/`)

1. `OAuth2Grant.java` — enum `AUTHORIZATION_CODE, CLIENT_CREDENTIALS, PASSWORD, REFRESH_TOKEN`.
   (Refine `OAuth2Credentials.grantType` in Task 08 to use this type.)
2. `OAuth2Token.java` — `record OAuth2Token(String accessToken, String refreshToken, long expiresIn, Instant expiresAt, String scope)` with `boolean isExpired()` / `isExpired(long skewMillis)`.
3. `TokenStore.java` — SPI: `Optional<OAuth2Token> load(String credentialId)`, `void save(String credentialId, OAuth2Token token)`, `void clear(String credentialId)`.
4. `InMemoryTokenStore.java` — default `ConcurrentHashMap` impl.
5. `OAuth2TokenClient.java` — performs raw token-endpoint calls (code exchange, client-credentials,
   password, refresh) by reusing `HttpTransport`/`HttpRequest`. Parses the JSON response into an
   `OAuth2Token`. JEXL-free. Methods:
   - `OAuth2Token exchangeAuthorizationCode(OAuth2Credentials creds, String code, String redirectUri)`
   - `OAuth2Token clientCredentials(OAuth2Credentials creds)`
   - `OAuth2Token password(OAuth2Credentials creds, String username, String password)`
   - `OAuth2Token refresh(OAuth2Credentials creds, String refreshToken)`
6. `OAuth2AuthorizationFlow.java` — **config-time (Phase A)**:
   - `static OAuth2AuthorizationFlow create(OAuth2Credentials creds)`.
   - `String authorizationUrl()` — builds provider URL carrying `client_id`, `redirect_uri`, `scope`,
     `state` (generated per call).
   - `OAuth2Token exchangeCode(String code)` — via `OAuth2TokenClient`.
   - `void persist(OAuth2Token token)` — `TokenStore.save(credentialId, token)`.
   - `OAuth2Token validate()` — load from `TokenStore`; present + not expired → return, else throw
     `OAuth2NotConfiguredException(credentialId)`.
7. `OAuth2RequestAuthenticator.java` — **request-time orchestrator implements `Authenticator`**:
   - `OAuth2Token tokenFor(String credentialId, OAuth2Credentials creds, OAuth2Grant grant)`
     (cache-hit → no I/O; absent + client-credentials/password → acquire fresh; expired → refresh).
   - `HttpRequest authenticate(DecryptedCredential credential, HttpRequest request)` — inject
     `Authorization: Bearer <token>`.
   - `HttpResult retryOn401(DecryptedCredential credential, HttpRequest request, HttpTransport transport)`
     — send; on 401 clear store, refresh **once** (single-flight), replay once; repeated 401 → throw
     `HttpApiException(401)`.
   - **Single-flight:** per-credential `ReentrantLock` (or memoized `CompletableFuture`) so N
     concurrent requests trigger ONE refresh.

## Design notes

- **Two-phase contract (§6.6):** config-time `validate()` is the enforcement point for
  `OAuth2NotConfiguredException` — no access token ⇒ not yet configured ⇒ throw BEFORE any request.
- **Token-endpoint reuse:** `OAuth2TokenClient` must use the same `HttpTransport`/`HttpRequest`
  plumbing so SSRF guards + TLS policy apply uniformly.
- **Concurrency:** only `OAuth2RequestAuthenticator` is stateful (token cache + refresh lock); the
  engine stays stateless. Use a `ConcurrentHashMap<String, ReentrantLock>` or
  `ConcurrentHashMap<String, CompletableFuture<OAuth2Token>>` keyed by `credentialId`.
- **Redaction (§7.4):** `access_token`/`refresh_token` are always matched by `SecretRedactor` in logs
  and exceptions (see Task 06).

## Acceptance criteria

Compiles + Checkstyle green. Tests map §9 items 6 & 7 (use a `FakeTransport` from Task 02 test scope
that records every `HttpRequest` and returns scripted `HttpResult`s):

- **Config-time (item 6):**
  - `authorizationUrl()` carries `client_id`, `redirect_uri`, `scope`, `state`.
  - A consumer-style callback: `exchangeCode(code)` → `persist(token)` puts a token in the store.
  - `validate()` passes with a valid stored token; throws `OAuth2NotConfiguredException` when none —
    **no HTTP request sent** (assert FakeTransport had zero sends).
- **Request-time (item 7):**
  - (a) warm token reused with **no** token-endpoint call (FakeTransport call count unchanged).
  - (b) expired token triggers a `refresh_token` grant.
  - (c) 401 on a warm token triggers a single refresh + retry.
  - (d) concurrent requests share ONE refresh (single-flight) — spawn N threads, assert exactly one
    refresh token call.
- No unused imports; `final` everywhere; method length < 150 (split helpers).

## Hand-off to next task

Task 13 (engine) selects auth: `none` / generic (`GenericAuthenticator`) / oauth2
(`OAuth2RequestAuthenticator`), calls `engine.validate(spec)` for config-time, and wraps request-time
with `retryOn401`. Keep `tokenFor`, `authenticate`, `retryOn401`, and `OAuth2AuthorizationFlow`
methods stable.
