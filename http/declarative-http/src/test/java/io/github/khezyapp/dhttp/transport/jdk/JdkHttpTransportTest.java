package io.github.khezyapp.dhttp.transport.jdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.cert.ClientTlsConfig;
import io.github.khezyapp.cert.PemUtils;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.transport.ArrayFormat;
import io.github.khezyapp.dhttp.transport.Auth;
import io.github.khezyapp.dhttp.transport.Body;
import io.github.khezyapp.dhttp.transport.HttpRequest;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Real end-to-end coverage for {@link JdkHttpTransport} against a local {@link HttpServer} on an
 * ephemeral loopback port (§10.1) — no external network.
 */
class JdkHttpTransportTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    @DisplayName("GET serializes query parameters per REPEAT array format and returns the body")
    void getWithRepeatQuery() throws IOException {
        final var query = new AtomicReference<String>();
        final var server = start(exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, "{\"ok\":true}");
        });
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/users")
                    .method(HttpMethod.GET)
                    .query("page", 2)
                    .query("ids", List.of(1, 2, 3))
                    .build();

            final var result = new JdkHttpTransport().send(request);

            assertEquals("page=2&ids=1&ids=2&ids=3", query.get());
            assertEquals(200, result.status());
            assertEquals("{\"ok\":true}", result.bodyText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("GET serializes array query values per COMMA and BRACKETS formats")
    void getWithCommaAndBracketsQuery() throws IOException {
        final var queries = new ArrayList<String>();
        final var server = start(exchange -> {
            queries.add(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, "[]");
        });
        try {
            final var comma = HttpRequest.builder()
                    .url(base(server) + "/filter")
                    .method(HttpMethod.GET)
                    .query("status", List.of("a", "b"))
                    .queryArrayFormat(ArrayFormat.COMMA)
                    .build();
            final var brackets = HttpRequest.builder()
                    .url(base(server) + "/filter")
                    .method(HttpMethod.GET)
                    .query("status", new String[]{"a", "b"})
                    .queryArrayFormat(ArrayFormat.BRACKETS)
                    .build();

            new JdkHttpTransport().send(comma);
            new JdkHttpTransport().send(brackets);

            assertEquals("status=a,b", queries.get(0));
            assertEquals("status[]=a&status[]=b", queries.get(1));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("POST sends a JSON body with content-type and passthrough headers")
    void postJsonBody() throws IOException {
        final var captured = new AtomicReference<CapturedRequest>();
        final var server = start(exchange -> {
            captured.set(read(exchange));
            respond(exchange, 201, "{\"id\":1}");
        });
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/contacts")
                    .method(HttpMethod.POST)
                    .header("X-Custom", "abc")
                    .body(new Body.JsonBody(JSON.write(Map.of("name", "SOK"))))
                    .build();

            final var result = new JdkHttpTransport().send(request);

            assertEquals(201, result.status());
            assertEquals("application/json", captured.get().contentType());
            assertEquals("abc", captured.get().header("X-Custom"));
            assertEquals("{\"name\":\"SOK\"}", captured.get().body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("FORM bodies stringify primitive values and are sent as multipart/form-data")
    void postFormBody() throws IOException {
        final var captured = new AtomicReference<CapturedRequest>();
        final var server = start(exchange -> {
            captured.set(read(exchange));
            respond(exchange, 200, "{}");
        });
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/form")
                    .method(HttpMethod.POST)
                    .body(new Body.FormBody(Map.of(
                            "name", "VISAL",
                            "city", "Battambang",
                            "age", 30,
                            "active", true)))
                    .build();

            new JdkHttpTransport().send(request);

            final var body = captured.get().body();
            assertTrue(captured.get().contentType().startsWith("multipart/form-data; boundary="));
            assertTrue(body.contains("name=\"name\""));
            assertTrue(body.contains("VISAL"));
            assertTrue(body.contains("name=\"age\""));
            assertTrue(body.contains("30"));
            assertTrue(body.contains("name=\"active\""));
            assertTrue(body.contains("true"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("URLENCODED bodies serialize percent-encoded key=value pairs with the form content type")
    void postUrlEncodedBody() throws IOException {
        final var captured = new AtomicReference<CapturedRequest>();
        final var server = start(exchange -> {
            captured.set(read(exchange));
            respond(exchange, 200, "{}");
        });
        try {
            final var fields = new LinkedHashMap<String, Object>();
            fields.put("name", "VISAL");
            fields.put("city", "Battambang Province");
            fields.put("age", 30);
            final var request = HttpRequest.builder()
                    .url(base(server) + "/token")
                    .method(HttpMethod.POST)
                    .body(new Body.UrlEncodedBody(fields))
                    .build();

            new JdkHttpTransport().send(request);

            assertEquals("application/x-www-form-urlencoded", captured.get().contentType());
            assertEquals("name=VISAL&city=Battambang+Province&age=30", captured.get().body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("FORM file parts carry the original filename and content type")
    void postFormFilePart() throws IOException {
        final var captured = new AtomicReference<CapturedRequest>();
        final var server = start(exchange -> {
            captured.set(read(exchange));
            respond(exchange, 200, "{}");
        });
        try {
            final var file = new Body.FormBody.FilePart(
                    "fake-pdf-bytes".getBytes(StandardCharsets.UTF_8),
                    "report.pdf",
                    "application/pdf");
            final var request = HttpRequest.builder()
                    .url(base(server) + "/upload")
                    .method(HttpMethod.POST)
                    .body(new Body.FormBody(Map.of("doc", file)))
                    .build();

            new JdkHttpTransport().send(request);

            final var body = captured.get().body();
            assertTrue(body.contains("Content-Disposition: form-data; name=\"doc\"; "
                    + "filename=\"report.pdf\""));
            assertTrue(body.contains("Content-Type: application/pdf"));
            assertTrue(body.contains("fake-pdf-bytes"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("BINARY bodies send their content type and default to octet-stream")
    void binaryBodyContentType() throws IOException {
        final var received = new ArrayList<String>();
        final var server = start(exchange -> {
            received.add(exchange.getRequestHeaders().getFirst("Content-Type"));
            respond(exchange, 200, "{}");
        });
        try {
            final var pdf = HttpRequest.builder()
                    .url(base(server) + "/bin")
                    .method(HttpMethod.POST)
                    .body(new Body.BinaryBody("data".getBytes(StandardCharsets.UTF_8),
                            "application/pdf"))
                    .build();
            final var plain = HttpRequest.builder()
                    .url(base(server) + "/bin")
                    .method(HttpMethod.POST)
                    .body(new Body.BinaryBody("data".getBytes(StandardCharsets.UTF_8)))
                    .build();

            new JdkHttpTransport().send(pdf);
            new JdkHttpTransport().send(plain);

            assertEquals(List.of("application/pdf", "application/octet-stream"), received);
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Bearer auth is applied unless an Authorization header already exists")
    void bearerAuth() throws IOException {
        final var received = new ArrayList<String>();
        final var server = start(exchange -> {
            received.add(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{}");
        });
        try {
            final var transport = new JdkHttpTransport();
            final var fromAuth = HttpRequest.builder()
                    .url(base(server) + "/a")
                    .method(HttpMethod.GET)
                    .auth(new Auth.BearerAuth("tok123"))
                    .build();
            final var explicit = HttpRequest.builder()
                    .url(base(server) + "/b")
                    .method(HttpMethod.GET)
                    .auth(new Auth.BearerAuth("tok123"))
                    .header("Authorization", "Bearer manual")
                    .build();

            transport.send(fromAuth);
            transport.send(explicit);

            assertEquals(List.of("Bearer tok123", "Bearer manual"), received);
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("A non-2xx response throws an HttpApiException carrying the status")
    void non2xxThrows() throws IOException {
        final var server = start(exchange -> respond(exchange, 404, "{\"error\":\"nope\"}"));
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/missing")
                    .method(HttpMethod.GET)
                    .build();

            final var thrown = assertThrows(HttpApiException.class,
                    () -> new JdkHttpTransport().send(request));
            assertEquals(404, thrown.getStatus());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("ignoreStatusErrors lets the caller receive the non-2xx response")
    void ignoreStatusErrors() throws IOException {
        final var server = start(exchange -> respond(exchange, 404, "{\"error\":\"nope\"}"));
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/missing")
                    .method(HttpMethod.GET)
                    .ignoreStatusError(404)
                    .build();

            final var result = new JdkHttpTransport().send(request);

            assertEquals(404, result.status());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Same-origin redirects are followed keeping credentials and capped by maxRedirects")
    void followsSameOriginRedirect() throws IOException {
        final var seenAuth = new ArrayList<String>();
        final var seenPaths = new ArrayList<String>();
        final var server = start(exchange -> {
            seenPaths.add(exchange.getRequestURI().getPath());
            seenAuth.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if ("/start".equals(exchange.getRequestURI().getPath())) {
                exchange.getResponseHeaders().add("Location", "/final");
                respond(exchange, 302, "");
                return;
            }
            respond(exchange, 200, "{\"done\":true}");
        });
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/start")
                    .method(HttpMethod.GET)
                    .auth(new Auth.BearerAuth("tok123"))
                    .build();

            final var result = new JdkHttpTransport().send(request);

            assertEquals(200, result.status());
            assertEquals(List.of("/start", "/final"), seenPaths);
            assertEquals(List.of("Bearer tok123", "Bearer tok123"), seenAuth);
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("disableFollowRedirect returns the 3xx response untouched")
    void disabledRedirects() throws IOException {
        final var server = start(exchange -> {
            exchange.getResponseHeaders().add("Location", "/final");
            respond(exchange, 302, "");
        });
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/start")
                    .method(HttpMethod.GET)
                    .disableFollowRedirect(true)
                    .build();

            final var result = new JdkHttpTransport().send(request);

            assertEquals(302, result.status());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Cross-origin redirects strip credentials per the RedirectPolicy")
    void crossOriginRedirectStripsCredentials() throws IOException {
        final var seenAuth = new AtomicReference<String>();
        final var second = start(exchange -> {
            seenAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{}");
        });
        final var origin = start(exchange -> {
            exchange.getResponseHeaders().add("Location", base(second) + "/final");
            respond(exchange, 302, "");
        });
        try {
            final var request = HttpRequest.builder()
                    .url(base(origin) + "/start")
                    .method(HttpMethod.GET)
                    .auth(new Auth.BearerAuth("tok123"))
                    .build();

            final var result = new JdkHttpTransport().send(request);

            assertEquals(200, result.status());
            assertTrue(seenAuth.get() == null || seenAuth.get().isBlank());
        } finally {
            origin.stop(0);
            second.stop(0);
        }
    }

    @Test
    @DisplayName("allowedDomains enforce the SSRF allow-list at the transport")
    void allowedDomainsGuarded() throws IOException {
        final var server = start(exchange -> respond(exchange, 200, "{}"));
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/data")
                    .method(HttpMethod.GET)
                    .allowedDomain("api.example.com")
                    .build();

            assertThrows(HttpApiException.class, () -> new JdkHttpTransport().send(request));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("allowIpLiteral lets a raw IP literal bypass the SSRF allow-list at the transport")
    void ipLiteralAllowedWhenOptedIn() throws IOException {
        final var server = start(exchange -> respond(exchange, 200, "{}"));
        try {
            final var allowed = HttpRequest.builder()
                    .url(base(server) + "/data")
                    .method(HttpMethod.GET)
                    .allowedDomain("api.example.com")
                    .allowIpLiteral(true)
                    .build();
            assertEquals(200, new JdkHttpTransport().send(allowed).status());

            final var denied = HttpRequest.builder()
                    .url(base(server) + "/data")
                    .method(HttpMethod.GET)
                    .allowedDomain("api.example.com")
                    .build();
            assertThrows(HttpApiException.class, () -> new JdkHttpTransport().send(denied));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("stripCrossOriginCredentials(false) forwards credentials to a cross-origin redirect")
    void crossOriginCredentialsForwardedWhenOptedOut() throws IOException {
        final var seenAuth = new AtomicReference<String>();
        final var second = start(exchange -> {
            seenAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{}");
        });
        final var origin = start(exchange -> {
            exchange.getResponseHeaders().add("Location", base(second) + "/final");
            respond(exchange, 302, "");
        });
        try {
            final var request = HttpRequest.builder()
                    .url(base(origin) + "/start")
                    .method(HttpMethod.GET)
                    .auth(new Auth.BearerAuth("tok123"))
                    .stripCrossOriginCredentials(false)
                    .build();

            final var result = new JdkHttpTransport().send(request);

            assertEquals(200, result.status());
            assertEquals("Bearer tok123", seenAuth.get());
        } finally {
            origin.stop(0);
            second.stop(0);
        }
    }

    @Test
    @DisplayName("The request timeout aborts slow responses")
    void timeoutAborts() throws IOException {
        final var server = start(exchange -> {
            try {
                Thread.sleep(2_000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "{}");
        });
        try {
            final var request = HttpRequest.builder()
                    .url(base(server) + "/slow")
                    .method(HttpMethod.GET)
                    .timeout(150)
                    .build();

            final var thrown = assertThrows(HttpApiException.class,
                    () -> new JdkHttpTransport().send(request));
            assertEquals(HttpApiException.NO_STATUS, thrown.getStatus());
            assertInstanceOf(IOException.class, thrown.getCause());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("skipSsl bypasses certificate validation against a self-signed HTTPS server")
    void skipSslTrustsSelfSigned(@TempDir final Path tempDir) throws Exception {
        final var keystore = tempDir.resolve("server.p12");
        final var context = httpsContext(generateKeystore(keystore));
        final var server = HttpsServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context));
        server.createContext("/", exchange -> respond(exchange, 200, "{\"secure\":true}"));
        server.start();
        try {
            final var url = "https://localhost:" + server.getAddress().getPort() + "/ping";
            final var insecure = HttpRequest.builder()
                    .url(url)
                    .method(HttpMethod.GET)
                    .skipSsl(true)
                    .build();
            final var verified = HttpRequest.builder()
                    .url(url)
                    .method(HttpMethod.GET)
                    .build();

            final var result = new JdkHttpTransport().send(insecure);
            assertEquals("{\"secure\":true}", result.bodyText());
            assertThrows(HttpApiException.class, () -> new JdkHttpTransport().send(verified));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Derived clients inherit the base client's sslContext")
    void derivedClientInheritsSslContext(@TempDir final Path tempDir) throws Exception {
        final var keystore = tempDir.resolve("server.p12");
        final var context = httpsContext(generateKeystore(keystore));
        final var server = HttpsServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context));
        server.createContext("/", exchange -> respond(exchange, 200, "{\"secure\":true}"));
        server.start();
        try {
            final var base = HttpClient.newBuilder()
                    .sslContext(trustAllContext())
                    .build();
            final var result = new JdkHttpTransport(base).send(HttpRequest.builder()
                    .url("https://localhost:" + server.getAddress().getPort() + "/ping")
                    .method(HttpMethod.GET)
                    .build());
            assertEquals("{\"secure\":true}", result.bodyText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Presents a transport-level mTLS client certificate to a two-way TLS server")
    void presentsDefaultClientCertificate(@TempDir final Path tempDir) throws Exception {
        final var identity = clientIdentity();
        final var serverKeystore = tempDir.resolve("server.p12");
        generateKeystore(serverKeystore);
        final var server = startMtlsServer(serverKeystore, trustOnly(identity.certificate()));
        try {
            final var transport = new JdkHttpTransport(identity.config());
            final var result = transport.send(HttpRequest.builder()
                    .url("https://localhost:" + server.getAddress().getPort() + "/ping")
                    .method(HttpMethod.GET)
                    .skipSsl(true)
                    .build());
            assertEquals("{\"secure\":true}", result.bodyText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Presents a per-request mTLS client certificate to a two-way TLS server")
    void presentsRequestClientCertificate(@TempDir final Path tempDir) throws Exception {
        final var identity = clientIdentity();
        final var serverKeystore = tempDir.resolve("server.p12");
        generateKeystore(serverKeystore);
        final var server = startMtlsServer(serverKeystore, trustOnly(identity.certificate()));
        try {
            final var transport = new JdkHttpTransport();
            final var result = transport.send(HttpRequest.builder()
                    .url("https://localhost:" + server.getAddress().getPort() + "/ping")
                    .method(HttpMethod.GET)
                    .skipSsl(true)
                    .tlsConfig(identity.config())
                    .build());
            assertEquals("{\"secure\":true}", result.bodyText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Rejects a request to a two-way TLS server without a client certificate")
    void rejectsWithoutClientCertificate(@TempDir final Path tempDir) throws Exception {
        final var identity = clientIdentity();
        final var serverKeystore = tempDir.resolve("server.p12");
        generateKeystore(serverKeystore);
        final var server = startMtlsServer(serverKeystore, trustOnly(identity.certificate()));
        try {
            final var transport = new JdkHttpTransport();
            assertThrows(HttpApiException.class, () -> transport.send(HttpRequest.builder()
                    .url("https://localhost:" + server.getAddress().getPort() + "/ping")
                    .method(HttpMethod.GET)
                    .skipSsl(true)
                    .build()));
        } finally {
            server.stop(0);
        }
    }

    private static ClientIdentity clientIdentity() throws Exception {
        final var pair = rsaPair();
        final var cert = selfSigned(pair, "client");
        return new ClientIdentity(
                new ClientTlsConfig(PemUtils.toPem(cert), PemUtils.toPem(pair.getPrivate())), cert);
    }

    private static KeyPair rsaPair() throws Exception {
        final var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static HttpsServer startMtlsServer(final Path serverKeystore,
                                               final KeyStore clientTrustStore) throws Exception {
        final var context = SSLContext.getInstance("TLS");
        context.init(keyManagers(serverKeystore, "changeit"), trustManagers(clientTrustStore), null);
        final var server = HttpsServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context) {
            @Override
            public void configure(final HttpsParameters params) {
                final var sslParameters = context.getDefaultSSLParameters();
                sslParameters.setNeedClientAuth(true);
                params.setSSLParameters(sslParameters);
            }
        });
        server.createContext("/", exchange -> respond(exchange, 200, "{\"secure\":true}"));
        server.start();
        return server;
    }

    private static KeyManager[] keyManagers(final Path keystore,
                                            final String password) throws Exception {
        final var store = KeyStore.getInstance("PKCS12");
        try (var in = Files.newInputStream(keystore)) {
            store.load(in, password.toCharArray());
        }
        final var factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, password.toCharArray());
        return factory.getKeyManagers();
    }

    private static TrustManager[] trustManagers(final KeyStore trustStore) throws Exception {
        final var factory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        return factory.getTrustManagers();
    }

    private record ClientIdentity(ClientTlsConfig config,
                                  X509Certificate certificate) {
    }

    private static SSLContext trustAllContext() throws Exception {
        final var context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{trustAll()}, new SecureRandom());
        return context;
    }

    private static X509TrustManager trustAll() {
        return new X509TrustManager() {
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
    }

    private static Path generateKeystore(final Path target) throws Exception {
        final var keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
        final var process = new ProcessBuilder(
                keytool.toString(),
                "-genkeypair", "-alias", "test", "-keyalg", "RSA", "-keysize", "2048",
                "-dname", "CN=localhost", "-validity", "365",
                "-keystore", target.toString(),
                "-storepass", "changeit", "-keypass", "changeit")
                .redirectErrorStream(true)
                .start();
        assertEquals(0, process.waitFor());
        return target;
    }

    private static SSLContext httpsContext(final Path keystore) throws Exception {
        final var store = KeyStore.getInstance("PKCS12");
        try (var in = Files.newInputStream(keystore)) {
            store.load(in, "changeit".toCharArray());
        }
        final var factories = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factories.init(store, "changeit".toCharArray());
        final var context = SSLContext.getInstance("TLS");
        context.init(factories.getKeyManagers(), null, null);
        return context;
    }

    private static HttpServer start(final HttpHandler handler) throws IOException {
        final var server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", handler);
        server.start();
        return server;
    }

    private static KeyStore trustOnly(final X509Certificate certificate) throws Exception {
        final var store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setCertificateEntry("client", certificate);
        return store;
    }

    private static X509Certificate selfSigned(final KeyPair pair,
                                              final String commonName) throws Exception {
        final var now = System.currentTimeMillis();
        final var name = new X500Name("CN=" + commonName);
        final var builder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(now),
                new Date(now - 86400000L), new Date(now + 365L * 86400000L), name, pair.getPublic())
                .build(new JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(pair.getPrivate()));
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder);
    }

    private static String base(final HttpServer server) {
        return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
    }

    private static void respond(final HttpExchange exchange,
                                final int status,
                                final String body) throws IOException {
        final var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static CapturedRequest read(final HttpExchange exchange) throws IOException {
        final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return new CapturedRequest(
                body,
                exchange.getRequestHeaders().getFirst("Content-Type"),
                exchange.getRequestHeaders());
    }

    private record CapturedRequest(String body,
                                   String contentType,
                                   com.sun.net.httpserver.Headers headers) {

        private String header(final String name) {
            return headers.getFirst(name);
        }
    }
}
