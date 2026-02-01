package org.gasoft.json_schema.dialects;

import org.gasoft.json_schema.common.SchemaCompileException;
import org.gasoft.json_schema.compilers.ICompiler;
import org.gasoft.json_schema.compilers.IValidatorsTransformer;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Dialect {

    private final DialectInfo dialectInfo;
    private final Set<Vocabulary> vocabularies;

    private Dialect(DialectInfo dialectInfo, Set<Vocabulary> vocabularies) {
        this.dialectInfo = dialectInfo;
        this.vocabularies = vocabularies;
    }

    static Dialect create(DialectInfo dialectInfo, Function<URI, Vocabulary> vocabularyResolver) {
        return new Dialect(dialectInfo,
                dialectInfo.asStream()
                        .map(state -> {
                            var cod = vocabularyResolver.apply(state.vocabulary());
                            if(cod == null) {
                                if(state.state()) {
                                    throw SchemaCompileException.create("The vocabulary {0} is required but not registered", state.vocabulary());
                                }
                            }
                            return cod;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
    }

    public boolean isAssertionRequired() {
        return dialectInfo.getVocabularyState(Defaults.DRAFT_2020_12_FORMAT_ASSERTION)
                .map(DialectInfo.VocabularyState::state)
                .orElse(false);
    }

    public @Nullable ICompiler optCompiler(String keyword) {
        return this.vocabularies.stream()
                .map(voc -> voc.opt(keyword))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    public Stream<IValidatorsTransformer> getTransformers() {
        return this.vocabularies.stream()
                .flatMap(Vocabulary::getTransformers)
                .distinct();
    }
}
