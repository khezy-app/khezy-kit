package io.github.khezyapp.datamasker.strategy;

import io.github.khezyapp.datamasker.api.SensitiveMasker;
import io.github.khezyapp.datamasker.api.SensitiveMaskerContext;
import io.github.khezyapp.datamasker.api.SensitiveMaskerStrategy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultSensitiveMasker implements SensitiveMasker {
    private final SensitiveMaskerStrategy masker;

    /**
     * The default implementation of the {@link SensitiveMasker} interface, acting as the
     * primary entry point for the masking process.
     * <p>
     * This class orchestrates the lifecycle of a masking operation by initializing a new
     * {@link SensitiveMaskerContext} for every request. This ensures that stateful
     * information, such as visited objects for circular dependency detection, is isolated
     * to a single execution thread.
     * </p>
     * <p>Key behaviors include:</p>
     * <ul>
     * <li><b>Context Initialization:</b> Creates a fresh {@link SensitiveMaskerContext}
     * using the provided {@link SensitiveMaskerStrategy} to manage the recursive
     * traversal of the payload.</li>
     * <li><b>Encapsulation:</b> Hides the complexity of context management and recursive
     * processing from the API consumer.</li>
     * <li><b>Thread Safety:</b> By creating a new context per {@code mask()} call, the
     * implementation remains safe for use in multi-threaded environments (provided
     * the underlying strategy is also thread-safe).</li>
     * </ul>
     *
     * @param payload the root object, collection, or map to be processed for sensitive data
     * @return a masked representation of the input payload (typically a Map or a new
     * Collection/Array) with sensitive information redacted
     */
    @Override
    public Object mask(final Object payload) {
        final var context = new SensitiveMaskerContext(masker);
        return context.processMask(payload);
    }
}
