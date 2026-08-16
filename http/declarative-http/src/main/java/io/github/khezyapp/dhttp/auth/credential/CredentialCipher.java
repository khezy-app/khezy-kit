package io.github.khezyapp.dhttp.auth.credential;

import java.util.Map;

/**
 * Encrypts credential properties so only ciphertext is persisted (§7 item 6).
 */
public interface CredentialCipher {

    /**
     * @param plaintext the credential properties to protect
     * @return the encrypted payload
     */
    EncryptedPayload encrypt(Map<String, Object> plaintext);

    /**
     * @param payload the encrypted payload
     * @return the decrypted credential properties
     */
    Map<String, Object> decrypt(EncryptedPayload payload);
}
