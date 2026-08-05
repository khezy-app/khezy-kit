package io.github.khezyapp.ast.core.nullstrategy;

import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.model.ParamSpec;
import java.util.Optional;

/**
 * Strategy interface for handling null argument values during evaluation.
 * <p>
 * When a resolved argument is {@code null}, the evaluation engine invokes
 * the applicable strategy to decide whether to propagate the null, substitute
 * a default value, or fail with an error. Strategies can be defined per-function
 * or at the registry level.
 * </p>
 *
 * @see NullStrategies
 */
@FunctionalInterface
public interface NullHandlingStrategy {
    /**
     * Handles a null value for the given parameter specification.
     *
     * @param spec the parameter specification
     * @param args the full argument set (for context)
     * @return an {@link Optional} containing the replacement value, or empty
     *         to propagate the null
     */
    Optional<Object> handleNull(ParamSpec spec, Arguments args);
}
