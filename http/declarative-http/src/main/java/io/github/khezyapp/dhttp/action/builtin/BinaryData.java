package io.github.khezyapp.dhttp.action.builtin;

import io.github.khezyapp.dhttp.action.PostReceiveAction;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.doa.DynamicObjects;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Forwards binary response data to a destination property: the raw payload becomes the record's
 * {@code binary} data and its base64 form is set on each record at the dotted destination path.
 * Covers {@link PostReceive.BinaryData}.
 *
 * @param destinationProperty the property to store the base64 payload in
 */
public record BinaryData(String destinationProperty) implements PostReceiveAction {

    public BinaryData {
        Objects.requireNonNull(destinationProperty, "destinationProperty");
    }

    public static BinaryData from(final PostReceive descriptor) {
        if (descriptor instanceof PostReceive.BinaryData binaryData) {
            return new BinaryData(binaryData.destinationProperty());
        }
        return new BinaryData(String.valueOf(BuiltinSupport.prop(descriptor, "destinationProperty")));
    }

    @Override
    public List<OutputRecord> apply(final List<OutputRecord> records,
                                    final HttpResult response) {
        final var bytes = response.body();
        if (Objects.isNull(bytes)) {
            return records;
        }
        if (records.isEmpty()) {
            return prepare(List.of(), bytes);
        }
        return prepare(records, bytes);
    }

    private List<OutputRecord> prepare(final List<OutputRecord> records,
                                       final byte[] bytes) {
        final var payload = Base64.getEncoder().encodeToString(bytes);
        if (records.isEmpty()) {
            final var json = new LinkedHashMap<String, Object>();
            json.put(destinationProperty, payload);
            return List.of(new OutputRecord(json, bytes, Map.of(), true));
        }
        final var result = new ArrayList<OutputRecord>();
        for (final var record : records) {
            final var json = new LinkedHashMap<>(record.json());
            DynamicObjects.set(json, destinationProperty, payload);
            final var binary = Objects.isNull(record.binary()) ? bytes : record.binary();
            result.add(new OutputRecord(json, binary, record.metadata(), record.isBinary()));
        }
        return List.copyOf(result);
    }
}
