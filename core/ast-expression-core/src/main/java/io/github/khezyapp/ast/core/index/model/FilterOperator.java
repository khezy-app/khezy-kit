package io.github.khezyapp.ast.core.index.model;

/**
 * Enumerates filter operators recognized by the index analysis system.
 * <p>
 * Operators are classified for index planning into equality, range, GIN-specific,
 * and other categories.
 * </p>
 */
public enum FilterOperator {
    EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    IS_IN_LIST,
    FUZZY_MATCH,
    STARTS_WITH,
    JSONB_CONTAINS,
    JSONB_KEY_EXISTS,
    ANY_KEY_EXISTS,
    ALL_KEYS_EXIST,
    JSONB_PATH_MATCH,
    ARRAY_CONTAINS,
    ARRAY_OVERLAP,
    ARRAY_CONTAINED_BY,
    FULLTEXT_MATCH,
    REGEX_MATCH,
    SIMILAR_TO,
    OTHER
}
