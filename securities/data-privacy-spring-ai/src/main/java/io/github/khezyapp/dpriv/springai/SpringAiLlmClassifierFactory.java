package io.github.khezyapp.dpriv.springai;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Convenience factories that bind a {@link ChatClient} to the built-in {@link LlmClassifier} families
 * (design §11.4). Each factory selects the matching {@link LlmPolicyPrompts} guardrail block via the
 * classifier's {@code beanName}.
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
}
