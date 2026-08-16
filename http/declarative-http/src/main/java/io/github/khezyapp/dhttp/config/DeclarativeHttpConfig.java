package io.github.khezyapp.dhttp.config;

import io.github.khezyapp.cert.ClientTlsConfig;
import io.github.khezyapp.dhttp.action.ActionRegistry;
import io.github.khezyapp.dhttp.auth.credential.AesGcmCredentialCipher;
import io.github.khezyapp.dhttp.auth.credential.CredentialService;
import io.github.khezyapp.dhttp.auth.credential.CredentialStore;
import io.github.khezyapp.dhttp.auth.credential.InMemoryCredentialRepository;
import io.github.khezyapp.dhttp.auth.credential.KeyProvider;
import io.github.khezyapp.dhttp.auth.oauth2.InMemoryTokenStore;
import io.github.khezyapp.dhttp.auth.oauth2.TokenStore;
import io.github.khezyapp.dhttp.engine.DeclarativeHttpEngine;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.pagination.PaginationRegistry;
import io.github.khezyapp.dhttp.pagination.PaginationStrategyFactory;
import io.github.khezyapp.dhttp.transport.HttpTransport;
import io.github.khezyapp.dhttp.transport.jdk.JdkHttpTransport;

import java.util.Objects;

/**
 * Immutable assembled configuration for the {@link DeclarativeHttpEngine} (§2, §6.2): every
 * dependency the engine needs is wired exactly once here and exposed through accessors.
 *
 * <p>Builders may inject a prebuilt {@link CredentialService} (which also wires the engine-facing
 * {@link CredentialStore} via {@code asStore()}) or only a {@link KeyProvider} — in which case a
 * default cipher-backed service over an in-memory repository is constructed. The core never
 * generates or defaults a master key, so a {@code build()} that can reach neither a service nor a
 * key fails fast.</p>
 */
public final class DeclarativeHttpConfig {

    private final JsonMapper jsonMapper;
    private final HttpTransport transport;
    private final ExpressionEvaluator evaluator;
    private final TokenStore tokenStore;
    private final CredentialStore credentialStore;
    private final CredentialService credentialService;
    private final DeclarativeHttpEngine engine;

    private DeclarativeHttpConfig(final JsonMapper jsonMapper,
                                  final HttpTransport transport,
                                  final ExpressionEvaluator evaluator,
                                  final TokenStore tokenStore,
                                  final CredentialStore credentialStore,
                                  final CredentialService credentialService,
                                  final DeclarativeHttpEngine engine) {
        this.jsonMapper = jsonMapper;
        this.transport = transport;
        this.evaluator = evaluator;
        this.tokenStore = tokenStore;
        this.credentialStore = credentialStore;
        this.credentialService = credentialService;
        this.engine = engine;
    }

    /**
     * @return the JSON mapper shared by planning, shaping, and the credential cipher
     */
    public JsonMapper jsonMapper() {
        return jsonMapper;
    }

    /**
     * @return the transport used for every send (including the OAuth2 token endpoint)
     */
    public HttpTransport transport() {
        return transport;
    }

    /**
     * @return the expression evaluator used for {@code =} and {@code {{...}}} values
     */
    public ExpressionEvaluator evaluator() {
        return evaluator;
    }

    /**
     * @return the token store backing the OAuth2 request-time lifecycle
     */
    public TokenStore tokenStore() {
        return tokenStore;
    }

    /**
     * @return the engine-facing read-side credential store (never performs CRUD)
     */
    public CredentialStore credentialStore() {
        return credentialStore;
    }

    /**
     * @return the configuration CRUD service, or {@code null} whens only a raw
     *         {@link CredentialStore} was injected
     */
    public CredentialService credentialService() {
        return credentialService;
    }

    /**
     * @return the assembled engine
     */
    public DeclarativeHttpEngine engine() {
        return engine;
    }

    /**
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent assembler for {@link DeclarativeHttpConfig}. The transport defaults to a real
     * {@link JdkHttpTransport}; credentials may arrive as a prebuilt {@link CredentialService}, a
     * raw {@link CredentialStore}, or a {@link KeyProvider} used to build a default cipher-backed
     * service.
     */
    public static final class Builder {

        private JsonMapper jsonMapper;
        private HttpTransport transport;
        private ExpressionEvaluator evaluator;
        private TokenStore tokenStore;
        private CredentialStore credentialStore;
        private CredentialService credentialService;
        private KeyProvider keyProvider;
        private ActionRegistry registry;
        private PaginationRegistry paginationRegistry;
        private ClientTlsConfig tlsConfig;

        /**
         * @param jsonMapper the JSON mapper to share; defaults to {@link JacksonJsonMapper#INSTANCE}
         * @return this builder
         */
        public Builder jsonMapper(final JsonMapper jsonMapper) {
            this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
            return this;
        }

        /**
         * @param registry the post-receive action registry; defaults to
         *                 {@link ActionRegistry#withBuiltins()} so custom option-shaping actions
         *                 can be registered before build
         * @return this builder
         */
        public Builder registry(final ActionRegistry registry) {
            this.registry = Objects.requireNonNull(registry, "registry");
            return this;
        }

        /**
         * @param paginationRegistry the pagination registry used to materialize route strategies;
         *                           defaults to {@link PaginationRegistry#withBuiltins()} so custom
         *                           strategies can be registered before build
         * @return this builder
         */
        public Builder pagination(final PaginationRegistry paginationRegistry) {
            this.paginationRegistry = Objects.requireNonNull(paginationRegistry, "paginationRegistry");
            return this;
        }

