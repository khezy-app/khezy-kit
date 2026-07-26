package io.github.khezyapp.ast.core.eval;

import io.github.khezyapp.ast.core.function.FunctionRegistry;
import io.github.khezyapp.ast.core.message.Message;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable context for AST evaluation, carrying the function registry,
 * input message (payload), evaluation cache, and configuration flags.
 * <p>
 * Created via the {@link Builder} which provides sensible defaults
 * (circuit-breaking and cost-optimization enabled, dry-run disabled,
 * system UTC clock).
 * </p>
 */
public final class EvaluationContext {
    private final FunctionRegistry registry;
    private final Message message;
    private final EvaluationCache cache;
    private final boolean circuitBreakingEnabled;
    private final boolean costOptimizationEnabled;
    private final boolean dryRun;
    private final Clock clock;

    private EvaluationContext(final Builder builder) {
        this.registry = Objects.requireNonNull(builder.registry);
        this.message = Objects.requireNonNullElse(builder.message, Message.of(Map.of()));
        this.cache = builder.cache;
        this.circuitBreakingEnabled = builder.circuitBreakingEnabled;
        this.costOptimizationEnabled = builder.costOptimizationEnabled;
        this.dryRun = builder.dryRun;
        this.clock = Objects.requireNonNullElse(builder.clock, Clock.systemUTC());
    }

    /**
     * Returns the function registry used for looking up function definitions.
     *
     * @return the function registry
     */
    public FunctionRegistry registry() {
        return registry;
    }

    /**
     * Returns the full message (headers + body).
     *
     * @return the message
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Returns the message body as a raw object.
     *
     * @return the message body
     */
    public Object getBody() {
        return message.getBody();
    }

    /**
     * Returns a specific header value.
     *
     * @param name the header name
     * @return the header value, or {@code null}
     */
    public Object getHeader(final String name) {
        return message.getHeader(name);
    }

    /**
     * Returns the evaluation cache, if any.
     *
     * @return the cache, or {@code null}
     */
    public EvaluationCache cache() {
        return cache;
    }

    /**
     * Returns whether circuit-breaking optimization is enabled.
     *
     * @return {@code true} if circuit-breaking is enabled
     */
    public boolean circuitBreakingEnabled() {
        return circuitBreakingEnabled;
    }

    /**
     * Returns whether cost-based optimization is enabled.
     *
     * @return {@code true} if cost optimization is enabled
     */
    public boolean costOptimizationEnabled() {
        return costOptimizationEnabled;
    }

    /**
     * Returns whether this evaluation is a dry run (no side effects).
     *
     * @return {@code true} if dry-run mode is active
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Returns the clock used for time-related functions.
     *
     * @return the clock
     */
    public Clock clock() {
        return clock;
    }

    /**
     * Retrieves a fact value from the message body (when body is a Map).
     *
     * @param name the fact key
     * @return the fact value, or {@code null}
     */
    public Object fact(final String name) {
        if (message.getBody() instanceof Map<?, ?> m) {
            return m.get(name);
        }
        return null;
    }

    /**
     * Returns a new context with circuit-breaking and cost optimization disabled.
     * Caching, dry-run flag, and clock are preserved.
     *
     * @return a new context without optimizations
     */
    public EvaluationContext withoutOptimizations() {
        return new Builder(registry)
                .message(message)
                .cache(cache)
                .circuitBreakingEnabled(false)
                .costOptimizationEnabled(false)
                .dryRun(dryRun)
                .clock(clock)
                .build();
    }

    /**
     * Fluent builder for {@link EvaluationContext}.
     * <p>
     * Only the {@link FunctionRegistry} is required; all other fields have
     * sensible defaults (empty message, no cache, optimizations enabled,
     * system clock).
     * </p>
     */
    public static final class Builder {
        private final FunctionRegistry registry;
        private Message message;
        private EvaluationCache cache;
        private boolean circuitBreakingEnabled = true;
        private boolean costOptimizationEnabled = true;
        private boolean dryRun = false;
        private Clock clock;

        /**
         * Creates a builder with the required function registry.
         *
         * @param registry the function registry (must not be null)
         */
        public Builder(final FunctionRegistry registry) {
            this.registry = Objects.requireNonNull(registry);
        }

        /**
         * Sets the input message.
         *
         * @param m the message
         * @return this builder
         */
        public Builder message(final Message m) {
            this.message = m;
            return this;
        }

        /**
         * Sets the message body, wrapping it in a default {@link Message}.
         *
         * @param body the message body
         * @return this builder
         */
        public Builder body(final Object body) {
            this.message = Message.of(body);
            return this;
        }

        /**
         * Sets the evaluation cache.
         *
         * @param c the cache
         * @return this builder
         */
        public Builder cache(final EvaluationCache c) {
            this.cache = c;
            return this;
        }

        /**
         * Enables or disables circuit-breaking optimization.
         *
         * @param v {@code true} to enable circuit-breaking (default)
         * @return this builder
         */
        public Builder circuitBreakingEnabled(final boolean v) {
            this.circuitBreakingEnabled = v;
            return this;
        }

        /**
         * Enables or disables cost-based optimization.
         *
         * @param v {@code true} to enable cost optimization (default)
         * @return this builder
         */
        public Builder costOptimizationEnabled(final boolean v) {
            this.costOptimizationEnabled = v;
            return this;
        }

        /**
         * Enables or disables dry-run mode.
         *
         * @param v {@code true} for dry-run mode (default false)
         * @return this builder
         */
        public Builder dryRun(final boolean v) {
            this.dryRun = v;
            return this;
        }

        /**
         * Sets the clock for time-related evaluators.
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Builds the evaluation context.
         *
         * @return a new {@link EvaluationContext}
         */
        public EvaluationContext build() {
            return new EvaluationContext(this);
        }
    }
}
