package io.github.khezyapp.dynamicform.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormSchemaJacksonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String EXAMPLE_JSON = """
        {
          "id": "kyc.personal-info",
          "version": 1,
          "titleKey": "forms.personalInfo.title",
          "fields": [
            {
              "name": "country",
              "displayNameKey": "forms.personalInfo.country",
              "renderType": "SELECT",
              "valueType": "STRING",
              "default": "US",
              "options": { "provider": "countryList" }
            },
            {
              "name": "state",
              "displayNameKey": "forms.personalInfo.state",
              "renderType": "SELECT",
              "valueType": "STRING",
              "default": null,
              "options": { "provider": "stateList", "dependsOn": ["country"] },
              "visibility": { "show": { "country": [ { "op": "EXISTS" } ] } }
            },
            {
              "name": "dateOfBirth",
              "displayNameKey": "forms.personalInfo.dateOfBirth",
              "renderType": "DATE_TIME",
              "valueType": "DATE_TIME",
              "constraints": { "required": true }
            },
            {
              "name": "documents",
              "displayNameKey": "forms.personalInfo.documents",
              "renderType": "GROUP",
              "valueType": "OBJECT",
              "children": [
                {
                  "name": "idType",
                  "renderType": "SELECT",
                  "valueType": "STRING",
                  "default": "passport",
                  "options": {
                    "inline": [
                      { "name": "Passport", "value": "passport" },
                      { "name": "Driving licence", "value": "dl" }
                    ]
                  }
                },
                {
                  "name": "idNumber",
                  "renderType": "STRING",
                  "valueType": "STRING",
                  "default": "",
                  "constraints": { "required": true }
                }
              ]
            },
            {
              "name": "notes",
              "renderType": "NOTICE",
              "valueType": null,
              "meta": { "textKey": "forms.personalInfo.notes" }
            }
          ]
        }
        """;

    @Test
    @DisplayName("Should deserialize the example schema JSON")
    void testDeserializeExample() throws Exception {
        final var schema = MAPPER.readValue(EXAMPLE_JSON, FormSchema.class);

        assertEquals("kyc.personal-info", schema.id());
        assertEquals(1, schema.version());
        assertEquals("forms.personalInfo.title", schema.titleKey());
        assertEquals(5, schema.fields().size());

        final var country = schema.fields().get(0);
        assertEquals("US", country.defaultValue());
        assertEquals("countryList", country.options().provider());

        final var state = schema.fields().get(1);
        assertEquals(RenderType.SELECT, state.renderType());
        assertEquals("stateList", state.options().provider());
        assertEquals(List.of("country"), state.options().dependsOn());
        assertEquals(Op.EXISTS, state.visibility().show().get("country").get(0).op());

        final var dateOfBirth = schema.fields().get(2);
        assertTrue(dateOfBirth.constraints().required());

        final var documents = schema.fields().get(3);
        assertEquals(ValueType.OBJECT, documents.valueType());
        assertEquals(2, documents.children().size());
        assertEquals("passport", documents.children().get(0).defaultValue());
        assertEquals("dl", documents.children().get(0).options().inline().get(1).value());
        assertTrue(documents.children().get(1).constraints().required());

        final var notes = schema.fields().get(4);
        assertNull(notes.valueType());
        assertEquals("forms.personalInfo.notes", notes.meta().get("textKey"));
    }

    @Test
    @DisplayName("Should round-trip serialize and deserialize")
    void testRoundTrip() throws Exception {
        final var original = MAPPER.readValue(EXAMPLE_JSON, FormSchema.class);
        final var json = MAPPER.writeValueAsString(original);
        final var restored = MAPPER.readValue(json, FormSchema.class);

        assertEquals(original, restored);
    }
}
