package io.github.khezyapp.dhttp.action.builtin;

import io.github.khezyapp.dhttp.action.ActionRegistry;
import io.github.khezyapp.dhttp.action.PostReceiveAction;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.spec.PostReceive;

import java.util.Objects;

/**
 * Bridges a {@code PostReceive.CustomActionReceive} descriptor to the action registered under its
 * {@code actionKey}.
 */
public final class CustomPostReceive {

    private CustomPostReceive() {
    }

    /**
     * @param descriptor the custom post-receive descriptor to resolve
     * @param registry   the registry holding the factory for {@code actionKey}
     * @param evaluator  the expression evaluator handed to the factory
     * @return the registered action
     * @throws IllegalArgumentException whens no factory is registered under {@code actionKey}
     */
    public static PostReceiveAction from(final PostReceive.CustomPostReceive descriptor,
                                         final ActionRegistry registry,
                                         final ExpressionEvaluator evaluator) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(evaluator, "evaluator");
        return registry.create(descriptor, evaluator);
    }
}
