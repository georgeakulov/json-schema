package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.ValidationResultFactory;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.stream.Stream;

public class PropertyNamesCompiler implements INamedCompiler{

    @Override
    public String getKeyword() {
        return "propertyNames";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_APPLICATOR,
                Defaults.DRAFT_2019_09_APPLICATOR
        );
    }

    @Override
    public IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {

        IValidator valueValidator = compileContext.compile(schemaNode, schemaLocator);

        return (instance, instancePtr,context) -> {

            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);

            if(instance.isObject()) {
                return Flux.fromStream(instance.propertyStream())
                        .parallel()
                        .flatMap(entry -> valueValidator.validate(TextNode.valueOf(entry.getKey()), instancePtr.appendProperty(entry.getKey()), context))
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
        mediator.process(pointer);
    }
}
