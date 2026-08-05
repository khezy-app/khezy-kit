package io.github.khezyapp.ast.core.function;

import io.github.khezyapp.ast.core.builtin.ArithmeticEvaluator;
import io.github.khezyapp.ast.core.builtin.BooleanLogicEvaluator;
import io.github.khezyapp.ast.core.builtin.ComparisonEvaluator;
import io.github.khezyapp.ast.core.builtin.EqualEvaluator;
import io.github.khezyapp.ast.core.builtin.IsEmptyEvaluator;
import io.github.khezyapp.ast.core.builtin.ListEvaluator;
import io.github.khezyapp.ast.core.builtin.NotEvaluator;
import io.github.khezyapp.ast.core.builtin.PayloadEvaluator;
import io.github.khezyapp.ast.core.builtin.CoalesceEvaluator;
import io.github.khezyapp.ast.core.builtin.DefaultIfNullEvaluator;
import io.github.khezyapp.ast.core.builtin.date.DateDiffEvaluator;
import io.github.khezyapp.ast.core.builtin.date.DateExtractEvaluator;
import io.github.khezyapp.ast.core.builtin.date.DateFormatEvaluator;
import io.github.khezyapp.ast.core.builtin.date.DateMinusEvaluator;
import io.github.khezyapp.ast.core.builtin.date.DateParseEvaluator;
import io.github.khezyapp.ast.core.builtin.date.DatePlusEvaluator;
import io.github.khezyapp.ast.core.builtin.date.NowEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringContainsEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringEndsWithEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringFuzzyMatchEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringLengthEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringMatchEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringReplaceEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringSimilarityEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringStartsWithEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringSubstringEvaluator;
import io.github.khezyapp.ast.core.builtin.string.StringTrimEvaluator;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.CoreFunctions;
import io.github.khezyapp.ast.core.model.FunctionId;
import io.github.khezyapp.ast.core.model.ParamSpec;
import io.github.khezyapp.ast.core.model.ParamType;
import io.github.khezyapp.ast.core.nullstrategy.NullHandlingStrategy;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for function definitions used by the AST evaluation engine.
 * <p>
 * Provides factory methods to create registries pre-populated with all built-in
 * evaluators ({@link #withBuiltins}) or empty for custom registration
 * ({@link #empty}). Once {@link #freeze()} is called, no further registrations
 * are allowed.
 * </p>
 */
public final class FunctionRegistry {
    private final ConcurrentHashMap<FunctionId, FunctionDefinition> definitions
        = new ConcurrentHashMap<>();
    private final NullHandlingStrategy nullStrategy;
    private volatile boolean frozen;

    private FunctionRegistry(final NullHandlingStrategy nullStrategy) {
        this.nullStrategy = Objects.requireNonNull(nullStrategy);
    }

    /**
     * Creates a registry pre-populated with all built-in evaluators (arithmetic,
     * boolean, comparison, string, date, coalesce, etc.).
     *
     * @param nullStrategy the default null-handling strategy for the registry
     * @return a new registry with all builtins registered
     */
    public static FunctionRegistry withBuiltins(final NullHandlingStrategy nullStrategy) {
        final var r = new FunctionRegistry(nullStrategy);
        registerAllBuiltins(r);
        return r;
    }

    private static void registerAllBuiltins(final FunctionRegistry r) {
        registerCoreBuiltins(r);
        registerStringBuiltins(r);
        registerDateBuiltins(r);
        registerCoalesceBuiltins(r);
    }

    private static void registerCoreBuiltins(final FunctionRegistry r) {
        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.ADD)
            .evaluator(new ArithmeticEvaluator(CoreFunctions.ADD))
            .positionalParam(ParamSpec.required("left", ParamType.INTEGER))
            .positionalParam(ParamSpec.required("right", ParamType.INTEGER))
            .attributes(FunctionAttributes.commutative(1))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.SUBTRACT)
            .evaluator(new ArithmeticEvaluator(CoreFunctions.SUBTRACT))
            .positionalParam(ParamSpec.required("left", ParamType.INTEGER))
            .positionalParam(ParamSpec.required("right", ParamType.INTEGER))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.MULTIPLY)
            .evaluator(new ArithmeticEvaluator(CoreFunctions.MULTIPLY))
            .positionalParam(ParamSpec.required("left", ParamType.INTEGER))
            .positionalParam(ParamSpec.required("right", ParamType.INTEGER))
            .attributes(FunctionAttributes.commutative(2))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.DIVIDE)
            .evaluator(new ArithmeticEvaluator(CoreFunctions.DIVIDE))
            .positionalParam(ParamSpec.required("numerator", ParamType.INTEGER))
            .positionalParam(ParamSpec.required("denominator", ParamType.INTEGER))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.GREATER_THAN)
            .evaluator(new ComparisonEvaluator(CoreFunctions.GREATER_THAN))
            .positionalParam(ParamSpec.required("left", ParamType.ANY))
            .positionalParam(ParamSpec.required("right", ParamType.ANY))
            .build());

        r.register(FunctionDefinition.builder()
                .functionId(CoreFunctions.GREATER_THAN_OR_EQUAL)
                .evaluator(new ComparisonEvaluator(CoreFunctions.GREATER_THAN_OR_EQUAL))
                .positionalParam(ParamSpec.required("left", ParamType.ANY))
                .positionalParam(ParamSpec.required("right", ParamType.ANY))
                .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.LESS_THAN)
            .evaluator(new ComparisonEvaluator(CoreFunctions.LESS_THAN))
            .positionalParam(ParamSpec.required("left", ParamType.ANY))
            .positionalParam(ParamSpec.required("right", ParamType.ANY))
            .build());

        r.register(FunctionDefinition.builder()
                .functionId(CoreFunctions.LESS_THAN_OR_EQUAL)
                .evaluator(new ComparisonEvaluator(CoreFunctions.LESS_THAN_OR_EQUAL))
                .positionalParam(ParamSpec.required("left", ParamType.ANY))
                .positionalParam(ParamSpec.required("right", ParamType.ANY))
                .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.EQUAL)
            .evaluator(new EqualEvaluator())
            .positionalParam(ParamSpec.required("left", ParamType.ANY))
            .positionalParam(ParamSpec.required("right", ParamType.ANY))
            .attributes(FunctionAttributes.commutative(1))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.AND)
            .evaluator(new BooleanLogicEvaluator(CoreFunctions.AND))
            .positionalParam(ParamSpec.required("operand", ParamType.BOOLEAN))
            .attributes(FunctionAttributes.shortCircuit(
                res -> Boolean.FALSE.equals(res.returnValue())))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.OR)
            .evaluator(new BooleanLogicEvaluator(CoreFunctions.OR))
            .positionalParam(ParamSpec.required("operand", ParamType.BOOLEAN))
            .attributes(FunctionAttributes.shortCircuit(
                res -> Boolean.TRUE.equals(res.returnValue())))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.NOT)
            .evaluator(new NotEvaluator())
            .positionalParam(ParamSpec.required("value", ParamType.BOOLEAN))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.IS_EMPTY)
            .evaluator(new IsEmptyEvaluator())
            .positionalParam(ParamSpec.required("value", ParamType.ANY))
            .build());
    }

    private static void registerStringBuiltins(final FunctionRegistry r) {
        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_CONTAINS)
            .evaluator(new StringContainsEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .namedParam(ParamSpec.required("substring", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.PAYLOAD)
            .evaluator(new PayloadEvaluator())
            .namedParam(ParamSpec.required("fieldName", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.LIST)
            .evaluator(new ListEvaluator())
            .positionalParam(ParamSpec.optional("items", ParamType.ANY))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_STARTS_WITH)
            .evaluator(new StringStartsWithEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .namedParam(ParamSpec.required("prefix", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_ENDS_WITH)
            .evaluator(new StringEndsWithEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .namedParam(ParamSpec.required("suffix", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_FUZZY_MATCH)
            .evaluator(new StringFuzzyMatchEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .namedParam(ParamSpec.required("pattern", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_SIMILARITY)
            .evaluator(new StringSimilarityEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .namedParam(ParamSpec.required("other", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_MATCH)
            .evaluator(new StringMatchEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .namedParam(ParamSpec.required("regex", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_LENGTH)
            .evaluator(new StringLengthEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_TRIM)
            .evaluator(new StringTrimEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_SUBSTRING)
            .evaluator(new StringSubstringEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.STRING_REPLACE)
            .evaluator(new StringReplaceEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .build());
    }

    private static void registerDateBuiltins(final FunctionRegistry r) {
        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.NOW)
            .evaluator(new NowEvaluator())
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.DATE_PLUS)
            .evaluator(new DatePlusEvaluator())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.required("amount", ParamType.INTEGER))
            .namedParam(ParamSpec.optional("unit", ParamType.STRING))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.DATE_MINUS)
            .evaluator(new DateMinusEvaluator())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.required("amount", ParamType.INTEGER))
            .namedParam(ParamSpec.optional("unit", ParamType.STRING))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.DATE_DIFF)
            .evaluator(new DateDiffEvaluator())
            .positionalParam(ParamSpec.required("start", ParamType.ANY))
            .namedParam(ParamSpec.required("end", ParamType.ANY))
            .namedParam(ParamSpec.optional("unit", ParamType.STRING))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.DATE_FORMAT)
            .evaluator(new DateFormatEvaluator())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.required("pattern", ParamType.STRING))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.DATE_PARSE)
            .evaluator(new DateParseEvaluator())
            .positionalParam(ParamSpec.required("input", ParamType.STRING))
            .namedParam(ParamSpec.required("pattern", ParamType.STRING))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.EXTRACT_YEAR)
            .evaluator(DateExtractEvaluator.year())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.EXTRACT_MONTH)
            .evaluator(DateExtractEvaluator.month())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.EXTRACT_DAY)
            .evaluator(DateExtractEvaluator.day())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.EXTRACT_HOUR)
            .evaluator(DateExtractEvaluator.hour())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.EXTRACT_MINUTE)
            .evaluator(DateExtractEvaluator.minute())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.EXTRACT_SECOND)
            .evaluator(DateExtractEvaluator.second())
            .positionalParam(ParamSpec.required("date", ParamType.ANY))
            .namedParam(ParamSpec.optional("zone", ParamType.STRING))
            .build());
    }

    private static void registerCoalesceBuiltins(final FunctionRegistry r) {
        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.COALESCE)
            .evaluator(new CoalesceEvaluator())
            .positionalParam(ParamSpec.optional("values", ParamType.ANY))
            .build());

        r.register(FunctionDefinition.builder()
            .functionId(CoreFunctions.DEFAULT_IF_NULL)
            .evaluator(new DefaultIfNullEvaluator())
            .positionalParam(ParamSpec.required("value", ParamType.ANY))
            .positionalParam(ParamSpec.optional("defaultValue", ParamType.ANY))
            .build());
    }

    /**
     * Creates an empty registry with no pre-registered functions.
     *
     * @param nullStrategy the default null-handling strategy
     * @return a new empty registry
     */
    public static FunctionRegistry empty(final NullHandlingStrategy nullStrategy) {
        return new FunctionRegistry(nullStrategy);
    }

    /**
     * Registers a function definition.
     *
     * @param def the function definition to register
     * @throws IllegalStateException if the registry is frozen or the function is already registered
     */
    public void register(final FunctionDefinition def) {
        if (frozen) {
            throw new IllegalStateException(
                "Registry is frozen, cannot register new functions");
        }
        if (definitions.putIfAbsent(def.functionId(), def) != null) {
            throw new IllegalStateException(
                "Function '" + def.functionId().value() + "' is already registered");
        }
    }

    /**
     * Freezes the registry, preventing any further registrations.
     */
    public void freeze() {
        this.frozen = true;
    }

    /**
     * Returns whether the registry is frozen.
     *
     * @return {@code true} if no more registrations are allowed
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Looks up the function definition for the given identifier.
     *
     * @param id the function identifier
     * @return the registered function definition
     * @throws IllegalArgumentException if no definition is found
     */
    public FunctionDefinition getDefinition(final FunctionId id) {
        final var def = definitions.get(id);
        if (Objects.isNull(def)) {
            throw new IllegalArgumentException(
                "No definition for '" + id.value() + "'");
        }
        return def;
    }

    /**
     * Convenience method to get the evaluator for the given function id.
     *
     * @param id the function identifier
     * @return the registered evaluator
     */
    public Evaluator getEvaluator(final FunctionId id) {
        return getDefinition(id).evaluator();
    }

    /**
     * Convenience method to get the attributes for the given function id.
     *
     * @param id the function identifier
     * @return the registered function attributes
     */
    public FunctionAttributes getAttributes(final FunctionId id) {
        return getDefinition(id).attributes();
    }

    /**
     * Returns the default null-handling strategy for this registry.
     *
     * @return the default null strategy
     */
    public NullHandlingStrategy nullHandlingStrategy() {
        return nullStrategy;
    }

    /**
     * Checks whether a function is registered.
     *
     * @param id the function identifier
     * @return {@code true} if the function is registered
     */
    public boolean contains(final FunctionId id) {
        return definitions.containsKey(id);
    }
}
