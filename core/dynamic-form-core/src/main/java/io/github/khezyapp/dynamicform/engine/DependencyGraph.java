package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.FieldSchema;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Static dependency analysis over distinct field names.
 * <p>
 * The authoritative resolution order is derived at runtime from live visibility (see
 * {@link ResolveEngine}); this class provides only a cheap early hint: a static topological
 * pre-sort, plus detection of hard cycles among name-based dependencies. It cannot express
 * duplicate-name declarations with different visibility — that is exactly why the runtime resolver
 * exists.
 */
public final class DependencyGraph {

    private DependencyGraph() {
    }

    /**
     * A static topological pre-sort hint of the distinct field names.
     *
     * @param fields the field declarations
     * @return the names in dependency order (may be incomplete when a cycle exists)
     */
    public static List<String> suggestedOrder(final List<FieldSchema> fields) {
        final var names = distinctNames(fields);
        final var inDegree = new HashMap<String, Integer>();
        final var dependents = new HashMap<String, Set<String>>();
        for (final var name : names) {
            inDegree.put(name, 0);
            dependents.put(name, new HashSet<>());
        }
        for (final var name : names) {
            for (final var dep : dependencyNamesOf(name, fields)) {
                if (names.contains(dep) && !dep.equals(name)) {
                    inDegree.put(name, inDegree.get(name) + 1);
                    dependents.get(dep).add(name);
                }
            }
        }

        final var queue = new ArrayDeque<String>();
        for (final var name : names) {
            if (inDegree.get(name) == 0) {
                queue.add(name);
            }
        }
        final var order = new ArrayList<String>();
        while (!queue.isEmpty()) {
            final var name = queue.poll();
            order.add(name);
            for (final var dependent : dependents.get(name)) {
                inDegree.put(dependent, inDegree.get(dependent) - 1);
                if (inDegree.get(dependent) == 0) {
                    queue.add(dependent);
                }
            }
        }
        return order;
    }

    /**
     * The names involved in a static dependency cycle, if any.
     *
     * @param fields the field declarations
     * @return the names left unresolved after Kahn's algorithm (the cycle members)
     */
    public static List<String> findCycle(final List<FieldSchema> fields) {
        final var order = suggestedOrder(fields);
        final var resolved = new HashSet<>(order);
        final var cycle = new ArrayList<String>();
        for (final var name : distinctNames(fields)) {
            if (!resolved.contains(name)) {
                cycle.add(name);
            }
        }
        return cycle;
    }

    /**
     * Whether the declarations contain a static dependency cycle.
     *
     * @param fields the field declarations
     * @return {@code true} when a cycle exists
     */
    public static boolean hasCycle(final List<FieldSchema> fields) {
        return !findCycle(fields).isEmpty();
    }

    private static List<String> distinctNames(final List<FieldSchema> fields) {
        final var names = new LinkedHashSet<String>();
        for (final var field : fields) {
            names.add(field.name());
        }
        return List.copyOf(names);
    }

    private static Set<String> dependencyNamesOf(final String name,
                                                 final List<FieldSchema> fields) {
        final var deps = new HashSet<String>();
        for (final var field : fields) {
            if (field.name().equals(name)) {
                deps.addAll(ResolveEngine.dependencyNames(field));
            }
        }
        return deps;
    }
}
