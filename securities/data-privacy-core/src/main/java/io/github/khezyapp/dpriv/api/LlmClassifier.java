package io.github.khezyapp.dpriv.api;

/**
 * SPI for LLM-as-judge classification checks (design §11.2). The canonical Spring AI implementation
 * lives in the adapter module (Task 12); core keeps only this contract with zero Spring dependency.
 */
public interface LlmClassifier {

    /**
     * Classifies the given input.
     *
     * @param input the text to classify
     * @return the verdict, never null
     */
    Verdict classify(String input);

    /**
     * Unique name per classifier, used as the {@code entityType} (e.g. {@code "jailbreak"},
     * {@code "nsfw"}).
     *
     * @return the bean / entity type name
     */
    String beanName();

    /**
     * Result of a single classification.
     *
     * @param flagged     whether the classifier flagged the input
     * @param confidence  the classifier's confidence in ({@code 0..1})
     */
    record Verdict(boolean flagged, double confidence) {
    }
}
