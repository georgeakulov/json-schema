package org.gasoft.json_schema.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.common.JsonUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.SchemaCompileException.checkIt;
import static org.gasoft.json_schema.common.SchemaCompileException.create;

class FileLoader implements IResourceLoader {

    @Override
    public Stream<String> getSupportedSchemes() {
        return Stream.of("file");
    }

    @Override
    public JsonNode loadResource(URI byUri) {
        Path path = Path.of(byUri.getPath());
        File file = path.toFile();
        checkIt(file.exists(), "File {0} does not exist", file);
        checkIt(file.isFile(), "File {0} does not regular file", file);
        checkIt(file.canRead(), "File {0} can`t be read", file);

        try(var is = new BufferedInputStream(new FileInputStream(file))) {
            return JsonUtils.parse(is);
        }
        catch(Exception ex) {
            throw create(ex, "File {0} read error", file);
        }
    }
}
