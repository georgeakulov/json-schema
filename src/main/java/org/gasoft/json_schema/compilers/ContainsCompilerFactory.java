package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.dialects.Vocabulary;
import org.gasoft.json_schema.results.EErrorType;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationError;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;
import static org.gasoft.json_schema.compilers.CompilerRegistry.VocabularySupport.of;

public class ContainsCompilerFactory implements ICompilerFactory {
    @Override
    public Stream<IVocabularySupport> getSupportedKeywords() {
        return Stream.of(
                of(Defaults.DRAFT_2020_12_APPLICATOR, "contains"),
                of(Defaults.DRAFT_2020_12_VALIDATION, "minContains", "maxContains"),
                of(Defaults.DRAFT_2019_09_APPLICATOR, "contains"),
                of(Defaults.DRAFT_2019_09_VALIDATION, "minContains", "maxContains")
        );
    }

    @Override
    public ICompiler getCompiler(String keyword, Vocabulary vocabulary) {
        return switch (keyword) {
            case "contains" -> new ContainsCompiler();
            case "minContains" -> new MinContainsCompiler();
            case "maxContains" -> new MaxContainsCompiler();
            default -> null;
        };
    }

    private static class ContainsCompiler implements ICompiler {

        private CompileConfig config;
        private ISchemaLocator schemaLocation;
        private IValidator validator;
        private @Nullable Integer minContains;
        private @Nullable Integer maxContains;

        @Override
        public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
            this.schemaLocation = schemaLocator;
            validator = compileContext.compile(schemaNode, schemaLocator);
            minContains = resolveDependentContainsParameters(compileContext, "minContains");
            maxContains = resolveDependentContainsParameters(compileContext, "maxContains");
            this.config = compileContext.getConfig();
            return this::validate;
        }

        private Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext validationContext) {

            var id = ValidationResultFactory.createId(schemaLocation, instancePtr);

            if(instance.isArray()) {
                return Flux.range(0, instance.size())
                        .flatMap(idx -> {
                            var itemPtr = instancePtr.appendIndex(idx);
                            return Flux.defer(() -> validator.validate(instance.get(idx), itemPtr, validationContext))
                                    .filter(IValidationResult::isOk)
                                    .map(res -> ValidationResultFactory.createAnnotation(schemaLocation, itemPtr));
                        })
                        .subscribeOn(config.getScheduler())
                        .collectList()
                        .map(allList -> {
                            int minContainsInt = minContains == null ? 1 : minContains;
                            var list = allList.stream()
                                    .filter(vr -> vr.getType() == IValidationResult.Type.ANNOTATION)
                                    .toList();
                            if(list.size() < minContainsInt) {
                                return ValidationError.create(id, EErrorType.CONTAINS_MIN, minContainsInt, list.size());
                            }
                            if(maxContains != null && list.size() > maxContains) {
                                return ValidationError.create(id, EErrorType.CONTAINS_MAX, maxContains, list.size());
                            }
                            return ValidationResultFactory.createContainer(id)
                                    .appendAll(list);
                        });
            }
            return ValidationResultFactory.createOk(id).publish();
        }

        @Override
        public void resolveCompilationOrder(List<ICompileAction> current, CompileContext compileContext, ISchemaLocator schemaLocator) {
            IntStream.range(0, current.size())
                    .filter(idx -> current.get(idx).keyword().equals("contains"))
                    .findAny()
                    .ifPresent(idx -> current.add(current.remove(idx)));
        }

        private Integer resolveDependentContainsParameters(CompileContext context, String keyword) {
            ICompiler compiler = context.optEvaluatedCompiler(keyword);
            if(compiler instanceof BaseContainsCompiler) {
                return ((BaseContainsCompiler)compiler).getValue();
            }
            return null;
        }

        @Override
        public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
            mediator.process(pointer);
        }
    }

    private static class MinContainsCompiler extends BaseContainsCompiler{
        @Override
        public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
            return super.compile(schemaNode, "minContains", schemaLocator);
        }
    }

    private static class MaxContainsCompiler extends BaseContainsCompiler{
        @Override
        public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
            return super.compile(schemaNode, "maxContains", schemaLocator);
        }
    }

    private abstract static class BaseContainsCompiler implements ICompiler {

        private int value;

        public IValidator compile(JsonNode schemaNode, String keyword, ISchemaLocator locator) {
            value = Utils.getCheckedInteger(locator, schemaNode, "The %s keyword value must be a non negative integer. Actual: %s", keyword, schemaNode);
            checkIt(value >= 0, locator,
                    "The {0} keyword value must be a non negative integer. Actual: {1}", keyword, schemaNode);
            return null;
        }

        @Override
        public boolean isSaveCompilerToCompileContext() {
            return true;
        }

        public int getValue() {
            return value;
        }
    }
}
