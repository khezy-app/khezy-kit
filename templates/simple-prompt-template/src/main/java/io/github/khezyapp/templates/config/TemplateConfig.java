package io.github.khezyapp.templates.config;

import io.github.khezyapp.templates.plugin.PluginRegistry;
import io.github.khezyapp.templates.resolver.ResolverChain;
import io.github.khezyapp.templates.runner.ShellRunner;
import java.util.Objects;

/**
 * Aggregated configuration for the template engine.
 * <p>
 * Wires together the resolver chain, plugin registry, shell runner, and
 * security configuration. All fields are required (validated via
 * {@link Objects#requireNonNull}).
 * <p>
 * Use the {@link Builder} to construct an instance.
 */
public final class TemplateConfig {

    private final ResolverChain resolverChain;
    private final PluginRegistry pluginRegistry;
    private final ShellRunner shellRunner;
    private final SecurityConfig securityConfig;

    private TemplateConfig(final Builder builder) {
        this.resolverChain = Objects.requireNonNull(builder.resolverChain);
        this.pluginRegistry = Objects.requireNonNull(builder.pluginRegistry);
        this.shellRunner = Objects.requireNonNull(builder.shellRunner);
        this.securityConfig = Objects.requireNonNull(builder.securityConfig);
    }

    /**
     * Returns the configured resolver chain.
     *
     * @return resolver chain
     */
    public ResolverChain resolverChain() {
        return resolverChain;
    }

    /**
     * Returns the configured plugin registry.
     *
     * @return plugin registry
     */
    public PluginRegistry pluginRegistry() {
        return pluginRegistry;
    }

    /**
     * Returns the configured shell runner.
     *
     * @return shell runner
     */
    public ShellRunner shellRunner() {
        return shellRunner;
    }

    /**
     * Returns the configured security configuration.
     *
     * @return security config
     */
    public SecurityConfig securityConfig() {
        return securityConfig;
    }

    /**
     * Returns a new {@link Builder} for creating a TemplateConfig.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TemplateConfig}.
     * <p>
     * All fields must be set before calling {@link #build()}.
     */
    public static final class Builder {

        private ResolverChain resolverChain;
        private PluginRegistry pluginRegistry;
        private ShellRunner shellRunner;
        private SecurityConfig securityConfig;

        Builder() {
        }

        /**
         * Sets the resolver chain.
         *
         * @param resolverChain the resolver chain
         * @return this builder
         */
        public Builder resolverChain(final ResolverChain resolverChain) {
            this.resolverChain = resolverChain;
            return this;
        }

        /**
         * Sets the plugin registry.
         *
         * @param pluginRegistry the plugin registry
         * @return this builder
         */
        public Builder pluginRegistry(final PluginRegistry pluginRegistry) {
            this.pluginRegistry = pluginRegistry;
            return this;
        }

        /**
         * Sets the shell runner.
         *
         * @param shellRunner the shell runner
         * @return this builder
         */
        public Builder shellRunner(final ShellRunner shellRunner) {
            this.shellRunner = shellRunner;
            return this;
        }

        /**
         * Sets the security configuration.
         *
         * @param securityConfig the security config
         * @return this builder
         */
        public Builder securityConfig(final SecurityConfig securityConfig) {
            this.securityConfig = securityConfig;
            return this;
        }

        /**
         * Builds the {@link TemplateConfig}.
         *
         * @return a new TemplateConfig instance
         */
        public TemplateConfig build() {
            return new TemplateConfig(this);
        }
    }
}
