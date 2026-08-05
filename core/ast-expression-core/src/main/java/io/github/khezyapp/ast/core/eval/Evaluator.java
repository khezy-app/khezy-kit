package io.github.khezyapp.ast.core.eval;

import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

/**
 * Functional interface for evaluating a function with resolved arguments.
 * <p>
 * Implementations receive already-resolved argument values (after null-strategy
 * processing and validation) and produce an {@link EvaluationOutcome}.
 * This is the core abstraction for all built-in and custom evaluators.
 * </p>
 */
@FunctionalInterface
public interface Evaluator {

    /**
     * Evaluates the function logic with the given context and arguments.
     *
     * @param ctx       the evaluation context (registry, payload, clock, etc.)
     * @param arguments the resolved positional and named arguments
     * @return the evaluation outcome (success with value, or failure with errors)
     */
    EvaluationOutcome evaluate(EvaluationContext ctx,
                                Arguments arguments);

}
