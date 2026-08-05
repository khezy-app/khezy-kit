package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.Options;
import io.github.khezyapp.dynamicform.model.RenderType;
import io.github.khezyapp.dynamicform.model.ValueType;
import io.github.khezyapp.dynamicform.spi.Option;
import io.github.khezyapp.dynamicform.spi.OptionRequest;
import io.github.khezyapp.dynamicform.spi.OptionsProviderRegistry;
import io.github.khezyapp.dynamicform.value.FormValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptionsProviderTest {

    @Test
    @DisplayName("Should load options from a registered provider by name")
    void testProviderLookup() {
        final var registry = OptionsProviderRegistry.empty();
        registry.register("countryList", request -> List.of(
            Option.of("Cambodia", "KH"),
            Option.of("United States", "US")
        ));

        final var engine = FormEngine.of(new FormRuntime(registry, null, null));
        final var field = FieldSchema.builder().name("country").renderType(RenderType.SELECT)
            .valueType(ValueType.STRING).options(Options.provider("countryList")).build();

        final var options = engine.loadOptions(field, FormValues.empty(), EvalContext.defaultContext());

        assertEquals(2, options.size());
        assertEquals("Cambodia", options.get(0).name());
        assertEquals("KH", options.get(0).value());
    }

    @Test
    @DisplayName("Should pass current values to the provider for country->state cascades")
    void testCascadeReceivesCurrentValues() {
        final var seenCountries = new ArrayList<String>();
        final var registry = OptionsProviderRegistry.empty();
        registry.register("stateList", request -> {
            seenCountries.add(String.valueOf(request.currentValues().get("country")));
            return List.of(Option.of("Phnom Penh", "pp"), Option.of("Battambang", "btb"));
        });

        final var engine = FormEngine.of(new FormRuntime(registry, null, null));
        final var field = FieldSchema.builder().name("state").renderType(RenderType.SELECT)
            .valueType(ValueType.STRING).options(Options.provider("stateList")).build();

        final var options = engine.loadOptions(field, FormValues.of(Map.of("country", "US")),
            EvalContext.defaultContext());

        assertEquals(List.of("US"), seenCountries);
        assertEquals("pp", options.get(0).value());
    }

    @Test
    @DisplayName("Should throw when a provider-backed field has no registered provider")
    void testMissingProvider() {
        final var engine = FormEngine.defaultEngine();
        final var field = FieldSchema.builder().name("country").renderType(RenderType.SELECT)
            .valueType(ValueType.STRING).options(Options.provider("unknown")).build();

        final var exception = assertThrows(IllegalArgumentException.class, () ->
            engine.loadOptions(field, FormValues.empty(), EvalContext.defaultContext()));

        assertTrue(exception.getMessage().contains("unknown"));
    }

    @Test
    @DisplayName("Should build an OptionRequest with default pagination")
    void testOptionRequestDefaults() {
        final var request = OptionRequest.of(FormValues.of(Map.of("country", "US")),
            EvalContext.defaultContext());

        assertNull(request.filter());
        assertEquals(0, request.page());
        assertEquals(50, request.pageSize());
        assertEquals("US", request.currentValues().get("country"));
    }
}
