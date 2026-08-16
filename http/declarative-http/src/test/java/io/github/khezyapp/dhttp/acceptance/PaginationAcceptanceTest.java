package io.github.khezyapp.dhttp.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.config.DeclarativeHttp;
import io.github.khezyapp.dhttp.config.DeclarativeHttpConfig;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * §9 acceptance item 3: offset pagination with {@code pageSize} and {@code maxResults} capping
 * through the assembled facade — asserting the request count and the total collected records.
 */
class PaginationAcceptanceTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Item 3: offset pagination advances pages and caps the total by maxResults")
    void offsetPaginationCapsByMaxResults() {
        final var transport = new QueueTransport(page(10, 1), page(10, 11), page(3, 21));
        final var http = facade(transport);
        final var pagination = new PaginationSpec("offset", 10, "data.items", "limit", "offset",
                true, null);
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(15, List.of(new PostReceive.RootProperty("data.items"))), pagination,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = http.execute(spec, new RequestContext("user.list", Map.of()));

        assertEquals(15, records.size());
        assertEquals(2, transport.callCount());
        assertEquals(0, transport.requests().get(0).query().get("offset"));
        assertEquals(10, transport.requests().get(0).query().get("limit"));
        assertEquals(10, transport.requests().get(1).query().get("offset"));
        assertEquals(10, transport.requests().get(1).query().get("limit"));
        assertEquals(15, records.stream().mapToInt(record -> (int) record.json().get("id")).max()
                .orElseThrow());
    }

    @Test
    @DisplayName("Item 3: an incomplete final page is still collected and returned")
    void incompleteFinalPageCollected() {
        final var transport = new QueueTransport(page(3, 1), page(2, 4));
        final var http = facade(transport);
        final var pagination = new PaginationSpec("offset", 10, "data.items", "limit", "offset",
                true, null);
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data.items"))), pagination,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = http.execute(spec, new RequestContext("user.list", Map.of()));

        assertEquals(1, transport.callCount());
        assertEquals(3, records.size());
    }

    @Test
    @DisplayName("Page pagination advances page numbers and caps the total by maxResults")
    void pagePaginationAdvancesAndCaps() {
        final var transport = new QueueTransport(page(10, 1), page(10, 11), page(3, 21));
        final var http = facade(transport);
        final var pagination = new PaginationSpec("page", 10, "data.items", "page_size", "page",
                true, null);
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(15, List.of(new PostReceive.RootProperty("data.items"))), pagination,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = http.execute(spec, new RequestContext("user.list", Map.of()));

        assertEquals(15, records.size());
        assertEquals(2, transport.callCount());
        assertEquals(1, transport.requests().get(0).query().get("page"));
        assertEquals(10, transport.requests().get(0).query().get("page_size"));
        assertEquals(2, transport.requests().get(1).query().get("page"));
        assertEquals(10, transport.requests().get(1).query().get("page_size"));
    }

    @Test
    @DisplayName("Next-URL pagination follows the returned next URL and stops on a page without one")
    void nextUrlPaginationFollowsNext() {
        final var transport = new QueueTransport(
                pageWithNext(10, 1, "https://api.example.com/users?page=2"),
                page(3, 11));
        final var http = facade(transport);
        final var pagination = new PaginationSpec("nextUrl", 10, "data.items", null, null, true,
                new Expression("= {{ $response.data.next }}"));
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data.items"))), pagination,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = http.execute(spec, new RequestContext("user.list", Map.of()));

        assertEquals(13, records.size());
        assertEquals(2, transport.callCount());
        assertEquals("https://api.example.com/users?page=2", transport.requests().get(1).url());
        assertTrue(transport.requests().get(1).query().isEmpty());
    }

    @Test
    @DisplayName("Offset pagination caps by the raw page record count, not the shaped record count")
    void capsByRawRecordCount() {
        final var transport = new QueueTransport(page(10, 1), page(10, 11), page(10, 21),
                page(10, 31));
        final var http = facade(transport);
        final var pagination = new PaginationSpec("offset", 10, "data.items", "limit", "offset",
                true, null);
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(30, List.of()), pagination, List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = http.execute(spec, new RequestContext("user.list", Map.of()));

        assertEquals(3, transport.callCount());
        assertEquals(10, transport.requests().get(1).query().get("offset"));
        assertEquals(20, transport.requests().get(2).query().get("offset"));
    }

    @Test
    @DisplayName("Cursor pagination sends the limit and cursor on every request")
    void cursorPaginationPassesLimitAndCursor() {
        final var transport = new QueueTransport(
                cursorPage(30, 1, "cursor-2"),
                cursorPage(30, 31, "cursor-3"),
                cursorPage(5, 61, null));
        final var http = facade(transport);
        final var pagination = new PaginationSpec("cursor", 30, "data", "limit", "cursor",
                true, new Expression("= {{ $response.nextCursor }}"));
        final var shape = new RequestShape(HttpMethod.GET, "/regions", Map.of(), Map.of(), null, null);
        final var operation = new Operation("regions.get", new Route(shape, List.of(),
                new Output(65, List.of(new PostReceive.RootProperty("data"))), pagination,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = http.execute(spec, new RequestContext("regions.get", Map.of()));

        assertEquals(3, transport.callCount());
        assertEquals(30, transport.requests().get(0).query().get("limit"));
        assertEquals(30, transport.requests().get(1).query().get("limit"));
        assertEquals("cursor-2", transport.requests().get(1).query().get("cursor"));
        assertEquals(30, transport.requests().get(2).query().get("limit"));
        assertEquals("cursor-3", transport.requests().get(2).query().get("cursor"));
        assertEquals(65, records.size());
    }

    private static DeclarativeHttp facade(final HttpTransport transport) {
        final var config = DeclarativeHttpConfig.builder()
                .transport(transport)
                .keyProvider(PaginationAcceptanceTest::newKey)
                .build();
        return DeclarativeHttp.create(config);
    }

    private static SecretKey newKey() {
        try {
            final var generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (final java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("AES unavailable", e);
        }
    }

    private static HttpResult page(final int count,
                                   final int startId) {
        return HttpResult.of(200, JSON.write(Map.of("data", Map.of("items", items(count, startId)))));
    }

    private static HttpResult pageWithNext(final int count,
                                           final int startId,
                                           final String nextUrl) {
        return HttpResult.of(200, JSON.write(Map.of("data", Map.of("items", items(count, startId),
                "next", nextUrl))));
    }

    private static HttpResult cursorPage(final int count,
                                         final int startId,
                                         final String nextCursor) {
        final var body = new java.util.LinkedHashMap<String, Object>();
        body.put("data", items(count, startId));
        if (nextCursor != null) {
            body.put("nextCursor", nextCursor);
        }
        return HttpResult.of(200, JSON.write(body));
    }

    private static List<Map<String, Object>> items(final int count,
                                                   final int startId) {
        final var items = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < count; i++) {
            items.add(Map.of("id", startId + i, "name", "SOK"));
        }
        return items;
    }

    private static final class QueueTransport implements HttpTransport {

        private final Queue<HttpResult> pages;
        private final List<HttpRequest> requests = new ArrayList<>();

        private QueueTransport(final HttpResult... pages) {
            this.pages = new ArrayDeque<>(Arrays.asList(pages));
        }

        @Override
        public HttpResult send(final HttpRequest request) throws HttpApiException {
            requests.add(request);
            final var next = pages.poll();
            if (next == null) {
                throw new IllegalStateException("no more pages");
            }
            return next;
        }

        private int callCount() {
            return requests.size();
        }

        private List<HttpRequest> requests() {
            return requests;
        }
    }
}
