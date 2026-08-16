package io.github.khezyapp.dhttp.expr.jexl;

import io.github.khezyapp.dhttp.expr.EvaluationScope;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;

import java.util.Objects;

import org.apache.commons.jexl3.JexlEngine;

/**
 * Default {@link ExpressionEvaluator} over JEXL 3.
 *
 * <ul>
 * <li>{@code =} marks the value as an expression; {@code {{...}}} wraps a Java (JEXL) expression
 * inside a template.</li>
 * <li>{@code ={{ expr }}} — exactly one expression and no surrounding text — returns the evaluated
 * object as its runtime type (e.g. a number, map, or list, not a string).</li>
 * <li>{@code = Hello Mr. {{ name }}} — text and/or several {@code {{...}}} blocks — renders to a
 * String template, each block replaced by the {@code toString()} of its evaluated expression.</li>
 * <li>{@code {{...}}} without a leading {@code =} renders the same String template.</li>
 * <li>anything else → returned as-is (literal).</li>
 * </ul>
 *
 * <p>Final conversion to the requested {@code type} goes through the {@link JsonMapper} so any
 * serializable target (record, POJO, collection, primitive) is supported.</p>
 */
public final class JexlExpressionEvaluator implements ExpressionEvaluator {

    private static final String OPEN = "{{";
    private static final String CLOSE = "}}";

    private final JexlEngine engine;
    private final JsonMapper jsonMapper;

    public JexlExpressionEvaluator() {
        this(JexlEngineFactory.defaultEngine(), JacksonJsonMapper.INSTANCE);
    }

    public JexlExpressionEvaluator(final JexlEngine engine) {
        this(engine, JacksonJsonMapper.INSTANCE);
    }

    public JexlExpressionEvaluator(final JexlEngine engine,
                                   final JsonMapper jsonMapper) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    @Override
    public boolean isExpression(final String value) {
        return value.startsWith("=");
    }

    @Override
    public <T> T evaluate(final String expression,
                          final EvaluationScope scope,
                          final Class<T> type) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(type, "type");
        if (Objects.isNull(expression)) {
            return null;
        }
        return convert(evaluateRaw(expression, scope), type);
    }

    private Object evaluateRaw(final String expression,
                               final EvaluationScope scope) {
        if (expression.startsWith("=")) {
            var body = expression.substring(1);
            if (body.startsWith(" ")) {
                body = body.substring(1);
            }
            final var single = singleExpression(body.strip());
            if (Objects.nonNull(single)) {
                return evaluateJexl(single, scope);
            }
            if (body.contains(OPEN)) {
                return renderTemplate(body, scope);
            }
            return body;
        }
        if (expression.contains(OPEN)) {
            return renderTemplate(expression, scope);
        }
        return expression;
    }

    private Object evaluateJexl(final String expr,
                                final EvaluationScope scope) {
        final var jexl = engine.createExpression(expr);
        return jexl.evaluate(JexlEngineFactory.context(scope));
    }

    /**
     * @param body the text after a leading {@code =}, already trimmed
     * @return the trimmed inner expression whens {@code body} is exactly one {@code {{...}}} block,
     * otherwise {@code null}
     */
    private static String singleExpression(final String body) {
        if (!body.startsWith(OPEN) || !body.endsWith(CLOSE)) {
            return null;
        }
        final var inner = body.substring(OPEN.length(), body.length() - CLOSE.length());
        if (inner.contains(OPEN) || inner.contains(CLOSE)) {
            return null;
        }
        return inner.strip();
    }

    private String renderTemplate(final String template,
                                  final EvaluationScope scope) {
        final var out = new StringBuilder();
        int pos = 0;
        while (true) {
            final int open = template.indexOf(OPEN, pos);
            if (open < 0) {
                out.append(template.substring(pos));
                break;
            }
            out.append(template, pos, open);
            final int close = template.indexOf(CLOSE, open + OPEN.length());
            if (close < 0) {
                throw new IllegalStateException("Unclosed '{{' in expression template: " + template);
            }
            final var expr = template.substring(open + OPEN.length(), close).trim();
            final var value = evaluateJexl(expr, scope);
            out.append(Objects.isNull(value) ? "" : value.toString());
            pos = close + CLOSE.length();
        }
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(final Object value,
                          final Class<T> type) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (type == Object.class) {
            return (T) value;
        }
        return jsonMapper.convert(value, type);
    }
}
