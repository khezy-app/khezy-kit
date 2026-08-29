package io.github.khezyapp.dpriv.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the Aho-Corasick automaton: multi-token matching, longest-match emission, overlap
 * reporting, and {@code compile} validation (design §7.3).
 */
class AhoCorasickTest {

    private static List<int[]> scan(final String input,
                                    final Map<String, List<String>> maskEntities) {
        final var automaton = AhoCorasick.compile(maskEntities);
        final var spans = new ArrayList<int[]>();
        automaton.scan(input, (start, end, token, entityType) -> spans.add(new int[]{start, end}));
        return spans;
    }

    @Test
    @DisplayName("should emit the longest match ending at each position")
    void emitsLongestMatchPerPosition() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_email_address", List.of("visal@example.com"));
        mask.put("secret", List.of("example"));

        final var spans = scan("reach visal@example.com now", mask);

        // Aho-Corasick reports the longest match ending at each position; the shorter "example"
        // ends earlier (19) than the containing email (23), so both are reported in end order.
        // Longest-first resolution happens in the redactor, not the automaton.
        assertThat(spans).containsExactly(
                new int[]{12, 19},
                new int[]{6, 23});
    }

    @Test
    @DisplayName("should report every longest match in ascending end order on a windowed input")
    void reportsMatchesInEndOrder() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_location", List.of("Phnom Penh"));
        mask.put("pii_email_address", List.of("ssok@example.com"));
        mask.put("pii_date_time", List.of("2026-08-29"));

        final var spans = scan("ssok@example.com lives in Phnom Penh on 2026-08-29", mask);

        assertThat(spans).containsExactly(
                new int[]{0, 16},
                new int[]{26, 36},
                new int[]{40, 50});
    }

    @Test
    @DisplayName("should prefer the longer token when overlap shares a start")
    void longerTokenWinsOnOverlap() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_email_address", List.of("visal@example.com"));
        mask.put("secret", List.of("example.com"));

        final var spans = scan("contact visal@example.com", mask);

        assertThat(spans).containsExactly(new int[]{8, 25});
    }

    @Test
    @DisplayName("should reject an empty maskEntities map")
    void rejectsEmptyMaskEntities() {
        assertThatThrownBy(() -> AhoCorasick.compile(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maskEntities");
    }

    @Test
    @DisplayName("should reject an empty token")
    void rejectsEmptyToken() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("secret", List.of(""));
        assertThatThrownBy(() -> AhoCorasick.compile(mask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token");
    }
}
