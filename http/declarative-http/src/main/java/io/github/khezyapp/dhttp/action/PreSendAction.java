package io.github.khezyapp.dhttp.action;

import io.github.khezyapp.dhttp.transport.HttpRequest;

/**
 * Transforms a request before it is sent ({@code R6}). Applied in order by the pipeline.
 */
@FunctionalInterface
public interface PreSendAction {

    /**
     * @param request the request about to be sent
     * @return the (possibly transformed) request
     */
    HttpRequest apply(HttpRequest request);
}
