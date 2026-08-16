package io.github.khezyapp.dhttp.auth.credential;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory {@link CredentialRepository} backed by a {@link ConcurrentHashMap}.
 */
public final class InMemoryCredentialRepository implements CredentialRepository {

    private final ConcurrentHashMap<String, StoredCredential> store = new ConcurrentHashMap<>();

    @Override
    public Optional<StoredCredential> findById(final String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<StoredCredential> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public StoredCredential save(final StoredCredential credential) {
        store.put(credential.id(), credential);
        return credential;
    }

    @Override
    public void deleteById(final String id) {
        store.remove(id);
    }
}
