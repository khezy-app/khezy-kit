package io.github.khezyapp.ast.core.model;

/**
 * Describes a single parameter specification for a function definition.
 * <p>
 * Each {@code ParamSpec} defines the parameter name, whether it is required,
 * its expected {@link ParamType}, and an optional default value.
 * Use the factory methods {@link #required(String, ParamType)} and
 * {@link #optional(String, ParamType)} for concise creation.
 * </p>
 *
 * @param name         the parameter name
 * @param required     whether the parameter is required
 * @param type         the expected parameter type
 * @param defaultValue the default value (may be {@code null})
 */
public record ParamSpec(
    String name,
    boolean required,
    ParamType type,
    Object defaultValue
) {
    /**
     * Creates a required parameter specification.
     *
     * @param name the parameter name
     * @param type the expected parameter type
     * @return a new required {@code ParamSpec}
     */
    public static ParamSpec required(final String name,
                                     final ParamType type) {
        return new ParamSpec(name, true, type, null);
    }

    /**
     * Creates an optional parameter specification with a default value.
     *
     * @param name         the parameter name
     * @param type         the expected parameter type
     * @param defaultValue the default value
     * @return a new optional {@code ParamSpec}
     */
    public static ParamSpec optional(final String name,
                                     final ParamType type,
                                     final Object defaultValue) {
        return new ParamSpec(name, false, type, defaultValue);
    }

    /**
     * Creates an optional parameter specification without a default value.
     *
     * @param name the parameter name
     * @param type the expected parameter type
     * @return a new optional {@code ParamSpec}
     */
    public static ParamSpec optional(final String name,
                                     final ParamType type) {
        return new ParamSpec(name, false, type, null);
    }
}
