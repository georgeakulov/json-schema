package org.gasoft.json_schema.compilers.base;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.common.LocatedSchemaCompileException;
import org.gasoft.json_schema.compilers.INamedCompiler;
import org.gasoft.json_schema.compilers.IValidationContext;
import org.gasoft.json_schema.compilers.IValidator;
import org.gasoft.json_schema.compilers.Utils;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.reactivestreams.Publisher;

import java.util.function.BiPredicate;

public abstract class BaseLengthCompiler implements INamedCompiler {

    protected IValidator create(ISchemaLocator schemaLocation, BiPredicate<Integer, Integer> compareFunc, EErrorType errorType, JsonNode schemaNode) {
        return new Validator(schemaLocation, compareFunc, schemaNode, errorType);
    }

    private class Validator implements IValidator {

        final EErrorType errorType;
        final ISchemaLocator schemaLocation;
        final BiPredicate<Integer, Integer> compareFunc;
        final int value;

        public Validator(ISchemaLocator schemaLocation, BiPredicate<Integer, Integer> compareFunc, JsonNode schemaNode, EErrorType errorType) {
            this.errorType = errorType;
            this.schemaLocation = schemaLocation;
            this.compareFunc = compareFunc;

            value = Utils.getCheckedInteger(schemaLocation, schemaNode, "The value of %s keyword, must be integer. Actual: %s", getKeyword(), schemaNode);
            LocatedSchemaCompileException.checkIt(value >= 0, schemaLocation,
                    "The value of %s keyword must be greater than 0. Actual: %s", getKeyword(), value);
        }

        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instanceLocation, IValidationContext context) {
            var id = ValidationResultFactory.createId(schemaLocation, instanceLocation);
            if(instance.isTextual()) {
                String str = instance.asText();
                int visibleCharacters = str.codePointCount(0, str.length());
                if(!compareFunc.test(value,  visibleCharacters)) {
                    return ValidationError.create(id, errorType, value, visibleCharacters)
                            .publish();
                }
            }
            return ValidationResultFactory.createOk(id).publish();
        }
    }
}
