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
import java.util.Objects;

/**
 * Page-number based pagination built-in ({@code R9}): pages advance by incrementing a page number
 * by one, with the page size fixed via {@code limitParam}. The continuation is decided by the
 * {@code continueExpression} when configured (e.g. a {@code hasMore} flag), otherwise a full page
 * is assumed to imply another page exists.
 */
public final class PagePagination implements PaginationStrategy {

    private final Integer pageSize;
    private final String rootProperty;
    private final String limitParam;
    private final String pageParam;
    private final boolean inQuery;
    private final Expression continueExpression;
    private final ExpressionEvaluator evaluator;
    private final JsonMapper jsonMapper;
    private int page = 1;
    private int collected;

    private PagePagination(final PaginationSpec spec,
                           final ExpressionEvaluator evaluator,
                           final JsonMapper jsonMapper) {
        this.pageSize = spec.pageSize();
        this.rootProperty = spec.rootProperty();
        this.limitParam = spec.limitParam();
        this.pageParam = spec.offsetParam();
        this.inQuery = spec.inQuery();
        this.continueExpression = spec.continueExpression();
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    /**
     * @param spec       the pagination settings ({@code limitParam} is the page-size parameter,
     *                   {@code offsetParam} the page-number parameter)
     * @param evaluator  the expression evaluator for the optional continuation expression
     * @param jsonMapper the JSON mapper used to count page records
     * @return a new page strategy
     */
    public static PagePagination from(final PaginationSpec spec,
                                      final ExpressionEvaluator evaluator,
                                      final JsonMapper jsonMapper) {
        return new PagePagination(spec, evaluator, jsonMapper);
    }

    @Override
    public boolean shouldPaginate(final RequestPlan plan,
                                  final HttpResult last) {
        if (plan.maxResults() > 0 && collected >= plan.maxResults()) {
            return false;
        }
        if (Objects.nonNull(continueExpression)) {
            return PaginationSupport.truthy(hasMore(last));
        }
        return Objects.nonNull(pageSize)
                && PaginationSupport.countRecords(last, jsonMapper, rootProperty) >= pageSize;
    }

    @Override
    public HttpRequest nextRequest(final RequestPlan plan,
                                   final HttpResult last) {
        page += 1;
        return paginated(plan, page);
    }

    @Override
    public HttpRequest initRequest(final RequestPlan plan) {
        return paginated(plan, page);
    }

    private HttpRequest paginated(final RequestPlan plan,
                                  final int pageNumber) {
        final var builder = plan.request().toBuilder();
        if (inQuery) {
            if (Objects.nonNull(pageSize)) {
                builder.query(limitParam, pageSize);
            }
            builder.query(pageParam, pageNumber);
        } else {
            final var params = new LinkedHashMap<String, Object>();
            if (Objects.nonNull(pageSize)) {
                params.put(limitParam, pageSize);
            }
            params.put(pageParam, pageNumber);
            builder.body(
                    PaginationSupport.withBodyParams(
                            plan.request().body(),
                            jsonMapper,
                            params
                    )
            );
        }
        return builder.build();
    }

    @Override
    public List<OutputRecord> collect(final RequestPlan plan,
                                      final HttpResult last,
                                      final List<OutputRecord> page) {
        collected += PaginationSupport.countRecords(last, jsonMapper, rootProperty);
        return page;
    }

    private Object hasMore(final HttpResult last) {
        final var body = PaginationSupport.readBody(last, jsonMapper);
        final var scope = EvaluationScope.create().bind(EvaluationScope.RESPONSE, body);
        if (continueExpression.isExpression()) {
            return evaluator.evaluate(continueExpression.raw(), scope, Object.class);
        }
        return continueExpression.literal();
    }
}
