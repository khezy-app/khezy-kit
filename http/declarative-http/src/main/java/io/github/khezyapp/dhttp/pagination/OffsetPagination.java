package io.github.khezyapp.dhttp.pagination;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.plan.RequestPlan;
import io.github.khezyapp.dhttp.spec.PaginationSpec;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Offset-based pagination built-in ({@code R9}): pages are advanced by incrementing an offset by the
 * page size, and {@code limit}/{@code offset} parameters are placed either in the query string or in
 * the JSON body.
 */
public final class OffsetPagination implements PaginationStrategy {

    private final Integer pageSize;
    private final String rootProperty;
    private final String limitParam;
    private final String offsetParam;
    private final boolean inQuery;
    private final JsonMapper jsonMapper;
    private int offset;
    private int collected;

    private OffsetPagination(final PaginationSpec spec,
                             final JsonMapper jsonMapper) {
        this.pageSize = spec.pageSize();
        this.rootProperty = spec.rootProperty();
        this.limitParam = spec.limitParam();
        this.offsetParam = spec.offsetParam();
        this.inQuery = spec.inQuery();
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    /**
     * @param spec       the pagination settings
     * @param jsonMapper the JSON mapper used to count page records
     * @return a new offset strategy
     */
    public static OffsetPagination from(final PaginationSpec spec,
                                        final JsonMapper jsonMapper) {
        return new OffsetPagination(spec, jsonMapper);
    }

    @Override
    public boolean shouldPaginate(final RequestPlan plan,
                                  final HttpResult last) {
        if (plan.maxResults() > 0 && collected >= plan.maxResults()) {
            return false;
        }
        return Objects.nonNull(pageSize)
                && PaginationSupport.countRecords(last, jsonMapper, rootProperty) >= pageSize;
    }

    @Override
    public HttpRequest nextRequest(final RequestPlan plan,
                                   final HttpResult last) {
        offset += pageSize;
        return paginated(plan, offset);
    }

    @Override
    public HttpRequest initRequest(final RequestPlan plan) {
        return paginated(plan, offset);
    }

    private HttpRequest paginated(final RequestPlan plan,
                                  final int offsetValue) {
        final var builder = plan.request().toBuilder();
        if (inQuery) {
            if (Objects.nonNull(pageSize)) {
                builder.query(limitParam, pageSize);
            }
            builder.query(offsetParam, offsetValue);
        } else {
            final var params = new LinkedHashMap<String, Object>();
            if (Objects.nonNull(pageSize)) {
                params.put(limitParam, pageSize);
            }
            params.put(offsetParam, offsetValue);
            builder.body(
                    PaginationSupport.withBodyParams(
                            plan.request().body(),
                            jsonMapper,
                            params
                    )
            );
        }
        return builder.build();
    }

    @Override
    public List<OutputRecord> collect(final RequestPlan plan,
                                      final HttpResult last,
                                      final List<OutputRecord> page) {
        collected += PaginationSupport.countRecords(last, jsonMapper, rootProperty);
        return page;
    }
}
