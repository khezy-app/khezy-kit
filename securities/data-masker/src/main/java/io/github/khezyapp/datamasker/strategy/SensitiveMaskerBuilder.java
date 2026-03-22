package io.github.khezyapp.datamasker.strategy;

import io.github.khezyapp.datamasker.api.SensitiveMasker;
import io.github.khezyapp.datamasker.api.SensitiveMaskerStrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A builder class designed to simplify the configuration and instantiation of a {@link SensitiveMasker}.
 * <p>
 * This builder implements a <b>Fluent API</b> to register various {@link SensitiveMaskerStrategy}
 * implementations. It ensures that a functional masking system is always produced by automatically
 * providing a set of default strategies (for collections, maps, and beans) if no custom
 * strategies are registered.
 * </p>
 * <p>Key behaviors include:</p>
 * <ul>
 * <li><b>Ordered Strategy Registration:</b> Uses a {@link LinkedHashMap} to maintain the
 * registration order of strategies, which is critical for the {@link CompositeSensitiveMaskerStrategy}
 * to evaluate payload support correctly.</li>
 * <li><b>Default Fallback:</b> If the builder is executed without custom strategies, it
 * automatically registers {@link CollectionSensitiveMaskerStrategy}, {@link MapSensitiveMaskerStrategy},
 * and {@link BeanSensitiveMaskerStrategy}.</li>
 * <li><b>Deduplication:</b> Ensures that only one instance of a specific strategy class is
 * present in the final configuration by using the strategy's class as a unique key.</li>
 * <li><b>Composition:</b> Upon calling {@code build()}, it wraps all registered strategies
 * into a {@link CompositeSensitiveMaskerStrategy} and provides it to a {@link DefaultSensitiveMasker}.</li>
 * </ul>
 *
 * @see SensitiveMasker
 * @see CompositeSensitiveMaskerStrategy
 */
public class SensitiveMaskerBuilder {
    private final Map<Class<?>, SensitiveMaskerStrategy> sensitiveMaskerStrategies = new LinkedHashMap<>();

    private SensitiveMaskerBuilder() {
    }

    public static SensitiveMaskerBuilder builder() {
        return new SensitiveMaskerBuilder();
    }

    public SensitiveMaskerBuilder registerStrategy(final SensitiveMaskerStrategy... sensitiveMaskerStrategy) {
        Objects.requireNonNull(sensitiveMaskerStrategy, "sensitiveMaskerStrategy must not contain null elements");
        for (final var strategy : sensitiveMaskerStrategy) {
            sensitiveMaskerStrategies.put(strategy.getClass(), strategy);
        }
        return this;
    }

    public SensitiveMasker build() {
        final var defaultMaskerStrategies = new SensitiveMaskerStrategy[] {
                new CollectionSensitiveMaskerStrategy(),
                new MapSensitiveMaskerStrategy(),
                new BeanSensitiveMaskerStrategy()
        };
        if (sensitiveMaskerStrategies.isEmpty()) {
            registerStrategy(defaultMaskerStrategies);
        } else {
            for (final var defaultMaskerStrategy : sensitiveMaskerStrategies.values()) {
                if (!sensitiveMaskerStrategies.containsKey(defaultMaskerStrategy.getClass())) {
                    sensitiveMaskerStrategies.put(defaultMaskerStrategy.getClass(), defaultMaskerStrategy);
                }
            }
        }
        return new DefaultSensitiveMasker(new CompositeSensitiveMaskerStrategy(
                new ArrayList<>(this.sensitiveMaskerStrategies.values())
        ));
    }
}
