package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

/**
 * Used when sourceUri presented as boolean value
 */
public class SchemaAsBooleanCompiler implements ICompiler {

    @Override
    public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isBoolean(), schemaLocator, "Illegal value node. Expected boolean, actual {0}", schemaNode.getNodeType());
        return (instance, instancePtr, context) -> {
            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);
            if(schemaNode.booleanValue()) {
                return ValidationResultFactory.createOk(id).publish();
            }
            return ValidationError.create(id, EErrorType.FALSE_SCHEMA).publish();
        };
    }
}
