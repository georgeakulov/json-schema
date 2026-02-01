package org.gasoft.json_schema.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.IExternalResolutionResult;
import org.gasoft.json_schema.IExternalResolver;
import org.gasoft.json_schema.common.JsonUtils;
import org.gasoft.json_schema.results.IValidationResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.*;

public class ExternalResolversHelper implements IExternalResolver {

    private final Map<String, IExternalResolutionResult> externalResolver = new HashMap<>();
    private final List<IExternalResolver> additionalResolutions = new ArrayList<>();

    @Override
    public @Nullable IExternalResolutionResult resolve(@NonNull String foundId, IValidationResult.@NonNull ISchemaLocator schemaLocator) {
        IExternalResolutionResult result = externalResolver.get(foundId);
        if(result == null) {
            for (IExternalResolver additionalResolution : additionalResolutions) {
                IExternalResolutionResult addResult = additionalResolution.resolve(foundId, schemaLocator);
                if(addResult != null) {
                    return addResult;
                }
            }
        }
        return result;
    }

    public void mapIdToSchema(String id, JsonNode schema) {
        externalResolver.put(Objects.requireNonNull(id), new ToSchemaResolver(Objects.requireNonNull(schema)));
    }

    public void mapIdToSchema(String id, String schemaStr) {
        Objects.requireNonNull(schemaStr);
        mapIdToSchema(id, JsonUtils.parse(schemaStr));
    }

    public void mapIdToUri(String id, URI mappedUri) {
        externalResolver.put(
                Objects.requireNonNull(id),
                new ToUriResolver(checkURI(mappedUri))
        );
    }

    public void mapIdToUriAndSchema(String id, URI uri, JsonNode schema) {
        externalResolver.put(
                Objects.requireNonNull(id),
                new ToUriAndSchemaResolver(
                        checkURI(uri),
                        Objects.requireNonNull(schema)
                )
        );
    }

    public void mapIdToUriAndSchema(String id, URI uri, String schema) {
        Objects.requireNonNull(schema);
        mapIdToUriAndSchema(id, uri, JsonUtils.parse(schema));
    }

    public void addResolver(IExternalResolver resolver) {
        this.additionalResolutions.add(Objects.requireNonNull(resolver));
    }

    private URI checkURI(URI uri) {
        Objects.requireNonNull(uri);
        if(!uri.isAbsolute()) {
            throw new IllegalArgumentException("The URI must be an absolute. Actual: " + uri);
        }
        return uri;
    }

    private record ToSchemaResolver(JsonNode schema) implements IExternalResolutionResult {
        @Override
        public @Nullable JsonNode getSchema() {
            return schema;
        }
    }

    private record ToUriResolver(URI uri) implements IExternalResolutionResult {
        @Override
        public @Nullable URI getAbsoluteUri() {
            return uri;
        }
    }

    private record ToUriAndSchemaResolver(URI uri, JsonNode schema) implements IExternalResolutionResult {
        @Override
        public @Nullable JsonNode getSchema() {
            return schema;
        }

        @Override
        public @Nullable URI getAbsoluteUri() {
            return uri;
        }
    }
}
