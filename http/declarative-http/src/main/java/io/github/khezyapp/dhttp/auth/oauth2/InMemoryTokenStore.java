package io.github.khezyapp.dhttp.auth.oauth2;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-process {@link TokenStore} backed by a {@link ConcurrentHashMap}.
 */
public final class InMemoryTokenStore implements TokenStore {

    private final Map<String, OAuth2Token> tokens = new ConcurrentHashMap<>();

    @Override
    public Optional<OAuth2Token> load(final String credentialId) {
        return Optional.ofNullable(tokens.get(Objects.requireNonNull(credentialId, "credentialId")));
    }

    @Override
    public void save(final String credentialId,
                     final OAuth2Token token) {
        tokens.put(Objects.requireNonNull(credentialId, "credentialId"),
                Objects.requireNonNull(token, "token"));
    }

    @Override
    public void clear(final String credentialId) {
        tokens.remove(Objects.requireNonNull(credentialId, "credentialId"));
    }
}
