package org.gasoft.json_schema.dialects;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

class DialectInfo {

    private final URI dialectUri;
    private final Map<URI, VocabularyState> vocabularies = new HashMap<>();

    DialectInfo(URI dialectUri) {
        this.dialectUri = dialectUri;
    }

    DialectInfo addVocabulary(URI vocabulary, boolean state) {
        this.vocabularies.put(vocabulary, new VocabularyState(vocabulary, state));
        return this;
    }

    URI getUri() {
        return dialectUri;
    }

    DialectInfo copy() {
        var result = new DialectInfo(dialectUri);
        result.vocabularies.putAll(vocabularies);
        return result;
    }

    Stream<VocabularyState> asStream() {
        return vocabularies.values().stream();
    }

    void clearVocabularies() {
        vocabularies.clear();
    }

    Optional<VocabularyState> getVocabularyState(URI uri) {
        return Optional.ofNullable(vocabularies.get(uri));
    }

    public record VocabularyState(URI vocabulary, boolean state){}
}
