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
 * Next-URL pagination built-in ({@code R9}): the {@code continueExpression} is evaluated against
 * the last response to resolve the absolute URL of the next page, which is then fetched as-is. Any
 * query parameters inherited from the previous request are dropped because the next URL is
 * authoritative; whens {@code limitParam} is configured it is sent with every request.
 */
public final class NextUrlPagination implements PaginationStrategy {

    private final Expression nextUrlExpression;
    private final ExpressionEvaluator evaluator;
    private final JsonMapper jsonMapper;
    private final String rootProperty;
    private final String limitParam;
    private final Integer pageSize;
    private final boolean inQuery;
    private int collected;

    private NextUrlPagination(final PaginationSpec spec,
                              final ExpressionEvaluator evaluator,
                              final JsonMapper jsonMapper) {
        this.nextUrlExpression = Objects.requireNonNull(spec.continueExpression(),
                "nextUrl mode requires a continueExpression that resolves the next URL");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
        this.rootProperty = spec.rootProperty();
        this.limitParam = spec.limitParam();
        this.pageSize = spec.pageSize();
        this.inQuery = spec.inQuery();
    }

    /**
     * @param spec       the pagination settings ({@code continueExpression} must resolve the next
     *                   URL from {@code $response})
     * @param evaluator  the expression evaluator for the next-URL expression
     * @param jsonMapper the JSON mapper used to read the response body
     * @return a new next-URL strategy
     */
    public static NextUrlPagination from(final PaginationSpec spec,
                                         final ExpressionEvaluator evaluator,
                                         final JsonMapper jsonMapper) {
        return new NextUrlPagination(spec, evaluator, jsonMapper);
    }

    @Override
    public boolean shouldPaginate(final RequestPlan plan,
                                  final HttpResult last) {
        if (plan.maxResults() > 0 && collected >= plan.maxResults()) {
            return false;
        }
        return !blank(nextUrl(last));
    }

    @Override
    public HttpRequest initRequest(final RequestPlan plan) {
        return PaginationSupport.applyParams(plan.request(), limitParams(), inQuery, jsonMapper);
    }

    @Override
    public HttpRequest nextRequest(final RequestPlan plan,
                                   final HttpResult last) {
        final var nextUrl = nextUrl(last);
        if (blank(nextUrl)) {
            return null;
        }
        final var next = plan.request().toBuilder().clearQuery().url(nextUrl).build();
        return PaginationSupport.applyParams(next, limitParams(), inQuery, jsonMapper);
    }

    @Override
    public List<OutputRecord> collect(final RequestPlan plan,
                                      final HttpResult last,
                                      final List<OutputRecord> page) {
        collected += PaginationSupport.countRecords(last, jsonMapper, rootProperty);
        return page;
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

    private String nextUrl(final HttpResult last) {
        final var body = PaginationSupport.readBody(last, jsonMapper);
        final var scope = EvaluationScope.create().bind(EvaluationScope.RESPONSE, body);
        final Object value;
        if (nextUrlExpression.isExpression()) {
            value = evaluator.evaluate(nextUrlExpression.raw(), scope, Object.class);
        } else {
            value = nextUrlExpression.literal();
        }
        return Objects.isNull(value) ? null : String.valueOf(value);
    }

    private static boolean blank(final String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
