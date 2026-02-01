package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class AdditionalPropertiesCompiler implements INamedCompiler, IValidatorsTransformer {

    @Override
    public String getKeyword() {
        return "additionalProperties";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_APPLICATOR, Defaults.DRAFT_2019_09_APPLICATOR);
    }

    @Override
    public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        return compileContext.compile(schemaNode, schemaLocator);
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        mediator.process(pointer);
    }

    @Override
    public void transform(Map<String, IValidatorAction> validators, CompileContext compileContext) {

        var current = validators.get(getKeyword());
        if(current == null) {
            return;
        }

        var preferValidators = Stream.of("properties", "patternProperties")
                .map(validators::remove)
                .filter(Objects::nonNull)
                .toList();

        var finishValidator = new BaseFinisherValidator(
                current,
                preferValidators,
                JsonNode::isObject,
                this::finishValidation
        );
        validators.put(getKeyword(), finishValidator);
    }

    private Publisher<? extends IValidationResult> finishValidation(
            IValidationId id,
            IValidator original,
            List<IValidationResult> validationResults,
            JsonNode instance,
            JsonPointer instancePtr,
            IValidationContext context) {

        Set<String> evaluatedFields = BaseFinisherValidator.filterAnnotationsProperty(validationResults, instancePtr);

        return Flux.fromStream(instance.propertyStream())
                .filter(prop -> !evaluatedFields.contains(prop.getKey()))
                .flatMap(prop -> {
                    JsonPointer instanceConcretePtr = instancePtr.appendProperty(prop.getKey());
                    return Mono.from(original.validate(prop.getValue(), instanceConcretePtr, context))
                            .map(validationResult -> {

                                // Analyse field checking results
                                if (validationResult.isOk()) {
                                    return ValidationResultFactory.createAnnotation(id.getSchemaLocator(), instanceConcretePtr);
                                } else {
                                    return validationResult;
                                }
                            });
                })
                .reduce(
                        ValidationResultFactory.createContainer(id),
                        ValidationResultFactory.ValidationResultContainer::append
                )
                .map(val -> val);
    }
}
