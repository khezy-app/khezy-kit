package io.github.khezyapp.dhttp.action;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;

/**
 * Shapes the output records after a response arrives ({@code R7}). Applied in pipeline order by the
 * engine.
 */
@FunctionalInterface
public interface PostReceiveAction {

    /**
     * @param records  the current output records
     * @param response the received HTTP response
     * @return the shaped output records (may be a different list)
     */
    List<OutputRecord> apply(List<OutputRecord> records, HttpResult response);
}
