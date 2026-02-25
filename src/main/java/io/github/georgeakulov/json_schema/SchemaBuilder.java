package io.github.georgeakulov.json_schema;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.georgeakulov.json_schema.common.SchemaCompileException;
import io.github.georgeakulov.json_schema.IContentProcessing.ContentValidationLevel;
import io.github.georgeakulov.json_schema.common.JsonUtils;
import io.github.georgeakulov.json_schema.common.content.IContentValidationRegistry.ExceptionableCons;
import io.github.georgeakulov.json_schema.common.content.IContentValidationRegistry.ExceptionableOp;
import io.github.georgeakulov.json_schema.common.content.MimeType;
import io.github.georgeakulov.json_schema.common.content.SimpleContentValidationRegistry;
import io.github.georgeakulov.json_schema.compilers.CompileConfig;
import io.github.georgeakulov.json_schema.common.content.MimeTypeValidator;
import io.github.georgeakulov.json_schema.compilers.Compiler;
import io.github.georgeakulov.json_schema.dialects.Defaults;
import io.github.georgeakulov.json_schema.loaders.ExternalResolversHelper;
import io.github.georgeakulov.json_schema.loaders.IResourceLoader;
import io.github.georgeakulov.json_schema.loaders.OriginalSpecResolver;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Setup json schema compilation parameters and compile
 */
public class SchemaBuilder {

    private URI defaultDialect;
    private boolean formatEnabled = false;
    private boolean allowTreatAsArray = false;
    private final ExternalResolversHelper externalSchemaResolver = new ExternalResolversHelper();
    private Scheduler scheduler;
    private final List<IResourceLoader> resourceLoaders = new ArrayList<>(1);
    private IRegexPredicateFactory regexPredicateFactory;
    private final Map<String, Predicate<String>> formatValidators = new HashMap<>();
    private ContentValidationLevel contentValidationLevel = ContentValidationLevel.DEFAULT;
    private final SimpleContentValidationRegistry contentValidationRegistry = new SimpleContentValidationRegistry();
    private boolean allowEmbedResourceLoaders = true;
    private boolean allowOriginalSpecPreload = true;


    private SchemaBuilder() {
    }

    public static SchemaBuilder create() {
        return new SchemaBuilder();
    }

    /**
     * @param defaultDialect the uri representation of dialect schema
     * @return this
     * @throws NullPointerException is defaultDialect is null
     * @throws IllegalArgumentException is defaultDialect is wrong URI string
     *
     */
    public SchemaBuilder setDefaultDialect(String defaultDialect) {
        return setDefaultDialect(URI.create(defaultDialect));
    }

    /**
     * Setup Draft 2020-12 dialect as default
     * @return this
     */
    public SchemaBuilder setDraft202012DefaultDialect() {
        return setDefaultDialect(Defaults.DIALECT_2020_12);
    }

    /**
     * Setup Draft 2019-09 dialect as default
     * @return this
     */
    public SchemaBuilder setDraft202009DefaultDialect() {
        return setDefaultDialect(Defaults.DIALECT_2020_12);
    }

    public SchemaBuilder setDraft07DefaultDialect() {
        return setDefaultDialect(Defaults.DIALECT_07);
    }

    /**
     * @param defaultDialect default dialect uri
     * @return this
     * @throws NullPointerException if dialect is null or not absolute
     */
    public SchemaBuilder setDefaultDialect(URI defaultDialect) {
        Objects.requireNonNull(defaultDialect, "Dialect is null");
        this.defaultDialect = defaultDialect;
        return this;
    }

    /**
     * Add loaders for URI schemes. Loaders are selected sequentially in the order they are added.
     * This allows you to override the default loaders.
     * @param resourceLoader added resource loader
     * @return this
     * @throws NullPointerException if argument is null
     */
    public SchemaBuilder addResourceLoader(IResourceLoader resourceLoader) {
        Objects.requireNonNull(resourceLoader, "The resource loader is null");
        resourceLoaders.add(resourceLoader);
        return this;
    }

    /**
     * Add loaders for URI schemes. Loaders are selected sequentially in the order they are added.
     * This allows you to override the default loaders.
     * @param schema the name of shema
     * @param loader loader function
     * @return this
     * @throws NullPointerException if argument is null
     */

    public SchemaBuilder addResourceLoader(String schema, Function<URI, JsonNode> loader) {
        return addResourceLoader(new ResourceLoader(
                Objects.requireNonNull(schema, "The schema is null"),
                Objects.requireNonNull(loader, "The loader is null")
        ));
    }

    /**
     * Allow using embed resource loaders. Default: true
     * @param allow permission
     * @return this
     */
    public SchemaBuilder allowEmbedResourceLoaders(boolean allow) {
        this.allowEmbedResourceLoaders = allow;
        return this;
    }

