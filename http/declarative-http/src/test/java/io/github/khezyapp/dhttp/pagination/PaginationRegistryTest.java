package io.github.khezyapp.dhttp.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaginationRegistryTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Should resolve the four built-in modes")
    void resolvesBuiltins() {
        final var registry = PaginationRegistry.withBuiltins();
        final var evaluator = new JexlExpressionEvaluator();

        assertInstanceOf(OffsetPagination.class, registry.create(
                new PaginationSpec("offset", 10, "data.items", "limit", "offset", true, null),
                evaluator, JSON));
        assertInstanceOf(PagePagination.class, registry.create(
                new PaginationSpec("page", 10, "data.items", "page_size", "page", true, null),
                evaluator, JSON));
        assertInstanceOf(CursorPagination.class, registry.create(
                new PaginationSpec("cursor", 10, "data.items", null, "cursor", true, null),
                evaluator, JSON));
        assertInstanceOf(NextUrlPagination.class, registry.create(
                new PaginationSpec("nextUrl", 10, null, null, null, true,
                        new Expression("= {{ $response.next }}")),
                evaluator, JSON));
    }

    @Test
    @DisplayName("Should fail fast for an unregistered mode")
    void failsFastForUnknownMode() {
        final var registry = PaginationRegistry.withBuiltins();

        final var e = assertThrows(IllegalArgumentException.class, () -> registry.create(
                new PaginationSpec("linkHeader", 10, "data.items", null, null, true, null),
                new JexlExpressionEvaluator(), JSON));

        assertEquals("No pagination strategy registered for mode 'linkHeader'", e.getMessage());
    }

    @Test
    @DisplayName("Should resolve a custom mode through its registered factory")
    void resolvesCustomMode() {
        final var registry = PaginationRegistry.withBuiltins().register("marker",
                (spec, evaluator, jsonMapper) -> new MarkerPagination());

        final var strategy = registry.create(
                new PaginationSpec("marker", 10, "data.items", null, null, true, null),
                new JexlExpressionEvaluator(), JSON);

        assertInstanceOf(MarkerPagination.class, strategy);
    }

    @Test
    @DisplayName("Should create a fresh strategy instance per call")
    void createsFreshStrategyPerPlan() {
        final var registry = PaginationRegistry.withBuiltins().register("marker",
                (spec, evaluator, jsonMapper) -> new MarkerPagination());

        final var first = registry.create(
                new PaginationSpec("marker", 10, "data.items", null, null, true, null),
                new JexlExpressionEvaluator(), JSON);
        final var second = registry.create(
                new PaginationSpec("marker", 10, "data.items", null, null, true, null),
                new JexlExpressionEvaluator(), JSON);

        assertNotSame(first, second);
    }

    @Test
    @DisplayName("Should report an unknown mode as empty through get")
    void reportsUnknownModeAsEmpty() {
        final var registry = PaginationRegistry.withBuiltins();

        assertTrue(registry.get("marker").isEmpty());
    }

    @Test
    @DisplayName("Should pass the spec to the factory so custom strategies read its fields")
    void passesSpecToFactory() {
        final var registry = PaginationRegistry.withBuiltins().register("capturing",
                (spec, evaluator, jsonMapper) -> new CapturingPagination(spec.pageSize()));

        final var strategy = registry.create(
                new PaginationSpec("capturing", 42, "data.items", null, null, true, null),
                new JexlExpressionEvaluator(), JSON);

        assertEquals(42, ((CapturingPagination) strategy).pageSize);
    }

    /** Minimal strategy that marks the registry lookup succeeded. */
    private static final class MarkerPagination implements PaginationStrategy {

        @Override
        public boolean shouldPaginate(final RequestPlan plan,
                                      final HttpResult last) {
            return false;
        }

        @Override
        public HttpRequest nextRequest(final RequestPlan plan,
                                       final HttpResult last) {
            return null;
        }

        @Override
        public List<OutputRecord> collect(final RequestPlan plan,
                                          final HttpResult last,
                                          final List<OutputRecord> page) {
            return page;
        }
    }

    /** Captures the spec value the factory handed over. */
    private static final class CapturingPagination implements PaginationStrategy {

        private final Integer pageSize;

        private CapturingPagination(final Integer pageSize) {
            this.pageSize = pageSize;
        }

        @Override
        public boolean shouldPaginate(final RequestPlan plan,
                                      final HttpResult last) {
            return false;
        }

        @Override
        public HttpRequest nextRequest(final RequestPlan plan,
                                       final HttpResult last) {
            return null;
        }

        @Override
        public List<OutputRecord> collect(final RequestPlan plan,
                                          final HttpResult last,
                                          final List<OutputRecord> page) {
            return page;
        }
    }
}
