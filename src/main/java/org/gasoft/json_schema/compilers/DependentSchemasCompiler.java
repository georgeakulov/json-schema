package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class DependentSchemasCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "dependentSchemas";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_APPLICATOR,
                Defaults.DRAFT_2019_09_APPLICATOR
        );
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isObject(), schemaLocator,
                "The {0} keyword value must be an object", getKeyword()
        );
        Map<String, IValidator> validators = schemaNode.propertyStream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> compileContext.compile(entry.getValue(), schemaLocator.appendProperty(entry.getKey()))
                ));

        return (instance, instancePtr, context) -> {

            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);
            if(instance.isObject()) {
                return Flux.fromStream(instance.propertyStream())
                        .filter(entry -> validators.containsKey(entry.getKey()))
                        .parallel()
                        .flatMap(entry -> {
                            var validator = validators.get(entry.getKey());
                            if(validator == null) {
                                return Mono.empty();
                            }

                            return validator.validate(instance, instancePtr, context);
                        })
                        .sequential()
                        .reduce(
                                ValidationResultFactory.createContainer(id),
                                ValidationResultFactory.ValidationResultContainer::append
                        )
                        .map(val -> val);
            }
            return ValidationResultFactory.createOk(id).publish();
        };
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        node.propertyStream()
                .forEach(entry -> mediator.process(pointer.appendProperty(entry.getKey())));
    }
}
