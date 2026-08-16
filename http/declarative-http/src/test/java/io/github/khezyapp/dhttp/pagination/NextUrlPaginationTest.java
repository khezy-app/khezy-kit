package io.github.khezyapp.dhttp.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NextUrlPaginationTest {

    private static final String URL = "https://api.example.com/users";
    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Should follow the next URL from the response, dropping inherited query params")
    void followsNextUrl() {
        final var strategy = strategy();
        final var plan = plan(strategy);
        final var last = HttpResult.of(200, "{\"next\":\"https://api.example.com/users?page=2\"}");

        assertTrue(strategy.shouldPaginate(plan, last));
        final var next = strategy.nextRequest(plan, last);

        assertEquals("https://api.example.com/users?page=2", next.url());
        assertTrue(next.query().isEmpty());
    }

    @Test
    @DisplayName("Should stop when the next URL is absent or blank")
    void stopsWithoutNextUrl() {
        final var strategy = strategy();
        final var plan = plan(strategy);

        assertFalse(strategy.shouldPaginate(plan, HttpResult.of(200, "{}")));
        assertFalse(strategy.shouldPaginate(plan, HttpResult.of(200, "{\"next\":\"\"}")));
        assertNull(strategy.nextRequest(plan, HttpResult.of(200, "{}")));
    }

    @Test
    @DisplayName("Should keep the first request unchanged when no limitParam is configured")
    void keepsFirstRequest() {
        final var strategy = strategy();
        final var plan = plan(strategy);

        assertSame(plan.request(), strategy.initRequest(plan));
    }

    @Test
    @DisplayName("Should seed the optional limit on the first request")
    void seedsLimitOnFirstRequest() {
        final var strategy = strategyWithLimit();
        final var plan = plan(strategy);

        final var first = strategy.initRequest(plan);

        assertEquals(30, first.query().get("limit"));
    }

    @Test
    @DisplayName("Should send the optional limit together with the next URL")
    void appliesLimitToNextUrl() {
        final var strategy = strategyWithLimit();
        final var plan = plan(strategy);
        final var last = HttpResult.of(200, "{\"next\":\"https://api.example.com/users?page=2\"}");

        final var next = strategy.nextRequest(plan, last);

        assertEquals("https://api.example.com/users?page=2", next.url());
        assertEquals(30, next.query().get("limit"));
    }

    @Test
    @DisplayName("Should require a next URL expression at construction")
    void requiresNextUrlExpression() {
        final var spec = new PaginationSpec("nextUrl", 10, null, null, null, true, null);

        assertThrows(NullPointerException.class, () ->
                NextUrlPagination.from(spec, new JexlExpressionEvaluator(), JSON));
    }

    private static NextUrlPagination strategy() {
        final var spec = new PaginationSpec("nextUrl", 10, null, null, null, true,
                new Expression("= {{ $response.next }}"));
        return NextUrlPagination.from(spec, new JexlExpressionEvaluator(), JSON);
    }

    private static NextUrlPagination strategyWithLimit() {
        final var spec = new PaginationSpec("nextUrl", 30, null, "limit", null, true,
                new Expression("= {{ $response.next }}"));
        return NextUrlPagination.from(spec, new JexlExpressionEvaluator(), JSON);
    }

    private static RequestPlan plan(final NextUrlPagination strategy) {
        final var first = HttpRequest.builder().url(URL).method(HttpMethod.GET).build();
        return new RequestPlan(first, List.of(), List.of(), strategy, 0, null);
    }
}
