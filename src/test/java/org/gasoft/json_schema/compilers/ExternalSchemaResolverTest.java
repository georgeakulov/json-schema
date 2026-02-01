package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.SchemaBuilder;
import org.gasoft.json_schema.common.SchemaCompileException;
import org.gasoft.json_schema.IExternalResolutionResult;
import org.gasoft.json_schema.IExternalResolver;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ExternalSchemaResolverTest {

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
    void testResolveContentSuccess() {
        var prep = Prepared.of();
        SchemaBuilder.create()
                .setDraft202012DefaultDialect()
                .addMappingIdToSchema(prep.uri.toString(), prep.schema())
                .compile(prep.schema);
    }

    @Test
    void testBothNulls() {
        var prep = Prepared.of();
        var resolver = new CustomResolver();
        Assertions.assertThrows(SchemaCompileException.class, () ->
                SchemaBuilder.create()
                        .setDraft202012DefaultDialect()
                        .setSchemaResolver(resolver)
                        .compile(prep.schema)
        );
    }

    @Nested
    public class TestWithRemotes {

        TestServer server = new TestServer();
        String content;

        @BeforeEach
        void setUp() {
            server.upWithContent(1234, "/", () -> content);
        }

        @Test
        void externalResolveByUri() {
            var prep = Prepared.of();
            content = schema2;
            SchemaBuilder.create()
                    .setDraft202012DefaultDialect()
                    .addMappingIdToURI(prep.uri.toString(), URI.create("http://localhost:1234/"))
                    .compile(prep.schema);
        }

        @AfterEach
        void tearDown() {
            server.down();
        }
    }

    private static class CustomResolver implements IExternalResolver {

        private final Map<String, ResolutionResult> schemas = new HashMap<>();

        @Override
        public IExternalResolutionResult resolve(@NonNull String foundId, @NonNull ISchemaLocator schemaLocator) {
            return new IExternalResolutionResult() {
            };
        }
    }

    private record ResolutionResult(JsonNode schema, URI uri) implements IExternalResolutionResult {
        @Override
        public @Nullable JsonNode getSchema() {
            return schema;
        }

        @Override
        public @Nullable URI getAbsoluteUri() {
            return uri;
        }
    }

    private record Prepared(String schema, URI uri) {
        static Prepared of() {
            UUID uuid =  UUID.randomUUID();
            URI uri = URI.create("urn:uuid:" + uuid);
            String schema = MessageFormat.format(schema1, uri);
            return new Prepared(schema, uri);
        }
    }
}
