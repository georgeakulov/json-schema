package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.dialects.Vocabulary;
import org.gasoft.json_schema.results.IValidationResult;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

import static org.gasoft.json_schema.compilers.CompilerRegistry.VocabularySupport.of;

public class EmptyCompilerFactory implements ICompilerFactory{

    private static final ICompiler EMPTY = new EmptyCompiler();

    @Override
    public Stream<IVocabularySupport> getSupportedKeywords() {
        return Stream.of(
                of(Defaults.DRAFT_2020_12_CORE, "$schema", "$comment", "$anchor", "$dynamicAnchor", "$vocabulary"),
                of(Defaults.DRAFT_2020_12_META_DATA, "title", "description", "deprecated", "readOnly", "writeOnly", "examples", "default"),
                of(Defaults.DRAFT_2020_12_CONTENT,  "contentEncoding", "contentMediaType", "contentSchema"),

                of(Defaults.DRAFT_2019_09_CORE, "$schema", "$comment", "$anchor", "$recursiveAnchor", "$vocabulary"),
                of(Defaults.DRAFT_2019_09_META_DATA,"title", "description", "deprecated", "readOnly", "writeOnly", "examples", "default")
        );
    }

    @Override
    public @Nullable ICompiler getCompiler(String keyword, Vocabulary vocabulary) {
        return EMPTY;
    }

    private static class EmptyCompiler implements ICompiler {
        @Override
        public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
            return null;
        }
    }
}
