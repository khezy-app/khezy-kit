package io.github.khezyapp.templates.resolver;

import io.github.khezyapp.templates.EscapeUtils;
import io.github.khezyapp.templates.TemplateContext;
import io.github.khezyapp.templates.TemplateResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrates a sequence of {@link PlaceholderResolver} stages.
 * <p>
 * Before running the resolvers, special characters are escaped via
 * {@link EscapeUtils#escape} so that backslash-escaped {@code $} and
 * {@code !} survive resolution. After all resolvers have run, the
 * escaped characters are restored via {@link EscapeUtils#unescape}.
 * <p>
 * Shell commands and errors are collected from any
 * {@link ShellPlaceholderResolver} in the chain.
 */
public final class ResolverChain {

    private final List<PlaceholderResolver> resolvers;

    /**
     * Creates a new ResolverChain with the given resolvers.
     *
     * @param resolvers list of placeholder resolvers (will be defensively copied)
     */
    public ResolverChain(final List<PlaceholderResolver> resolvers) {
        this.resolvers = Collections.unmodifiableList(new ArrayList<>(resolvers));
    }

    /**
     * Resolves a template by running it through all registered resolvers.
     *
     * @param template the template string to resolve
     * @param ctx      the template context
     * @return the resolution result
     */
    public TemplateResult resolve(final String template,
                                  final TemplateContext ctx) {
        var escaped = EscapeUtils.escape(template);

        for (final var resolver : resolvers) {
            escaped = resolver.resolve(escaped, ctx);
        }

        final var resolved = EscapeUtils.unescape(escaped);

        final var commands = new ArrayList<String>();
        final var errors = new ArrayList<String>();
        for (final var resolver : resolvers) {
            if (resolver instanceof final ShellPlaceholderResolver shell) {
                commands.addAll(shell.executedCommands());
                errors.addAll(shell.errors());
            }
        }

        return new TemplateResult(resolved, commands, errors);
    }
}
