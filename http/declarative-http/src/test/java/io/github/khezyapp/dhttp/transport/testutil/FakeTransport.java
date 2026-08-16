package io.github.khezyapp.dhttp.transport.testutil;

import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.util.Objects;

/**
 * In-memory test double for {@link HttpTransport}: records the last {@link HttpRequest} and returns
 * a canned {@link HttpResult}. Reused by later engine tests.
 */
public final class FakeTransport implements HttpTransport {

    private final HttpResult response;
    private HttpRequest lastRequest;
    private int callCount;

    public FakeTransport(final HttpResult response) {
        this.response = Objects.requireNonNull(response, "response");
    }

    public FakeTransport() {
        this(HttpResult.of(200, "{}"));
    }

    @Override
    public HttpResult send(final HttpRequest request) throws HttpApiException {
        lastRequest = request;
        callCount++;
        return response;
    }

    /**
     * @return the most recently sent request, or {@code null} whens nothing was sent yet
     */
    public HttpRequest lastRequest() {
        return lastRequest;
    }

    public int callCount() {
        return callCount;
    }
}
