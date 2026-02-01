package org.gasoft.json_schema.loaders;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.gasoft.json_schema.compilers.ICompiler;
import org.gasoft.json_schema.dialects.Dialect;
import org.gasoft.json_schema.loaders.SchemaInfo.SubSchemaInfo;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.*;

import static org.gasoft.json_schema.common.SchemaCompileException.checkIt;
import static org.gasoft.json_schema.common.SchemaCompileException.checkNonNull;

public class SchemaPreprocessor {

    public SchemaProcessingResult onSchemaLoaded(Dialect dialect, JsonNode schema, @Nullable URI parentId) {

        SchemaProcessingResult schemaProcessingResult = new SchemaProcessingResult(dialect, schema);
        var mediator = new PreprocessorMediator(
                schema, schemaProcessingResult, parentId, schemaProcessingResult.rootSubSchema
        );
        mediator.process(JsonPointer.empty());
        return schemaProcessingResult;
    }

    public URI resolveId(@NonNull String idValue, @Nullable URI parentId) {
        // todo Resolve relative ids with authoring part
        // optional/refOfUnknownKeyword.json - "reference of an arbitrary keyword of a sub-schema with encoded ref"
        URI idUri = URI.create(idValue);
        checkIt(idUri.getFragment() == null || idUri.getFragment().isEmpty(), "The id {0} can`t contains fragment", idUri);
        if(idUri.isAbsolute()) {
            return idUri;
        }

        if(parentId != null) {
            return parentId.resolve(idUri);
        }

        return idUri;
    }

    private Optional<String> optId(JsonNode node) {
        return Optional.of(node.path("$id"))
                .filter(JsonNode::isTextual)
                .map(JsonNode::textValue);
    }

    public class SchemaProcessingResult {

        private final Dialect dialect;
        private SubSchemaInfo rootSubSchema;
        private final Map<URI, SubSchemaInfo> subschemaInfos = new HashMap<>();

        public SchemaProcessingResult(Dialect dialect, JsonNode schema) {
            this.dialect = dialect;
            this.rootSubSchema = SubSchemaInfo.emptyAnchors(UUID.randomUUID(), null, JsonPointer.empty(), schema);
        }

        private CreateSubSchemaResult onNewId(String idValue, JsonPointer pointer, URI parentId, JsonNode atObject, @Nullable SubSchemaInfo parentSubSchema) {
            URI id = resolveId(idValue, parentId);

            if(pointer.equals(JsonPointer.empty())) {
                rootSubSchema = new SubSchemaInfo(rootSubSchema.uuid(), id, JsonPointer.empty(), rootSubSchema.schema(),
                        rootSubSchema.anchors(), rootSubSchema.dynamicAnchors(), new HashMap<>());
                return new CreateSubSchemaResult(id, rootSubSchema);
            }
            else {
                var subschema = SubSchemaInfo.emptyAnchors(UUID.randomUUID(), id, pointer, atObject);
                subschemaInfos.put(id, subschema);
                Objects.requireNonNull(parentSubSchema, "The parent subschema is null");
                var relative = childToRelative(parentSubSchema.absolutePointer(), pointer);
                parentSubSchema.subschemas().put(relative.toString(), subschema);
                return new CreateSubSchemaResult(id, subschema);
            }
        }

        private void onAnchor(JsonNode node, JsonPointer pointer, URI parentId) {

            checkIt(node.isTextual(), "The $anchor must be an string. Actual: {0}", node);

            SubSchemaInfo subSchema = getSubSchema(parentId, pointer);
            JsonPointer relative = childToRelative(subSchema.absolutePointer(), pointer);
            checkIt(subSchema.anchors().put(node.textValue(), relative) == null, "The anchor {0} already exists", node);
        }

        private SubSchemaInfo getSubSchema(URI parentId, JsonPointer pointer) {
            SubSchemaInfo subSchema;
            if(Objects.equals(rootSubSchema.id(), parentId)) {
                subSchema = rootSubSchema;
            }
            else {
                subSchema = subschemaInfos.get(parentId);
            }
            return checkNonNull(subSchema, "Can`t resolve subschema by ptr: {0} and id: {1}", pointer, parentId);
        }

