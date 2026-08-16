package io.github.khezyapp.dhttp.action.builtin;

import io.github.khezyapp.dhttp.action.PostReceiveAction;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.doa.DynamicObjects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Sorts the records by a dotted key, ascending or descending. Covers {@link PostReceive.SortByKey}.
 *
 * @param key  the dotted sort key
 * @param desc sort descending whens true
 */
public record SortByKey(String key, boolean desc) implements PostReceiveAction {

    public SortByKey {
        Objects.requireNonNull(key, "key");
    }

    public static SortByKey from(final PostReceive descriptor) {
        if (descriptor instanceof PostReceive.SortByKey sortByKey) {
            return new SortByKey(sortByKey.key(), sortByKey.desc());
        }
        return new SortByKey(String.valueOf(BuiltinSupport.prop(descriptor, "key")),
                Boolean.parseBoolean(String.valueOf(BuiltinSupport.prop(descriptor, "desc"))));
    }

    @Override
    public List<OutputRecord> apply(final List<OutputRecord> records,
                                    final HttpResult response) {
        final var sorted = new ArrayList<>(records);
        sorted.sort(comparator());
        return List.copyOf(sorted);
    }

    private Comparator<OutputRecord> comparator() {
        final var natural = naturalOrder();
        final var ascending = Comparator.<OutputRecord, Object>comparing(
                (final var record) -> DynamicObjects.get(record.json(), key), natural);
        return desc ? ascending.reversed() : ascending;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparator<Object> naturalOrder() {
        return (final var a, final var b) -> {
            if (a == b) {
                return 0;
            }
            if (Objects.isNull(a)) {
                return -1;
            }
            if (Objects.isNull(b)) {
                return 1;
            }
            if (a instanceof Comparable && a.getClass().isInstance(b)) {
                return ((Comparable) a).compareTo(b);
            }
            return String.valueOf(a).compareTo(String.valueOf(b));
        };
    }
}
