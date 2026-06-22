package io.github.khezyapp.templates.resolver;

import io.github.khezyapp.templates.TemplateContext;
import io.github.khezyapp.templates.plugin.PluginRegistry;
import io.github.khezyapp.templates.runner.ShellExecutionException;
import io.github.khezyapp.templates.runner.ShellRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves shell command placeholders of the form {@code !`command`}.
 * <p>
 * Each placeholder is executed via the configured {@link ShellRunner}.
 * Plugin hooks ({@code beforeShellRun}, {@code afterShellRun},
 * {@code onResolveError}) are fired at each stage.
 * <p>
 * Thread-safe: uses {@link ThreadLocal} to keep per-thread command and
 * error lists isolated.
 */
public final class ShellPlaceholderResolver implements PlaceholderResolver {

    private static final Pattern SHELL_PATTERN = Pattern.compile("!`([^`]*)`");

    private final ShellRunner shellRunner;
    private final PluginRegistry pluginRegistry;
    private final ThreadLocal<List<String>> executedCommands;
    private final ThreadLocal<List<String>> errors;

    /**
     * Creates a new ShellPlaceholderResolver.
     *
     * @param shellRunner    the shell runner to execute commands
     * @param pluginRegistry the plugin registry for lifecycle hooks
     */
    public ShellPlaceholderResolver(
            final ShellRunner shellRunner,
            final PluginRegistry pluginRegistry
    ) {
        this.shellRunner = shellRunner;
        this.pluginRegistry = pluginRegistry;
        this.executedCommands = ThreadLocal.withInitial(ArrayList::new);
        this.errors = ThreadLocal.withInitial(ArrayList::new);
    }

    @Override
    public String resolve(final String template,
                          final TemplateContext ctx) {
        final var commands = executedCommands.get();
        final var errs = errors.get();
        commands.clear();
        errs.clear();

        final var matcher = SHELL_PATTERN.matcher(template);
        final var sb = new StringBuffer();

        while (matcher.find()) {
            final var fullMatch = matcher.group(0);
            final var command = matcher.group(1);

            if (!pluginRegistry.fireBeforeShellRun(command)) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(fullMatch));
                continue;
            }

            try {
                final var output = shellRunner.run(command);
                final var processedOutput = pluginRegistry.fireAfterShellRun(command, output);
                commands.add(command);

                final var trimmed = processedOutput.stripTrailing();
                matcher.appendReplacement(sb, Matcher.quoteReplacement(trimmed));
            } catch (final ShellExecutionException e) {
                pluginRegistry.fireOnResolveError(fullMatch, e);
                errs.add(fullMatch + ": " + e.getMessage());
                matcher.appendReplacement(sb, Matcher.quoteReplacement(fullMatch));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Returns the shell commands that were executed in the current thread's
     * most recent resolution.
     *
     * @return list of executed commands
     */
    public List<String> executedCommands() {
        return List.copyOf(executedCommands.get());
    }

    /**
     * Returns the errors that occurred in the current thread's most recent
     * resolution.
     *
     * @return list of error messages
     */
    public List<String> errors() {
        return List.copyOf(errors.get());
    }
}
