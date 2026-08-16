package io.github.khezyapp.dhttp.spec;

import java.util.List;
import java.util.Objects;

/**
 * Output shaping configuration: an optional item cap plus a chain of {@link PostReceive} steps.
 *
 * @param maxResults  the maximum number of output items ({@code R8})
 * @param postReceive the ordered post-response shaping steps ({@code R7})
 */
public record Output(int maxResults, List<PostReceive> postReceive) {

    public Output {
        if (maxResults < 0) {
            throw new IllegalArgumentException("maxResults must be non-negative");
        }
        postReceive = List.copyOf(Objects.requireNonNullElseGet(postReceive, List::of));
    }

    public static Output of(final int maxResults) {
        return new Output(maxResults, List.of());
    }
}