    /**
     * Sometime json schema contains $ref to original specifications. This library already contains its specifications.
     * This flag allow use this specifications without loading from web. Default: true
     * @param allow permission
     * @return this
     */
    public SchemaBuilder allowEmbedOriginalSpec(boolean allow) {
        this.allowOriginalSpecPreload = allow;
        return this;
    }

    /**
     * Add an external subschemas resolver. This allows you to refine or override the location or
     * content of schemas based on the URI used in the schema.
     * @param externalResolver external resolver
     * @return this
     * @throws NullPointerException if any of arguments is null
     */
    public SchemaBuilder setSchemaResolver(IExternalResolver externalResolver) {
        externalSchemaResolver.addResolver(externalResolver);
        return this;
    }

    /**
     * Interpret the object and values as an array if required by the schema. Default false.
     * <b>experimental</b>
     * @return this
     */
    public SchemaBuilder setTryCastToArray(boolean tryCastToArray) {
        allowTreatAsArray = tryCastToArray;
        return this;
    }

    /**
     * Setup validation level of contentXXX keywords. Default: {@link  ContentValidationLevel#DEFAULT}
     *
     * @param contentValidationLevel content validation level.
     * @return this
     * @see ContentValidationLevel
     */
    public SchemaBuilder setContentVocabularyBehavior(ContentValidationLevel contentValidationLevel) {
        this.contentValidationLevel = contentValidationLevel;
        return this;
    }

    /**
     * Add custom or replace default contentEncoding keyword validator. The {@code validator} can throws Exception
     * on invalid validation and must return valid decoded instance value if contentMediaType validation is implied.
     * @param encodingType then content type name case-insensitive
     * @param validator the validator. The function receives encoded value and returns decoded value or throwing an Exception
     *                  if value encoding is invalid
     * @return this
     * @throws NullPointerException is any of arguments is null
     */
    public SchemaBuilder addContentEncodingValidator(String encodingType, ExceptionableOp validator) {
        Objects.requireNonNull(encodingType, "The encodingType is null");
        Objects.requireNonNull(validator, "The validator is null");
        contentValidationRegistry.addContentEncodingValidator(encodingType, validator);
        return this;
    }

    /**
     * Add custom or replace existing contentMediaType validator.
     * @param mimeTypePredicate predicate for selecting validator by mime type. More here {@link MimeType}
     * @param validator the consumer which must throwing an exception if validation is failed
     * @return this
     * @throws NullPointerException if any of arguments is null
     */
    public SchemaBuilder addContentMediaTypeValidator(Predicate<MimeType> mimeTypePredicate, ExceptionableCons validator) {
        Objects.requireNonNull(mimeTypePredicate, "The mime type predicate is null");
        Objects.requireNonNull(validator, "The validator is null");
        contentValidationRegistry.addContentTypeValidator(new MimeTypeValidator(mimeTypePredicate, validator));
        return this;
    }

    /**
     * Perform format assertions if  a dictionary that supports the $format keyword is defined.
     * May cause a compilation error if a format assertion dictionary is selected and this property is disabled.
     * Disabled by default.
     * @return this
     */
    public SchemaBuilder setFormatAssertionsEnabled(boolean forceAssertions) {
        formatEnabled = forceAssertions;
        return this;
    }

    /**
     * Add mapping id to resource. {@link IExternalResolver}
     * @param id mapping id
     * @param schema mapped json schema
     * @return this
     * @throws NullPointerException if any of arguments is null
     */
    public SchemaBuilder addMappingIdToSchema(String id, JsonNode schema) {
        externalSchemaResolver.mapIdToSchema(id, schema);
        return this;
    }

    /**
     * Add mapping id to resource. {@link IExternalResolver}
     * @param id mapping id
     * @param schemaStr mapped json schema string representation
     * @return this
     * @throws NullPointerException if any of arguments is null
     * @throws IllegalArgumentException if {@code schemaStr} is not valid json
     */
    public SchemaBuilder addMappingIdToSchema(String id, String schemaStr) {
        externalSchemaResolver.mapIdToSchema(id, schemaStr);
        return this;
    }

    /**
     * Resolve id to URI. {@link IExternalResolver}
     * @param id mapping id
     * @param schemaLocation resolved URI
     * @return this
     * @throws NullPointerException if any of arguments is null
     */
    public SchemaBuilder addMappingIdToURI(String id, URI schemaLocation) {
        externalSchemaResolver.mapIdToUri(id, schemaLocation);
        return this;
    }

    /**
     * Resolve id to URI and schema. {@link IExternalResolver} <br/>
     * This makes sense when resolving relative link identifiers embedded in this schema.
     * @param id mapping id
     * @param schemaLocation resolved URI
     * @param schema the resolved schema
     * @return this
     * @throws NullPointerException if any of arguments is null
     */
    public SchemaBuilder addMappingIdToUriAndSchema(String id, URI schemaLocation, JsonNode schema) {
        externalSchemaResolver.mapIdToUriAndSchema(id, schemaLocation, schema);
        return this;
    }

