package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.base.BaseSomeOfCompiler;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

public class AnyOfCompiler extends BaseSomeOfCompiler {

    @Override
    public String getKeyword() {
        return "anyOf";
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
        List<IValidator> validators = super.prepareValidators(schemaNode, schemaLocator, compileContext);
        return (instance, instancePtr, context) ->
            Flux.fromIterable(validators)
                    .flatMap(validator -> validator.validate(instance, instancePtr, context))
                    .filter(IValidationResult::isOk)
                    .subscribeOn(compileContext.getConfig().getScheduler())
                    .collectList()
                    .map(list -> {

                        var id = ValidationResultFactory.createId(schemaLocator, instancePtr);

                        if(list.isEmpty()) {
                            return ValidationError.create(id, EErrorType.ANY_OF);
                        }

                        return ValidationResultFactory.createContainer(id)
                                .appendAll(list);
                    });
    }
}
