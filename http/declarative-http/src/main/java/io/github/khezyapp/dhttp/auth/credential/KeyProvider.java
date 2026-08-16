package io.github.khezyapp.dhttp.auth.credential;

import javax.crypto.SecretKey;

/**
 * Supplies the master encryption key (§7 item 6). The consumer provides the key; the core never
 * generates, stores, or defaults it.
 */
@FunctionalInterface
public interface KeyProvider {

    /**
     * @return the master key used for credential encryption/decryption
     */
    SecretKey key();
}
