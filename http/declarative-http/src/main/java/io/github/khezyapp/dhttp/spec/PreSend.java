package io.github.khezyapp.dhttp.spec;

import java.util.Map;
import java.util.Objects;

/**
 * A pre-send transformation hook descriptor ({@code R6}).
 *
 * @param actionKey the registered pre-send action identifier
 * @param props     action-specific properties
 */
public record PreSend(String actionKey, Map<String, Object> props) {

    public PreSend {
        Objects.requireNonNull(actionKey, "actionKey");
        props = Map.copyOf(Objects.requireNonNullElseGet(props, Map::of));
    }
}
