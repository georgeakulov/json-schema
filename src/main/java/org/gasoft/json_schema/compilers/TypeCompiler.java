package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;
import static org.gasoft.json_schema.common.LocatedSchemaCompileException.create;

public class TypeCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "type";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_VALIDATION, Defaults.DRAFT_2019_09_VALIDATION);
    }

    @NonNull
    public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        List<Function<JsonNode, Boolean>> validTypes = new ArrayList<>();
        if(schemaNode.isArray()) {
            schemaNode.valueStream()
                    .peek(val -> checkKeywordValueType(schemaLocator, val))
                    .map(JsonNode::asText)
                    .map(val -> resolveType(schemaLocator, val, compileContext.getConfig()))
                    .distinct()
                    .forEach(validTypes::add);
        }
        else {

            checkKeywordValueType(schemaLocator, schemaNode);
            validTypes.add(resolveType(schemaLocator, schemaNode.textValue(), compileContext.getConfig()));
        }

        return (instance, instancePtr, context) -> {

            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);

            return validTypes.stream()
                    .parallel()
                    .map(func -> func.apply(instance))
                    .filter(val -> val)
                    .findAny()
                    .map(ignore -> ValidationResultFactory.createOk(id))
                    .orElseGet(() -> ValidationError.create(
                                            id,
                                            EErrorType.TYPE,
                                            instance.asText(), schemaNode
                ))
                    .publish();
        };
    }

    private void checkKeywordValueType(ISchemaLocator locator, JsonNode value) {
        checkIt(value.isTextual(), locator, "The {0} keyword values be a string or array of strings", getKeyword());
    }

    private Function<JsonNode, Boolean> resolveType(ISchemaLocator locator, String value, CompileConfig cfg) {
        return switch (value) {
            case "null" -> validateByType(JsonNodeType.NULL);
            case "string" -> validateByType(JsonNodeType.STRING);
            case "boolean" -> validateByType(JsonNodeType.BOOLEAN);
            case "number" -> validateByType(JsonNodeType.NUMBER);
            case "integer" -> validateInteger();
            case "array" -> validateArray(cfg);
            case "object" -> validateByType(JsonNodeType.OBJECT);
            default -> throw create(locator, "The {0} keyword value {1} is not supported",  getKeyword(), value);
        };
    }

    private Function<JsonNode, Boolean> validateInteger() {
        return node -> {
            if(node.getNodeType() != JsonNodeType.NUMBER) {
                return false;
            }
            return node.decimalValue().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0;
        };
    }

    private Function<JsonNode, Boolean> validateArray(CompileConfig cfg) {
        return node -> {
            if(cfg.isAllowTreatAsArray()) {
                return true;
            }
            return node.getNodeType() == JsonNodeType.ARRAY;
        };
    }

    private Function<JsonNode, Boolean> validateByType(JsonNodeType type) {
        return node -> node.getNodeType() == type;
    }
}

