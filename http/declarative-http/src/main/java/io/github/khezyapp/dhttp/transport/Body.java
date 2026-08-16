package io.github.khezyapp.dhttp.transport;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Request body sealed model. Every variant carries its payload and reports its {@link BodyKind}.
 */
public sealed interface Body
        permits Body.JsonBody, Body.FormBody, Body.UrlEncodedBody, Body.RawBody, Body.BinaryBody,
        Body.NoBody {

    /**
     * The transport-level body kind.
     */
    enum BodyKind {
        JSON,
        FORM,
        URLENCODED,
        RAW,
        BINARY,
        NONE
    }

    BodyKind kind();

    /**
     * A JSON payload, sent with {@code Content-Type: application/json}.
     *
     * @param json the serialized JSON string
     */
    record JsonBody(String json) implements Body {

        public JsonBody {
            Objects.requireNonNull(json, "json");
        }

        @Override
        public BodyKind kind() {
            return BodyKind.JSON;
        }
    }

    /**
     * A {@code multipart/form-data} payload.
     *
     * <p>Values may be any non-null {@link String}, number, boolean, or a {@link FilePart}.
     * Primitive values are converted to strings by the transport; {@link FilePart} values are sent
     * as file parts carrying their original filename and content type.</p>
     *
     * @param fields the form fields
     */
    record FormBody(Map<String, ?> fields) implements Body {

        public FormBody {
            Objects.requireNonNull(fields, "fields");
            final var copy = new LinkedHashMap<String, Object>();
            for (final var entry : fields.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "form field name");
                Objects.requireNonNull(entry.getValue(), "form field value");
                copy.put(entry.getKey(), entry.getValue());
            }
            fields = Collections.unmodifiableMap(copy);
        }

        @Override
        public BodyKind kind() {
            return BodyKind.FORM;
        }

        /**
         * A file part inside a {@link FormBody}. The transport emits it as a multipart part with
         * {@code Content-Disposition: form-data; name="..." ; filename="..."} and the given content
         * type (defaulting to {@code application/octet-stream}).
         *
         * @param bytes       the file bytes
         * @param fileName    the original file name, may be {@code null}
         * @param contentType the content type, may be {@code null}
         */
        public record FilePart(byte[] bytes, String fileName, String contentType) {

            public FilePart {
                Objects.requireNonNull(bytes, "bytes");
            }
        }
    }

    /**
     * An {@code application/x-www-form-urlencoded} payload.
     *
     * <p>Fields are percent-encoded (UTF-8, spaces become {@code +}) in insertion order and joined
     * as {@code key=value&key=value...}. Values may be any non-null {@link String}, number, or
     * boolean.</p>
     *
     * @param body the fully serialized form string
     */
    record UrlEncodedBody(String body) implements Body {

        public UrlEncodedBody {
            Objects.requireNonNull(body, "body");
        }

        /**
         * Encodes the fields into a form string.
         *
         * @param fields the form fields
         */
        public UrlEncodedBody(final Map<String, ?> fields) {
            this(encode(fields));
        }

        private static String encode(final Map<String, ?> fields) {
            Objects.requireNonNull(fields, "fields");
            final var sb = new StringBuilder();
            for (final var entry : fields.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "form field name");
                Objects.requireNonNull(entry.getValue(), "form field value");
                if (!sb.isEmpty()) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
            }
            return sb.toString();
        }

        @Override
        public BodyKind kind() {
            return BodyKind.URLENCODED;
        }
    }

    /**
     * A raw payload with an explicit content type.
     *
     * @param contentType the content type, e.g. {@code application/xml}
     * @param bytes       the raw bytes
     */
    record RawBody(String contentType, byte[] bytes) implements Body {

        public RawBody {
            Objects.requireNonNull(contentType, "contentType");
            Objects.requireNonNull(bytes, "bytes");
        }

        @Override
        public BodyKind kind() {
            return BodyKind.RAW;
        }
    }

    /**
     * A binary payload.
     *
     * <p>When {@code contentType} is {@code null} the payload is sent as
     * {@code application/octet-stream}.</p>
     *
     * @param bytes       the binary bytes
     * @param contentType the content type, may be {@code null} to use the default
     */
    record BinaryBody(byte[] bytes, String contentType) implements Body {

        private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

        public BinaryBody {
            Objects.requireNonNull(bytes, "bytes");
            if (Objects.isNull(contentType)) {
                contentType = DEFAULT_CONTENT_TYPE;
            }
        }

        /**
         * Creates a binary payload sent as {@code application/octet-stream}.
         *
         * @param bytes the binary bytes
         */
        public BinaryBody(final byte[] bytes) {
            this(bytes, null);
        }

        @Override
        public BodyKind kind() {
            return BodyKind.BINARY;
        }
    }

    /**
     * No request body.
     */
    record NoBody() implements Body {

        @Override
        public BodyKind kind() {
            return BodyKind.NONE;
        }
    }
}
