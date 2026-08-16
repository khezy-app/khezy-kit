package io.github.khezyapp.dhttp.pagination;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.EvaluationScope;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cursor-based pagination built-in ({@code R9}): a continuation expression is evaluated against the
 * last response to decide whether more pages exist and to resolve the next cursor value, which is
 * then applied to the request query string or body.
 */
public final class CursorPagination implements PaginationStrategy {

    private final String cursorParam;
    private final boolean inQuery;
    private final String rootProperty;
    private final String limitParam;
    private final Integer pageSize;
    private final Expression continueExpression;
    private final ExpressionEvaluator evaluator;
    private final JsonMapper jsonMapper;
    private int collected;

    private CursorPagination(final PaginationSpec spec,
                             final ExpressionEvaluator evaluator,
                             final JsonMapper jsonMapper) {
        this.cursorParam = spec.offsetParam();
        this.inQuery = spec.inQuery();
        this.rootProperty = spec.rootProperty();
        this.limitParam = spec.limitParam();
        this.pageSize = spec.pageSize();
        this.continueExpression = spec.continueExpression();
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    /**
     * @param spec       the pagination settings
     * @param evaluator  the expression evaluator for the continuation expression
     * @param jsonMapper the JSON mapper used to read the response body
     * @return a new cursor strategy
     */
    public static CursorPagination from(final PaginationSpec spec,
                                        final ExpressionEvaluator evaluator,
                                        final JsonMapper jsonMapper) {
        return new CursorPagination(spec, evaluator, jsonMapper);
    }

    @Override
    public boolean shouldPaginate(final RequestPlan plan,
                                  final HttpResult last) {
        if (plan.maxResults() > 0 && collected >= plan.maxResults()) {
            return false;
        }
        return PaginationSupport.truthy(cursor(last));
    }

    @Override
    public HttpRequest initRequest(final RequestPlan plan) {
        return PaginationSupport.applyParams(plan.request(), limitParams(), inQuery, jsonMapper);
    }

    @Override
    public HttpRequest nextRequest(final RequestPlan plan,
                                   final HttpResult last) {
        final var cursorValue = cursor(last);
        if (!PaginationSupport.truthy(cursorValue)) {
            return null;
        }
        final var params = new LinkedHashMap<String, Object>();
        params.putAll(limitParams());
        params.put(cursorParam, cursorValue);
        return PaginationSupport.applyParams(plan.request(), params, inQuery, jsonMapper);
    }

    /**
     * @return the optional page-size parameter, empty whens {@code limitParam} or {@code pageSize}
     *         is not configured
     */
    private Map<String, Object> limitParams() {
        final var params = new LinkedHashMap<String, Object>();
        if (Objects.nonNull(limitParam) && !limitParam.isBlank() && Objects.nonNull(pageSize)) {
            params.put(limitParam, pageSize);
        }
        return params;
    }

    @Override
    public List<OutputRecord> collect(final RequestPlan plan,
                                      final HttpResult last,
                                      final List<OutputRecord> page) {
        collected += PaginationSupport.countRecords(last, jsonMapper, rootProperty);
        return page;
    }

    private Object cursor(final HttpResult last) {
        final var body = PaginationSupport.readBody(last, jsonMapper);
        final var scope = EvaluationScope.create().bind(EvaluationScope.RESPONSE, body);
        if (continueExpression.isExpression()) {
            return evaluator.evaluate(continueExpression.raw(), scope, Object.class);
        }
        return continueExpression.literal();
    }
}
