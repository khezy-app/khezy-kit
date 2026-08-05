package io.github.khezyapp.ast.core.index.planner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.khezyapp.ast.core.index.model.AggregateQueryFamily;
import io.github.khezyapp.ast.core.index.model.ConcreteIndex;
import io.github.khezyapp.ast.core.index.model.IndexFamily;
import io.github.khezyapp.ast.core.index.model.IndexType;

/**
 * Plans index families and concrete index definitions from query access patterns.
 * <p>
 * Takes an {@link io.github.khezyapp.ast.core.index.model.AggregateQueryFamily}
 * and produces candidate index families, filters against existing indexes,
 * minimizes overlapping families, and projects them into concrete index definitions.
 * Also supports GIN index planning.
 * </p>
 */
public final class IndexPlanner {

    private final RefinementMerger merger = new RefinementMerger();

    /**
     * Plans index families from an aggregate query family.
     * <p>
     * Equality conditions become flex columns. Each inequality condition
     * generates a separate family with that column as the last column.
     * Non-indexable conditions become included columns.
     * </p>
     *
     * @param qf the aggregate query family
     * @return a set of index families (empty if no indexable conditions)
     */
    public Set<IndexFamily> planIndexFamilies(final AggregateQueryFamily qf) {
        if (!qf.hasIndexableConditions()) {
            return Set.of();
        }

        final var functionalColumns = qf.functionalColumns();

        if (qf.ineqConditions().isEmpty()) {
            final var base = IndexFamily.builder()
                    .tableName(qf.tableName())
                    .flexColumns(qf.eqConditions())
                    .functionalColumns(functionalColumns);
            for (final var c : qf.otherConditions()) {
                base.addIncluded(c);
            }
            return Set.of(base.build());
        }

        final var families = new LinkedHashSet<IndexFamily>();
        for (final var ineqField : qf.ineqConditions()) {
            final var builder = IndexFamily.builder()
                    .tableName(qf.tableName())
                    .flexColumns(qf.eqConditions())
                    .last(ineqField);

            for (final var f : functionalColumns) {
                builder.addFunctional(f);
            }
            for (final var c : qf.otherConditions()) {
                builder.addIncluded(c);
            }
            for (final var otherIneq : qf.ineqConditions()) {
                if (!otherIneq.equals(ineqField)) {
                    builder.addIncluded(otherIneq);
                }
            }

            families.add(builder.build());
        }
        return families;
    }

    /**
     * Filters out index families that are already covered by existing indexes.
     *
     * @param families the proposed index families
     * @param existing the list of existing concrete indexes
     * @return the set of families not covered by existing indexes
     */
    public Set<IndexFamily> filterExisting(final Set<IndexFamily> families,
                                           final List<ConcreteIndex> existing) {
        final var result = new LinkedHashSet<IndexFamily>();
        for (final var family : families) {
            var covered = false;
            for (final var idx : existing) {
                if (idx.covers(family)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                result.add(family);
            }
        }
        return result;
    }

    /**
     * Minimizes the set of index families by merging overlapping families.
     *
     * @param families the set of index families
     * @return a minimized set of families
     */
    public Set<IndexFamily> minimize(final Set<IndexFamily> families) {
        final var grouped = families.stream()
                .collect(Collectors.groupingBy(IndexFamily::tableName));

        final var result = new LinkedHashSet<IndexFamily>();
        for (final var entry : grouped.entrySet()) {
            final var tableFamilies = new ArrayList<>(entry.getValue());
            tableFamilies.sort(Comparator.comparing(IndexFamily::toString));

            final var merged = new ArrayList<IndexFamily>();
            for (final var family : tableFamilies) {
                var wasMerged = false;
                for (var i = 0; i < merged.size(); i++) {
                    final var m = merger.refine(family, merged.get(i));
                    if (m.isPresent()) {
                        merged.set(i, m.get());
                        wasMerged = true;
                        break;
                    }
                }
                if (!wasMerged) {
                    merged.add(family);
                }
            }
            result.addAll(merged);
        }
        return result;
    }

    /**
     * Projects index families into concrete index definitions.
     *
     * @param families the index families
     * @return a list of concrete index definitions
     */
    public List<ConcreteIndex> projectToConcrete(final Set<IndexFamily> families) {
        final var result = new ArrayList<ConcreteIndex>();
        for (final var family : families) {
            final var indexed = new ArrayList<String>();
            indexed.addAll(family.fixed());
            indexed.addAll(family.flex().stream().sorted().toList());
            if (!family.last().isEmpty()) {
                indexed.add(family.last());
            }

            final var included = family.included().stream().sorted().toList();

            final var type = family.functional().isEmpty()
                    ? IndexType.AGGREGATION
                    : IndexType.FUNCTIONAL;

            result.add(new ConcreteIndex(
                    family.tableName(),
                    List.copyOf(indexed),
                    List.copyOf(included),
                    type,
                    ""
            ));
        }
        return result;
    }

    /**
     * Plans GIN indexes for a query family that has no B-tree indexable conditions.
     *
     * @param qf the aggregate query family
     * @return a set of GIN concrete index proposals
     */
    public Set<ConcreteIndex> planGinIndexes(final AggregateQueryFamily qf) {
        if (qf.hasIndexableConditions()) {
            return Set.of();
        }
        if (!qf.hasGinConditions()) {
            return Set.of();
        }

        final var result = new LinkedHashSet<ConcreteIndex>();
        for (final var col : qf.ginConditions()) {
            result.add(ConcreteIndex.gin(qf.tableName(), col, "jsonb_ops"));
        }
        return result;
    }

    /**
     * Filters out GIN index proposals that are already covered by existing GIN indexes.
     *
     * @param proposals the proposed GIN indexes
     * @param existing  the list of existing GIN indexes
     * @return the set of proposals not covered
     */
    public Set<ConcreteIndex> filterExistingGin(final Set<ConcreteIndex> proposals,
                                                final List<ConcreteIndex> existing) {
        final var result = new LinkedHashSet<ConcreteIndex>();
        for (final var proposal : proposals) {
            var covered = false;
            for (final var existingIdx : existing) {
                if (existingIdx.coversGin(proposal)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                result.add(proposal);
            }
        }
        return result;
    }

    /**
     * Generates a unique index name based on table, columns, and a UUID suffix.
     * Names are truncated to 63 characters (PostgreSQL identifier limit).
     *
     * @param index the concrete index
     * @return a generated index name
     */
    public static String generateName(final ConcreteIndex index) {
        if (index.isGin()) {
            final var sb = new StringBuilder("idx_");
            sb.append(index.tableName()).append("_");
            sb.append(index.indexed().get(0)).append("_gin_");
            final var uuidSuffix = UUID.randomUUID().toString().substring(0, 8);
            sb.append(uuidSuffix);
            return sb.length() > 63 ? sb.substring(0, 63) : sb.toString();
        }

        final var sb = new StringBuilder("idx_");
        sb.append(index.tableName()).append("_");
        for (final var col : index.indexed()) {
            sb.append(col).append("_");
        }
        final var uuidSuffix = UUID.randomUUID().toString().substring(0, 8);
        sb.append(uuidSuffix);

        if (sb.length() > 63) {
            sb.setLength(63);
        }
        return sb.toString();
    }
}
