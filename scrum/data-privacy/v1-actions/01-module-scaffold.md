# Task 01 — Module scaffold (core + Spring AI adapter) & root wiring

## Objective

Create the two Gradle composite-build modules from design §4:
`securities/data-privacy-core` (pure JDK, zero deps) and
`securities/data-privacy-spring-ai` (Spring AI adapter), wire them into the root composite build,
and create `CREDITS.md` with MIT attribution. After this task the modules compile, pass Checkstyle
(empty `src` is fine), and are discoverable by every later task.

## Hand-off context

- **Design doc:** §4 "Module & package layout" (exact `build.gradle` recipes), §4.1/§4.2.
- **Precedent to copy (already in-repo, do not re-derive):**
  - `securities/data-masker/settings.gradle` + `build.gradle` — the nearest sibling module shape.
  - `http/declarative-http/build.gradle` — shows the version-variable + `api "io.github.khezyapp:..."`
    dependency style used for composite-build siblings.
  - Repo level: AGENTS.md "Build system" + "Key conventions".
- **You do NOT need to read:** the design docs `01/02/03`, the `docs/` folder, or any other
  module's source. Package names and versions are pinned below.

## Module 1 — `securities/data-privacy-core`

**settings.gradle**
```groovy
pluginManagement {
    includeBuild("../../build-logic")
}
rootProject.name = "data-privacy-core"
```

**build.gradle** (design §4.1)
```groovy
plugins {
    id("khezy.java-library")
}

group = "io.github.khezyapp"
version = "1.0.0"

mavenPublishing {
    pom {
        name = 'data-privacy-core'
        description = """Deterministic text-level data privacy engine: PII catalog, secret scanning, URL validation, typed redaction, streaming support, and a pluggable LLM-classifier SPI."""
    }
}
```

- **Zero runtime dependencies. Do not add `khezy.java-lombok`.** Records only, pure JDK 17.
- Create the package root scaffold dir (can be gitignored-empty or contain a `package-info.java` if
  Checkstyle demands it — check `checkstyleMain`; a missing-dir is not a compile error).

## Module 2 — `securities/data-privacy-spring-ai`

**settings.gradle**
```groovy
pluginManagement {
    includeBuild("../../build-logic")
}
rootProject.name = "data-privacy-spring-ai"
```

**build.gradle** (design §4.2, adapted to repo composite-build convention)
```groovy
plugins {
    id("khezy.java-library")
    id("khezy.java-lombok")
}

group = "io.github.khezyapp"
version = "1.0.0"

dependencies {
    api "io.github.khezyapp:data-privacy-core:1.0.0"
    // Spring AI coordinates to confirm at implementation time (design §4.2 note):
    implementation platform("org.springframework.ai:spring-ai-bom:1.0.0")
    implementation "org.springframework.ai:spring-ai-client-chat"
    testImplementation "org.springframework.ai:spring-ai-test"
}
```

- The extension `project(":data-privacy-core")` in design §4.2 is **not valid** across sibling
  composite builds. Use the **coordinate** form `api "io.github.khezyapp:data-privacy-core:1.0.0"`
  (same style declarative-http uses for `dynamic-object`); Gradle substitutes the included build.
- **This adapter module applies `khezy.java-lombok`** (unlike core). The `SpringAiLlmClassifier`
  builder (Task 12) has no custom logic, so use lombok `@Builder`/`@Getter` etc. instead of hand-written
  getters/setters and a manual builder class. Core stays records-only, pure JDK 17, with no lombok.
- **Adapter dependency direction:** adapter depends on core, never the reverse. Core has no idea
  Spring exists.
- `spring-ai-bom` / `spring-ai-client-chat` resolve against `mavenCentral()` (already in root
  `dependencyResolutionManagement`). If the exact BOM coordinates differ in the resolved Spring AI
  GA line, adjust and **record the real coordinates in the handoff log** — this is a known-open
  implementation detail.

## Root wiring — `settings.gradle` (repo root)

Add next to the other `includeBuild(...)` lines:

```groovy
includeBuild("securities/data-privacy-core")
includeBuild("securities/data-privacy-spring-ai")
```

## CREDITS.md (core module)

Create `securities/data-privacy-core/CREDITS.md` with attribution (design §17):

- **n8n Guardrails node** — https://github.com/n8n-io/n8n, `packages/@n8n/nodes-langchain/nodes/Guardrails/` (MIT) — contract semantics ported, not code copied.
- **OpenAI Guardrails JS** — https://github.com/openai/openai-guardrails-js (MIT) — PII/URL/keyword regex tables. Later tasks extend CREDITS.md when they port patterns.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` and `./gradlew :data-privacy-spring-ai:build` both →
  BUILD SUCCESSFUL (compile + Checkstyle green on empty modules).
- Root `settings.gradle` lists both modules; `./gradlew projects` shows them as included builds.
- `data-privacy-core` has **no** third-party dependencies (`./gradlew :data-privacy-core:dependencies`
  shows only JDK). If Spring AI resolution fails, do not widen scope — log the exact failure with
  the candidate coordinates and mark the adapter deps block `// TODO resolve at Task 12` so Task 12
  finishes it; core must still be green.

## Hand-off to next task (log in 00-HANDOFF.md)

- Exact `build.gradle`/`settings.gradle` contents you settled on (especially the real Spring AI
  coordinates if resolved now, else the TODO).
- Confirmed commands + results.
- File list added to `CREDITS.md`.
- Next task (02) builds `api/` in `data-privacy-core` — confirm the package root dir exists.