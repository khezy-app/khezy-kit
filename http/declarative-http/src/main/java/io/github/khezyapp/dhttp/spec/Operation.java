package io.github.khezyapp.dhttp.spec;

import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.Objects;

/**
 * A single operation ("resource + operation") within a spec.
 *
 * @param id    a unique identifier, e.g. {@code contact.create}
 * @param whens  preconditions that gate whether this operation is selected ({@code R3})
 * @param route the route (request shape, sends, output, hooks)
 */
@Builder
public record Operation(String id, @Singular List<Condition> whens, Route route) {

    public Operation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(route, "route");
        whens = List.copyOf(Objects.requireNonNullElseGet(whens, List::of));
    }

    public Operation(final String id,
                     final Route route) {
        this(id, List.of(), route);
    }
}
