package io.github.khezyapp.templates.runner;

/**
 * Strategy interface for executing shell commands.
 * <p>
 * Implementations are responsible for running a command string in a shell
 * environment and returning its stdout output. The default implementation
 * uses {@link ProcessBuilder} with {@code sh -c}.
 */
public interface ShellRunner {

    /**
     * Executes a shell command and returns its output.
     *
     * @param command the shell command to execute
     * @return the command's stdout output
     * @throws ShellExecutionException if execution fails, times out, or
     *                                 returns a non-zero exit code
     */
    String run(String command) throws ShellExecutionException;
}
