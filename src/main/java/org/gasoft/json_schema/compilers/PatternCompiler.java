package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;

import java.net.URI;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class PatternCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "pattern";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_VALIDATION,
                Defaults.DRAFT_2019_09_VALIDATION
        );
    }

    @Override
    public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isTextual(), schemaLocator,
                "The {0} keyword value must be a string", getKeyword());
        String patternStr = schemaNode.asText();
        Predicate<String> patternPredicate = compileContext.getConfig().getRegexpFactory().compile(patternStr);

        return (node, instancePtr, context) -> {

            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);

            if(node.isTextual() && !patternPredicate.test(node.asText())) {
                return ValidationError.create(id, EErrorType.PATTERN, node.asText(), patternStr)
                        .publish();
            }
            return ValidationResultFactory.createOk(id).publish();
        };
    }
}
