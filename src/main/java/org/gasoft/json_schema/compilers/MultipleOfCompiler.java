package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.net.URI;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class MultipleOfCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "multipleOf";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_VALIDATION,
                Defaults.DRAFT_2019_09_VALIDATION
        );
    }

    @Override
    public @NonNull IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isNumber(), schemaLocator, "The {0} keyword value must be number", getKeyword());
        BigDecimal schemaValue = schemaNode.decimalValue();
        checkIt(schemaNode.decimalValue().compareTo(BigDecimal.ZERO) > 0, schemaLocator,
                "The {0} keyword value must be a positive number", getKeyword());
        return (node, instancePtr, context) -> {

            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);

            if(node.decimalValue().divideAndRemainder(schemaValue)[1].abs().compareTo(BigDecimal.ZERO) > 0) {
                return ValidationError.create(id, EErrorType.MULTIPLE_OF, node, schemaNode)
                        .publish();
            }
            return ValidationResultFactory.createOk(id).publish();
        };
    }
}
