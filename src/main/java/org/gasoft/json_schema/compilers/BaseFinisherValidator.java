package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.ICompiler.IValidatorAction;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseFinisherValidator implements IValidatorAction, IValidator {

    protected final IValidatorAction original;
    protected final List<IValidatorAction> dependent;
    private final Predicate<JsonNode> typeFilter;
    private final FinishValidationFunc finishValidationFunc;

    public interface FinishValidationFunc {
        Publisher<? extends IValidationResult> validate(
                IValidationId id,
                IValidator original,
                List<IValidationResult> prevValidationResult,
                JsonNode instance,
                JsonPointer instancePtr,
                IValidationContext context);
    }

    public BaseFinisherValidator(
            IValidatorAction original,
            List<IValidatorAction> dependent,
            Predicate<JsonNode> typeFilter,
            FinishValidationFunc finishValidationFunc) {
        this.original = original;
        this.dependent = dependent;
        this.typeFilter = typeFilter;
        this.finishValidationFunc = finishValidationFunc;
    }

    @Override
    public IValidator validator() {
        return this;
    }

    @Override
    public ICompiler.ICompileAction compileAction() {
        return original.compileAction();
    }

    @Override
    public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instancePtr, IValidationContext context) {
        var id = original.compileAction().createId(instancePtr);
        if(typeFilter.test(instance)) {
            return Flux.fromIterable(dependent)
                    .flatMap(validator -> validator.validator().validate(instance, instancePtr, context))
                    .collectList()
                    .flatMapMany(list ->
                            Flux.fromIterable(list)
                                    .concatWith(finishValidationFunc.validate(id, original.validator(), list, instance, instancePtr, context))
                    );
        }
        return ValidationResultFactory.createOk(id).publish();
    }

    public static Stream<IValidationId> preFilter(List<IValidationResult> validationResults, JsonPointer childOf) {
        return validationResults.stream()
                .parallel()
                .flatMap(IValidationResult::asStream)
                .filter(vr -> vr.getType() == IValidationResult.Type.ANNOTATION)
                .map(IValidationResult::getId)
                .filter(id -> isChildOf(id, childOf));
    }

    public static Set<String> filterAnnotationsProperty(List<IValidationResult> validationResults, JsonPointer childOf) {
        return preFilter(validationResults, childOf)
                .map(id -> id.getInstanceRef().last().getMatchingProperty())
                .collect(Collectors.toSet());
    }

    public static Set<Integer> filterAnnotationsItems(List<IValidationResult> validationResults, JsonPointer instancePtr) {
        return preFilter(validationResults, instancePtr)
                .map(id -> id.getInstanceRef().last().getMatchingIndex())
                .filter(idx -> idx >= 0)
                .collect(Collectors.toSet());
    }

    public static boolean isChildOf(IValidationId id, JsonPointer pointer) {
        return id.getInstanceRef().head().equals(pointer);
    }
}
