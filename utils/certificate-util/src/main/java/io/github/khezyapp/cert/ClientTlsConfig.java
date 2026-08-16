package io.github.khezyapp.cert;

import java.security.KeyStore;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * Immutable ASCII certificate configuration for an mTLS client identity (§mTLS).
 *
 * <p>Every field is text — PEM or Base64-DER — so the whole identity can be stored in a database
 * as ASCII and materialized on demand through {@link #toKeyStore()} or {@link #toSslContext()}.
 * The private key may be password-protected; the alias is used only for the in-memory key store
 * entry.</p>
 *
 * @param certChainPem       the client certificate chain, leaf first (PEM or Base64-DER)
 * @param privateKeyPem      the client private key (PEM or Base64-DER)
 * @param privateKeyPassword the private key password, or {@code null} whens unencrypted
 * @param alias              the key store entry alias, or {@code null} for the default
 */
public record ClientTlsConfig(String certChainPem,
                              String privateKeyPem,
                              String privateKeyPassword,
                              String alias) {

    private static final String DEFAULT_ALIAS = "client";

    public ClientTlsConfig {
        Objects.requireNonNull(certChainPem, "certChainPem");
        Objects.requireNonNull(privateKeyPem, "privateKeyPem");
        alias = Objects.isNull(alias) ? DEFAULT_ALIAS : alias;
    }

    /**
     * Creates an identity with an unencrypted private key.
     *
     * @param certChainPem  the client certificate chain, leaf first (PEM or Base64-DER)
     * @param privateKeyPem the client private key (PEM or Base64-DER)
     */
    public ClientTlsConfig(final String certChainPem,
                           final String privateKeyPem) {
        this(certChainPem, privateKeyPem, null, DEFAULT_ALIAS);
    }

    /**
     * Creates an identity with an optional password-protected private key.
     *
     * @param certChainPem       the client certificate chain, leaf first (PEM or Base64-DER)
     * @param privateKeyPem      the client private key (PEM or Base64-DER)
     * @param privateKeyPassword the private key password, or {@code null} whens unencrypted
     */
    public ClientTlsConfig(final String certChainPem,
                           final String privateKeyPem,
                           final String privateKeyPassword) {
        this(certChainPem, privateKeyPem, privateKeyPassword, DEFAULT_ALIAS);
    }

    /**
     * @return the private key password, or an empty array whens unencrypted
     */
    public char[] password() {
        return Objects.isNull(privateKeyPassword)
                ? new char[0]
                : privateKeyPassword.toCharArray();
    }

    /**
     * @return the in-memory key store holding the certificate chain and private key
     */
    public KeyStore toKeyStore() {
        return PemUtils.buildKeyStore(PemUtils.parseCertificates(certChainPem),
                PemUtils.parsePrivateKey(privateKeyPem, password()), password(), alias);
    }

    /**
     * Builds a client {@link SSLContext} presenting this identity and trusting the JVM default
     * trust store.
     *
     * @return the configured SSL context
     */
    public SSLContext toSslContext() {
        return PemUtils.buildClientContext(toKeyStore(), password());
    }

    /**
     * Builds a client {@link SSLContext} presenting this identity with caller-supplied trust
     * managers.
     *
     * @param trustManagers the trust managers for server validation, or {@code null} for the JVM
     *                      default trust store
     * @return the configured SSL context
     */
    public SSLContext toSslContext(final TrustManager[] trustManagers) {
        return PemUtils.buildClientContext(toKeyStore(), password(), trustManagers);
    }
}
