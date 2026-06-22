package io.github.khezyapp.templates.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Security configuration for shell command execution.
 * <p>
 * Maintains a blocklist of dangerous command patterns and a configurable
 * timeout. The default blocklist covers destructive operations (rm -rf,
 * mkfs, dd, fork bomb) and network access (wget, curl).
 * <p>
 * Use the {@link Builder} to customise patterns and timeout.
 */
public final class SecurityConfig {

    private final Set<String> blockedPatterns;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private SecurityConfig(final Builder builder) {
        this.blockedPatterns = Collections.unmodifiableSet(new HashSet<>(builder.blockedPatterns));
        this.timeout = builder.timeout;
        this.timeoutUnit = builder.timeoutUnit;
    }

    /**
     * Returns the set of blocked command patterns (unmodifiable).
     *
     * @return blocked patterns
     */
    public Set<String> blockedPatterns() {
        return blockedPatterns;
    }

    /**
     * Returns the command execution timeout value.
     *
     * @return timeout value
     */
    public long timeout() {
        return timeout;
    }

    /**
     * Returns the time unit for the timeout.
     *
     * @return timeout time unit
     */
    public TimeUnit timeoutUnit() {
        return timeoutUnit;
    }

    /**
     * Checks whether a command contains any blocked pattern.
     *
     * @param command the shell command to check
     * @return true if the command is blocked
     */
    public boolean isBlocked(final String command) {
        for (final var pattern : blockedPatterns) {
            if (command.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a new {@link Builder} for creating a SecurityConfig.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SecurityConfig}.
     * <p>
     * Pre-populated with a default blocklist of dangerous command patterns
     * and a 30-second timeout.
     */
    public static final class Builder {

        private final Set<String> blockedPatterns = new HashSet<>();
        private long timeout = 30;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;

        Builder() {
            blockedPatterns.add("rm -rf /");
            blockedPatterns.add("rm -rf /*");
            blockedPatterns.add("mkfs");
            blockedPatterns.add("dd if=");
            blockedPatterns.add(":(){");
            blockedPatterns.add("> /dev/sda");
            blockedPatterns.add("mv /");
            blockedPatterns.add("wget");
            blockedPatterns.add("curl");
            blockedPatterns.add("chmod -R 000");
        }

        /**
         * Adds command patterns to the blocklist.
         *
         * @param patterns patterns to block (commands containing any pattern
         *                 will be rejected)
         * @return this builder
         */
        public Builder blockCommands(final String... patterns) {
            for (final var pattern : patterns) {
                blockedPatterns.add(Objects.requireNonNull(pattern));
            }
            return this;
        }

        /**
         * Sets the command execution timeout.
         *
         * @param timeout the timeout value
         * @param unit    the time unit
         * @return this builder
         */
        public Builder timeout(final long timeout, final TimeUnit unit) {
            this.timeout = timeout;
            this.timeoutUnit = Objects.requireNonNull(unit);
            return this;
        }

        /**
         * Builds the {@link SecurityConfig}.
         *
         * @return a new SecurityConfig instance
         */
        public SecurityConfig build() {
            return new SecurityConfig(this);
        }
    }
}
