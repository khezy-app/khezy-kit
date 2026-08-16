package io.github.khezyapp.dhttp.engine;

import io.github.khezyapp.dhttp.action.ActionRegistry;
import io.github.khezyapp.dhttp.auth.Authenticator;
import io.github.khezyapp.dhttp.auth.GenericAuthenticator;
import io.github.khezyapp.dhttp.auth.credential.CredentialStore;
import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.auth.oauth2.InMemoryTokenStore;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2AuthorizationFlow;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2RequestAuthenticator;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2TokenClient;
import io.github.khezyapp.dhttp.auth.oauth2.TokenStore;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.pagination.PaginationRegistry;
import io.github.khezyapp.dhttp.plan.AuthRequest;
import io.github.khezyapp.dhttp.plan.ConditionEvaluator;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.plan.RequestPlanner;
import io.github.khezyapp.dhttp.security.SensitiveOutputRedactor;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic execution engine (§3.3, R13/R15/R16): config-time validation and per-item execution
 * over a {@link HttpRequestSpec}.
 *
 * <p>Stateless — all per-call inputs travel in {@link RequestContext}. Operation selection uses the
 * stateless {@link ConditionEvaluator} utility; planning is delegated to {@link RequestPlanner};
 * execution to {@link Pipeline}.</p>
 */
public final class DeclarativeHttpEngine {

    private final RequestPlanner planner;
    private final CredentialStore store;
    private final HttpTransport transport;
    private final JsonMapper jsonMapper;
    private final TokenStore tokenStore;
    private final GenericAuthenticator genericAuth = new GenericAuthenticator();
    private final OAuth2RequestAuthenticator oauth2Auth;
    private final Pipeline pipeline;

    /**
     * Creates an engine with the default JSON mapper and a fresh in-memory token store.
     *
     * @param registry  the post-receive action registry
     * @param store     the credential store
     * @param transport the transport used for every send
     * @param evaluator the expression evaluator
     */
    public DeclarativeHttpEngine(final ActionRegistry registry,
                                 final CredentialStore store,
                                 final HttpTransport transport,
                                 final ExpressionEvaluator evaluator) {
        this(registry, store, transport, evaluator, JacksonJsonMapper.INSTANCE, new InMemoryTokenStore());
    }

    /**
     * Creates an engine with full dependency control (e.g. a shared token store so OAuth2 tokens
     * survive across engine instances).
     *
     * @param registry    the post-receive action registry
     * @param store       the credential store
     * @param transport   the transport used for every send (including the token endpoint)
     * @param evaluator   the expression evaluator
     * @param jsonMapper  the JSON mapper shared with planning and shaping
     * @param tokenStore  the token store shared with the OAuth2 request-time lifecycle
     */
    public DeclarativeHttpEngine(final ActionRegistry registry,
                                 final CredentialStore store,
                                 final HttpTransport transport,
                                 final ExpressionEvaluator evaluator,
                                 final JsonMapper jsonMapper,
                                 final TokenStore tokenStore) {
        this(registry, PaginationRegistry.withBuiltins(), store, transport, evaluator,
                jsonMapper, tokenStore);
    }

    /**
     * Creates an engine with full dependency control, including a custom pagination registry.
     *
     * @param registry           the post-receive action registry
     * @param paginationRegistry the pagination registry used to materialize route strategies
     * @param store              the credential store
     * @param transport          the transport used for every send (including the token endpoint)
     * @param evaluator          the expression evaluator
     * @param jsonMapper         the JSON mapper shared with planning and shaping
     * @param tokenStore         the token store shared with the OAuth2 request-time lifecycle
     */
    public DeclarativeHttpEngine(final ActionRegistry registry,
                                 final PaginationRegistry paginationRegistry,
                                 final CredentialStore store,
                                 final HttpTransport transport,
                                 final ExpressionEvaluator evaluator,
                                 final JsonMapper jsonMapper,
                                 final TokenStore tokenStore) {
        this.store = Objects.requireNonNull(store, "store");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
        this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore");
        this.planner = new RequestPlanner(
                Objects.requireNonNull(registry, "registry"),
                Objects.requireNonNull(paginationRegistry, "paginationRegistry"),
                Objects.requireNonNull(evaluator, "evaluator"),
                jsonMapper
        );
        this.oauth2Auth = new OAuth2RequestAuthenticator(tokenStore,
                new OAuth2TokenClient(transport, jsonMapper), jsonMapper, 0L);
        this.pipeline = new Pipeline(jsonMapper);
    }

