package io.github.khezyapp.dynamicform.spi;

import io.github.khezyapp.dynamicform.engine.EvalContext;
import io.github.khezyapp.dynamicform.value.FormValues;

/**
 * The request handed to an {@link OptionsProvider}.
 * <p>
 * The provider receives the <strong>current values</strong> of the form so that cascading options
 * are natural — a {@code stateList} provider reads {@code currentValues().get("country")} to filter
 * the states. {@code filter} and pagination fields are optional hints used by searchable selects.
 *
 * @param currentValues the values resolved so far in the form
 * @param ctx           the evaluation context (version, features, deployment)
 * @param filter        optional free-text filter for searchable options, may be {@code null}
 * @param page          the zero-based page requested
 * @param pageSize      the number of options per page
 */
public record OptionRequest(
        FormValues currentValues,
        EvalContext ctx,
        String filter,
        int page,
        int pageSize
) {

    /**
     * Creates a request without filtering or pagination.
     *
     * @param currentValues the values resolved so far
     * @param ctx           the evaluation context
     * @return a new request with default pagination
     */
    public static OptionRequest of(final FormValues currentValues,
                                   final EvalContext ctx) {
        return new OptionRequest(currentValues, ctx, null, 0, 50);
    }
}
