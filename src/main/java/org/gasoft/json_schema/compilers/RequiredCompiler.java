package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class RequiredCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "required";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_VALIDATION,
                Defaults.DRAFT_2019_09_VALIDATION
        );
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isArray(), schemaLocator,
                "The {0} keyword value must be an array. Actual: {1}", getKeyword(), schemaNode.getNodeType());
        checkIt(schemaNode.valueStream().allMatch(JsonNode::isTextual), schemaLocator,
                "The {0} keyword value array items type nust an string", getKeyword());

        var namesSet = schemaNode.valueStream().map(JsonNode::asText).collect(Collectors.toSet());
        checkIt(namesSet.size() == schemaNode.size(), schemaLocator,
                "The {0} keyword value array items must be unique", getKeyword());
        if(namesSet.isEmpty()) {
            return null;
        }
        return (instance, instancePtr, context) -> {

            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);

            if(instance.isObject()) {
                var missingProperties = namesSet.stream().filter(name -> !instance.has(name))
                        .toList();
                if(!missingProperties.isEmpty()) {
                    return ValidationError.create(id, EErrorType.REQUIRED, String.join(",", missingProperties))
                            .publish();
                }
            }

            return ValidationResultFactory.createOk(id).publish();
        };
    }
}
