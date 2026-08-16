package io.github.khezyapp.dhttp.transport;

import io.github.khezyapp.cert.ClientTlsConfig;
import io.github.khezyapp.dhttp.spec.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for {@link HttpRequest}. Requires {@code url} and {@code method}; everything else
 * has a safe default.
 */
public final class HttpRequestBuilder {

    private static final long DEFAULT_TIMEOUT_MILLIS = 30_000L;
    private static final int DEFAULT_MAX_REDIRECTS = 5;

    private String url;
    private String baseUrl;
    private HttpMethod method;
    private Headers headers = Headers.of();
    private final Map<String, Object> query = new LinkedHashMap<>();
    private ArrayFormat queryArrayFormat = ArrayFormat.REPEAT;
    private Body body = new Body.NoBody();
    private Auth auth = new Auth.NoAuth();
    private String proxy;
    private long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
    private boolean skipSsl;
    private int maxRedirects = DEFAULT_MAX_REDIRECTS;
    private boolean disableFollowRedirect;
    private String encoding;
    private boolean jsonAccept;
    private final List<Integer> ignoreStatusErrors = new ArrayList<>();
    private String abortSignal;
    private final List<String> allowedDomains = new ArrayList<>();
    private boolean allowIpLiteral;
    private boolean stripCrossOriginCredentials = true;
    private boolean returnFullResponse;
    private ClientTlsConfig tlsConfig;

    HttpRequestBuilder() {
    }

    public HttpRequestBuilder url(final String url) {
        this.url = url;
        return this;
    }

    public HttpRequestBuilder baseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public HttpRequestBuilder method(final HttpMethod method) {
        this.method = method;
        return this;
    }

    public HttpRequestBuilder header(final String name,
                                     final String value) {
        headers = headers.withAdded(name, value);
        return this;
    }

    public HttpRequestBuilder headers(final Map<String, String> headers) {
        final var multi = new LinkedHashMap<String, List<String>>();
        for (final Map.Entry<String, String> e : headers.entrySet()) {
            multi.put(e.getKey(), List.of(e.getValue()));
        }
        this.headers = Headers.of(multi);
        return this;
    }

    public HttpRequestBuilder headers(final Headers headers) {
        this.headers = Objects.requireNonNull(headers, "headers");
        return this;
    }

    public HttpRequestBuilder query(final String name,
                                    final Object value) {
        query.put(name, value);
        return this;
    }

    public HttpRequestBuilder clearQuery() {
        query.clear();
        return this;
    }

    public HttpRequestBuilder queryArrayFormat(final ArrayFormat queryArrayFormat) {
        this.queryArrayFormat = Objects.requireNonNull(queryArrayFormat, "queryArrayFormat");
        return this;
    }

    public HttpRequestBuilder body(final Body body) {
        this.body = Objects.requireNonNull(body, "body");
        return this;
    }

    public HttpRequestBuilder auth(final Auth auth) {
        this.auth = Objects.requireNonNull(auth, "auth");
        return this;
    }

    public HttpRequestBuilder proxy(final String proxy) {
        this.proxy = proxy;
        return this;
    }

    public HttpRequestBuilder timeout(final long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public HttpRequestBuilder skipSsl(final boolean skipSsl) {
        this.skipSsl = skipSsl;
        return this;
    }

    public HttpRequestBuilder maxRedirects(final int maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    public HttpRequestBuilder disableFollowRedirect(final boolean disableFollowRedirect) {
        this.disableFollowRedirect = disableFollowRedirect;
        return this;
    }

    public HttpRequestBuilder encoding(final String encoding) {
        this.encoding = encoding;
        return this;
    }

    public HttpRequestBuilder jsonAccept(final boolean jsonAccept) {
        this.jsonAccept = jsonAccept;
        return this;
    }

    public HttpRequestBuilder ignoreStatusError(final int status) {
        ignoreStatusErrors.add(status);
        return this;
    }

    public HttpRequestBuilder allowedDomain(final String domain) {
        allowedDomains.add(domain);
        return this;
    }

    public HttpRequestBuilder allowIpLiteral(final boolean allowIpLiteral) {
        this.allowIpLiteral = allowIpLiteral;
        return this;
    }

    public HttpRequestBuilder stripCrossOriginCredentials(final boolean stripCrossOriginCredentials) {
        this.stripCrossOriginCredentials = stripCrossOriginCredentials;
        return this;
    }

    public HttpRequestBuilder abortSignal(final String abortSignal) {
        this.abortSignal = abortSignal;
        return this;
    }

    public HttpRequestBuilder returnFullResponse(final boolean returnFullResponse) {
        this.returnFullResponse = returnFullResponse;
        return this;
    }

    /**
     * @param tlsConfig the mTLS client identity to present on this request, or {@code null} for
     *                  none
     * @return this builder
     */
    public HttpRequestBuilder tlsConfig(final ClientTlsConfig tlsConfig) {
        this.tlsConfig = tlsConfig;
        return this;
    }

    public HttpRequest build() {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("url is required");
        }
        if (method == null) {
            throw new IllegalStateException("method is required");
        }
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis must be non-negative");
        }
        if (maxRedirects < 0) {
            throw new IllegalArgumentException("maxRedirects must be non-negative");
        }
        return new HttpRequest(this);
    }

    String url() {
        return url;
    }

    String baseUrl() {
        return baseUrl;
    }

    HttpMethod method() {
        return method;
    }

    Headers headers() {
        return headers;
    }

    Map<String, Object> query() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(query));
    }

    ArrayFormat queryArrayFormat() {
        return queryArrayFormat;
    }

    Body body() {
        return body;
    }

    Auth auth() {
        return auth;
    }

    String proxy() {
        return proxy;
    }

    long timeoutMillis() {
        return timeoutMillis;
    }

    boolean skipSsl() {
        return skipSsl;
    }

    int maxRedirects() {
        return maxRedirects;
    }

    boolean disableFollowRedirect() {
        return disableFollowRedirect;
    }

    String encoding() {
        return encoding;
    }

    boolean jsonAccept() {
        return jsonAccept;
    }

    List<Integer> ignoreStatusErrors() {
        return List.copyOf(ignoreStatusErrors);
    }

    String abortSignal() {
        return abortSignal;
    }

    List<String> allowedDomains() {
        return List.copyOf(allowedDomains);
    }

    boolean allowIpLiteral() {
        return allowIpLiteral;
    }

    boolean stripCrossOriginCredentials() {
        return stripCrossOriginCredentials;
    }

    boolean returnFullResponse() {
        return returnFullResponse;
    }

    ClientTlsConfig tlsConfig() {
        return tlsConfig;
    }
}
