package org.gasoft.json_schema.compilers.v2019;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.*;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AdditionalItemsCompiler implements INamedCompiler, IValidatorsTransformer {

    @Override
    public String getKeyword() {
        return "additionalItems";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2019_09_APPLICATOR);
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
        return compileContext.compile(schemaNode, schemaLocator);
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void transform(Map<String, IValidatorAction> validators, CompileContext compileContext) {
        var current = validators.remove(getKeyword());
        if(current == null) {
            return;
        }

        var preferValidators = Stream.of("items")
                .map(validators::remove)
                .filter(Objects::nonNull)
                .toList();

        if(preferValidators.isEmpty()) {
            return;
        }

        validators.put(getKeyword(), new BaseFinisherValidator(
                current,
                preferValidators,
                JsonNode::isArray,
                this::finisher
        ));
    }

    private Publisher<? extends IValidationResult> finisher(
            IValidationId id,
            IValidator validator,
            List<IValidationResult> internalResults,
            JsonNode instance,
            JsonPointer instancePtr,
            IValidationContext context) {

        Set<Integer> proceed = BaseFinisherValidator.filterAnnotationsItems(internalResults, instancePtr);
        return Flux.fromStream(IntStream.range(0, instance.size())
                        .filter(idx -> !proceed.contains(idx))
                        .boxed())
                .parallel()
                .flatMap(idx -> {
                    JsonPointer itemIdxPtr = instancePtr.appendIndex(idx);
                    return ValidationResultFactory.tryAppendAnnotation(
                            () -> validator.validate(instance.get(idx), itemIdxPtr, context),
                            ValidationResultFactory.createId(id.getSchemaLocator(), itemIdxPtr)
                    );
                })
                .sequential()
                .reduce(
                        ValidationResultFactory.createContainer(id),
                        ValidationResultFactory.ValidationResultContainer::append
                );
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        if(node.isArray()) {
            IntStream.range(0, node.size())
                    .forEach(idx -> mediator.process(pointer.appendIndex(idx)));
        }
    }
}
