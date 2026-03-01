package io.github.khezyapp.clone;

import io.github.khezyapp.clone.api.CloneContext;
import io.github.khezyapp.clone.api.CloneStrategy;
import io.github.khezyapp.clone.api.Cloner;
import io.github.khezyapp.clone.strategy.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of the {@link Cloner} interface.
 * <p>
 * This class uses a prioritized list of {@link CloneStrategy} objects to perform deep copies.
 * It provides a {@link Builder} to allow customization of the cloning process by registering
 * specialized strategies before the default ones.
 * </p>
 */
public final class DefaultCloner implements Cloner {
    private final List<CloneStrategy> strategies;

    /**
     * Private constructor used by the Builder.
     *
     * @param strategies the ordered list of strategies to use for cloning
     */
    private DefaultCloner(final List<CloneStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * Creates a new Builder instance to configure a DefaultCloner.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Entry point for a deep clone operation.
     * <p>
     * Initializes a new {@link CloneContext} for the current call stack to manage
     * circular references and state.
     * </p>
     *
     * @param origin the object to clone
     * @return the deep copied instance
     */
    @Override
    public <T> T deepClone(final T origin) {
        return new CloneContext(this).proceed(origin);
    }

    /**
     * Resolves the first strategy that supports the given class.
     *
     * @param clz the class to evaluate
     * @return the supporting {@link CloneStrategy}
     * @throws IllegalArgumentException if no strategy is found (unlikely given ReflectionStrategy fallback)
     */
    @Override
    public CloneStrategy getCloneStrategy(final Class<?> clz) {
        return strategies.stream()
                .filter(s -> s.support(clz))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Could not find CloneStrategy for class " + clz.getName()));
    }

    /**
     * Builder class for {@link DefaultCloner}.
     * <p>
     * Custom strategies registered via {@link #registerStrategy(CloneStrategy)} take
     * precedence over the default internal strategies.
     * </p>
     */
    public static final class Builder {
        private final List<CloneStrategy> strategies = new ArrayList<>();

        private Builder() {
        }

        /**
         * Registers a custom strategy at the beginning of the strategy chain.
         * * @param strategy the strategy to register
         * @return this builder
         */
        public Builder registerStrategy(final CloneStrategy strategy) {
            this.strategies.add(0, strategy);
            return this;
        }

        /**
         * Appends default strategies and constructs the DefaultCloner.
         *
         * @return a configured {@link DefaultCloner}
         */
        public DefaultCloner build() {
            this.strategies.add(new ImmutableStrategy());
            this.strategies.add(new MapStrategy());
            this.strategies.add(new CollectionStrategy());
            this.strategies.add(new ArrayStrategy());
            this.strategies.add(new ReflectionStrategy());
            return new DefaultCloner(this.strategies);
        }
    }
}
