package io.github.khezyapp.dhttp.action.builtin;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.EvaluationScope;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.doa.DynamicObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared evaluation and response-binding helpers for the built-in post-receive actions.
 */
final class BuiltinSupport {

    private static final String RESULT = "result";

    private BuiltinSupport() {
    }

    /**
     * Parses the response body into any JSON shape and always exposes it under the key
     * {@code result}. The wrapper guarantees a plain map regardless of whether the server returned
     * an object, an array, or a scalar.
     *
     * @param response   the HTTP response
     * @param jsonMapper the JSON mapper
     * @return a map with the parsed body under {@code result}, or an empty map
     */
    static Map<String, Object> parseBody(final HttpResult response,
                                         final JsonMapper jsonMapper) {
        final var body = response.bodyString();
        if (body.isBlank()) {
            return Map.of();
        }
        try {
            final var parsed = jsonMapper.read(body, Object.class);
            return Objects.isNull(parsed) ? Map.of() : Map.of(RESULT, parsed);
        } catch (final RuntimeException e) {
            return Map.of(RESULT, body);
        }
    }

    /**
     * Converts a parsed response body into output records. When the body is an object and a property
     * is given, the dotted property is resolved against it; otherwise the whole body is used, so an
     * array or a scalar server response is still turned into usable records.
     *
     * @param body     the body produced by {@link #parseBody}
     * @param property the dotted property to read, or a blank string for the whole body
     * @return the output records
     */
    static List<OutputRecord> recordsFromBody(final Map<String, Object> body,
                                              final String property) {
        final var root = body.get(RESULT);
        if (root instanceof Map<?, ?> map && !property.isBlank()) {
            return toRecords(DynamicObjects.get(map, property));
        }
        return toRecords(root);
    }

    /**
     * Builds the per-record evaluation scope bound with {@code $item}/{@code $responseItem},
     * {@code $value}, {@code $index}, and {@code $response}.
     *
     * <p>{@code $responseItem} is an alias of {@code $item}: the output record currently being
     * shaped (each post-receive step transforms the record list, so both names expose the same
     * record).</p>
     *
     * @param record the record being shaped, or {@code null}
     * @param index  the record index
     * @param body   the parsed response body
     * @return the bound scope
     */
    static EvaluationScope scope(final OutputRecord record,
                                 final int index,
                                 final Map<String, Object> body) {
        final var json = Objects.isNull(record) ? Map.of() : record.json();
        return EvaluationScope.create()
                .bind(EvaluationScope.ITEM, json)
                .bind(EvaluationScope.RESPONSE_ITEM, json)
                .bind(EvaluationScope.VALUE, json)
                .bind(EvaluationScope.INDEX, index)
                .bind(EvaluationScope.RESPONSE, body);
    }

    /**
     * Evaluates an {@link Expression} against the scope, or returns its literal value whens it is not
     * an expression.
     *
     * @param evaluator  the expression evaluator
     * @param expression the expression or literal
     * @param scope      the bindings
     * @return the resolved value
     */
    static Object evaluate(final ExpressionEvaluator evaluator,
                           final Expression expression,
                           final EvaluationScope scope) {
        if (expression.isExpression()) {
            return evaluator.evaluate(expression.raw(), scope, Object.class);
        }
        return expression.literal();
    }

    /**
     * @return true whens the expression (or literal) is truthy
     */
    static boolean truthy(final ExpressionEvaluator evaluator,
                          final Expression expression,
                          final EvaluationScope scope) {
        if (expression.isExpression()) {
            return Boolean.TRUE.equals(evaluator.evaluate(expression.raw(), scope, Boolean.class));
        }
        return Boolean.parseBoolean(expression.literal());
    }

    /**
     * Extracts a named property from a {@code CustomPostReceive} descriptor's props.
     *
     * @return the prop value
     * @throws IllegalArgumentException whens the descriptor is not a custom action or the prop is absent
     */
    static Object prop(final PostReceive descriptor,
                       final String name) {
        if (descriptor instanceof PostReceive.CustomPostReceive custom) {
            final var value = custom.props().get(name);
            if (Objects.isNull(value)) {
                throw new IllegalArgumentException("Custom post-receive action is missing prop '" + name + "'");
            }
            return value;
        }
        throw new IllegalArgumentException(
                "Expected a CustomPostReceive descriptor, got " + descriptor.getClass().getSimpleName());
    }

    /**
     * Converts an evaluated value into output records: a {@link List} becomes one record per item
     * (non-object items wrapped under {@code value}), a {@link Map} becomes a single record, anything
     * else is wrapped under {@code value}.
     *
     * @param found the evaluated value
     * @return the output records
     */
    static List<OutputRecord> toRecords(final Object found) {
        if (Objects.isNull(found)) {
            return List.of();
        }
        if (found instanceof List<?> items) {
            final var result = new ArrayList<OutputRecord>();
            for (final var item : items) {
                if (Objects.isNull(item)) {
                    continue;
                }
                if (item instanceof Map<?, ?>) {
                    result.add(OutputRecord.ofJson(asMap(item)));
                } else {
                    result.add(OutputRecord.ofJson(Map.of("value", item)));
                }
            }
            return List.copyOf(result);
        }
        if (found instanceof Map<?, ?>) {
            return List.of(OutputRecord.ofJson(asMap(found)));
        }
        return List.of(OutputRecord.ofJson(Map.of("value", found)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(final Object value) {
        return (Map<String, Object>) value;
    }
}
