# CREDITS

This module ports contract semantics and pattern tables from MIT-licensed open-source projects.
**Semantics ported, not code copied.** Attribution below.

## Attributions

- **n8n Guardrails node** — https://github.com/n8n-io/n8n,
  `packages/@n8n/nodes-langchain/nodes/Guardrails/` (MIT) — contract semantics ported, not code copied.
- **OpenAI Guardrails JS** — https://github.com/openai/openai-guardrails-js (MIT) — PII/URL/keyword
  regex tables. Later tasks extend CREDITS.md when they port patterns.
- **`DEFAULT_PII_PATTERNS` table** — ported verbatim (pattern strings only) from OpenAI Guardrails JS
  `src/checks/pii.ts` at commit `b9b99b4fb454f02a362c2836aec6285176ec40a8` (MIT), as mirrored in the
  n8n Guardrails node `.../nodes-langchain/nodes/Guardrails/actions/checks/pii.ts` (MIT). Used by
  `io.github.khezyapp.dpriv.policy.PiiPatterns`. Checksum validators (Luhn / ISO 7064 mod-97 /
  Verhoeff) are standard public algorithms, not ported code.
- **Secret entropy preset scheme** — the STRICT/BALANCED/PERMISSIVE tuple table
  (min_length / min_entropy / min_diversity / strict_mode) and the Shannon-entropy + char
  diversity decision contract are ported from the n8n Guardrails node
  `actions/checks/secretKeys.ts` (MIT). Used by `io.github.khezyapp.dpriv.policy.SecretPreset`
  and `io.github.khezyapp.dpriv.internal.SecretCandidateFilter`. Semantics ported, not code copied.
- **URL + keyword check patterns** — ported from OpenAI Guardrails JS `src/checks/urls.ts` and
  `src/checks/keywords.ts` at commit `b9b99b4fb454f02a362c2836aec6285176ec40a8` (MIT), as mirrored in
  the n8n Guardrails node `.../nodes-langchain/nodes/Guardrails/actions/checks/urls.ts` and
  `keywords.ts` (MIT). Used by `io.github.khezyapp.dpriv.checks.UrlsCheck` and
  `io.github.khezyapp.dpriv.checks.KeywordsCheck`. Semantics ported, not code copied.
