package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.TestUtils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class JsonSchemaTestDataProvider {

    static final Path testDirectory = Path.of(System.getProperty("user.dir") +"/test_sources/tests/");

    static Stream<TestFile> getTestFiles() {
        return TestUtils.getPathsFromDir(testDirectory, ignore -> true, Integer.MAX_VALUE)
                .stream()
                .map(path -> new TestFile(
                        path,
                        testDirectory.relativize(path),
                        toSchema(path).toList(),
                        testDirectory.relativize(path)
                ));
    }

    static TestFile toTestFile(Path parent, Path path) {
        return new TestFile(path, parent.relativize(path), toSchema(path).toList(), testDirectory.relativize(path));
    }

    static List<TestUtils.IFile> getFilesHierarchy() {
        return TestUtils.getPathsHierarchy(testDirectory, ignore -> true);
    }

    public static Stream<Schema> toSchema(Path path) {
        return TestUtils.loadJson(path.toFile())
                    .valueStream()
                    .map(schema -> {
                        var tests = schema.get("tests")
                                .valueStream()
                                .map(test -> new Test(
                                        test.get("description").asText(),
                                        test.get("data"),
                                        test.get("valid").asBoolean()
                                ))
                                .toList();
                        return new Schema(
                                schema.get("schema"),
                                schema.get("description").asText(),
                                tests
                        );
                    });
    }

    record Test(String description, JsonNode value, boolean expected){

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Test{");
            sb.append("description='").append(description).append('\'');
            sb.append(", expected=").append(expected);
            sb.append('}');
            return sb.toString();
        }
    }

    record Schema(JsonNode schemaValue, String description, Collection<Test> tests){

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Schema{");
            sb.append(", description='").append(description).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    record TestFile(Path file, Path relativePath, Collection<Schema> schemas, Path rootRelativePath) {
        @Override
        public String toString() {
            return relativePath.toString();
        }
    }

}
