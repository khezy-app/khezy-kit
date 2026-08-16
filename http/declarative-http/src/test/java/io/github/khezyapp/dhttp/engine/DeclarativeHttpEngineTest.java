package io.github.khezyapp.dhttp.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.action.ActionRegistry;
import io.github.khezyapp.dhttp.auth.credential.CredentialStore;
import io.github.khezyapp.dhttp.auth.credential.DecryptedCredential;
import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.auth.oauth2.InMemoryTokenStore;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Grant;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2Token;
import io.github.khezyapp.dhttp.auth.oauth2.TokenStore;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.error.OAuth2NotConfiguredException;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.pagination.PaginationRegistry;
import io.github.khezyapp.dhttp.pagination.PaginationStrategy;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.Condition;
import io.github.khezyapp.dhttp.spec.CredentialRef;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.spec.Send;
import io.github.khezyapp.dhttp.spec.Target;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeclarativeHttpEngineTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Should execute a Brevo-style spec with the exact resolved request and output records")
    void executesBrevoStyleSpec() {
        final var transport = new FakeTransport(HttpResult.of(200,
                JSON.write(Map.of("data", Map.of("id", 42, "name", "SOK")))));
        final var store = MapCredentialStore.of(Map.of("brevo", new DecryptedCredential<>("brevo",
                "api-key", Map.of("headerName", "api-key", "value", "xkeysib-super-secret"), null)));
        final var engine = newEngine(transport, store, new InMemoryTokenStore());

        final var records = engine.execute(brevoSpec(), new RequestContext("contact.create",
                Map.of("contact", Map.of("name", "SOK", "email", "sok@example.com",
                        "attributes", Map.of("city", "Battambang")))));

        assertEquals(HttpMethod.POST, transport.lastRequest().method());
        assertEquals("https://api.brevo.com/v3/contacts", transport.lastRequest().url());
        assertEquals("application/json", transport.lastRequest().headers().first("Accept").orElseThrow());
        assertEquals("xkeysib-super-secret",
                transport.lastRequest().headers().first("api-key").orElseThrow());
        final var parsed = JSON.read(((Body.JsonBody) transport.lastRequest().body()).json(), Map.class);
        assertEquals(Map.of("city", "Battambang"), parsed.get("attributes"));

        assertEquals(1, records.size());
        assertEquals(Map.of("id", 42, "name", "SOK"), records.get(0).json());
    }

    @Test
    @DisplayName("Should route a shared operation id to the provider-selected full URL")
    void routesSharedOperationIdToProviderUrl() {
        final var transport = new FakeTransport(HttpResult.of(200, "{\"ok\":true}"));
        final var engine = newEngine(transport, MapCredentialStore.of(Map.of()),
                new InMemoryTokenStore());
        final var visa = new Operation("payment.execute",
                List.of(new Condition("provider", "visa")),
                new Route(new RequestShape(HttpMethod.POST, "https://visa.example.com/pay",
                        Map.of(), Map.of(), null, null), Output.of(10)));
        final var mastercard = new Operation("payment.execute",
                List.of(new Condition("provider", "mastercard")),
                new Route(new RequestShape(HttpMethod.POST, "https://mastercard.example.com/pay",
                        Map.of(), Map.of(), null, null), Output.of(10)));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(visa, mastercard), null, null, SecurityPolicy.defaults());

        engine.execute(spec, new RequestContext("payment.execute", Map.of("provider", "visa")));
        assertEquals("https://visa.example.com/pay", transport.lastRequest().url());

        engine.execute(spec, new RequestContext("payment.execute",
                Map.of("provider", "mastercard")));
        assertEquals("https://mastercard.example.com/pay", transport.lastRequest().url());
    }

    @Test
    @DisplayName("Should validate OAuth2 at config time without sending any request")
    void validatesOAuth2ConfigTime() {
        final var tokenStore = new InMemoryTokenStore();
        final var store = MapCredentialStore.of(Map.of("gsheets", new DecryptedCredential<>("gsheets",
                "oauth2", JSON.toMap(new OAuth2Credentials("client-1", "s3cr3t",
                        "https://auth.example.com/token", null, null, "sheets.read",
                        OAuth2Grant.CLIENT_CREDENTIALS, Map.of())), null)));
        final var transport = new FakeTransport();
        final var engine = newEngine(transport, store, tokenStore);
        final var spec = oauth2Spec();

        assertThrows(OAuth2NotConfiguredException.class, () -> engine.validate(spec));
        assertEquals(0, transport.callCount());

        tokenStore.save("gsheets", new OAuth2Token("at-1", "rt-1", 3600,
                Instant.now().plusSeconds(3600), null));
        engine.validate(spec);
        assertEquals(0, transport.callCount());
    }

    @Test
    @DisplayName("Should paginate with offset and cap the accumulated output by maxResults")
    void paginatesAndCaps() {
        final var transport = new QueueTransport(page(10, 1), page(10, 11), page(3, 21));
        final var engine = newEngine(transport, MapCredentialStore.of(Map.of()),
                new InMemoryTokenStore());
        final var pagination = new PaginationSpec("offset", 10, "data.items", "limit", "offset",
                true, null);
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(15, List.of(new PostReceive.RootProperty("data.items"))), pagination,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = engine.execute(spec, new RequestContext("user.list", Map.of()));

        assertEquals(15, records.size());
        assertEquals(2, transport.callCount());
        assertEquals(10, transport.requests().get(1).query().get("offset"));
        assertEquals(10, transport.requests().get(1).query().get("limit"));
    }

    @Test
    @DisplayName("Should page through a custom link-header strategy registered in the pagination registry")
    void paginatesWithCustomStrategy() {
        final var pagination = PaginationRegistry.withBuiltins().register("linkHeader",
                (spec, evaluator, jsonMapper) -> new PaginationStrategy() {
                    @Override
                    public boolean shouldPaginate(final RequestPlan plan,
                                                  final HttpResult last) {
                        return last.headers().containsKey("X-Next");
                    }

                    @Override
                    public HttpRequest nextRequest(final RequestPlan plan,
                                                   final HttpResult last) {
                        final var next = last.headers().get("X-Next").get(0);
                        return plan.request().toBuilder().query("cursor", next).build();
                    }

                    @Override
                    public List<OutputRecord> collect(final RequestPlan plan,
                                                      final HttpResult last,
                                                      final List<OutputRecord> page) {
                        return page;
                    }
                });
        final var firstPage = HttpResult.of(200,
                JSON.write(Map.of("data", Map.of("items", items(10, 1)))),
                Map.of("X-Next", List.of("cursor-1")));
        final var transport = new QueueTransport(firstPage, page(5, 11));
        final var engine = new DeclarativeHttpEngine(ActionRegistry.withBuiltins(), pagination,
                MapCredentialStore.of(Map.of()), transport, new JexlExpressionEvaluator(),
                JSON, new InMemoryTokenStore());
        final var paginationSpec = new PaginationSpec("linkHeader", 10, "data.items", null, null,
                true, null);
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data.items"))), paginationSpec,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = engine.execute(spec, new RequestContext("user.list", Map.of()));

        assertEquals(15, records.size());
        assertEquals(2, transport.callCount());
        assertEquals("cursor-1", transport.requests().get(1).query().get("cursor"));
    }

    @Test
    @DisplayName("Should feed the onResponse callback with each page's HttpResult")
    void feedsOnResponsePerPage() {
        final var transport = new QueueTransport(page(10, 1), page(3, 11));
        final var engine = newEngine(transport, MapCredentialStore.of(Map.of()),
                new InMemoryTokenStore());
        final var pagination = new PaginationSpec("offset", 10, "data.items", "limit", "offset",
                true, null);
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data.items"))), pagination,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());
        final var seen = new ArrayList<HttpResult>();

        final var records = engine.execute(spec,
                new RequestContext("user.list", Map.of(), seen::add));

        assertEquals(2, seen.size());
        assertEquals(200, seen.get(0).status());
        assertEquals(200, seen.get(1).status());
        assertEquals(2, transport.callCount());
        assertEquals(13, records.size());
    }

    @Test
    @DisplayName("Should not invoke onResponse whens it is null")
    void skipsOnResponseWhenNull() {
        final var transport = new QueueTransport(page(2, 1));
        final var engine = newEngine(transport, MapCredentialStore.of(Map.of()),
                new InMemoryTokenStore());
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data.items"))), null,
                List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var records = engine.execute(spec, new RequestContext("user.list", Map.of()));

        assertEquals(2, records.size());
        assertEquals(1, transport.callCount());
    }

    @Test
    @DisplayName("Should return shaped options with presentation metadata via a registered option action")
    @SuppressWarnings("unchecked")
    void describeReturnsShapedOptions() {
        final var registry = ActionRegistry.withBuiltins().register("loadContacts",
                (descriptor, evaluator) -> (records, response) -> {
                    final var data = JSON.read(response.bodyString(), Map.class);
                    final var items = (List<Map<String, Object>>) data.get("data");
                    return items.stream()
                            .map(item -> OutputRecord.ofJson(Map.of(
                                    "name", item.get("name"),
                                    "value", item.get("id"),
                                    "description", item.get("description"),
                                    "icon", item.get("icon"),
                                    "group", item.get("group"),
                                    "disabled", item.get("disabled"))))
                            .toList();
                });
        final var transport = new FakeTransport(HttpResult.of(200, JSON.write(Map.of("data",
                List.of(Map.of("id", 1, "name", "SOK", "description", "Project manager",
                                "icon", "lucide:user", "group", "Team", "disabled", false),
                        Map.of("id", 2, "name", "VISAL", "description", "Developer",
                                "icon", "lucide:code", "group", "Team", "disabled", true))))));
        final var engine = new DeclarativeHttpEngine(registry,
                MapCredentialStore.of(Map.of()), transport, new JexlExpressionEvaluator());
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(), null, null);
        final var operation = new Operation("contact.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.CustomPostReceive("loadContacts", Map.of()))),
                null, List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var page = engine.describe(spec, new RequestContext("contact.list", Map.of()),
                "loadContacts");

        assertEquals(List.of(
                new OptionItem("SOK", "1", "Project manager", "lucide:user", "Team", false),
                new OptionItem("VISAL", "2", "Developer", "lucide:code", "Team", true)), page.items());
        assertFalse(page.hasMore());
        assertNull(page.nextCursor());
        assertEquals(Map.of(), page.nextParameters());
    }

    @Test
    @DisplayName("Should surface hasMore and nextCursor stamped by the option-shaping action")
    @SuppressWarnings("unchecked")
    void describeReturnsPagingState() {
        final var registry = ActionRegistry.withBuiltins().register("loadUsers",
                (descriptor, evaluator) -> (records, response) -> {
                    final var data = JSON.read(response.bodyString(), Map.class);
                    final var items = (List<Map<String, Object>>) data.get("users");
                    final var hasMore = Boolean.TRUE.equals(data.get("hasMore"));
                    final var nextCursor = (String) data.get("nextCursor");
                    return items.stream()
                            .map(item -> OutputRecord.ofJson(
                                    Map.of("name", item.get("name"), "value", item.get("id")),
                                    Map.of("hasMore", hasMore,
                                            "nextCursor", nextCursor)))
                            .toList();
                });
        final var transport = new FakeTransport(HttpResult.of(200, JSON.write(Map.of(
                "users", List.of(Map.of("id", 1, "name", "ALICE"), Map.of("id", 2, "name", "BOB")),
                "hasMore", true,
                "nextCursor", "cursor-42"))));
        final var engine = new DeclarativeHttpEngine(registry,
                MapCredentialStore.of(Map.of()), transport, new JexlExpressionEvaluator());
        final var shape = new RequestShape(HttpMethod.GET, "/users?cursor={{ $parameter.cursor ?: '' }}",
                Map.of(), Map.of(), null, null);
        final var operation = new Operation("user.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.CustomPostReceive("loadUsers", Map.of()))),
                null, List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var page = engine.describe(spec, new RequestContext("user.list", Map.of()),
                "loadUsers");

        assertEquals(List.of(
                new OptionItem("ALICE", "1", null, null, null, false),
                new OptionItem("BOB", "2", null, null, null, false)), page.items());
        assertTrue(page.hasMore());
        assertEquals("cursor-42", page.nextCursor());
        assertEquals(Map.of(), page.nextParameters());
    }

    @Test
    @DisplayName("Should surface structured nextParameters stamped by the option-shaping action")
    @SuppressWarnings("unchecked")
    void describeReturnsStructuredNextParameters() {
        final var registry = ActionRegistry.withBuiltins().register("loadRegions",
                (descriptor, evaluator) -> (records, response) -> {
                    final var data = JSON.read(response.bodyString(), Map.class);
                    final var items = (List<Map<String, Object>>) data.get("regions");
                    final var hasMore = Boolean.TRUE.equals(data.get("hasMore"));
                    final var next = Map.of("offset", 30, "limit", 30, "since", "2024-01-01");
                    return items.stream()
                            .map(item -> OutputRecord.ofJson(
                                    Map.of("name", item.get("name"), "value", item.get("code")),
                                    Map.of("hasMore", hasMore, "nextParameters", next)))
                            .toList();
                });
        final var transport = new FakeTransport(HttpResult.of(200, JSON.write(Map.of(
                "regions", List.of(Map.of("code", "BTB", "name", "Battambang")),
                "hasMore", true))));
        final var engine = new DeclarativeHttpEngine(registry,
                MapCredentialStore.of(Map.of()), transport, new JexlExpressionEvaluator());
        final var shape = new RequestShape(HttpMethod.GET,
                "/api/v1/options/regions?offset={{ $parameter.offset ?: 0 }}"
                        + "&limit={{ $parameter.limit ?: 30 }}&since={{ $parameter.since ?: '' }}",
                Map.of(), Map.of(), null, null);
        final var operation = new Operation("regions.get", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.CustomPostReceive("loadRegions", Map.of()))),
                null, List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var page = engine.describe(spec, new RequestContext("regions.get", Map.of()),
                "loadRegions");

        assertTrue(page.hasMore());
        assertEquals(Map.of("offset", 30, "limit", 30, "since", "2024-01-01"), page.nextParameters());
        assertNull(page.nextCursor());
        final var next = new RequestContext("regions.get", page.nextParameters());
        assertEquals(30, next.parameters().get("offset"));
        assertEquals("2024-01-01", next.parameters().get("since"));
    }

    @Test
    @DisplayName("Should wrap a 404 from the transport into an HttpApiException with status and operationId")
    void wrapsTransportErrors() {
        final var engine = newEngine(new FailingTransport(), MapCredentialStore.of(Map.of()),
                new InMemoryTokenStore());
        final var shape = new RequestShape(HttpMethod.GET, "/contacts/42", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.get",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var e = assertThrows(HttpApiException.class,
                () -> engine.execute(spec, new RequestContext("contact.get", Map.of())));

        assertEquals(404, e.getStatus());
        assertEquals("contact.get", e.getOperationId());
    }

    @Test
    @DisplayName("Should mask sensitiveOutputFields in the returned records")
    void redactsSensitiveOutputFields() {
        final var transport = new FakeTransport(HttpResult.of(200, JSON.write(Map.of(
                "data", Map.of("token", "s3cr3t", "name", "SOK")))));
        final var engine = newEngine(transport, MapCredentialStore.of(Map.of()),
                new InMemoryTokenStore());
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data"))), null, List.of()));
        final var policy = new SecurityPolicy(List.of(), false, true, List.of("token"));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, policy);

        final var records = engine.execute(spec, new RequestContext("contact.list", Map.of()));

        assertEquals(1, records.size());
        assertEquals("***", records.get(0).json().get("token"));
        assertEquals("SOK", records.get(0).json().get("name"));
    }

    private static DeclarativeHttpEngine newEngine(final HttpTransport transport,
                                                   final CredentialStore store,
                                                   final TokenStore tokenStore) {
        return new DeclarativeHttpEngine(ActionRegistry.withBuiltins(), store, transport,
                new JexlExpressionEvaluator(), JSON, tokenStore);
    }

    private static HttpRequestSpec brevoSpec() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts", Map.of(), Map.of(),
                null, null);
        final var send = new Send("contact", Target.BODY, "attributes", true, null);
        final var operation = new Operation("contact.create", new Route(shape, List.of(send),
                new Output(50, List.of(new PostReceive.RootProperty("data"))), null, List.of()));
        return new HttpRequestSpec("https://api.brevo.com/v3", Map.of("Accept", "application/json"),
                30000L, false, List.of(operation), CredentialRef.of("api-key", "brevo"),
                null, SecurityPolicy.defaults());
    }

    private static HttpRequestSpec oauth2Spec() {
        final var shape = new RequestShape(HttpMethod.GET, "/values/A1:B2", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("sheets.values.get",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        return new HttpRequestSpec("https://sheets.googleapis.com/v4", Map.of(), 30000L, false,
                List.of(operation), CredentialRef.of("oauth2", "gsheets"), null,
                SecurityPolicy.defaults());
    }

    private static HttpResult page(final int count,
                                   final int startId) {
        return HttpResult.of(200, JSON.write(Map.of("data", Map.of("items", items(count, startId)))));
    }

    private static List<Map<String, Object>> items(final int count,
                                                   final int startId) {
        final var items = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < count; i++) {
            items.add(Map.of("id", startId + i, "name", "SOK"));
        }
        return items;
    }

    private static final class MapCredentialStore implements CredentialStore {

        private final Map<String, DecryptedCredential<?>> credentials;

        private MapCredentialStore(final Map<String, DecryptedCredential<?>> credentials) {
            this.credentials = credentials;
        }

        private static MapCredentialStore of(final Map<String, DecryptedCredential<?>> credentials) {
            return new MapCredentialStore(credentials);
        }

        @Override
        public Optional<DecryptedCredential<?>> resolve(final CredentialRef ref,
                                                        final RequestContext ctx) {
            return Optional.ofNullable(credentials.get(ref.id()));
        }
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

    private static final class FailingTransport implements HttpTransport {

        @Override
        public HttpResult send(final HttpRequest request) throws HttpApiException {
            throw new HttpApiException(404, "contact.get", 0,
                    "HTTP 404 (non-2xx) for operation 'contact.get'");
        }
    }
}
