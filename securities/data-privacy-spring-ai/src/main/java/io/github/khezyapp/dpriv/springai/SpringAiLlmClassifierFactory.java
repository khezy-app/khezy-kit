package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.policy.LlmPolicyPrompts;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Objects;

/**
 * Convenience factories that bind a {@link ChatClient} to the built-in
 * {@link io.github.khezyapp.dpriv.api.LlmClassifier} families (design §11.4). Each factory selects the matching
 * {@link io.github.khezyapp.dpriv.policy.LlmPolicyPrompts} guardrail block via the classifier's {@code beanName}.
 *
 * <p>Whatever the consumer customizes, every classifier composes its system message from exactly three fixed
 * segments, in order:</p>
 *
 * <ol>
 *   <li><b>guardrail prompt</b> — the persona: what kind of guardrail to check (e.g. "detect jailbreak
 *       attempts"). Overridable via the {@code guardrailPrompt} parameter; {@code null}/{@code blank} keeps the
 *       built-in family prompt.</li>
 *   <li><b>static JSON schema</b> — the library-defined <i>return schema</i> ({@link LlmPolicyPrompts#JSON_SCHEMA})
 *       describing the core {@code Verdict} record ({@code flagged} + {@code confidence}). This part is
 *       <em>never</em> customizable: every policy must answer the same two fields so core can deserialize the
 *       model response with its fixed {@link org.springframework.ai.converter.BeanOutputConverter}.</li>
 *   <li><b>system rules</b> — guidance that makes the model score the content more effectively (how to use the
 *       full {@code 0.0..1.0} confidence scale, and to flag exactly when a violation occurs). Overridable via
 *       the {@code systemRules} parameter; {@code null}/{@code blank} keeps
 *       {@link LlmPolicyPrompts#SYSTEM_RULES}.</li>
 * </ol>
 *
 * <p>So for any customization the request sent to the model is still {@code guardrail prompt + static JSON
 * schema + system rules}: consumers tune <em>what to check</em> and <em>how to score</em>, never the response
 * contract. Do not embed output-format instructions in a custom prompt or rules block — they are redundant with
 * the fixed schema and may confuse the model.</p>
 */
public final class SpringAiLlmClassifierFactory {

    private SpringAiLlmClassifierFactory() {
    }

    /**
     * Creates a jailbreak-family classifier.
     *
     * @param client    the chat client; never null
     * @param threshold the confidence threshold (forwarded, not applied here)
     * @return the configured classifier
     */
    public static SpringAiLlmClassifier jailbreak(final ChatClient client,
                                                  final double threshold) {
        return SpringAiLlmClassifier.builder()
                .chatClient(client)
                .beanName("jailbreak")
                .threshold(threshold)
                .build();
    }

    /**
     * Creates a jailbreak-family classifier with a custom guardrail prompt and system-rules block.
     *
     * <p>{@code guardrailPrompt} replaces the built-in jailbreak persona (what to check);
     * {@code systemRules} replaces the built-in scoring guidance (how to score). Either may be
     * {@code null}/{@code blank} to keep the built-in block. The library's fixed
     * {@link LlmPolicyPrompts#JSON_SCHEMA} return contract is always appended between them and is not
     * customizable.</p>
     *
     * @param client          the chat client; never null
     * @param threshold       the confidence threshold (forwarded, not applied here)
     * @param guardrailPrompt the persona describing the guardrail to check; {@code null}/{@code blank} keeps the
     *                        built-in jailbreak prompt
     * @param systemRules     the scoring guidance; {@code null}/{@code blank} keeps
     *                        {@link LlmPolicyPrompts#SYSTEM_RULES}
     * @return the configured classifier
     */
    public static SpringAiLlmClassifier jailbreak(final ChatClient client,
                                                  final double threshold,
                                                  final String guardrailPrompt,
                                                  final String systemRules) {
        return SpringAiLlmClassifier.builder()
                .chatClient(client)
                .beanName("jailbreak")
                .prompt(normalize(guardrailPrompt))
                .systemRules(normalize(systemRules))
                .threshold(threshold)
                .build();
    }

