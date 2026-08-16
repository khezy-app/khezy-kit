package io.github.khezyapp.dhttp.json.jackson3;

import io.github.khezyapp.dhttp.json.JsonMapper;

import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Default {@link JsonMapper} backed by Jackson 3 ({@code tools.jackson.*}).
 *
 * <p>Thread-safe and immutable after construction; record support is enabled by default so record
 * configs round-trip through {@code toMap}/{@code fromMap}. Use {@link #INSTANCE} or construct a new
 * instance with a custom {@link ObjectMapper}.
 */
public final class JacksonJsonMapper implements JsonMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    public static final JacksonJsonMapper INSTANCE = new JacksonJsonMapper();

    private final ObjectMapper mapper;

    public JacksonJsonMapper() {
        this(tools.jackson.databind.json.JsonMapper.builder().build());
    }

    public JacksonJsonMapper(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> toMap(final Object value) {
        return mapper.convertValue(value, MAP_TYPE);
    }

    @Override
    public <T> T fromMap(final Map<String, Object> map,
                         final Class<T> type) {
        return mapper.convertValue(map, type);
    }

    @Override
    public <T> T fromMap(final Map<String, Object> map,
                         final TypeReference<T> type) {
        return mapper.convertValue(map, type);
    }

    @Override
    public String write(final Object value) {
        return mapper.writeValueAsString(value);
    }

    @Override
    public <T> T read(final String json,
                      final Class<T> type) {
        return mapper.readValue(json, type);
    }

    @Override
    public <T> T read(final String json,
                      final TypeReference<T> type) {
        return mapper.readValue(json, type);
    }

    @Override
    public <T> T convert(final Object value,
                         final Class<T> type) {
        return mapper.convertValue(value, type);
    }

    @Override
    public <T> T convert(final Object value,
                         final TypeReference<T> type) {
        return mapper.convertValue(value, type);
    }
}
