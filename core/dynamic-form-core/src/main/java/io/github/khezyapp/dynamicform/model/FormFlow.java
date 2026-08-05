package io.github.khezyapp.dynamicform.model;

import java.util.List;
import java.util.Objects;

/**
 * Optional wizard composition: an ordered list of {@link FormSchema} steps (the Ballerine
 * {@code collection_flow} concept). Each step is a plain schema; the state machine still receives a
 * single final {@code submit} carrying the merged values.
 *
 * @param id    the flow identifier
 * @param steps the ordered step schemas, at least one required
 */
public record FormFlow(String id, List<FormSchema> steps) {

    /**
     * Compact canonical constructor that requires at least one step.
     */
    public FormFlow {
        id = Objects.requireNonNull(id, "id must not be null");
        if (Objects.isNull(steps) || steps.isEmpty()) {
            throw new IllegalArgumentException("FormFlow requires at least one step");
        }
        steps = List.copyOf(steps);
    }

    /**
     * Creates a flow.
     *
     * @param id    the flow identifier
     * @param steps the ordered step schemas
     * @return a new flow
     */
    public static FormFlow of(final String id,
                              final List<FormSchema> steps) {
        return new FormFlow(id, steps);
    }

    /**
     * Returns the number of steps.
     *
     * @return the step count
     */
    public int stepCount() {
        return this.steps.size();
    }

    /**
     * Returns the schema of the step at the given index.
     *
     * @param index the zero-based step index
     * @return the step schema
     */
    public FormSchema stepAt(final int index) {
        return this.steps.get(index);
    }
}
