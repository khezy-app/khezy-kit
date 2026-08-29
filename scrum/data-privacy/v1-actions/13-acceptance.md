# Task 13 — Acceptance: guarantee-scope regression (G1–G7), end-to-end, READMEs

## Objective

Validate the whole library against the design's **guarantee scope §3 (G1–G7 / N1–N5)**, run an
end-to-end demo with ALL families (deterministic + Spring AI classifier), and bring documentation
(main + module READMEs) in line. This is the final task — it must prove the compact answers to the
design's §3 guarantees, not just "tests pass".

## Hand-off context

- **Design doc:** §3 (guarantee scope — read it), §14 (testing strategy cheatsheet),
  §16/§17 (datasheet/attribution), §12 (facade semantics from Task 10/11 handoff).
- **From Task 10 handoff:** `Guardrails.scan/redact/run` + `failOnlyOnErrors` + SANITIZE-short-circuit.
- **From Task 12 handoff:** resolved Spring AI GA version, `SpringAiLlmClassifier` API, wiring note
  (`Guardrails.builder().withClassifier(...)`).
- **Repo conventions from AGENTS.md:** build/test/Checkstyle commands for both modules;
  `graphify update .` after final edits.

## Deliverables

### 1. Guarantee regression suite — `securities/data-privacy-core/src/test/java/io/github/khezyapp/dpriv/GuaranteeScopeTest.java`

One named test per design §3 item:
- **G1 determinism** — identical input ⇒ binary-identical `scan`/`redact`/`run` outputs, in-memory
  AND streaming, repeated N times.
- **G2 redaction completeness** — every token in every `maskEntities` value is un-findable in
  `cleanedValue` (substring scan over output; placeholder protection proves the `<...>` literal isn't
  false-positive redacted).
- **G3+ remaining items** — enumerate §3 exactly; map each to an assert (read §3, name tests after
  the guarantee id, e.g. `g3NoClassificationOverRedact`).
- **N1–N5** — negative tests documenting what is NOT guaranteed (e.g. subclass-only patterns,
  LLM confidence as-opinion, overlap-capped matches) — assert the documented non-behavior to pin it.
- **End-to-end parity** — a single big fixture: `scan`, `redact`, streaming variants, CLASSIFY with a
  stubbed classifier, and (if the Spring AI classifier is wired) one real `run(..., CLASSIFY)` smoke.
  Assert outputs align with §12.3 outcome shapes.

### 2. Cross-module smoke test — `securities/data-privacy-spring-ai/src/test/.../EndToEndSpringAiTest.java`
- `Guardrails.builder().withClassifier(SpringAiLlmClassifier.jailbreak(chatClient, 0.7)).build()`
  → `run(text, CLASSIFY)` returns a `GuardrailsOutcome` whose jailbreak family surfaces; adapt if
  the classifier/factory API changed during Task 12 (log the concrete wiring here — this is the
  repo's reference usage).

### 3. Documentation
- `securities/data-privacy-core/README.md` + `securities/data-privacy-spring-ai/README.md`:
  one-screen each (what it is, build/test commands, minimal scan/redact + CLASSIFY example, config
  defaults, guarantee box G1–G7/N1–N5, attribution §17 → CREDITS.md).
- `scrum/data-privacy/README.md`: add a **v1 milestone** section pointing at the design doc +
  `v1-actions/` + both modules’ READMEs; mark `data_privacy_core_design.md` as "implemented".
- Run `graphify update .` at repo root.

## Acceptance criteria

- `./gradlew :data-privacy-core:build` and `./gradlew :data-privacy-spring-ai:build` → fully green
  (compile + full JUnit suite + Checkstyle).
- Every §3 guarantee/non-guarantee has a named test; the suite is the "covered by G1–G7" checklist.
- README examples are copy-paste-runnable against the shipped API (verify by running the snippet as a
  test when feasible).
- v1-actions: mark all 13 tasks complete in the INDEX (check the boxes) and append the final
  handoff entry summarizing the milestone.
- Handoff log records: full guarantee→test mapping, the Spring AI wiring snippet, the README paths,
  any leftover deviations, and a recommendation of what a v1.1 could fold in (if anything).

## Hand-off to next task (log in 00-HANDOFF.md)

There is no next build task — this is the wrap. Record instead:
- Guarantee mapping table (G1–G7, N1–N5 → test method names).
- Final API surface summary (one line per public type) for anyone consuming the library.
- Any `v1-actions/*.md` corrections made during acceptance (so the plan stays the plan of record).