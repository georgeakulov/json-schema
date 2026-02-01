package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class UnevaluatedItemsCompiler implements INamedCompiler, IValidatorsTransformer {

    private final static Set<String> EXPECTED_KEYWORDS = Set.of(
            "prefixItems", "items", "contains", "$ref", "$dynamicRef", "additionalItems", "$recursiveRef"
    );

    private final static Set<String> AWAITED_KEYWORDS = Stream.of(
                    EXPECTED_KEYWORDS,
                    List.of(
                            // Inplace
                            "allOf", "anyOf", "oneOf", "not", "if", "then", "else", "dependentSchemas"
                    )
            )
            .flatMap(Collection::stream).collect(Collectors.toSet());


    @Override
    public String getKeyword() {
        return "unevaluatedItems";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_UNEVALUATED,
                Defaults.DRAFT_2019_09_CORE
        );
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
        return compileContext.compile(schemaNode, schemaLocator);
    }

    @Override
    public void transform(Map<String, IValidatorAction> validators, CompileContext compileContext) {
        IValidatorAction current = validators.get(getKeyword());
        if (current == null) {
            return;
        }

        List<IValidatorAction> preferredActions = AWAITED_KEYWORDS.stream()
                .map(validators::remove)
                .filter(Objects::nonNull)
                .toList();

        validators.put(getKeyword(), new UnevaluatedItemsValidator(
                current,
                preferredActions,
                compileContext
        ));
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        mediator.process(pointer);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private static class UnevaluatedItemsValidator implements IValidator, IValidatorAction {

        private final IValidatorAction current;
        private final List<IValidatorAction> preferred;
        private final CompileContext compileContext;

        public UnevaluatedItemsValidator(IValidatorAction current, List<IValidatorAction> preferred, CompileContext compileContext) {
            this.current = current;
            this.preferred = preferred;
            this.compileContext = compileContext;
        }

        @Override
        public IValidator validator() {
            return this;
        }

        @Override
        public ICompileAction compileAction() {
            return current.compileAction();
        }

        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {
            var id = current.compileAction().createId(instancePtr);
            if(instance.isArray()) {
                return Flux.fromIterable(preferred)
                        .flatMap(action -> action.validator().validate(instance, instancePtr, context))
                        .subscribeOn(compileContext.getConfig().getScheduler())
                        .collectList()
                        .flatMapMany(list ->
                                Flux.fromIterable(list)
                                        .concatWith(validate(id, list, (ArrayNode)instance, instancePtr, context))
                        );
            }
            return ValidationResultFactory.createOk(id).publish();
        }

        private Publisher<? extends IValidationResult> validate(IValidationId id, List<IValidationResult> list,
                                                                ArrayNode instance, JsonPointer instancePtr,
                                                                IValidationContext context) {
            Set<Integer> evaluated = extractEvaluated(list, instancePtr);

            return Flux.fromStream(IntStream.range(0, instance.size())
                            .filter(idx -> !evaluated.contains(idx))
                            .boxed())
                    .parallel()
                    .flatMap(idx -> {
                            JsonPointer itemIdxPtr = instancePtr.appendIndex(idx);
                            return ValidationResultFactory.tryAppendAnnotation(
                                    () -> current.validator().validate(instance.get(idx), itemIdxPtr, context),
                                    ValidationResultFactory.createId(id.getSchemaLocator(), itemIdxPtr)
                            );
                    })
                    .sequential()
                    .reduce(
                            ValidationResultFactory.createContainer(id),
                            ValidationResultFactory.ValidationResultContainer::append
                    );
        }

        private Set<Integer> extractEvaluated(List<IValidationResult> list, JsonPointer instancePtr) {
            return list.stream()
                    .parallel()
                    .flatMap(IValidationResult::asStream)
                    .filter(vr -> vr.getType() == IValidationResult.Type.ANNOTATION)
                    .map(IValidationResult::getId)
                    .filter(id -> isChildOf(id, instancePtr))
                    .map(id -> id.getInstanceRef().last().getMatchingIndex())
                    .filter(idx -> idx >= 0)
                    .collect(Collectors.toSet());

        }

        private boolean isChildOf(IValidationResult.IValidationId id, JsonPointer pointer) {
            return id.getInstanceRef().head().equals(pointer);
        }
    }
}
