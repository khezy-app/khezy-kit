package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.api.LlmClassifier;
import io.github.khezyapp.dpriv.policy.LlmPolicyPrompts;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.Objects;

/**
 * Canonical Spring AI {@link LlmClassifier} (design §11.2): a {@link ChatClient}-backed implementation that
 * deserializes the core {@code Verdict} record via a {@link BeanOutputConverter}.
 *
 * <p>The classifier owns <em>no</em> decision logic: it only returns the raw {@link LlmClassifier.Verdict}
 * produced by the model. The threshold rule lives in core's {@code LlmContract} (Tasks 08/10). Prompts are
 * taken from {@link LlmPolicyPrompts} per built-in family unless the builder supplies a custom one.
 */
public final class SpringAiLlmClassifier implements LlmClassifier {

    private final ChatClient chatClient;
    private final String beanName;
    private final String systemMessage;
    private final double threshold;
    private final BeanOutputConverter<LlmClassifier.Verdict> converter;

    private SpringAiLlmClassifier(final Builder builder) {
        this.chatClient = Objects.requireNonNull(builder.chatClient, "chatClient");
        this.beanName = Objects.requireNonNull(builder.beanName, "beanName");
        final var guardrailPrompt = builder.prompt != null
                ? builder.prompt
                : defaultPrompt(this.beanName);
        this.systemMessage = LlmPolicyPrompts.systemMessage(guardrailPrompt, builder.systemRules);
        this.threshold = builder.threshold;
        this.converter = new BeanOutputConverter<>(LlmClassifier.Verdict.class);
    }

    /**
     * Creates a fluent builder for a {@link SpringAiLlmClassifier}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LlmClassifier.Verdict classify(final String input) {
        Objects.requireNonNull(input, "input");
        final var content = chatClient.prompt()
                .system(systemMessage)
                .user(input)
                .call()
                .content();
        if (Objects.isNull(content)) {
            throw new IllegalStateException("ChatClient returned no content for input");
        }
        final var verdict = converter.convert(content);
        return sanitize(verdict);
    }

    /**
     * The configured confidence threshold. The adapter does not apply it (core owns the decision), but it is
     * exposed so callers can forward it to {@code Guardrails} configuration.
     *
     * @return the threshold
     */
    @Override
    public String beanName() {
        return beanName;
    }

    public double threshold() {
        return threshold;
    }

    private LlmClassifier.Verdict sanitize(final LlmClassifier.Verdict raw) {
        final var confidence = sanitizeConfidence(raw.confidence());
        return new LlmClassifier.Verdict(raw.flagged(), confidence);
    }

    private static double sanitizeConfidence(final double confidence) {
        if (Double.isNaN(confidence) || Double.isInfinite(confidence)) {
            return 0.0d;
        }
        return Math.min(1.0d, Math.max(0.0d, confidence));
    }

    private static String defaultPrompt(final String name) {
        return switch (name) {
            case "jailbreak" -> LlmPolicyPrompts.jailbreakPrompt();
            case "nsfw" -> LlmPolicyPrompts.nsfwPrompt();
            case "topical", "topicalAlignment" -> LlmPolicyPrompts.topicalAlignmentPrompt();
            default -> throw new IllegalStateException(
                    "No built-in guardrail prompt for beanName '" + name + "'; supply prompt(...)");
        };
    }

    /**
     * Fluent builder for {@link SpringAiLlmClassifier}.
     */
    public static final class Builder {

        private ChatClient chatClient;
        private String beanName;
        private String prompt;
        private String systemRules;
        private double threshold;

        private Builder() {
        }

        /**
         * Sets the Spring AI {@link ChatClient} that performs the classification.
         *
         * @param value the chat client; never null
         * @return this builder
         */
        public Builder chatClient(final ChatClient value) {
            this.chatClient = Objects.requireNonNull(value, "chatClient");
            return this;
        }

        /**
         * Sets the unique bean name (also the {@code entityType}, e.g. {@code "jailbreak"}). Must match a core
         * built-in family, or {@link #prompt(String)} must be supplied.
         *
         * @param value the bean name; never null
         * @return this builder
         */
        public Builder beanName(final String value) {
            this.beanName = Objects.requireNonNull(value, "beanName");
            return this;
        }

        /**
         * Supplies a custom guardrail prompt block, overriding the built-in family prompt.
         *
         * @param value the guardrail prompt
         * @return this builder
         */
        public Builder prompt(final String value) {
            this.prompt = value;
            return this;
        }

        /**
         * Supplies a custom system-rules block, overriding the built-in {@link LlmPolicyPrompts#SYSTEM_RULES}.
         * A {@code null} or blank value falls back to the built-in rules.
         *
         * @param value the system rules
         * @return this builder
         */
        public Builder systemRules(final String value) {
            this.systemRules = value;
            return this;
        }

        /**
         * Sets the confidence threshold. The adapter does not apply it (core owns the decision); it is recorded
         * for forwarding to {@code Guardrails} configuration.
         *
         * @param value the threshold
         * @return this builder
         */
        public Builder threshold(final double value) {
            this.threshold = value;
            return this;
        }

        /**
         * Builds the {@link SpringAiLlmClassifier}.
         *
         * @return the configured classifier
         */
        public SpringAiLlmClassifier build() {
            return new SpringAiLlmClassifier(this);
        }
    }
}
