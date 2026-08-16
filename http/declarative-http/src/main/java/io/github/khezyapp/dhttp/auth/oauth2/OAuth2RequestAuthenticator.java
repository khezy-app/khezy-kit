package io.github.khezyapp.dhttp.auth.oauth2;

import io.github.khezyapp.dhttp.auth.AuthResult;
import io.github.khezyapp.dhttp.auth.Authenticator;
import io.github.khezyapp.dhttp.auth.credential.DecryptedCredential;
import io.github.khezyapp.dhttp.auth.credential.type.OAuth2Credentials;
import io.github.khezyapp.dhttp.error.HttpApiException;
import io.github.khezyapp.dhttp.error.OAuth2NotConfiguredException;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.transport.Auth;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.HttpTransport;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Request-time OAuth2 orchestrator implementing {@link Authenticator} (§6.6, R10).
 *
 * <p>Owns the token cache and per-credential refresh lock (the only stateful request-time
 * component): a warm token is reused with no token-endpoint I/O, an expired token is refreshed once
 * under a per-credential {@link ReentrantLock}, and a {@code 401} clears the store, refreshes once,
 * and replays once before giving up with {@link HttpApiException}.</p>
 */
public final class OAuth2RequestAuthenticator implements Authenticator {

    private static final int NOT_AN_ITEM = -1;

    private final TokenStore tokenStore;
    private final OAuth2TokenClient tokenClient;
    private final JsonMapper jsonMapper;
    private final long skewMillis;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public OAuth2RequestAuthenticator(final TokenStore tokenStore,
                                      final OAuth2TokenClient tokenClient) {
        this(tokenStore, tokenClient, JacksonJsonMapper.INSTANCE, 0L);
    }

    public OAuth2RequestAuthenticator(final TokenStore tokenStore,
                                      final OAuth2TokenClient tokenClient,
                                      final JsonMapper jsonMapper,
                                      final long skewMillis) {
        this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore");
        this.tokenClient = Objects.requireNonNull(tokenClient, "tokenClient");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
        this.skewMillis = skewMillis;
    }

    /**
     * Resolves a usable token for a credential: cache-hit returns it with no I/O; absent +
     * client-credentials/password acquires fresh; expired refreshes via the {@code refresh_token}
     * grant. Single-flight per credential id.
     *
     * @param credentialId the credential store id
     * @param creds        the OAuth2 client registration
     * @param grant        the configured grant flow
     * @return a valid, non-expired token
     */
    public OAuth2Token tokenFor(final String credentialId,
                                final OAuth2Credentials creds,
                                final OAuth2Grant grant) {
        Objects.requireNonNull(credentialId, "credentialId");
        Objects.requireNonNull(creds, "creds");
        Objects.requireNonNull(grant, "grant");
        final var lock = lockFor(credentialId);
        lock.lock();
        try {
            return tokenForLocked(credentialId, creds, grant);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Authenticates a request by injecting {@code Authorization: Bearer <access_token>}.
     *
     * @param credential the resolved OAuth2 credential
     * @param request    the request to authenticate
     * @return a new request carrying the bearer token
     */
    public HttpRequest authenticate(final DecryptedCredential<?> credential,
                                    final HttpRequest request) {
        return apply(credential, request, new AuthResult());
    }

    @Override
    public HttpRequest apply(final DecryptedCredential<?> credential,
                             final HttpRequest request,
                             final AuthResult out) {
        Objects.requireNonNull(credential, "credential");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(out, "out");
        final var creds = toCredentials(credential);
        final var token = tokenFor(credential.id(), creds, creds.grantType());
        out.markApplied(true);
        out.setCredentialId(credential.id());
        out.setTokenExpiresAt(token.expiresAt());
        return request.toBuilder()
                .auth(new Auth.BearerAuth(token.accessToken()))
                .build();
    }

    /**
     * Sends a request with OAuth2 auth; on a {@code 401} clears the store, refreshes once
     * (single-flight), and replays once. A repeated {@code 401} raises {@link HttpApiException}.
     *
     * @param credential the resolved OAuth2 credential
     * @param request    the request to send
     * @param transport  the transport for the protected request
     * @return the final {@link HttpResult}
     */
    public HttpResult retryOn401(final DecryptedCredential<?> credential,
                                 final HttpRequest request,
                                 final HttpTransport transport) {
        Objects.requireNonNull(credential, "credential");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(transport, "transport");
        final var first = transport.send(authenticate(credential, request));
        if (first.status() != 401) {
            return first;
        }
        final var refreshToken = tokenStore.load(credential.id())
                .map(OAuth2Token::refreshToken)
                .orElse(null);
        tokenStore.clear(credential.id());
        final var token = refreshOnce(credential.id(), toCredentials(credential), refreshToken);
        final var retry = request.toBuilder()
                .auth(new Auth.BearerAuth(token.accessToken()))
                .build();
        final var second = transport.send(retry);
        if (second.status() == 401) {
            throw new HttpApiException(401, null, NOT_AN_ITEM,
                    "OAuth2 credential '" + credential.id() + "' still returns 401 after refresh");
        }
        return second;
    }

    private OAuth2Token tokenForLocked(final String credentialId,
                                       final OAuth2Credentials creds,
                                       final OAuth2Grant grant) {
        final var cached = tokenStore.load(credentialId);
        if (cached.isPresent() && !cached.get().isExpired(skewMillis)) {
            return cached.get();
        }
        final var fresh = acquire(credentialId, creds, grant, cached.orElse(null));
        tokenStore.save(credentialId, fresh);
        return fresh;
    }

    private OAuth2Token acquire(final String credentialId,
                                final OAuth2Credentials creds,
                                final OAuth2Grant grant,
                                final OAuth2Token cached) {
        if (Objects.nonNull(cached) && Objects.nonNull(cached.refreshToken())) {
            return tokenClient.refresh(creds, cached.refreshToken());
        }
        return switch (grant) {
            case CLIENT_CREDENTIALS -> tokenClient.clientCredentials(creds);
            case PASSWORD -> tokenClient.password(
                    creds,
                    requireExtraParam(creds, "username"),
                    requireExtraParam(creds, "password")
            );
            case REFRESH_TOKEN, AUTHORIZATION_CODE -> throw new OAuth2NotConfiguredException(credentialId);
        };
    }

    private OAuth2Token refreshOnce(final String credentialId,
                                    final OAuth2Credentials creds,
                                    final String refreshToken) {
        final var lock = lockFor(credentialId);
        lock.lock();
        try {
            final var fresh = tokenStore.load(credentialId);
            if (fresh.isPresent() && !fresh.get().isExpired(skewMillis)) {
                return fresh.get();
            }
            if (refreshToken == null) {
                throw new HttpApiException(401, null, NOT_AN_ITEM,
                        "OAuth2 credential '" + credentialId + "' returned 401 but has no refresh token");
            }
            final var refreshed = tokenClient.refresh(creds, refreshToken);
            tokenStore.save(credentialId, refreshed);
            return refreshed;
        } finally {
            lock.unlock();
        }
    }

    private OAuth2Credentials toCredentials(final DecryptedCredential<?> credential) {
        return jsonMapper.fromMap(credential.fields(), OAuth2Credentials.class);
    }

    private ReentrantLock lockFor(final String credentialId) {
        return locks.computeIfAbsent(credentialId, id -> new ReentrantLock());
    }

    private static String requireExtraParam(final OAuth2Credentials creds,
                                            final String name) {
        final var value = creds.extraBodyParams().get(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    "PASSWORD grant requires '" + name + "' in extraBodyParams");
        }
        return String.valueOf(value);
    }
}
