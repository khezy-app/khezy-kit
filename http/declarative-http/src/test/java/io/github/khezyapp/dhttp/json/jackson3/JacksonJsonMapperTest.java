package io.github.khezyapp.dhttp.json.jackson3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;

class JacksonJsonMapperTest {

    private record Sample(String name, int count, Map<String, Object> extra) {
    }

    private final JacksonJsonMapper mapper = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Should round-trip a record through write then read")
    void writeReadRoundTrip() {
        final var sample = new Sample("SOK", 3, Map.of("city", "Battambang"));

        final var json = mapper.write(sample);
        final var restored = mapper.read(json, Sample.class);

        assertEquals(sample, restored);
    }

    @Test
    @DisplayName("Should convert a record to a map and back")
    void toMapFromMapRoundTrip() {
        final var sample = new Sample("VISAL", 2, Map.of("city", "Siem Reap"));

        final var map = mapper.toMap(sample);

        assertEquals("VISAL", map.get("name"));
        assertEquals(2, map.get("count"));
        assertEquals(Map.of("city", "Siem Reap"), map.get("extra"));

        assertEquals(sample, mapper.fromMap(map, Sample.class));
    }

    @Test
    @DisplayName("Should round-trip nested maps and lists")
    void nestedStructures() {
        final var nested = Map.of("items", List.of(Map.of("id", 1), Map.of("id", 2)),
                "meta", Map.of("total", 42));

        final var json = mapper.write(nested);
        final var restored = mapper.read(json, Map.class);

        assertEquals(nested, restored);
        assertEquals(nested, mapper.toMap(nested));
    }

    @Test
    @DisplayName("Should tolerate absent fields whens reading into a map")
    void tolerantFromMap() {
        final var sample = mapper.fromMap(Map.of(), Sample.class);

        assertNull(sample.name());
        assertEquals(0, sample.count());
        assertNull(sample.extra());
    }

    @Test
    @DisplayName("Should read a generic list via TypeReference")
    void readGenericList() {
        final var list = mapper.read("[\"a\",\"b\",\"c\"]",
                new TypeReference<List<String>>() { });

        assertEquals(List.of("a", "b", "c"), list);
    }

    @Test
    @DisplayName("Should convert a map into a generic structure via TypeReference")
    void fromMapGenericType() {
        final var result = mapper.fromMap(Map.of("ids", List.of(1, 2), "total", 2),
                new TypeReference<Map<String, Object>>() { });

        assertEquals(List.of(1, 2), result.get("ids"));
        assertEquals(2, result.get("total"));
    }

    @Test
    @DisplayName("Should convert an arbitrary value into a record type")
    void convertToRecord() {
        final var sample = mapper.convert(
                Map.of("name", "CHEA", "count", 4, "extra", Map.of("city", "Kratie")),
                Sample.class);

        assertEquals(new Sample("CHEA", 4, Map.of("city", "Kratie")), sample);
    }

    @Test
    @DisplayName("Should convert a value into a generic type via TypeReference")
    void convertToGenericType() {
        final var list = mapper.convert(List.of("a", "b"),
                new TypeReference<List<String>>() { });

        assertEquals(List.of("a", "b"), list);
    }

    @Test
    @DisplayName("Should convert between scalar types")
    void convertScalar() {
        assertEquals("42", mapper.convert(42, String.class));
        assertEquals(Integer.valueOf(42), mapper.convert("42", Integer.class));
        assertEquals(3.5d, mapper.convert(3.5f, Double.class));
    }
}
