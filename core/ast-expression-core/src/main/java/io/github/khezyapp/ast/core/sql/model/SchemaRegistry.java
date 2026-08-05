package io.github.khezyapp.ast.core.sql.model;

import java.util.Objects;

import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry for database schema metadata including tables, fields, links,
 * operator compatibility, and aggregation function validation.
 * <p>
 * Provides validation methods for filter operators against field types,
 * aggregator compatibility, path resolution, and type inference for
 * literal values.
 * </p>
 */
public final class SchemaRegistry {

    private static final Set<String> VALID_OPERATORS = Set.of(
        "=", ">", ">=", "<", "<=", "!=",
        "IN", "NOT_IN", "IS_NULL", "IS_NOT_NULL",
        "CONTAINS", "NOT_CONTAINS", "CONTAINS_ANY", "NOT_CONTAINS_ANY",
        "STARTS_WITH", "ENDS_WITH", "WILDCARD", "MATCH"
    );

    private static final Map<String, Set<DataType>> OPERATOR_COMPATIBILITY;
    static {
        final var map = new HashMap<String, Set<DataType>>();
        map.put("=", Set.of(DataType.STRING, DataType.INTEGER, DataType.FLOAT, DataType.BOOLEAN));
        map.put(">", Set.of(DataType.INTEGER, DataType.FLOAT, DataType.TIMESTAMP));
        map.put(">=", Set.of(DataType.INTEGER, DataType.FLOAT, DataType.TIMESTAMP));
        map.put("<", Set.of(DataType.INTEGER, DataType.FLOAT, DataType.TIMESTAMP));
        map.put("<=", Set.of(DataType.INTEGER, DataType.FLOAT, DataType.TIMESTAMP));
        map.put("!=", Set.of(DataType.STRING, DataType.INTEGER, DataType.FLOAT, DataType.BOOLEAN));
        map.put("IN", Set.of(DataType.STRING, DataType.INTEGER, DataType.FLOAT));
        map.put("NOT_IN", Set.of(DataType.STRING, DataType.INTEGER, DataType.FLOAT));
        map.put("IS_NULL", Set.of(DataType.STRING, DataType.INTEGER,
                DataType.FLOAT, DataType.BOOLEAN, DataType.TIMESTAMP));
        map.put("IS_NOT_NULL", Set.of(DataType.STRING, DataType.INTEGER,
                DataType.FLOAT, DataType.BOOLEAN, DataType.TIMESTAMP));
        map.put("CONTAINS", Set.of(DataType.STRING));
        map.put("NOT_CONTAINS", Set.of(DataType.STRING));
        map.put("CONTAINS_ANY", Set.of(DataType.STRING));
        map.put("NOT_CONTAINS_ANY", Set.of(DataType.STRING));
        map.put("STARTS_WITH", Set.of(DataType.STRING));
        map.put("ENDS_WITH", Set.of(DataType.STRING));
        map.put("WILDCARD", Set.of(DataType.STRING));
        map.put("MATCH", Set.of(DataType.STRING, DataType.INTEGER, DataType.FLOAT));
        OPERATOR_COMPATIBILITY = Collections.unmodifiableMap(map);
    }

    private static final Set<String> UNARY_OPERATORS = Set.of("IS_NULL", "IS_NOT_NULL");

    private static final Map<String, DataType> AGGREGATOR_RETURN_TYPES = Map.of(
        "AVG", DataType.FLOAT,
        "COUNT", DataType.INTEGER,
        "COUNT_DISTINCT", DataType.INTEGER,
        "MAX", DataType.FLOAT,
        "MIN", DataType.FLOAT,
        "SUM", DataType.FLOAT,
        "STDDEV", DataType.FLOAT,
        "PERCENTILE", DataType.FLOAT,
        "MEDIAN", DataType.FLOAT
    );