    /**
     * Config-time validation (R10, §6.6): resolves the spec's default credential and, for
     * {@code oauth2}, verifies a valid access token is stored. Only the credential/token stores are
     * touched — never the transport.
     *
     * @param spec the spec to validate
     * @throws io.github.khezyapp.dhttp.error.OAuth2NotConfiguredException whens the OAuth2 credential
     *         has no valid token
     * @throws IllegalArgumentException whens the referenced credential does not exist
     */
    public void validate(final HttpRequestSpec spec) {
        Objects.requireNonNull(spec, "spec");
        final var ref = spec.defaultCredential();
        if (Objects.isNull(ref)) {
            return;
        }
        final var credential = store.resolve(ref, new RequestContext(ref.id(), Map.of()))
                .orElseThrow(() -> new IllegalArgumentException("credential not found: " + ref.id()));
        if ("oauth2".equals(ref.type())) {
            final var creds = jsonMapper.fromMap(credential.fields(), OAuth2Credentials.class);
            OAuth2AuthorizationFlow.create(ref.id(), creds, null, tokenStore).validate();
        }
    }

    /**
     * Executes the operation whose preconditions match the context: plan, then run the pipeline.
     *
     * @param spec the root spec
     * @param ctx  the per-item context
     * @return the shaped output records
     * @throws HttpApiException whens no operation matches or any send fails (R13)
     */
    public List<OutputRecord> execute(final HttpRequestSpec spec,
                                      final RequestContext ctx) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(ctx, "ctx");
        final var operation = selectOperation(spec, ctx);
        if (Objects.isNull(operation)) {
            throw new HttpApiException(ctx.operationId(), -1,
                    "No operation matched the request context");
        }
        final var plan = planner.plan(spec, operation, ctx);
        return redact(pipeline.run(plan, ctx, transport, authenticatorFor(plan.authRequest()), store),
                spec.security().sensitiveOutputFields());
    }

    /**
     * Executes one operation over many items with optional batching pacing (§10.2): each context is
     * run through the per-item pipeline, sleeping {@code batchIntervalMillis} before every
     * {@code batchSize}-th item (n8n V3 throttle semantics). The accumulated records are capped by
     * the active operation's {@code maxResults}.
     *
     * @param spec     the root spec
     * @param contexts the per-item contexts
     * @return the combined shaped output records
     */
    public List<OutputRecord> executeAll(final HttpRequestSpec spec,
                                         final List<RequestContext> contexts) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(contexts, "contexts");
        if (contexts.isEmpty()) {
            return List.of();
        }
        final var batching = spec.batching();
        final var batchSize = Objects.isNull(batching) ? 1 : batching.batchSize();
        final var interval = Objects.isNull(batching) ? 0L : batching.batchIntervalMillis();
        final var operationId = contexts.get(0).operationId();
        final var all = new ArrayList<OutputRecord>();
        for (int i = 0; i < contexts.size(); i++) {
            if (i > 0 && interval > 0 && i % batchSize == 0) {
                pause(interval, operationId, i);
            }
            all.addAll(execute(spec, contexts.get(i)));
        }
        return redact(capAll(all, spec, operationId), spec.security().sensitiveOutputFields());
    }

    /**
     * Design-time metadata mode (R15): plans the active operation and applies a single
     * option-shaping post-receive — a {@link PostReceive.CustomPostReceive} registered under
     * {@code loadKey} — whose output records become {@link OptionItem}s for a dropdown.
     *
     * <p>Paging state is driven by the option-shaping action through {@link
     * OutputRecord#metadata()}: a record metadata key {@code hasMore} (truthy) marks another page
     * available, an optional {@code nextCursor} carries a single cursor, and an optional
     * {@code nextParameters} map carries the request parameters for the next page (for APIs that
     * paginate with several parameters). The engine aggregates these into the returned
     * {@link OptionPage}.</p>
     *
     * @param spec    the root spec
     * @param ctx     the design-time context
     * @param loadKey the registered option-shaping action key
     * @return the shaped options plus paging state
     */
    public OptionPage describe(final HttpRequestSpec spec,
                               final RequestContext ctx,
                               final String loadKey) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(loadKey, "loadKey");
        final var operation = selectOperation(spec, ctx);
        if (Objects.isNull(operation)) {
            throw new HttpApiException(ctx.operationId(), -1, "No operation matched the request context");
        }
        final var plan = planner.plan(spec, operation, ctx);
        final var step = plan.postReceives().stream()
                .filter(candidate -> candidate.descriptor() instanceof PostReceive.CustomPostReceive custom
                        && loadKey.equals(custom.actionKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No option-shaping action registered for loadKey '" + loadKey + "'"));
        final var optionPlan = new RequestPlan(
                plan.request(),
                plan.preSends(),
                List.of(step),
                plan.pagination(),
                plan.maxResults(),
                plan.authRequest()
        );
        final var records = redact(
                pipeline.run(optionPlan, ctx, transport, authenticatorFor(plan.authRequest()), store),
                spec.security().sensitiveOutputFields()
        );
        return toOptionPage(records);
    }

    /**
     * Masks the configured {@code sensitiveOutputFields} in the returned records before they reach
     * the caller, so sensitive values never leak into logged output.
     *
     * @param records              the shaped output records
     * @param sensitiveOutputFields the dotted fields to mask, or empty for no redaction
     * @return records with the sensitive fields masked, or the same list whens none are configured
     */
    private static List<OutputRecord> redact(final List<OutputRecord> records,
                                             final List<String> sensitiveOutputFields) {
        if (sensitiveOutputFields.isEmpty()) {
            return records;
        }
        final var result = new ArrayList<OutputRecord>(records.size());
        for (final var record : records) {
            if (record.isBinary()) {
                result.add(record);
                continue;
            }
            result.add(
                    OutputRecord.ofJson(
                            SensitiveOutputRedactor.redact(record.json(), sensitiveOutputFields),
                            record.metadata()
                    )
            );
        }
        return result;
    }

    private static OptionPage toOptionPage(final List<OutputRecord> records) {
        final var items = new ArrayList<OptionItem>(records.size());
        var hasMore = false;
        String nextCursor = null;
        Map<String, Object> nextParameters = null;
        for (final var record : records) {
            items.add(toOptionItem(record));
            final var metadata = record.metadata();
            hasMore |= boolOf(metadata, "hasMore");
            if (Objects.isNull(nextCursor) && Objects.nonNull(metadata.get("nextCursor"))) {
                nextCursor = String.valueOf(metadata.get("nextCursor"));
            }
            if (Objects.isNull(nextParameters)) {
                nextParameters = nextParametersOf(metadata);
            }
        }
        return new OptionPage(List.copyOf(items), hasMore, nextCursor,
                Objects.requireNonNullElse(nextParameters, Map.of()));
    }

    private static Map<String, Object> nextParametersOf(final Map<String, Object> metadata) {
        final var raw = metadata.get("nextParameters");
        if (!(raw instanceof Map<?, ?> params)) {
            return null;
        }
        final var result = new LinkedHashMap<String, Object>();
        for (final var entry : params.entrySet()) {
            final var value = entry.getValue();
            if (Objects.nonNull(value)) {
                result.put(String.valueOf(entry.getKey()), value);
            }
        }
        return result;
    }

    private static Operation selectOperation(final HttpRequestSpec spec,
                                             final RequestContext ctx) {
        return ConditionEvaluator.selectOperation(spec, ctx);
    }

    private static void pause(final long millis,
                              final String operationId,
                              final int itemIndex) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpApiException(operationId, itemIndex,
                    "Batching throttle interrupted before item " + itemIndex);
        }
    }

    private static List<OutputRecord> capAll(final List<OutputRecord> all,
                                             final HttpRequestSpec spec,
                                             final String operationId) {
        final var max = maxResults(spec, operationId);
        if (max > 0 && all.size() > max) {
            return List.copyOf(all.subList(0, max));
        }
        return List.copyOf(all);
    }

    private static int maxResults(final HttpRequestSpec spec,
                                  final String operationId) {
        for (final Operation candidate : spec.operations()) {
            if (operationId.equals(candidate.id())
                    && Objects.nonNull(candidate.route())
                    && Objects.nonNull(candidate.route().output())) {
                return candidate.route().output().maxResults();
            }
        }
        return 0;
    }

    private Authenticator authenticatorFor(final AuthRequest authRequest) {
        if (Objects.isNull(authRequest)) {
            return null;
        }
        return "oauth2".equals(authRequest.type()) ? oauth2Auth : genericAuth;
    }

    private static OptionItem toOptionItem(final OutputRecord record) {
        final var json = record.json();
        return new OptionItem(
                stringOf(json, "name"),
                stringOf(json, "value"),
                nullableOf(json, "description"),
                nullableOf(json, "icon"),
                nullableOf(json, "group"),
                boolOf(json, "disabled"));
    }

    private static String stringOf(final Map<String, Object> json,
                                   final String key) {
        final var value = json.get(key);
        return Objects.isNull(value) ? "" : String.valueOf(value);
    }

    private static String nullableOf(final Map<String, Object> json,
                                     final String key) {
        final var value = json.get(key);
        return Objects.isNull(value) ? null : String.valueOf(value);
    }

    private static boolean boolOf(final Map<String, Object> json,
                                  final String key) {
        final var value = json.get(key);
        return Objects.nonNull(value) && Boolean.parseBoolean(String.valueOf(value));
    }
}
