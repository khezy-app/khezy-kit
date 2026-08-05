package io.github.khezyapp.ast.core.result;

import io.github.khezyapp.ast.core.error.EvaluationError;
import java.util.List;
import java.util.Map;

/**
 * The immediate outcome of evaluating a single function, consisting of a
 * return value, an optional list of errors, and metadata attributes.
 * <p>
 * An {@code EvaluationOutcome} is the raw result produced by an
 * {@link io.github.khezyapp.ast.core.eval.Evaluator} and is later wrapped
 * into an {@link EvaluationResult} by the evaluation engine.
 * </p>
 *
 * @param value      the return value (may be {@code null})
 * @param errors     the evaluation errors (empty if successful)
 * @param attributes metadata attributes from the evaluator
 */
public record EvaluationOutcome(
    Object value,
    List<EvaluationError> errors,
    Map<String, Object> attributes
) {
    /**
     * Creates a successful outcome with no errors or attributes.
     *
     * @param value the return value
     * @return a success outcome
     */
    public static EvaluationOutcome success(final Object value) {
        return new EvaluationOutcome(value, List.of(), Map.of());
    }

    /**
     * Creates a successful outcome with metadata attributes.
     *
     * @param value      the return value
     * @param attributes metadata attributes
     * @return a success outcome with attributes
     */
    public static EvaluationOutcome success(final Object value,
                                            final Map<String, Object> attributes) {
        return new EvaluationOutcome(value, List.of(), attributes);
    }

    /**
     * Creates a failure outcome from a list of errors.
     *
     * @param errors the evaluation errors
     * @return a failure outcome
     */
    public static EvaluationOutcome failure(final List<EvaluationError> errors) {
        return new EvaluationOutcome(null, errors, Map.of());
    }

    /**
     * Creates a failure outcome from a single error.
     *
     * @param error the evaluation error
     * @return a failure outcome
     */
    public static EvaluationOutcome failure(final EvaluationError error) {
        return new EvaluationOutcome(null, List.of(error), Map.of());
    }
}
