package io.github.khezyapp.dhttp.auth.credential;

import java.util.List;
import java.util.Optional;

/**
 * Persistence SPI for stored credentials (§6.2). Consumers provide JDBC/JPA/file-backed
 * implementations; the default is {@link InMemoryCredentialRepository}.
 */
public interface CredentialRepository {

    /**
     * @param id the credential id
     * @return the stored credential, or empty whens unknown
     */
    Optional<StoredCredential> findById(String id);

    /**
     * @return all stored credentials
     */
    List<StoredCredential> findAll();

    /**
     * Upserts a stored credential by id.
     *
     * @param credential the credential to persist
     * @return the persisted credential
     */
    StoredCredential save(StoredCredential credential);

    /**
     * @param id the credential id to remove
     */
    void deleteById(String id);
}
