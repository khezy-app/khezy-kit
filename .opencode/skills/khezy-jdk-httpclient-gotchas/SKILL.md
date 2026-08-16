---
name: khezy-jdk-httpclient-gotchas
description: "Non-obvious java.net.http.HttpClient behaviors that cause silent or surprising failures: a trust-all SSLContext still enforces hostname verification, client.newBuilder() silently calls the static factory and inherits no config, and Map.copyOf drops insertion order. Load whens using the JDK HttpClient (e.g. JdkHttpTransport) or serializing query parameters."
---

Use this skill when writing or debugging code that uses `java.net.http.HttpClient` (e.g. the
`JdkHttpTransport` default transport) or preserves query-parameter/map order.

## 1. HttpClient ALWAYS verifies hostnames, even with a trust-all SSLContext

Before every handshake, `jdk.internal.net.http.AbstractAsyncSSLConnection.createSSLParameters`
unconditionally runs `sslParameters.setEndpointIdentificationAlgorithm("HTTPS")`, and
`SSLContextImpl.AbstractTrustManagerWrapper` then calls `X509TrustManagerImpl.checkIdentity` →
`HostnameChecker` ON TOP of any custom trust manager. A custom `X509TrustManager` only bypasses
certificate-chain validation — never hostname matching.

Observed failure: trust-all + `https://127.0.0.1` against a cert with only `CN=localhost` and no
SAN → `SSLHandshakeException` caused by `CertificateException` "No subject alternative names
present" (`HostnameChecker.matchIP`), even though the custom trust manager accepts everything.

Consequences:
- `skipSsl` = trust bypass only. The URL hostname must still match the certificate subject.
- The only way to disable hostname verification is the global JVM property
  `-Djdk.internal.httpclient.disableHostnameVerification=true`, read ONCE at
  `jdk.internal.net.http.common.Utils` class-init. There is no per-client switch; setting it from a
  library is a whole-JVM side effect and unreliable if `Utils` is already loaded.
- Tests: use `https://localhost:port` with a `CN=localhost` self-signed cert. With no SAN present,
  `HostnameChecker.matchDNS` falls back to CN, so the hostname check passes and the trust-all TM
  handles the rest. Never connect to a bare IP for a `skipSsl` success test.

## 2. client.newBuilder() silently calls the STATIC HttpClient.newBuilder()

`HttpClient` has NO instance `newBuilder()` — only `public static Builder newBuilder()`. javac
accepts `instance.staticMethod()` with just a `[static]` warning, so `client.newBuilder()` compiles
but returns a FRESH default builder that inherits NONE of the base client's config (sslContext,
sslParameters, proxy, executor, redirect policy...).

```java
// TRAP — neverRedirectClient has no relation to `client`
final var derived = client.newBuilder().followRedirects(Redirect.NEVER).build();
```

Set every needed option explicitly on the derived builder (as `JdkHttpTransport` does), or build
all variants from the same source config. Watch for the `[static]` warning — it is a red flag that
an instance call resolved to a static method.

## 3. Map.copyOf / Map.of do NOT preserve insertion order

`Map.copyOf(new LinkedHashMap<>(page, ids))` returns hash-ordered keys (`[ids, page]`), so query
parameter order and string-assertion tests break even though the source map was ordered.

```java
// loses order
final var query = Map.copyOf(builder.query());
// preserves order
final var query = Collections.unmodifiableMap(new LinkedHashMap<>(builder.query()));
```

Never assert a specific serialization order derived from `Map.of(...)` or `Map.copyOf(...)` — their
iteration order is unspecified. Order-preserving query maps must go through
`Collections.unmodifiableMap(new LinkedHashMap<>(...))` (as `HttpRequestBuilder.query()` does).
