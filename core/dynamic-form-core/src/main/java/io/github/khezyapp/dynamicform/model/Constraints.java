package io.github.khezyapp.dynamicform.model;

import java.util.List;
import java.util.Objects;

/**
 * Static constraints applied by the {@code Validator} after coercion.
 *
 * @param required     the field is mandatory unconditionally
 * @param requiredWhen the field is mandatory only while a dependency condition holds
 * @param min          lower bound (numeric or comparable); item count for {@code COLLECTION} via its spec
 * @param max          upper bound (numeric or comparable)
 * @param scale        decimal places for {@code DECIMAL} (money precision), also used to detect overflow
 * @param precision    total digit count for {@code DECIMAL(p,s)} overflow detection
 * @param minLength    minimum string length
 * @param maxLength    maximum string length
 * @param pattern      regex the string value must fully match
 * @param noExpression the value must not contain an expression (e.g. {@code ${...}})
 */
public record Constraints(
        boolean required,
        List<RequiredWhen> requiredWhen,
        Object min,
        Object max,
        Integer scale,
        Integer precision,
        Integer minLength,
        Integer maxLength,
        String pattern,
        boolean noExpression
) {

    /**
     * Compact canonical constructor that normalises a null requiredWhen list.
     */
    public Constraints {
        requiredWhen = Objects.nonNull(requiredWhen) ? List.copyOf(requiredWhen) : List.of();
    }

    /**
     * Creates an empty constraint set.
     *
     * @return an unconstrained field
     */
    public static Constraints of() {
        return new Constraints(false, null, null, null, null, null, null, null, null, false);
    }

    /**
     * Creates an unconditionally required field.
     *
     * @return a required constraint set
     */
    public static Constraints mandatory() {
        return new Constraints(true, null, null, null, null, null, null, null, null, false);
    }

    /**
     * Starts a fluent builder for a constraint set.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Constraints}.
     */
    public static final class Builder {
        private boolean required;
        private List<RequiredWhen> requiredWhen;
        private Object min;
        private Object max;
        private Integer scale;
        private Integer precision;
        private Integer minLength;
        private Integer maxLength;
        private String pattern;
        private boolean noExpression;

        private Builder() {
        }

        /**
         * Sets whether the field is unconditionally required.
         *
         * @param required {@code true} to require the field
         * @return this builder
         */
        public Builder required(final boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Sets the conditional-required rules.
         *
         * @param requiredWhen the rules, may be empty
         * @return this builder
         */
        public Builder requiredWhen(final RequiredWhen... requiredWhen) {
            this.requiredWhen = List.of(requiredWhen);
            return this;
        }

        /**
         * Sets the lower bound.
         *
         * @param min the lower bound
         * @return this builder
         */
        public Builder min(final Object min) {
            this.min = min;
            return this;
        }

        /**
         * Sets the upper bound.
         *
         * @param max the upper bound
         * @return this builder
         */
        public Builder max(final Object max) {
            this.max = max;
            return this;
        }

        /**
         * Sets the decimal scale.
         *
         * @param scale the number of decimal places
         * @return this builder
         */
        public Builder scale(final Integer scale) {
            this.scale = scale;
            return this;
        }

        /**
         * Sets the total precision.
         *
         * @param precision the total digit count
         * @return this builder
         */
        public Builder precision(final Integer precision) {
            this.precision = precision;
            return this;
        }

        /**
         * Sets the minimum string length.
         *
         * @param minLength the minimum length
         * @return this builder
         */
        public Builder minLength(final Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        /**
         * Sets the maximum string length.
         *
         * @param maxLength the maximum length
         * @return this builder
         */
        public Builder maxLength(final Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        /**
         * Sets the regex pattern.
         *
         * @param pattern the pattern
         * @return this builder
         */
        public Builder pattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * Sets whether expressions are disallowed.
         *
         * @param noExpression {@code true} to reject expression values
         * @return this builder
         */
        public Builder noExpression(final boolean noExpression) {
            this.noExpression = noExpression;
            return this;
        }

        /**
         * Builds the constraint set.
         *
         * @return the immutable constraints
         */
        public Constraints build() {
            return new Constraints(this.required, this.requiredWhen, this.min, this.max, this.scale,
                    this.precision, this.minLength, this.maxLength, this.pattern, this.noExpression);
        }
    }
}
