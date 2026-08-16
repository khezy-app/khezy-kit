package io.github.khezyapp.dhttp.action.builtin;

import io.github.khezyapp.dhttp.action.PostReceiveAction;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;

/**
 * Caps the number of output records at {@code max}. Covers {@link PostReceive.LimitItems}.
 *
 * @param max the maximum item count
 */
public record LimitItems(int max) implements PostReceiveAction {

    public static LimitItems from(final PostReceive descriptor) {
        if (descriptor instanceof PostReceive.LimitItems limitItems) {
            return new LimitItems(limitItems.max());
        }
        return new LimitItems(Integer.parseInt(String.valueOf(BuiltinSupport.prop(descriptor, "max"))));
    }

    @Override
    public List<OutputRecord> apply(final List<OutputRecord> records,
                                    final HttpResult response) {
        if (max <= 0 || records.isEmpty()) {
            return List.of();
        }
        if (records.size() <= max) {
            return records;
        }
        return List.copyOf(records.subList(0, max));
    }
}