    private static final Map<String, List<DataType>> AGGREGATOR_VALID_TYPES = Map.of(
        "AVG", List.of(DataType.INTEGER, DataType.FLOAT),
        "COUNT", List.of(DataType.STRING, DataType.INTEGER, DataType.FLOAT,
                DataType.BOOLEAN, DataType.TIMESTAMP),
        "COUNT_DISTINCT", List.of(DataType.STRING, DataType.INTEGER, DataType.FLOAT,
                DataType.BOOLEAN, DataType.TIMESTAMP),
        "MAX", List.of(DataType.INTEGER, DataType.FLOAT, DataType.TIMESTAMP),
        "MIN", List.of(DataType.INTEGER, DataType.FLOAT, DataType.TIMESTAMP),
        "SUM", List.of(DataType.INTEGER, DataType.FLOAT),
        "STDDEV", List.of(DataType.INTEGER, DataType.FLOAT),
        "PERCENTILE", List.of(DataType.INTEGER, DataType.FLOAT),
        "MEDIAN", List.of(DataType.INTEGER, DataType.FLOAT)
    );

    private final Map<String, TableMetadata> tables;

    /**
     * Creates a schema registry from a list of table metadata definitions.
     *
     * @param tables the table metadata list
     */
    public SchemaRegistry(final List<TableMetadata> tables) {
        this.tables = new HashMap<>();
        for (final var t : tables) {
            this.tables.put(t.name(), t);
        }
    }

    /**
     * Looks up table metadata by name.
     *
     * @param name the table name
     * @return the table metadata, or {@code null}
     */
    public TableMetadata getTable(final String name) {
        return tables.get(name);
    }

    /**
     * Checks whether a table exists in the schema.
     *
     * @param name the table name
     * @return {@code true} if the table is registered
     */
    public boolean hasTable(final String name) {
        return tables.containsKey(name);
    }

    /**
     * Looks up field metadata within a table.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @return the field metadata, or {@code null}
     */
    public FieldMetadata getField(final String tableName,
                                   final String fieldName) {
        final var table = tables.get(tableName);
        if (Objects.isNull(table)) {
            return null;
        }
        return table.fields().get(fieldName);
    }

    /**
     * Looks up a relationship link from a table.
     *
     * @param tableName the source table name
     * @param linkName  the link name
     * @return the link metadata, or {@code null}
     */
    public LinkMetadata getLink(final String tableName,
                                 final String linkName) {
        final var table = tables.get(tableName);
        if (Objects.isNull(table)) {
            return null;
        }
        return table.links().get(linkName);
    }

    /**
     * Validates a join path (sequence of link names) starting from a table.
     *
     * @param startTable the starting table name
     * @param path       the sequence of link names
     * @return a list of validation errors (empty if valid)
     */
    public List<EvaluationError> validatePath(final String startTable,
                                              final List<String> path) {
        final var errors = new ArrayList<EvaluationError>();
        var currentTable = startTable;

        for (final var linkName : path) {
            final var link = getLink(currentTable, linkName);
            if (Objects.isNull(link)) {
                errors.add(EvaluationError.of(StandardErrors.SCHEMA_VALIDATION,
                        "Link '" + linkName + "' not found on table '"
                                + currentTable + "'",
                        "path:" + linkName));
                break;
            }
            currentTable = link.parentTableName();
        }

        return errors;
    }

    /**
     * Validates that a field exists on a table.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @return an error if validation fails, or {@code null}
     */
    public EvaluationError validateField(final String tableName,
                                          final String fieldName) {
        if (!hasTable(tableName)) {
            return EvaluationError.of(StandardErrors.SCHEMA_VALIDATION,
                    "Table '" + tableName + "' not found in schema",
                    "table:" + tableName);
        }
        if (Objects.isNull(getField(tableName, fieldName))) {
            return EvaluationError.of(StandardErrors.SCHEMA_VALIDATION,
                    "Field '" + fieldName + "' not found on table '"
                            + tableName + "'",
                    "field:" + fieldName);
        }
        return null;
    }

    /**
     * Returns the data type of a field.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @return the field data type (defaults to {@link DataType#STRING})
     */
    public DataType getFieldType(final String tableName,
                                 final String fieldName) {
        final var field = getField(tableName, fieldName);
        if (Objects.isNull(field)) {
            return DataType.STRING;
        }
        return DataType.fromString(field.dataType());
    }

    /**
     * Checks whether an operator is recognized.
     *
     * @param operator the operator string
     * @return {@code true} if valid
     */
    public boolean isOperatorValid(final String operator) {
        return VALID_OPERATORS.contains(operator);
    }

