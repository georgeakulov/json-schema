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

public class MaxItemsCompiler extends BaseIntegerCompiler {

    @Override
    public String getKeyword() {
        return "maxItems";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_VALIDATION, Defaults.DRAFT_2019_09_VALIDATION);
    }

    @Override
    protected IValidator compile(int maxItems, CompileContext compileContext, ISchemaLocator schemaLocation) {
        checkIt(maxItems >= 0, schemaLocation,
                "The {0} keyword value must be non-negative. Actual: {1}", getKeyword(), maxItems);
        return (instance, instancePtr,context) -> {
            var id = ValidationResultFactory.createId(schemaLocation, instancePtr);

            if(instance.isArray() && instance.size() > maxItems) {
                return ValidationError.create(id, EErrorType.MAX_ITEMS, getKeyword(), maxItems, instance.size())
                        .publish();
            }
            return ValidationResultFactory.createOk(id).publish();
        };
    }
}
