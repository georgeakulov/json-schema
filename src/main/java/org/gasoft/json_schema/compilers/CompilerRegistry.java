package org.gasoft.json_schema.compilers;

import org.gasoft.json_schema.compilers.ICompilerFactory.IVocabularySupport;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.*;

public class CompilerRegistry {

    private final Map<URI, VocabularyCompilersRegistry> registries = new HashMap<>();
    CompilerRegistry() {
        // Add all compilers to registry
    }

    public static CompilerRegistry getInstance() {
        return CommonCompilersFactory.getCompilerRegistry();
    }

    @Nullable
    public VocabularyCompilersRegistry getCompilersForVocabulary(URI uri) {
        return registries.get(uri);
    }

    CompilerRegistry addCompiler(INamedCompiler compiler) {
        Objects.requireNonNull(compiler);
        compiler.getVocabularies().forEach(vocabulary ->
            registries.computeIfAbsent(vocabulary, ignore -> new VocabularyCompilersRegistry())
                    .add(compiler.getKeyword(), compiler)
        );
        return this;
    }

    CompilerRegistry addCompiler(ICompilerFactory compilerFactory) {
        Objects.requireNonNull(compilerFactory);
        compilerFactory.getSupportedKeywords().forEach(supported -> {
            var reg = registries.computeIfAbsent(
                            supported.vocabulary(),
                            ignore -> new VocabularyCompilersRegistry());
            supported.keywords().forEach(keyword -> reg.add(keyword, compilerFactory));
        });
        return this;
    }

    public record VocabularySupport(List<String> keywords, URI vocabulary) implements IVocabularySupport {
        public static VocabularySupport of(String keyword, URI vocabulary) {
            return new VocabularySupport(List.of(keyword), vocabulary);
        }
        public static VocabularySupport of(URI vocabulary, String ... keywords) {
            return new VocabularySupport(Arrays.asList(keywords), vocabulary);
        }
    }
}