    /**
     * Resolve id to URI and schema. {@link IExternalResolver}<br/>
     * This makes sense when resolving relative link identifiers embedded in this schema.
     * @param id mapping id
     * @param schemaLocation resolved URI
     * @param schemaStr the resolved schema string representation
     * @return this
     * @throws NullPointerException if any of arguments is null
     * @throws IllegalArgumentException if {@code schemaStr} is not valid json
     */
    public SchemaBuilder addMappingIdToUriAndSchema(String id, URI schemaLocation, String schemaStr) {
        externalSchemaResolver.mapIdToUriAndSchema(id, schemaLocation, schemaStr);
        return this;
    }

    /**
     * Set custom regular expression factory
     * @param regexpPredicateFactory the realization of {@link IRegexPredicateFactory}
     * @return this
     * @throws NullPointerException if {@code regexpPredicateFactory} is null
     */
    public SchemaBuilder setRegexPredicateFactory(IRegexPredicateFactory regexpPredicateFactory) {
        Objects.requireNonNull(regexpPredicateFactory, "The regexpPredicateFactory is null");
        this.regexPredicateFactory = regexpPredicateFactory;
        return this;
    }

    /**
     * Add custom or replace existing format validator
     * @param formatName format name
     * @param formatValidator format validator
     * @return this
     * @throws NullPointerException if any or arguments is null
     */
    public SchemaBuilder addFormatValidator(String formatName, Predicate<String> formatValidator) {
        Objects.requireNonNull(formatName, "The formatName is null");
        Objects.requireNonNull(formatValidator, "The formatValidator is null");
        this.formatValidators.put(formatName, formatValidator);
        return this;
    }

    /**
     * Mass add format validators.
     * @param formatValidators format validators
     * @return this
     * @throws  NullPointerException if {@code formatValidators} is null or any key or value at this map is null
     */
    public SchemaBuilder addFormatValidators(Map<String, Predicate<String>> formatValidators) {
        Objects.requireNonNull(formatValidators, "The formatValidators is null");
        formatValidators.forEach(this::addFormatValidator);
        return this;
    }

    /**
     * Set scheduler for parallel validation
     * @param scheduler scheduler
     * @return this
     * @throws NullPointerException if {@code scheduler} is null
     */
    public SchemaBuilder setScheduler(Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "The scheduler is null");
        this.scheduler = scheduler;
        return this;
    }


    /**
     * Set Executor service for parallel validation
     * @param executorService the executor service
     * @return this
     * @throws NullPointerException if {@code executorService} is null
     */
    public SchemaBuilder setExecutorService(ExecutorService executorService) {
        Objects.requireNonNull(executorService, "The executorService is null");
        return setScheduler(Schedulers.fromExecutorService(executorService));
    }

    /**
     * Compile the json schema with the previously set parameters
     * @param schema json schema
     * @return compiled schema
     * @throws NullPointerException is schema is null
     * @throws SchemaCompileException on compilation errors
     */
    public Schema compile(JsonNode schema) {
        Objects.requireNonNull(schema, "The schema is null");
        if(allowOriginalSpecPreload) {
            externalSchemaResolver.addResolver(new OriginalSpecResolver());
        }
        var validateFunc = new Compiler()
                .compileSchema(schema, defaultDialect, new CompileConfig()
                        .setExternalSchemaResolver(externalSchemaResolver)
                        .addResourceLoaders(resourceLoaders)
                        .allowEmbedResourceLoaders(allowEmbedResourceLoaders)
                        .setAllowTreatAsArray(allowTreatAsArray)
                        .setRegexpFactory(regexPredicateFactory)
                        .setScheduler(scheduler)
                        .setFormatEnabled(formatEnabled)
                        .setContentValidationLevel(contentValidationLevel)
                        .addFirstContentValidationRegistry(this.contentValidationRegistry)
                        .addFormatValidators(this.formatValidators)
                );
        return new Schema(validateFunc);
    }

    /**
     * Compile the json schema string representation with the previously set parameters
     * @param schemaString json schema string representation
     * @return compiled schema
     * @throws NullPointerException if {@code schemaString} is null
     * @throws IllegalArgumentException if {@code schemaString} is not valid json
     *
     */
    public Schema compile(String schemaString) {
        Objects.requireNonNull(schemaString, "The schema string is null");
        return compile(JsonUtils.parse(schemaString));
    }

    private record ResourceLoader(String schema, Function<URI, JsonNode> loader) implements IResourceLoader{
        @Override
        public Stream<String> getSupportedSchemes() {
            return Stream.of(schema);
        }

        @Override
        public JsonNode loadResource(URI byUri) {
            return loader.apply(byUri);
        }
    }
}
