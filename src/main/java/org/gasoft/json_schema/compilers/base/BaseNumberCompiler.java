package org.gasoft.json_schema.compilers.base;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.CompileContext;
import org.gasoft.json_schema.compilers.INamedCompiler;
import org.gasoft.json_schema.compilers.IValidator;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.ValidationResultFactory;

import java.math.BigDecimal;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public abstract class BaseNumberCompiler implements INamedCompiler {

    protected abstract IValidationResult analyse(IValidationId id, JsonNode schemaValue, JsonNode instanceValue, int compareResult);

    @Override
    public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isNumber(), schemaLocator,
                "Value of %s keyword must be a number. Actual: %s", getKeyword(), schemaNode);
        BigDecimal decimal = schemaNode.decimalValue();
        return (instance, instancePtr, context) -> {

            var id = ValidationResultFactory.createId(schemaLocator, instancePtr);
            if(instance.isNumber()) {
                return analyse(id, schemaNode, instance, decimal.compareTo(instance.decimalValue())).publish();
            }
            return ValidationResultFactory.createOk(id).publish();
        };
    }
}
