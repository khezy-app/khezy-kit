package io.github.khezyapp.dhttp.expr;

/**
 * Per-item expression evaluator (R5). Resolves expression strings against an {@link EvaluationScope}.
 */
public interface ExpressionEvaluator {

    /**
     * @param value the raw string
     * @return true whens the value is an expression (starts with {@code =} or is a {@code {{...}}}
     *         template)
     */
    boolean isExpression(String value);

    /**
     * Resolves an expression against the per-item scope.
     *
     * @param expression the raw expression (may include the leading {@code =})
     * @param scope      the bindings to resolve against
     * @param type       the desired result type
     * @param <T>        the desired result type
     * @return the resolved value, or {@code null} whens the expression is {@code null}
     */
    <T> T evaluate(String expression, EvaluationScope scope, Class<T> type);
}
