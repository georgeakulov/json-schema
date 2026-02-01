package org.gasoft.json_schema.compilers;

import org.gasoft.json_schema.compilers.base.BaseIntegerCompiler;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;

import java.net.URI;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class MaxPropertiesCompiler extends BaseIntegerCompiler {

    @Override
    public String getKeyword() {
        return "maxProperties";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_VALIDATION,
                Defaults.DRAFT_2019_09_VALIDATION
        );
    }

    @Override
    protected IValidator compile(int maxProperties, CompileContext compileContext, ISchemaLocator schemaLocation) {

        checkIt(maxProperties >= 0, schemaLocation,
                "The value of {0} keyword must be non negative integer. Actual: {1}", getKeyword(), maxProperties);

        return (instance, instancePtr, context) -> {

            var id = ValidationResultFactory.createId(schemaLocation, instancePtr);
            if(instance.isObject() && instance.size() > maxProperties) {
                return ValidationError.create(
                        id, EErrorType.MAX_PROPERTIES, maxProperties, instance.size()
                ).publish();
            }
            return ValidationResultFactory.createOk(id).publish();
        };
    }
}
