package io.github.khezyapp.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;

/**
 * Certificate utilities built on Bouncy Castle (§mTLS): turn ASCII certificate/key material into
 * usable JCA objects and build in-memory key stores and client {@link SSLContext}s.
 *
 * <p>All parsing methods accept both PEM text ({@code -----BEGIN CERTIFICATE-----} ...) and
 * Base64-encoded DER text, so certificate material can be stored as ASCII in a database and fed in
 * directly. Private keys are supported in PKCS#8 PEM, traditional PKCS#1/EC PEM, encrypted PKCS#8
 * PEM, and Base64 PKCS#8 DER form; encrypted keys require a password.</p>
 *
 * <p>The {@link BouncyCastleProvider} is registered lazily and idempotently on first use so the
 * Bouncy Castle converters can find it. Client contexts built here use the JVM default trust store
 * unless caller-supplied {@link TrustManager}s override it — {@link #trustAllManager()} is provided
 * for the {@code skipSsl} use case.</p>
 */
public final class PemUtils {

    private static final String CERTIFICATE_LABEL = "CERTIFICATE";
    private static final String PRIVATE_KEY_LABEL = "PRIVATE KEY";

    private PemUtils() {
    }

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Parses a single certificate from PEM text or Base64-encoded DER text.
     *
     * @param ascii the PEM or Base64-DER certificate text
     * @return the parsed certificate
     * @throws IllegalArgumentException whens the input contains no valid certificate
     */
    public static X509Certificate parseCertificate(final String ascii) {
        Objects.requireNonNull(ascii, "ascii");
        final var trimmed = ascii.strip();
        if (trimmed.startsWith("-----BEGIN")) {
            return parsePemCertificate(trimmed);
        }
        return parseDerCertificate(trimmed);
    }