        private void onNewDynamicAnchor(JsonNode node, JsonPointer pointer, URI parentId) {

            checkIt(node.isTextual(), "The $dynamicAnchor keyword value must be an string. Actual: {0}", node);
            var subSchema = getSubSchema(parentId, pointer);
            JsonPointer childToRelative = childToRelative(subSchema.absolutePointer(), pointer);
            checkIt(subSchema.dynamicAnchors().put(node.textValue(), childToRelative) == null, "The $dynaminAnchor {0} already exists", node);
        }

        private void onRecursiveAnchor(JsonNode node, JsonPointer pointer, URI parentId) {
            checkIt(node.isBoolean(), "The $recursiveAnchor must be boolean. Actual: {0}", node);
            var subSchema = getSubSchema(parentId, pointer);
            subSchema.markRecursiveAnchor(node.asBoolean());
        }

        public SubSchemaInfo getRootSubSchema() {
            return rootSubSchema;
        }

        public Map<URI, SubSchemaInfo> getSubSchemas() {
            return subschemaInfos;
        }
    }

    private record CreateSubSchemaResult(URI id, SubSchemaInfo parentSubSchema){}

    private class PreprocessorMediator implements ICompiler.IPreprocessorMediator {

        final JsonNode schema;
        final SchemaProcessingResult schemaProcessingResult;
        URI parentId;
        SubSchemaInfo parentSubSchema;

        private PreprocessorMediator(JsonNode schema, SchemaProcessingResult schemaProcessingResult, URI parentId, SubSchemaInfo parent) {
            this.schema = schema;
            this.schemaProcessingResult = schemaProcessingResult;
            this.parentId = parentId;
            this.parentSubSchema = parent;
        }

        @Override
        public void process(JsonPointer pointer) {
            PreprocessorMediator mediator = new PreprocessorMediator(schema, schemaProcessingResult, parentId, parentSubSchema);
            mediator.processImpl(pointer);
        }

        public void processImpl(JsonPointer pointer) {
            JsonNode node = schema.at(pointer);
            if(!node.isObject()) {
                return;
            }
            ObjectNode obj = (ObjectNode) node;

            optId(obj)
                    .ifPresent(
                            idValue -> {
                                var idResult = schemaProcessingResult.onNewId(idValue, pointer, parentId, node, parentSubSchema);
                                parentId = idResult.id();
                                parentSubSchema = idResult.parentSubSchema();
                                obj.set("$id", TextNode.valueOf(parentId.toString()));
                            }
                    );

            if(schemaProcessingResult.dialect.optCompiler("$anchor") != null) {
                var anchor = node.path("$anchor");
                if(!anchor.isMissingNode()) {
                    schemaProcessingResult.onAnchor(anchor, pointer, parentId);
                }
            }

            if(schemaProcessingResult.dialect.optCompiler("$dynamicAnchor") != null) {
                var dynamicAnchor = node.path("$dynamicAnchor");
                if(!dynamicAnchor.isMissingNode()) {
                    schemaProcessingResult.onNewDynamicAnchor(dynamicAnchor, pointer, parentId);
                }
            }

            if(schemaProcessingResult.dialect.optCompiler("$recursiveAnchor") != null) {
                var dynamicAnchor = node.path("$recursiveAnchor");
                if(!dynamicAnchor.isMissingNode()) {
                    schemaProcessingResult.onRecursiveAnchor(dynamicAnchor, pointer, parentId);
                }
            }

            node.propertyStream()
                    .forEach(entry -> {
                        ICompiler compiler = schemaProcessingResult.dialect.optCompiler(entry.getKey());
                        if(compiler != null) {
                            compiler.preprocess(this, entry.getKey(), entry.getValue(), pointer.appendProperty(entry.getKey()));
                        }
                        else {
                            System.out.println("No compiler for key: " + entry.getKey());
                        }
                    });
        }
    }

    private static JsonPointer childToRelative(JsonPointer parent, JsonPointer child) {
        String parentStr = parent.toString();
        String childStr = child.toString();

        checkIt(childStr.startsWith(parentStr), "The parent pointer {0} not a part of child pointer {1}", parent, child);

        String childStrTrimmed = childStr.substring(parentStr.length());
        if(childStrTrimmed.isBlank()) {
            return JsonPointer.empty();
        }
        return JsonPointer.compile(childStrTrimmed);
    }

}
