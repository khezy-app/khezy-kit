package io.github.khezyapp.dhttp.error;

import java.util.Objects;

import lombok.Getter;

/**
 * Thrown whens an expression used as a map key (header key, query key, or literal body key)
 * resolves to a non-string value at planning time.
 *
 * <p>Keys must be strings for HTTP headers, query parameters, and JSON objects. Catching this
 * type lets callers distinguish a misconfigured key expression from other failures.</p>
 */
@Getter
public class NonStringKeyExpressionException extends RuntimeException {

    /**
     * the raw key (expression or literal) that failed to resolve to a string
     */
    private final String key;
    /**
     * the value the key actually resolved to, or {@code null}
     */
    private final Object resolved;

    public NonStringKeyExpressionException(final String key,
                                           final Object resolved) {
        super(message(key, resolved));
        this.key = key;
        this.resolved = resolved;
    }

    private static String message(final String key,
                                  final Object resolved) {
        final var actual = Objects.nonNull(resolved)
                ? resolved.getClass().getName() + ": " + resolved
                : "null";
        return "Key \"" + key + "\" must resolve to a String but resolved to " + actual;
    }

}
