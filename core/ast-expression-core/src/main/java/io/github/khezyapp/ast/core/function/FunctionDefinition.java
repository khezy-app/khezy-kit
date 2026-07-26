package io.github.khezyapp.ast.core.function;

import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.model.FunctionId;
import io.github.khezyapp.ast.core.model.ParamSpec;
import io.github.khezyapp.ast.core.nullstrategy.NullHandlingStrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defines a complete function registration including its identifier, evaluator,
 * parameter specifications, attributes, and null-handling strategy.
 * <p>
 * A {@code FunctionDefinition} is built using the fluent {@link Builder} and
 * registered in a {@link FunctionRegistry}. It provides validation of arguments
 * against the declared parameter specs before evaluation.
 * </p>
 */
public final class FunctionDefinition {
    private final FunctionId functionId;
    private final Evaluator evaluator;
    private final List<ParamSpec> positionalParams;
    private final Map<String, ParamSpec> namedParams;
    private final FunctionAttributes attributes;
    private final NullHandlingStrategy nullStrategy;

    private FunctionDefinition(final FunctionId functionId, final Evaluator evaluator,
                               final List<ParamSpec> positionalParams,
                               final Map<String, ParamSpec> namedParams,
                               final FunctionAttributes attributes,
                               final NullHandlingStrategy nullStrategy) {
        this.functionId = Objects.requireNonNull(functionId);
        this.evaluator = Objects.requireNonNull(evaluator);
        this.positionalParams = List.copyOf(positionalParams);
        this.namedParams = Map.copyOf(namedParams);
        this.attributes = Objects.requireNonNullElse(attributes, FunctionAttributes.DEFAULT);
        this.nullStrategy = nullStrategy;
    }

    /**
     * Creates a new {@link Builder} for constructing a function definition.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the function identifier.
     *
     * @return the function id
     */
    public FunctionId functionId() {
        return functionId;
    }
    /**
     * Returns the evaluator that performs the function logic.
     *
     * @return the evaluator
     */
    public Evaluator evaluator() {
        return evaluator;
    }
    /**
     * Returns the list of positional parameter specifications.
     *
     * @return an unmodifiable list of param specs
     */
    public List<ParamSpec> positionalParams() {
        return positionalParams;
    }
    /**
     * Returns the map of named parameter specifications.
     *
     * @return an unmodifiable map of param specs keyed by name
     */
    public Map<String, ParamSpec> namedParams() {
        return namedParams;
    }
    /**
     * Returns the function attributes (laziness, cost, commutativity).
     *
     * @return the function attributes
     */
    public FunctionAttributes attributes() {
        return attributes;
    }
    /**
     * Returns the per-function null-handling strategy, if any.
     *
     * @return the null strategy, or {@code null} to use the registry default
     */
    public NullHandlingStrategy nullStrategy() {
        return nullStrategy;
    }

    /**
     * Validates the given arguments against the declared parameter specifications.
     *
     * @param args the arguments to validate
     * @return a list of validation errors (empty if valid)
     */
    public List<EvaluationError> validate(final Arguments args) {
        final var errors = new ArrayList<EvaluationError>();

        if (args.positional().size() < requiredPositionalCount()) {
            errors.add(EvaluationError.of(StandardErrors.WRONG_ARG_COUNT,
                "Expected at least " + requiredPositionalCount()
                    + " positional arguments, got " + args.positional().size(),
                "positional"));
        }

        for (final var spec : namedParams.values()) {
            if (spec.required() && !args.named().containsKey(spec.name())) {
                errors.add(EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                    "Required named argument '" + spec.name() + "' is missing",
                    "named:" + spec.name()));
            }
        }

        return errors;
    }

    private long requiredPositionalCount() {
        return positionalParams.stream().filter(ParamSpec::required).count();
    }

    /**
     * Fluent builder for constructing a {@link FunctionDefinition}.
     * <p>
     * The {@code functionId} and {@code evaluator} fields are required;
     * all others have sensible defaults.
     * </p>
     */
    public static final class Builder {
        private FunctionId functionId;
        private Evaluator evaluator;
        private final List<ParamSpec> positionalParams = new ArrayList<>();
        private final Map<String, ParamSpec> namedParams = new LinkedHashMap<>();
        private FunctionAttributes attributes = FunctionAttributes.DEFAULT;
        private NullHandlingStrategy nullStrategy;

        private Builder() { }

        /**
         * Sets the function identifier (required).
         *
         * @param functionId the function id
         * @return this builder
         */
        public Builder functionId(final FunctionId functionId) {
            this.functionId = functionId;
            return this;
        }

        /**
         * Sets the evaluator implementation (required).
         *
         * @param evaluator the evaluator
         * @return this builder
         */
        public Builder evaluator(final Evaluator evaluator) {
            this.evaluator = evaluator;
            return this;
        }

        /**
         * Adds a positional parameter specification.
         *
         * @param spec the positional parameter spec
         * @return this builder
         */
        public Builder positionalParam(final ParamSpec spec) {
            this.positionalParams.add(spec);
            return this;
        }

        /**
         * Adds a named parameter specification.
         *
         * @param spec the named parameter spec
         * @return this builder
         */
        public Builder namedParam(final ParamSpec spec) {
            this.namedParams.put(spec.name(), spec);
            return this;
        }

        /**
         * Sets the function attributes (default: {@link FunctionAttributes#DEFAULT}).
         *
         * @param attributes the function attributes
         * @return this builder
         */
        public Builder attributes(final FunctionAttributes attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Sets a per-function null-handling strategy.
         *
         * @param strategy the null strategy
         * @return this builder
         */
        public Builder nullStrategy(final NullHandlingStrategy strategy) {
            this.nullStrategy = strategy;
            return this;
        }

        /**
         * Builds the {@link FunctionDefinition}.
         *
         * @return the completed function definition
         * @throws NullPointerException if functionId or evaluator is null
         */
        public FunctionDefinition build() {
            Objects.requireNonNull(functionId, "functionId is required");
            Objects.requireNonNull(evaluator, "evaluator is required");
            return new FunctionDefinition(functionId, evaluator,
                positionalParams, namedParams, attributes, nullStrategy);
        }
    }
}
