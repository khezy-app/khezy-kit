package io.github.khezyapp.dhttp.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.config.DeclarativeHttp;
import io.github.khezyapp.dhttp.config.DeclarativeHttpConfig;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.spec.BatchingSpec;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * §10.2 acceptance: batching throttle on multi-item execution — one request per item, paced by
 * {@code batchSize}/{@code batchIntervalMillis} exactly like n8n's V3 batching.
 */
class BatchingAcceptanceTest {

    @Test
    @DisplayName("Batching paces the loop: one request per item with a pause before each batch")
    void throttlePacesBetweenBatches() {
        final var transport = new TimingTransport();
        final var http = facade(transport);
        final var spec = spec(new BatchingSpec(2, 120));

        final var records = http.executeAll(spec, contexts(4));

        assertEquals(4, records.size());
        assertEquals(4, transport.requestCount());
        assertEquals(1, transport.pauses());
        assertTrue(transport.minPauseMillis() >= 100);
    }

    @Test
    @DisplayName("Without a batching spec items run back to back with no pauses")
    void noBatchingRunsSequentially() {
        final var transport = new TimingTransport();
        final var http = facade(transport);
        final var spec = spec(null);

        final var records = http.executeAll(spec, contexts(3));

        assertEquals(3, records.size());
        assertEquals(3, transport.requestCount());
        assertEquals(0, transport.pauses());
    }

    @Test
    @DisplayName("A single item is one batch: no pause and one request")
    void singleItemIsOneBatch() {
        final var transport = new TimingTransport();
        final var http = facade(transport);
        final var spec = spec(new BatchingSpec(5, 200));

        final var records = http.executeAll(spec, contexts(1));

        assertEquals(1, records.size());
        assertEquals(1, transport.requestCount());
        assertEquals(0, transport.pauses());
    }

    @Test
    @DisplayName("The accumulated records are capped by the active operation maxResults")
    void capsAccumulatedRecords() {
        final var transport = new TimingTransport();
        final var http = facade(transport);
        final var operation = new Operation("echo", new Route(
                new RequestShape(HttpMethod.GET, "/echo", Map.of(), Map.of(), null, null),
                List.of(), new Output(2, List.of(new PostReceive.RootProperty("data"))), null,
                List.of()));
        final var spec = new HttpRequestSpec("http://127.0.0.1", Map.of(), 30_000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults(),
                new BatchingSpec(1, 0));

        final var records = http.executeAll(spec, contexts(3));

        assertEquals(2, records.size());
        assertEquals(3, transport.requestCount());
    }

    @Test
    @DisplayName("BatchingSpec rejects a zero batch size and negative intervals")
    void batchingSpecValidation() {
        assertThrows(IllegalArgumentException.class, () -> new BatchingSpec(0, 100));
        assertThrows(IllegalArgumentException.class, () -> new BatchingSpec(2, -1));
    }

    @Test
    @DisplayName("An interrupt during a pacing pause surfaces as an HttpApiException")
    void interruptedPauseThrows() throws Exception {
        final var transport = new TimingTransport();
        final var http = facade(transport);
        final var spec = spec(new BatchingSpec(1, 10_000));
        final var thrown = new AtomicReference<Throwable>();
        final var thread = new Thread(() -> {
            try {
                http.executeAll(spec, contexts(2));
            } catch (final Throwable t) {
                thrown.set(t);
            }
        });
        thread.start();
        Thread.sleep(200);
        thread.interrupt();
        thread.join(5_000);

        assertFalse(thread.isAlive());
        assertInstanceOf(HttpApiException.class, thrown.get());
    }

    private static DeclarativeHttp facade(final HttpTransport transport) {
        final var config = DeclarativeHttpConfig.builder()
                .transport(transport)
                .keyProvider(BatchingAcceptanceTest::newKey)
                .build();
        return DeclarativeHttp.create(config);
    }

    private static HttpRequestSpec spec(final BatchingSpec batching) {
        final var operation = new Operation("echo", new Route(
                new RequestShape(HttpMethod.GET, "/echo", Map.of(), Map.of(), null, null),
                List.of(), new Output(100, List.of(new PostReceive.RootProperty("data"))), null,
                List.of()));
        return new HttpRequestSpec("http://127.0.0.1", Map.of(), 30_000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults(), batching);
    }

    private static List<RequestContext> contexts(final int count) {
        final var result = new ArrayList<RequestContext>();
        for (int i = 0; i < count; i++) {
            result.add(new RequestContext("echo", Map.of("index", i)));
        }
        return result;
    }

    private static SecretKey newKey() {
        try {
            final var generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (final java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("AES unavailable", e);
        }
    }
    private static final class TimingTransport implements HttpTransport {

        private static final long PAUSE_THRESHOLD_NANOS = 50_000_000L;

        private final AtomicLong previous = new AtomicLong(System.nanoTime());
        private final AtomicLong minPause = new AtomicLong(Long.MAX_VALUE);
        private int requestCount;
        private int pauses;

        @Override
        public HttpResult send(final HttpRequest request) {
            final var now = System.nanoTime();
            final var elapsed = now - previous.getAndSet(now);
            if (requestCount > 0 && elapsed >= PAUSE_THRESHOLD_NANOS) {
                pauses++;
                minPause.accumulateAndGet(elapsed, Math::min);
            }
            requestCount++;
            return HttpResult.of(200, "{\"data\":[{\"item\":1}]}");
        }

        private int requestCount() {
            return requestCount;
        }

        private int pauses() {
            return pauses;
        }

        private long minPauseMillis() {
            final var value = minPause.get();
            return value == Long.MAX_VALUE ? 0 : value / 1_000_000;
        }
    }
}
