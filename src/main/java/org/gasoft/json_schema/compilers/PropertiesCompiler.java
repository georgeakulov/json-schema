package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.base.BasePropertiesCompiler;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class PropertiesCompiler extends BasePropertiesCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "properties";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_APPLICATOR, Defaults.DRAFT_2019_09_APPLICATOR);
    }

    @Override
    public @NonNull IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isObject(), schemaLocator,"The {0} keyword value must be an object. Actual {1}", getKeyword(), schemaNode);
        final PropertiesTask propertiesTask = new PropertiesTask();
        schemaNode.propertyStream()
                .forEach(property ->
                    propertiesTask.addValidator(property.getKey(),
                            compileContext.compile(property.getValue(), schemaLocator.appendProperty(property.getKey())))
                );

        return new PropertiesValidator(schemaLocator, propertiesTask::getValidators, compileContext.getConfig());
    }

    private static class PropertiesTask {

        private final Map<String, IValidator> validators = new HashMap<>();

        void addValidator(String property, IValidator validator) {
            this.validators.put(property, validator);
        }

        Stream<IValidator> getValidators(String propertyName) {
            return Stream.of(validators.get(propertyName))
                    .filter(Objects::nonNull);
        }
    }
}
