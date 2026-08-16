# certificate-util

Bouncy Castle-backed certificate utilities for mTLS: turn ASCII certificate/key material into
usable JCA objects and build in-memory key stores and client SSL contexts.

## Coordinates

`io.github.khezyapp:certificate-util:1.0.0`

## What it does

- **PEM and Base64-DER parsing** for certificates (`parseCertificate`, `parseCertificates`) and
  private keys (`parsePrivateKey`) — so certificate material can be stored as ASCII in a database
  and fed in directly.
- **Private key formats**: PKCS#8 PEM, traditional PKCS#1/EC PEM, encrypted PKCS#8 PEM (password
  required), and Base64 PKCS#8 DER.
- **PEM encoding** (`toPem`) for certificates and PKCS#8 private keys — the ASCII form to persist.
- **Materialization**: `buildKeyStore` (in-memory PKCS#12 holding chain + key) and
  `buildClientContext` (an `SSLContext` presenting the client certificate, trusting the JVM default
  trust store unless caller-supplied trust managers override it; `trustAllManager()` covers the
  `skipSsl` case).

## ClientTlsConfig

`ClientTlsConfig(certChainPem, privateKeyPem, privateKeyPassword, alias)` is an immutable record —
all fields are ASCII text, so a full mTLS identity can be stored in a database. Materialize it with
`toKeyStore()` or `toSslContext()`.

```java
final var config = new ClientTlsConfig(certChainPem, privateKeyPem); // password optional
final var context = config.toSslContext();
```

## Dependency

Uses Bouncy Castle `bcpkix-jdk18on:1.85`; the `BC` provider is registered lazily and idempotently.
