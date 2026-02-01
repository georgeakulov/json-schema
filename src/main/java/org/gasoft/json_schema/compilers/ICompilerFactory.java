package org.gasoft.json_schema.compilers;

import org.gasoft.json_schema.dialects.Vocabulary;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

public interface ICompilerFactory {

    Stream<IVocabularySupport> getSupportedKeywords();

    @Nullable ICompiler getCompiler(String keyword, Vocabulary vocabulary);

    interface IVocabularySupport {
        List<String> keywords();
        URI vocabulary();
    }
}
