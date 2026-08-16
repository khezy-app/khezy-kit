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
import java.util.Map;
import java.util.Objects;

/**
 * Replaces each record's payload with the evaluated value: a {@link Map} becomes the record's json,
 * a scalar is wrapped under {@code value}. Covers {@link PostReceive.SetValue}.
 */
public final class SetValue implements PostReceiveAction {

    private final Expression value;
    private final ExpressionEvaluator evaluator;
    private final JsonMapper jsonMapper;

    public SetValue(final Expression value,
                    final ExpressionEvaluator evaluator) {
        this(value, evaluator, JacksonJsonMapper.INSTANCE);
    }

    public SetValue(final Expression value,
                    final ExpressionEvaluator evaluator,
                    final JsonMapper jsonMapper) {
        this.value = Objects.requireNonNull(value, "value");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    public static SetValue from(final PostReceive descriptor,
                                final ExpressionEvaluator evaluator) {
        if (descriptor instanceof PostReceive.SetValue setValue) {
            return new SetValue(setValue.value(), evaluator);
        }
        return new SetValue(new Expression(String.valueOf(BuiltinSupport.prop(descriptor, "value"))),
                evaluator);
    }

    @Override
    public List<OutputRecord> apply(final List<OutputRecord> records,
                                    final HttpResult response) {
        final var body = BuiltinSupport.parseBody(response, jsonMapper);
        final var result = new ArrayList<OutputRecord>();
        for (int index = 0; index < records.size(); index++) {
            final var record = records.get(index);
            final var scope = BuiltinSupport.scope(record, index, body);
            result.add(replace(record, BuiltinSupport.evaluate(evaluator, value, scope)));
        }
        return List.copyOf(result);
    }

    private static OutputRecord replace(final OutputRecord record,
                                        final Object evaluated) {
        if (evaluated instanceof Map<?, ?>) {
            return new OutputRecord(map(evaluated), record.binary(), record.metadata(), record.isBinary());
        }
        if (Objects.isNull(evaluated)) {
            return new OutputRecord(Map.of(), record.binary(), record.metadata(), record.isBinary());
        }
        return new OutputRecord(Map.of("value", evaluated), record.binary(), record.metadata(), record.isBinary());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(final Object evaluated) {
        return (Map<String, Object>) evaluated;
    }
}
