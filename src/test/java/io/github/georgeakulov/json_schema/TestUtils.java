package io.github.georgeakulov.json_schema;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TestUtils {

    static final JsonMapper mapper = new JsonMapper();

    public static List<Path> getPathsFromDir(Path folderPath, Predicate<Path> filter, int depth) {

        List<Path> paths = new ArrayList<>();
        getPathsFromDirCb(folderPath, depth, path -> {
            File file = path.toFile();
            if(!file.isDirectory() && filter.test(path)) {
                paths.add(path);
            }
        });
        return paths;
    }

    public static List<IFile> getPathsHierarchy(Path folderPath, Predicate<Path> fileFilter) {
        return getPathsHierarchy(folderPath, folderPath, fileFilter);
    }

    private static List<IFile> getPathsHierarchy(Path root, Path folderPath, Predicate<Path> fileFilter) {
        List<IFile> files = new ArrayList<>();
        getPathsFromDirCb(folderPath, 1, path -> {
            if(folderPath.equals(path)) {
                return;
            }
            File file = path.toFile();
            if(file.isDirectory()) {
                files.add(new IntDirectory(path, getPathsHierarchy(root, path, fileFilter), root.relativize(path)));
            }
            else {
                if(fileFilter.test(path)) {
                    files.add(new IntFile(path, root.relativize(path)));
                }
            }
        });
        return files;
    }

    public static void getPathsFromDirCb(Path folderPath, int depth, Consumer<Path> cb) {
        if(!folderPath.toFile().exists()) {
            throw new IllegalArgumentException("The path " + folderPath + " not exists");
        }

        try(Stream<Path> files = Files.find(
                folderPath,
                depth,
                (path, attrs) -> true)) {
            files.forEach(cb);
        }
        catch(IOException e) {
            throw new RuntimeException("Error on search files in folder: " + folderPath, e);
        }
    }

    public static JsonNode getResource(String path) {

        try(InputStream is = Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream(path), () -> "No resource " + path)) {
            return loadJson(is);
        }
        catch (IOException e) {
            throw new RuntimeException("Error load json: " + path, e);
        }
    }

    public static JsonNode loadJson(File file) {
        try (InputStream inStream = Files.newInputStream(file.toPath())) {
            return loadJson(inStream);
        }
        catch(IOException e) {
            throw new RuntimeException("Error load json: " + file, e);
        }
    }

    public static JsonNode loadJson(InputStream is) throws IOException{
        return mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true).reader().readTree(is);
    }

    public static JsonNode fromString(String str) {
        try {
            return mapper.reader().readTree(str);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public interface IFile {
        Path path();
        List<IFile> childs();
        Path relativeToRoot();
        boolean isDirectory();
    }

    record IntFile(Path path, Path relativeToRoot) implements IFile {
        @Override
        public List<IFile> childs() {
            return List.of();
        }

        @Override
        public boolean isDirectory() {
            return false;
        }
    }
    record IntDirectory(Path path, List<IFile> childs, Path relativeToRoot) implements IFile {
        @Override
        public boolean isDirectory() {
            return true;
        }
    }
}
