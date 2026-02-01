package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.ICompiler.IValidatorAction;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.dialects.Vocabulary;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class IfThenElseCompilerFactory implements ICompilerFactory, IValidatorsTransformer {

    @Override
    public Stream<IVocabularySupport> getSupportedKeywords() {
        return Stream.of(
                CompilerRegistry.VocabularySupport.of(Defaults.DRAFT_2020_12_APPLICATOR, "if", "then", "else"),
                CompilerRegistry.VocabularySupport.of(Defaults.DRAFT_2019_09_APPLICATOR, "if", "then", "else")
        );
    }

    @Override
    public ICompiler getCompiler(String keyword, Vocabulary vocabulary) {
        return switch (keyword) {
            case "if", "then", "else" -> new CaseCompiler();
            default -> null;
        };
    }

    @Override
    public void transform(Map<String, IValidatorAction> validators, CompileContext compileContext) {
        var thenValidator = validators.remove("then");
        var elseValidator = validators.remove("else");
        var ifValidator = validators.remove("if");
        if(ifValidator != null) {
            validators.put("if", new ConditionalValidator(ifValidator, thenValidator, elseValidator));
        }
    }

    private static class ConditionalValidator implements IValidator, IValidatorAction {

        private final IValidatorAction conditionValidator;
        private final IValidatorAction thenValidator;
        private final IValidatorAction elseValidator;

        public ConditionalValidator(IValidatorAction conditionValidator, IValidatorAction thenValidator, IValidatorAction elseValidator) {
            this.conditionValidator = conditionValidator;
            this.thenValidator = thenValidator;
            this.elseValidator = elseValidator;
        }

        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instanceLocation, IValidationContext context) {
            return Flux.defer(() -> conditionValidator.validator().validate(instance, instanceLocation, context))
                    .flatMap(validationResult -> {
                        var id = ValidationResultFactory.createId(conditionValidator.compileAction().locator(), instanceLocation);
                        ValidationResultFactory.ValidationResultContainer container = ValidationResultFactory.createContainer(id);
                        validationResult.asStream()
                                .filter(vr -> vr.getType() == IValidationResult.Type.ANNOTATION)
                                .forEach(container::append);
                        Mono<@NonNull List<IValidationResult>> results = null;
                        if(validationResult.isOk()) {
                            if(thenValidator != null) {
                                results = Flux.from(thenValidator.validator().validate(instance, instanceLocation, context))
                                        .collectList();
                            }
                        }
                        else {
                            if(elseValidator != null) {
                                results = Flux.from(elseValidator.validator().validate(instance, instanceLocation, context))
                                        .collectList();
                            }
                        }
                        if(results != null) {
                            return Mono.from(results)
                                    .map(list -> {
                                        container.appendAll(list);
                                        return container;
                                    });
                        }
                        return container.publish();
                    });
        }

        @Override
        public IValidator validator() {
            return this;
        }

        @Override
        public ICompiler.ICompileAction compileAction() {
            return conditionValidator.compileAction();
        }
    }

    private static class CaseCompiler implements ICompiler {

        @Override
        public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
            return compileContext.compile(schemaNode, schemaLocator);
        }

        @Override
        public void preprocess(IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer) {
            mediator.process(pointer);
        }
    }
}
