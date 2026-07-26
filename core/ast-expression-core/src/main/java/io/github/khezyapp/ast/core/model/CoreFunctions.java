package io.github.khezyapp.ast.core.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Central registry of built-in core function identifiers used throughout the AST
 * expression evaluation library.
 * <p>
 * Every constant {@link FunctionId} declared here is automatically resolved as a
 * core function by {@link FunctionId#of(String)}. The corresponding
 * {@link io.github.khezyapp.ast.core.eval.Evaluator} implementations are registered
 * in {@link io.github.khezyapp.ast.core.function.FunctionRegistry#withBuiltins}.
 * </p>
 */
public final class CoreFunctions {
    /** Identifier for a constant/leaf value node. */
    public static final FunctionId CONSTANT = core("__const__");
    /** Arithmetic addition. */
    public static final FunctionId ADD = core("add");
    /** Arithmetic subtraction. */
    public static final FunctionId SUBTRACT = core("subtract");
    /** Arithmetic multiplication. */
    public static final FunctionId MULTIPLY = core("multiply");
    /** Arithmetic division. */
    public static final FunctionId DIVIDE = core("divide");
    /** Greater-than comparison. */
    public static final FunctionId GREATER_THAN = core("gt");
    /** Greater-than-or-equal comparison. */
    public static final FunctionId GREATER_THAN_OR_EQUAL = core("gte");
    /** Less-than comparison. */
    public static final FunctionId LESS_THAN = core("lt");
    /** Less-than-or-equal comparison. */
    public static final FunctionId LESS_THAN_OR_EQUAL = core("lte");
    /** Equality comparison. */
    public static final FunctionId EQUAL = core("eq");
    /** Logical AND (supports short-circuit). */
    public static final FunctionId AND = core("and");
    /** Logical OR (supports short-circuit). */
    public static final FunctionId OR = core("or");
    /** Logical NOT. */
    public static final FunctionId NOT = core("not");
    /** Emptiness check (null, blank string, empty collection/map). */
    public static final FunctionId IS_EMPTY = core("isEmpty");
    /** Payload field access by dot-separated path. */
    public static final FunctionId PAYLOAD = core("payload");
    /** String containment check. */
    public static final FunctionId STRING_CONTAINS = core("stringContains");
    /** String starts-with check. */
    public static final FunctionId STRING_STARTS_WITH = core("stringStartsWith");
    /** String ends-with check. */
    public static final FunctionId STRING_ENDS_WITH = core("stringEndsWith");
    /** Fuzzy (Levenshtein) string match. */
    public static final FunctionId STRING_FUZZY_MATCH = core("stringFuzzyMatch");
    /** String similarity scoring. */
    public static final FunctionId STRING_SIMILARITY = core("stringSimilarity");
    /** Regex string match. */
    public static final FunctionId STRING_MATCH = core("stringMatch");
    /** String length calculation. */
    public static final FunctionId STRING_LENGTH = core("stringLength");
    /** String whitespace trimming. */
    public static final FunctionId STRING_TRIM = core("stringTrim");
    /** String substring extraction. */
    public static final FunctionId STRING_SUBSTRING = core("stringSubstring");
    /** String search-and-replace. */
    public static final FunctionId STRING_REPLACE = core("stringReplace");
    /** List construction from positional arguments. */
    public static final FunctionId LIST = core("list");
    /** SQL filter builder. */
    public static final FunctionId BUILD_FILTER = core("buildFilter");
    /** Database table access. */
    public static final FunctionId DB_ACCESS = core("dbAccess");
    /** Database aggregation query. */
    public static final FunctionId DB_AGGREGATOR = core("dbAggregator");
    /** Cross-table field access via join paths. */
    public static final FunctionId DB_FIELD_ACCESS = core("dbFieldAccess");
    /** Current timestamp retrieval. */
    public static final FunctionId NOW = core("now");
    /** Date/time addition. */
    public static final FunctionId DATE_PLUS = core("datePlus");
    /** Date/time subtraction. */
    public static final FunctionId DATE_MINUS = core("dateMinus");
    /** Date/time difference calculation. */
    public static final FunctionId DATE_DIFF = core("dateDiff");
    /** Date/time formatting to string. */
    public static final FunctionId DATE_FORMAT = core("dateFormat");
    /** Date/time string parsing. */
    public static final FunctionId DATE_PARSE = core("dateParse");
    /** Year extraction from a date. */
    public static final FunctionId EXTRACT_YEAR = core("extractYear");
    /** Month extraction from a date. */
    public static final FunctionId EXTRACT_MONTH = core("extractMonth");
    /** Day-of-month extraction from a date. */
    public static final FunctionId EXTRACT_DAY = core("extractDay");
    /** Hour extraction from a date/time. */
    public static final FunctionId EXTRACT_HOUR = core("extractHour");
    /** Minute extraction from a date/time. */
    public static final FunctionId EXTRACT_MINUTE = core("extractMinute");
    /** Second extraction from a date/time. */
    public static final FunctionId EXTRACT_SECOND = core("extractSecond");
    /** Coalesce: returns first non-null value. */
    public static final FunctionId COALESCE = core("coalesce");
    /** Returns a default value if the input is null. */
    public static final FunctionId DEFAULT_IF_NULL = core("defaultIfNull");

    private CoreFunctions() {
    }

    private static FunctionId core(final String v) {
        return new FunctionId.Core(v);
    }

    static Optional<FunctionId> forValue(final String v) {
        return Arrays.stream(CoreFunctions.class.getFields())
                .filter(f -> f.getType() == FunctionId.class)
                .map(f -> {
                    try {
                        return (FunctionId) f.get(null);
                    } catch (final IllegalAccessException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(id -> id.value().equals(v))
                .findFirst();
    }
}
