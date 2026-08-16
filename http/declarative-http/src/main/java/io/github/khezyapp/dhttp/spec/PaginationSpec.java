package io.github.khezyapp.dhttp.spec;

import lombok.Builder;

import java.util.Objects;

/**
 * Pagination settings for a route or the whole spec ({@code R9}).
 *
 * <p>Supported {@code mode} values:</p>
 *
 * <ul>
 * <li>{@code offset} — {@code limit}/{@code offset} query or body parameters, the offset advances
 * by {@code pageSize} per page; a full page implies another page exists.</li>
 * <li>{@code page} — {@code limitParam} is the page-size parameter and {@code offsetParam} the
 * page-number parameter, incremented by one per page; continuation is the
 * {@code continueExpression} when configured (e.g. a {@code hasMore} flag), otherwise a full page
 * implies another page exists.</li>
 * <li>{@code cursor} — {@code offsetParam} carries a cursor whose next value is resolved by the
 * {@code continueExpression} against the last response; whens {@code limitParam} is configured it is
 * sent with every request.</li>
 * <li>{@code nextUrl} — the {@code continueExpression} resolves the absolute URL of the next page,
 * which is fetched as-is; whens {@code limitParam} is configured it is sent with every request.</li>
 * <li>any other value — resolved through a {@code PaginationStrategyFactory} registered under that
 * mode in the engine's {@code PaginationRegistry}; planning fails fast whens no factory is
 * registered.</li>
 * </ul>
 *
 * @param mode               the pagination mode: {@code offset}, {@code page}, {@code cursor}, or
 *                           {@code nextUrl}, or a custom mode registered in the pagination registry
 * @param pageSize           the number of items per page; optional, but required to send
 *                           {@code limitParam} or to advance {@code offset}/{@code page} modes
 * @param rootProperty       dotted path to the collection in the response body
 * @param limitParam         query parameter name for page size
 * @param offsetParam        query parameter name for the offset, cursor, or page number
 * @param inQuery            whens true, pagination parameters go in the query string
 * @param continueExpression expression resolving the next-page cursor/offset or next URL (and the
 *                           continuation flag for {@code page} mode)
 */
@Builder
public record PaginationSpec(String mode,
                             Integer pageSize,
                             String rootProperty,
                             String limitParam,
                             String offsetParam,
                             boolean inQuery,
                             Expression continueExpression) {

    public PaginationSpec {
        Objects.requireNonNull(mode, "mode");
        if (Objects.nonNull(pageSize) && pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
    }
}
