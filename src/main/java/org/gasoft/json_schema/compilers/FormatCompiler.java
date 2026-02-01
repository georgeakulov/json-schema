package org.gasoft.json_schema.compilers;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.common.CommonFormatValidations;
import org.gasoft.json_schema.common.DateTimeFormatValidation;
import org.gasoft.json_schema.common.email.EmailValidator;
import org.gasoft.json_schema.common.email.HostnameValidator;
import org.gasoft.json_schema.common.uritemplate.URITemplateParser;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.dialects.Dialect;
import org.gasoft.json_schema.results.IValidationResult;
import org.gasoft.json_schema.results.ValidationError;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.gasoft.json_schema.common.LocatedSchemaCompileException.checkIt;
import static org.gasoft.json_schema.common.LocatedSchemaCompileException.create;
import static org.gasoft.json_schema.results.EErrorType.FORMAT;
import static org.gasoft.json_schema.results.ValidationResultFactory.createId;

public class FormatCompiler implements INamedCompiler {

    @Override
    public String getKeyword() {
        return "format";
    }

    @Override
    public Stream<URI> getVocabularies() {
        return Stream.of(Defaults.DRAFT_2020_12_FORMAT_ANNOTATION, Defaults.DRAFT_2020_12_FORMAT_ASSERTION, Defaults.DRAFT_2019_09_FORMAT);
    }

    @Override
    public @Nullable IValidator compile(JsonNode schemaNode, CompileContext compileContext, IValidationResult.ISchemaLocator schemaLocator) {
        checkIt(schemaNode.isTextual(), schemaLocator, "The {0} keyword must be a string", getKeyword());

        Dialect dialect = compileContext.getDialect(schemaLocator);
        if(!(dialect.isAssertionRequired() || compileContext.getConfig().isFormatEnabled())) {
            return null;
        }

        Predicate<String> predicate = switch(schemaNode.textValue()) {
            case "date"             -> DateTimeFormatValidation::validateDate;
            case "time"             -> DateTimeFormatValidation::validateTime;
            case "date-time"        -> DateTimeFormatValidation::validateDateTime;
            case "duration"         -> DateTimeFormatValidation::validateDuration;
            case "regex"            -> str -> validateRegex(str, compileContext);
            case "uuid"             -> CommonFormatValidations.getUUIDFormatValidator();
            case "uri"              -> CommonFormatValidations.getURIValidator();
            case "uri-reference"    -> CommonFormatValidations.getURIReferenceValidator();
            case "ipv4"             -> CommonFormatValidations.getIpv4Validator();
            case "ipv6"             -> CommonFormatValidations.getIpv6Validator();
            case "uri-template"     -> URITemplateParser::parse;
            case "json-pointer"     -> CommonFormatValidations.getJsonPointerValidator();
            case "relative-json-pointer" -> CommonFormatValidations.getRelativeJsonPointerValidator();
            case "iri"              -> CommonFormatValidations.getIriValidator();
            case "iri-reference"    -> CommonFormatValidations.getIriReferenceValidator();
            case "email" , "idn-email" -> EmailValidator.getInstance(true, false)::isValid;
            case "hostname"         -> HostnameValidator.getHostNameValidator();
            case "idn-hostname"     -> HostnameValidator.getIDNAHostnameValidator();
            default -> {
                if(compileContext.getDialect(schemaLocator).isAssertionRequired()) {
                    throw create(schemaLocator, "The format {0} not supported", schemaNode);
                }
                yield null;
            }
        };
        if(predicate != null) {
            return new FormatValidator(schemaNode.textValue(), schemaLocator, predicate);
        }
        return null;
    }

    protected static class FormatValidator implements IValidator {

        private final String format;
        private final IValidationResult.ISchemaLocator locator;
        private final Predicate<String> validator;

        public FormatValidator(String format, IValidationResult.ISchemaLocator locator, Predicate<String> validator) {
            this.format = format;
            this.locator = locator;
            this.validator = validator;
        }

        @Override
        public Publisher<IValidationResult> validate(JsonNode instance, JsonPointer instanceLocation, IValidationContext context) {
            return Mono.just(createId(locator, instanceLocation))
                    .filter(id -> instance.isTextual())
                    .filter(id -> !validator.test(instance.textValue()))
                    .map(id -> ValidationError.create(id, FORMAT, instance, format));
        }
    }

    private static boolean validateRegex(String value, CompileContext ctxt) {
        try {
            ctxt.getConfig().getRegexpFactory().compile(value);
            return true;
        }
        catch(Exception e) {
            return false;
        }
    }
}
