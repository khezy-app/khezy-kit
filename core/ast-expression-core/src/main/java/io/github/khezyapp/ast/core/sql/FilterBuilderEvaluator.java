package io.github.khezyapp.ast.core.sql;

import java.util.Objects;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;
import io.github.khezyapp.ast.core.sql.model.DbColumn;
import io.github.khezyapp.ast.core.sql.model.DbFilter;
import io.github.khezyapp.ast.core.sql.model.DbQuery;
import io.github.khezyapp.ast.core.sql.model.DbTable;
import io.github.khezyapp.ast.core.sql.model.FilterValue;
import io.github.khezyapp.ast.core.sql.model.SchemaRegistry;

/**
 * Evaluator for building filter conditions (buildFilter).
 * <p>
 * Validates the operator and field type against the schema registry, then
 * produces a {@link DbFilter} with the appropriate
 * {@link io.github.khezyapp.ast.core.sql.model.FilterValue} variant
 * (literal, column reference, or subquery).
 * </p>
 */
public class FilterBuilderEvaluator implements Evaluator {
    private final SchemaRegistry schemaRegistry;

    /**
     * Creates a filter builder evaluator.
     *
     * @param schemaRegistry the schema registry for validation
     */
    public FilterBuilderEvaluator(final SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var commandTableName = (String) args.named().get("tableName");
        final var fieldName = (String) args.named().get("fieldName");
        final var operator = (String) args.named().get("operator");
        final var value = args.named().get("value");
        final var valueType = (String) args.named()
                .getOrDefault("valueType", "literal");

        if (Objects.isNull(commandTableName)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'tableName' is missing",
                            "named:tableName"));
        }
        if (Objects.isNull(fieldName)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'fieldName' is missing",
                            "named:fieldName"));
        }
        if (Objects.isNull(operator)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'operator' is missing",
                            "named:operator"));
        }

        if (!schemaRegistry.isOperatorValid(operator)) {
            return EvaluationOutcome.failure(
                EvaluationError.of(StandardErrors.SCHEMA_VALIDATION,
                    "Unknown filter operator '" + operator + "'",
                    "named:operator"));
        }

        final var fieldType = schemaRegistry.getFieldType(commandTableName, fieldName);
        if (!schemaRegistry.isOperatorValidForType(operator, fieldType)) {
            return EvaluationOutcome.failure(
                EvaluationError.of(StandardErrors.SCHEMA_VALIDATION,
                    "Operator '" + operator + "' is not valid for field type "
                        + fieldType,
                    "named:operator"));
        }

        if (!schemaRegistry.isUnaryOperator(operator)) {
            final var typeError = schemaRegistry.validateValueAgainstType(
                value, fieldType);
            if (Objects.nonNull(typeError)) {
                return EvaluationOutcome.failure(typeError);
            }
        }

        final var table = DbTable.of(commandTableName);
        final var column = DbColumn.of(table, fieldName);

        final var filterValue = switch (valueType) {
            case "columnRef" -> {
                if (value instanceof String colName) {
                    yield new FilterValue.ColumnRef(DbColumn.of(table, colName));
                }
                yield new FilterValue.Literal(value);
            }
            case "subquery" -> {
                if (value instanceof DbQuery q) {
                    yield new FilterValue.Subquery(q);
                }
                yield new FilterValue.Literal(value);
            }
            default -> new FilterValue.Literal(value);
        };

        return EvaluationOutcome.success(
                DbFilter.of(column, operator, filterValue));
    }
}
