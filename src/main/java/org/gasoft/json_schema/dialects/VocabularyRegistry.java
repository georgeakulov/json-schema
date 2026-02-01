package org.gasoft.json_schema.dialects;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VocabularyRegistry {

    private final Map<URI, Vocabulary> vocabularies = new ConcurrentHashMap<>();

    VocabularyRegistry() {
    }

    void addVocabulary(Vocabulary vocabulary) {
        vocabularies.put(vocabulary.getUri(), vocabulary);
    }

    @Nullable Vocabulary optVocabulary(URI uri) {
        return vocabularies.get(uri);
    }
}
