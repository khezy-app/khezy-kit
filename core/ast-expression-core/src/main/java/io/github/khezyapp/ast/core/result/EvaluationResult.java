package io.github.khezyapp.ast.core.result;

import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.model.CoreFunctions;
import io.github.khezyapp.ast.core.model.FunctionId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Complete result of evaluating an AST node, including the return value,
 * errors, child results, trace metadata, and attributes.
 * <p>
 * An {@code EvaluationResult} is a recursive structure that mirrors the AST:
 * each function node's result contains the results of its children, enabling
 * full tree traversal via {@link #flatten()} and error aggregation via
 * {@link #flattenErrors()}.
 * </p>
 */
public final class EvaluationResult {
    private final FunctionId function;
    private final Object returnValue;
    private final List<EvaluationError> errors;
    private final List<EvaluationResult> children;
    private final Map<String, EvaluationResult> namedChildren;
    private final EvaluationTrace trace;
    private final Map<String, Object> attributes;

    private EvaluationResult(final FunctionId function, final Object returnValue,
                             final List<EvaluationError> errors,
                             final List<EvaluationResult> children,
                             final Map<String, EvaluationResult> namedChildren,
                             final EvaluationTrace trace,
                             final Map<String, Object> attributes) {
        this.function = Objects.requireNonNull(function);
        this.returnValue = returnValue;
        this.errors = List.copyOf(errors);
        this.children = List.copyOf(children);
        this.namedChildren = Map.copyOf(namedChildren);
        this.trace = trace;
        this.attributes = Map.copyOf(attributes);
    }

    /**
     * Creates a result for a constant value node.
     *
     * @param value the constant value
     * @return a new result with {@link CoreFunctions#CONSTANT} function
     */
    public static EvaluationResult constant(final Object value) {
        return new EvaluationResult(CoreFunctions.CONSTANT, value, List.of(),
            List.of(), Map.of(), EvaluationTrace.EVALUATED, Map.of());
    }

    /**
     * Creates a result for a function evaluation from its outcome and children.
     *
     * @param fn            the function identifier
     * @param outcome       the evaluation outcome (success or failure)
     * @param children      the evaluated positional child results
     * @param namedChildren the evaluated named child results
     * @param trace         the evaluation trace metadata
     * @return a new function result
     */
    public static EvaluationResult function(final FunctionId fn,
                                            final EvaluationOutcome outcome,
                                            final List<EvaluationResult> children,
                                            final Map<String, EvaluationResult> namedChildren,
                                            final EvaluationTrace trace) {
        return new EvaluationResult(fn, outcome.value(), outcome.errors(),
            children, namedChildren, trace, outcome.attributes());
    }

    /**
     * Creates a result for a short-circuited function evaluation.
     *
     * @param fn            the function identifier
     * @param value         the short-circuit return value
     * @param children      the evaluated child results (remaining children are skipped)
     * @param namedChildren the evaluated named child results
     * @param durationNanos the evaluation duration in nanoseconds
     * @return a new short-circuit result
     */
    public static EvaluationResult shortCircuit(final FunctionId fn, final Object value,
                                                final List<EvaluationResult> children,
                                                final Map<String, EvaluationResult> namedChildren,
                                                final long durationNanos) {
        return new EvaluationResult(fn, value, List.of(),
            children, namedChildren,
            new EvaluationTrace(false, false, durationNanos), Map.of());
    }

    /**
     * Creates a failure result with the given errors.
     *
     * @param fn            the function identifier
     * @param errors        the evaluation errors
     * @param children      the evaluated child results
     * @param namedChildren the evaluated named child results
     * @param trace         the evaluation trace metadata
     * @return a new failure result
     */
    public static EvaluationResult failure(final FunctionId fn,
                                           final List<EvaluationError> errors,
                                           final List<EvaluationResult> children,
                                           final Map<String, EvaluationResult> namedChildren,
                                           final EvaluationTrace trace) {
        return new EvaluationResult(fn, null, errors,
            children, namedChildren, trace, Map.of());
    }

    /**
     * Creates a skipped result (for children not evaluated due to short-circuit).
     *
     * @param fn the function identifier
     * @return a new skipped result
     */
    public static EvaluationResult skipped(final FunctionId fn) {
        return new EvaluationResult(fn, null, List.of(),
            List.of(), Map.of(), EvaluationTrace.SKIPPED, Map.of());
    }

    /**
     * Returns a new result with the given trace information replacing the existing one.
     *
     * @param newTrace the new trace metadata
     * @return a new result with the updated trace
     */
    public EvaluationResult withTrace(final EvaluationTrace newTrace) {
        return new EvaluationResult(function, returnValue, errors,
            children, namedChildren, newTrace, attributes);
    }

    public FunctionId function() {
        return function;
    }

    /**
     * Returns the return value of this evaluation.
     *
     * @return the return value (may be {@code null})
     */
    public Object returnValue() {
        return returnValue;
    }

    /**
     * Returns the list of errors produced by this evaluation.
     *
     * @return an unmodifiable list of errors
     */
    public List<EvaluationError> errors() {
        return errors;
    }

    /**
     * Returns the evaluated positional child results.
     *
     * @return an unmodifiable list of child results
     */
    public List<EvaluationResult> children() {
        return children;
    }

    /**
     * Returns the evaluated named child results.
     *
     * @return an unmodifiable map of named child results
     */
    public Map<String, EvaluationResult> namedChildren() {
        return namedChildren;
    }

    /**
     * Returns the evaluation trace metadata.
     *
     * @return the trace
     */
    public EvaluationTrace trace() {
        return trace;
    }

    /**
     * Returns the result attributes (metadata key-value pairs).
     *
     * @return an unmodifiable map of attributes
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    /**
     * Retrieves a specific attribute value.
     *
     * @param key the attribute key
     * @return the attribute value, or {@code null}
     */
    public Object getAttribute(final String key) {
        return attributes.get(key);
    }

    /**
     * Checks whether a specific attribute exists.
     *
     * @param key the attribute key
     * @return {@code true} if the attribute is present
     */
    public boolean hasAttribute(final String key) {
        return attributes.containsKey(key);
    }

    /**
     * Recursively collects all errors from this result and its descendants.
     *
     * @return a flat list of all errors in the result tree
     */
    public List<EvaluationError> flattenErrors() {
        final var all = new ArrayList<>(errors);
        for (final var c : children) {
            all.addAll(c.flattenErrors());
        }
        for (final var c : namedChildren.values()) {
            all.addAll(c.flattenErrors());
        }
        return all;
    }

    /**
     * Recursively collects all evaluation results in the tree (depth-first).
     *
     * @return a flat list of all results
     */
    public List<EvaluationResult> flatten() {
        final var all = new ArrayList<EvaluationResult>();
        flattenInto(all);
        return all;
    }

    private void flattenInto(final List<EvaluationResult> acc) {
        acc.add(this);
        for (final var c : children) {
            c.flattenInto(acc);
        }
        for (final var c : namedChildren.values()) {
            c.flattenInto(acc);
        }
    }

    /**
     * Returns whether this result was skipped due to short-circuit evaluation.
     *
     * @return {@code true} if skipped
     */
    public boolean isSkipped() {
        return trace.skipped();
    }

    /**
     * Returns whether this result was served from cache.
     *
     * @return {@code true} if cached
     */
    public boolean isCached() {
        return trace.cached();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationResult that)) {
            return false;
        }
        return function.equals(that.function)
            && Objects.equals(returnValue, that.returnValue)
            && errors.equals(that.errors)
            && children.equals(that.children)
            && namedChildren.equals(that.namedChildren)
            && trace.equals(that.trace)
            && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(function, returnValue, errors,
            children, namedChildren, trace, attributes);
    }

    @Override
    public String toString() {
        return "EvaluationResult{function=" + function.value()
            + ", value=" + returnValue
            + ", errors=" + errors.size()
            + ", trace=" + trace + "}";
    }
}