    /**
     * Checks whether an operator is compatible with a field type.
     *
     * @param operator  the operator string
     * @param fieldType the field data type
     * @return {@code true} if compatible
     */
    public boolean isOperatorValidForType(final String operator,
                                          final DataType fieldType) {
        final var compatible = OPERATOR_COMPATIBILITY.get(operator);
        if (Objects.isNull(compatible)) {
            return false;
        }
        return compatible.contains(fieldType);
    }

    /**
     * Checks whether an operator is unary (e.g., IS_NULL, IS_NOT_NULL).
     *
     * @param operator the operator string
     * @return {@code true} if unary
     */
    public boolean isUnaryOperator(final String operator) {
        return UNARY_OPERATORS.contains(operator);
    }

    /**
     * Checks whether an aggregator function is recognized.
     *
     * @param aggregator the aggregator name
     * @return {@code true} if valid
     */
    public boolean isAggregatorValid(final String aggregator) {
        return AGGREGATOR_RETURN_TYPES.containsKey(aggregator);
    }

    /**
     * Checks whether an aggregator function is valid for a field type.
     *
     * @param aggregator the aggregator name
     * @param fieldType  the field data type
     * @return {@code true} if valid
     */
    public boolean isAggregatorValidForType(final String aggregator,
                                            final DataType fieldType) {
        final var valid = AGGREGATOR_VALID_TYPES.get(aggregator);
        if (Objects.isNull(valid)) {
            return false;
        }
        return valid.contains(fieldType);
    }

    /**
     * Returns the default value for an aggregator when no data is found.
     *
     * @param aggregator the aggregator name
     * @return the default value (0 for COUNT/SUM, null for AVG/MAX/MIN)
     */
    public Object defaultAggregatorValue(final String aggregator) {
        return switch (aggregator) {
            case "SUM", "COUNT", "COUNT_DISTINCT" -> 0;
            case "AVG" -> null;
            case "MAX", "MIN", "STDDEV", "MEDIAN", "PERCENTILE" -> null;
            default -> null;
        };
    }

    /**
     * Validates a value against the expected type of a table field.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @param value     the value to validate
     * @return an error if validation fails, or {@code null}
     */
    public EvaluationError validateValueType(final String tableName,
                                              final String fieldName,
                                              final Object value) {
        final var dataType = getFieldType(tableName, fieldName);
        return validateValueAgainstType(value, dataType);
    }

    /**
     * Validates a value against an expected data type.
     *
     * @param value        the value to validate
     * @param expectedType the expected data type
     * @return an error if validation fails, or {@code null}
     */
    public EvaluationError validateValueAgainstType(final Object value,
                                                     final DataType expectedType) {
        if (Objects.isNull(value)) {
            return null;
        }
        final var actualType = inferType(value);
        if (Objects.equals(actualType, expectedType)) {
            return null;
        }
        if (expectedType.isNumeric() && "INTEGER".equals(actualType.name())) {
            return null;
        }
        if (expectedType.isNumeric() && "FLOAT".equals(actualType.name())) {
            return null;
        }
        return EvaluationError.of(StandardErrors.INVALID_FILTER_VALUE,
                "Value type '" + actualType + "' is not compatible with expected type '"
                        + expectedType + "'",
                "value:" + value);
    }

    /**
     * Infers the {@link DataType} of a Java object.
     *
     * @param value the object (may be {@code null})
     * @return the inferred data type
     */
    public static DataType inferType(final Object value) {
        if (value instanceof String) {
            return DataType.STRING;
        }
        if (value instanceof Boolean) {
            return DataType.BOOLEAN;
        }
        if (value instanceof Long || value instanceof Integer
                || value instanceof Short || value instanceof Byte) {
            return DataType.INTEGER;
        }
        if (value instanceof Number) {
            return DataType.FLOAT;
        }
        if (value instanceof java.time.Instant
                || value instanceof java.time.LocalDateTime
                || value instanceof java.time.ZonedDateTime) {
            return DataType.TIMESTAMP;
        }
        return DataType.STRING;
    }
}
