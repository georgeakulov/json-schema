package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.base.BaseSomeOfCompiler;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;


public class AllOfCompiler extends BaseSomeOfCompiler {

    @Override
    public String getKeyword() {
        return "allOf";
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
        List<IValidator> validators = super.prepareValidators(schemaNode, schemaLocator, compileContext);
        return (instance, instancePtr, context) ->
            Flux.fromIterable(validators)
                    .flatMap(validator -> validator.validate(instance, instancePtr, context))
                    .subscribeOn(compileContext.getConfig().getScheduler())
                    .reduce(
                            ValidationResultFactory.createContainer(ValidationResultFactory.createId(schemaLocator, instancePtr)),
                            ValidationResultFactory.ValidationResultContainer::append
                    )
                    .map(val -> val);
    }
}
