package io.github.khezyapp.dhttp.auth.credential;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A decrypted credential: the plaintext fields plus a type-safe view built via {@code JsonMapper}.
 *
 * @param id     the credential id
 * @param type   the credential type
 * @param fields the decrypted properties
 * @param data   the type-safe view of {@code fields} (e.g. a typed config record)
 */
public record DecryptedCredential<T>(String id,
                                     String type,
                                     Map<String, Object> fields,
                                     T data) {

    public DecryptedCredential {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    /**
     * @return the decrypted properties
     */
    public Map<String, Object> fields() {
        return fields;
    }
}
