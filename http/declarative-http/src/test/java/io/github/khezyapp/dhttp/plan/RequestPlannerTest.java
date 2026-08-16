package io.github.khezyapp.dhttp.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.action.ActionRegistry;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.error.NonStringKeyExpressionException;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.pagination.NextUrlPagination;
import io.github.khezyapp.dhttp.pagination.PagePagination;
import io.github.khezyapp.dhttp.pagination.PaginationRegistry;
import io.github.khezyapp.dhttp.pagination.PaginationStrategy;
import io.github.khezyapp.dhttp.spec.CredentialRef;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.spec.Send;
import io.github.khezyapp.dhttp.spec.Target;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestPlannerTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    private final RequestPlanner planner = new RequestPlanner();

    @Test
    @DisplayName("Should plan a Brevo-style request with correct method/path/headers/body")
    void plansBrevoStyleRequest() {
        final var spec = brevoSpec();

        final var plan = planner.plan(spec, spec.operations().get(0),
                new RequestContext("contact.create",
                        Map.of("contact", Map.of("name", "SOK", "email", "sok@example.com",
                                "attributes", Map.of("city", "Battambang")))));

        assertEquals(HttpMethod.POST, plan.request().method());
        assertEquals("https://api.brevo.com/v3/contacts", plan.request().url());
        assertEquals("application/json",
                plan.request().headers().first("Accept").orElseThrow());

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), Map.class);
        assertEquals(Map.of("city", "Battambang"), parsed.get("attributes"));
    }

    @Test
    @DisplayName("Should resolve a nested parameter through dot notation")
    void resolvesDotNotationSend() {
        final var spec = brevoSpec();

        final var plan = planner.plan(spec, spec.operations().get(0),
                new RequestContext("contact.create",
                        Map.of("contact", Map.of("attributes",
                                Map.of("city", "Kampot", "country", "KH")))));

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), Map.class);
        assertEquals(Map.of("city", "Kampot", "country", "KH"), parsed.get("attributes"));
    }

    @Test
    @DisplayName("Should resolve an expression value override via the evaluator")
    void resolvesExpressionValueOverride() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(), null, null);
        final var send = new Send("ignored", Target.BODY, "name", false,
                new Expression("= {{ $parameter.contact.name }}"));
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(send), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.create",
                        Map.of("contact", Map.of("name", "VISAL"))));

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), Map.class);
        assertEquals("VISAL", parsed.get("name"));
    }

    @Test
    @DisplayName("Should place a plain parameter into the query")
    void placesQuerySend() {
        final var shape = new RequestShape(HttpMethod.GET, "/contacts",
                Map.of(), Map.of(), null, null);
        final var send = new Send("limit", Target.QUERY, "limit", false, null);
        final var operation = new Operation("contact.list",
                new Route(shape, List.of(send), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.list", Map.of("limit", 10)));

        assertEquals(10, plan.request().query().get("limit"));
        assertEquals("https://api.brevo.com/v3/contacts", plan.request().url());
    }

    @Test
    @DisplayName("Should skip a send whose parameter is missing")
    void skipsMissingParameter() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(), null, null);
        final var send = new Send("missing", Target.BODY, "name", false, null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(send), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.create", Map.of()));

        assertEquals(Body.BodyKind.NONE, plan.request().body().kind());
    }

    @Test
    @DisplayName("Should resolve a templated path through the evaluator")
    void resolvesTemplatedPath() {
        final var shape = new RequestShape(HttpMethod.GET, "/contacts/{{ $parameter.id }}",
                Map.of(), Map.of(), null, null);
        final var operation = new Operation("contact.get",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.get", Map.of("id", "42")));

        assertEquals("https://api.brevo.com/v3/contacts/42", plan.request().url());
    }

    @Test
    @DisplayName("Should attach the default credential as the auth request")
    void attachesDefaultCredential() {
        final var spec = brevoSpec();

        final var plan = planner.plan(spec, spec.operations().get(0),
                new RequestContext("contact.create", Map.of()));

        assertEquals(new CredentialRef("api-key", "brevo"), plan.authRequest().ref());
        assertEquals("api-key", plan.authRequest().type());
    }

    @Test
    @DisplayName("Should leave auth request null whens no credential is configured")
    void noCredentialMeansNoAuth() {
        final var spec = spec(brevoSpec().operations().get(0), null);

        final var plan = planner.plan(spec, spec.operations().get(0),
                new RequestContext("contact.create", Map.of()));

        assertNull(plan.authRequest());
    }

    @Test
    @DisplayName("Should produce identical plans for identical input")
    void deterministicPlanning() {
        final var spec = brevoSpec();
        final var ctx = new RequestContext("contact.create",
                Map.of("contact", Map.of("name", "SOK",
                        "attributes", Map.of("city", "Battambang"))));
        final var operation = spec.operations().get(0);

        final var first = planner.plan(spec, operation, ctx);
        final var second = planner.plan(spec, operation, ctx);

        assertEquals(first.request().url(), second.request().url());
        assertEquals(first.request().method(), second.request().method());
        assertEquals(first.request().headers().asMap(), second.request().headers().asMap());
        assertEquals(first.request().body().kind(), second.request().body().kind());
        assertEquals(((Body.JsonBody) first.request().body()).json(),
                ((Body.JsonBody) second.request().body()).json());
        assertEquals(first.maxResults(), second.maxResults());
        assertEquals(first.authRequest(), second.authRequest());
        assertEquals(first.postReceives(), second.postReceives());
    }

    @Test
    @DisplayName("Should serialize a Map literal body whens no body send is configured")
    void usesMapLiteralBodyWhenNoBodySends() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(),
                Map.of("id", 6, "name", "Yong An", "attributes", Map.of()), null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.create", Map.of()));

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), Map.class);
        assertEquals(6, parsed.get("id"));
        assertEquals("Yong An", parsed.get("name"));
        assertEquals(Map.of(), parsed.get("attributes"));
    }

    @Test
    @DisplayName("Should serialize a List literal body as an array root")
    void sendsArrayRootBody() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts/batch",
                Map.of(), Map.of(),
                List.of(Map.of("name", "SOK"), Map.of("name", "VISAL")), null);
        final var operation = new Operation("contact.createBatch",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.createBatch", Map.of()));

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), List.class);
        assertEquals(2, parsed.size());
        assertEquals("SOK", ((Map<?, ?>) parsed.get(0)).get("name"));
        assertEquals("VISAL", ((Map<?, ?>) parsed.get(1)).get("name"));
    }

    @Test
    @DisplayName("Should pass a raw JSON string through verbatim")
    void usesRawJsonStringVerbatim() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(), "{\"id\":6,\"name\":\"Yong An\",\"attributes\":{}}", null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.create", Map.of()));

        assertEquals("{\"id\":6,\"name\":\"Yong An\",\"attributes\":{}}",
                ((Body.JsonBody) plan.request().body()).json());
    }

    @Test
    @DisplayName("Should merge body sends on top of the literal body")
    void mergesBodySendsIntoLiteralJson() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(),
                Map.of("id", 6, "name", "default", "attributes", Map.of()), null);
        final var send = new Send("name", Target.BODY, "name", false, null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(send), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.create", Map.of("name", "Yong An")));

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), Map.class);
        assertEquals(6, parsed.get("id"));
        assertEquals("Yong An", parsed.get("name"));
        assertEquals(Map.of(), parsed.get("attributes"));
    }

    @Test
    @DisplayName("Should keep the literal body whens a body send parameter is missing")
    void keepsLiteralBodyWhenSendParameterMissing() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(), Map.of("id", 6, "name", "Yong An"), null);
        final var send = new Send("missing", Target.BODY, "name", false, null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(send), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.create", Map.of()));

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), Map.class);
        assertEquals(6, parsed.get("id"));
        assertEquals("Yong An", parsed.get("name"));
    }

    @Test
    @DisplayName("Should reject a non-object literal body combined with body sends")
    void rejectsNonObjectLiteralJsonWithBodySends() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(), List.of(1, 2, 3), null);
        final var send = new Send("name", Target.BODY, "name", false, null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(send), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        assertThrows(IllegalArgumentException.class,
                () -> planner.plan(spec, operation,
                        new RequestContext("contact.create", Map.of("name", "Yong An"))));
    }

    @Test
    @DisplayName("Should use an absolute path as the full URL")
    void usesAbsolutePathAsFullUrl() {
        final var shape = new RequestShape(HttpMethod.POST, "https://visa.example.com/pay",
                Map.of(), Map.of(), null, null);
        final var operation = new Operation("payment.execute",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("payment.execute", Map.of()));

        assertEquals("https://visa.example.com/pay", plan.request().url());
    }

    @Test
    @DisplayName("Should prefer the route base URL over the spec base URL")
    void usesRouteBaseUrl() {
        final var shape = new RequestShape(HttpMethod.POST, "/pay",
                Map.of(), Map.of(), null, null, "https://visa.example.com");
        final var operation = new Operation("payment.execute",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("payment.execute", Map.of()));

        assertEquals("https://visa.example.com/pay", plan.request().url());
    }

    @Test
    @DisplayName("Should resolve expressions in literal body leaves")
    void resolvesExpressionsInLiteralBody() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(),
                Map.of("name", "= {{ $parameter.name }}",
                        "tags", List.of("= {{ $parameter.tag }}")), null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.create",
                        Map.of("name", "Yong An", "tag", "vip")));

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), Map.class);
        assertEquals("Yong An", parsed.get("name"));
        assertEquals(List.of("vip"), parsed.get("tags"));
    }

    @Test
    @DisplayName("Should resolve expressions in literal body keys")
    void resolvesExpressionsInLiteralBodyKeys() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(),
                Map.of("= {{ $parameter.field }}", "Yong An"), null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.create", Map.of("field", "name")));

        final var parsed = JSON.read(((Body.JsonBody) plan.request().body()).json(), Map.class);
        assertEquals(Map.of("name", "Yong An"), parsed);
    }

    @Test
    @DisplayName("Should reject a body key expression that resolves to a non-string")
    void rejectsNonStringBodyKeyExpression() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(),
                Map.of("= {{ $parameter.field }}", "Yong An"), null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        final var e = assertThrows(NonStringKeyExpressionException.class, () -> planner.plan(spec,
                operation, new RequestContext("contact.create", Map.of("field", 42))));

        assertEquals("= {{ $parameter.field }}", e.getKey());
        assertEquals(42, e.getResolved());
    }

    @Test
    @DisplayName("Should reject a header key expression that resolves to a non-string")
    void rejectsNonStringHeaderKeyExpression() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of("= {{ $parameter.headerName }}", "token"), Map.of(), null, null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var spec = spec(operation, null);

        assertThrows(NonStringKeyExpressionException.class, () -> planner.plan(spec, operation,
                new RequestContext("contact.create", Map.of("headerName", true))));
    }

    @Test
    @DisplayName("Should carry the security policy's allowIpLiteral and stripCrossOriginCredentials onto the request")
    void carriesSecurityFlags() {
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list",
                new Route(shape, List.of(), Output.of(50), null, List.of()));
        final var policy = new SecurityPolicy(List.of("api.example.com"), true, false, List.of());
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, policy);

        final var plan = planner.plan(spec, operation,
                new RequestContext("contact.list", Map.of()));

        assertTrue(plan.request().allowIpLiteral());
        assertFalse(plan.request().stripCrossOriginCredentials());
        assertEquals(List.of("api.example.com"), plan.request().allowedDomains());
    }

    private static HttpRequestSpec brevoSpec() {
        final var shape = new RequestShape(HttpMethod.POST, "/contacts",
                Map.of(), Map.of(), null, null);
        final var send = new Send("contact", Target.BODY, "attributes", true, null);
        final var operation = new Operation("contact.create",
                new Route(shape, List.of(send), Output.of(50), null, List.of()));
        return spec(operation, CredentialRef.of("api-key", "brevo"));
    }

    @Test
    @DisplayName("Should build a page-number strategy from a page mode spec")
    void plansPagePagination() {
        final var pagination = new PaginationSpec("page", 10, "data.items", "page_size", "page",
                true, null);
        final var operation = operationWith(pagination);

        final var plan = planner.plan(spec(operation, null), operation,
                new RequestContext("user.list", Map.of()));

        assertInstanceOf(PagePagination.class, plan.pagination());
    }

    @Test
    @DisplayName("Should build a next-URL strategy from a nextUrl mode spec")
    void plansNextUrlPagination() {
        final var pagination = new PaginationSpec("nextUrl", 10, null, null, null, true,
                new Expression("= {{ $response.next }}"));
        final var operation = operationWith(pagination);

        final var plan = planner.plan(spec(operation, null), operation,
                new RequestContext("user.list", Map.of()));

        assertInstanceOf(NextUrlPagination.class, plan.pagination());
    }

    @Test
    @DisplayName("Should resolve a custom pagination mode through the injected registry")
    void plansCustomPaginationMode() {
        final var pagination = new PaginationSpec("marker", 10, "data.items", null, null, true, null);
        final var operation = operationWith(pagination);
        final var registry = PaginationRegistry.withBuiltins().register("marker",
                (spec, evaluator, jsonMapper) -> new MarkerPagination());
        final var plannerWithCustom = new RequestPlanner(
                ActionRegistry.withBuiltins(), registry,
                new JexlExpressionEvaluator(), JacksonJsonMapper.INSTANCE);

        final var plan = plannerWithCustom.plan(spec(operation, null), operation,
                new RequestContext("user.list", Map.of()));

        assertInstanceOf(MarkerPagination.class, plan.pagination());
    }

    @Test
    @DisplayName("Should fail fast for an unregistered custom pagination mode")
    void failsFastOnUnknownPaginationMode() {
        final var pagination = new PaginationSpec("unknownMode", 10, "data.items", null, null,
                true, null);
        final var operation = operationWith(pagination);

        final var e = assertThrows(IllegalArgumentException.class, () -> planner.plan(
                spec(operation, null), operation, new RequestContext("user.list", Map.of())));

        assertEquals("No pagination strategy registered for mode 'unknownMode'", e.getMessage());
    }

    private static Operation operationWith(final PaginationSpec pagination) {
        final var shape = new RequestShape(HttpMethod.GET, "/users", Map.of(), Map.of(), null, null);
        return new Operation("user.list", new Route(shape, List.of(),
                new Output(50, List.of()), pagination, List.of()));
    }

    private static HttpRequestSpec spec(final Operation operation,
                                        final CredentialRef credential) {
        return new HttpRequestSpec("https://api.brevo.com/v3",
                Map.of("Accept", "application/json"),
                30000L, false, List.of(operation), credential, null, SecurityPolicy.defaults());
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
}
