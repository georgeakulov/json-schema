package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.gasoft.json_schema.compilers.base.BasePropertiesCollectorValidator;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnevaluatedPropertiesCompiler implements INamedCompiler, IValidatorsTransformer {

    private final static Set<String> EXPECTED_KEYWORDS = Set.of(
            "properties", "patternProperties", "additionalProperties", "$ref", "$dynamicRef", "$recursiveRef"
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
        return "unevaluatedProperties";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_UNEVALUATED,
                Defaults.DRAFT_2019_09_APPLICATOR
        );
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
        return compileContext.compile(schemaNode, schemaLocator);
    }

    @Override
    public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
        mediator.process(pointer);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void transform(Map<String, IValidatorAction> validators, CompileContext compileContext) {

        IValidatorAction current = validators.get(getKeyword());
        if(current == null) {
            return;
        }

        List<IValidatorAction> preferredActions = AWAITED_KEYWORDS.stream()
                .map(validators::remove)
                .filter(Objects::nonNull)
                .toList();

        validators.put(getKeyword(), new UnevaluatedPropertiesValidator(
                current,
                preferredActions
        ));
    }

    private static class UnevaluatedPropertiesValidator extends BasePropertiesCollectorValidator implements IValidatorAction {

        public UnevaluatedPropertiesValidator(IValidatorAction current, List<IValidatorAction> preferred) {
            super(current, preferred);
        }

        @Override
        public IValidator validator() {
            return this;
        }

        @Override
        public ICompileAction compileAction() {
            return original.compileAction();
        }


        protected Publisher<IValidationResult> validate(
                IValidationId id,
                List<IValidationResult> list,
                ObjectNode instance,
                JsonPointer instancePtr,
                IValidationContext context) {

            Set<String> evaluated = extractEvaluatedFields(list, instancePtr);
            return Flux.fromStream(instance.propertyStream())
                    .filter(prop -> !evaluated.contains(prop.getKey()))
                    .parallel()
                    .flatMap(prop ->

                            Mono.from(original.validator()
                                            .validate(prop.getValue(), instancePtr.appendProperty(prop.getKey()), context))
                                            .map(vr -> {
                                                if(vr.isOk()) {
                                                    return ValidationResultFactory.createAnnotation(vr.getId());
                                                }
                                                return vr;
                                            })
                    )
                    .sequential()
                    .reduce(
                            ValidationResultFactory.createContainer(id),
                            ValidationResultFactory.ValidationResultContainer::append
                    )
                    .map(vr -> vr);
        }

        private Set<String> extractEvaluatedFields(List<IValidationResult> list, JsonPointer childOf) {
            return list.stream()
                    .flatMap(IValidationResult::asStream)
                    .filter(vr -> vr.getType() == IValidationResult.Type.ANNOTATION)
                    .map(IValidationResult::getId)
                    .filter(id -> isChildOf(id, childOf))
                    .map(id -> id.getInstanceRef().last().getMatchingProperty())
                    .collect(Collectors.toSet());
        }

        private boolean isChildOf(IValidationResult.IValidationId id, JsonPointer pointer) {
            return id.getInstanceRef().head().equals(pointer);
        }
    }
}
