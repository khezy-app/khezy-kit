package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.FileSpec;
import io.github.khezyapp.dynamicform.model.FormSchema;
import io.github.khezyapp.dynamicform.model.RenderType;
import io.github.khezyapp.dynamicform.model.ValueType;
import io.github.khezyapp.dynamicform.spi.InMemoryFileUploadProvider;
import io.github.khezyapp.dynamicform.spi.UploadedRef;
import io.github.khezyapp.dynamicform.value.FormValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadTest {

    private static final EvalContext CTX = EvalContext.defaultContext();

    private static FormSchema fileForm(final FileSpec spec,
                                       final ValueType valueType) {
        return FormSchema.of("upload", 1, "forms.upload.title", List.of(
            FieldSchema.builder().name("photo").renderType(RenderType.FILE)
                .valueType(valueType).file(spec).build()
        ));
    }

    @Test
    @DisplayName("Should upload raw bytes and return a stable reference")
    void testUploadBytes() {
        final var engine = FormEngine.defaultEngine();
        final var schema = fileForm(FileSpec.any(), ValueType.FILE);
        final var bytes = "photo-content".getBytes(StandardCharsets.UTF_8);

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("photo", bytes)), CTX);

        assertTrue(resolved.isValid());
        final var ref = (UploadedRef) resolved.values().get("photo");
        assertEquals(bytes.length, ref.size());
        assertNotNull(ref.ref());
        assertEquals("application/octet-stream", ref.mime());

        final var provider = (InMemoryFileUploadProvider) engine.uploadRegistry().resolve(null);
        assertArrayEquals(bytes, provider.retrieve(ref.ref()));
    }

    @Test
    @DisplayName("Should reject an upload whose mime type is not accepted")
    void testMimeRejected() {
        final var engine = FormEngine.defaultEngine();
        final var schema = fileForm(FileSpec.of(0, 0, List.of("application/pdf")), ValueType.FILE);

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("photo", Map.of(
            "bytes", new byte[]{1, 2, 3},
            "mime", "text/plain"
        ))), CTX);

        assertFalse(resolved.isValid());
        assertEquals("photo", resolved.issues().get(0).path());
        assertTrue(resolved.issues().get(0).message().contains("not accepted"));
    }

    @Test
    @DisplayName("Should reject an upload exceeding maxBytes")
    void testSizeRejected() {
        final var engine = FormEngine.defaultEngine();
        final var schema = fileForm(FileSpec.of(10, 0, List.of()), ValueType.FILE);

        final var resolved = engine.resolve(schema,
            FormValues.of(Map.of("photo", new byte[100])), CTX);

        assertFalse(resolved.isValid());
        assertTrue(resolved.issues().get(0).message().contains("maxBytes"));
    }

    @Test
    @DisplayName("Should reject a multi-file upload exceeding maxCount")
    void testMaxCountRejected() {
        final var engine = FormEngine.defaultEngine();
        final var schema = fileForm(FileSpec.of(0, 2, List.of()), ValueType.ARRAY);

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("photo", List.of(
            new byte[1], new byte[1], new byte[1]
        ))), CTX);

        assertFalse(resolved.isValid());
        assertTrue(resolved.issues().stream()
            .anyMatch(issue -> issue.path().equals("photo") && issue.message().contains("exceed 2")));
    }

    @Test
    @DisplayName("Should pass through an already-uploaded reference")
    void testUploadedRefPassthrough() {
        final var engine = FormEngine.defaultEngine();
        final var schema = fileForm(FileSpec.of(0, 0, List.of("image/png")), ValueType.FILE);
        final var ref = UploadedRef.of("existing", "https://cdn/pic.png", new byte[42], "image/png");

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("photo", ref)), CTX);

        assertTrue(resolved.isValid());
        assertSame(ref, resolved.values().get("photo"));
    }
}
