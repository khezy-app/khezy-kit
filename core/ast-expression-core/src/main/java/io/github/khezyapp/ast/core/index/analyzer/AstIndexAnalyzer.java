package io.github.khezyapp.ast.core.index.analyzer;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.github.khezyapp.ast.core.CoreUtils;
import io.github.khezyapp.ast.core.index.model.AggregateQueryFamily;
import io.github.khezyapp.ast.core.index.model.FieldExpression;
import io.github.khezyapp.ast.core.index.model.FilterOperator;
import io.github.khezyapp.ast.core.index.model.FunctionalColumn;
import io.github.khezyapp.ast.core.index.model.GinColumn;
import io.github.khezyapp.ast.core.index.model.PlainColumn;
import io.github.khezyapp.ast.core.model.Node;

import static io.github.khezyapp.ast.core.model.CoreFunctions.DB_ACCESS;
import static io.github.khezyapp.ast.core.model.CoreFunctions.DB_AGGREGATOR;

/**
 * Analyzes an AST to extract query families for dynamic index detection.
 * <p>
 * Walks the AST tree to find {@code dbAggregator} and {@code dbAccess} nodes,
 * extracts filter conditions, classifies operators, and builds
 * {@link io.github.khezyapp.ast.core.index.model.AggregateQueryFamily}
 * instances that describe the access pattern for index planning.
 * </p>
 */
public final class AstIndexAnalyzer {

    private static final IndexResolverRegistry DEFAULT_RESOLVERS = IndexResolverRegistry.withBuiltins();

    private final IndexResolverRegistry resolverRegistry;

    /**
     * Creates an analyzer with the default built-in resolvers.
     */
    public AstIndexAnalyzer() {
        this(DEFAULT_RESOLVERS);
    }

    /**
     * Creates an analyzer with a custom resolver registry.
     *
     * @param resolverRegistry the expression index resolver registry
     */
    public AstIndexAnalyzer(final IndexResolverRegistry resolverRegistry) {
        this.resolverRegistry = Objects.requireNonNull(resolverRegistry);
    }

    /**
     * Walks the AST and extracts all query families from dbAggregator and dbAccess nodes.
     *
     * @param root the root AST node
     * @return a set of extracted query families
     */
    public Set<AggregateQueryFamily> extractQueryFamilies(final Node root) {
        final var families = new LinkedHashSet<AggregateQueryFamily>();
        walk(root, families);
        return families;
    }

    private void walk(final Node node,
                      final Set<AggregateQueryFamily> families) {
        if (isNodeSupportDynamicFilter(node)) {
            final var family = extractFamily(node);
            if (Objects.nonNull(family)) {
                families.add(family);
            }
        }
        for (final var child : CoreUtils.emptyListIfNull(node.children())) {
            walk(child, families);
        }
        for (final var child : CoreUtils.emptyMapIfNull(node.namedChildren()).values()) {
            walk(child, families);
        }
    }

    private boolean isNodeSupportDynamicFilter(final Node node) {
        return DB_AGGREGATOR.value().equals(node.function().value()) ||
                DB_ACCESS.value().equals(node.function().value());
    }

    private AggregateQueryFamily extractFamily(final Node aggregatorNode) {
        final var tableNameNode = aggregatorNode.namedChildren().get("tableName");
        final var fieldNameNode = aggregatorNode.namedChildren().get("fieldName");
        if (Objects.isNull(tableNameNode) || Objects.isNull(fieldNameNode)) {
            return null;
        }
        if (!tableNameNode.isConstant() || !fieldNameNode.isConstant()) {
            return null;
        }

        final var tableName = tableNameNode.constant().toString();
        final var fieldName = fieldNameNode.constant().toString();

        final var builder = AggregateQueryFamily.builder()
                .tableName(tableName)
                .fieldName(fieldName);

        final var filtersNode = aggregatorNode.namedChildren().get("filters");
        if (Objects.nonNull(filtersNode)) {
            for (final var filterNode : filtersNode.children()) {
                extractFilter(filterNode, builder);
            }
        }

        if (!builder.build().eqConditions().contains(fieldName)
                && !builder.build().ineqConditions().contains(fieldName)
                && !builder.build().ginConditions().contains(fieldName)) {
            builder.addOther(fieldName);
        }

        return builder.build();
    }

