package io.github.khezyapp.dhttp.auth.credential;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A credential as persisted: the {@code data} map holds only the {@link EncryptedPayload} (ciphertext,
 * never plaintext) produced by a {@link CredentialCipher} (§6.2, §7 item 6).
 *
 * @param id        the unique credential id
 * @param type      the credential type (e.g. {@code oauth2}, {@code api-key})
 * @param data      the encrypted payload as a map
 * @param createdAt creation timestamp
 * @param updatedAt last-update timestamp
 */
public record StoredCredential(String id,
                               String type,
                               Map<String, Object> data,
                               Instant createdAt,
                               Instant updatedAt) {

    public StoredCredential {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        data = Map.copyOf(Objects.requireNonNullElseGet(data, Map::of));
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
