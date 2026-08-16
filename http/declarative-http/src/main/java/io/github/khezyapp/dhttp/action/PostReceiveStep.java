package io.github.khezyapp.dhttp.action;

import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.spec.PostReceive;

import java.util.Objects;

/**
 * One planned post-receive step: the descriptor plus the concrete {@link PostReceiveAction}
 * materialized from it ({@code R7}). Produced by the planner and executed in order by the pipeline.
 *
 * @param descriptor the post-receive descriptor to execute
 * @param action     the action bound to the descriptor
 */
public record PostReceiveStep(PostReceive descriptor, PostReceiveAction action) {

    public PostReceiveStep {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(action, "action");
    }

    /**
     * Materializes a step from a descriptor via the given registry.
     *
     * @param descriptor the post-receive descriptor
     * @param evaluator  the expression evaluator the action binds to
     * @param registry   the action registry that maps the descriptor to a factory
     * @return the bound step
     */
    public static PostReceiveStep materialize(final PostReceive descriptor,
                                              final ExpressionEvaluator evaluator,
                                              final ActionRegistry registry) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(evaluator, "evaluator");
        Objects.requireNonNull(registry, "registry");
        return new PostReceiveStep(descriptor, registry.create(descriptor, evaluator));
    }
}
