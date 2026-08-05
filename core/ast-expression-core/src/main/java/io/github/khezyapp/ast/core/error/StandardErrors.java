package io.github.khezyapp.ast.core.error;

/**
 * Predefined standard error codes used throughout the evaluation system.
 * <p>
 * Each constant is an {@link ErrorCode.Standard} instance that describes
 * a common failure scenario such as wrong argument counts, type mismatches,
 * division by zero, missing fields, etc.
 * </p>
 */
public final class StandardErrors {
    /** Wrong number of positional arguments. */
    public static final ErrorCode WRONG_ARG_COUNT = std("WRONG_ARG_COUNT",
            "Wrong number of arguments");
    /** Required named argument is missing. */
    public static final ErrorCode MISSING_NAMED_ARG = std("MISSING_NAMED_ARG",
            "Required named argument is missing");
    /** Argument type mismatch (e.g., expected number, got string). */
    public static final ErrorCode ARGUMENT_TYPE_MISMATCH = std("ARGUMENT_TYPE_MISMATCH",
            "Argument type mismatch");
    /** Division by zero error. */
    public static final ErrorCode DIVISION_BY_ZERO = std("DIVISION_BY_ZERO",
            "Division by zero");
    /** No evaluator registered for the given function. */
    public static final ErrorCode FUNCTION_NOT_FOUND = std("FUNCTION_NOT_FOUND",
            "No evaluator registered");
    /** Null value not allowed for this argument. */
    public static final ErrorCode NULL_NOT_ALLOWED = std("NULL_NOT_ALLOWED",
            "Null value not allowed for this argument");
    /** Generic runtime evaluation error. */
    public static final ErrorCode RUNTIME_ERROR = std("RUNTIME_ERROR",
            "Runtime evaluation error");
    /** Required data record not found. */
    public static final ErrorCode DATA_NOT_FOUND = std("DATA_NOT_FOUND",
            "Required data record not found");
    /** Operation received no input records. */
    public static final ErrorCode EMPTY_INPUT = std("EMPTY_INPUT",
            "Operation received no input records");
    /** Required field does not exist in payload. */
    public static final ErrorCode MISSING_FIELD = std("MISSING_FIELD",
            "Required field does not exist in payload");
    /** Field exists in payload but value is null. */
    public static final ErrorCode NULL_FIELD_VALUE = std("NULL_FIELD_VALUE",
            "Field exists in payload but value is null");
    /** Schema validation failed. */
    public static final ErrorCode SCHEMA_VALIDATION = std("SCHEMA_VALIDATION",
            "Schema validation failed");
    /** Invalid regular expression pattern. */
    public static final ErrorCode INVALID_REGEX = std("INVALID_REGEX",
            "Invalid regular expression pattern");
    /** Invalid filter value type. */
    public static final ErrorCode INVALID_FILTER_VALUE = std("INVALID_FILTER_VALUE",
            "Invalid filter value type");

    private StandardErrors() {
    }

    private static ErrorCode std(final String code,
                                 final String desc) {
        return new ErrorCode.Standard(code, desc);
    }
}
