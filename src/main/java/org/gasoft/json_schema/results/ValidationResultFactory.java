package org.gasoft.json_schema.results;

import com.fasterxml.jackson.core.JsonPointer;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.IValidationResult.IValidationAnnotation;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.IValidationResult.IValidationResultContainer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValidationResultFactory {
    
    public static ISchemaLocator createSchemaLocator(UUID uuid, @Nullable URI originUri, @Nullable URI id, JsonPointer ref) {
        return new SchemaLocator(uuid, originUri, id, ref, null);
    }

    public static ISchemaLocator createSubSchemaLocator(UUID uuid, @Nullable URI originUri, @Nullable URI id, JsonPointer ref, ISchemaLocator parent) {
        return new SchemaLocator(uuid, originUri, id, ref, parent);
    }

    public static IValidationId createId(ISchemaLocator schemaLocator, JsonPointer instancePtr) {
        return new ValidationId(createSchemaLocator(schemaLocator.getSchemaUUID(), null, null, schemaLocator.getSchemaRef()), instancePtr);
    }

    public static IValidationResult createOk(ISchemaLocator schemaLocator, JsonPointer instancePtr) {
        return createOk(createId(schemaLocator, instancePtr));
    }

    public static IValidationResult createOk(IValidationId id) {
        return new ValidationOk(id);
    }

    public static ValidationAnnotation createAnnotation(IValidationId id) {
        return new ValidationAnnotation(id);
    }

    public static ValidationAnnotation createAnnotation(ISchemaLocator schemaLocator, JsonPointer instancePtr) {
        return createAnnotation(createId(schemaLocator, instancePtr));
    }

    public static ValidationResultContainer createContainer(ISchemaLocator schemaLocator, JsonPointer instancePtr) {
        return createContainer(createId(schemaLocator, instancePtr));
    }

    public static ValidationResultContainer createContainer(IValidationId id) {
        return new ValidationResultContainer(id, true);
    }

    public static class ValidationResultContainer implements IValidationResultContainer {

        private final IValidationId id;
        private final List<IValidationResult> nested = new CopyOnWriteArrayList<>();
        private boolean valid;

        private ValidationResultContainer(IValidationId id, boolean valid) {
            this.id = id;
            this.valid = valid;
        }

        @Override
        public IValidationResult.IValidationId getId() {
            return id;
        }

        @Override
        public Stream<IValidationResult> getNestedResults() {
            return nested.stream();
        }

        public ValidationResultContainer append(IValidationResult result) {
            nested.add(result);
            valid = valid && result.isOk();
            return this;
        }

        public ValidationResultContainer appendAll(Iterable<? extends IValidationResult> results) {
            results.forEach(this::append);
            return this;
        }

        @Override
        public boolean isOk() {
            return valid;
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(valid ? "OK" : "ERR");
            sb.append("id=").append(id);
            sb.append(", nested=").append(getNestedResults().map(String::valueOf).collect(Collectors.joining("], [", "[", "]")));
            sb.append('}');
            return sb.toString();
        }
    }

    public record ValidationAnnotation(IValidationId id) implements IValidationAnnotation {

        @Override
        public IValidationId getId() {
            return id;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ANT{");
            sb.append("id=").append(id);
            sb.append('}');
            return sb.toString();
        }
    }

    public record ValidationOk(IValidationId id) implements IValidationResult {
        @Override
        public Type getType() {
            return Type.OK;
        }

        @Override
        public IValidationId getId() {
            return id;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("OK{");
            sb.append("id=").append(id);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean isOk() {
            return true;
        }
    }

    public record ValidationId(ISchemaLocator schemaLocator, JsonPointer instancePtr) implements IValidationId {

        @Override
        public ISchemaLocator getSchemaLocator() {
            return schemaLocator;
        }

        @Override
        public JsonPointer getInstanceRef() {
            return instancePtr;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("id{");
            sb.append("loc=").append(schemaLocator);
            sb.append(", inst=").append(instancePtr);
            sb.append('}');
            return sb.toString();
        }
    }

    public record SchemaLocator(@NonNull UUID uuid, @Nullable URI originUri, @Nullable URI id, JsonPointer schemaRef,
                                ISchemaLocator parent) implements ISchemaLocator {

        @Override
        public @NonNull UUID getSchemaUUID() {
            return uuid;
        }

        @Override
        public @Nullable URI getId() {
            return id;
        }

        @Override
        public @Nullable URI getOriginUri() {
            return originUri;
        }

        @Override
        public JsonPointer getSchemaRef() {
            return schemaRef;
        }

        @Override
        public @Nullable ISchemaLocator getParent() {
            return parent;
        }

        private void toStringMain(StringBuilder builder, ISchemaLocator schemaLocator, int level) {
            if(level > 0) {
                builder.append(System.lineSeparator()).append("\t".repeat(level)).append("-- ");
            }
            builder.append("id=").append(schemaLocator.getId())
                    .append(", org=").append(schemaLocator.getOriginUri())
                    .append(", ptr=").append(schemaLocator.getSchemaRef());
        }

        @Override
        public String toString() {
            StringBuilder bld = new StringBuilder();
            int level = 0;
            ISchemaLocator loc = this;
            do {
                toStringMain(bld, loc, level++);
                loc = loc.getParent();
            }while(loc != null);
            return bld.toString();
        }

        @Override
        public ISchemaLocator appendProperty(String property) {
            return new SchemaLocator(uuid, originUri, id, schemaRef.appendProperty(property), parent);
        }

        @Override
        public ISchemaLocator appendIndex(int idx) {
            return new SchemaLocator(uuid, originUri, id, schemaRef.appendIndex(idx), parent);
        }
    }

    public static String hierarchyFormat(IValidationResult result) {
        try(var bos = new ByteArrayOutputStream(); var stream = new PrintStream(new BufferedOutputStream(bos), false, StandardCharsets.UTF_8)) {
            hierarchyFormat(stream, 0, result);
            stream.flush();
            return bos.toString(StandardCharsets.UTF_8);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void hierarchyFormat(PrintStream builder, int level, IValidationResult result) {
        builder.print("\t".repeat(level));
        switch (result.getType()) {
            case ANNOTATION, ERROR, OK -> builder.println(result);
            case CONTAINER -> {
                var cont = (IValidationResultContainer) result;
                builder.println("CONT-" + (cont.isOk() ? " OK" : "ERR") + " " + cont.getId());
                ((IValidationResultContainer) result).getNestedResults()
                                .forEach(inner -> hierarchyFormat(builder, level + 1, inner ));
            }
        }
    }

    public static Flux<@NonNull IValidationResult> tryAppendAnnotation(Supplier<? extends Publisher<IValidationResult>> dispatched, IValidationId id) {

        return Flux.defer(dispatched)
                .flatMap(vr -> {
                    if(vr.isOk()) {
                        return Flux.just(vr, ValidationResultFactory.createAnnotation(id));
                    }
                    return Mono.just(vr);
                });
    }

}
