package org.gasoft.json_schema.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.common.JsonUtils;
import org.gasoft.json_schema.common.SchemaCompileException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.stream.Stream;

class ClasspathLoader implements IResourceLoader{

    @Override
    public Stream<String> getSupportedSchemes() {
        return Stream.of("classpath");
    }

    @Override
    public JsonNode loadResource(URI byUri) {
        try(InputStream is = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(byUri.getPath())) {
            return JsonUtils.parse(is);
        }
        catch(IOException e) {
            throw SchemaCompileException.create(e, "Error on load:{0}", byUri);
        }
    }
}
