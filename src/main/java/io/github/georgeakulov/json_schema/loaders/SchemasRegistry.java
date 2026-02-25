package io.github.georgeakulov.json_schema.loaders;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.georgeakulov.json_schema.compilers.CompileConfig;
import io.github.georgeakulov.json_schema.common.LocatedSchemaCompileException;
import io.github.georgeakulov.json_schema.common.SchemaCompileException;
import io.github.georgeakulov.json_schema.dialects.Dialect;
import io.github.georgeakulov.json_schema.dialects.DialectResolver;
import io.github.georgeakulov.json_schema.loaders.SchemaInfo.SubSchemaInfo;
import io.github.georgeakulov.json_schema.loaders.SchemaPreprocessor.SchemaProcessingResult;
import io.github.georgeakulov.json_schema.results.IValidationResult.ISchemaLocator;
import io.github.georgeakulov.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.georgeakulov.json_schema.common.LocatedSchemaCompileException.checkIt;
import static io.github.georgeakulov.json_schema.common.LocatedSchemaCompileException.create;
import static io.github.georgeakulov.json_schema.common.SchemaCompileException.checkNonNull;

public class SchemasRegistry implements IReferenceResolver {

    private final BaseResourceLoader resourceLoaders = new BaseResourceLoader();
    private final CompileConfig compileConfig;
    private final SchemaPreprocessor schemaPreprocessor = new SchemaPreprocessor();
    private final DialectResolver dialectResolver;

    /**
     * Id of schemes and subschemes
     */
    private final Map<URI, UUID> idToHolders = new HashMap<>();
    private final Map<URI, Set<UUID>> originToContent = new HashMap<>();
    private final Map<UUID, SchemaInfo> content = new HashMap<>();

    public SchemasRegistry(DialectResolver dialectResolver, CompileConfig compileConfig) {
        this.compileConfig = compileConfig;
        this.dialectResolver = dialectResolver;
        resourceLoaders.setUseEmbedded(compileConfig.isAllowEmbedResourceLoaders());
        compileConfig.getResourceLoaders().reversed().forEach(resourceLoaders::addLoader);
    }

    public ISchemaLocator registerInitialSchema(JsonNode node, @Nullable URI defaultDialectUri) {

        SchemaInfo info = registerSchema(node, null, null, dialectResolver.optDefaultDialect(defaultDialectUri));
        return ValidationResultFactory.createSchemaLocator(info.getUuid(), info.getOrigin(), info.getId(), JsonPointer.empty());
    }

    private JsonNode tryResolveExternalSchema(URI id, ISchemaLocator schemaLocator) {
        if(this.compileConfig.getExternalSchemaResolver() != null) {
            var externalResult = this.compileConfig.getExternalSchemaResolver().resolve(id.toString(), schemaLocator);
            if (externalResult != null) {
                // Какой то результат есть
                if(externalResult.getSchema() != null) {
                    return externalResult.getSchema();
                }

                var uri = externalResult.getAbsoluteUri();
                if(uri != null && !uri.equals(id)) {
                    return tryResolveExternalSchema(uri, schemaLocator);
                }
            }
        }

        return this.resourceLoaders.loadResource(id);
    }

    /**
     * @return loaded schema id if found
     */
    @NonNull
    private SchemaInfo registerSchema(JsonNode node, @Nullable URI byUri, ISchemaLocator parentLocator, @Nullable Dialect defaultDialect) {

        Dialect dialect = dialectResolver.resolveDialect(node, uri -> tryResolveExternalSchema(uri, parentLocator));
        if(dialect == null) {
            if(defaultDialect == null) {
                throw SchemaCompileException.create("Can`t resolve dialect. You can setup default dialect");
            }
            dialect = defaultDialect;
        }

        SchemaProcessingResult result;
        try {
            result = schemaPreprocessor.onSchemaLoaded(
                    dialect,
                    node,
                    parentLocator == null ? null : parentLocator.getId()
            );
        }
        catch(Exception e) {
            throw create(parentLocator, e, "Error on schema preprocessing");
        }

        if(byUri != null && !byUri.isAbsolute()) {
            byUri = null;
        }

        SchemaInfo root = registerSubSchema(dialect, byUri, result.getRootSubSchema());
        for (SubSchemaInfo value : result.getSubSchemas().values()) {
            registerSubSchema(dialect, byUri, value);
        }

        return root;
    }