        /**
         * Registers a custom pagination strategy factory under a mode, so a
         * {@code PaginationSpec} with that {@code mode} resolves it. The effective registry
         * defaults to {@link PaginationRegistry#withBuiltins()}, so built-in modes keep working
         * alongside the registered one.
         *
         * @param mode    the pagination mode string used by {@code PaginationSpec.mode}
         * @param factory the factory building the strategy
         * @return this builder, for chaining
         */
        public Builder registerPagination(final String mode,
                                          final PaginationStrategyFactory factory) {
            if (Objects.isNull(paginationRegistry)) {
                paginationRegistry = PaginationRegistry.withBuiltins();
            }
            paginationRegistry.register(mode, factory);
            return this;
        }

        /**
         * @param transport the transport for every send; defaults to a real
         *                  {@link io.github.khezyapp.dhttp.transport.jdk.JdkHttpTransport} whens not
         *                  set (§10.1)
         * @return this builder
         */
        public Builder transport(final HttpTransport transport) {
            this.transport = Objects.requireNonNull(transport, "transport");
            return this;
        }

        /**
         * Configures the default transport to present this mTLS client identity on every send
         * (high-security mTLS deployments). Ignored whens a custom {@link #transport(HttpTransport)}
         * is supplied, since the caller then owns transport configuration.
         *
         * @param tlsConfig the ASCII client identity (certificate chain + private key)
         * @return this builder
         */
        public Builder tlsConfig(final ClientTlsConfig tlsConfig) {
            this.tlsConfig = Objects.requireNonNull(tlsConfig, "tlsConfig");
            return this;
        }

        /**
         * @param evaluator the expression evaluator; defaults to a fresh
         *                  {@link JexlExpressionEvaluator}
         * @return this builder
         */
        public Builder evaluator(final ExpressionEvaluator evaluator) {
            this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
            return this;
        }

        /**
         * @param tokenStore the OAuth2 token store; defaults to a fresh
         *                   {@link InMemoryTokenStore}
         * @return this builder
         */
        public Builder tokenStore(final TokenStore tokenStore) {
            this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore");
            return this;
        }

        /**
         * Wires the configuration CRUD service and, via {@code asStore()}, the engine-facing
         * {@link CredentialStore} (§6.2).
         *
         * @param service the credential service
         * @return this builder
         */
        public Builder credentialService(final CredentialService service) {
            this.credentialService = Objects.requireNonNull(service, "service");
            this.credentialStore = service.asStore();
            return this;
        }

        /**
         * @param credentialStore the engine-facing credential store, bypassing the CRUD service
         * @return this builder
         */
        public Builder credentialStore(final CredentialStore credentialStore) {
            this.credentialStore = Objects.requireNonNull(credentialStore, "credentialStore");
            return this;
        }

        /**
         * Supplies the master key used to build the default cipher-backed credential service
         * (§7.6). The core never generates, stores, or defaults a key.
         *
         * @param keyProvider the consumer-owned master key provider
         * @return this builder
         */
        public Builder keyProvider(final KeyProvider keyProvider) {
            this.keyProvider = Objects.requireNonNull(keyProvider, "keyProvider");
            return this;
        }

        /**
         * Assembles the immutable {@link DeclarativeHttpConfig}.
         *
         * @return the assembled config
         * @throws IllegalArgumentException whens neither a credential service, key provider, nor raw
         *         credential store is present
         */
        public DeclarativeHttpConfig build() {
            final var mapper = Objects.isNull(jsonMapper) ? JacksonJsonMapper.INSTANCE : jsonMapper;
            final var expr = Objects.isNull(evaluator) ? new JexlExpressionEvaluator() : evaluator;
            final var tokens = Objects.isNull(tokenStore) ? new InMemoryTokenStore() : tokenStore;
            final var wire = Objects.isNull(transport) ? defaultTransport() : transport;
            if (Objects.isNull(credentialService) && Objects.isNull(keyProvider)
                    && Objects.isNull(credentialStore)) {
                throw new IllegalArgumentException(
                        "a keyProvider (or a prebuilt credentialService/credentialStore) is required: "
                                + "the core never generates a master key");
            }
            final var service = Objects.isNull(credentialService) ? defaultService(mapper, keyProvider)
                    : credentialService;
            final var store = Objects.isNull(credentialStore) ? service.asStore() : credentialStore;
            final var actions = Objects.isNull(registry) ? ActionRegistry.withBuiltins() : registry;
            final var pageModes = Objects.isNull(paginationRegistry)
                    ? PaginationRegistry.withBuiltins()
                    : paginationRegistry;
            final var engine = new DeclarativeHttpEngine(actions, pageModes, store,
                    wire, expr, mapper, tokens);
            return new DeclarativeHttpConfig(mapper, wire, expr, tokens, store, service, engine);
        }

        private static CredentialService defaultService(final JsonMapper mapper,
                                                        final KeyProvider keyProvider) {
            return new CredentialService(new InMemoryCredentialRepository(),
                    new AesGcmCredentialCipher(keyProvider, mapper), mapper);
        }

        private HttpTransport defaultTransport() {
            return Objects.isNull(tlsConfig) ? new JdkHttpTransport() : new JdkHttpTransport(tlsConfig);
        }
    }
}
