package io.github.khezyapp.dhttp.spec;

import java.util.Map;
import java.util.Objects;

/**
 * Post-response shaping instructions applied by the engine ({@code R7}).
 *
 * <p>Each variant is a self-contained instruction. {@link SetKeyValue} and
 * {@link CustomPostReceive} defensively copy their map fields.
 */
public sealed interface PostReceive
        permits PostReceive.RootProperty, PostReceive.FilterItems, PostReceive.LimitItems,
        PostReceive.SetValue, PostReceive.SortByKey, PostReceive.SetKeyValue,
        PostReceive.BinaryData, PostReceive.CustomPostReceive {

    /**
     * Reads a single property (dotted path) from the response body.
     *
     * @param property the dotted path to read
     */
    record RootProperty(String property) implements PostReceive {
    }

    /**
     * Keeps items whose {@link Expression} evaluates truthy.
     *
     * @param pass the filter expression
     */
    record FilterItems(Expression pass) implements PostReceive {
    }

    /**
     * Caps the number of output items.
     *
     * @param max the maximum item count
     */
    record LimitItems(int max) implements PostReceive {
    }

    /**
     * Replaces the output with an evaluated value.
     *
     * @param value the value expression
     */
    record SetValue(Expression value) implements PostReceive {
    }

    /**
     * Sorts items by a key.
     *
     * @param key  the dotted sort key
     * @param desc sort descending whens true
     */
    record SortByKey(String key, boolean desc) implements PostReceive {
    }

    /**
     * Sets several dotted fields on the output.
     *
     * @param fields map of dotted key to value expression
     */
    record SetKeyValue(Map<String, Expression> fields) implements PostReceive {
        public SetKeyValue {
            fields = Map.copyOf(Objects.requireNonNullElseGet(fields, Map::of));
        }
    }

    /**
     * Forwards binary response data to a destination property.
     *
     * @param destinationProperty the property to store the binary payload in
     */
    record BinaryData(String destinationProperty) implements PostReceive {
    }

    /**
     * Delegates shaping to a registered custom action.
     *
     * @param actionKey the registered action identifier
     * @param props     action-specific properties
     */
    record CustomPostReceive(String actionKey, Map<String, Object> props) implements PostReceive {
        public CustomPostReceive {
            props = Map.copyOf(Objects.requireNonNullElseGet(props, Map::of));
        }
    }
}
