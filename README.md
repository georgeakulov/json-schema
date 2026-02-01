# Json schema implementation

Here is another implementation of the JSON Schema Draft 2020-12, Draft 2019-09 specification. 
The main difference in this implementation is its support for multithreading, which significantly 
speeds up the processing of JSON that contains or consists of large arrays. 


## Requirements
Java 21


## How it works

First, the JSON schema is compiled, and then the compiled schema can be used repeatedly and concurrently to validate JSON data.

During the schema compilation process, all \$ref and \$dynamicRef references are resolved, and all related schemas are loaded and compiled.

## Features

1. Reference resolution
   During compilation, the URIs referenced by all \$ref and \$dynamicRef are calculated. The schema referenced by this URI can then be forcibly redefined.

2. Defining the schema loader.
   By default, the simplest clients for loading URIs are implemented. Supported schemes: `http(s)`, `file`, `classpath`. 
   You can specify an external data loader and thus load data using schemes such as urn, ftp, ssh, etc.

## Usage

### Simple schema validation

```java
        String schema = """
                {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "type": "array",
                    "items": {
                        "type": "integer"
                    }
                }
                """;
        String json = "[1,2,3,4,5,6,7,8]";

        // For config schema compilation
        SchemaBuilder builder = SchemaBuilder.create();

        // Compiled schema. The schema is immutable.
        Schema compiledSchema = builder.compile(schema);

        // Validation result.
        IValidationResult result = compiledSchema.apply(json);
        assertTrue(result.isOk());

        // More simple fluent invocation
        result = SchemaBuilder.create()
                .compile(schema)
                .apply(json);
        
        assertTrue(result.isOk());
```

### Define own resource loaders
If the library does not support loading sub-schemas for certain types of identifiers, 
you can define your own loader.
```java
        String schema = """
                {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "type": "array",
                    "items": {
                        "$ref": "urn:uuid:fd823a01-2ef5-4091-b36a-a117ecfa8827"
                    }
                }
                """;
        String schemaRef = """
                {
                    "type": "integer"
                }
                """;
        JsonNode schemaRefJson = new JsonMapper().readTree(schemaRef);
        String json = "[1,2,3,4,5,6,7,8]";

        IValidationResult result = SchemaBuilder.create()
                .addResourceLoader("urn", uri -> schemaRefJson)
                .compile(schema)
                .apply(json);

        assertTrue(result.isOk());

```
### Resolve schema references to concrete URI or concrete schema
Supports the ability to force identifiers to resolve to absolute URIs and/or provide schemes 
for identifiers specified in the scheme.

Resolving a reference to a specific schema
```java

        String schema = """
                {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "type": "array",
                    "items": {
                        "$ref": "someIdentifier"
                    }
                }
                """;
        String schemaRef = """
                {
                    "type": "integer"
                }
                """;
        String json = "[1,2,3,4,5,6,7,8]";

        IValidationResult result = SchemaBuilder.create()
                .addMappingIdToSchema("someIdentifier", schemaRef)
                .compile(schema)
                .apply(json);

        assertTrue(result.isOk());
```

### Resolving reference to uri

Example of resolving a link to an absolute URI and subsequent loading using the added IResourceLoader
```java
        String schema = """
                {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "type": "array",
                    "items": {
                        "$ref": "someRef"
                    }
                }
                """;
        String schemaRef = """
                {
                    "type": "integer"
                }
                """;
        JsonNode schemaRefJson = new JsonMapper().readTree(schemaRef);
        String json = "[1,2,3,4,5,6,7,8]";

        // This can be real http or file link with really resource location
        URI middleUri = URI.create("urn:uuid:fd823a01-2ef5-4091-b36a-a117ecfa8827");
        IValidationResult result = SchemaBuilder.create()
                .addMappingIdToURI("someRef", middleUri)
                .addResourceLoader("urn", uri -> {
                    if(uri.equals(middleUri)) {
                        return schemaRefJson;
                    }
                    return null;
                })
                .compile(schema)
                .apply(json);

        assertTrue(result.isOk;
```

## Define custom regular expression dialect
This library does not support ECMA-262 regular expressions. By default, it uses the jdk 21 standard, 
which is closest to the PCRE standard. More information 
here [Pattern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html)

You can override the engine used by the regular expression engine using `IRegexPredicateFactory`.

## Concurrency
`SchemaBuilder` is not thread safe. However, `Schema` is immutable and thread safe.

By default, validation of any arrays occurs in parallel. The [io.reactor](https://projectreactor.io/) library is 
used for this purpose. Parallel computations are launched on java `VirtualThreadPerTaskExecutor` by default, 
but you can override this behavior by setting your own `Scheduler`. For example: `Schedulers.parallel()` or
`Schedulers.immediate()` for single thread execution.

## Limitations
1. Regex support does not comply with the ecma-262 standard. Java 21 regexp is used.
2. The output format does not yet fully comply with the specification requirements.
