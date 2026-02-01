package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.gasoft.json_schema.common.LocatedSchemaCompileException;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class DependenciesCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "dependencies";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_APPLICATOR, Defaults.DRAFT_2019_09_CORE);
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {

        checkIt(schemaNode.isObject(), schemaLocator, "The {0} keyword value must be an object", getKeyword());
        Map<String, SubValidator> validators = schemaNode.propertyStream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> createSubValidator(schemaLocator, compileContext, entry)
                ));
        if(validators.isEmpty()) {
            return null;
        }
        return (instance, instancePtr, context) -> {
            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);
            return Flux.fromIterable(validators.values())
                    .parallel()
                    .flatMap(validator -> validator.validate(instance, instancePtr, context))
                    .sequential()
                    .reduce(
                            ValidationResultFactory.createContainer(id),
                            ValidationResultFactory.ValidationResultContainer::append
                    )
                    .map(val -> val);
        };
    }

    private SubValidator createSubValidator(ISchemaLocator locator, CompileContext context, Map.Entry<String, JsonNode> schemaEntry) {
        return switch(schemaEntry.getValue().getNodeType()) {
            case OBJECT, BOOLEAN -> new DependentSchemaSubValidator(locator.appendProperty(schemaEntry.getKey()), schemaEntry.getKey(), schemaEntry.getValue(), context);
            case ARRAY -> new DependentRequiredSubValidator(locator, schemaEntry.getKey(), (ArrayNode) schemaEntry.getValue());
            default ->
                throw LocatedSchemaCompileException.create(locator, "Values of {0} keyword properties must be an array or object", getKeyword());
        };
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        node.propertyStream()
                .filter(val -> val.getValue().isObject())
                .forEach(val -> mediator.process(pointer.appendProperty(val.getKey())));
    }

    private abstract static class SubValidator implements IValidator{

        protected final String property;
        protected final ISchemaLocator locator;

        public SubValidator(String property, ISchemaLocator locator) {
            this.property = property;
            this.locator = locator;
        }

        public abstract Publisher<IValidationResult> validateImpl(JsonNode instance, JsonPointer instancePtr, IValidationContext context);

        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instanceLocation, IValidationContext context) {
            if(instance.isObject()) {
                return validateImpl(instance, instanceLocation, context);
            }
            return createOk(instanceLocation).publish();
        }

        protected IValidationResult createOk(JsonPointer instancePtr) {
            return ValidationResultFactory.createOk(locator, instancePtr);
        }
    }

    private static class DependentSchemaSubValidator extends SubValidator {
        private final IValidator validator;

        public DependentSchemaSubValidator(ISchemaLocator locator, String key, JsonNode value, CompileContext context) {
            super(key, locator);
            validator = context.compile(value, locator);
        }

        @Override
        public Publisher<IValidationResult> validateImpl(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {
            if(instance.has(property)) {
                return validator.validate(instance, instancePtr, context);
            }
            return createOk(instancePtr).publish();
        }
    }


    private class DependentRequiredSubValidator extends SubValidator {

        private final Set<String> required = new HashSet<>();

        public DependentRequiredSubValidator(ISchemaLocator locator, String key, ArrayNode value) {

            super(key, locator);

            value.valueStream()
                    .peek(val ->
                            checkIt(val.getNodeType() == JsonNodeType.STRING, locator,
                            "The {0} keyword for property {1} must contains array of STRING", getKeyword(), key))
                    .forEach(val ->
                            checkIt(required.add(val.textValue()), locator,
                                    "The {0} keyword for property {1} contains duplicated strings", getKeyword(), key)
                    );
        }

        @Override
        public Publisher<IValidationResult> validateImpl(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {
            if(instance.has(property)) {
                if(!this.required.stream()
                        .allMatch(instance::has)) {
                    return ValidationError.create(
                            ValidationResultFactory.createId(locator, instancePtr),
                            EErrorType.DEPENDENCIES,
                            property
                    ).publish();
                }
            }
            return createOk(instancePtr).publish();
        }
    }
}
