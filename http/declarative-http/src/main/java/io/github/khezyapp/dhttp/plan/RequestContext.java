package io.github.khezyapp.dhttp.plan;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable per-item bindings that planning and expression evaluation resolve against (§3.2).
 *
 * @param operationId the id of the operation being executed
 * @param item        the current input record, or {@code null}
 * @param parameters  resolved node/operation parameters
 * @param credentials decrypted credential fields ({@code R10})
 * @param variables   environment/instance variables
 * @param onResponse  optional callback fed with each page's {@link HttpResult} right after the
 *                    transport send and before post-receive shaping (including every page fetched
 *                    by a pagination strategy and every item in {@code executeAll})
 */
public record RequestContext(String operationId,
                             OutputRecord item,
                             Map<String, Object> parameters,
                             Map<String, Object> credentials,
                             Map<String, Object> variables,
                             Consumer<HttpResult> onResponse) {

    public RequestContext {
        Objects.requireNonNull(operationId, "operationId");
        parameters = copy(parameters);
        credentials = copy(credentials);
        variables = copy(variables);
    }

    public RequestContext(final String operationId,
                          final Map<String, Object> parameters) {
        this(operationId, null, parameters, Map.of(), Map.of(), null);
    }

    public RequestContext(final String operationId,
                          final Map<String, Object> parameters,
                          final Consumer<HttpResult> onResponse) {
        this(operationId, null, parameters, Map.of(), Map.of(), onResponse);
    }

    private static Map<String, Object> copy(final Map<String, Object> source) {
        if (source == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
