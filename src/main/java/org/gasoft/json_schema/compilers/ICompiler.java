package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.compilers.base.BaseSomeOfCompiler;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.gasoft.json_schema.results.IValidationResult.IValidationId;
import org.gasoft.json_schema.results.ValidationResultFactory;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface ICompiler {

    @Nullable
    IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator);

    default void resolveCompilationOrder(List<ICompileAction> current, CompileContext compileContext, ISchemaLocator schemaLocator) {}

    default boolean isSaveCompilerToCompileContext() {
        return false;
    }

    default void preprocess(BaseSomeOfCompiler.IPreprocessorMediator mediator, String keyword, JsonNode node, JsonPointer pointer){}


    interface ICompileAction {
        String keyword();
        JsonNode schemaNode();
        ICompiler compiler();
        ISchemaLocator locator();

        default IValidationId createId(JsonPointer ptr) {
            return ValidationResultFactory.createId(locator(), ptr);
        }
    }

    interface IValidatorAction {
        IValidator validator();
        ICompileAction compileAction();
    }


    interface IPreprocessorMediator {

        void process(JsonPointer pointer);
    }
}
