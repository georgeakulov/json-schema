package org.gasoft.json_schema.compilers;

import java.net.URI;
import java.util.stream.Stream;

public interface INamedCompiler extends ICompiler{

    String getKeyword();

    Stream<URI> getVocabularies();
}
