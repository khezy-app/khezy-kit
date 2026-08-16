package io.github.khezyapp.dhttp.engine;

import io.github.khezyapp.dhttp.auth.AuthResult;
import io.github.khezyapp.dhttp.auth.Authenticator;
import io.github.khezyapp.dhttp.auth.credential.CredentialStore;
import io.github.khezyapp.dhttp.auth.credential.DecryptedCredential;
import io.github.khezyapp.dhttp.auth.oauth2.OAuth2RequestAuthenticator;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.error.HttpErrorFactory;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.plan.AuthRequest;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.security.SecretRedactor;
import io.github.khezyapp.dhttp.security.SsrfGuard;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic execution pipeline (§4): preSend → auth → transport → postReceive, with pagination
 * and maxResults capping.
 *
 * <p>Stateless: every input arrives through {@link #run(RequestPlan, RequestContext, HttpTransport,
 * Authenticator, CredentialStore)}. Each send is wrapped with the Task 06 SSRF allow-list guard and
 * every {@link HttpApiException} is re-raised through {@link HttpErrorFactory} with secrets redacted.
 * When a route declares no post-receive shaping, the response body is passed through by default —
 * an object becomes one record, an array one record per item — so an operation without an
 * {@code Output} still returns the server response.</p>
 */
public final class Pipeline {

    private final JsonMapper jsonMapper;

    /**
     * @param jsonMapper the JSON mapper used for the default body-passthrough mapping
     */
    public Pipeline(final JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }
    /**
     * Executes one operation's plan, fetching additional pages whens a pagination strategy is present
     * and capping the accumulated records by the plan's maxResults.
     *
     * @param plan      the resolved request plan
     * @param ctx       the per-item context
     * @param transport the transport used for every send (initial, paged, and OAuth2 retry)
     * @param auth      the authenticator matching {@code plan.authRequest()}, or {@code null}
     * @param store     the credential store used to resolve {@code plan.authRequest().ref()}
     * @return the shaped output records
     */
    public List<OutputRecord> run(final RequestPlan plan,
                                  final RequestContext ctx,
                                  final HttpTransport transport,
                                  final Authenticator auth,
                                  final CredentialStore store) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(transport, "transport");
        final var guarded = new GuardedTransport(transport, ctx);
        final var all = new ArrayList<OutputRecord>();
        var request = Objects.isNull(plan.pagination())
                ? plan.request()
                : plan.pagination().initRequest(plan);
        while (true) {
            final var result = send(applyPreSends(plan, request), plan, ctx, guarded, store, auth);
            var page = applyPostReceives(plan, result);
            if (Objects.nonNull(plan.pagination())) {
                page = plan.pagination().collect(plan, result, page);
            }
            all.addAll(page);
            final var next = nextPage(plan, result);
            if (Objects.isNull(next)) {
                break;
            }
            request = next;
        }
        return cap(all, plan.maxResults());
    }

    private static HttpRequest applyPreSends(final RequestPlan plan,
                                             final HttpRequest request) {
        var current = request;
        for (final var preSend : plan.preSends()) {
            current = preSend.apply(current);
        }
        return current;
    }

    private List<OutputRecord> applyPostReceives(final RequestPlan plan,
                                                 final HttpResult result) {
        var records = passthrough(result);
        for (final var step : plan.postReceives()) {
            records = step.action().apply(records, result);
        }
        return records;
    }

    /**
     * Default response mapping (n8n semantics): the body object becomes one record, an array one
     * record per item, and a scalar is wrapped under {@code value}. Applied whens no post-receive
     * step replaces the records, so a route without output shaping still yields the response.
     */
    private List<OutputRecord> passthrough(final HttpResult result) {
        final var body = result.bodyString();
        if (body.isBlank()) {
            return List.of();
        }
        try {
            return toRecords(jsonMapper.read(body, Object.class));
        } catch (final RuntimeException e) {
            return List.of(OutputRecord.ofJson(Map.of("value", body)));
        }
    }

    private static List<OutputRecord> toRecords(final Object found) {
        if (Objects.isNull(found)) {
            return List.of();
        }
        if (found instanceof List<?> items) {
            final var result = new ArrayList<OutputRecord>();
            for (final var item : items) {
                if (Objects.isNull(item)) {
                    continue;
                }
                if (item instanceof Map<?, ?>) {
                    result.add(OutputRecord.ofJson(map(item)));
                } else {
                    result.add(OutputRecord.ofJson(Map.of("value", item)));
                }
            }
            return List.copyOf(result);
        }
        if (found instanceof Map<?, ?>) {
            return List.of(OutputRecord.ofJson(map(found)));
        }
        return List.of(OutputRecord.ofJson(Map.of("value", found)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(final Object value) {
        return (Map<String, Object>) value;
    }

    private static HttpRequest nextPage(final RequestPlan plan,
                                        final HttpResult result) {
        if (Objects.isNull(plan.pagination()) || !plan.pagination().shouldPaginate(plan, result)) {
            return null;
        }
        return plan.pagination().nextRequest(plan, result);
    }

    private static HttpResult send(final HttpRequest request,
                                   final RequestPlan plan,
                                   final RequestContext ctx,
                                   final HttpTransport guarded,
                                   final CredentialStore store,
                                   final Authenticator auth) {
        final var authRequest = plan.authRequest();
        final HttpResult result;
        if (Objects.isNull(authRequest)) {
            result = guarded.send(request);
        } else {
            final var credential = resolve(store, authRequest, ctx);
            if (auth instanceof OAuth2RequestAuthenticator oauth) {
                result = oauth.retryOn401(credential, request, guarded);
            } else {
                final var out = new AuthResult();
                result = guarded.send(auth.apply(credential, request, out));
            }
        }
        feed(ctx, result);
        return result;
    }

    private static void feed(final RequestContext ctx,
                             final HttpResult result) {
        if (Objects.nonNull(ctx.onResponse())) {
            ctx.onResponse().accept(result);
        }
    }

    private static DecryptedCredential<?> resolve(final CredentialStore store,
                                                  final AuthRequest authRequest,
                                                  final RequestContext ctx) {
        return store.resolve(authRequest.ref(), ctx).orElseThrow(() ->
                HttpErrorFactory.of(HttpApiException.NO_STATUS, ctx.operationId(), -1,
                        "Credential '" + authRequest.ref().id() + "' could not be resolved"));
    }

    private static List<OutputRecord> cap(final List<OutputRecord> records,
                                          final int maxResults) {
        if (maxResults > 0 && records.size() > maxResults) {
            return List.copyOf(records.subList(0, maxResults));
        }
        return List.copyOf(records);
    }

    /**
     * Transport decorator applying the security guards around every send: the SSRF allow-list is
     * enforced before the delegate runs and any {@link HttpApiException} is re-raised redacted.
     */
    private static final class GuardedTransport implements HttpTransport {

        private final HttpTransport delegate;
        private final RequestContext ctx;

        GuardedTransport(final HttpTransport delegate,
                         final RequestContext ctx) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.ctx = Objects.requireNonNull(ctx, "ctx");
        }

        @Override
        public HttpResult send(final HttpRequest request) throws HttpApiException {
            if (!request.allowedDomains().isEmpty()) {
                SsrfGuard.validate(request.url(), request.allowedDomains(), request.allowIpLiteral());
            }
            try {
                return delegate.send(request);
            } catch (final HttpApiException e) {
                throw HttpErrorFactory.of(
                        e.getStatus(),
                        ctx.operationId(),
                        e.getItemIndex(),
                        SecretRedactor.get().redact(e.getMessage(), ctx.credentials()),
                        e
                );
            }
        }
    }
}
