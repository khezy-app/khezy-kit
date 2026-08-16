package io.github.khezyapp.dhttp.engine;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One page of dropdown options returned by {@code describe(...)} (R15): the shaped {@link
 * OptionItem}s plus the paging state that lets a searchable/paginated loader render the next
 * action.
 *
 * <p>The paging fields are driven by the option-shaping action, which already reads the full
 * response: it stamps {@code hasMore} and either a single {@code nextCursor} or a structured
 * {@code nextParameters} map into {@link OutputRecord#metadata()}, and the engine aggregates them
 * into this page. The caller echoes the cursor or the whole parameter map back into the next
 * {@link RequestContext}, and the request shape binds each name with {@code $parameter}.</p>
 *
 * @param items          the shaped options of this page
 * @param hasMore        whether another page can be loaded
 * @param nextCursor     opaque cursor for the next page, or {@code null} whens the action did not
 *                       expose one
 * @param nextParameters the request parameters for the next page (e.g. {@code offset}, {@code page},
 *                       a date range), or empty whens the action did not expose any
 */
public record OptionPage(List<OptionItem> items,
                         boolean hasMore,
                         String nextCursor,
                         Map<String, Object> nextParameters) {

    public OptionPage {
        items = List.copyOf(Objects.requireNonNullElseGet(items, List::of));
        nextParameters = Map.copyOf(Objects.requireNonNullElseGet(nextParameters, Map::of));
    }

    public OptionPage(final List<OptionItem> items,
                      final boolean hasMore,
                      final String nextCursor) {
        this(items, hasMore, nextCursor, Map.of());
    }
}
