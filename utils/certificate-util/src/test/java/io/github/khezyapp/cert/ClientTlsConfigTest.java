package io.github.khezyapp.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientTlsConfigTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    @DisplayName("Defaults the alias to client")
    void defaultsAlias() {
        final var config = new ClientTlsConfig("chain", "key");

        assertEquals("client", config.alias());
    }

    @Test
    @DisplayName("Builds a TLS context from ASCII PEM material without a password")
    void toSslContextWithoutPassword() throws Exception {
        final var pair = rsaPair();
        final var cert = selfSigned(pair, "localhost");
        final var config = new ClientTlsConfig(certPem(cert), pkcs8Pem(pair.getPrivate()));

        assertNotNull(config.toSslContext());
    }

    @Test
    @DisplayName("Builds a TLS context from ASCII PEM material with a password")
    void toSslContextWithPassword() throws Exception {
        final var pair = rsaPair();
        final var cert = selfSigned(pair, "localhost");
        final var config = new ClientTlsConfig(certPem(cert), pkcs8Pem(pair.getPrivate()),
                "s3cret", null);

        assertNotNull(config.toSslContext());
    }

    private static KeyPair rsaPair() throws Exception {
        final var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
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

    private static String pkcs8Pem(final java.security.PrivateKey key) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + java.util.Base64.getMimeEncoder(64, "\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                        .encodeToString(key.getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    private static String certPem(final X509Certificate cert) throws Exception {
        return "-----BEGIN CERTIFICATE-----\n"
                + java.util.Base64.getMimeEncoder(64, "\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                        .encodeToString(cert.getEncoded())
                + "\n-----END CERTIFICATE-----";
    }
}
