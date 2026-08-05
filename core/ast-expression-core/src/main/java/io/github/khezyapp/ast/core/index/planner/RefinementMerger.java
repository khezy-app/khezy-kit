package io.github.khezyapp.ast.core.index.planner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import io.github.khezyapp.ast.core.CoreUtils;
import io.github.khezyapp.ast.core.index.model.IndexFamily;

/**
 * Internal utility for merging overlapping {@link IndexFamily} definitions.
 * <p>
 * Used by {@link IndexPlanner#minimize} to reduce the number of index families
 * by merging those with compatible column sets.
 * </p>
 */
final class RefinementMerger {

    Optional<IndexFamily> refine(final IndexFamily a,
                                 final IndexFamily b) {
        if (!a.tableName().equals(b.tableName())) {
            return Optional.empty();
        }

        final var prefixLen = Math.min(a.fixed().size(), b.fixed().size());
        if (prefixLen > 0) {
            final var prefix = new ArrayList<String>();
            for (var i = 0; i < prefixLen; i++) {
                if (!a.fixed().get(i).equals(b.fixed().get(i))) {
                    return Optional.empty();
                }
                prefix.add(a.fixed().get(i));
            }
            final var aRemainder = stripPrefix(a, prefix);
            final var bRemainder = stripPrefix(b, prefix);
            final var merged = refine(aRemainder, bRemainder);
            return merged.map(m -> m.withFixedPrefix(prefix));
        }

        if (b.fixed().isEmpty()) {
            return mergeWhenFirstHasNoFixed(b, a);
        } else {
            return mergeWhenFirstHasNoFixed(a, b);
        }
    }

    private Optional<IndexFamily> mergeWhenFirstHasNoFixed(final IndexFamily flexible,
                                                           final IndexFamily other) {
        final var flexCols = indexedColumns(flexible);
        final var otherCols = indexedColumns(other);

        if (flexCols.size() > otherCols.size() && new HashSet<>(otherCols).containsAll(flexCols)) {
            return Optional.of(mergeInto(flexible, other));
        }

        if (flexCols.size() == otherCols.size()
                && new HashSet<>(flexCols).equals(new HashSet<>(otherCols))) {
            return Optional.of(mergeInto(flexible, other));
        }

        if (flexCols.size() < otherCols.size() && new HashSet<>(flexCols).containsAll(otherCols)) {
            return Optional.empty();
        }

        if (new HashSet<>(otherCols).containsAll(flexCols)) {
            return mergeSubset(flexible, other);
        }

        return Optional.empty();
    }

    private IndexFamily mergeInto(final IndexFamily source,
                                  final IndexFamily target) {
        final var mergedIncluded = new LinkedHashSet<>(source.included());
        mergedIncluded.addAll(target.included());
        final var mergedFunctional = new LinkedHashSet<>(source.functional());
        mergedFunctional.addAll(target.functional());
        return IndexFamily.builder()
                .tableName(target.tableName())
                .fixedColumns(target.fixed())
                .flexColumns(target.flex())
                .last(target.last())
                .includedColumns(mergedIncluded)
                .functionalColumns(mergedFunctional)
                .build();
    }

    private Optional<IndexFamily> mergeSubset(final IndexFamily smaller, final IndexFamily larger) {
        final var smallerColsSet = new HashSet<>(indexedColumns(smaller));
        final var largerColsSet = new HashSet<>(indexedColumns(larger));

        if (!largerColsSet.containsAll(smallerColsSet)) {
            return Optional.empty();
        }

        for (final var fixedCol : larger.fixed()) {
            if (!smaller.flex().contains(fixedCol)) {
                return Optional.empty();
            }
        }

        if (!smaller.last().isEmpty()
                && !larger.flex().contains(smaller.last())) {
            return Optional.empty();
        }

        final var extraFlexInSmaller = new LinkedHashSet<>(smaller.flex());
        extraFlexInSmaller.removeAll(largerColsSet);

        final var newFixed = new ArrayList<>(larger.fixed());
        newFixed.addAll(extraFlexInSmaller);

        final var mergedIncluded = new LinkedHashSet<>(smaller.included());
        mergedIncluded.addAll(larger.included());

        final var mergedFunctional = new LinkedHashSet<>(smaller.functional());
        mergedFunctional.addAll(larger.functional());

        return Optional.of(IndexFamily.builder()
                .tableName(larger.tableName())
                .fixedColumns(newFixed)
                .flexColumns(larger.flex())
                .last(larger.last())
                .includedColumns(mergedIncluded)
                .functionalColumns(mergedFunctional)
                .build());
    }

    private static IndexFamily stripPrefix(final IndexFamily family,
                                           final List<String> prefix) {
        final var newFixed = new ArrayList<>(family.fixed());
        newFixed.removeAll(prefix);
        return IndexFamily.builder()
                .tableName(family.tableName())
                .fixedColumns(newFixed)
                .flexColumns(family.flex())
                .last(family.last())
                .includedColumns(family.included())
                .functionalColumns(family.functional())
                .build();
    }

    private static List<String> indexedColumns(final IndexFamily f) {
        final var cols = new ArrayList<String>();
        cols.addAll(f.fixed());
        cols.addAll(f.flex());
        if (CoreUtils.isNotEmpty(f.last())) {
            cols.add(f.last());
        }
        return cols;
    }
}
