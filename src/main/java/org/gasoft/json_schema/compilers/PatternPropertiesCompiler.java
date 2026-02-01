package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.base.BasePropertiesCompiler;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class PatternPropertiesCompiler extends BasePropertiesCompiler implements INamedCompiler{

    @Override
    public String getKeyword() {
        return "patternProperties";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_APPLICATOR,
                Defaults.DRAFT_2019_09_APPLICATOR
        );
    }

    @Override
    public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isObject(), schemaLocator,"The {0} keyword value must be object. Actual {1}", getKeyword(), schemaNode);
        final PatternPropertiesTask patternPropertiesTask = new PatternPropertiesTask();
        schemaNode.propertyStream()
                .forEach(property ->
                    patternPropertiesTask.addValidator(
                            property.getKey(),
                            compileContext.getConfig().getRegexpFactory().compile(property.getKey()),
                            compileContext.compile(property.getValue(), schemaLocator.appendProperty(property.getKey()))
                    )
                );

        return new PropertiesValidator(
                schemaLocator,
                patternPropertiesTask::findValidators,
                compileContext.getConfig()
        );
    }

    private record PatternValidator(String patternStr, Predicate<String> matchPredicate, IValidator validator){}

    private static class PatternPropertiesTask {

        private final List<PatternPropertiesCompiler.PatternValidator> list = new ArrayList<>();

        void addValidator(String patternStr, Predicate<String> pattern, IValidator validator) {
            list.add(new PatternValidator(patternStr, pattern, validator));
        }

        @Nullable
        Stream<IValidator> findValidators(String property) {
            return list.stream()
                    .filter(p -> p.matchPredicate().test(property))
                    .map(PatternValidator::validator);
        }
    }

}
