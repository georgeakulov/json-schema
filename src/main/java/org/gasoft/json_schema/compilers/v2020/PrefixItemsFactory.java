package org.gasoft.json_schema.compilers.v2020;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.*;
import org.gasoft.json_schema.compilers.CompilerRegistry.VocabularySupport;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.dialects.Vocabulary;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;

public class PrefixItemsFactory implements ICompilerFactory {

    @Override
    public Stream<IVocabularySupport> getSupportedKeywords() {
        return Stream.of(
                VocabularySupport.of(Defaults.DRAFT_2020_12_APPLICATOR, "prefixItems")
        );
    }

    @Override
    public ICompiler getCompiler(String keyword, Vocabulary vocabulary) {
        return new PrefixItemsCompiler();
    }

    static class PrefixItemsCompiler implements ICompiler {

        private ISchemaLocator schemaLocation;
        private final List<IValidator> validators = new ArrayList<>();

        @Override
        public boolean isSaveCompilerToCompileContext() {
            return true;
        }

        @Override
        public IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
            this.schemaLocation = schemaLocator;
            checkIt(schemaNode.isArray() && !schemaNode.isEmpty(), schemaLocator,
                    "The {0} keyword value must be non empty array. Actual: {1}", "prefixItems", schemaNode.getNodeType());
            for (int idx = 0; idx < schemaNode.size(); idx++) {
                validators.add(compileContext.compile(schemaNode.get(idx), schemaLocator.appendIndex(idx)));
            }
            return this::validate;
        }

        public int getValidateItemsCount() {
            return validators.size();
        }

        private Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext validationContext) {
            var id = ValidationResultFactory.createId(schemaLocation, instancePtr);
            if(instance.isArray()) {

                return Flux.range(0, Math.min(instance.size(), validators.size()))
                        .flatMap(idx -> {
                            var itemPtr = instancePtr.appendIndex(idx);
                            return ValidationResultFactory.tryAppendAnnotation(
                                    () -> validators.get(idx).validate(instance.get(idx), itemPtr, validationContext),
                                    ValidationResultFactory.createId(schemaLocation, itemPtr)
                            );
                        })
                        .reduce(
                                ValidationResultFactory.createContainer(id),
                                ValidationResultFactory.ValidationResultContainer::append
                        )
                        .map(value -> value);
            }
            return ValidationResultFactory.createOk(id).publish();
        }

        @Override
        public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
            if(node.isArray()) {
                IntStream.range(0, node.size())
                        .forEach(idx -> mediator.process(pointer.appendIndex(idx)));
            }
        }
    }
}
