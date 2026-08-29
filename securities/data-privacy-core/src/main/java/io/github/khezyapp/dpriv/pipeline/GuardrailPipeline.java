package io.github.khezyapp.dpriv.pipeline;

import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.checks.KeywordsCheck;
import io.github.khezyapp.dpriv.checks.PiiCheck;
import io.github.khezyapp.dpriv.checks.SecretKeysCheck;
import io.github.khezyapp.dpriv.checks.UrlsCheck;
import io.github.khezyapp.dpriv.internal.ParallelStageRunner;
import io.github.khezyapp.dpriv.redact.Redactor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The two-stage in-memory evaluator (design §6). Stage 1 (preflight) runs the deterministic
 * families — PII, secrets, URLs, keywords — in parallel; stage 2 (classify) runs the LLM-as-judge
 * checks in parallel. Redaction is the preflight stage's combined {@code cleanedValue} (no LLM
 * involvement). The {@code failOnlyOnErrors} policy is owned by the facade and exposed via
 * {@link #failOnlyOnErrors()} so classifier errors can be toggled between fail-closed and pass.
 */
public final class GuardrailPipeline {

    private final GuardrailsConfig config;
    private final List<GuardrailCheck> preflight;
    private final List<GuardrailCheck> classificatory;
    private final boolean failOnlyOnErrors;
    private final ParallelStageRunner runner;

    /**
     * Builds the pipeline from policy alone: the default preflight families and an empty
     * classificatory stage. Errors fail-closed by default.
     *
     * @param config the policy; never null
     */
    public GuardrailPipeline(final GuardrailsConfig config) {
        this(config, true);
    }

    /**
     * Builds the pipeline from policy alone with an explicit error policy.
     *
     * @param config           the policy; never null
     * @param failOnlyOnErrors whether a classifier error fails the input (true) or is treated as a pass (false)
     */
    public GuardrailPipeline(final GuardrailsConfig config,
                             final boolean failOnlyOnErrors) {
        this(config, defaultPreflight(config), List.of(), failOnlyOnErrors);
    }

    /**
     * Builds the pipeline with explicit stage lists and error policy.
     *
     * @param config           the policy; never null
     * @param preflight        the deterministic preflight checks, in stage order; never null
     * @param classificatory   the LLM-as-judge checks, in stage order; never null
     * @param failOnlyOnErrors whether a classifier error fails the input (true) or is treated as a pass (false)
     */
    public GuardrailPipeline(final GuardrailsConfig config,
                             final List<GuardrailCheck> preflight,
                             final List<GuardrailCheck> classificatory,
                             final boolean failOnlyOnErrors) {
        this.config = Objects.requireNonNull(config, "config");
        this.preflight = List.copyOf(Objects.requireNonNull(preflight, "preflight"));
        this.classificatory = List.copyOf(Objects.requireNonNull(classificatory, "classificatory"));
        this.failOnlyOnErrors = failOnlyOnErrors;
        this.runner = new ParallelStageRunner(new Redactor());
    }

    /**
     * Runs the preflight (deterministic) stage.
     *
     * @param input the text to check
     * @return the preflight stage result
     */
    public StageResult preflight(final String input) {
        return runner.run("preflight", preflight, input);
    }

    /**
     * Runs the classificatory (LLM-as-judge) stage.
     *
     * @param input the text to check (callers pass the masked text for classify semantics)
     * @return the classify stage result
     */
    public StageResult classify(final String input) {
        return runner.run("classify", classificatory, input);
    }

    /**
     * Redacts {@code input} using the preflight stage only (no LLM involvement). Fail-closed (G4):
     * throws when any preflight check errored, because the returned text would be under-redacted.
     *
     * @param input the text to redact
     * @return the redacted text
     * @throws IllegalStateException if a preflight check failed (design §13)
     */
    public String redact(final String input) {
        final var preflight = preflight(input);
        if (!preflight.errors().isEmpty()) {
            throw new IllegalStateException("redaction aborted on check error: " + preflight.errors());
        }
        return preflight.cleanedValue();
    }

    /**
     * The configured error policy for classifier failures.
     *
     * @return true if a classifier error fails the input
     */
    public boolean failOnlyOnErrors() {
        return failOnlyOnErrors;
    }

    /**
     * The default preflight stage: the four deterministic families built from {@code config}.
     *
     * @param config the policy; never null
     * @return the preflight checks in stage order
     */
    public static List<GuardrailCheck> defaultPreflight(final GuardrailsConfig config) {
        Objects.requireNonNull(config, "config");
        final var redactor = new Redactor();
        final var checks = new ArrayList<GuardrailCheck>();
        checks.add(new PiiCheck(config.pii(), redactor));
        checks.add(new SecretKeysCheck(config.secrets(), redactor));
        checks.add(new UrlsCheck(config.urls(), redactor));
        checks.add(new KeywordsCheck(config.keywords(), redactor));
        return List.copyOf(checks);
    }
}
