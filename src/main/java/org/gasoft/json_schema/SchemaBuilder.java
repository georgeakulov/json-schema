package org.gasoft.json_schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.gasoft.json_schema.common.JsonUtils;
import org.gasoft.json_schema.compilers.CompileConfig;
import org.gasoft.json_schema.compilers.Compiler;
import org.gasoft.json_schema.dialects.Defaults;
import org.gasoft.json_schema.loaders.ExternalResolversHelper;
import org.gasoft.json_schema.loaders.IResourceLoader;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Setup json schema compilation parameters and compile
 */
public class SchemaBuilder {

    private URI defaultDialect;
    private boolean formatEnabled = false;
    private boolean allowTreatAsArray = false;
    private ExternalResolversHelper externalSchemaResolver;
    private Scheduler scheduler;
    private final List<IResourceLoader> resourceLoaders = new ArrayList<>(1);
    private IRegexPredicateFactory regexPredicateFactory;

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

    public SchemaBuilder addResourceLoader(String schema, Function<URI, JsonNode> loader) {
        return addResourceLoader(new ResourceLoader(
                Objects.requireNonNull(schema, "The schema is null"),
                Objects.requireNonNull(loader, "The loader is null")
        ));
    }

    /**
     * Add an external subschemas resolver. This allows you to refine or override the location or
     * content of schemas based on the URI used in the schema.
     * @param externalResolver external resolver
     * @return this
     * @throws NullPointerException if any of arguments is null
     */
    public SchemaBuilder setSchemaResolver(IExternalResolver externalResolver) {
        withResolverHelper().addResolver(externalResolver);
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
        withResolverHelper().mapIdToSchema(id, schema);
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
        withResolverHelper().mapIdToSchema(id, schemaStr);
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
        withResolverHelper().mapIdToUri(id, schemaLocation);
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
        withResolverHelper().mapIdToUriAndSchema(id, schemaLocation, schema);
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
        withResolverHelper().mapIdToUriAndSchema(id, schemaLocation, schemaStr);
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

    private ExternalResolversHelper withResolverHelper() {
        if(externalSchemaResolver == null) {
            externalSchemaResolver = new ExternalResolversHelper();
        }
        return externalSchemaResolver;
    }

    /**
     * Compile the json schema with the previously set parameters
     * @param schema json schema
     * @return compiled schema
     * @throws NullPointerException is schema is null
     * @throws org.gasoft.json_schema.common.SchemaCompileException on compilation errors
     */
    public Schema compile(JsonNode schema) {
        Objects.requireNonNull(schema, "The schema is null");
        var validateFunc = new Compiler()
                .compileSchema(schema, defaultDialect, new CompileConfig()
                        .setExternalSchemaResolver(externalSchemaResolver)
                        .addResourceLoaders(resourceLoaders)
                        .setAllowTreatAsArray(allowTreatAsArray)
                        .setRegexpFactory(regexPredicateFactory)
                        .setScheduler(scheduler)
                        .setFormatEnabled(formatEnabled)
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
