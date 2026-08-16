package io.github.khezyapp.dhttp.plan;

import io.github.khezyapp.dhttp.spec.Condition;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.doa.DynamicObjects;

import java.util.List;
import java.util.Objects;

/**
 * Precondition gating for operation selection ({@code R3}).
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    /**
     * @param when the operation's preconditions (may be empty)
     * @param ctx  the per-item context
     * @return true whens every condition passes (an empty list always passes)
     */
    public static boolean evaluate(final List<Condition> when,
                                   final RequestContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        if (Objects.isNull(when) || when.isEmpty()) {
            return true;
        }
        for (final Condition condition : when) {
            if (!matches(condition, ctx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Selects the operation whose id equals the context's {@code operationId} and whose
     * {@code whens} all pass ({@code R3}).
     *
     * <p>When several operations share the same id (variants of one operation), the first whose
     * conditions pass wins, so conditioned variants must precede a no-condition fallback.</p>
     *
     * @param spec the root spec
     * @param ctx  the per-item context
     * @return the matching operation, or {@code null} whens none matches
     */
    public static Operation selectOperation(final HttpRequestSpec spec,
                                            final RequestContext ctx) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(ctx, "ctx");
        for (final Operation operation : spec.operations()) {
            if (ctx.operationId().equals(operation.id())
                    && evaluate(operation.whens(), ctx)) {
                return operation;
            }
        }
        return null;
    }

    private static boolean matches(final Condition condition,
                                   final RequestContext ctx) {
        final var value = DynamicObjects.get(ctx.parameters(), condition.property());
        if (Objects.nonNull(condition.exists())) {
            final var expected = "true".equalsIgnoreCase(condition.exists());
            return expected == Objects.nonNull(value);
        }
        if (Objects.nonNull(condition.equals())) {
            return Objects.equals(value, condition.equals());
        }
        return Objects.nonNull(value);
    }
}
