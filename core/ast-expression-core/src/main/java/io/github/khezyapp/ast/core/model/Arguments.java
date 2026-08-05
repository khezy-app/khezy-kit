package io.github.khezyapp.ast.core.model;

import java.util.List;
import java.util.Map;

/**
 * Holds both positional and named arguments for evaluator invocation.
 * <p>
 * After child nodes are evaluated in the engine pipeline, the resulting values
 * are assembled into an {@code Arguments} instance and passed to the
 * {@link io.github.khezyapp.ast.core.eval.Evaluator#evaluate} method.
 * </p>
 *
 * @param positional the positional argument values
 * @param named      the named argument values keyed by parameter name
 */
public record Arguments(
    List<Object> positional,
    Map<String, Object> named
) {
    /**
     * Creates arguments with positional values only and an empty named map.
     *
     * @param positional the positional argument values
     * @return new arguments with the given positional values
     */
    public static Arguments of(final List<Object> positional) {
        return new Arguments(positional, Map.of());
    }
}
