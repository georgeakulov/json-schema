package org.gasoft.json_schema.dialects;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.gasoft.json_schema.dialects.Defaults.*;

public class DialectRegistry {

    private static final DialectRegistry INSTANCE = new DialectRegistry();

    private final Map<URI, DialectInfo> predefined = new HashMap<>();
    private final VocabularyRegistry vocabularyRegistry = new VocabularyRegistry();

    public static  DialectRegistry getInstance() {
        return INSTANCE;
    }

    private DialectRegistry() {
        addDialect(
            new DialectInfoBuilder(DIALECT_2020_12)
                    .addVocabulary(DRAFT_2020_12_CORE)
                    .addVocabulary(DRAFT_2020_12_APPLICATOR)
                    .addVocabulary(DRAFT_2020_12_UNEVALUATED)
                    .addVocabulary(DRAFT_2020_12_VALIDATION)
                    .addVocabulary(DRAFT_2020_12_FORMAT_ANNOTATION)
                    .addVocabulary(DRAFT_2020_12_META_DATA)
                    .addVocabulary(DRAFT_2020_12_CONTENT)
                    .forEachVocabulary(vocabularyRegistry::addVocabulary)
                    .build()
        );

        vocabularyRegistry.addVocabulary(new Vocabulary(DRAFT_2020_12_FORMAT_ASSERTION));

        addDialect(
            new DialectInfoBuilder(DIALECT_2019_09)
                .addVocabulary(DRAFT_2019_09_CORE)
                .addVocabulary(DRAFT_2019_09_APPLICATOR)
                .addVocabulary(DRAFT_2019_09_VALIDATION)
                .addVocabulary(DRAFT_2019_09_FORMAT)
                .addVocabulary(DRAFT_2019_09_META_DATA)
                .addVocabulary(DRAFT_2019_09_CONTENT)
                .forEachVocabulary(vocabularyRegistry::addVocabulary)
                .build()
        );
    }

    @Nullable
    Vocabulary optVocabulary(URI uri) {
        return vocabularyRegistry.optVocabulary(uri);
    }

    private DialectRegistry addDialect(DialectInfo dialectInfo) {
        this.predefined.put(dialectInfo.getUri(), dialectInfo);
        return this;
    }

    @Nullable DialectInfo optDialect(URI dialectUri) {
        DialectInfo info = predefined.get(dialectUri);
        if(info != null) {
            return info.copy();
        }
        return null;
    }

    private static class DialectInfoBuilder {
        private final URI dialectUri;
        private final List<Vocabulary> vocabularies = new ArrayList<>();

        public DialectInfoBuilder(URI dialectUri) {
            this.dialectUri = dialectUri;
        }

        private DialectInfoBuilder addVocabulary(URI vocabulary) {
            vocabularies.add(new Vocabulary(vocabulary));
            return this;
        }

        private DialectInfoBuilder forEachVocabulary(Consumer<Vocabulary> cons) {
            vocabularies.forEach(cons);
            return this;
        }

        private DialectInfo build() {
            var dialectInfo = new DialectInfo(dialectUri);
            vocabularies.forEach(voc -> dialectInfo.addVocabulary(voc.getUri(), true));
            return dialectInfo;
        }
    }

}
