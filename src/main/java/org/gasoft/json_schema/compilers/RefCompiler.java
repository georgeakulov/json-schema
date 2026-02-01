package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.loaders.IReferenceResolver.IResolutionResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;
import static org.gasoft.json_schema.compilers.IdCompiler.isSame;

public class RefCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "$ref";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_CORE, Defaults.DRAFT_2019_09_CORE);
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isTextual(), schemaLocator,"The {0} keyword value must be an string", getKeyword());

        IResolutionResult result = compileContext.resolveRef(schemaNode.textValue(), schemaLocator);
        ISchemaLocator locator = result.getResolvedLocator();
        if (isSame(schemaLocator, result.getResolvedLocator())) {
            locator = schemaLocator;
        }
        JsonNode navigatedToPtr = result.getSchema().at(result.getReferencedPtr());
        checkIt(!navigatedToPtr.isMissingNode(), schemaLocator,
                "The {0} keyword resolution result is invalid. Reference not exists in resolve result {1}",
                getKeyword(), result);

        return  compileContext.compile(navigatedToPtr, locator);
    }
}
