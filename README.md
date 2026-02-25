# Json schema implementation

Here is another implementation of the JSON Schema Draft 2020-12, Draft 2019-09 specification. 
The main difference in this implementation is its support for multithreading, which significantly 
speeds up the processing of JSON that contains or consists of large arrays. 

<!-- TOC -->
* [Json schema implementation](#json-schema-implementation)
  * [Requirements](#requirements)
  * [How it works](#how-it-works)
  * [Features](#features)
  * [Usage](#usage)
    * [Repository placement and dependencies](#repository-placement-and-dependencies)
      * [Link this library](#link-this-library-)
      * [Library dependencies](#library-dependencies)
    * [Simple schema validation](#simple-schema-validation)
    * [Define own resource loaders](#define-own-resource-loaders)
    * [Resolve schema references to concrete URI or concrete schema](#resolve-schema-references-to-concrete-uri-or-concrete-schema)
    * [Resolving reference to uri](#resolving-reference-to-uri)
    * [Define custom format validators](#define-custom-format-validators)
    * [Content validation features](#content-validation-features)
  * [Define custom regular expression dialect](#define-custom-regular-expression-dialect)
  * [Concurrency](#concurrency)
  * [Limitations](#limitations)
<!-- TOC -->

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

### Repository placement and dependencies

#### Link this library 
```xml
        <dependency>
            <groupId>io.github.georgeakulov</groupId>
            <artifactId>json-schema</artifactId>
            <version>1.2.2</version>
        </dependency>
```
#### Library dependencies
```xml
    <dependencies>
        <!-- Parallelization -->
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-core</artifactId>
            <version>3.8.1</version>
        </dependency>
        <!-- Json support -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.20.1</version>
        </dependency>
    </dependencies>
```


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

### Define custom format validators
You can add custom or replace existing (default) format validators
```java
        String schema = """
                {
                    "$schema": "https://json-schema.org/draft/2020-12/schema",
                    "format": "thousandNumber"
                }
                """;
        Schema schemaInst = SchemaBuilder.create()
                .setDraft202012DefaultDialect()
                .setFormatAssertionsEnabled(true)
                .addFormatValidator("thousandNumber", str -> str.equals("1000"))
                .compile(schema);
        
        assertTrue(schemaInst.apply("\"1000\"").isOk());
        
        assertFalse(schemaInst.apply("\"1001\"").isOk());
```
### Content validation features
In the Draft7 specification, validation of contentEncoding and contentMediaType keywords is enabled by default. 
In contrast, in the Draft2019 and Draft2020 specifications, it is disabled. The following option has been added to control this behavior.

The library defines four levels of keyword processing behavior from the Content vocabulary:

| Level    | Draft7                   | Draft2019+                                                |
|----------|--------------------------|-----------------------------------------------------------|
| DISABLED | All validations disabled | All validations disabled                                  |
| DEFAULT  | All validations applied  | All validations disabled                                  |  
| ENCODING | All validations applied  | All validation applyed, but contentSchema not checked     |  
| ENCODING_AND_SCHEMA | All validations applied  | All validations are applied and contentSchema is checked. |

More explained in example:
```java
String schema = """
                {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "contentEncoding": "base64",
                "contentMediaType": "application/json",
                "contentSchema": {
                        "$schema": "https://json-schema.org/draft/2020-12/schema",
                        "type": "string"
                    }
                }
                """;
        String invalidEncoding = "\"MQ!==\""; // Symbol ! not allowed
        String validEncoding = "\"MQ==\""; // base64 encoded integer 1
        String validEncodingAndSchema = "\"IjEi\""; // base64 encoded string "1"

        // Behavior for disabled validations and invalid data
        IValidationResult result = SchemaBuilder.create()
                .setContentVocabularyBehavior(IContentProcessing.ContentValidationLevel.DISABLE)
                .compile(schema)
                .apply(validEncoding);
        // Validations do not apply 
        assertTrue(result.isOk());

        // Behavior for validating contentEncoding and contentMediaType
        Schema compiledSchema = SchemaBuilder.create()
                .setContentVocabularyBehavior(IContentProcessing.ContentValidationLevel.ENCODING)
                .compile(schema);

        // The base64 encoding is invalid
        result = compiledSchema.apply(invalidEncoding);
        assertFalse(result.isOk());

        // The base64 encoding is valid
        result = compiledSchema.apply(validEncoding);
        assertTrue(result.isOk());

        // Behavior for validating contentEncoding and contentMediaType and contentSchema too
        compiledSchema = SchemaBuilder.create()
                .setContentVocabularyBehavior(IContentProcessing.ContentValidationLevel.ENCODING_AND_SCHEMA)
                .compile(schema);

        // Valid base64 encoding but value not conform to contentSchema
        result = compiledSchema.apply(validEncoding);
        assertFalse(result.isOk());

        // Valid base64 encoding and  value is conform to contentSchema
        result = compiledSchema.apply(validEncodingAndSchema);
        assertTrue(result.isOk());
```
Builtin support for contentEncoding types: `base64`, `quoted-printable`, `7bit`.
Builtin support for json evaluable contentMediaType

Also you can add or redefined validators for contentEncoding and contentMediaType schema values. 

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
