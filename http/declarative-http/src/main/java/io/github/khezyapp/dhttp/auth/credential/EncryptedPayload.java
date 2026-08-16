package io.github.khezyapp.dhttp.auth.credential;

import java.util.Map;
import java.util.Objects;

/**
 * The ciphertext payload stored inside {@link StoredCredential#data()} (§6.2).
 *
 * @param algorithm  the cipher algorithm identifier
 * @param iv         the base64 initialization vector
 * @param ciphertext the base64 ciphertext
 */
public record EncryptedPayload(String algorithm, String iv, String ciphertext) {

    public EncryptedPayload {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(iv, "iv");
        Objects.requireNonNull(ciphertext, "ciphertext");
    }

    /**
     * @param map the stored map (keys {@code algorithm}, {@code iv}, {@code ciphertext})
     * @return the reconstructed payload
     */
    public static EncryptedPayload fromMap(final Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        return new EncryptedPayload(
                (String) map.get("algorithm"),
                (String) map.get("iv"),
                (String) map.get("ciphertext"));
    }

    /**
     * @return the map form used for storage
     */
    public Map<String, Object> toMap() {
        return Map.of("algorithm", algorithm, "iv", iv, "ciphertext", ciphertext);
    }
}
