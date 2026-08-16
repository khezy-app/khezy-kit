package io.github.khezyapp.dhttp.transport;

import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.security.RedirectPolicy;
import io.github.khezyapp.dhttp.security.SsrfGuard;
import io.github.khezyapp.dhttp.spec.HttpMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Skeleton implementation of {@link HttpTransport} owning every client-agnostic step of an HTTP
 * exchange and delegating only the actual send to a concrete adapter.
 *
 * <p>The template method {@link #send(HttpRequest)} performs the full lifecycle in a fixed order:
 * SSRF allow-list validation, URL and query serialization per {@link ArrayFormat}, a manual redirect
 * loop that re-validates each hop against the allow-list and strips cross-origin credentials per
 * {@link RedirectPolicy}, and finally maps the raw wire response to an {@link HttpResult} or an
 * {@link HttpApiException} for non-2xx statuses. A subclass implements only
 * {@link #execute(HttpRequest, URI)}: build a native client request from the neutral
 * {@link HttpRequest}, send it, and return the raw status/headers/body as a {@link RawResponse}.
 * The subclass must not follow redirects or interpret status codes — those belong to the template,
 * which guarantees identical redirect, SSRF, and error semantics for every adapter.</p>
 *
 * <p>Shared building blocks are exposed for the subclass: {@link #prepareBody(Body)} turns any
 * {@link Body} into payload bytes plus a content type (multipart included),
 * {@link #effectiveHeaders(HttpRequest)} computes the outgoing headers including the Basic/Bearer
 * auth fallback and the JSON {@code Accept} header, and {@link #uriOf(HttpRequest)} serializes the
 * query string. Transport-level failures (connection refused, timeouts, interrupted I/O) are
 * reported as {@link HttpApiException} by the subclass while performing {@link #execute}.</p>
 */
public abstract class AbstractHttpTransport implements HttpTransport {

    /**
     * Hop-by-hop headers that must not be set explicitly: the JDK {@code HttpClient} rejects them
     * and other clients would let a caller corrupt framing.
     */
    protected static final Set<String> RESTRICTED_HEADERS = Set.of(
            "connection", "content-length", "expect", "host", "upgrade");

    /**
     * Headers stripped whens a redirect crosses origin boundaries.
     */
    protected static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "proxy-authorization");

    protected static final Charset UTF_8 = StandardCharsets.UTF_8;

    private static final RedirectPolicy REDIRECT_POLICY = new RedirectPolicy();

    @Override
    public final HttpResult send(final HttpRequest request) throws HttpApiException {
        Objects.requireNonNull(request, "request");
        if (!request.allowedDomains().isEmpty()) {
            SsrfGuard.validate(request.url(), request.allowedDomains(), request.allowIpLiteral());
        }
        var current = request;
        var uri = uriOf(current);
        for (var hop = 0; ; hop++) {
            final var response = execute(current, uri);
            if (isRedirect(response.status()) &&
                    !current.disableFollowRedirect()
                    && hop < current.maxRedirects()) {
                final var location = response.headers().first("location").orElse(null);
                if (Objects.nonNull(location)) {
                    final var next = uri.resolve(location);
                    if (!current.allowedDomains().isEmpty()) {
                        SsrfGuard.validate(next.toString(), current.allowedDomains(), current.allowIpLiteral());
                    }
                    final var strip = current.stripCrossOriginCredentials()
                            && REDIRECT_POLICY.shouldStripCredentials(uri.toString(), next.toString());
                    uri = next;
                    current = redirectRequest(current, next, response.status(), strip);
                    continue;
                }
            }
            return toResult(response, current);
        }
    }

    /**
     * Sends one request with the concrete HTTP client and returns the raw wire response.
     *
     * <p>Implementations map the neutral {@link HttpRequest} to their client's native request type,
     * send it, and wrap the native response into a {@link RawResponse}. They should not follow
     * redirects (the template does), and they should report transport-level failures — connection
     * errors, timeouts, interrupts — as {@link HttpApiException}.</p>
     *
     * @param request the neutral request (already carrying the resolved target URL in {@code uri})
     * @param uri     the full target URI including the serialized query string
     * @return the raw response for the template to interpret
     */
    protected abstract RawResponse execute(HttpRequest request, URI uri) throws HttpApiException;

    /**
     * Serializes the request into its full target URI, appending the query string per the request's
     * {@link ArrayFormat}.
     */
    protected static URI uriOf(final HttpRequest request) {
        final var query = queryString(request);
        final var url = request.url();
        final String full;
        if (query.isEmpty()) {
            full = url;
        } else if (url.contains("?")) {
            full = url + (url.endsWith("?") ? "" : "&") + query;
        } else {
            full = url + "?" + query;
        }
        return URI.create(full);
    }

    /**
     * Turns any {@link Body} into payload bytes plus a content type. Returns a body-less
     * {@link PreparedBody} for {@link Body.NoBody}.
     */
    protected static PreparedBody prepareBody(final Body body) {
        if (body instanceof Body.JsonBody json) {
            return new PreparedBody(json.json().getBytes(UTF_8), "application/json");
        }
        if (body instanceof Body.RawBody raw) {
            return new PreparedBody(raw.bytes(), raw.contentType());
        }
        if (body instanceof Body.BinaryBody binary) {
            return new PreparedBody(binary.bytes(), binary.contentType());
        }
        if (body instanceof Body.FormBody form) {
            final var multipart = multipart(form.fields());
            return new PreparedBody(multipart.bytes(),
                    "multipart/form-data; boundary=" + multipart.boundary());
        }
        if (body instanceof Body.UrlEncodedBody url) {
            return new PreparedBody(url.body().getBytes(UTF_8), "application/x-www-form-urlencoded");
        }
        return new PreparedBody(null, null);
    }

    /**
     * Computes the effective outgoing headers: the request's own headers plus the Basic/Bearer auth
     * fallback from {@code request.auth()} (unless an Authorization header already exists) and the
     * JSON {@code Accept} header whens requested.
     */
    protected static Headers effectiveHeaders(final HttpRequest request) {
        var headers = request.headers();
        final var auth = request.auth();
        if (!headers.contains("Authorization")) {
            if (auth instanceof Auth.BasicAuth basic) {
                final var token = Base64.getEncoder().encodeToString(
                        (basic.username() + ":" + basic.password()).getBytes(UTF_8));
                headers = headers.withAdded("Authorization", "Basic " + token);
            } else if (auth instanceof Auth.BearerAuth bearer) {
                headers = headers.withAdded("Authorization", "Bearer " + bearer.token());
            }
        }
        if (request.jsonAccept() && !headers.contains("Accept")) {
            headers = headers.withAdded("Accept", "application/json");
        }
        return headers;
    }

    /**
     * Extracts the host part of a {@code "scheme://host:port"} proxy string.
     */
    protected static String proxyHost(final String proxy) {
        final var rest = proxyRest(proxy);
        final var colon = rest.lastIndexOf(':');
        return colon < 0 ? rest : rest.substring(0, colon);
    }

    /**
     * Extracts the port part of a {@code "scheme://host:port"} proxy string.
     */
    protected static int proxyPort(final String proxy) {
        final var rest = proxyRest(proxy);
        final var colon = rest.lastIndexOf(':');
        if (colon < 0) {
            return 80;
        }
        try {
            return Integer.parseInt(rest.substring(colon + 1));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid proxy: " + proxy);
        }
    }

    private static String queryString(final HttpRequest request) {
        if (request.query().isEmpty()) {
            return "";
        }
        final var format = request.queryArrayFormat();
        final var pairs = new ArrayList<String>();
        for (final var entry : request.query().entrySet()) {
            addQueryPairs(pairs, format, encode(entry.getKey()), entry.getValue());
        }
        if (format == ArrayFormat.COMMA) {
            return commaJoined(pairs);
        }
        return String.join("&", pairs);
    }

    private static void addQueryPairs(final List<String> pairs,
                                      final ArrayFormat format,
                                      final String name,
                                      final Object value) {
        if (value instanceof List<?> list) {
            for (var i = 0; i < list.size(); i++) {
                pairs.add(formatPair(name, i, list.get(i), format));
            }
        } else if (Objects.nonNull(value) && value.getClass().isArray()) {
            final var length = Array.getLength(value);
            for (var i = 0; i < length; i++) {
                pairs.add(formatPair(name, i, Array.get(value, i), format));
            }
        } else {
            pairs.add(formatPair(name, 0, value, format));
        }
    }

    private static String formatPair(final String name,
                                     final int index,
                                     final Object value,
                                     final ArrayFormat format) {
        final var encoded = encode(String.valueOf(value));
        return switch (format) {
            case INDICES -> name + "[" + index + "]=" + encoded;
            case BRACKETS -> name + "[]=" + encoded;
            case COMMA, REPEAT -> name + "=" + encoded;
        };
    }

    private static String commaJoined(final List<String> pairs) {
        final var grouped = new LinkedHashMap<String, StringBuilder>();
        for (final String pair : pairs) {
            final var equals = pair.indexOf('=');
            grouped.computeIfAbsent(pair.substring(0, equals), key -> new StringBuilder())
                    .append(',')
                    .append(pair.substring(equals + 1));
        }
        final var joined = new ArrayList<String>();
        for (final var entry : grouped.entrySet()) {
            joined.add(entry.getKey() + "=" + entry.getValue().substring(1));
        }
        return String.join("&", joined);
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, UTF_8).replace("+", "%20");
    }

    private static Multipart multipart(final Map<String, ?> fields) {
        final var boundary = "----KhezyHttpBoundary"
                + Long.toHexString(ThreadLocalRandom.current().nextLong());
        final var out = new ByteArrayOutputStream();
        try {
            for (final var field : fields.entrySet()) {
                if (field.getValue() instanceof Body.FormBody.FilePart file) {
                    writeFilePart(out, boundary, field.getKey(), file);
                } else {
                    write(out, "--" + boundary + "\r\n");
                    write(out, "Content-Disposition: form-data; name=\""
                            + field.getKey() + "\"\r\n\r\n");
                    write(out, field.getValue() + "\r\n");
                }
            }
            write(out, "--" + boundary + "--\r\n");
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot build multipart body", e);
        }
        return new Multipart(boundary, out.toByteArray());
    }

    private static void writeFilePart(final ByteArrayOutputStream out,
                                      final String boundary,
                                      final String name,
                                      final Body.FormBody.FilePart file) throws IOException {
        write(out, "--" + boundary + "\r\n");
        write(out, "Content-Disposition: form-data; name=\"" + name + "\"");
        if (Objects.nonNull(file.fileName())) {
            write(out, "; filename=\"" + file.fileName() + "\"");
        }
        write(out, "\r\n");
        write(out, "Content-Type: "
                + (Objects.nonNull(file.contentType())
                        ? file.contentType() : "application/octet-stream")
                + "\r\n\r\n");
        out.write(file.bytes());
        write(out, "\r\n");
    }

    private static void write(final ByteArrayOutputStream out,
                              final String value) throws IOException {
        out.write(value.getBytes(UTF_8));
    }

    private static HttpResult toResult(final RawResponse response,
                                       final HttpRequest request) {
        final var status = response.status();
        final var ok = status >= 200 && status < 300
                || (request.disableFollowRedirect() && isRedirect(status))
                || request.ignoreStatusErrors().contains(status);
        if (ok) {
            final var headers = new LinkedHashMap<String, List<String>>();
            for (final var entry : response.headers().asMap().entrySet()) {
                headers.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
            final var body = response.body();
            return new HttpResult(status, headers, body, decode(body, response));
        }
        throw new HttpApiException(status, "transport", -1,
                "HTTP " + status + " response for " + request.method() + " " + pathOnly(request.url()));
    }

    private static String decode(final byte[] body,
                                 final RawResponse response) {
        if (Objects.isNull(body) || body.length == 0) {
            return "";
        }
        return new String(body, charsetOf(response));
    }

    private static Charset charsetOf(final RawResponse response) {
        final var contentType = response.headers().first("Content-Type").orElse("");
        final var marker = "charset=";
        final var index = contentType.toLowerCase(Locale.ROOT).indexOf(marker);
        if (index < 0) {
            return UTF_8;
        }
        try {
            return Charset.forName(contentType.substring(index + marker.length()).strip());
        } catch (final RuntimeException e) {
            return UTF_8;
        }
    }

    private static String pathOnly(final String url) {
        final var index = url.indexOf('?');
        return index < 0 ? url : url.substring(0, index);
    }

    private static boolean isRedirect(final int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static HttpRequest redirectRequest(final HttpRequest current,
                                               final URI target,
                                               final int status,
                                               final boolean stripCredentials) {
        final var builder = HttpRequest.builder()
                .url(target.toString())
                .baseUrl(current.baseUrl())
                .method(methodFor(status, current.method()))
                .headers(stripCredentials ? stripSensitive(current.headers()) : current.headers())
                .auth(stripCredentials ? new Auth.NoAuth() : current.auth())
                .queryArrayFormat(current.queryArrayFormat())
                .timeout(current.timeoutMillis())
                .skipSsl(current.skipSsl())
                .maxRedirects(current.maxRedirects())
                .disableFollowRedirect(current.disableFollowRedirect())
                .jsonAccept(current.jsonAccept())
                .returnFullResponse(current.returnFullResponse());
        if (Objects.nonNull(current.proxy())) {
            builder.proxy(current.proxy());
        }
        if (status != 303) {
            builder.body(current.body());
        } else {
            builder.body(new Body.NoBody());
        }
        for (final int ignored : current.ignoreStatusErrors()) {
            builder.ignoreStatusError(ignored);
        }
        for (final String domain : current.allowedDomains()) {
            builder.allowedDomain(domain);
        }
        builder.allowIpLiteral(current.allowIpLiteral())
                .stripCrossOriginCredentials(current.stripCrossOriginCredentials())
                .tlsConfig(current.tlsConfig());
        return builder.build();
    }

    private static HttpMethod methodFor(final int status,
                                        final HttpMethod method) {
        if (status == 303) {
            return HttpMethod.GET;
        }
        return method;
    }

    private static Headers stripSensitive(final Headers headers) {
        final var map = new LinkedHashMap<String, List<String>>();
        for (final var entry : headers.asMap().entrySet()) {
            if (!SENSITIVE_HEADERS.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return Headers.of(map);
    }

    private static String proxyRest(final String proxy) {
        final var scheme = proxy.indexOf("://");
        return scheme < 0 ? proxy : proxy.substring(scheme + 3);
    }

    /**
     * A serialized request body: the payload bytes plus the content type to send, or a body-less
     * carrier (both {@code null}) for requests without a body.
     */
    protected record PreparedBody(byte[] bytes,
                                  String contentType) {

        /**
         * @return true whens this carrier holds a real payload
         */
        public boolean hasBody() {
            return Objects.nonNull(bytes);
        }
    }

    private record Multipart(String boundary,
                             byte[] bytes) {
    }
}
