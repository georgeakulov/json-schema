package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.stream.Stream;

public class DefsCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "$defs";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_CORE, Defaults.DRAFT_2019_09_CORE);
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
        return null;
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        if(node.isObject()) {
            node.propertyStream().forEach(entry ->
                    mediator.process(pointer.appendProperty(entry.getKey()))
            );
        }
    }
}
