package io.github.khezyapp.dhttp.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorPaginationTest {

    private static final String URL = "https://api.example.com/users";
    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Should apply the resolved cursor to the next request")
    void appliesResolvedCursor() {
        final var strategy = strategy("= {{ $response.data.nextCursor }}");
        final var plan = plan(strategy);
        final var last = HttpResult.of(200, "{\"data\":{\"nextCursor\":\"abc123\"}}");

        assertTrue(strategy.shouldPaginate(plan, last));
        final var next = strategy.nextRequest(plan, last);

        assertEquals("abc123", next.query().get("cursor"));
    }

    @Test
    @DisplayName("Should stop whens the continuation resolves to null")
    void stopsWhenCursorIsNull() {
        final var strategy = strategy("= {{ $response.data.nextCursor }}");
        final var plan = plan(strategy);

        final var last = HttpResult.of(200, "{\"data\":{}}");

        assertFalse(strategy.shouldPaginate(plan, last));
    }

    @Test
    @DisplayName("Should stop on a literal false continuation")
    void stopsOnLiteralFalse() {
        final var strategy = strategy("false");
        final var plan = plan(strategy);

        assertFalse(strategy.shouldPaginate(plan, HttpResult.of(200, "{}")));
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
    @DisplayName("Should carry the optional limit together with the cursor on next requests")
    void appliesLimitWithCursor() {
        final var strategy = strategyWithLimit();
        final var plan = plan(strategy);

        final var next = strategy.nextRequest(plan,
                HttpResult.of(200, "{\"nextCursor\":\"abc123\"}"));

        assertEquals("abc123", next.query().get("cursor"));
        assertEquals(30, next.query().get("limit"));
    }

    @Test
    @DisplayName("Should place the limit and cursor in the JSON body whens inQuery is false")
    void placesLimitAndCursorInBody() {
        final var spec = new PaginationSpec("cursor", 30, "data.items", "limit", "cursor", false,
                new Expression("= {{ $response.nextCursor }}"));
        final var strategy = CursorPagination.from(spec, new JexlExpressionEvaluator(), JSON);
        final var bare = HttpRequest.builder().url(URL).method(HttpMethod.GET)
                .body(new Body.JsonBody(JSON.write(Map.of("page", 1)))).build();
        final var plan = new RequestPlan(bare, List.of(), List.of(), strategy, 0, null);

        final var next = strategy.nextRequest(plan, HttpResult.of(200, "{\"nextCursor\":\"x\"}"));

        final var parsed = JSON.read(((Body.JsonBody) next.body()).json(), Map.class);
        assertEquals(1, parsed.get("page"));
        assertEquals(30, parsed.get("limit"));
        assertEquals("x", parsed.get("cursor"));
    }

    private static CursorPagination strategyWithLimit() {
        final var spec = new PaginationSpec("cursor", 30, "data.items", "limit", "cursor", true,
                new Expression("= {{ $response.nextCursor }}"));
        return CursorPagination.from(spec, new JexlExpressionEvaluator(), JSON);
    }

    private static CursorPagination strategy(final String expression) {
        final var spec = new PaginationSpec("cursor", 10, null, null, "cursor", true,
                new Expression(expression));
        return CursorPagination.from(spec, new JexlExpressionEvaluator(), JSON);
    }

    private static RequestPlan plan(final CursorPagination strategy) {
        final var first = HttpRequest.builder().url(URL).method(HttpMethod.GET).build();
        return new RequestPlan(first, List.of(), List.of(), strategy, 0, null);
    }
}
