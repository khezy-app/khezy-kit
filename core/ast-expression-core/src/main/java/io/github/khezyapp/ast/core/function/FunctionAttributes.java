package io.github.khezyapp.ast.core.function;

import io.github.khezyapp.ast.core.result.EvaluationResult;
import java.util.function.Predicate;

/**
 * Describes evaluation attributes of a function, including support for lazy
 * (short-circuit) evaluation, commutativity, and computational cost.
 * <p>
 * These attributes are used by the {@link io.github.khezyapp.ast.core.eval.AstEvaluator}
 * to apply optimizations such as short-circuiting logical operators and
 * cost-based reordering of commutative operations.
 * </p>
 *
 * @param lazyChildEvaluation   whether child evaluation should be lazy (short-circuit capable)
 * @param shortCircuitPredicate predicate that determines when to short-circuit
 * @param commutative           whether the function is commutative
 * @param cost                  relative computational cost for optimization
 */
public record FunctionAttributes(
    boolean lazyChildEvaluation,
    Predicate<EvaluationResult> shortCircuitPredicate,
    boolean commutative,
    int cost
) {
    /** Default attributes: no laziness, no commutativity, zero cost. */
    public static final FunctionAttributes DEFAULT = new FunctionAttributes(false, null, false, 0);

    /**
     * Creates attributes for short-circuit evaluation.
     *
     * @param p the predicate that, when true on a child result, triggers short-circuit
     * @return attributes with lazy evaluation enabled
     */
    public static FunctionAttributes shortCircuit(final Predicate<EvaluationResult> p) {
        return new FunctionAttributes(true, p, false, 0);
    }

    /**
     * Creates attributes for a commutative function with the given cost.
     *
     * @param cost the relative computational cost
     * @return attributes with commutativity enabled
     */
    public static FunctionAttributes commutative(final int cost) {
        return new FunctionAttributes(false, null, true, cost);
    }

    /**
     * Creates attributes combining short-circuit and commutativity.
     *
     * @param p    the short-circuit predicate
     * @param cost the relative computational cost
     * @return attributes with both laziness and commutativity enabled
     */
    public static FunctionAttributes shortCircuitCommutative(
            final Predicate<EvaluationResult> p,
            final int cost
    ) {
        return new FunctionAttributes(true, p, true, cost);
    }
}
