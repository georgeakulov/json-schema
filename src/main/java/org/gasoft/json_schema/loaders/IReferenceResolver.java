package org.gasoft.json_schema.loaders;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.jspecify.annotations.NonNull;

public interface IReferenceResolver {

    @NonNull IResolutionResult resolveDynamicRef(String refValue, @NonNull ISchemaLocator schemaLocator);
    @NonNull IResolutionResult resolveRecursiveRef(String refValue, @NonNull ISchemaLocator schemaLocator);

    /**
     * @param awaitedResource reference without fragment
     * @param schemaLocator   context sourceUri location
     * @return if null then will be resolved by defaults
     */
    @NonNull IResolutionResult resolveRef(@NonNull String awaitedResource, @NonNull ISchemaLocator schemaLocator);

    interface IResolutionResult {

        @NonNull ISchemaLocator getResolvedLocator();

        /**
         * @return the json node of schema or subschema
         */
        @NonNull JsonNode getSchema();

        /**
         * @return reference fragment as JsonPointer for this schema
         */
        @NonNull JsonPointer getReferencedPtr();
    }

}
