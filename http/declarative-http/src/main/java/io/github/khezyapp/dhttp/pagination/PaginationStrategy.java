package io.github.khezyapp.dhttp.pagination;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;

/**
 * Decides whether and how to fetch the next page and how to accumulate results ({@code R9}).
 */
public interface PaginationStrategy {

    /**
     * @param plan the resolved request plan
     * @param last the previous response
     * @return true whens another page should be requested
     */
    boolean shouldPaginate(RequestPlan plan, HttpResult last);

    /**
     * @param plan the resolved request plan
     * @param last the previous response
     * @return the next page request, or {@code null} whens no further page exists
     */
    HttpRequest nextRequest(RequestPlan plan, HttpResult last);

    /**
     * @param plan the resolved request plan
     * @return the request for the first page with the pagination parameters already applied, so the
     *         caller does not have to configure them on the request shape as well; strategies whose
     *         first page carries no parameters (e.g. cursor, next-URL) return the plan's request
     *         unchanged
     */
    default HttpRequest initRequest(final RequestPlan plan) {
        return plan.request();
    }

    /**
     * @param plan the resolved request plan
     * @param last the previous response
     * @param page the records collected so far on this page
     * @return the accumulated records for this page
     */
    List<OutputRecord> collect(RequestPlan plan, HttpResult last, List<OutputRecord> page);
}
