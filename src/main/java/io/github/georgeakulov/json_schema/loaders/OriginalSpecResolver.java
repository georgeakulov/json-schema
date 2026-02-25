package io.github.georgeakulov.json_schema.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgeakulov.json_schema.IExternalResolutionResult;
import io.github.georgeakulov.json_schema.IExternalResolver;
import io.github.georgeakulov.json_schema.common.JsonUtils;
import io.github.georgeakulov.json_schema.loaders.ExternalResolversHelper.ToUriAndSchemaResolver;
import io.github.georgeakulov.json_schema.results.IValidationResult.ISchemaLocator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OriginalSpecResolver implements IExternalResolver {

    private static Map<String, IExternalResolutionResult> preloaded;

    public OriginalSpecResolver() {
        preload();
    }

    public static void main(String [] args) {
        new OriginalSpecResolver();
    }

    @Override
    public @Nullable IExternalResolutionResult resolve(@NonNull String foundId, @NonNull ISchemaLocator schemaLocator) {
        return preloaded.get(foundId);
    }

    private static synchronized void preload() {
        if(preloaded == null) {
            try {

                List<JsonNode> jsons = new ArrayList<>();

                try(var is = new ZipInputStream(
                        Objects.requireNonNull(
                                ClassLoader.getSystemClassLoader().getResourceAsStream("dialects.zip")))
                ) {

                    ZipEntry entry;

                    while((entry = is.getNextEntry()) != null) {
                        if(!entry.isDirectory()) {
                            byte [] bytes = is.readAllBytes();
                            jsons.add(new ObjectMapper().readTree(bytes));
                        }
                        is.closeEntry();
                    }
                }

                preloaded = jsons.stream()
                        .map(json -> Tuples.of(extractId(json), json))
                        .collect(Collectors.toMap(
                                tuple -> tuple.getT1().toString(),
                                tuple -> new ToUriAndSchemaResolver(
                                        tuple.getT1(),
                                        tuple.getT2()
                                )
                        ));
            }
            catch(Exception io) {
                throw new RuntimeException("Can`t preload original specs", io);
            }
        }
    }

    private static URI extractId(JsonNode node) {
        return URI.create(node.path("$id").textValue());
    }

    private static JsonNode loadFile(Path path) {
        try(var is = new InflaterInputStream(Files.newInputStream(path))) {
            return JsonUtils.parse(is);
        }
        catch(IOException e) {
            throw new RuntimeException("Can`t lod file " + path, e);
        }
    }
}
