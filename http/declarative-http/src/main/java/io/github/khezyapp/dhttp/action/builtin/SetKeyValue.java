package io.github.khezyapp.dhttp.action.builtin;

import io.github.khezyapp.dhttp.action.PostReceiveAction;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.doa.DynamicObjects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sets one or more dotted fields on every record via {@link io.github.khezyapp.doa.DynamicObjects}, evaluating
 * each value expression per record. Covers {@link PostReceive.SetKeyValue}.
 */
public final class SetKeyValue implements PostReceiveAction {

    private final Map<String, Expression> fields;
    private final ExpressionEvaluator evaluator;
    private final JsonMapper jsonMapper;

    public SetKeyValue(final Map<String, Expression> fields,
                       final ExpressionEvaluator evaluator) {
        this(fields, evaluator, JacksonJsonMapper.INSTANCE);
    }

    public SetKeyValue(final Map<String, Expression> fields,
                       final ExpressionEvaluator evaluator,
                       final JsonMapper jsonMapper) {
        this.fields = Map.copyOf(Objects.requireNonNullElseGet(fields, Map::of));
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    public static SetKeyValue from(final PostReceive descriptor,
                                   final ExpressionEvaluator evaluator) {
        if (descriptor instanceof PostReceive.SetKeyValue setKeyValue) {
            return new SetKeyValue(setKeyValue.fields(), evaluator);
        }
        final var raw = BuiltinSupport.prop(descriptor, "fields");
        final var fields = new LinkedHashMap<String, Expression>();
        if (raw instanceof Map<?, ?> map) {
            for (final var entry : map.entrySet()) {
                fields.put(String.valueOf(entry.getKey()), new Expression(String.valueOf(entry.getValue())));
            }
        }
        return new SetKeyValue(fields, evaluator);
    }

    @Override
    public List<OutputRecord> apply(final List<OutputRecord> records,
                                    final HttpResult response) {
        final var body = BuiltinSupport.parseBody(response, jsonMapper);
        final var result = new ArrayList<OutputRecord>();
        for (int index = 0; index < records.size(); index++) {
            final var record = records.get(index);
            final var scope = BuiltinSupport.scope(record, index, body);
            final var json = new LinkedHashMap<>(record.json());
            for (final var entry : fields.entrySet()) {
                final var value = BuiltinSupport.evaluate(evaluator, entry.getValue(), scope);
                if (Objects.nonNull(value)) {
                    DynamicObjects.set(json, entry.getKey(), value);
                }
            }
            result.add(new OutputRecord(json, record.binary(), record.metadata(), record.isBinary()));
        }
        return List.copyOf(result);
    }
}
