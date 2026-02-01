package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.stream.Stream;

import static org.gasoft.json_schema.results.EErrorType.NOT;

public class NotCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "not";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_APPLICATOR,
                Defaults.DRAFT_2019_09_APPLICATOR
        );
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
        IValidator validator = compileContext.compile(schemaNode, schemaLocator);
        return (instance, instancePtr, context) ->
            Flux.defer(() -> validator.validate(instance, instancePtr, context))
                    .filter(result -> !result.isOk())
                    .collectList()
                    .map(invalidResults -> {
                        var id = ValidationResultFactory.createId(schemaLocator, instancePtr);

                        if(invalidResults.isEmpty()) {
                            return ValidationError.create(id, NOT);
                        }
                        return ValidationResultFactory.createOk(id);
                    });
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        mediator.process(pointer);
    }
}
