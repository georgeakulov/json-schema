package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.results.IValidationResult.ISchemaLocator;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class IdCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "$id";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(
                Defaults.DRAFT_2020_12_CORE,
                Defaults.DRAFT_2019_09_CORE
                );
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, ISchemaLocator schemaLocator) {
        return null;
    }

    @Override
    public void resolveCompilationOrder(List<ICompileAction> current, CompileContext compileContext, ISchemaLocator schemaLocator) {
        ICompileAction idAction = current.stream()
                .filter(action -> action.keyword().equals("$id"))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Should`t be happen"));

        String idValue = idAction.schemaNode().textValue();
        ISchemaLocator changeLocator = compileContext.resolveId(idValue, idAction.locator());
        if(changeLocator.getSchemaUUID().equals(schemaLocator.getSchemaUUID())) {
            return;
        }
        List<ICompileAction> updatedActions = current.stream()
                        .filter(action -> !action.keyword().equals("$id"))
                        .map(action -> {
                            if(!isSame(action.locator(), changeLocator)) {
                                return new ChangeLocatorAction(
                                        action,
                                        changeLocator
                                );
                            }
                            return action;

                        })
                        .toList();
        current.clear();
        current.addAll(updatedActions);
    }

    public static boolean isSame(ISchemaLocator before, ISchemaLocator after) {
        return Objects.equals(before.getSchemaUUID(), after.getSchemaUUID())
                && Objects.equals(before.getSchemaRef(), after.getSchemaRef());
    }

    private static class ChangeLocatorAction implements ICompileAction {

        private final ICompileAction action;
        private final ISchemaLocator updatedLocator;

        private ChangeLocatorAction(ICompileAction action, ISchemaLocator updatedLocator) {
            this.action = action;
            this.updatedLocator = updatedLocator;
        }

        @Override
        public String keyword() {
            return action.keyword();
        }

        @Override
        public JsonNode schemaNode() {
            return action.schemaNode();
        }

        @Override
        public ICompiler compiler() {
            return action.compiler();
        }

        @Override
        public ISchemaLocator locator() {
            return updatedLocator;
        }
    }
}
