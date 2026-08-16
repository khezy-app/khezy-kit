# Task 13 — Engine + pipeline (`engine/`)

## Objective

Implement the **deterministic execution engine** (R13, R16, R15): `Pipeline` (preSend → auth →
transport → postReceive, with pagination + maxResults capping), `OutputRecord`, `HttpResult`
(re-exported), and `DeclarativeHttpEngine` (validate / execute / describe). This orchestrates every
previous task into the §4 pipeline.

## Hand-off context

- **Design doc:** §2 (`engine/` tree), §3.3 (`DeclarativeHttpEngine` validate/execute/describe), §4
  (pipeline mermaid), §5 R6, R7, R8, R11, R13, R15, R16.
- **Already done (prior tasks):**
  - 07 `plan/`: `RequestContext`, `RequestPlan`, `RequestPlanner`, `FragmentMerger`,
    `ConditionEvaluator`.
  - 09 `auth/`: `Authenticator`, `GenericAuthenticator`, `AuthResult`.
  - 10 `auth/oauth2/`: `OAuth2RequestAuthenticator`, `OAuth2AuthorizationFlow`, `OAuth2TokenClient`.
  - 11 `action/`: `PreSendAction`, `PostReceiveAction`, `ActionRegistry`, built-ins.
  - 12 `pagination/`: `PaginationStrategy`, `OffsetPagination`, `CursorPagination`, `PaginationContext`.
  - 06 `security/`: `SsrfGuard`, `DomainAllowList`, `RedirectPolicy`, `SecretRedactor`.
  - 02 `transport/`: `HttpTransport`, `HttpResult`, `HttpRequest`.
- **Package:** `io.github.khezyapp.dhttp.engine`.
- **Conventions:** stateless engine (per-call `RequestContext`); injected dependencies. Read
  `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/engine/`)

1. `OutputRecord.java` — record: `record OutputRecord(Map<String,Object> json, byte[] binary, Map<String,Object> metadata, boolean isBinary)` with factories `OutputRecord.ofJson(Map)`, `OutputRecord.ofBinary(byte[])`. (Referenced by `RequestContext.item` and `RequestPlan`/actions.)
2. `HttpResult.java` — **re-export** the `transport.HttpResult` (or import it) so callers have one
   name; prefer importing `io.github.khezyapp.dhttp.transport.HttpResult` (no duplicate class).
3. `Pipeline.java` — orchestrates one operation's execution:
   `List<OutputRecord> run(RequestPlan plan, RequestContext ctx, HttpTransport transport, Authenticator auth, CredentialStore store)`:
   - apply `PreSendAction`s to the request (§4 preSend hooks);
   - select auth: none / generic / oauth2 (detect credential type from `plan.authRequest`), inject via
     `Authenticator.apply`;
   - call transport (wrapping `HttpApiException` via `HttpErrorFactory`);
   - apply `PostReceiveStep`s in order (materialized from `Route.output.postReceive` via `ActionRegistry`);
   - cap by `plan.maxResults`;
   - if `plan.pagination != null`, loop `send → collect → shouldPaginate → nextRequest`.
   - wrap each send with `SsrfGuard`/`DomainAllowList`/`RedirectPolicy` (Task 06) and redact via
     `SecretRedactor`.
4. `DeclarativeHttpEngine.java` — `public final class`, injected with dependencies
   (`RequestPlanner`, `ActionRegistry`, `ConditionEvaluator`, `CredentialStore`, `HttpTransport`,
   `ExpressionEvaluator`).
   - `void validate(HttpRequestSpec spec)` — config-time: resolve `defaultCredential`/operation
     credential refs; for OAuth2 → `OAuth2AuthorizationFlow.validate()`; no valid token ⇒ throw
     `OAuth2NotConfiguredException` BEFORE any request. Non-OAuth2 ⇒ only check the ref resolves.
   - `List<OutputRecord> execute(HttpRequestSpec spec, RequestContext ctx)` — select active operation
     (preconditions), `RequestPlanner.plan`, run `Pipeline`, wrap errors (R13), respect continueOnError.
   - `List<OptionItem> describe(HttpRequestSpec spec, RequestContext ctx, String loadKey)` — design-time
     mode: reuse the same planner, run an option-shaping postReceive, return `OptionItem` list (R15).

## Design notes

- **Stateless engine (principle 7 / R16):** hold no mutable state; all inputs in `RequestContext`.
  Deterministic given same spec+context+transport.
- **Auth wiring:** add a small `AuthRequest` record in `plan/` or `auth/` that the planner fills from
  `spec.defaultCredential`/operation override: `record AuthRequest(CredentialRef ref, String type)`.
  The engine/pipeline maps `type=="oauth2"` → `OAuth2RequestAuthenticator`, else `GenericAuthenticator`,
  else none.
- **Config-time vs request-time:** `validate` only touches `CredentialStore`/`TokenStore`, never the
  transport — so a missing token throws with zero HTTP calls (§6.6).
- **`describe` (R15):** option-shaping is a postReceive whose output items become `OptionItem`
  records (`record OptionItem(String name, String value)`).
- **Security:** guard every `transport.send`; redact secrets in any wrapped exception message.

## Acceptance criteria

Compiles + Checkstyle green. Tests use `FakeTransport` + in-memory stores:

- **execute (R16/§9 item 2):** a Brevo-style spec against a mocked transport asserts the **exact
  resolved `HttpRequest`** (method, path, headers, body) and the **exact output records** after
  `RootProperty`.
- **validate (R10/§9 item 6c):** with no stored token, `validate(spec)` throws
  `OAuth2NotConfiguredException` and FakeTransport has zero sends; with a valid token it passes.
- **pagination (R9/§9 item 3):** offset pagination with `pageSize` + `maxResults` capping yields the
  expected total and request count.
- **describe (R15/§9 item 5):** returns shaped `OptionItem`s for a dropdown.
- **errors (R13):** a 404 from the transport produces an `HttpApiException` with status/operationId.
- No unused imports; `final` everywhere; method length < 150.

## Hand-off to next task

Task 14 (`config/`) assembles `DeclarativeHttpConfig` and the `DeclarativeHttp` facade wiring an
engine from defaults. Keep `DeclarativeHttpEngine`'s constructor/`validate`/`execute`/`describe`
signatures stable.
