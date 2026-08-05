package io.github.khezyapp.dynamicform.model;

import java.util.List;
import java.util.Objects;

/**
 * Spec for a repeatable {@code COLLECTION} field — a variable-length list of typed items.
 * <p>
 * Each row is resolved against {@code itemSchema}, producing row-level issues with index paths such
 * as {@code directors[2].idNumber}. This covers KYC director/shareholder/UBO sets.
 *
 * @param minItems   the minimum number of rows, {@code null} means 0
 * @param maxItems   the maximum number of rows, {@code null} means unbounded
 * @param itemSchema the schema of a single repeated row
 */
public record CollectionSpec(
        Integer minItems,
        Integer maxItems,
        List<FieldSchema> itemSchema
) {

    /**
     * Compact canonical constructor that normalises a null item schema.
     */
    public CollectionSpec {
        itemSchema = Objects.nonNull(itemSchema) ? List.copyOf(itemSchema) : List.of();
    }

    /**
     * Creates an unbounded collection spec with no min/max.
     *
     * @param itemSchema the repeated row schema
     * @return a new spec
     */
    public static CollectionSpec of(final List<FieldSchema> itemSchema) {
        return new CollectionSpec(null, null, itemSchema);
    }

    /**
     * Creates a bounded collection spec.
     *
     * @param minItems   the minimum row count
     * @param maxItems   the maximum row count
     * @param itemSchema the repeated row schema
     * @return a new spec
     */
    public static CollectionSpec of(final Integer minItems,
                                    final Integer maxItems,
                                    final List<FieldSchema> itemSchema) {
        return new CollectionSpec(minItems, maxItems, itemSchema);
    }
}
