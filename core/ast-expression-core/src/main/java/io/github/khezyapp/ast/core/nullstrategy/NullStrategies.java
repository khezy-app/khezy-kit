package io.github.khezyapp.ast.core.nullstrategy;

import io.github.khezyapp.ast.core.model.ParamType;
import java.util.Optional;

/**
 * Predefined null-handling strategy implementations.
 * <p>
 * Use these constants with {@link io.github.khezyapp.ast.core.function.FunctionRegistry}
 * or per-function null strategies to control how null arguments are processed
 * during evaluation.
 * </p>
 */
public final class NullStrategies {
    private NullStrategies() { }

    /**
     * Propagates null values without modification (returns {@link Optional#empty()}).
     */
    public static final NullHandlingStrategy PROPAGATE = (spec, args) -> Optional.empty();

    /**
     * Coerces null values to type-appropriate defaults:
     * <ul>
     *   <li>{@link ParamType#INTEGER} → {@code 0L}</li>
     *   <li>{@link ParamType#FLOAT} → {@code 0.0}</li>
     *   <li>{@link ParamType#BOOLEAN} → {@code false}</li>
     *   <li>{@link ParamType#STRING} → {@code ""}</li>
     *   <li>Otherwise uses the spec's default value if present</li>
     * </ul>
     */
    public static final NullHandlingStrategy COERCE_DEFAULT = (spec, args) -> {
        if (spec.defaultValue() != null) {
            return Optional.of(spec.defaultValue());
        }
        if (spec.type() == ParamType.INTEGER) {
            return Optional.of(0L);
        }
        if (spec.type() == ParamType.FLOAT) {
            return Optional.of(0.0);
        }
        if (spec.type() == ParamType.BOOLEAN) {
            return Optional.of(false);
        }
        if (spec.type() == ParamType.STRING) {
            return Optional.of("");
        }
        return Optional.empty();
    };

    /**
     * Fails with an {@link IllegalArgumentException} when a null value is encountered.
     */
    public static final NullHandlingStrategy FAIL = (spec, args) -> {
        throw new IllegalArgumentException(
            "Null value for required argument '" + spec.name() + "'");
    };
}
