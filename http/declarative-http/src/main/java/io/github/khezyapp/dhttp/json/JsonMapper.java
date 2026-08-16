package io.github.khezyapp.dhttp.json;

import java.util.Map;

import tools.jackson.core.type.TypeReference;

/**
 * JSON / object-mapper SPI: converts between typed objects, generic maps, and JSON strings.
 *
 * <p>Default implementation: {@code jackson3.JacksonJsonMapper} over Jackson 3. Consumers (credential
 * service, engine) plug any conforming implementation in.
 */
public interface JsonMapper {

    /**
     * @param value the object to convert
     * @return the value as a generic map
     */
    Map<String, Object> toMap(Object value);

    /**
     * @param map  the generic map to convert
     * @param type the target type (e.g. a record class)
     * @param <T>  the target type
     * @return the map as an instance of {@code type}
     */
    <T> T fromMap(Map<String, Object> map, Class<T> type);

    /**
     * @param map  the generic map to convert
     * @param type the target type (e.g. {@code new TypeReference<List<String>>() { }})
     * @param <T>  the target type
     * @return the map as an instance of {@code type}
     */
    <T> T fromMap(Map<String, Object> map, TypeReference<T> type);

    /**
     * Converts an arbitrary value into the requested type, using JSON semantics (records, POJOs,
     * collections, primitives, {@code Object}).
     *
     * @param value the value to convert
     * @param type  the target type
     * @param <T>   the target type
     * @return the value as an instance of {@code type}
     */
    <T> T convert(Object value, Class<T> type);

    /**
     * Converts an arbitrary value into a generic type, using JSON semantics.
     *
     * @param value the value to convert
     * @param type  the target type (e.g. {@code new TypeReference<List<String>>() { }})
     * @param <T>   the target type
     * @return the value as an instance of {@code type}
     */
    <T> T convert(Object value, TypeReference<T> type);

    /**
     * @param value the object to serialize
     * @return the JSON string
     */
    String write(Object value);

    /**
     * @param json the JSON string to parse
     * @param type the target type
     * @param <T>  the target type
     * @return the parsed instance of {@code type}
     */
    <T> T read(String json, Class<T> type);

    /**
     * @param json the JSON string to parse
     * @param type the target type (e.g. {@code new TypeReference<List<String>>() { }})
     * @param <T>  the target type
     * @return the parsed instance of {@code type}
     */
    <T> T read(String json, TypeReference<T> type);
}

