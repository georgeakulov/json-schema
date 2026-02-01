package org.gasoft.json_schema.compilers.v2019;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.*;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Items2019Compiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "items";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2019_09_APPLICATOR);
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {


        if(schemaNode.isArray()) {
            List<IValidator> validators = new ArrayList<>();
            for(int idx = 0; idx < schemaNode.size(); idx++) {
                validators.add(compileContext.compile(schemaNode.get(idx), schemaLocator.appendIndex(idx)));
            }
            return new ArrayValidator(validators, compileContext.getConfig(), schemaLocator);
        }
        else {
            return new SingleValidator(compileContext.compile(schemaNode, schemaLocator), compileContext.getConfig(), schemaLocator);
        }
    }

    private record ArrayValidator(List<IValidator> validators, CompileConfig config, ISchemaLocator locator) implements IValidator {
        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {
            var id = ValidationResultFactory.createId(locator, instancePtr);
            if(instance.isArray()) {
                return Flux.range(0, Math.min(validators.size(), instance.size()))
                        .flatMap(idx -> {
                            var itemPtr = instancePtr.appendIndex(idx);
                            return ValidationResultFactory.tryAppendAnnotation(
                                    () -> validators.get(idx).validate(instance.get(idx), itemPtr, context),
                                    ValidationResultFactory.createId(locator, itemPtr)
                            );
                        })
                        .subscribeOn(config.getScheduler())
                        .reduce(
                                ValidationResultFactory.createContainer(id),
                                ValidationResultFactory.ValidationResultContainer::append
                        )
                        .map(value -> value);
            }
            return ValidationResultFactory.createOk(id).publish();
        }
    }

    private record SingleValidator(IValidator validator, CompileConfig config, ISchemaLocator locator) implements IValidator {

        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {
            var id = ValidationResultFactory.createId(locator, instancePtr);
            if(instance.isArray()) {
                return Flux.range(0, instance.size())
                        .flatMap(idx -> {
                            var itemPtr = instancePtr.appendIndex(idx);
                            return ValidationResultFactory.tryAppendAnnotation(
                                    () -> validator.validate(instance.get(idx), itemPtr, context),
                                    ValidationResultFactory.createId(locator, itemPtr)
                            );
                        })
                        .subscribeOn(config.getScheduler())
                        .reduce(
                                ValidationResultFactory.createContainer(id),
                                ValidationResultFactory.ValidationResultContainer::append
                        )
                        .map(value -> value);
            }
            return ValidationResultFactory.createOk(id).publish();
        }
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        if(node.isObject()) {
            mediator.process(pointer);
        }
        else if(node.isArray()) {
            IntStream.range(0, node.size())
                    .forEach(idx -> mediator.process(pointer.appendIndex(idx)));
        }
    }

}
