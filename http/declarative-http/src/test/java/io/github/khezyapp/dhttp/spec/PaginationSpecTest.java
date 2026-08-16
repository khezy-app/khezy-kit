package io.github.khezyapp.dhttp.spec;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaginationSpecTest {

    @Test
    @DisplayName("Should allow a missing pageSize, validating it only when configured")
    void allowsMissingPageSize() {
        final var spec = assertDoesNotThrow(() ->
                new PaginationSpec("cursor", null, "data", "limit", "cursor", true, null));

        assertNull(spec.pageSize());
    }

    @Test
    @DisplayName("Should reject a non-positive pageSize when configured")
    void rejectsNonPositivePageSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new PaginationSpec("offset", 0, "data", "limit", "offset", true, null));
        assertThrows(IllegalArgumentException.class, () ->
                new PaginationSpec("offset", -1, "data", "limit", "offset", true, null));
    }

    @Test
    @DisplayName("Should require a mode")
    void requiresMode() {
        assertThrows(NullPointerException.class, () ->
                new PaginationSpec(null, 10, "data", "limit", "offset", true, null));
    }
}
