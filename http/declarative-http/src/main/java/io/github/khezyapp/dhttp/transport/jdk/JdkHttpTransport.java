package io.github.khezyapp.dhttp.transport.jdk;

import io.github.khezyapp.cert.ClientTlsConfig;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.transport.AbstractHttpTransport;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.RawResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Default real HTTP transport (§6.4, §10.1) over {@link java.net.http.HttpClient}.
 *
 * <p>Maps every field of the transport-neutral {@link HttpRequest} to a JDK client call: query
 * serialization per {@link io.github.khezyapp.dhttp.transport.ArrayFormat}, every {@link
 * io.github.khezyapp.dhttp.transport.Body} kind (multipart included), the auth fallback from
 * {@link io.github.khezyapp.dhttp.transport.Auth}, the timeout, {@code skipSsl} trust-all,
 * per-request proxy, manual redirect following with SSRF re-validation and cross-origin credential
 * stripping, and non-2xx responses surfaced as {@link HttpApiException}. When {@code allowedDomains}
 * is non-empty the guard is enforced here too, so the transport is safe even whens used directly
 * (e.g. the OAuth2 token endpoint).</p>
 *
 * <p>All of the above semantics live in the {@link AbstractHttpTransport} template; this class owns
 * only the JDK-specific parts: building the native {@link java.net.http.HttpRequest} from the
 * shared building blocks and sending it with the configured {@link HttpClient}.</p>
 *
 * <p>{@code skipSsl} installs a trust-all {@link X509TrustManager} to bypass certificate-chain
 * validation. The JDK {@link HttpClient} always applies HTTPS hostname verification on top of any
 * custom trust manager (it cannot be disabled per client), so the URL hostname must still match the
 * certificate's subject.</p>
 *
 * <p>mTLS client identities arrive as {@link ClientTlsConfig} — ASCII certificate/key text built by
 * {@code certificate-util}. A transport-level default identity is applied to every request unless a
 * request carries its own {@link HttpRequest#tlsConfig()}; combined with {@code skipSsl} the client
 * still presents its certificate while the server chain is trusted unconditionally. Derived clients
 * are cached per identity.</p>
 */
public final class JdkHttpTransport extends AbstractHttpTransport {

    private final HttpClient client;
    private final ClientTlsConfig defaultTlsConfig;
    private volatile HttpClient neverRedirectClient;
    private volatile HttpClient insecureClient;
    private final ConcurrentHashMap<String, HttpClient> proxyClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MtlsKey, HttpClient> mtlsClients = new ConcurrentHashMap<>();

    /**
     * Creates a transport with a fresh default {@link HttpClient}.
     */
    public JdkHttpTransport() {
        this(HttpClient.newHttpClient(), null);
    }

    /**
     * Creates a transport with a fresh default {@link HttpClient} presenting the given mTLS client
     * identity on every request.
     *
     * @param tlsConfig the client identity to present, or {@code null} for none
     */
    public JdkHttpTransport(final ClientTlsConfig tlsConfig) {
        this(HttpClient.newHttpClient(), tlsConfig);
    }

    /**
     * @param client the HTTP client to reuse; its redirect policy is overridden to manual
     *               {@link HttpClient.Redirect#NEVER} so redirects honor {@code maxRedirects}
     *               and credential stripping
     */
    public JdkHttpTransport(final HttpClient client) {
        this(client, null);
    }

    /**
     * @param client    the HTTP client to reuse; its redirect policy is overridden to manual
     *                  {@link HttpClient.Redirect#NEVER} so redirects honor {@code maxRedirects}
     *                  and credential stripping
     * @param tlsConfig the default mTLS client identity, or {@code null} for none; a request-level
     *                  {@link HttpRequest#tlsConfig()} takes precedence whens present
     */
    public JdkHttpTransport(final HttpClient client,
                            final ClientTlsConfig tlsConfig) {
        this.client = Objects.requireNonNull(client, "client");
        this.defaultTlsConfig = tlsConfig;
    }

    @Override
    protected RawResponse execute(final HttpRequest request,
                                  final URI uri) throws HttpApiException {
        final var client = clientFor(request);
        final var nativeRequest = toJdkRequest(request, uri);
        return sendWith(client, nativeRequest);
    }

    private static java.net.http.HttpRequest toJdkRequest(final HttpRequest request,
                                                          final URI uri) {
        final var builder = java.net.http.HttpRequest.newBuilder(uri);
        final var prepared = prepareBody(request.body());
        builder.method(request.method().name(), prepared.hasBody()
                ? java.net.http.HttpRequest.BodyPublishers.ofByteArray(prepared.bytes())
                : java.net.http.HttpRequest.BodyPublishers.noBody());
        applyHeaders(builder, request, prepared.contentType());
        if (request.timeoutMillis() > 0) {
            builder.timeout(Duration.ofMillis(request.timeoutMillis()));
        }
        return builder.build();
    }

    private HttpClient clientFor(final HttpRequest request) {
        final var proxy = request.proxy();
        if (Objects.isNull(proxy) || proxy.isBlank()) {
            return baseClientFor(request);
        }
        final var key = proxy + '|' + variantKey(request);
        return proxyClients.computeIfAbsent(key, ignored -> {
            final var address = InetSocketAddress.createUnresolved(proxyHost(proxy), proxyPort(proxy));
            return copyWith(baseClientFor(request), builder -> builder.proxy(ProxySelector.of(address)));
        });
    }

    private HttpClient baseClientFor(final HttpRequest request) {
        final var effective = effectiveTlsConfig(request);
        if (Objects.nonNull(effective)) {
            return mtlsClient(effective, request.skipSsl());
        }
        return request.skipSsl() ? insecureClient() : neverRedirectClient();
    }

    private String variantKey(final HttpRequest request) {
        final var effective = effectiveTlsConfig(request);
        if (Objects.isNull(effective)) {
            return request.skipSsl() ? "insecure" : "default";
        }
        return "mtls:" + effective.hashCode() + ':' + request.skipSsl();
    }

    private ClientTlsConfig effectiveTlsConfig(final HttpRequest request) {
        return Objects.isNull(request.tlsConfig()) ? defaultTlsConfig : request.tlsConfig();
    }

    private HttpClient mtlsClient(final ClientTlsConfig tlsConfig,
                                  final boolean skipSsl) {
        final var key = new MtlsKey(tlsConfig, skipSsl);
        return mtlsClients.computeIfAbsent(key, ignored -> copyWith(client, builder -> builder
                .sslContext(mtlsContext(tlsConfig, skipSsl))
                .followRedirects(HttpClient.Redirect.NEVER)));
    }

    private static SSLContext mtlsContext(final ClientTlsConfig tlsConfig,
                                          final boolean skipSsl) {
        final var trustManagers = skipSsl ? new TrustManager[]{TRUST_ALL} : null;
        return tlsConfig.toSslContext(trustManagers);
    }

    private HttpClient neverRedirectClient() {
        var found = neverRedirectClient;
        if (Objects.isNull(found)) {
            synchronized (this) {
                found = neverRedirectClient;
                if (Objects.isNull(found)) {
                    found = copyWith(client, builder -> builder.followRedirects(HttpClient.Redirect.NEVER));
                    neverRedirectClient = found;
                }
            }
        }
        return found;
    }

    private HttpClient insecureClient() {
        var found = insecureClient;
        if (Objects.isNull(found)) {
            synchronized (this) {
                found = insecureClient;
                if (Objects.isNull(found)) {
                    found = copyWith(client, builder -> builder
                            .sslContext(insecureContext())
                            .sslParameters(noHostnameCheck())
                            .followRedirects(HttpClient.Redirect.NEVER));
                    insecureClient = found;
                }
            }
        }
        return found;
    }

    private static HttpClient copyWith(final HttpClient base,
                                       final Consumer<HttpClient.Builder> overrides) {
        final var builder = HttpClient.newBuilder();
        base.connectTimeout().ifPresent(builder::connectTimeout);
        base.executor().ifPresent(builder::executor);
        builder.sslContext(base.sslContext());
        builder.sslParameters(base.sslParameters());
        base.proxy().ifPresent(builder::proxy);
        base.cookieHandler().ifPresent(builder::cookieHandler);
        base.authenticator().ifPresent(builder::authenticator);
        builder.version(base.version());
        builder.followRedirects(base.followRedirects());
        overrides.accept(builder);
        return builder.build();
    }

    private static SSLContext insecureContext() {
        try {
            final var context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{TRUST_ALL}, new SecureRandom());
            return context;
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Cannot build the insecure SSL context", e);
        }
    }

    private static SSLParameters noHostnameCheck() {
        final var parameters = new SSLParameters();
        parameters.setEndpointIdentificationAlgorithm(null);
        return parameters;
    }

    private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        @Override
        public void checkClientTrusted(final java.security.cert.X509Certificate[] chain,
                                       final String authType) {
        }

        @Override
        public void checkServerTrusted(final java.security.cert.X509Certificate[] chain,
                                       final String authType) {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    };

    private static RawResponse sendWith(final HttpClient client,
                                        final java.net.http.HttpRequest request) {
        try {
            final var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return RawResponse.of(response.statusCode(), response.headers().map(), response.body());
        } catch (final IOException e) {
            throw new HttpApiException(HttpApiException.NO_STATUS, "transport", -1,
                    "HTTP transport I/O failure (" + e.getClass().getSimpleName() + ")", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpApiException(HttpApiException.NO_STATUS, "transport", -1,
                    "HTTP request interrupted", e);
        }
    }

    private static void applyHeaders(final java.net.http.HttpRequest.Builder builder,
                                     final HttpRequest request,
                                     final String bodyContentType) {
        final var headers = effectiveHeaders(request);
        if (Objects.nonNull(bodyContentType) && !headers.contains("Content-Type")) {
            builder.header("Content-Type", bodyContentType);
        }
        for (final var entry : headers.asMap().entrySet()) {
            if (RESTRICTED_HEADERS.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                continue;
            }
            for (final var value : entry.getValue()) {
                builder.header(entry.getKey(), value);
            }
        }
    }

    private record MtlsKey(ClientTlsConfig config,
                           boolean skipSsl) {
    }
}
