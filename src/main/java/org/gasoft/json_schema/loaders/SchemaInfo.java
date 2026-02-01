package org.gasoft.json_schema.loaders;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Dialect;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SchemaInfo {

    private final Dialect dialect;
    private final UUID uuid;
    @Nullable private final URI id;
    @Nullable private final URI origin;
    @NonNull private final JsonPointer pointerOfRoot;
    @NonNull private final JsonNode content;

    private final Map<String, JsonPointer> anchors;
    private final boolean recursiveAnchor;
    private final Map<String, JsonPointer> dynamicAnchors;
    private final Map<String, UUID> subschemas;

    SchemaInfo(Dialect dialect,
               UUID uuid,
               @Nullable URI id,
               @Nullable URI origin,
               @NonNull JsonPointer pointerOfRoot,
               @NonNull JsonNode content,
               Map<String, JsonPointer> anchors,
               Map<String, JsonPointer> dynamicAnchors,
               Map<String, UUID> subschemas,
               boolean recursiveAnchor) {
        this.uuid = uuid;
        this.id = id;
        this.origin = origin;
        this.pointerOfRoot = pointerOfRoot;
        this.content = content;
        this.anchors = anchors;
        this.dynamicAnchors = dynamicAnchors;
        this.dialect = dialect;
        this.subschemas = subschemas;
        this.recursiveAnchor = recursiveAnchor;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public UUID getUuid() {
        return uuid;
    }

    public @NonNull JsonPointer getPointerOfRoot() {
        return pointerOfRoot;
    }

    public JsonPointer optAnchor(String anchorValue) {
        return anchors.get(anchorValue);
    }

    public JsonPointer optDynamicAnchor(String anchorValue) {
        return dynamicAnchors.get(anchorValue);
    }

    public JsonNode getContent() {
        return content;
    }

    public @Nullable URI getId(){
        return id;
    }

    public @Nullable URI getOrigin() {
        return origin;
    }

    public @Nullable UUID resolveSubSchema(JsonPointer pointer) {
        return subschemas.get(pointer.toString());
    }

    public boolean hasRecursiveAnchor() {
        return this.recursiveAnchor;
    }

    public static final class SubSchemaInfo {
        private final UUID uuid;
        private final URI id;
        private final JsonPointer absolutePointer;
        private final JsonNode schema;
        private final Map<String, JsonPointer> anchors;
        private final Map<String, JsonPointer> dynamicAnchors;
        private final Map<String, SubSchemaInfo> subschemas;
        private boolean recursiveAnchor;

        public SubSchemaInfo(UUID uuid, URI id, JsonPointer absolutePointer, JsonNode schema, Map<String, JsonPointer> anchors,
                             Map<String, JsonPointer> dynamicAnchors, Map<String, SubSchemaInfo> subschemas) {
            this.uuid = uuid;
            this.id = id;
            this.absolutePointer = absolutePointer;
            this.schema = schema;
            this.anchors = anchors;
            this.dynamicAnchors = dynamicAnchors;
            this.subschemas = subschemas;
        }

        public static SubSchemaInfo emptyAnchors(UUID uuid, URI id, JsonPointer absolutePointer, JsonNode schema) {
                return new SubSchemaInfo(uuid, id, absolutePointer, schema, new HashMap<>(), new HashMap<>(), new HashMap<>());
            }

        public void markRecursiveAnchor(boolean isRecursive) {
            recursiveAnchor = isRecursive;
        }

        public boolean isRecursiveAnchor() {
            return recursiveAnchor;
        }

        public UUID uuid() {
            return uuid;
        }

        public URI id() {
            return id;
        }

        public JsonPointer absolutePointer() {
            return absolutePointer;
        }

        public JsonNode schema() {
            return schema;
        }

        public Map<String, JsonPointer> anchors() {
            return anchors;
        }

        public Map<String, JsonPointer> dynamicAnchors() {
            return dynamicAnchors;
        }

        public Map<String, SubSchemaInfo> subschemas() {
            return subschemas;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (SubSchemaInfo) obj;
            return Objects.equals(this.uuid, that.uuid) &&
                    Objects.equals(this.id, that.id) &&
                    Objects.equals(this.absolutePointer, that.absolutePointer) &&
                    Objects.equals(this.schema, that.schema) &&
                    Objects.equals(this.anchors, that.anchors) &&
                    Objects.equals(this.dynamicAnchors, that.dynamicAnchors) &&
                    Objects.equals(this.subschemas, that.subschemas);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, id, absolutePointer, schema, anchors, dynamicAnchors, subschemas);
        }

        @Override
        public String toString() {
            return "SubSchemaInfo[" +
                    "uuid=" + uuid + ", " +
                    "id=" + id + ", " +
                    "absolutePointer=" + absolutePointer + ", " +
                    "schema=" + schema + ", " +
                    "anchors=" + anchors + ", " +
                    "dynamicAnchors=" + dynamicAnchors + ", " +
                    "subschemas=" + subschemas + ']';
        }

        }
}
