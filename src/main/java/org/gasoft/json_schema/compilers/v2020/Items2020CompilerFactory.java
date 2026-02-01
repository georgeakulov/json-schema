package org.gasoft.json_schema.compilers.v2020;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.gasoft.json_schema.compilers.*;
import org.gasoft.json_schema.compilers.CompilerRegistry.VocabularySupport;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.dialects.Vocabulary;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Items2020CompilerFactory implements ICompilerFactory {

    @Override
    public Stream<IVocabularySupport> getSupportedKeywords() {
        return Stream.of(
                VocabularySupport.of(Defaults.DRAFT_2020_12_APPLICATOR, "items")
        );
    }

    @Override
    public ICompiler getCompiler(String keyword, Vocabulary vocabulary) {
        return new ItemsCompiler();
    }

    private static class ItemsCompiler implements ICompiler, IValidator {
        private ISchemaLocator locator;
        private CompileConfig config;
        private int prefixItemsCount;
        private IValidator validator;

        @Override
        public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
            this.locator = schemaLocator;
            this.config = compileContext.getConfig();
            this.validator = compileContext.compile(schemaNode, schemaLocator);
            prefixItemsCount = resolveMinIndexForValidate(compileContext);
            return this;
        }


        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {
            var id = ValidationResultFactory.createId(locator, instancePtr);

            var evaluated = ToArrayWrapper.tryWrap(instance, config);
            if(evaluated.isArray()) {

                Flux<Integer> range = evaluated.size() > prefixItemsCount
                        ? Flux.range(prefixItemsCount, evaluated.size() - prefixItemsCount)
                        : Flux.empty();
                return range
                        .flatMap(idx -> {
                            var idxPtr = instancePtr.appendIndex(idx);
                            return ValidationResultFactory.tryAppendAnnotation(
                                    () -> validator.validate(evaluated.get(idx), idxPtr, context),
                                    ValidationResultFactory.createId(locator, idxPtr)
                            );
                        })
                        .subscribeOn(config.getScheduler())
                        .reduce(
                                ValidationResultFactory.createContainer(id),
                                ValidationResultFactory.ValidationResultContainer::append
                        )
                        .map(val -> val);

            }
            return ValidationResultFactory.createOk(id).publish();
        }

        @Override
        public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
            mediator.process(pointer);
        }

        @Override
        public void resolveCompilationOrder(List<ICompileAction> current, CompileContext compileContext, ISchemaLocator schemaLocator) {
            IntStream.range(0, current.size())
                    .filter(i -> current.get(i).keyword().equals("items"))
                    .findAny()
                    .ifPresent(idx -> current.add(current.remove(idx)));
        }

        private int resolveMinIndexForValidate(CompileContext compileContext) {
            ICompiler compiler = compileContext.optEvaluatedCompiler("prefixItems");
            if(compiler instanceof PrefixItemsFactory.PrefixItemsCompiler) {
                return ((PrefixItemsFactory.PrefixItemsCompiler)compiler).getValidateItemsCount();
            }
            return 0;
        }
    }

    private static class ToArrayWrapper extends ArrayNode {

        public ToArrayWrapper(JsonNode node) {
            super(null);
            this.add(node);
        }

        private static JsonNode tryWrap(JsonNode node, CompileConfig config) {
            if(node.isArray()) {
                return node;
            }
            if(config.isAllowTreatAsArray()) {
                return new ToArrayWrapper(node);
            }
            return node;
        }
    }
}
