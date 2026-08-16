package io.github.khezyapp.dhttp.transport;

import io.github.khezyapp.cert.ClientTlsConfig;
import io.github.khezyapp.dhttp.spec.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * Transport-neutral request value object mirroring {@code IHttpRequestOptions} (R11). Immutable; use
 * {@link #builder()} to construct.
 */
public final class HttpRequest {

    private final String url;
    private final String baseUrl;
    private final HttpMethod method;
    private final Headers headers;
    private final Map<String, Object> query;
    private final ArrayFormat queryArrayFormat;
    private final Body body;
    private final Auth auth;
    private final String proxy;
    private final long timeoutMillis;
    private final boolean skipSsl;
    private final int maxRedirects;
    private final boolean disableFollowRedirect;
    private final String encoding;
    private final boolean jsonAccept;
    private final List<Integer> ignoreStatusErrors;
    private final String abortSignal;
    private final List<String> allowedDomains;
    private final boolean allowIpLiteral;
    private final boolean stripCrossOriginCredentials;
    private final boolean returnFullResponse;
    private final ClientTlsConfig tlsConfig;

    HttpRequest(final HttpRequestBuilder builder) {
        this.url = builder.url();
        this.baseUrl = builder.baseUrl();
        this.method = builder.method();
        this.headers = builder.headers();
        this.query = builder.query();
        this.queryArrayFormat = builder.queryArrayFormat();
        this.body = builder.body();
        this.auth = builder.auth();
        this.proxy = builder.proxy();
        this.timeoutMillis = builder.timeoutMillis();
        this.skipSsl = builder.skipSsl();
        this.maxRedirects = builder.maxRedirects();
        this.disableFollowRedirect = builder.disableFollowRedirect();
        this.encoding = builder.encoding();
        this.jsonAccept = builder.jsonAccept();
        this.ignoreStatusErrors = builder.ignoreStatusErrors();
        this.abortSignal = builder.abortSignal();
        this.allowedDomains = builder.allowedDomains();
        this.allowIpLiteral = builder.allowIpLiteral();
        this.stripCrossOriginCredentials = builder.stripCrossOriginCredentials();
        this.returnFullResponse = builder.returnFullResponse();
        this.tlsConfig = builder.tlsConfig();
    }

    public static HttpRequestBuilder builder() {
        return new HttpRequestBuilder();
    }

    /**
     * @return a builder pre-filled with this request's values
     */
    public HttpRequestBuilder toBuilder() {
        final var b = HttpRequest.builder();
        b.url(url).baseUrl(baseUrl).method(method).headers(headers);
        b.queryArrayFormat(queryArrayFormat).body(body).auth(auth).proxy(proxy);
        b.timeout(timeoutMillis).skipSsl(skipSsl).maxRedirects(maxRedirects);
        b.disableFollowRedirect(disableFollowRedirect).encoding(encoding).jsonAccept(jsonAccept);
        b.abortSignal(abortSignal).returnFullResponse(returnFullResponse);
        b.allowIpLiteral(allowIpLiteral).stripCrossOriginCredentials(stripCrossOriginCredentials);
        b.tlsConfig(tlsConfig);
        for (final var status : ignoreStatusErrors) {
            b.ignoreStatusError(status);
        }
        for (final var domain : allowedDomains) {
            b.allowedDomain(domain);
        }
        for (final var q : query.entrySet()) {
            b.query(q.getKey(), q.getValue());
        }
        return b;
    }

    public String url() {
        return url;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public HttpMethod method() {
        return method;
    }

    public Headers headers() {
        return headers;
    }

    public Map<String, Object> query() {
        return query;
    }

    public ArrayFormat queryArrayFormat() {
        return queryArrayFormat;
    }

    public Body body() {
        return body;
    }

    public Auth auth() {
        return auth;
    }

    public String proxy() {
        return proxy;
    }

    public long timeoutMillis() {
        return timeoutMillis;
    }

    public boolean skipSsl() {
        return skipSsl;
    }

    public int maxRedirects() {
        return maxRedirects;
    }

    public boolean disableFollowRedirect() {
        return disableFollowRedirect;
    }

    public String encoding() {
        return encoding;
    }

    public boolean jsonAccept() {
        return jsonAccept;
    }

    public List<Integer> ignoreStatusErrors() {
        return ignoreStatusErrors;
    }

    public String abortSignal() {
        return abortSignal;
    }

    public List<String> allowedDomains() {
        return allowedDomains;
    }

    /**
     * @return whens {@code true}, raw IP literals bypass the SSRF allow-list
     */
    public boolean allowIpLiteral() {
        return allowIpLiteral;
    }

    /**
     * @return whens {@code true}, credentials are not forwarded to a cross-origin redirect target
     */
    public boolean stripCrossOriginCredentials() {
        return stripCrossOriginCredentials;
    }

    public boolean returnFullResponse() {
        return returnFullResponse;
    }

    /**
     * @return the mTLS client identity to present, or {@code null} whens none is configured
     */
    public ClientTlsConfig tlsConfig() {
        return tlsConfig;
    }

    @Override
    public String toString() {
        final var bodyKind = body == null ? "NONE" : body.kind();
        return "HttpRequest{url='" + url + "', method=" + method + ", body=" + bodyKind
                + ", returnFullResponse=" + returnFullResponse + '}';
    }
}
