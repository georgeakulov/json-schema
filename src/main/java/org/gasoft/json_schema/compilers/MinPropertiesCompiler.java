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

public class MinPropertiesCompiler extends BaseIntegerCompiler {

    @Override
    public String getKeyword() {
        return "minProperties";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_VALIDATION,
                Defaults.DRAFT_2019_09_VALIDATION
        );
    }

    @Override
    protected IValidator compile(int minProperties, CompileContext compileContext, ISchemaLocator schemaLocation) {

        checkIt(minProperties >= 0, schemaLocation,
                "The {0} keyword value must be non negative integer. Actual: {1}", getKeyword(), minProperties);

        return (instance, instancePtr, context) -> {

            var id = ValidationResultFactory.createId(schemaLocation, instancePtr);

            if(instance.isObject() && instance.size() < minProperties) {
                return ValidationError.create(id, EErrorType.MIN_PROPERTIES, minProperties, instance.size())
                        .publish();
            }
            return ValidationResultFactory.createOk(id).publish();
        };
    }
}
