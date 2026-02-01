package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.base.BaseNumberCompiler;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;

import java.net.URI;
import java.util.stream.Stream;

public class ExclusiveMaximumCompiler extends BaseNumberCompiler {

    @Override
    public String getKeyword() {
        return "exclusiveMaximum";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_VALIDATION,
                Defaults.DRAFT_2019_09_VALIDATION
        );
    }

    @Override
    protected IValidationResult analyse(IValidationId id, JsonNode schemaValue, JsonNode instanceValue, int compareResult) {
        if(compareResult <= 0) {
            return ValidationError.create(id, EErrorType.EXCLUSIVE_MAXIMUM, instanceValue, schemaValue);
        }
        return ValidationResultFactory.createOk(id);
    }
}
