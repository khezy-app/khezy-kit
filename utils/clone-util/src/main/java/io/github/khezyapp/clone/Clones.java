package io.github.khezyapp.clone;

import io.github.khezyapp.clone.api.CloneStrategy;
import io.github.khezyapp.clone.api.Cloner;

/**
 * Static facade utility for convenient access to cloning operations.
 * <p>
 * This class provides a globally shared {@code ENGINE} instance for standard use cases,
 * while allowing the creation of ad-hoc cloners with custom strategies.
 * </p>
 */
public final class Clones {

    /**
     * Internal shared cloner instance.
     */
    private static final Cloner ENGINE = defaultCloner();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Clones() {
    }

    /**
     * Creates a new {@link Cloner} instance with optional custom strategies.
     *
     * @param customStrategies strategies to be prioritized over default ones
     * @return a new {@link Cloner}
     */
    public static Cloner defaultCloner(final CloneStrategy... customStrategies) {
        final var builder = DefaultCloner.builder();
        for (final var strategy : customStrategies) {
            builder.registerStrategy(strategy);
        }
        return builder.build();
    }

    /**
     * Performs a deep clone using the shared default engine.
     *
     * @param <T>    the type of the object
     * @param origin the object to clone
     * @return the deep copied instance
     */
    public static <T> T deepClone(final T origin) {
        return ENGINE.deepClone(origin);
    }
}
