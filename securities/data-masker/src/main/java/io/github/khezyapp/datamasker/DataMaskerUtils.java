package io.github.khezyapp.datamasker;

import io.github.khezyapp.datamasker.api.SensitiveMasker;
import io.github.khezyapp.datamasker.api.SensitiveMaskerStrategy;
import io.github.khezyapp.datamasker.strategy.SensitiveMaskerBuilder;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A utility class providing static access to sensitive data masking capabilities.
 * <p>
 * This class serves as a convenience wrapper around {@link SensitiveMaskerBuilder}, offering
 * a thread-safe singleton instance for standard masking tasks and flexible factory methods
 * for creating custom-configured maskers.
 * </p>
 * <p>Key behaviors include:</p>
 * <ul>
 * <li><b>Singleton Access:</b> Maintains a static, pre-configured {@link SensitiveMasker}
 * instance initialized with default strategies for immediate use.</li>
 * <li><b>Functional Customization:</b> Provides a {@link Consumer}-based entry point to
 * programmatically configure a {@link SensitiveMaskerBuilder} for specific masking requirements.</li>
 * <li><b>Strategy Overloading:</b> Allows for the quick creation of a custom masker by
 * providing a varargs list of {@link SensitiveMaskerStrategy} implementations.</li>
 * <li><b>Null Safety:</b> The static {@code mask()} method includes a guard clause to
 * return {@code null} immediately if the input payload is null, preventing downstream
 * processing errors.</li>
 * <li><b>Private Constructor:</b> Enforces the utility pattern by preventing instantiation
 * of the class.</li>
 * </ul>
 *
 * @see SensitiveMasker
 * @see SensitiveMaskerBuilder
 */
public final class DataMaskerUtils {

    private static final SensitiveMasker INSTANCE = SensitiveMaskerBuilder.builder().build();

    private DataMaskerUtils() {
    }

    public static Object mask(final Object payload) {
        if (Objects.isNull(payload)) {
            return null;
        }
        return INSTANCE.mask(payload);
    }

    public static SensitiveMasker custom(final Consumer<SensitiveMaskerBuilder> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        final var builder = SensitiveMaskerBuilder.builder();
        consumer.accept(builder);
        return builder.build();
    }

    public static SensitiveMasker custom(final SensitiveMaskerStrategy... strategies) {
        Objects.requireNonNull(strategies, "strategies must not be null");
        return SensitiveMaskerBuilder.builder()
                .registerStrategy(strategies)
                .build();
    }
}
