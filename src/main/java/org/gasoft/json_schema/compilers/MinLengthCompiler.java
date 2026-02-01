package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.base.BaseLengthCompiler;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult;

import java.net.URI;
import java.util.stream.Stream;

public class MinLengthCompiler extends BaseLengthCompiler {

    @Override
    public String getKeyword() {
        return "minLength";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_VALIDATION, Defaults.DRAFT_2019_09_VALIDATION);
    }

    @Override
    public IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
        return super.create(
                schemaLocator,
                (expected, actual) -> actual >= expected,
                EErrorType.MIN_LENGTH,
                schemaNode
        );
    }
}
