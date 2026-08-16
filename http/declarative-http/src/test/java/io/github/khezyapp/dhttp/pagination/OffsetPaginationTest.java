package io.github.khezyapp.dhttp.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OffsetPaginationTest {

    private static final String URL = "https://api.example.com/users";
    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Should advance the offset across full pages")
    void advancesOffsetAcrossPages() {
        final var strategy = OffsetPagination.from(spec(), JSON);
        final var plan = plan(request(0, 10), strategy, 0);
        final var transport = new QueueTransport(page(10, 1), page(10, 11), page(3, 21));

        final var sent = runPages(transport, List.of(10, 10, 3), strategy, plan);

        assertEquals(3, sent.size());
        assertEquals(List.of(0, 10, 20),
                sent.stream().map(request -> request.query().get("offset")).toList());
        assertEquals(List.of(10, 10, 10),
                sent.stream().map(request -> request.query().get("limit")).toList());
    }

    @Test
    @DisplayName("Should stop whens a page returns fewer than pageSize records")
    void stopsOnShortPage() {
        final var strategy = OffsetPagination.from(spec(), JSON);
        final var plan = plan(request(0, 10), strategy, 0);
        final var transport = new QueueTransport(page(3, 1));

        final var sent = runPages(transport, List.of(3), strategy, plan);

        assertEquals(1, sent.size());
    }

    @Test
    @DisplayName("Should stop at the maxResults cap without over-fetching")
    void stopsAtMaxResults() {
        final var strategy = OffsetPagination.from(spec(), JSON);
        final var plan = plan(request(0, 10), strategy, 15);
        final var transport = new QueueTransport(page(10, 1), page(10, 11), page(10, 21));

        final var sent = runPages(transport, List.of(10, 10, 10), strategy, plan);

        assertEquals(2, sent.size());
        assertEquals(List.of(0, 10),
                sent.stream().map(request -> request.query().get("offset")).toList());
    }

    @Test
    @DisplayName("Should seed limit and offset on the first request without query config")
    void seedsFirstRequest() {
        final var strategy = OffsetPagination.from(spec(), JSON);
        final var bare = HttpRequest.builder().url(URL).method(HttpMethod.GET).build();
        final var plan = plan(bare, strategy, 0);

        final var first = strategy.initRequest(plan);

        assertEquals(0, first.query().get("offset"));
        assertEquals(10, first.query().get("limit"));
    }

    @Test
    @DisplayName("Should place limit and offset in the JSON body whens inQuery is false")
    void placesParamsInBody() {
        final var spec = new PaginationSpec("offset", 10, "data.items", "limit", "offset",
                false, null);
        final var strategy = OffsetPagination.from(spec, JSON);
        final var first = HttpRequest.builder().url(URL).method(HttpMethod.GET)
                .body(new Body.JsonBody(JSON.write(Map.of("page", 1)))).build();
        final var plan = plan(first, strategy, 0);
        final var result = page(10, 1);
        strategy.collect(plan, result, records(10));

        assertTrue(strategy.shouldPaginate(plan, result));
        final var next = strategy.nextRequest(plan, result);

        final var parsed = JSON.read(((Body.JsonBody) next.body()).json(), Map.class);
        assertEquals(1, parsed.get("page"));
        assertEquals(10, parsed.get("limit"));
        assertEquals(10, parsed.get("offset"));
    }

    private static PaginationSpec spec() {
        return new PaginationSpec("offset", 10, "data.items", "limit", "offset", true, null);
    }

    private static RequestPlan plan(final HttpRequest first,
                                    final OffsetPagination strategy,
                                    final int maxResults) {
        return new RequestPlan(first, List.of(), List.of(), strategy, maxResults, null);
    }

    private static HttpRequest request(final Object offset,
                                       final Object limit) {
        return HttpRequest.builder().url(URL).method(HttpMethod.GET)
                .query("offset", offset).query("limit", limit).build();
    }

    private static HttpResult page(final int count,
                                   final int startId) {
        final var items = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < count; i++) {
            items.add(Map.of("id", startId + i, "name", "SOK"));
        }
        return HttpResult.of(200, JSON.write(Map.of("data", Map.of("items", items))));
    }

    private static List<OutputRecord> records(final int count) {
        final var records = new ArrayList<OutputRecord>();
        for (int i = 0; i < count; i++) {
            records.add(OutputRecord.ofJson(Map.of("id", i)));
        }
        return records;
    }

    private static List<HttpRequest> runPages(final QueueTransport transport,
                                              final List<Integer> pageSizes,
                                              final OffsetPagination strategy,
                                              final RequestPlan plan) {
        final var sent = new ArrayList<HttpRequest>();
        var request = plan.request();
        for (int i = 0; i < pageSizes.size(); i++) {
            sent.add(request);
            final var result = transport.send(request);
            strategy.collect(plan, result, records(pageSizes.get(i)));
            if (!strategy.shouldPaginate(plan, result)) {
                break;
            }
            request = strategy.nextRequest(plan, result);
        }
        return sent;
    }

    private static final class QueueTransport implements HttpTransport {

        private final Queue<HttpResult> pages;

        QueueTransport(final HttpResult... pages) {
            this.pages = new ArrayDeque<>(Arrays.asList(pages));
        }

        @Override
        public HttpResult send(final HttpRequest request) throws HttpApiException {
            final var next = pages.poll();
            if (next == null) {
                throw new IllegalStateException("no more pages");
            }
            return next;
        }
    }
}
