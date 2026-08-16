package io.github.khezyapp.dhttp.auth.credential;

import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.spec.CredentialRef;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified credential configuration &amp; management (§6.2, R10).
 *
 * <p>Owns the encrypt/decrypt round-trip: typed configs become maps via {@link JsonMapper}, are
 * encrypted with the {@link CredentialCipher}, and persisted through the {@link CredentialRepository}.
 * The engine only reaches credentials through {@link #asStore()}; it never touches CRUD here.</p>
 */
public final class CredentialService {

    private final CredentialRepository repository;
    private final CredentialCipher cipher;
    private final JsonMapper jsonMapper;

    public CredentialService(final CredentialRepository repository,
                             final CredentialCipher cipher,
                             final JsonMapper jsonMapper) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.cipher = Objects.requireNonNull(cipher, "cipher");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    /**
     * Creates a credential from a raw properties map.
     *
     * @param type       the credential type
     * @param properties the plaintext properties
     * @return the new credential id
     */
    public String create(final String type,
                         final Map<String, Object> properties) {
        final var id = UUID.randomUUID().toString();
        save(id, type, properties);
        return id;
    }

    /**
     * Creates a credential from a typed config record.
     *
     * @param type        the credential type
     * @param typedConfig the typed configuration
     * @param <T>         the config type
     * @return the new credential id
     */
    public <T> String create(final String type,
                             final T typedConfig) {
        return create(type, jsonMapper.toMap(typedConfig));
    }

    /**
     * @param id the credential id
     * @return the decrypted credential (map view), or empty whens unknown
     */
    public Optional<DecryptedCredential<?>> get(final String id) {
        return repository.findById(id).map(this::decryptMap);
    }

    /**
     * @param id   the credential id
     * @param type the desired typed config class
     * @param <T>  the config type
     * @return the decrypted credential with a type-safe view, or empty whens unknown
     */
    public <T> Optional<DecryptedCredential<T>> get(final String id,
                                                    final Class<T> type) {
        return repository.findById(id).map(stored -> decryptTyped(stored, type));
    }

    /**
     * @return id + type only; never secrets
     */
    public List<CredentialSummary> list() {
        return repository.findAll().stream()
                .map(credential -> new CredentialSummary(credential.id(), credential.type()))
                .toList();
    }

    /**
     * Re-encrypts a credential from a raw properties map.
     *
     * @param id         the credential id (must exist)
     * @param properties the new plaintext properties
     * @return the decrypted credential after update
     */
    public DecryptedCredential<?> update(final String id,
                                         final Map<String, Object> properties) {
        final var stored = requireStored(id);
        save(id, stored.type(), properties);
        return get(id).orElseThrow();
    }

    /**
     * Re-encrypts a credential from a typed config record.
     *
     * @param id          the credential id (must exist)
     * @param typedConfig the new typed configuration
     * @param <T>         the config type
     * @return the decrypted credential after update
     */
    @SuppressWarnings("unchecked")
    public <T> DecryptedCredential<T> update(final String id,
                                             final T typedConfig) {
        final var stored = requireStored(id);
        save(id, stored.type(), jsonMapper.toMap(typedConfig));
        return get(id, (Class<T>) typedConfig.getClass()).orElseThrow();
    }

    /**
     * @param id the credential id to remove
     */
    public void delete(final String id) {
        repository.deleteById(id);
    }

    /**
     * @return an engine-facing {@link CredentialStore} backed by this service
     */
    public CredentialStore asStore() {
        return (final var ref, final var ctx) -> resolve(ref);
    }

    /**
     * Resolves a credential reference to its decrypted credential.
     *
     * @param ref the credential reference
     * @return the decrypted credential, or empty whens unknown
     */
    public Optional<DecryptedCredential<?>> resolve(final CredentialRef ref) {
        Objects.requireNonNull(ref, "ref");
        return get(ref.id());
    }

    private void save(final String id,
                      final String type,
                      final Map<String, Object> properties) {
        final var now = Instant.now();
        final var stored = repository.findById(id).orElse(null);
        final var createdAt = stored == null ? now : stored.createdAt();
        final var encrypted = cipher.encrypt(properties);
        repository.save(new StoredCredential(id, type, encrypted.toMap(), createdAt, now));
    }

    private DecryptedCredential<?> decryptMap(final StoredCredential stored) {
        final var plain = cipher.decrypt(EncryptedPayload.fromMap(stored.data()));
        return new DecryptedCredential<>(stored.id(), stored.type(), plain, plain);
    }

    private <T> DecryptedCredential<T> decryptTyped(final StoredCredential stored,
                                                    final Class<T> type) {
        final var plain = cipher.decrypt(EncryptedPayload.fromMap(stored.data()));
        return new DecryptedCredential<>(stored.id(), stored.type(), plain,
                jsonMapper.fromMap(plain, type));
    }

    private StoredCredential requireStored(final String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown credential id: " + id));
    }
}
