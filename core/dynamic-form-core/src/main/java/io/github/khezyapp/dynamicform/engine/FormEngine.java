package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.FieldAction;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.FormSchema;
import io.github.khezyapp.dynamicform.model.ResolvedForm;
import io.github.khezyapp.dynamicform.spi.ActionContext;
import io.github.khezyapp.dynamicform.spi.ActionHandler;
import io.github.khezyapp.dynamicform.spi.ActionHandlerRegistry;
import io.github.khezyapp.dynamicform.spi.ActionResult;
import io.github.khezyapp.dynamicform.spi.FileUploadProvider;
import io.github.khezyapp.dynamicform.spi.FileUploadProviderRegistry;
import io.github.khezyapp.dynamicform.spi.Option;
import io.github.khezyapp.dynamicform.spi.OptionRequest;
import io.github.khezyapp.dynamicform.spi.OptionsProviderRegistry;
import io.github.khezyapp.dynamicform.value.FormValues;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The stateless, pure facade over the form engine (P15).
 * <p>
 * A {@code FormEngine} holds an immutable {@link FormRuntime} (the consumer-implemented SPIs) and
 * resolves {@code (schema, values, context)} into {@code (values, issues)}. It is safe to share a
 * single instance across a REST request, a batch job, or a pre-render pass — the engine never holds
 * per-form state. The workflow engine never knows about forms: a clean resolve merges the values
 * into the context data bus and fires one {@code submit} event.
 */
public final class FormEngine {

    private final FormRuntime runtime;

    private FormEngine(final FormRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Creates an engine with empty option/action registries and the in-memory upload default.
     *
     * @return a default engine
     */
    public static FormEngine defaultEngine() {
        return new FormEngine(FormRuntime.defaults());
    }

    /**
     * Creates an engine wired to the given runtime.
     *
     * @param runtime the extension-point wiring
     * @return a new engine
     */
    public static FormEngine of(final FormRuntime runtime) {
        return new FormEngine(runtime);
    }

    /**
     * Resolves a form: visibility → defaults (dependency order) → coercion → validation.
     *
     * @param schema the form schema
     * @param raw    the raw submitted values
     * @param ctx    the evaluation context
     * @return the resolved values plus issues
     */
    public ResolvedForm resolve(final FormSchema schema,
                                final FormValues raw,
                                final EvalContext ctx) {
        return ResolveEngine.resolve(schema, raw, ctx, this.runtime);
    }

    /**
     * Resolves a form from a plain value map.
     *
     * @param schema the form schema
     * @param raw    the raw submitted values
     * @param ctx    the evaluation context
     * @return the resolved values plus issues
     */
    public ResolvedForm resolve(final FormSchema schema,
                                final Map<String, Object> raw,
                                final EvalContext ctx) {
        return resolve(schema, FormValues.of(raw), ctx);
    }

    /**
     * Resolves a form using an explicit upload backend, overriding any named providers.
     *
     * @param schema  the form schema
     * @param raw     the raw submitted values
     * @param ctx     the evaluation context
     * @param uploads the upload backend for FILE fields
     * @return the resolved values plus issues
     */
    public ResolvedForm resolve(final FormSchema schema,
                                final FormValues raw,
                                final EvalContext ctx,
                                final FileUploadProvider uploads) {
        final var registry = new FileUploadProviderRegistry(uploads);
        final var runtime = new FormRuntime(this.runtime.optionsRegistry(), registry, this.runtime.actionRegistry());
        return ResolveEngine.resolve(schema, raw, ctx, runtime);
    }

    /**
     * Loads the dynamic options of a provider-backed field against the current values. Cascading
     * options (e.g. country → state) read their dependency straight from {@code values}.
     *
     * @param field  the SELECT-like field
     * @param values the current form values
     * @param ctx    the evaluation context
     * @return the options served by the field's provider
     * @throws IllegalArgumentException when the field has no provider or none is registered
     */
    public List<Option> loadOptions(final FieldSchema field,
                                    final FormValues values,
                                    final EvalContext ctx) {
        final var options = field.options();
        if (Objects.isNull(options) || Objects.isNull(options.provider())) {
            throw new IllegalArgumentException("field '" + field.name() + "' has no options provider");
        }
        final var provider = this.runtime.optionsRegistry().get(options.provider())
                .orElseThrow(() -> new IllegalArgumentException(
                        "no OptionsProvider registered for '" + options.provider() + "'"));
        return provider.load(OptionRequest.of(values, ctx));
    }

    /**
     * Invokes a declared field action through its registered handler.
     *
     * @param action the declared action
     * @param field  the owning field
     * @param values the current form values
     * @param ctx    the evaluation context
     * @return the handler's result
     * @throws IllegalArgumentException when no handler is registered for the action
     */
    public ActionResult invokeAction(final FieldAction action,
                                     final FieldSchema field,
                                     final FormValues values,
                                     final EvalContext ctx) {
        final var handler = handlerFor(action);
        return handler.handle(new ActionContext(action, field, values, ctx));
    }

    /**
     * The options provider registry (register providers here).
     *
     * @return the registry
     */
    public OptionsProviderRegistry optionsRegistry() {
        return this.runtime.optionsRegistry();
    }

    /**
     * The file upload provider registry (register providers here).
     *
     * @return the registry
     */
    public FileUploadProviderRegistry uploadRegistry() {
        return this.runtime.uploadRegistry();
    }

    /**
     * The action handler registry (register handlers here).
     *
     * @return the registry
     */
    public ActionHandlerRegistry actionRegistry() {
        return this.runtime.actionRegistry();
    }

    private ActionHandler handlerFor(final FieldAction action) {
        return this.runtime.actionRegistry().get(action.handler())
                .orElseThrow(() -> new IllegalArgumentException(
                        "no ActionHandler registered for '" + action.handler() + "'"));
    }
}
