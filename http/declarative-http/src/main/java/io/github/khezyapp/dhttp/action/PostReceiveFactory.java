package io.github.khezyapp.dhttp.action;

import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.spec.PostReceive;

/**
 * Builds a {@link PostReceiveAction} from a {@link PostReceive} descriptor ({@code R7}).
 */
@FunctionalInterface
public interface PostReceiveFactory {

    /**
     * @param descriptor the post-receive descriptor (the built-in variant or a custom action)
     * @param evaluator  the expression evaluator the action binds to
     * @return the concrete action
     */
    PostReceiveAction create(PostReceive descriptor, ExpressionEvaluator evaluator);
}
