package io.github.khezyapp.datamasker.strategy;

import io.github.khezyapp.datamasker.api.SensitiveMaskerContext;
import io.github.khezyapp.datamasker.api.SensitiveMaskerStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * A composite implementation of {@link SensitiveMaskerStrategy} that acts as a central
 * dispatcher for multiple masking strategies.
 * <p>
 * This class implements the <b>Strategy Design Pattern</b> by maintaining a prioritized
 * list of specific masking strategies (e.g., for Beans, Collections, or Maps). It
 * iterates through these strategies to find the first one that supports the given
 * payload and delegates the masking operation to it.
 * </p>
 * <p>Key behaviors include:</p>
 * <ul>
 * <li><b>Support:</b> Always returns {@code true}, as it serves as a universal entry
 * point for any object type.</li>
 * <li><b>Null Safety:</b> Returns {@code null} immediately if the provided payload is null.</li>
 * <li><b>First-Match Dispatching:</b> Executes the first strategy in the
 * {@code sensitiveMaskerStrategies} list that reports {@code supports(payload)}.</li>
 * <li><b>Fallback:</b> If no registered strategy supports the payload, the original
 * object is returned as-is to ensure data continuity.</li>
 * </ul>
 */
@RequiredArgsConstructor
public class CompositeSensitiveMaskerStrategy implements SensitiveMaskerStrategy {
    private final List<SensitiveMaskerStrategy> sensitiveMaskerStrategies;

    @Override
    public boolean supports(final Object payload) {
        return true;
    }

    @Override
    public Object mask(final Object payload,
                       final SensitiveMaskerContext context) {
        if (Objects.isNull(payload)) {
            return null;
        }

        if (isPrimitive(payload.getClass())) {
            return payload;
        }

        for (final var sensitiveMasker : sensitiveMaskerStrategies) {
            if (sensitiveMasker.supports(payload)) {
                return sensitiveMasker.mask(payload, context);
            }
        }
        return payload;
    }
}
