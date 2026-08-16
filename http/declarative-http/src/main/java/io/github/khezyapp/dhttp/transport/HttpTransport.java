package io.github.khezyapp.dhttp.transport;

import io.github.khezyapp.dhttp.error.HttpApiException;

/**
 * SPI for sending a {@link HttpRequest} and receiving an {@link HttpResult}.
 *
 * <p>The core engine performs no network I/O itself: every HTTP send is delegated to an
 * implementation of this interface. Concrete adapters — such as the bundled
 * {@link io.github.khezyapp.dhttp.transport.jdk.JdkHttpTransport} over the JDK {@code HttpClient},
 * or a third-party OkHttp/Spring RestClient adapter — own the actual socket connection. The engine
 * only ever depends on this interface, so it stays transport-neutral and can be exercised with a
 * fake/in-memory implementation in tests.</p>
 *
 * <p>Most adapters should extend {@link AbstractHttpTransport} instead of implementing this
 * interface directly: it owns the shared request pipeline (query serialization, redirects with SSRF
 * re-validation and credential stripping, body/header preparation, status mapping) and only asks the
 * subclass to send one request and return the raw wire response.</p>
 */
@FunctionalInterface
public interface HttpTransport {

    HttpResult send(HttpRequest request) throws HttpApiException;
}
