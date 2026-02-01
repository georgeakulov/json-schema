package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.util.stream.Stream;

public class ConstCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "const";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_VALIDATION,
                Defaults.DRAFT_2019_09_VALIDATION
        );
    }

    @Override
    public @NonNull IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
        return (node, instancePtr, context) -> {
            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);
            if (Utils.checkEquality(node, schemaNode)) {
                return ValidationResultFactory.createOk(id).publish();
            }
            return ValidationError.create(id, EErrorType.CONST, node, schemaNode)
                    .publish();
        };
    }
}
