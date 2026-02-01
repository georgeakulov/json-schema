package org.gasoft.json_schema.compilers;

import org.gasoft.json_schema.dialects.Vocabulary;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.SchemaCompileException.checkIt;
import static org.gasoft.json_schema.common.SchemaCompileException.checkNonNull;

public class VocabularyCompilersRegistry {

    private final Map<String, Function<Vocabulary, ICompiler>> compilers = new HashMap<>();
    private final Set<IValidatorsTransformer> transformers = new HashSet<>();

    void add(String keyword, ICompiler compiler) {
        checkNonNull(compiler, "Compiler is null");
        add(keyword, ignore -> compiler);
        tryAddTransformers(compiler);
    }

    private void add(String keyword, Function<Vocabulary, ICompiler> compiler) {
        checkNonNull(keyword, "Keyword is null");
        checkIt(compilers.put(keyword, compiler) == null, "The compiler for {0} keyword is duplicated", keyword);
    }

    void add(String keyword, ICompilerFactory compilerFactory) {
        checkNonNull(compilerFactory, "CompilerFactory is null");
        add(keyword, new CompilerFunc(keyword, compilerFactory));
        tryAddTransformers(compilerFactory);
    }

    @Nullable
    public Function<Vocabulary, ICompiler> getCompiler(String keyword) {
        return compilers.get(keyword);
    }

    private void tryAddTransformers(Object mayBeTransformer) {
        if(mayBeTransformer instanceof IValidatorsTransformer) {
            transformers.add((IValidatorsTransformer) mayBeTransformer);
        }
    }

    public Stream<IValidatorsTransformer> getTransformers() {
        return transformers.stream();
    }

    private record CompilerFunc(String keyword, ICompilerFactory factory) implements Function<Vocabulary, ICompiler> {

        @Override
        public ICompiler apply(Vocabulary vocabulary) {
            return factory.getCompiler(keyword, vocabulary);
        }
    }
}