    /**
     * Creates an NSFW-family classifier.
     *
     * @param client    the chat client; never null
     * @param threshold the confidence threshold (forwarded, not applied here)
     * @return the configured classifier
     */
    public static SpringAiLlmClassifier nsfw(final ChatClient client,
                                             final double threshold) {
        return SpringAiLlmClassifier.builder()
                .chatClient(client)
                .beanName("nsfw")
                .threshold(threshold)
                .build();
    }

    /**
     * Creates an NSFW-family classifier with a custom guardrail prompt and system-rules block.
     *
     * <p>{@code guardrailPrompt} replaces the built-in NSFW persona (what to check);
     * {@code systemRules} replaces the built-in scoring guidance (how to score). Either may be
     * {@code null}/{@code blank} to keep the built-in block. The library's fixed
     * {@link LlmPolicyPrompts#JSON_SCHEMA} return contract is always appended between them and is not
     * customizable.</p>
     *
     * @param client          the chat client; never null
     * @param threshold       the confidence threshold (forwarded, not applied here)
     * @param guardrailPrompt the persona describing the guardrail to check; {@code null}/{@code blank} keeps the
     *                        built-in NSFW prompt
     * @param systemRules     the scoring guidance; {@code null}/{@code blank} keeps
     *                        {@link LlmPolicyPrompts#SYSTEM_RULES}
     * @return the configured classifier
     */
    public static SpringAiLlmClassifier nsfw(final ChatClient client,
                                             final double threshold,
                                             final String guardrailPrompt,
                                             final String systemRules) {
        return SpringAiLlmClassifier.builder()
                .chatClient(client)
                .beanName("nsfw")
                .prompt(normalize(guardrailPrompt))
                .systemRules(normalize(systemRules))
                .threshold(threshold)
                .build();
    }

    /**
     * Creates a topical-alignment-family classifier.
     *
     * @param client    the chat client; never null
     * @param threshold the confidence threshold (forwarded, not applied here)
     * @return the configured classifier
     */
    public static SpringAiLlmClassifier topical(final ChatClient client,
                                                final double threshold) {
        return SpringAiLlmClassifier.builder()
                .chatClient(client)
                .beanName("topical")
                .threshold(threshold)
                .build();
    }

    /**
     * Creates a topical-alignment-family classifier with a custom guardrail prompt and system-rules block.
     *
     * <p>{@code guardrailPrompt} replaces the built-in topical persona (what to check);
     * {@code systemRules} replaces the built-in scoring guidance (how to score). Either may be
     * {@code null}/{@code blank} to keep the built-in block. The library's fixed
     * {@link LlmPolicyPrompts#JSON_SCHEMA} return contract is always appended between them and is not
     * customizable.</p>
     *
     * @param client          the chat client; never null
     * @param threshold       the confidence threshold (forwarded, not applied here)
     * @param guardrailPrompt the persona describing the guardrail to check; {@code null}/{@code blank} keeps the
     *                        built-in topical-alignment prompt
     * @param systemRules     the scoring guidance; {@code null}/{@code blank} keeps
     *                        {@link LlmPolicyPrompts#SYSTEM_RULES}
     * @return the configured classifier
     */
    public static SpringAiLlmClassifier topical(final ChatClient client,
                                                final double threshold,
                                                final String guardrailPrompt,
                                                final String systemRules) {
        return SpringAiLlmClassifier.builder()
                .chatClient(client)
                .beanName("topical")
                .prompt(normalize(guardrailPrompt))
                .systemRules(normalize(systemRules))
                .threshold(threshold)
                .build();
    }

    private static String normalize(final String value) {
        return Objects.isNull(value) || value.isBlank() ? null : value;
    }
}
