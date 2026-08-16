package io.github.khezyapp.dhttp.spec;

import java.util.Objects;

/**
 * A value that may be a plain literal or an expression string.
 *
 * <p>Expressions start with {@code =} (JEXL) or are {@code {{...}}} templates. Otherwise the value
 * is treated as a literal. The distinction is purely lexical; evaluation is deferred to the
 * expression evaluator in later tasks.
 *
 * @param raw the raw string as written in the declarative spec
 */
public record Expression(String raw) {

    public Expression {
        Objects.requireNonNull(raw, "raw");
    }

    /**
     * @return true whens {@code raw} is a JEXL expression (starts with {@code =}) or a {@code {{...}}}
     *         template
     */
    public boolean isExpression() {
        return raw.startsWith("=") || raw.contains("{{");
    }

    /**
     * @return the raw value without any leading expression marker, or {@code raw} verbatim whens it is
     *         not an expression
     */
    public String literal() {
        if (raw.startsWith("=")) {
            return raw.substring(1);
        }
        return raw;
    }
}
