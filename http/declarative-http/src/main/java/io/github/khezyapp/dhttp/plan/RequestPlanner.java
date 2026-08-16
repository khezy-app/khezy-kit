package io.github.khezyapp.dhttp.plan;

import io.github.khezyapp.dhttp.action.ActionRegistry;
import io.github.khezyapp.dhttp.action.PostReceiveStep;
import io.github.khezyapp.dhttp.error.NonStringKeyExpressionException;
import io.github.khezyapp.dhttp.expr.EvaluationScope;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.pagination.PaginationRegistry;
import io.github.khezyapp.dhttp.pagination.PaginationStrategy;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.Send;
import io.github.khezyapp.dhttp.spec.Target;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpRequestBuilder;
import io.github.khezyapp.doa.DynamicObjects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pure request-planning core ({@code R16}): spec + context → {@link RequestPlan}.
 *
 * <p>Stateless; all inputs are explicit. Expressions resolve through an injected
 * {@link ExpressionEvaluator} and bodies serialize through an injected {@link JsonMapper}.</p>
 */
public final class RequestPlanner {

    private final ActionRegistry registry;
    private final PaginationRegistry paginationRegistry;
    private final ExpressionEvaluator evaluator;
    private final JsonMapper jsonMapper;

    public RequestPlanner() {
        this(ActionRegistry.withBuiltins(), PaginationRegistry.withBuiltins(),
                new JexlExpressionEvaluator(), JacksonJsonMapper.INSTANCE);
    }

    public RequestPlanner(final ExpressionEvaluator evaluator,
                          final JsonMapper jsonMapper) {
        this(ActionRegistry.withBuiltins(), PaginationRegistry.withBuiltins(), evaluator, jsonMapper);
    }

    /**
     * @param registry   the action registry used to materialize post-receive steps (custom actions
     *                   registered here resolve by their {@code actionKey})
     * @param evaluator  the expression evaluator
     * @param jsonMapper the JSON mapper
     */
    public RequestPlanner(final ActionRegistry registry,
                          final ExpressionEvaluator evaluator,
                          final JsonMapper jsonMapper) {
        this(registry, PaginationRegistry.withBuiltins(), evaluator, jsonMapper);
    }

