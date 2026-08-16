package io.github.khezyapp.dhttp.plan;

import io.github.khezyapp.dhttp.action.PostReceiveStep;
import io.github.khezyapp.dhttp.action.PreSendAction;
import io.github.khezyapp.dhttp.pagination.PaginationStrategy;
import io.github.khezyapp.dhttp.transport.HttpRequest;

import java.util.List;
import java.util.Objects;

/**
 * Output of the planner: a fully resolved request plus the pipeline references (§3.2).
 *
 * @param request      the transport-neutral resolved request
 * @param preSends     pre-send transformation hooks ({@code R6})
 * @param postReceives post-receive shaping steps ({@code R7})
 * @param pagination   pagination strategy, or {@code null}
 * @param maxResults   output item cap ({@code R8})
 * @param authRequest  what credential to use and how, or {@code null}
 */
public record RequestPlan(HttpRequest request,
                          List<PreSendAction> preSends,
                          List<PostReceiveStep> postReceives,
                          PaginationStrategy pagination,
                          int maxResults,
                          AuthRequest authRequest) {

    public RequestPlan {
        Objects.requireNonNull(request, "request");
        preSends = List.copyOf(Objects.requireNonNullElseGet(preSends, List::of));
        postReceives = List.copyOf(Objects.requireNonNullElseGet(postReceives, List::of));
    }
}
