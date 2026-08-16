package io.github.khezyapp.dhttp.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FragmentMergerTest {

    @Test
    @DisplayName("Should merge default headers under operation headers per key")
    void mergesHeadersPerKey() {
        final var spec = spec(Map.of(
                "Accept", "application/json",
                "X-Default", "default"));
        final var route = new Route(
                new RequestShape(HttpMethod.POST, "/contacts",
                        Map.of("Accept", "text/plain", "X-Op", "op"), Map.of(), null, null),
                Output.of(10));

        final var merged = FragmentMerger.mergeDefaults(spec, operation(route), route);

        assertEquals("text/plain", merged.request().headers().get("Accept"));
        assertEquals("default", merged.request().headers().get("X-Default"));
        assertEquals("op", merged.request().headers().get("X-Op"));
    }

    @Test
    @DisplayName("Should deep-merge nested maps recursively")
    void deepMergesNestedMaps() {
        final var base = new LinkedHashMap<String, Object>();
        base.put("profile", new LinkedHashMap<>(Map.of("name", "SOK", "city", "Kampot")));
        final var overrides = new LinkedHashMap<String, Object>();
        overrides.put("profile", new LinkedHashMap<>(Map.of("city", "Siem Reap", "country", "KH")));

        final var merged = FragmentMerger.deepMerge(base, overrides);

        final var profile = new LinkedHashMap<>(Map.of("name", "SOK", "city", "Siem Reap", "country", "KH"));
        assertEquals(profile, merged.get("profile"));
    }

    @Test
    @DisplayName("Should keep route pagination over the spec default")
    void routePaginationWins() {
        final var spec = specWithPagination(new PaginationSpec(
                "offset", 50, null, "limit", "offset", true, null));
        final var routePagination = new PaginationSpec(
                "cursor", 10, "items", "pageSize", "cursor", true, null);
        final var route = new Route(
                new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(), null, null),
                List.of(),
                Output.of(10),
                routePagination,
                List.of());

        final var merged = FragmentMerger.mergeDefaults(spec, operation(route), route);

        assertSame(routePagination, merged.pagination());
    }

    @Test
    @DisplayName("Should fall back to the spec default pagination")
    void fallsBackToSpecPagination() {
        final var defaultPagination = new PaginationSpec(
                "offset", 25, null, "limit", "offset", true, null);
        final var spec = specWithPagination(defaultPagination);
        final var route = new Route(
                new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(), null, null),
                Output.of(10));

        final var merged = FragmentMerger.mergeDefaults(spec, operation(route), route);

        assertSame(defaultPagination, merged.pagination());
    }

    private static Operation operation(final Route route) {
        return new Operation("contact.list", route);
    }

    private static HttpRequestSpec spec(final Map<String, String> defaultHeaders) {
        return new HttpRequestSpec("https://api.brevo.com/v3", defaultHeaders,
                30000L, false, List.of(), null, null, SecurityPolicy.defaults());
    }

    private static HttpRequestSpec specWithPagination(final PaginationSpec pagination) {
        return new HttpRequestSpec("https://api.brevo.com/v3", Map.of(),
                30000L, false, List.of(), null, pagination, SecurityPolicy.defaults());
    }
}
