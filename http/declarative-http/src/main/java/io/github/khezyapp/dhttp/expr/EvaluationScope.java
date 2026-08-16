package io.github.khezyapp.dhttp.expr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mutable holder of the per-item {@code $} bindings expressions resolve against.
 *
 * <p>The standard keys are predefined as constants so planners and actions bind consistently:</p>
 * <ul>
 * <li>{@code $credentials} — the stored credential values.</li>
 * <li>{@code $parameter} — the per-invocation parameters.</li>
 * <li>{@code $response} — the parsed response body (during post-receive shaping it is wrapped
 * under {@code result}).</li>
 * <li>{@code $responseItem} — the output record currently being shaped; an alias of {@code $item}
 * in the post-receive stage.</li>
 * <li>{@code $value} — the current value being transformed.</li>
 * <li>{@code $env} — runtime variables.</li>
 * <li>{@code $item} — the current input item at plan time; the output record being shaped during
 * post-receive.</li>
 * <li>{@code $index} — the record index.</li>
 * <li>{@code $parent} — reserved for chained operations (a parent operation's output); not bound
 * by the current engine.</li>
 * </ul>
 */
public final class EvaluationScope {

    public static final String CREDENTIALS = "$credentials";
    public static final String PARAMETER = "$parameter";
    public static final String RESPONSE = "$response";
    public static final String RESPONSE_ITEM = "$responseItem";
    public static final String VALUE = "$value";
    public static final String ENV = "$env";
    public static final String ITEM = "$item";
    public static final String INDEX = "$index";
    public static final String PARENT = "$parent";

    private final Map<String, Object> bindings = new HashMap<>();

    public static EvaluationScope create() {
        return new EvaluationScope();
    }

    public EvaluationScope bind(final String name,
                                final Object value) {
        bindings.put(name, value);
        return this;
    }

    public Object get(final String name) {
        return bindings.get(name);
    }

    public Map<String, Object> bindings() {
        return Collections.unmodifiableMap(bindings);
    }
}
