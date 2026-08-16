# Task 08 — Credential CRUD + encryption (`auth/credential/`)

## Objective

Implement the **unified credential configuration & management** layer (R10 / §6.2): `CredentialService`
CRUD (Map + type-safe overloads), the `CredentialRepository` SPI + in-memory default, the encrypted
`StoredCredential` model, the `CredentialCipher`/`KeyProvider` encryption SPI + AES-GCM default, the
engine-facing `CredentialStore`, and the typed config records. The engine must never touch CRUD — it
only calls `CredentialStore.resolve`.

## Hand-off context

- **Design doc:** §2 (`auth/credential/` tree), §6.2 (full API + round-trip flow + encryption +
  repository + wiring), §5 R10, §7 item 6 (encrypted at rest).
- **Already done (prior tasks):**
  - 03 `json/`: `JsonMapper` + `JacksonJsonMapper` (enables type-safe overloads).
  - 05 `error/`: `OAuth2NotConfiguredException` not needed here but `CredentialService` may throw
    structured errors.
- **Package:** `io.github.khezyapp.dhttp.auth.credential` and `.type`.
- **Conventions:** records for models, interface SPIs, `final` service. Read
  `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create

`src/main/java/io/github/khezyapp/dhttp/auth/credential/`:
1. `CredentialStore.java` — engine read-side SPI (§6.2):
   `Optional<DecryptedCredential<?>> resolve(CredentialRef ref, RequestContext ctx)`.
   (Import `RequestContext` from Task 07, `CredentialRef` from Task 01.)
2. `CredentialService.java` — `public final class` (§6.2 full signature): `create(type, Map)` /
   `create(type, T)`, `get(id)` / `get(id, Class<T>)`, `list()`, `update(...)` Map + typed,
   `delete(id)`, and internal `resolve(CredentialRef ref)`. Plus `asStore()` returning a
   `CredentialStore` delegating to `resolve`. Owns the encrypt/decrypt round-trip.
3. `CredentialRepository.java` — `public interface`: `findById`, `findAll`, `save` (upsert),
   `deleteById`.
4. `InMemoryCredentialRepository.java` — default `ConcurrentHashMap` impl.
5. `StoredCredential.java` — `record StoredCredential(String id, String type, Map<String,Object> data, Instant createdAt, Instant updatedAt)` — `data` is the encrypted payload as Map (never plaintext).
6. `DecryptedCredential.java` — `record DecryptedCredential<T>(String id, String type, Map<String,Object> fields, T data)` + `fields()` accessor.
7. `CredentialSummary.java` — `record CredentialSummary(String id, String type)` (never secrets).
8. `EncryptedPayload.java` — `record EncryptedPayload(String algorithm, String iv, String ciphertext)`
   with `fromMap(Map)` / `toMap()` for storage inside `StoredCredential.data`.
9. `CredentialCipher.java` — `public interface`: `EncryptedPayload encrypt(Map<String,Object> plaintext)`,
   `Map<String,Object> decrypt(EncryptedPayload payload)`.
10. `AesGcmCredentialCipher.java` — default JDK AES-256-GCM: random IV per credential,
    payload `{ algorithm, iv, ciphertext }` (base64).
11. `KeyProvider.java` — `public interface { SecretKey key(); }` — the consumer supplies the key;
    the core never generates/stores/defaults it.
12. `CredentialType.java` — marker `record CredentialType(String name, Class<?> dataType)`.

`src/main/java/io/github/khezyapp/dhttp/auth/credential/type/`:
13. `OAuth2Credentials.java` — `record OAuth2Credentials(String clientId, String clientSecret, String tokenUrl, String authorizationUrl, String redirectUri, String scope, OAuth2Grant grantType, Map<String,Object> extraBodyParams)`. (`OAuth2Grant` from Task 10 — until then use a `String grantType` or a local enum promoted in Task 10.)
14. `HeaderApiKeyCredentials.java` — `record HeaderApiKeyCredentials(String headerName, String value)`.
15. `BasicAuthCredentials.java` — `record BasicAuthCredentials(String username, String password)`.
16. `HttpHeaderCredentials.java` — `record HttpHeaderCredentials(Map<String,String> headers)`.

## Design notes

- **Round-trip (§6.2):**
  - create (typed): `Map<String,Object> props = jsonMapper.toMap(config);` →
    `EncryptedPayload enc = cipher.encrypt(props);` → `StoredCredential(id, type, enc.toMap(), now, now);`
    → `repository.save(stored)`.
  - get (typed): `repository.findById(id)` → `cipher.decrypt(fromMap(stored.data()))` → `Map` →
    `new DecryptedCredential<>(id, type, plain, jsonMapper.fromMap(plain, type))`.
  - Map-form and typed-form converge on identical stored payloads (typed = `toMap` up front).
- **`CredentialService` constructor:** `new CredentialService(repository, cipher, jsonMapper)`.
- **`asStore()`:** returns a `CredentialStore` whose `resolve(ref, ctx)` calls internal
  `resolve(ref)` — engine never sees CRUD methods.
- **Key safety (§7.6):** key comes only from consumer `KeyProvider`; never default it.
- `list()` returns `List<CredentialSummary>` only.

## Acceptance criteria

- Compiles + Checkstyle green.
- `CredentialServiceTest` (maps §9 item 8):
  - `create("oauth2", config)` stores **no plaintext** — `StoredCredential.data()` contains only the
    `EncryptedPayload` (algorithm/iv/ciphertext), no field name→value pairs.
  - `get(id, OAuth2Credentials.class)` round-trips decrypt → Map → typed config equal to input.
  - Map-form and type-safe `create` produce **identical** stored payloads (byte/string-equal ciphertext).
  - `update`/`delete` mutate the repository.
  - `list()` exposes only id + type.
  - A consumer-supplied `KeyProvider` key drives the cipher; a wrong key on decrypt throws (proves key
    is not inside the core).
  - `CredentialStore.resolve(ref, ctx)` (via `asStore()`) delegates to the service.
- No unused imports; `final` everywhere; method length < 150.

## Hand-off to next task

Task 09 (`Authenticator`/`GenericAuthenticator`) consumes `DecryptedCredential` and `CredentialStore`;
Task 10 (OAuth2) consumes `OAuth2Credentials`, `EncryptedPayload` conventions, and `KeyProvider`.
Keep `asStore()` and the constructor signature stable.
