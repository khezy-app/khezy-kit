package io.github.khezyapp.dhttp.action.builtin;

import io.github.khezyapp.dhttp.action.PostReceiveAction;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Keeps the records whose {@link Expression} evaluates truthy, evaluated per record against
 * {@code $item}, {@code $value}, {@code $index}, and {@code $response}. Covers
 * {@link PostReceive.FilterItems}.
 */
public final class FilterItems implements PostReceiveAction {

    private final Expression pass;
    private final ExpressionEvaluator evaluator;
    private final JsonMapper jsonMapper;

    public FilterItems(final Expression pass,
                       final ExpressionEvaluator evaluator) {
        this(pass, evaluator, JacksonJsonMapper.INSTANCE);
    }

    public FilterItems(final Expression pass,
                       final ExpressionEvaluator evaluator,
                       final JsonMapper jsonMapper) {
        this.pass = Objects.requireNonNull(pass, "pass");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    public static FilterItems from(final PostReceive descriptor,
                                   final ExpressionEvaluator evaluator) {
        if (descriptor instanceof PostReceive.FilterItems filterItems) {
            return new FilterItems(filterItems.pass(), evaluator);
        }
        return new FilterItems(new Expression(String.valueOf(BuiltinSupport.prop(descriptor, "pass"))), evaluator);
    }

    @Override
    public List<OutputRecord> apply(final List<OutputRecord> records,
                                    final HttpResult response) {
        final var body = BuiltinSupport.parseBody(response, jsonMapper);
        final var result = new ArrayList<OutputRecord>();
        for (int index = 0; index < records.size(); index++) {
            final var record = records.get(index);
            final var scope = BuiltinSupport.scope(record, index, body);
            if (BuiltinSupport.truthy(evaluator, pass, scope)) {
                result.add(record);
            }
        }
        return List.copyOf(result);
    }
}
