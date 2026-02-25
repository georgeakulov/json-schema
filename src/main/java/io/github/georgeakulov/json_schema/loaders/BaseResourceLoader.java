package io.github.georgeakulov.json_schema.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.georgeakulov.json_schema.common.SchemaCompileException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static io.github.georgeakulov.json_schema.common.SchemaCompileException.checkIt;

public class BaseResourceLoader implements IResourceLoader {

    private final static List<IResourceLoader> EMBED_LOADERS = new ArrayList<>();
    private final List<IResourceLoader> loaders = new ArrayList<>();

    private boolean useEmbedded = true;

    static {
        Stream.of(new HttpLoader(), new FileLoader(), new ClasspathLoader())
                        .forEach(EMBED_LOADERS::add);
    }

    public BaseResourceLoader() {
    }

    public BaseResourceLoader addLoader(IResourceLoader loader) {
        loaders.addFirst(loader);
        return this;
    }

    public void setUseEmbedded(boolean useEmbedded) {
        this.useEmbedded = useEmbedded;
    }

    @Override
    public Stream<String> getSupportedSchemes() {
        return Stream.of(loaders, getEmbeddedLoaders())
                .flatMap(List::stream)
                .flatMap(IResourceLoader::getSupportedSchemes);
    }

    private List<IResourceLoader> getEmbeddedLoaders() {
        return useEmbedded ? EMBED_LOADERS : Collections.emptyList();
    }

    @Override
    public JsonNode loadResource(URI byUri) {
        checkIt(byUri.isAbsolute(), "The uri %s is not absolute", byUri);
        String scheme = byUri.getScheme();
        return Stream.of(loaders, getEmbeddedLoaders())
                .flatMap(List::stream)
                .filter(loader -> loader.getSupportedSchemes().anyMatch(scheme::equals))
                .findFirst()
                .orElseThrow(() -> SchemaCompileException.create("Can`t find loader for schema {0} from uri {1}", scheme, byUri))
                .loadResource(byUri);
    }
}
