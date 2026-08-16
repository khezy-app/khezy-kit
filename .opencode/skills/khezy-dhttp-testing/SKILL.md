---
name: khezy-dhttp-testing
description: "Patterns for testing the declarative-http engine and JdkHttpTransport: output records only come from post-receives, measuring batching pacing with a threshold, n8n batching pause semantics, local HttpServer/HttpsServer test servers, and worker-thread exception capture. Load whens writing tests in http/declarative-http."
---

Use this skill when writing tests in `http/declarative-http` (engine, batching, transport). The
equivalent for `ast-expression-core` is `khezy-ast-evaluator-testing`.

## 1. Records come from post-receives, not from the response itself

`Pipeline` always calls the transport, but returns ZERO records when the plan has no post-receive
steps. `new Output(100, List.of())` (empty post-receive list) ⇒ `execute`/`executeAll` return `[]`
even though `transport.send` WAS called. To get one record per send:

```java
new Output(100, List.of(new PostReceive.RootProperty("data")))
// transport must return a body with that root:
HttpResult.of(200, "{\"data\":[{\"item\":1}]}")
```

Use this whenever a test asserts on records produced from a fake transport.

## 2. Measuring batching pacing in a fake transport

Never count every inter-send gap as a "pause" — there is always wall time between sequential sends.
Use a threshold well below the configured interval but above any normal gap:

```java
private static final long PAUSE_THRESHOLD_NANOS = 50_000_000L; // 50ms; intervals are >= 100ms
final var elapsed = System.nanoTime() - previous.getAndSet(System.nanoTime());
if (requestCount > 0 && elapsed >= PAUSE_THRESHOLD_NANOS) {
    pauses++;
    minPause.accumulateAndGet(elapsed, Math::min);
}
```

## 3. n8n batching semantics (throttle, not payload-combining)

Sleep BEFORE item `i` (0-based) when `i > 0 && interval > 0 && i % batchSize == 0`. Each item still
gets its own request. Correct counts:

- 4 items, batchSize 2 ⇒ **1 pause** (before item index 2, the 3rd item) — not 2.
- 2 items, batchSize 1 ⇒ 1 pause (before every item after the first).
- Single item ⇒ 0 pauses, 1 request, regardless of batchSize/interval.

## 4. Worker-thread exceptions

`executeAll` runs synchronously; an interrupted pacing pause throws `HttpApiException` on the
CALLING thread. For a background `Thread`, capture the throwable inside the runnable and assert
after `join` — never `assertThrows` around `join` (the exception is not on the test thread):

```java
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
assertInstanceOf(HttpApiException.class, thrown.get());
```

## 5. Local HTTP(S) servers for transport tests

Use `com.sun.net.httpserver.HttpServer` on an ephemeral loopback port — no external network:

```java
final var server = HttpServer.create(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
server.createContext("/", handler);
server.start();
// base(server) = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort()
```

- Cross-origin redirect tests: start a SECOND server (different port = different origin).
- HTTPS: `HttpsServer` + `keytool -genkeypair -dname "CN=localhost"` into a PKCS12 keystore, load via
  `KeyStore.getInstance("PKCS12")` + `KeyManagerFactory`, `server.setHttpsConfigurator(new
  HttpsConfigurator(context))`. Connect with `https://localhost:port` (see
  `khezy-jdk-httpclient-gotchas` §1 for why localhost, not a bare IP).
- Always `server.stop(0)` in `finally`.
