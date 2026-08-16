package io.github.khezyapp.dhttp.auth.credential.type;

import java.util.Objects;

/**
 * Typed configuration for a {@code client-certificate} credential (§6.2): the mTLS client identity
 * stored as ASCII text so it can live in a database (and is encrypted at rest by the
 * {@code CredentialService} cipher).
 *
 * @param certChainPem       the client certificate chain, leaf first (PEM or Base64-DER)
 * @param privateKeyPem      the client private key (PEM or Base64-DER)
 * @param privateKeyPassword the private key password, or {@code null} whens unencrypted
 */
public record ClientCertificateCredentials(String certChainPem,
                                           String privateKeyPem,
                                           String privateKeyPassword) {

    public ClientCertificateCredentials {
        Objects.requireNonNull(certChainPem, "certChainPem");
        Objects.requireNonNull(privateKeyPem, "privateKeyPem");
    }
}
