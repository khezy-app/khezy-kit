package io.github.khezyapp.dhttp.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.spec.Condition;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConditionEvaluatorTest {

    @Test
    @DisplayName("Should select the operation whose id and precondition match")
    void selectsMatchingOperation() {
        final var matching = new Operation("contact.create",
                List.of(new Condition("state", "open")), route());
        final var other = new Operation("contact.list", route());
        final var spec = spec(matching, other);

        final var selected = ConditionEvaluator.selectOperation(spec,
                new RequestContext("contact.create", Map.of("state", "open")));

        assertSame(matching, selected);
    }

    @Test
    @DisplayName("Should select by operation id alone whens there is no whens")
    void idSelectsAmongUnconditionedOperations() {
        final var list = new Operation("contact.list", route());
        final var get = new Operation("contact.get", route());
        final var spec = spec(list, get);

        final var selected = ConditionEvaluator.selectOperation(spec,
                new RequestContext("contact.get", Map.of()));

        assertSame(get, selected);
    }

    @Test
    @DisplayName("Should route same-id variants by their conditions")
    void routesSameIdVariantByCondition() {
        final var byId = new Operation("contact.get",
                List.of(new Condition("id", null, "true")), route());
        final var byEmail = new Operation("contact.get",
                List.of(new Condition("email", null, "true")), route());
        final var spec = spec(byId, byEmail);

        assertSame(byId, ConditionEvaluator.selectOperation(spec,
                new RequestContext("contact.get", Map.of("id", 6))));
        assertSame(byEmail, ConditionEvaluator.selectOperation(spec,
                new RequestContext("contact.get", Map.of("email", "sok@example.com"))));
    }

    @Test
    @DisplayName("Should pick the first same-id variant whose conditions pass")
    void firstMatchingVariantWins() {
        final var both = new Operation("contact.get",
                List.of(new Condition("id", null, "true")), route());
        final var fallback = new Operation("contact.get", route());
        final var spec = spec(both, fallback);

        assertSame(both, ConditionEvaluator.selectOperation(spec,
                new RequestContext("contact.get", Map.of("id", 6))));
        assertSame(fallback, ConditionEvaluator.selectOperation(spec,
                new RequestContext("contact.get", Map.of("email", "sok@example.com"))));
    }

    @Test
    @DisplayName("Should not select an operation whose conditions match but id differs")
    void differentIdIsIgnored() {
        final var open = new Operation("open", List.of(new Condition("state", "open")), route());
        final var closed = new Operation("closed", List.of(new Condition("state", "closed")), route());
        final var spec = spec(open, closed);

        final var selected = ConditionEvaluator.selectOperation(spec,
                new RequestContext("closed", Map.of("state", "open")));

        assertNull(selected);
    }

    @Test
    @DisplayName("Should select a single operation whens there is no whens")
    void noWhenSelectsSingle() {
        final var single = new Operation("contact.list", route());

        final var selected = ConditionEvaluator.selectOperation(spec(single),
                new RequestContext("contact.list", Map.of()));

        assertSame(single, selected);
    }

    @Test
    @DisplayName("Should return null whens no same-id operation matches")
    void noneMatchReturnsNull() {
        final var open = new Operation("open", List.of(new Condition("state", "open")), route());

        final var selected = ConditionEvaluator.selectOperation(spec(open),
                new RequestContext("open", Map.of("state", "closed")));

        assertNull(selected);
    }

    @Test
    @DisplayName("Should return null whens the operation id does not exist")
    void unknownIdReturnsNull() {
        final var single = new Operation("contact.list", route());

        final var selected = ConditionEvaluator.selectOperation(spec(single),
                new RequestContext("contact.get", Map.of()));

        assertNull(selected);
    }

    @Test
    @DisplayName("Should require every condition to pass")
    void allConditionsMustPass() {
        final var when = List.of(
                new Condition("state", "open"),
                new Condition("plan", "premium"));

        assertTrue(ConditionEvaluator.evaluate(when,
                new RequestContext("x", Map.of("state", "open", "plan", "premium"))));
        assertFalse(ConditionEvaluator.evaluate(when,
                new RequestContext("x", Map.of("state", "open", "plan", "free"))));
    }

    @Test
    @DisplayName("Should support exists checks")
    void existsChecks() {
        assertTrue(ConditionEvaluator.evaluate(List.of(new Condition("email", null, "true")),
                new RequestContext("x", Map.of("email", "sok@example.com"))));
        assertFalse(ConditionEvaluator.evaluate(List.of(new Condition("email", null, "true")),
                new RequestContext("x", Map.of())));
        assertTrue(ConditionEvaluator.evaluate(List.of(new Condition("email", null, "false")),
                new RequestContext("x", Map.of())));
    }

    @Test
    @DisplayName("Should match a nested parameter through dot notation")
    void nestedCondition() {
        final var when = List.of(new Condition("contact.state", "open"));

        assertTrue(ConditionEvaluator.evaluate(when,
                new RequestContext("x", Map.of("contact", Map.of("state", "open")))));
    }

    private static HttpRequestSpec spec(final Operation... operations) {
        return new HttpRequestSpec("https://api.brevo.com/v3", Map.of(),
                30000L, false, List.of(operations), null, null, SecurityPolicy.defaults());
    }

    private static Route route() {
        return new Route(
                new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(), null, null),
                Output.of(10));
    }
}
