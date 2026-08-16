package io.github.khezyapp.dhttp.spec;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpRequestSpecTest {

    @Test
    @DisplayName("Should build a full Brevo-style spec from the public types")
    void buildsBrevoSpec() {
        final var contactCreate = new Operation("contact.create",
                new Route(
                        new RequestShape(HttpMethod.POST, "/v3/contacts", Map.of(),
                                Map.of(), null, null),
                        List.of(new Send("email", Target.BODY, "email", false, null),
                                new Send("attributes", Target.BODY, "attributes", true, null)),
                        new Output(100,
                                List.of(new PostReceive.RootProperty("data.items"))),
                        null, List.of()));

        final var contactList = new Operation("contact.list",
                new Route(
                        new RequestShape(HttpMethod.GET, "/v3/contacts", Map.of(),
                                Map.of(), null, null),
                        List.of(),
                        new Output(50, List.of(new PostReceive.RootProperty("contacts"))),
                        null, List.of()));

        final var spec = new HttpRequestSpec("https://api.brevo.com/v3",
                List.of(contactCreate, contactList),
                SecurityPolicy.defaults());

        assertEquals("https://api.brevo.com/v3", spec.baseUrl());
        assertEquals(2, spec.operations().size());
        assertEquals(2, spec.operations().get(0).route().sends().size());
        final var send = spec.operations().get(0).route().sends().get(1);
        assertEquals(Target.BODY, send.target());
        assertTrue(send.dotNotation());
        assertEquals("attributes", send.property());
        assertEquals("data.items",
                ((PostReceive.RootProperty) spec.operations().get(0).route().output().postReceive().get(0)).property());
    }

    @Test
    @DisplayName("Should expose a dot-notation BODY send for attributes")
    void exposesDotNotationBodySend() {
        final var route = new Route(
                new RequestShape(HttpMethod.POST, "/v3/contacts", Map.of(), Map.of(), null, null),
                List.of(new Send("attributes", Target.BODY, "attributes", true, null)),
                Output.of(0), null, List.of());

        final var send = route.sends().get(0);
        assertEquals("attributes", send.fromParam());
        assertEquals(Target.BODY, send.target());
        assertTrue(send.dotNotation());
        assertEquals("attributes", send.property());
    }

    @Test
    @DisplayName("Should enumerate every PostReceive variant in a sealed switch")
    void switchIsExhaustive() {
        final var postReceives = List.<PostReceive>of(
                new PostReceive.RootProperty("data.items"),
                new PostReceive.FilterItems(new Expression("=item.active")),
                new PostReceive.LimitItems(10),
                new PostReceive.SetValue(new Expression("=summary")),
                new PostReceive.SortByKey("createdAt", true),
                new PostReceive.SetKeyValue(Map.of("total", new Expression("=items.length"))),
                new PostReceive.BinaryData("file"),
                new PostReceive.CustomPostReceive("custom.shaper", Map.of("step", 2)));

        for (final PostReceive pr : postReceives) {
            assertDoesNotThrow(() -> describe(pr));
        }
        assertEquals(8, postReceives.size());
    }

    @Test
    @DisplayName("Should detect expression strings")
    void detectsExpressions() {
        assertTrue(new Expression("=1 + 1").isExpression());
        assertTrue(new Expression("Hello {{name}}").isExpression());
        assertFalse(new Expression("plain").isExpression());
        assertEquals("1 + 1", new Expression("=1 + 1").literal());
        assertEquals("plain", new Expression("plain").literal());
    }

    @Test
    @DisplayName("Should defensively copy collection fields")
    void defensivelyCopiesCollections() {
        final var headers = new java.util.HashMap<String, String>();
        headers.put("X-Test", "1");
        final var spec = new HttpRequestSpec("https://api.example.com", headers, 1000L, false,
                List.of(), null, null, SecurityPolicy.defaults());
        headers.put("X-Test", "2");

        assertEquals("1", spec.defaultHeaders().get("X-Test"));
        assertThrows(UnsupportedOperationException.class,
                () -> spec.defaultHeaders().put("X-Other", "3"));
    }

    @Test
    @DisplayName("Should validate required and bounded fields")
    void validatesFields() {
        assertThrows(NullPointerException.class, () -> new HttpRequestSpec(null, List.of(),
                SecurityPolicy.defaults()));
        assertThrows(NullPointerException.class, () -> new CredentialRef(null, "id"));
        assertThrows(IllegalArgumentException.class, () -> new Output(-1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PaginationSpec("offset", 0, "items",
                "limit", "offset", true, null));
    }

    private String describe(final PostReceive pr) {
        final String kind;
        if (pr instanceof PostReceive.RootProperty r) {
            kind = "root:" + r.property();
        } else if (pr instanceof PostReceive.FilterItems f) {
            kind = "filter:" + f.pass();
        } else if (pr instanceof PostReceive.LimitItems l) {
            kind = "limit:" + l.max();
        } else if (pr instanceof PostReceive.SetValue s) {
            kind = "set:" + s.value();
        } else if (pr instanceof PostReceive.SortByKey s) {
            kind = "sort:" + s.key();
        } else if (pr instanceof PostReceive.SetKeyValue s) {
            kind = "setkv:" + s.fields().size();
        } else if (pr instanceof PostReceive.BinaryData b) {
            kind = "binary:" + b.destinationProperty();
        } else if (pr instanceof PostReceive.CustomPostReceive c) {
            kind = "custom:" + c.actionKey();
        } else {
            kind = "unknown";
        }
        return kind;
    }
}
