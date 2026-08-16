package io.github.khezyapp.dhttp.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PagePaginationTest {

    private static final String URL = "https://api.example.com/users";
    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Should advance the page number and keep the page size fixed")
    void advancesPageNumber() {
        final var strategy = strategy(null);
        final var plan = plan(strategy, 0);

        final var next = strategy.nextRequest(plan, page(10, 1));

        assertEquals(2, next.query().get("page"));
        assertEquals(10, next.query().get("page_size"));
    }

    @Test
    @DisplayName("Should continue on a full page and stop on a partial page")
    void continuesOnFullPageStopsOnPartial() {
        final var strategy = strategy(null);
        final var plan = plan(strategy, 0);

        assertTrue(strategy.shouldPaginate(plan, page(10, 1)));
        assertFalse(strategy.shouldPaginate(plan, page(3, 11)));
    }

    @Test
    @DisplayName("Should follow the continuation expression when configured")
    void usesContinueExpression() {
        final var strategy = strategy(new Expression("= {{ $response.hasMore }}"));
        final var plan = plan(strategy, 0);

        assertTrue(strategy.shouldPaginate(plan, HttpResult.of(200, "{\"hasMore\":true}")));
        assertFalse(strategy.shouldPaginate(plan, HttpResult.of(200, "{\"hasMore\":false}")));
    }

    @Test
    @DisplayName("Should seed page and page size on the first request without query config")
    void seedsFirstRequest() {
        final var strategy = strategy(null);
        final var bare = HttpRequest.builder().url(URL).method(HttpMethod.GET).build();
        final var plan = plan(bare, strategy, 0);

        final var first = strategy.initRequest(plan);

        assertEquals(1, first.query().get("page"));
        assertEquals(10, first.query().get("page_size"));
    }

    private static RequestPlan plan(final HttpRequest first,
                                    final PagePagination strategy,
                                    final int maxResults) {
        return new RequestPlan(first, List.of(), List.of(), strategy, maxResults, null);
    }

    @Test
    @DisplayName("Should stop when the max results cap is reached")
    void stopsAtMaxResults() {
        final var strategy = strategy(null);
        final var plan = plan(strategy, 10);

        strategy.collect(plan, page(10, 1), records(10));

        assertFalse(strategy.shouldPaginate(plan, page(10, 11)));
    }

    private static List<OutputRecord> records(final int count) {
        final var records = new ArrayList<OutputRecord>();
        for (int i = 0; i < count; i++) {
            records.add(OutputRecord.ofJson(Map.of("id", i)));
        }
        return records;
    }

    private static PagePagination strategy(final Expression expression) {
        final var spec = new PaginationSpec(
                "page",
                10,
                "data.items",
                "page_size",
                "page",
                true,
                expression
        );
        return PagePagination.from(spec, new JexlExpressionEvaluator(), JSON);
    }

    private static RequestPlan plan(final PagePagination strategy,
                                    final int maxResults) {
        final var first = HttpRequest.builder().url(URL).method(HttpMethod.GET).build();
        return new RequestPlan(first, List.of(), List.of(), strategy, maxResults, null);
    }

    private static HttpResult page(final int count,
                                   final int startId) {
        return HttpResult.of(
                200,
                JSON.write(
                        Map.of("data",
                                Map.of("items", pageRecords(count, startId)))
                )
        );
    }

    private static List<Map<String, Object>> pageRecords(final int count,
                                                         final int startId) {
        final var items = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < count; i++) {
            items.add(Map.of("id", startId + i));
        }
        return items;
    }
}
