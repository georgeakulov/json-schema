package io.github.georgeakulov.json_schema.compilers;

import io.github.georgeakulov.json_schema.Schema;
import io.github.georgeakulov.json_schema.SchemaBuilder;
import io.github.georgeakulov.json_schema.TestUtils.IFile;
import io.github.georgeakulov.json_schema.results.ValidationResultFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.georgeakulov.json_schema.compilers.JsonSchemaTestDataProvider.*;


@Execution(ExecutionMode.CONCURRENT)
public class JsonSchemaOrgTests {

    static final TestServer server = new TestServer();

    private static final List<ITestConfig> TEST_CONFIGS = List.of(
            new TestConfig("draft2019")
                    .setSchemaCustomizer((schema, builder) -> builder
                            .setDraft202009DefaultDialect()
                            .setFormatAssertionsEnabled(
                                    schema.testFile().rootRelativePath().toString().contains("optional/format")
                                            || schema.testFile().relativePath().toString().contains("optional/format-assertion.json")
                            )
                    ),
            new TestConfig("draft2020")
                    .setSchemaCustomizer((schema, builder) -> builder
                            .setDraft202012DefaultDialect()
                            .setFormatAssertionsEnabled(
                                    schema.testFile().rootRelativePath().toString().contains("optional/format")
                                            || schema.testFile().relativePath().toString().contains("optional/format-assertion.json")
                            )
                    ),
            new TestConfig("draft7")
                    .setSchemaCustomizer((schema, builder) ->
                        builder.setDraft07DefaultDialect()
                                .setFormatAssertionsEnabled(
                                        schema.testFile().rootRelativePath().toString().contains("optional/format")
                                                || schema.testFile().relativePath().toString().contains("optional/format-assertion.json")
                                )
                    )
    );

    @BeforeAll
    static void beforeAll() {
        server.upWithPath(1234, new File(System.getProperty("user.dir") + "/test_sources/remotes"));
    }

    @AfterAll
    static void afterAll() {
        server.down();
    }

    @TestFactory()
    Stream<DynamicNode> testDrafts() {
        return JsonSchemaTestDataProvider.getFilesHierarchy()
                .stream()
                .map(file -> {
                    var config = TEST_CONFIGS.stream()
                            .filter(cfg -> file.relativeToRoot().toString().startsWith(cfg.getDirectory()))
                            .findAny()
                            .orElse(null);
                    if(config != null) {
                        return forFile(testDirectory, file, config);
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    DynamicNode forFile(Path parent, IFile file, ITestConfig testConfig) {

        if(file.isDirectory()) {
            String relative = parent.relativize(file.path()).toString();
            if(!testConfig.filterDirectory(file)) {
                return null;
            }
            return DynamicContainer.dynamicContainer(
                    "\uD83D\uDCC1: " + relative,
                    file.childs().stream()
                            .sorted(Comparator.comparing(inFile -> inFile.path().getFileName().toString()))
                            .map(inFile -> forFile(file.path(), inFile, testConfig))
                            .filter(Objects::nonNull)
                            .toList()
            );
        }
        else {
            if(!testConfig.filterFile(file)) {
                return null;
            }
            JsonSchemaTestDataProvider.TestFile testFile = toTestFile(parent, file);
            return fileToTest(testFile, testConfig);
        }
    }

    DynamicNode fileToTest(TestFile testFile, ITestConfig testConfig) {
        return DynamicContainer.dynamicContainer(
                testFile.toString(),
                testFile.schemas()
                        .stream()
                        .filter(testConfig::filterSchema)
                        .map(schema -> {
                            try {
                                var builder = SchemaBuilder.create();
                                testConfig.customizeSchemaBuilder(schema, builder);
                                var compiledSchema = builder.compile(schema.schemaValue());

                                return DynamicContainer.dynamicContainer(
                                        schema.description(),
                                        schema.tests().stream()
                                                .filter(testConfig::filterTest)
                                                .map(test -> DynamicTest.dynamicTest(
                                                        test.description(),
                                                        toExecutable(compiledSchema, schema, test)
                                                ))
                                );
                            }
                            catch(Exception ex) {
                                return DynamicTest.dynamicTest(
                                        schema.description(),
                                        () -> Assertions.fail("Schema compilation error", ex)
                                );
                            }
                        })
        );
    }

    Executable toExecutable(Schema compiledSchema, JsonSchemaTestDataProvider.Schema schema, JsonSchemaTestDataProvider.Test test){
        return () -> {

            var result = compiledSchema.apply(test.value());

            Assertions.assertEquals(test.expected(), result.isOk(), () -> MessageFormat.format(
                "The schema \"{0}\" and test \"{1}\" has non expected result {2}, with msg: \"{3}\"",
                schema.description(),
                test.description(),
                result.isOk(),
                ValidationResultFactory.hierarchyFormat(result)
            ));
        };
    }

    interface ITestConfig {
        String getDirectory();
        default void customizeSchemaBuilder(JsonSchemaTestDataProvider.Schema schema, SchemaBuilder builder){}
        default boolean filterDirectory(IFile iFile) {
            return true;
        }
        default boolean filterFile(IFile iFile){
            return true;
        }
        default boolean filterSchema(JsonSchemaTestDataProvider.Schema schema) {
            return true;
        }
        default boolean filterTest(JsonSchemaTestDataProvider.Test test) {
            return true;
        }
    }

    static class TestConfig implements ITestConfig {
        private final String directory;
        private BiConsumer<JsonSchemaTestDataProvider.Schema, SchemaBuilder> schemaCustomizer = (ignoreSchema, ignore) -> {};
        private Predicate<IFile> directoryFilter = ignore -> true;
        private Predicate<IFile> fileFilter = ignore -> true;
        private Predicate<JsonSchemaTestDataProvider.Schema> schemaFilter = ignore -> true;
        private Predicate<JsonSchemaTestDataProvider.Test> testFilter = ignore -> true;

        @Override
        public String getDirectory() {
            return directory;
        }

        TestConfig(String directory) {
            this.directory = directory;
        }

        public TestConfig setSchemaCustomizer(BiConsumer<JsonSchemaTestDataProvider.Schema, SchemaBuilder> schemaCustomizer) {
            this.schemaCustomizer = schemaCustomizer;
            return this;
        }

        public TestConfig setDirectoryFilter(Predicate<IFile> directoryFilter) {
            this.directoryFilter = directoryFilter;
            return this;
        }

        public TestConfig setFileFilter(Predicate<IFile> fileFilter) {
            this.fileFilter = fileFilter;
            return this;
        }

        public TestConfig setSchemaFilter(Predicate<JsonSchemaTestDataProvider.Schema> schemaFilter) {
            this.schemaFilter = schemaFilter;
            return this;
        }

        public TestConfig setTestFilter(Predicate<JsonSchemaTestDataProvider.Test> testFilter) {
            this.testFilter = testFilter;
            return this;
        }

        @Override
        public void customizeSchemaBuilder(JsonSchemaTestDataProvider.Schema schema, SchemaBuilder builder) {
            schemaCustomizer.accept(schema, builder);
        }

        @Override
        public boolean filterDirectory(IFile iFile) {
            return directoryFilter.test(iFile);
        }

        @Override
        public boolean filterFile(IFile iFile) {
            return fileFilter.test(iFile);
        }

        @Override
        public boolean filterSchema(JsonSchemaTestDataProvider.Schema schema) {
            return schemaFilter.test(schema);
        }

        @Override
        public boolean filterTest(JsonSchemaTestDataProvider.Test test) {
            return testFilter.test(test);
        }
    }
}
