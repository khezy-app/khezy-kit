package io.github.khezyapp.templates.plugin;

import io.github.khezyapp.templates.TemplateContext;
import io.github.khezyapp.templates.TemplateResult;
import java.util.Optional;

/**
 * Rich context object for plugin lifecycle notifications.
 * <p>
 * All fields are wrapped in {@link Optional} and may be absent depending
 * on the event type. Use the {@link Builder} to construct an instance.
 */
public final class PluginContext {

    private final PluginEvent event;
    private final TemplateContext templateContext;
    private final TemplateResult templateResult;
    private final String command;
    private final String commandOutput;
    private final String placeholder;
    private final Exception error;

    private PluginContext(final Builder builder) {
        this.event = builder.event;
        this.templateContext = builder.templateContext;
        this.templateResult = builder.templateResult;
        this.command = builder.command;
        this.commandOutput = builder.commandOutput;
        this.placeholder = builder.placeholder;
        this.error = builder.error;
    }

    /**
     * Returns the lifecycle event this context is associated with.
     *
     * @return the plugin event
     */
    public PluginEvent event() {
        return event;
    }

    /**
     * Returns the template context if applicable to this event.
     *
     * @return optional template context
     */
    public Optional<TemplateContext> templateContext() {
        return Optional.ofNullable(templateContext);
    }

    /**
     * Returns the template result if applicable to this event.
     *
     * @return optional template result
     */
    public Optional<TemplateResult> templateResult() {
        return Optional.ofNullable(templateResult);
    }

    /**
     * Returns the shell command if applicable to this event.
     *
     * @return optional command string
     */
    public Optional<String> command() {
        return Optional.ofNullable(command);
    }

    /**
     * Returns the command output if applicable to this event.
     *
     * @return optional command output
     */
    public Optional<String> commandOutput() {
        return Optional.ofNullable(commandOutput);
    }

    /**
     * Returns the original placeholder text if applicable to this event.
     *
     * @return optional placeholder
     */
    public Optional<String> placeholder() {
        return Optional.ofNullable(placeholder);
    }

    /**
     * Returns the error if applicable to this event.
     *
     * @return optional error
     */
    public Optional<Exception> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Returns a new {@link Builder} for creating a PluginContext.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PluginContext}.
     */
    public static final class Builder {

        private PluginEvent event;
        private TemplateContext templateContext;
        private TemplateResult templateResult;
        private String command;
        private String commandOutput;
        private String placeholder;
        private Exception error;

        Builder() {
        }

        /**
         * Sets the lifecycle event.
         *
         * @param event the plugin event
         * @return this builder
         */
        public Builder event(final PluginEvent event) {
            this.event = event;
            return this;
        }

        /**
         * Sets the template context.
         *
         * @param templateContext the template context
         * @return this builder
         */
        public Builder templateContext(final TemplateContext templateContext) {
            this.templateContext = templateContext;
            return this;
        }

        /**
         * Sets the template result.
         *
         * @param templateResult the template result
         * @return this builder
         */
        public Builder templateResult(final TemplateResult templateResult) {
            this.templateResult = templateResult;
            return this;
        }

        /**
         * Sets the shell command.
         *
         * @param command the command string
         * @return this builder
         */
        public Builder command(final String command) {
            this.command = command;
            return this;
        }

        /**
         * Sets the command output.
         *
         * @param commandOutput the command output
         * @return this builder
         */
        public Builder commandOutput(final String commandOutput) {
            this.commandOutput = commandOutput;
            return this;
        }

        /**
         * Sets the placeholder text.
         *
         * @param placeholder the placeholder text
         * @return this builder
         */
        public Builder placeholder(final String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Sets the error.
         *
         * @param error the exception
         * @return this builder
         */
        public Builder error(final Exception error) {
            this.error = error;
            return this;
        }

        /**
         * Builds the {@link PluginContext}.
         *
         * @return a new PluginContext instance
         */
        public PluginContext build() {
            return new PluginContext(this);
        }
    }
}
