package org.gasoft.json_schema.compilers.base;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.common.LocatedSchemaCompileException;
import org.gasoft.json_schema.compilers.CompileContext;
import org.gasoft.json_schema.compilers.INamedCompiler;
import org.gasoft.json_schema.compilers.IValidator;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public abstract class BaseSomeOfCompiler implements INamedCompiler {

    protected List<IValidator> prepareValidators(JsonNode schemaNode, ISchemaLocator schemaLocation, CompileContext compileContext) {
        LocatedSchemaCompileException.checkIt(schemaNode.isArray() && !schemaNode.isEmpty(), schemaLocation,
                "The value of %s keyword keyword must be an non empty array");
        List<IValidator> validators = new ArrayList<>();
        for(int idx = 0; idx < schemaNode.size(); ++idx) {
            validators.add(compileContext.compile(schemaNode.get(idx), schemaLocation.appendIndex(idx)));
        }
        return validators;
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        if(node.isArray()) {
            IntStream.range(0, node.size())
                    .forEach(idx -> mediator.process(pointer.appendIndex(idx)));
        }
    }
}
