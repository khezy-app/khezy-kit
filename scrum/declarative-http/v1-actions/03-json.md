# Task 03 — JSON / object-mapper SPI (`json/`)

## Objective

Implement the **`JsonMapper`** SPI (Map ↔ typed ↔ JSON string) and the **Jackson 3 default**.
This is what enables the credential service's type-safe overloads (record → Map → encrypt,
decrypt → Map → record) and later response parsing.

## Hand-off context

- **Design doc:** §2 (`json/` tree), §6.2 "JSON / object-mapper SPI" (`JsonMapper` interface +
  Jackson default), §5 R10.
- **Already done (prior tasks):** none — leaf task. Consumer is Task 08 (credential CRUD) primarily,
  and 13 (engine) for response shaping.
- **Package:** `io.github.khezyapp.dhttp.json`.
- **Dependency:** `tools.jackson.core:jackson-databind:3.2.1` is already declared
  (`implementation` scope) in `build.gradle`. Do not change it.
- **Conventions:** `@FunctionalInterface`-style SPI + final class default. Read
  `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/json/`)

1. `JsonMapper.java` — public interface:
   - `Map<String,Object> toMap(Object value)`
   - `<T> T fromMap(Map<String,Object> map, Class<T> type)`
   - `String write(Object value)`
   - `<T> T read(String json, Class<T> type)`
2. `jackson3/JacksonJsonMapper.java` — `public final class JacksonJsonMapper implements JsonMapper`
   delegating to a Jackson 3 `ObjectMapper` (package `tools.jackson.databind.ObjectMapper`).
   Thread-safe singleton-friendly: hold a `private final ObjectMapper mapper`; make the default
   constructor use a configured `ObjectMapper` (record support, JSON via `JsonMapper.builder()`).
   Provide `JacksonJsonMapper.INSTANCE` or a static `defaultInstance()`.

## Design notes

- **Jackson 3 is `tools.jackson.*`**, not the legacy `com.fasterxml.*`. Use
  `tools.jackson.databind.ObjectMapper`, `tools.jackson.databind.json.JsonMapper`.
- **Record support** must be on (Jackson 3 enables it by default) so `OAuth2Credentials` and other
  record configs round-trip via `toMap`/`fromMap`.
- Map values may be `String`, `Number`, `Boolean`, nested `Map`/`List` — do not assume flat.

## Acceptance criteria

- Compiles + Checkstyle green under `./gradlew :declarative-http:build`.
- `JacksonJsonMapperTest`: define a record `Sample(String name, int count, Map<String,Object> extra)`;
  - `write` then `read` round-trips equal value.
  - `toMap(record)` yields a `Map` with the record fields; `fromMap(map, Sample.class)` reconstructs.
  - `toMap`/`fromMap` round-trip a nested `Map`/`List`.
- Empty/absent handling: `fromMap` with null-safe map is tolerant (no NPE).
- No unused imports; `final` everywhere.

## Hand-off to next task

Task 08 injects a `JsonMapper` into `CredentialService`. Keep the interface small and stable —
three implementations must be able to plug in (Jackson default + consumer custom).
