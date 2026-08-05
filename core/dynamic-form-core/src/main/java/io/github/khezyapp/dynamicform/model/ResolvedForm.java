package io.github.khezyapp.dynamicform.model;

import io.github.khezyapp.dynamicform.value.FormValues;

import java.util.List;
import java.util.Objects;

/**
 * The output of a form resolution: coerced, defaulted values plus any validation issues.
 *
 * @param values the resolved values (defaults applied, coerced, hidden fields dropped)
 * @param issues the validation/coercion issues, empty when the form is valid
 */
public record ResolvedForm(FormValues values, List<FieldIssue> issues) {

    /**
     * Compact canonical constructor that normalises null facets.
     */
    public ResolvedForm {
        values = Objects.nonNull(values) ? values : FormValues.empty();
        issues = Objects.nonNull(issues) ? List.copyOf(issues) : List.of();
    }

    /**
     * Whether resolution produced no issues (the form may be submitted).
     *
     * @return {@code true} when {@code issues} is empty
     */
    public boolean isValid() {
        return this.issues.isEmpty();
    }
}