    /**
     * @param registry           the action registry used to materialize post-receive steps (custom
     *                           actions registered here resolve by their {@code actionKey})
     * @param paginationRegistry the pagination registry used to materialize the route's strategy
     *                           (custom strategies registered here resolve by their mode)
     * @param evaluator          the expression evaluator
     * @param jsonMapper         the JSON mapper
     */
    public RequestPlanner(final ActionRegistry registry,
                          final PaginationRegistry paginationRegistry,
                          final ExpressionEvaluator evaluator,
                          final JsonMapper jsonMapper) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.paginationRegistry = Objects.requireNonNull(paginationRegistry, "paginationRegistry");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    /**
     * @param spec      the root spec (defaults source)
     * @param operation the operation to plan
     * @param ctx       the per-item context
     * @return a fully resolved {@link RequestPlan}
     */
    public RequestPlan plan(final HttpRequestSpec spec,
                            final Operation operation,
                            final RequestContext ctx) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(ctx, "ctx");
        final var route = FragmentMerger.mergeDefaults(spec, operation, operation.route());
        final var scope = scope(ctx);
        final var request = buildRequest(spec, route, ctx, scope);
        final var maxResults = Objects.isNull(route.output()) ? 0 : route.output().maxResults();
        return new RequestPlan(
                request,
                List.of(),
                postReceives(route),
                pagination(route),
                maxResults,
                authRequest(spec));
    }

    private HttpRequest buildRequest(final HttpRequestSpec spec,
                                     final Route route,
                                     final RequestContext ctx,
                                     final EvaluationScope scope) {
        final var shape = route.request();
        final var builder = HttpRequest.builder()
                .url(url(spec.baseUrl(), shape.baseUrl(), shape.path(), scope))
                .method(shape.method())
                .headers(resolveHeaders(shape.headers(), scope))
                .timeout(spec.defaultTimeoutMillis())
                .skipSsl(spec.defaultSkipSsl());
        if (Objects.nonNull(spec.security())) {
            for (final var domain : spec.security().allowedDomains()) {
                builder.allowedDomain(domain);
            }
            builder.allowIpLiteral(spec.security().allowIpLiteral())
                    .stripCrossOriginCredentials(spec.security().stripCrossOriginCredentials());
        }
        for (final var query : shape.query().entrySet()) {
            builder.query(resolveKey(query.getKey(), scope), resolveValue(query.getValue(), scope));
        }
        applySends(builder, route.sends(), shape.json(), ctx, scope);
        return builder.build();
    }

    /**
     * Resolves the request URL: an absolute {@code path} is used as the full URL; otherwise the
     * route's {@code shapeBaseUrl} (when configured) or the spec's base URL is joined with the
     * path.
     *
     * @param specBaseUrl  the spec-level base URL
     * @param shapeBaseUrl the route-level base URL override, or {@code null}
     * @param path         the URL path (may be templated)
     * @param scope        the expression scope
     * @return the resolved URL
     */
    private String url(final String specBaseUrl,
                       final String shapeBaseUrl,
                       final String path,
                       final EvaluationScope scope) {
        final var resolvedPath = evaluator.evaluate(path, scope, String.class);
        if (isAbsoluteUrl(resolvedPath)) {
            return resolvedPath;
        }
        final var base = Objects.nonNull(shapeBaseUrl) && !shapeBaseUrl.isBlank()
                ? shapeBaseUrl
                : specBaseUrl;
        final var resolvedBase = evaluator.evaluate(base, scope, String.class);
        if (resolvedBase.isBlank()) {
            return resolvedPath;
        }
        final var trimmed = resolvedBase.endsWith("/")
                ? resolvedBase.substring(0, resolvedBase.length() - 1)
                : resolvedBase;
        if (resolvedPath.startsWith("/")) {
            return trimmed + resolvedPath;
        }
        return trimmed + "/" + resolvedPath;
    }

    private static boolean isAbsoluteUrl(final String url) {
        final var lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private Map<String, String> resolveHeaders(final Map<String, String> headers,
                                               final EvaluationScope scope) {
        final var result = new LinkedHashMap<String, String>();
        for (final var entry : headers.entrySet()) {
            result.put(
                    resolveKey(entry.getKey(), scope),
                    evaluator.evaluate(entry.getValue(), scope, String.class)
            );
        }
        return result;
    }

    private void applySends(final HttpRequestBuilder builder,
                            final List<Send> sends,
                            final Object literalJson,
                            final RequestContext ctx,
                            final EvaluationScope scope) {
        if (literalBodyOnly(literalJson, sends)) {
            builder.body(new Body.JsonBody(serializeLiteral(literalJson, scope)));
            return;
        }
        final var body = new LinkedHashMap<String, Object>();
        if (hasLiteralBody(literalJson)) {
            final var resolved = resolveBody(literalJson, scope);
            if (!(resolved instanceof Map<?, ?>)) {
                throw new IllegalArgumentException(
                        "RequestShape.json must be a JSON object (Map) when combined with BODY sends");
            }
            body.putAll(asMap(resolved));
        }
        for (final var send : sends) {
            final var value = resolveSendValue(send, ctx, scope);
            if (Objects.isNull(value)) {
                continue;
            }
            if (send.target() == Target.BODY) {
                DynamicObjects.set(body, send.property(), value);
            } else {
                builder.query(send.property(), value);
            }
        }
        if (!body.isEmpty()) {
            builder.body(new Body.JsonBody(jsonMapper.write(body)));
        }
    }

    /**
     * @param literalJson the literal body declared on the request shape
     * @param sends       the route's send descriptors
     * @return true whens a literal body is declared and no body send merges into it
     */
    private static boolean literalBodyOnly(final Object literalJson,
                                           final List<Send> sends) {
        return hasLiteralBody(literalJson)
                && sends.stream().noneMatch(send -> send.target() == Target.BODY);
    }

    /**
     * @param literalJson the literal body declared on the request shape
     * @return true whens a literal body is declared (blank raw text counts as none)
     */
    private static boolean hasLiteralBody(final Object literalJson) {
        if (Objects.isNull(literalJson)) {
            return false;
        }
        return !(literalJson instanceof String s && s.isBlank());
    }

    /**
     * Serializes a literal body used as-is: raw JSON text passes through, every other value has its
     * expression leaves resolved and is converted by the JSON mapper (so a {@link List} root yields
     * an array body).
     *
     * @param literalJson the literal body declared on the request shape
     * @param scope       the expression scope
     * @return the JSON body text
     */
    private String serializeLiteral(final Object literalJson,
                                    final EvaluationScope scope) {
        if (literalJson instanceof String s) {
            return s;
        }
        return jsonMapper.write(resolveBody(literalJson, scope));
    }

    /**
     * Resolves expression leaves in a body structure: {@link Map} and {@link List} are walked
     * recursively and every {@link String} key or value is evaluated (so
     * {@code "= {{ $parameter.x }}"} as a value or a key resolves at runtime); all other values
     * pass through. Keys must resolve to a string.
     *
     * @param body  the literal body structure
     * @param scope the expression scope
     * @return the body with expression leaves resolved
     */
    private Object resolveBody(final Object body,
                               final EvaluationScope scope) {
        if (body instanceof Map<?, ?> map) {
            final var resolved = new LinkedHashMap<String, Object>();
            for (final var entry : map.entrySet()) {
                resolved.put(resolveKey(entry.getKey(), scope), resolveBody(entry.getValue(), scope));
            }
            return resolved;
        }
        if (body instanceof List<?> list) {
            final var resolved = new ArrayList<>();
            for (final var item : list) {
                resolved.add(resolveBody(item, scope));
            }
            return resolved;
        }
        return resolveValue(body, scope);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(final Object value) {
        return (Map<String, Object>) value;
    }

    private Object resolveSendValue(final Send send,
                                    final RequestContext ctx,
                                    final EvaluationScope scope) {
        final Object raw;
        if (Objects.nonNull(send.valueOverride())) {
            raw = resolveExpression(send.valueOverride(), scope);
        } else {
            raw = ctx.parameters().get(send.fromParam());
        }
        if (Objects.isNull(raw)) {
            return null;
        }
        if (send.dotNotation()) {
            return DynamicObjects.get(raw, send.property());
        }
        return raw;
    }

    private Object resolveExpression(final Expression expression,
                                     final EvaluationScope scope) {
        if (expression.isExpression()) {
            return evaluator.evaluate(expression.raw(), scope, Object.class);
        }
        return expression.literal();
    }

    private Object resolveValue(final Object value,
                                final EvaluationScope scope) {
        if (value instanceof String s) {
            return evaluator.evaluate(s, scope, Object.class);
        }
        return value;
    }

    /**
     * Resolves a map key (header key, query key, or literal body key) and validates it is a string.
     *
     * @param key   the raw key, which may be an expression
     * @param scope the expression scope
     * @return the resolved key
     * @throws NonStringKeyExpressionException whens the key resolves to a non-string value
     */
    private String resolveKey(final Object key,
                              final EvaluationScope scope) {
        final var resolved = resolveValue(key, scope);
        if (resolved instanceof String s) {
            return s;
        }
        throw new NonStringKeyExpressionException(String.valueOf(key), resolved);
    }

    private static EvaluationScope scope(final RequestContext ctx) {
        final var scope = EvaluationScope.create()
                .bind(EvaluationScope.PARAMETER, ctx.parameters())
                .bind(EvaluationScope.CREDENTIALS, ctx.credentials())
                .bind(EvaluationScope.ENV, ctx.variables());
        if (Objects.nonNull(ctx.item())) {
            scope.bind(EvaluationScope.ITEM, ctx.item().json());
        }
        return scope;
    }

    private List<PostReceiveStep> postReceives(final Route route) {
        if (Objects.isNull(route.output())) {
            return List.of();
        }
        return route.output().postReceive().stream()
                .map(descriptor -> PostReceiveStep.materialize(descriptor, evaluator, registry))
                .toList();
    }

    private static AuthRequest authRequest(final HttpRequestSpec spec) {
        final var ref = spec.defaultCredential();
        if (Objects.isNull(ref)) {
            return null;
        }
        return new AuthRequest(ref, ref.type());
    }

    /**
     * Builds the pagination strategy for a route, or {@code null} whens none is configured. A fresh
     * strategy is created per plan so page cursors/offsets never leak across executions.
     *
     * <p>Built-in modes ({@code offset}, {@code page}, {@code cursor}, {@code nextUrl}) resolve
     * through the built-in registry factories; any other mode must have been registered via the
     * injected {@link PaginationRegistry} or planning fails fast.</p>
     *
     * @param route the merged route
     * @return the strategy for the route's mode, or {@code null}
     */
    private PaginationStrategy pagination(final Route route) {
        final var pagination = route.pagination();
        if (Objects.isNull(pagination)) {
            return null;
        }
        return paginationRegistry.create(pagination, evaluator, jsonMapper);
    }
}
