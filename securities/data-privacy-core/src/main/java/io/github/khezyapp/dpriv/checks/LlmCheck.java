package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.api.LlmCheckConfig;
import io.github.khezyapp.dpriv.api.LlmClassifier;
import io.github.khezyapp.dpriv.policy.LlmContract;

import java.util.Objects;

/**
 * LLM-as-judge guardrail check (design §11). Binds any {@link LlmClassifier} SPI to the core
 * {@link GuardrailResult} contract via {@link LlmContract}. Classificatory only (design §12.2): a detected
 * verdict still produces an empty {@code maskEntities} and an unchanged {@code cleanedValue}.
 *
 * <p>A disabled check short-circuits to a pass without touching the classifier (design: disabled check
 * short-circuits). The prompt templates live in {@code LlmPolicyPrompts}, consumed by the wiring that
 * constructs and configures classifiers — not by this check.
 */
public final class LlmCheck implements GuardrailCheck {

    private final LlmClassifier classifier;
    private final LlmCheckConfig config;

    /**
     * Creates the LLM check for the given classifier and configuration.
     *
     * @param classifier the LLM classifier SPI; never null
     * @param config     the configuration; {@code null} is treated as {@link LlmCheckConfig#DEFAULTS}
     */
    public LlmCheck(final LlmClassifier classifier,
                    final LlmCheckConfig config) {
        this.classifier = Objects.requireNonNull(classifier, "classifier");
        this.config = Objects.nonNull(config) ? config : LlmCheckConfig.DEFAULTS;
    }

    @Override
    public GuardrailResult run(final String input) {
        Objects.requireNonNull(input, "input");
        if (!config.enabled()) {
            return GuardrailResult.pass(classifier.beanName(), input);
        }
        final var verdict = classifier.classify(input);
        return LlmContract.toResult(classifier, verdict, config, input);
    }

    @Override
    public String name() {
        return "LlmCheck";
    }
}
