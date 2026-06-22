package io.github.khezyapp.templates;

import io.github.khezyapp.templates.config.TemplateConfig;
import io.github.khezyapp.templates.plugin.PluginRegistry;
import io.github.khezyapp.templates.resolver.ResolverChain;
import java.util.List;
import java.util.Objects;

/**
 * Main entry point for the prompt template resolution engine.
 * <p>
 * Accepts a {@link TemplateConfig} that wires together the resolver chain,
 * plugin registry, shell runner, and security configuration. Each call to
 * {@link #resolve(String, String...)} fires plugin lifecycle hooks and
 * delegates to the configured {@link ResolverChain}.
 */
public final class TemplateEngine {

    private final ResolverChain resolverChain;
    private final PluginRegistry pluginRegistry;

    /**
     * Creates a new TemplateEngine from the given configuration.
     *
     * @param config aggregated configuration (must have non-null
     *               resolverChain and pluginRegistry)
     */
    public TemplateEngine(final TemplateConfig config) {
        this.resolverChain = Objects.requireNonNull(config.resolverChain());
        this.pluginRegistry = Objects.requireNonNull(config.pluginRegistry());
    }

    /**
     * Resolves a template with the given varargs arguments.
     *
     * @param template the template string to resolve
     * @param args     positional argument values
     * @return the resolution result
     */
    public TemplateResult resolve(final String template,
                                  final String... args) {
        return resolve(template, List.of(args));
    }

    /**
     * Resolves a template with the given list of arguments.
     * <p>
     * Fires {@code beforeResolve} plugins (which may block resolution),
     * runs the resolver chain, then fires {@code afterResolve} plugins.
     *
     * @param template the template string to resolve
     * @param args     positional argument values
     * @return the resolution result
     */
    public TemplateResult resolve(final String template,
                                  final List<String> args) {
        Objects.requireNonNull(template);
        Objects.requireNonNull(args);

        final var ctx = new TemplateContext(String.join(" ", args), args);

        if (!pluginRegistry.fireBeforeResolve(ctx)) {
            return new TemplateResult(template, List.of(), List.of("Blocked by plugin"));
        }

        final var result = resolverChain.resolve(template, ctx);

        pluginRegistry.fireAfterResolve(result);

        return result;
    }
}
