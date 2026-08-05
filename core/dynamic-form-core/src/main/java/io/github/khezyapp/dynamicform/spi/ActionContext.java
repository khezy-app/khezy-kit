package io.github.khezyapp.dynamicform.spi;

import io.github.khezyapp.dynamicform.engine.EvalContext;
import io.github.khezyapp.dynamicform.model.FieldAction;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.value.FormValues;

/**
 * Everything an {@link ActionHandler} needs to run a declared action.
 *
 * @param action        the declared action being invoked
 * @param field         the field that declared the action
 * @param currentValues the values resolved so far in the form
 * @param ctx           the evaluation context
 */
public record ActionContext(
    FieldAction action,
    FieldSchema field,
    FormValues currentValues,
    EvalContext ctx
) {
}