    /**
     * Parses a certificate chain (leaf first) from PEM text or a single Base64-encoded DER
     * certificate.
     *
     * @param ascii the PEM chain or Base64-DER certificate text
     * @return the parsed certificate chain
     * @throws IllegalArgumentException whens the input contains no certificate
     */
    public static List<X509Certificate> parseCertificates(final String ascii) {
        Objects.requireNonNull(ascii, "ascii");
        final var trimmed = ascii.strip();
        if (!trimmed.startsWith("-----BEGIN")) {
            return List.of(parseDerCertificate(trimmed));
        }
        final var converter = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME);
        final var result = new ArrayList<X509Certificate>();
        try (var parser = new PEMParser(new StringReader(trimmed))) {
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof X509CertificateHolder holder) {
                    result.add(converter.getCertificate(holder));
                }
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot read PEM certificate chain", e);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot parse PEM certificate chain", e);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No certificate found in the given ASCII input");
        }
        return List.copyOf(result);
    }

    /**
     * Parses a private key from PEM text or Base64-encoded PKCS#8 DER text, assuming no password.
     *
     * @param ascii the PEM or Base64-DER private key text
     * @return the parsed private key
     * @throws IllegalArgumentException whens the key is encrypted or cannot be parsed
     */
    public static PrivateKey parsePrivateKey(final String ascii) {
        return parsePrivateKey(ascii, null);
    }

    /**
     * Parses a private key from PEM text or Base64-encoded PKCS#8 DER text.
     *
     * <p>Supported PEM forms: PKCS#8 ({@code BEGIN PRIVATE KEY}), traditional RSA/EC
     * ({@code BEGIN RSA PRIVATE KEY}/{@code BEGIN EC PRIVATE KEY}), encrypted PKCS#8
     * ({@code BEGIN ENCRYPTED PRIVATE KEY}), and encrypted traditional
     * ({@code BEGIN ENCRYPTED ... PRIVATE KEY}).</p>
     *
     * @param ascii    the PEM or Base64-DER private key text
     * @param password the password for encrypted keys, or {@code null} for unencrypted keys
     * @return the parsed private key
     * @throws IllegalArgumentException whens the key cannot be parsed or a password is missing
     */
    public static PrivateKey parsePrivateKey(final String ascii,
                                             final char[] password) {
        Objects.requireNonNull(ascii, "ascii");
        final var trimmed = ascii.strip();
        if (trimmed.startsWith("-----BEGIN")) {
            return parsePemPrivateKey(trimmed, password);
        }
        return parseDerPrivateKey(trimmed);
    }

    /**
     * Encodes a certificate as PEM text ({@code -----BEGIN CERTIFICATE-----}).
     *
     * @param certificate the certificate to encode
     * @return the PEM text, ready for ASCII storage
     */
    public static String toPem(final X509Certificate certificate) {
        Objects.requireNonNull(certificate, "certificate");
        try {
            return pem(CERTIFICATE_LABEL, certificate.getEncoded());
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot encode certificate as PEM", e);
        }
    }

    /**
     * Encodes a private key as PKCS#8 PEM text ({@code -----BEGIN PRIVATE KEY-----}).
     *
     * @param privateKey the private key to encode
     * @return the PEM text, ready for ASCII storage
     */
    public static String toPem(final PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "privateKey");
        final var encoded = privateKey.getEncoded();
        if (Objects.isNull(encoded)) {
            throw new IllegalArgumentException("The private key is not encodable");
        }
        return pem(PRIVATE_KEY_LABEL, encoded);
    }

    /**
     * Builds an in-memory PKCS#12 {@link KeyStore} holding the private key and its certificate
     * chain, ready for a {@link KeyManagerFactory}.
     *
     * @param chain      the certificate chain, leaf first (entry zero must match the key)
     * @param privateKey the private key
     * @param password   the key store / key password, or {@code null} for none
     * @param alias      the entry alias, or {@code null} for the default
     * @return the populated key store
     */
    public static KeyStore buildKeyStore(final List<X509Certificate> chain,
                                         final PrivateKey privateKey,
                                         final char[] password,
                                         final String alias) {
        Objects.requireNonNull(chain, "chain");
        Objects.requireNonNull(privateKey, "privateKey");
        try {
            final var store = KeyStore.getInstance("PKCS12");
            store.load(null, null);
            store.setKeyEntry(Objects.requireNonNullElse(alias, "client"), privateKey,
                    passwordOrEmpty(password), chain.toArray(new X509Certificate[0]));
            return store;
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot build the key store", e);
        }
    }

    /**
     * Builds a client {@link SSLContext} presenting the key store's client certificate, trusting
     * the JVM default trust store.
     *
     * @param keyStore the key store holding the client certificate chain and private key
     * @param password the key store password, or {@code null} for none
     * @return the configured SSL context
     */
    public static SSLContext buildClientContext(final KeyStore keyStore,
                                                final char[] password) {
        return buildClientContext(keyStore, password, null);
    }

    /**
     * Builds a client {@link SSLContext} presenting the key store's client certificate with
     * caller-supplied trust managers.
     *
     * @param keyStore      the key store holding the client certificate chain and private key
     * @param password      the key store password, or {@code null} for none
     * @param trustManagers the trust managers for server validation, or {@code null} for the JVM
     *                      default trust store
     * @return the configured SSL context
     */
    public static SSLContext buildClientContext(final KeyStore keyStore,
                                                final char[] password,
                                                final TrustManager[] trustManagers) {
        Objects.requireNonNull(keyStore, "keyStore");
        try {
            final var factory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            factory.init(keyStore, passwordOrEmpty(password));
            final var context = SSLContext.getInstance("TLS");
            context.init(factory.getKeyManagers(),
                    Objects.isNull(trustManagers) ? defaultTrustManagers() : trustManagers,
                    new SecureRandom());
            return context;
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot build the client SSL context", e);
        }
    }

    /**
     * @return a trust-all {@link X509TrustManager} for the {@code skipSsl} use case
     */
    public static X509TrustManager trustAllManager() {
        return TRUST_ALL;
    }

    private static X509Certificate parsePemCertificate(final String pem) {
        try (var parser = new PEMParser(new StringReader(pem))) {
            final var object = parser.readObject();
            if (object instanceof X509CertificateHolder holder) {
                return new JcaX509CertificateConverter().setProvider(
                        BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);
            }
            throw new IllegalArgumentException("No certificate found in the given PEM input");
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot read PEM certificate", e);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot parse PEM certificate", e);
        }
    }

    private static X509Certificate parseDerCertificate(final String base64Der) {
        try {
            final var bytes = Base64.getMimeDecoder().decode(stripBase64(base64Der));
            final var factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(bytes));
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot parse base64 DER certificate", e);
        }
    }

    private static PrivateKey parsePemPrivateKey(final String pem,
                                                 final char[] password) {
        try (var parser = new PEMParser(new StringReader(pem))) {
            final var object = parser.readObject();
            if (object instanceof PEMEncryptedKeyPair encryptedPair) {
                return convert(decrypt(encryptedPair, password).getPrivateKeyInfo());
            }
            if (object instanceof PKCS8EncryptedPrivateKeyInfo encrypted) {
                return convert(decrypt(encrypted, password));
            }
            if (object instanceof PEMKeyPair pair) {
                return convert(pair.getPrivateKeyInfo());
            }
            if (object instanceof PrivateKeyInfo info) {
                return convert(info);
            }
            throw new IllegalArgumentException("No private key found in the given PEM input");
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot read PEM private key", e);
        }
    }

    private static PrivateKey parseDerPrivateKey(final String base64Der) {
        try {
            final var bytes = Base64.getMimeDecoder().decode(stripBase64(base64Der));
            return convert(PrivateKeyInfo.getInstance(bytes));
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot parse base64 DER private key", e);
        }
    }

    private static PEMKeyPair decrypt(final PEMEncryptedKeyPair encrypted,
                                      final char[] password) {
        requirePassword(password);
        final var decryptor = new JcePEMDecryptorProviderBuilder().setProvider(
                BouncyCastleProvider.PROVIDER_NAME).build(password);
        try {
            return encrypted.decryptKeyPair(decryptor);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot decrypt the traditional private key", e);
        }
    }

    private static PrivateKeyInfo decrypt(final PKCS8EncryptedPrivateKeyInfo encrypted,
                                          final char[] password) {
        requirePassword(password);
        final var decryptor = new JcePKCSPBEInputDecryptorProviderBuilder().setProvider(
                BouncyCastleProvider.PROVIDER_NAME).build(password);
        try {
            return encrypted.decryptPrivateKeyInfo(decryptor);
        } catch (final PKCSException e) {
            throw new IllegalArgumentException("Cannot decrypt the PKCS#8 private key", e);
        }
    }

    private static void requirePassword(final char[] password) {
        if (Objects.isNull(password) || password.length == 0) {
            throw new IllegalArgumentException(
                    "The private key is encrypted and requires a password");
        }
    }

    private static PrivateKey convert(final PrivateKeyInfo info) {
        try {
            return new JcaPEMKeyConverter().setProvider(
                    BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(info);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot convert the private key", e);
        }
    }

    private static String pem(final String label,
                              final byte[] der) {
        final var base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + base64 + "\n-----END " + label + "-----";
    }

    private static String stripBase64(final String base64) {
        return base64.replaceAll("\\s+", "");
    }

    private static char[] passwordOrEmpty(final char[] password) {
        return Objects.isNull(password) ? new char[0] : password;
    }

    private static TrustManager[] defaultTrustManagers() {
        try {
            final var factory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            factory.init((KeyStore) null);
            return factory.getTrustManagers();
        } catch (final Exception e) {
            throw new IllegalArgumentException("Cannot load the JVM default trust store", e);
        }
    }

    private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        @Override
        public void checkClientTrusted(final X509Certificate[] chain,
                                       final String authType) {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain,
                                       final String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
