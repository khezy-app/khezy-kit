package io.github.khezyapp.templates.resolver;

import io.github.khezyapp.templates.TemplateContext;

/**
 * Strategy interface for resolving placeholders within a template string.
 * <p>
 * Implementations are composed into a {@link ResolverChain} and run in
 * sequence. Each resolver transforms the template and passes the result
 * to the next resolver in the chain.
 */
public interface PlaceholderResolver {

    /**
     * Resolves placeholders in the given template using the provided context.
     *
     * @param template the template string (may already be partially resolved
     *                 by earlier resolvers in the chain)
     * @param ctx      the template context with positional arguments
     * @return the template with placeholders resolved
     */
    String resolve(String template, TemplateContext ctx);
}
