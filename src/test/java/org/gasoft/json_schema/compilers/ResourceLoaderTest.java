package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.SchemaBuilder;
import org.gasoft.json_schema.common.SchemaCompileException;
import org.gasoft.json_schema.loaders.IResourceLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.gasoft.json_schema.TestUtils.fromString;

public class ResourceLoaderTest {

    private static final String schema1 = """
                '{'
                    "$ref": "{0}"
                '}'
                """;
    private static final String schema2 = """
                {
                    "type": "integer"
                }
                """;

    @Test
    void testCustomSchemeLoader() {
        var prep = Prepared.of();
        CustomLoader loader = new CustomLoader();
        loader.content.put(prep.uri(), fromString(schema2));
        SchemaBuilder.create()
                .setDraft202012DefaultDialect()
                .addResourceLoader(loader)
                .compile(prep.schema());
    }

    @Test
    void testUnknownResourceLoader() {
        var prep = Prepared.of();
        Assertions.assertThrows(SchemaCompileException.class, () ->
                SchemaBuilder.create()
                        .setDraft202012DefaultDialect()
                        .compile(prep.schema())
        );
    }

    private static class CustomLoader implements IResourceLoader {

        private final Map<URI, JsonNode> content = new HashMap<>();

        @Override
        public Stream<String> getSupportedSchemes() {
            return Stream.of("urn");
        }

        @Override
        public JsonNode loadResource(URI byUri) {
            return content.get(byUri);
        }
    }

    private record Prepared(JsonNode schema, URI uri) {
        static Prepared of() {
            UUID uuid =  UUID.randomUUID();
            URI uri = URI.create("urn:uuid:" + uuid);
            String schema = MessageFormat.format(schema1, uri);
            return new Prepared(fromString(schema), uri);
        }
    }
}
