package org.gasoft.json_schema.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.common.SchemaCompileException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.SchemaCompileException.checkIt;

public class BaseResourceLoader implements IResourceLoader {

    private final List<IResourceLoader> loaders = new ArrayList<>();

    public BaseResourceLoader() {
        this
                .addLoader(new HttpLoader())
                .addLoader(new FileLoader())
                .addLoader(new ClasspathLoader());
    }

    public BaseResourceLoader addLoader(IResourceLoader loader) {
        loaders.addFirst(loader);
        return this;
    }

    @Override
    public Stream<String> getSupportedSchemes() {
        return loaders.stream()
                .flatMap(IResourceLoader::getSupportedSchemes);
    }

    @Override
    public JsonNode loadResource(URI byUri) {
        checkIt(byUri.isAbsolute(), "The uri %s is not absolute", byUri);
        String scheme = byUri.getScheme();
        return loaders.stream()
                .filter(loader -> loader.getSupportedSchemes().anyMatch(scheme::equals))
                .findFirst()
                .orElseThrow(() -> SchemaCompileException.create("Can`t find loader for schema {0} from uri {1}", scheme, byUri))
                .loadResource(byUri);
    }
}