    private void extractFilter(final Node filterNode,
                               final AggregateQueryFamily.Builder builder) {
        final var fieldExprNode = filterNode.namedChildren().get("fieldName");
        final var operatorNode = filterNode.namedChildren().get("operator");
        if (Objects.isNull(fieldExprNode) || Objects.isNull(operatorNode)) {
            return;
        }
        if (!operatorNode.isConstant()) {
            return;
        }

        final var opStr = operatorNode.constant().toString();
        final var op = classifyOperator(opStr);

        final var metadata = resolverRegistry.resolve(fieldExprNode);
        if (!metadata.isIndexable()) {
            builder.addOther(metadata.columnName());
            return;
        }

        final var col = metadata.columnName();

        if (isGinOperator(op)) {
            if (metadata instanceof GinColumn) {
                builder.addGin(col);
            } else {
                builder.addGin(col);
            }
            return;
        }

        if (metadata instanceof FunctionalColumn) {
            switch (op) {
                case EQUAL -> builder.addEqFunctional(col);
                case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                    if (builder.build().eqConditions().contains(col)) {
                        return;
                    }
                    builder.addIneqFunctional(col);
                }
                case IS_IN_LIST, FUZZY_MATCH, STARTS_WITH -> {
                    if (!builder.build().eqConditions().contains(col)
                            && !builder.build().ineqConditions().contains(col)
                            && !builder.build().ginConditions().contains(col)) {
                        builder.addOther(col);
                    }
                }
                default -> {
                    if (!builder.build().eqConditions().contains(col)
                            && !builder.build().ineqConditions().contains(col)
                            && !builder.build().ginConditions().contains(col)) {
                        builder.addOther(col);
                    }
                }
            }
        } else {
            switch (op) {
                case EQUAL -> builder.addEq(col);
                case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                    if (builder.build().eqConditions().contains(col)) {
                        return;
                    }
                    builder.addIneq(col);
                }
                case IS_IN_LIST, FUZZY_MATCH, STARTS_WITH -> {
                    if (!builder.build().eqConditions().contains(col)
                            && !builder.build().ineqConditions().contains(col)
                            && !builder.build().ginConditions().contains(col)) {
                        builder.addOther(col);
                    }
                }
                default -> {
                    if (!builder.build().eqConditions().contains(col)
                            && !builder.build().ineqConditions().contains(col)
                            && !builder.build().ginConditions().contains(col)) {
                        builder.addOther(col);
                    }
                }
            }
        }
    }

    private static boolean isGinOperator(final FilterOperator op) {
        return switch (op) {
            case JSONB_CONTAINS, JSONB_KEY_EXISTS, ANY_KEY_EXISTS, ALL_KEYS_EXIST,
                 JSONB_PATH_MATCH, ARRAY_CONTAINS, ARRAY_OVERLAP, ARRAY_CONTAINED_BY,
                 FULLTEXT_MATCH, REGEX_MATCH, SIMILAR_TO -> true;
            default -> false;
        };
    }

    public static FieldExpression extractFieldExpression(final Node expr) {
        final var metadata = DEFAULT_RESOLVERS.resolve(expr);
        if (metadata instanceof PlainColumn plain) {
            return FieldExpression.plain(plain.columnName());
        }
        if (metadata instanceof FunctionalColumn func) {
            return FieldExpression.functional(func.columnName(), func.transformFunction());
        }
        return FieldExpression.plain("");
    }

    public static FilterOperator classifyOperator(final String op) {
        return switch (op) {
            case "=" -> FilterOperator.EQUAL;
            case "<" -> FilterOperator.LESS_THAN;
            case "<=" -> FilterOperator.LESS_OR_EQUAL;
            case ">" -> FilterOperator.GREATER_THAN;
            case ">=" -> FilterOperator.GREATER_OR_EQUAL;
            case "IsInList" -> FilterOperator.IS_IN_LIST;
            case "FuzzyMatch" -> FilterOperator.FUZZY_MATCH;
            case "StartsWith" -> FilterOperator.STARTS_WITH;
            case "jsonb_contains" -> FilterOperator.JSONB_CONTAINS;
            case "jsonb_key_exists" -> FilterOperator.JSONB_KEY_EXISTS;
            case "jsonb_any_key_exists" -> FilterOperator.ANY_KEY_EXISTS;
            case "jsonb_all_keys_exist" -> FilterOperator.ALL_KEYS_EXIST;
            case "jsonb_path_match" -> FilterOperator.JSONB_PATH_MATCH;
            case "array_contains" -> FilterOperator.ARRAY_CONTAINS;
            case "array_overlap" -> FilterOperator.ARRAY_OVERLAP;
            case "array_contained_by" -> FilterOperator.ARRAY_CONTAINED_BY;
            case "fulltext_match" -> FilterOperator.FULLTEXT_MATCH;
            case "regex_match" -> FilterOperator.REGEX_MATCH;
            case "similar_to" -> FilterOperator.SIMILAR_TO;
            default -> FilterOperator.OTHER;
        };
    }
}
