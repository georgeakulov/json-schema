package org.gasoft.json_schema.compilers.base;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.CompileContext;
import org.gasoft.json_schema.compilers.INamedCompiler;
import org.gasoft.json_schema.compilers.IValidator;
import org.gasoft.json_schema.loaders.IReferenceResolver.IResolutionResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.jspecify.annotations.Nullable;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;
import static org.gasoft.json_schema.compilers.IdCompiler.isSame;

public abstract class BaseReferenceCompiler implements INamedCompiler {

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {

        checkIt(schemaNode.isTextual(),schemaLocator, "The {0} keyword value must be an string", getKeyword());

        IResolutionResult result = resolveRef(compileContext, schemaNode.textValue(), schemaLocator);
        ISchemaLocator locator = result.getResolvedLocator();
        if (isSame(schemaLocator, result.getResolvedLocator())) {
            locator = schemaLocator;
        }
        JsonNode navigatedToPtr = result.getSchema().at(result.getReferencedPtr());
        checkIt(!navigatedToPtr.isMissingNode(), schemaLocator,"Invalid {0} keyword value resolution result. Can`t detect subschema",
                result);

        return compileContext.compile(navigatedToPtr, locator);
    }

    protected abstract IResolutionResult resolveRef(CompileContext context, String textValue, ISchemaLocator schemaLocator);
}
