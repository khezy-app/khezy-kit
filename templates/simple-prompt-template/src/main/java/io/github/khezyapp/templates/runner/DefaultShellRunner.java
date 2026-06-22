package io.github.khezyapp.templates.runner;

import io.github.khezyapp.templates.config.SecurityConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Default shell command runner that executes commands via
 * {@link ProcessBuilder} with {@code sh -c}.
 * <p>
 * Checks the command against the configured {@link SecurityConfig} blocklist,
 * enforces a configurable timeout, captures merged stdout/stderr, and throws
 * {@link ShellExecutionException} on non-zero exit codes.
 */
public final class DefaultShellRunner implements ShellRunner {

    private final SecurityConfig securityConfig;

    /**
     * Creates a new DefaultShellRunner with the given security config.
     *
     * @param securityConfig the security configuration
     */
    public DefaultShellRunner(final SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public String run(final String command) throws ShellExecutionException {
        if (securityConfig.isBlocked(command)) {
            throw new ShellExecutionException("Command blocked by security policy: " + command);
        }

        try {
            final var process = new ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            final var finished = process.waitFor(securityConfig.timeout(), securityConfig.timeoutUnit());
            if (!finished) {
                process.destroyForcibly();
                throw new ShellExecutionException("Command timed out: " + command);
            }

            final var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final var exitCode = process.exitValue();

            if (exitCode != 0) {
                throw new ShellExecutionException(
                        "Command failed with exit code " + exitCode + ": " + command
                );
            }

            return output;
        } catch (final IOException e) {
            throw new ShellExecutionException("Failed to execute command: " + command, e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShellExecutionException("Command was interrupted: " + command, e);
        }
    }
}
