package io.github.khezyapp.dhttp.spec;

import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.Objects;

/**
 * A single request shape with its pre-send hooks, parameter mapping, and output shaping.
 *
 * @param request    the concrete request shape
 * @param sends      parameter-to-request mapping ({@code R4})
 * @param output     output cap and post-receive steps ({@code R7}, {@code R8})
 * @param pagination pagination settings ({@code R9})
 * @param preSends   pre-send transformation hooks ({@code R6})
 */
@Builder
public record Route(RequestShape request,
                    @Singular List<Send> sends,
                    Output output,
                    PaginationSpec pagination,
                    @Singular List<PreSend> preSends) {

    public Route {
        Objects.requireNonNull(request, "request");
        sends = List.copyOf(Objects.requireNonNullElseGet(sends, List::of));
        preSends = List.copyOf(Objects.requireNonNullElseGet(preSends, List::of));
    }

    public Route(final RequestShape request,
                 final Output output) {
        this(request, List.of(), output, null, List.of());
    }
}
