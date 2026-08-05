package io.github.khezyapp.dynamicform.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Declarative visibility rules for a field.
 * <p>
 * {@code show} is an <strong>AND</strong> of named dependency predicates — every dependency's
 * condition set must be satisfied. {@code hide} is an <strong>OR</strong> — any matching predicate
 * hides the field. The same {@link Condition} language powers both, plus conditional-required.
 * <p>
 * Dependency names are dot-paths into the form values (e.g. {@code "customerType"}), or the special
 * context references {@code @version}, {@code @deployment}, and {@code @feature[:name]} evaluated
 * against the {@code EvalContext}.
 *
 * @param show the AND-grouped conditions that must all match for the field to be shown
 * @param hide the OR-grouped conditions, any match hides the field
 */
public record Visibility(
    Map<String, List<Condition>> show,
    Map<String, List<Condition>> hide
) {

    /**
     * Compact canonical constructor that normalises null groups to empty immutable maps.
     */
    public Visibility {
        show = normalize(show);
        hide = normalize(hide);
    }

    /**
     * Creates a visibility with only a {@code show} group.
     *
     * @param show the AND-grouped show conditions
     * @return a new visibility rule
     */
    public static Visibility of(final Map<String, List<Condition>> show) {
        return new Visibility(show, null);
    }

    /**
     * Creates a visibility showing the field when a single dependency matches all given conditions.
     *
     * @param name       the dependency path
     * @param conditions the conditions (AND) the dependency must satisfy
     * @return a new visibility rule
     */
    public static Visibility show(final String name, final Condition... conditions) {
        return new Visibility(Map.of(name, List.of(conditions)), null);
    }

    private static Map<String, List<Condition>> normalize(final Map<String, List<Condition>> groups) {
        final var result = new LinkedHashMap<String, List<Condition>>();
        if (Objects.nonNull(groups)) {
            groups.forEach((key, conditions) -> result.put(key, List.copyOf(conditions)));
        }
        return result;
    }
}