    private SchemaInfo registerSubSchema(Dialect dialect, URI origin, SubSchemaInfo subSchemaInfo) {
        SchemaInfo info = new SchemaInfo(
                dialect,
                subSchemaInfo.uuid(),
                subSchemaInfo.id(),
                origin,
                subSchemaInfo.absolutePointer(),
                subSchemaInfo.schema(),
                subSchemaInfo.anchors(),
                subSchemaInfo.dynamicAnchors(),
                subSchemaInfo.subschemas().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                ss -> ss.getValue().uuid()
                        )),
                subSchemaInfo.isRecursiveAnchor()
        );

        content.put(info.getUuid(), info);
        if(origin != null) {
            originToContent.computeIfAbsent(origin, ignore -> new HashSet<>())
                    .add(info.getUuid());
        }
        if(info.getId() != null) {
            idToHolders.put(info.getId(), info.getUuid());
        }
        return info;
    }

    @NonNull
    public SchemaInfo getSchema(UUID internalId) {
        return checkNonNull(content.get(internalId), "Can`t find schema with internal id %s", internalId);
    }

    /**
     * Used by IdCompiler to change context in subschema evaluation
     */
    public ISchemaLocator resolveExistingId(String idValue, ISchemaLocator parentLocator) {

        URI id = schemaPreprocessor.resolveId(idValue, parentLocator.getId());
        SchemaInfo info = content.get(idToHolders.get(id));
        return ValidationResultFactory.createSubSchemaLocator(info.getUuid(), info.getOrigin(), info.getId(), JsonPointer.empty(), parentLocator);
    }

    public @NonNull IResolutionResult resolveRecursiveRef(String refValue, @NonNull ISchemaLocator locator) {
        var resolution = new RefResolutionResult(refValue);
        checkIt(
                !resolution.hasFragment() && !resolution.hasPath(),
                locator,
                "The $recursiveRef can contains only '#' value. Actual: {0}", refValue
        );
        SchemaInfo info = getSchema(locator.getSchemaUUID());
        if(info.hasRecursiveAnchor()) {
            var locatorIt = locator.getParent();
            while(locatorIt != null) {
                var schema = content.get(locatorIt.getSchemaUUID());
                var resolved = schema.hasRecursiveAnchor();
                if(resolved) {
                    info = schema;
                }
                locatorIt = locatorIt.getParent();
            }
        }

        return new ReferenceResolutionResult(
                ValidationResultFactory.createSubSchemaLocator(info.getUuid(), info.getOrigin(), info.getId(), JsonPointer.empty(), locator),
                info.getContent(),
                JsonPointer.empty()
        );
    }

    @Override
    public @NonNull IResolutionResult resolveDynamicRef(String refValue, @NonNull ISchemaLocator schemaLocator) {

        var resolution = new RefResolutionResult(refValue);
        SchemaInfo info = tryResolveSchema(resolution, schemaLocator);
        if(info == null) {
            throw new IllegalStateException("Can`t resolve schema for $dynamicRef: " + refValue + " at " + schemaLocator);
        }

        IResolutionResult result = tryResolveDynamicRef(resolution, info, schemaLocator);
        // If in current subscheme exists dynamicAnchor then resolve oldest (nearest to top stack)
        // If dynamic anchor not exists then resolve as simple reference
        if(result != null) {
            var locator = schemaLocator.getParent();
            while(locator != null) {
                var resolved = tryResolveDynamicRef(resolution, null, locator);
                if(resolved != null) {
                    result = resolved;
                }
                locator = locator.getParent();
            }
        }
        else {
            result = tryResolveRef(resolution, schemaLocator);
        }
        if(result == null) {
            throw new IllegalStateException("Can`t resolve $dynamicRef: " + refValue + " in context " + schemaLocator);
        }
        return result;
    }

    private IResolutionResult tryResolveDynamicRef(RefResolutionResult refResolutionResult, @Nullable SchemaInfo info, ISchemaLocator locator) {
        info = info == null ? content.get(locator.getSchemaUUID()) : info;
        JsonPointer pointer = info.optDynamicAnchor(refResolutionResult.getFragment());
        if(pointer != null) {
            return new ReferenceResolutionResult(
                    ValidationResultFactory.createSubSchemaLocator(info.getUuid(), info.getOrigin(), info.getId(), pointer, locator),
                    info.getContent(),
                    pointer
            );
        }
        return null;
    }

    @Override
    public @NonNull IResolutionResult resolveRef(@NonNull String refValue, @NonNull ISchemaLocator schemaLocator) {

        var resolution = new RefResolutionResult(refValue);
        IResolutionResult result = tryResolveRef(resolution, schemaLocator);
        if(result == null) {
            throw new IllegalStateException("Can't resolve ref:" + refValue + " at location: " + schemaLocator);
        }
        return result;
    }

    private @Nullable SchemaInfo tryResolveSchema(RefResolutionResult resolution, ISchemaLocator schemaLocator) {
        if(resolution.hasPath()) {
            return resolvePath(resolution.getPath(), schemaLocator);
        }
        else {
            var schemaInfo = content.get(schemaLocator.getSchemaUUID());
            if(resolution.hasFragment()) {
                return findNearestSchema(schemaInfo, resolution, schemaLocator);
            }
            return content.get(schemaLocator.getSchemaUUID());
        }
    }

    /*
    When accessing via the $ref link, in the nested $defs, each of which has its own $id, the current $id should be resolved.
     */
    private SchemaInfo findNearestSchema(SchemaInfo schemaInfo, RefResolutionResult resolution, ISchemaLocator schemaLocator) {
        UUID uuid = schemaInfo.findNearestSubschema(resolution.getFragment());
        if(uuid != null) {
            // Resolve subschema
            SchemaInfo schema = content.get(uuid);
            String pointer = schema.getPointerOfRoot().toString();
            String modifiedFragment = resolution.getFragment().substring(pointer.length());
            resolution.modifyFragment(modifiedFragment);
            return schema;
        }
        return schemaInfo;
    }

    private @Nullable IResolutionResult tryResolveRef(RefResolutionResult resolution, ISchemaLocator schemaLocator) {
        SchemaInfo info = tryResolveSchema(resolution, schemaLocator);

        if(info == null) {
            return null;
        }

        JsonPointer pointer;
        if(resolution.hasFragment()) {
            pointer = resolveFragment(resolution.getFragment(), info);
            // may be subschema. see optional/dynamicRef.json
            if(pointer != null) {
                var uuid = info.resolveSubSchema(pointer);
                if(uuid != null) {
                    info = content.get(uuid);
                    pointer = JsonPointer.empty();
                }
            }
        }
        else {
            pointer = JsonPointer.empty();
        }

        if(pointer == null) {
            return null;
        }

        return new ReferenceResolutionResult(
                ValidationResultFactory.createSubSchemaLocator(info.getUuid(), info.getOrigin(), info.getId(), pointer, schemaLocator),
                info.getContent(),
                pointer
        );
    }

    public Dialect getDialect(ISchemaLocator locator) {
        return getSchema(locator.getSchemaUUID()).getDialect();
    }

    private SchemaInfo resolvePath(String uri, ISchemaLocator schemaLocator) throws LocatedSchemaCompileException {

        URI resolved = null;
        if(compileConfig.getExternalSchemaResolver() != null) {
            var externalResult = compileConfig.getExternalSchemaResolver().resolve(uri, schemaLocator);
            if (externalResult != null) {
                // Какой то результат есть
                if(externalResult.getSchema() != null) {
                    return registerSchema(externalResult.getSchema(), externalResult.getAbsoluteUri(), schemaLocator, getDialect(schemaLocator));
                }

                resolved = externalResult.getAbsoluteUri();
                if(resolved != null) {
                    if(!resolved.isAbsolute()) {
                        throw SchemaCompileException.create("The external resolver return non absolute URI {0}. Source: {1}, context URL: {2} result",
                                        resolved, uri, schemaLocator.getOriginUri());
                    }
                }
            }
        }

        // Check external resolution result
        if(resolved == null) {
            // nonnull or exception
            resolved = applyDefaultResolution(uri, schemaLocator);
        }

        // Can`t resolve URI
        if(resolved == null) {
            return null;
        }

        // May be the resolved uri is $id
        UUID pointer = idToHolders.get(resolved);
        if(pointer != null) {
            return content.get(pointer);
        }

        // May be the resolved uri was loaded early
        var pointers = originToContent.get(resolved);
        if(pointers != null) {
            return pointers.stream()
                    .map(content::get)
                    .filter(schemaInfo -> schemaInfo.getPointerOfRoot().equals(JsonPointer.empty()))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("Can`t find root schema"));
        }

        // This is absolute file load them
        JsonNode schema;
        try {
            schema = tryResolveExternalSchema(resolved, schemaLocator);

        }
        catch(Throwable e) {
            throw create(schemaLocator, e, "Error on loading resource {0} by locator {1}", resolved, schemaLocator);
        }

        if(schema == null) {
            throw create(schemaLocator, "Unable to load resource {0}", resolved);
        }

        return registerSchema(schema, resolved, schemaLocator, getDialect(schemaLocator));
    }

    private @Nullable URI applyDefaultResolution(String uriStr, ISchemaLocator schemaLocator) {

        URI uri = URI.create(uriStr);

        if(!uri.isAbsolute()) {

            // This is relative file. Try resolve it
            URI resolved = this.resolveRelativeResource(uri, schemaLocator);

            if(resolved == null) {
                // Can`t resolve
                return null;
            }

            if(!resolved.isAbsolute()) {
                throw new IllegalStateException(String.format("Unable to resolve resource of %s with context %s to absolute URI", uri, schemaLocator));
            }
            return resolved;
        }
        return uri;
    }

    private @Nullable URI resolveRelativeResource(URI resource, ISchemaLocator schemaLocator) {

        if(schemaLocator.getId() != null) {
            return schemaLocator.getId().resolve(resource);
        }

        if(schemaLocator.getOriginUri() == null) {
            return null;
        }
        if(!schemaLocator.getOriginUri().isAbsolute()) {
            return null;
        }
        return schemaLocator.getOriginUri().resolve(resource);
    }

    private @Nullable JsonPointer resolveFragment(String fragmentValue, SchemaInfo schemaInfo) {
        String decoded = URLDecoder.decode(fragmentValue, StandardCharsets.UTF_8);
        try {
            return JsonPointer.compile(decoded);
        }
        catch(IllegalArgumentException ise) {
            // ignore may be anchor
        }

        JsonPointer pointer =  schemaInfo.optAnchor(decoded);
        if(pointer != null) {
            return pointer;
        }

        return schemaInfo.optDynamicAnchor(decoded);
    }

    public record ReferenceResolutionResult(ISchemaLocator locator, JsonNode schema, JsonPointer path) implements IResolutionResult{

        @Override
        public @NonNull JsonNode getSchema() {
            return schema;
        }

        @Override
        public @NonNull JsonPointer getReferencedPtr() {
            return path;
        }

        @Override
        public @NonNull ISchemaLocator getResolvedLocator() {
            return locator;
        }
    }

    private static class RefResolutionResult {

        private final String[] parts;
        private RefResolutionResult(String refValue) {
            parts = refValue.split("#");
        }

        private boolean hasPath() {
            return parts.length > 0 && !parts[0].isBlank();
        }

        private boolean hasFragment() {
            return parts.length > 1 && !parts[1].isBlank();
        }

        private String getPath() {
            return parts[0];
        }

        private String getFragment() {
            return parts[1];
        }

        private void modifyFragment(String modified) {
            if(hasFragment()) {
                this.parts[1] = modified;
            }
        }
    }
}
