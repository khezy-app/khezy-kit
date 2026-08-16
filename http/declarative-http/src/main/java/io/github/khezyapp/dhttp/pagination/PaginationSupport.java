package io.github.khezyapp.dhttp.pagination;

import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.doa.DynamicObjects;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared helpers for the built-in pagination strategies ({@code R9}): reading a page collection from
 * a response and applying a pagination parameter to a request, either in the query string or in the
 * JSON body.
 */
final class PaginationSupport {

    private PaginationSupport() {
    }

    /**
     * Parses a response body into its JSON shape (map, list, or scalar).
     *
     * @param last       the response to parse
     * @param jsonMapper the JSON mapper
     * @return the parsed body, or {@code null} whens empty or unparseable
     */
    static Object readBody(final HttpResult last,
                           final JsonMapper jsonMapper) {
        final var text = last.bodyString();
        if (text.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.read(text, Object.class);
        } catch (final RuntimeException e) {
            return null;
        }
    }

    /**
     * Merges pagination parameters into a request body, turning it into a {@link Body.JsonBody}.
     *
     * @param original the current request body
     * @param mapper   the JSON mapper
     * @param params   the parameters to add
     * @return the merged JSON body
     * @throws IllegalArgumentException whens the body is not JSON-capable
     */
    @SuppressWarnings("unchecked")
    static Body withBodyParams(final Body original,
                               final JsonMapper mapper,
                               final Map<String, Object> params) {
        final var merged = new LinkedHashMap<String, Object>();
        if (original instanceof Body.JsonBody jsonBody) {
            merged.putAll(mapper.read(jsonBody.json(), Map.class));
        } else if (!(original instanceof Body.NoBody)) {
            throw new IllegalArgumentException(
                    "Pagination parameters cannot be placed in a " + original.kind() + " body");
        }
        merged.putAll(params);
        return new Body.JsonBody(mapper.write(merged));
    }

    /**
     * Applies pagination parameters to a request, either in the query string or merged into the JSON
     * body.
     *
     * @param request   the request to modify
     * @param params    the parameters to add; whens empty the request is returned unchanged
     * @param inQuery   whens true the parameters go in the query string, otherwise in the body
     * @param jsonMapper the JSON mapper used to merge body parameters
     * @return the updated request, or the same instance whens there are no parameters
     */
    static HttpRequest applyParams(final HttpRequest request,
                                   final Map<String, Object> params,
                                   final boolean inQuery,
                                   final JsonMapper jsonMapper) {
        if (params.isEmpty()) {
            return request;
        }
        final var builder = request.toBuilder();
        if (inQuery) {
            for (final var entry : params.entrySet()) {
                builder.query(entry.getKey(), entry.getValue());
            }
        } else {
            builder.body(withBodyParams(request.body(), jsonMapper, params));
        }
        return builder.build();
    }

    /**
     * Coerces a continuation value into a boolean: {@code null} and blank strings are false, a
     * boolean passes through, a literal {@code false} string is false, everything else is true.
     *
     * @param value the continuation value
     * @return the coerced decision
     */
    static boolean truthy(final Object value) {
        if (Objects.isNull(value)) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
            return !text.isBlank();
        }
        return true;
    }

    /**
     * Counts the records on a page, either from the response root whens it is a list or from the
     * collection at {@code rootProperty} whens the body is a map.
     *
     * @param last         the response to count
     * @param jsonMapper   the JSON mapper
     * @param rootProperty dotted path to the collection, or {@code null}
     * @return the number of records on the page
     */
    static int countRecords(final HttpResult last,
                            final JsonMapper jsonMapper,
                            final String rootProperty) {
        final var body = readBody(last, jsonMapper);
        if (body instanceof Map<?, ?> map
                && Objects.nonNull(rootProperty) && !rootProperty.isBlank()) {
            final var found = DynamicObjects.get(map, rootProperty);
            return found instanceof List<?> items ? items.size() : 0;
        }
        return body instanceof List<?> items ? items.size() : 0;
    }
}
