package io.github.khezyapp.dynamicform.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The evaluation context threaded through every resolution call — schema version, deployment, and
 * feature flags. Visibility conditions may reference it through the special keys {@code @version},
 * {@code @deployment}, {@code @feature}, and {@code @feature:<name>}.
 *
 * @param schemaVersion the schema version being evaluated
 * @param deployment    the deployment identifier (e.g. {@code "cloud"}, {@code "hosted"})
 * @param features      feature-flag values
 */
public record EvalContext(
        int schemaVersion,
        String deployment,
        Map<String, Object> features
) {

    /**
     * Compact canonical constructor that normalises a null features map.
     */
    public EvalContext {
        features = Objects.nonNull(features) ? Map.copyOf(features) : Map.of();
    }

    /**
     * Creates a bare context for simple evaluations.
     *
     * @return a context with no deployment and no features
     */
    public static EvalContext defaultContext() {
        return new EvalContext(1, "default", Map.of());
    }

    /**
     * Starts a fluent builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reads a feature-flag value.
     *
     * @param name the flag name
     * @return the value, or {@code null} if absent
     */
    public Object feature(final String name) {
        return this.features.get(name);
    }

    /**
     * Whether a feature flag is present.
     *
     * @param name the flag name
     * @return {@code true} when present
     */
    public boolean hasFeature(final String name) {
        return this.features.containsKey(name);
    }

    /**
     * Fluent builder for {@link EvalContext}.
     */
    public static final class Builder {
        private int schemaVersion;
        private String deployment;
        private final Map<String, Object> features = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Sets the schema version.
         *
         * @param schemaVersion the version
         * @return this builder
         */
        public Builder schemaVersion(final int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        /**
         * Sets the deployment identifier.
         *
         * @param deployment the deployment
         * @return this builder
         */
        public Builder deployment(final String deployment) {
            this.deployment = deployment;
            return this;
        }

        /**
         * Merges the given feature flags.
         *
         * @param featureFlags the flags to merge
         * @return this builder
         */
        public Builder features(final Map<String, Object> featureFlags) {
            this.features.putAll(featureFlags);
            return this;
        }

        /**
         * Sets a single feature flag.
         *
         * @param name  the flag name
         * @param value the flag value
         * @return this builder
         */
        public Builder feature(final String name,
                               final Object value) {
            this.features.put(name, value);
            return this;
        }

        /**
         * Builds the context.
         *
         * @return a new context
         */
        public EvalContext build() {
            return new EvalContext(this.schemaVersion, this.deployment, this.features);
        }
    }
}
