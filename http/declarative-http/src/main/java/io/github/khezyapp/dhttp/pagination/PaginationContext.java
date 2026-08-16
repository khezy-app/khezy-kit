package io.github.khezyapp.dhttp.pagination;

import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.transport.HttpResult;

/**
 * Carries everything a pagination decision needs: the plan, the last response, and whether the last
 * response indicated more pages ({@code R9}).
 *
 * @param plan              the resolved request plan
 * @param last              the last response received
 * @param continueEvaluated whether the last response indicated more pages
 */
public record PaginationContext(RequestPlan plan,
                                HttpResult last,
                                boolean continueEvaluated) {
}
