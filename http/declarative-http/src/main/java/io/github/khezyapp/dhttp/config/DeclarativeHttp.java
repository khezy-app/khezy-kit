package io.github.khezyapp.dhttp.config;

import io.github.khezyapp.dhttp.auth.credential.CredentialService;
import io.github.khezyapp.dhttp.engine.DeclarativeHttpEngine;
import io.github.khezyapp.dhttp.engine.OptionPage;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;

import java.util.List;
import java.util.Objects;

/**
 * Top-level facade for the declarative HTTP core (§2 comment "engine + validate"): consumers wire
 * everything once through {@link DeclarativeHttpConfig} and drive the engine through this thin
 * delegation layer.
 */
public final class DeclarativeHttp {

    private final DeclarativeHttpConfig config;

    private DeclarativeHttp(final DeclarativeHttpConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * @param config the assembled configuration
     * @return a facade wrapping the configured engine
     */
    public static DeclarativeHttp create(final DeclarativeHttpConfig config) {
        return new DeclarativeHttp(config);
    }

    /**
     * @return a config builder, the entry point for wiring the facade
     */
    public static DeclarativeHttpConfig.Builder builder() {
        return DeclarativeHttpConfig.builder();
    }

    /**
     * Config-time validation, delegated to the engine (R10): resolves the spec's default credential
     * and, for {@code oauth2}, verifies a valid access token is stored.
     *
     * @param spec the spec to validate
     */
    public void validate(final HttpRequestSpec spec) {
        config.engine().validate(spec);
    }

    /**
     * Executes the operation matching the context, delegated to the engine.
     *
     * @param spec the root spec
     * @param ctx  the per-item context
     * @return the shaped output records
     */
    public List<OutputRecord> execute(final HttpRequestSpec spec,
                                      final RequestContext ctx) {
        return config.engine().execute(spec, ctx);
    }

    /**
     * Executes one operation over many items with optional batching pacing (§10.2), delegated to
     * the engine.
     *
     * @param spec     the root spec
     * @param contexts the per-item contexts
     * @return the combined shaped output records
     */
    public List<OutputRecord> executeAll(final HttpRequestSpec spec,
                                         final List<RequestContext> contexts) {
        return config.engine().executeAll(spec, contexts);
    }

    /**
     * Design-time metadata mode (R15), delegated to the engine.
     *
     * @param spec    the root spec
     * @param ctx     the design-time context
     * @param loadKey the registered option-shaping action key
     * @return the shaped options for a dropdown plus paging state
     */
    public OptionPage describe(final HttpRequestSpec spec,
                               final RequestContext ctx,
                               final String loadKey) {
        return config.engine().describe(spec, ctx, loadKey);
    }

    /**
     * @return the assembled engine
     */
    public DeclarativeHttpEngine engine() {
        return config.engine();
    }

    /**
     * @return the credential CRUD service, or {@code null} whens only a raw store was injected
     */
    public CredentialService credentialService() {
        return config.credentialService();
    }
}
