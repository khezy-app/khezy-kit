package io.github.khezyapp.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PemUtilsTest {

    private static final char[] KEY_PASSWORD = "secret".toCharArray();

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    @DisplayName("Parses a certificate from PEM text")
    void parsesPemCertificate() throws Exception {
        final var cert = selfSigned(rsaPair(), "localhost");

        final var parsed = PemUtils.parseCertificate(certPem(cert));

        assertEquals(cert, parsed);
    }

    @Test
    @DisplayName("Parses a certificate from Base64-DER text")
    void parsesBase64DerCertificate() throws Exception {
        final var cert = selfSigned(rsaPair(), "localhost");

        final var parsed = PemUtils.parseCertificate(base64(cert.getEncoded()));

        assertEquals(cert, parsed);
    }

    @Test
    @DisplayName("Parses a certificate chain from concatenated PEM blocks")
    void parsesCertificateChain() throws Exception {
        final var root = selfSigned(rsaPair(), "root");
        final var leaf = selfSigned(rsaPair(), "leaf");

        final var chain = PemUtils.parseCertificates(certPem(leaf) + "\n" + certPem(root));

        assertEquals(2, chain.size());
        assertEquals(leaf, chain.get(0));
        assertEquals(root, chain.get(1));
    }

    @Test
    @DisplayName("Parses a PKCS#8 private key from PEM text")
    void parsesPkcs8PrivateKey() throws Exception {
        final var pair = rsaPair();

        final var parsed = PemUtils.parsePrivateKey(pkcs8Pem(pair.getPrivate()));

        assertSignsWith(pair, parsed);
    }

    @Test
    @DisplayName("Parses a PKCS#1 RSA private key from PEM text")
    void parsesPkcs1RsaPrivateKey() throws Exception {
        final var pair = rsaPair();

        final var parsed = PemUtils.parsePrivateKey(rsaPkcs1Pem(pair.getPrivate()));

        assertSignsWith(pair, parsed);
    }

    @Test
    @DisplayName("Parses an EC private key from PEM text")
    void parsesEcPrivateKey() throws Exception {
        final var pair = ecPair();

        final var parsed = PemUtils.parsePrivateKey(pkcs8Pem(pair.getPrivate()));

        assertSignsWith(pair, parsed);
    }

    @Test
    @DisplayName("Parses a private key from Base64 PKCS#8 DER text")
    void parsesBase64DerPrivateKey() throws Exception {
        final var pair = rsaPair();

        final var parsed = PemUtils.parsePrivateKey(base64(pair.getPrivate().getEncoded()));

        assertSignsWith(pair, parsed);
    }

    @Test
    @DisplayName("Decrypts an encrypted PKCS#8 private key with the right password")
    void parsesEncryptedPkcs8PrivateKey() throws Exception {
        final var pair = rsaPair();
        final var encrypted = encryptedPkcs8Pem(pair.getPrivate(), KEY_PASSWORD);

        final var parsed = PemUtils.parsePrivateKey(encrypted, KEY_PASSWORD);

        assertSignsWith(pair, parsed);
    }

    @Test
    @DisplayName("Rejects an encrypted private key without a password")
    void encryptedKeyRequiresPassword() throws Exception {
        final var encrypted = encryptedPkcs8Pem(rsaPair().getPrivate(), KEY_PASSWORD);

        assertThrows(IllegalArgumentException.class, () -> PemUtils.parsePrivateKey(encrypted));
    }

    @Test
    @DisplayName("Encodes a certificate back to PEM for ASCII storage")
    void toPemCertificateRoundTrips() throws Exception {
        final var cert = selfSigned(rsaPair(), "localhost");

        final var roundTripped = PemUtils.parseCertificate(PemUtils.toPem(cert));

        assertEquals(cert, roundTripped);
    }

    @Test
    @DisplayName("Encodes a private key back to PKCS#8 PEM for ASCII storage")
    void toPemPrivateKeyRoundTrips() throws Exception {
        final var pair = rsaPair();

        final var roundTripped = PemUtils.parsePrivateKey(PemUtils.toPem(pair.getPrivate()));

        assertSignsWith(pair, roundTripped);
    }

    @Test
    @DisplayName("Builds a key store holding the private key and its chain")
    void buildKeyStoreHoldsKeyAndChain() throws Exception {
        final var pair = rsaPair();
        final var cert = selfSigned(pair, "localhost");

        final var store = PemUtils.buildKeyStore(List.of(cert), pair.getPrivate(), KEY_PASSWORD, null);

        assertTrue(store.isKeyEntry("client"));
        assertNotNull(store.getCertificate("client"));
        assertNotNull(store.getKey("client", KEY_PASSWORD));
    }

    @Test
    @DisplayName("Builds a TLS client context from the key store")
    void buildClientContextSucceeds() throws Exception {
        final var pair = rsaPair();
        final var cert = selfSigned(pair, "localhost");
        final var store = PemUtils.buildKeyStore(List.of(cert), pair.getPrivate(), KEY_PASSWORD, null);

        final var context = PemUtils.buildClientContext(store, KEY_PASSWORD);

        assertEquals("TLS", context.getProtocol());
    }

    @Test
    @DisplayName("Rejects malformed ASCII input")
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class,
                () -> PemUtils.parseCertificate("not a certificate"));
        assertThrows(IllegalArgumentException.class,
                () -> PemUtils.parsePrivateKey("not a key"));
    }

    private static void assertSignsWith(final KeyPair pair,
                                        final PrivateKey parsed) throws Exception {
        final var data = "khezy-cert-util".getBytes(StandardCharsets.UTF_8);
        final var algorithm = "EC".equals(pair.getPrivate().getAlgorithm())
                ? "SHA256withECDSA" : "SHA256withRSA";
        final var signer = Signature.getInstance(algorithm);
        signer.initSign(parsed);
        signer.update(data);
        final var signature = signer.sign();
        final var verifier = Signature.getInstance(algorithm);
        verifier.initVerify(pair.getPublic());
        verifier.update(data);
        assertTrue(verifier.verify(signature));
    }

    private static KeyPair rsaPair() throws Exception {
        final var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static KeyPair ecPair() throws Exception {
        final var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
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

    private static String pkcs8Pem(final PrivateKey key) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + base64(key.getEncoded()) + "\n-----END PRIVATE KEY-----";
    }

    private static String rsaPkcs1Pem(final PrivateKey key) throws Exception {
        final var info = PrivateKeyInfo.getInstance(key.getEncoded());
        final var rsa = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(info.parsePrivateKey());
        return "-----BEGIN RSA PRIVATE KEY-----\n"
                + base64(rsa.getEncoded()) + "\n-----END RSA PRIVATE KEY-----";
    }

    private static String encryptedPkcs8Pem(final PrivateKey key,
                                            final char[] password) throws Exception {
        final var encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .setPassword(password);
        final var out = new StringWriter();
        try (var writer = new JcaPEMWriter(out)) {
            writer.writeObject(new PKCS8Generator(
                    PrivateKeyInfo.getInstance(key.getEncoded()), encryptor.build()));
        }
        return out.toString();
    }

    private static String certPem(final X509Certificate cert) throws Exception {
        return "-----BEGIN CERTIFICATE-----\n"
                + base64(cert.getEncoded()) + "\n-----END CERTIFICATE-----";
    }

    private static String base64(final byte[] der) {
        return Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(der);
    }
}
