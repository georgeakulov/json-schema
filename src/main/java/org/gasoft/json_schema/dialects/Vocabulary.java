package org.gasoft.json_schema.dialects;

import org.gasoft.json_schema.compilers.CompilerRegistry;
import org.gasoft.json_schema.compilers.IValidatorsTransformer;
import org.gasoft.json_schema.compilers.VocabularyCompilersRegistry;
import org.gasoft.json_schema.compilers.ICompiler;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;

public class Vocabulary {

    private final URI vocabularyUri;
    private final VocabularyCompilersRegistry registry;

    public Vocabulary(URI vocabularyUri) {
        this.vocabularyUri = vocabularyUri;
        var foundRegistry = CompilerRegistry.getInstance().getCompilersForVocabulary(vocabularyUri);
        if(foundRegistry == null) {
            foundRegistry = new VocabularyCompilersRegistry();
        }
        this.registry = foundRegistry;
    }

    public URI getUri() {
        return vocabularyUri;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Vocabulary that = (Vocabulary) o;
        return Objects.equals(vocabularyUri, that.vocabularyUri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(vocabularyUri);
    }

    @Nullable
    ICompiler opt(String keyword) {
        var item  = registry.getCompiler(keyword);
        if(item != null) {
            return item.apply(this);
        }
        return null;
    }

    Stream<IValidatorsTransformer> getTransformers() {
        return registry.getTransformers();
    }

}
