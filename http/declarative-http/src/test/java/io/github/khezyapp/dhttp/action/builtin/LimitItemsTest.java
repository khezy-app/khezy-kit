package io.github.khezyapp.dhttp.action.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LimitItemsTest {

    @Test
    @DisplayName("Should cap the records at max")
    void capsAtMax() {
        final var records = records(5);

        final var limited = new LimitItems(3).apply(records, HttpResult.of(200, "{}"));

        assertEquals(3, limited.size());
    }

    @Test
    @DisplayName("Should keep all records whens size is within the cap")
    void keepsAllUnderCap() {
        final var records = records(2);

        final var result = new LimitItems(5).apply(records, HttpResult.of(200, "{}"));

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should yield nothing for a zero cap")
    void zeroCapYieldsNothing() {
        final var records = records(3);

        final var result = new LimitItems(0).apply(records, HttpResult.of(200, "{}"));

        assertTrue(result.isEmpty());
    }

    private static List<OutputRecord> records(final int count) {
        final var records = new ArrayList<OutputRecord>();
        for (int i = 1; i <= count; i++) {
            records.add(OutputRecord.ofJson(Map.of("id", i)));
        }
        return records;
    }
}
