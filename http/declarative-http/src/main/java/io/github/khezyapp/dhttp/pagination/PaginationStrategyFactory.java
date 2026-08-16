package io.github.khezyapp.dhttp.pagination;

import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.spec.PaginationSpec;

/**
 * Builds a {@link PaginationStrategy} from a {@link PaginationSpec} ({@code R9}).
 *
 * <p>Factories are invoked once per plan, so a strategy may carry per-execution state (page
 * cursors, offsets, collected counts) without leaking across executions.</p>
 */
@FunctionalInterface
public interface PaginationStrategyFactory {

    /**
     * @param spec       the pagination settings (the route's or the spec-default merged fragment)
     * @param evaluator  the expression evaluator used to resolve continuation expressions
     * @param jsonMapper the JSON mapper used to read response bodies
     * @return a fresh strategy for this plan
     */
    PaginationStrategy create(PaginationSpec spec, ExpressionEvaluator evaluator, JsonMapper jsonMapper);
}
