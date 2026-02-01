package org.gasoft.json_schema.compilers.base;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.CompileConfig;
import org.gasoft.json_schema.compilers.INamedCompiler;
import org.gasoft.json_schema.compilers.IValidationContext;
import org.gasoft.json_schema.compilers.IValidator;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class BasePropertiesCompiler implements INamedCompiler {

    public static class PropertiesValidator implements IValidator {

        private final ISchemaLocator schemaLocation;
        private final Function<String, Stream<IValidator>> validatorResolver;
        private final CompileConfig config;
        public PropertiesValidator(ISchemaLocator schemaLocation, Function<String, Stream<IValidator>> validatorResolver, CompileConfig config) {
            this.schemaLocation = schemaLocation;
            this.validatorResolver = validatorResolver;
            this.config = config;
        }

        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {

            if(instance.isObject()) {

                return Flux.fromStream(instance.propertyStream())
                        .flatMap(entry ->
                                Flux.fromStream(
                                    validatorResolver.apply(entry.getKey())
                                            .filter(Objects::nonNull)
                                            .map(validator -> new FieldValidator(entry.getKey(), schemaLocation, validator))
                                )
                        )
                        .flatMap(named -> named.validate(instance.get(named.name()), instancePtr, context))
                        .reduce(
                                ValidationResultFactory.createContainer(schemaLocation, instancePtr),
                                ValidationResultFactory.ValidationResultContainer::append
                        )
                        .map(value -> value);
            }

            return ValidationResultFactory.createOk(schemaLocation, instancePtr)
                    .publish();
        }
    }

    public record FieldValidator(String name, ISchemaLocator schemaLocation, IValidator validator) implements IValidator {
        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {
            var ptr = instancePtr.appendProperty(name);
            return Mono.from(validator.validate(instance, ptr, context))
                    .flatMapMany(validationResult -> {

                        var result = Flux.just(validationResult);
                        // Analyse field checking results
                        if(validationResult.isOk()) {
                            result = result.concatWith(Mono.just(ValidationResultFactory.createAnnotation(schemaLocation, ptr)));
                        }
                        return result;
                    });
        }
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        if(node.isObject()) {
            node.propertyStream()
                    .forEach(property -> mediator.process(pointer.appendProperty(property.getKey())));
        }
    }
}
