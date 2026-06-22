# khezy-kit — AGENTS.md

## Project structure

Multi-module Gradle **composite build** (7 independent Gradle builds wired via root `settings.gradle`). Each module has its own `settings.gradle`, `build.gradle`, and version.

| Module | Path | Maven coordinate | Version |
|---|---|---|---|
| storage-api | `storage/storage-api` | `io.github.khezyapp:storage-api` | `1.0.0-SNAPSHOT` |
| storage-fs | `storage/storage-fs` | `io.github.khezyapp:storage-fs` | `1.0.0-SNAPSHOT` |
| string-util | `utils/string-util` | `io.github.khezyapp:string-util` | `1.0.0` |
| dynamic-object | `utils/dynamic-object` | `io.github.khezyapp:dynamic-object` | `1.0.0` |
| clone-util | `utils/clone-util` | `io.github.khezyapp:clone-util` | `1.0.0` |
| data-masker | `securities/data-masker` | `io.github.khezyapp:data-masker` | `1.0.2` |
| simple-prompt-template | `templates/simple-prompt-template` | `io.github.khezyapp:simple-prompt-template` | `1.0.0` |

## Build system

- **Gradle 8.14.5** (wrapper), **JDK 21** toolchain, **`--release 17`** target bytecode
- Convention plugins live in `build-logic/src/main/groovy/`. **Do not apply `java-library` directly** — use `khezy.java-library` instead (aggregates base-java-library + junit5 + maven-publish + code-quality)
- Lombok/SLF4J is **opt-in**: apply `khezy.java-lombok` separately when needed (Lombok 1.18.42, SLF4J 2.0.17)
- Checkstyle 13.1.0 is always enforced (custom rules in `build-logic/src/main/resources/config/checkstyle/checkstyle.xml`); Javadoc lint is suppressed

## Commands

```sh
# Build + test + checkstyle everything
./gradlew build

# Test everything (JUnit 5, parallel forks = half CPUs)
./gradlew test

# Run a single module's tests
./gradlew :dynamic-object:test

# Run Checkstyle only
./gradlew check

# JMH benchmarks (dynamic-object only)
./gradlew :dynamic-object:jmh

# Release to Maven Central (manual CI only — see .github/workflows/manual_release.yml)
```

## Key conventions

- Package base: `io.github.khezyapp.<module-prefix>` (e.g. `io.github.khezyapp.doa`, `io.github.khezyapp.datamasker`, `io.github.khezyapp.clone`)
- Each module declares its own `group` and `version` in `build.gradle` (or `gradle.properties` for storage modules)
- Storage modules (`storage-api`, `storage-fs`) are still `1.0.0-SNAPSHOT` (pre-release); all others are published releases
- Modules that use Lombok set `compileOnly` extends `annotationProcessor` via convention plugin; no manual lombok config needed
- Test fixtures: `clone-util` tests use `joda-time:joda-time:2.14.0` as a third-party class to verify deep cloning
- `dynamic-object` has JMH benchmarks in `src/jmh/java/` (JMH plugin 0.7.3)

## Publishing

- Publishing uses `com.vanniktech.maven.publish` plugin (version managed in `build-logic/build.gradle`)
- Publishes to Maven Central (Sonatype Central Portal) with automatic signing
- Release is only triggered manually via GitHub Actions (`workflow_dispatch`); accepts comma-separated module names or `"all"`
- Set version via `-Pversion=` at release time

## CI

Single workflow: `.github/workflows/manual_release.yml` — manual Maven Central release only. No CI runs on push/PR.
