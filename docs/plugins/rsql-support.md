
# RSQL Plugin

Creating search endpoints in webapp is a tedious process, 
even with Vaultaire's query DSL. The main maintenance effort 
goes to mapping URL parameters to query criteria and operators. 
What if those could be evaluated dynamically?

This plugin provides support for 
[RSQL](https://www.baeldung.com/rest-api-search-language-rsql-fiql) 
and [FIQL](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00) 
queries. RSQL is ideal for describing a query in URLs, including 
both logical and comparison operators. The simplicity of RSQL and its 
capacity for complex queries in a compact, URL-friendly manner makes 
it a great generic query language for REST endpoints.

## Installation

The plugin is mainly intended for Corda client applications using 
Spring, Spring Boot or Jackson. It can however be used with other 
JVM frameworks and even within cordapps.

To use RSQL in your Corda client, add Vaultaire's plugin for RSQL 
to your Gradle build using an `api`, `implementation` or `compile` 
(deprecated) dependency.

```groovy
implementation "com.github.manosbatsis.vaultaire:vaultaire-plugin-rsql:$vaultaire_version"
```

> __Note__: To use the plugin in cordapps, follow the same rules 
> as with the main Vaiultaire dependency.

## Plugin Features

### Extension Function

The plugin introduces an extension function for generated DSL classes, 
i.e. subclasses of `VaultQueryCriteriaCondition`. Consider 
the following query DSL without RSQL:

```kotlin
// Use the generated DSL to create query criteria
val query = bookStateService.buildQuery {
    status = Vault.StateStatus.ALL
    and {
    	// Check publisher?
        if(checkPublisher) fields.publisher `==` "Corda Books Ltd."
        or {
            fields.title `like` "%Corda%"
            fields.price gte 12
        }
    }
    orderBy {
        fields.title sort DESC
    }
}

bookStateService.queryBy(query.toCriteria(), query.toSort())
```

The equivalent using RSQL:


```kotlin
// Use the generated DSL to create query criteria
val query = bookStateService.buildQuery {
    status = Vault.StateStatus.ALL
    orderBy {
        fields.title sort DESC
    }
}
// Use the RSQL extension function
.withRsql("title==*Corda*;price>=12", converterFactory)

bookStateService.queryBy(query.toCriteria(), query.toSort())
```

If you're wondering about `converterFactory`, see the 
[Value Converters](#value-converters) section bellow.

### Operators

The plugin supports the following operators:

| Operator | Example | Description |
|----------|---------|-------------|
| `==`     | `propName==queryValue` | Performs an **equals** or **like** query (using `*` as a wildcard). Returns all entries  where values in `propName` exactly equal *queryValue* |  
| `!=`     | `propName!=queryValue` | Performs a **not equals** or **not like** query (using `*` as a wildcard) query. Returns all entries  where values in `propName` do not equal *queryValue* |
| `=in=`   | `propName=in=(valueA, valueB)` | Performs an **in** query. Returns all entries  where `propName` contains *valueA* OR *valueB* |
| `=out=`   | `propName=out=(valueA, valueB)` | Performs an **not in** query. Returns all entries  where `propName` contains *valueA* OR *valueB* |
| `<` & `=lt=` | `propName<queryValue`, `propName=lt=queryValue` | Performs a **lesser than** query. Returns all entries  where values in `propName` are lesser than *queryValue* |
| `=le=` & `<=` | `propName<=queryValue`, `propName=le=queryValue` | Performs a **lesser than or equal to** query. Returns all entries  where values in `propName` are lesser than or equal to *queryValue* |
| `<` & `=gt=` | `propName>queryValue`, `propName=gt=queryValue` | Performs a **greater than** query. Returns all entries  where values in `propName` are greater than *queryValue* |
| `>=` & `=ge=` | `propName>=queryValue`, `propName=ge=queryValue` | Performs a **equal to or greater than** query. Returns all entries  where values in `propName` are equal to or greater than *queryValue* |
| `=null=`, `=isnull=` | `propName=null=`, `propName=isnull=` | Performs an **is null** query. Returns all entries  where values in `propName` are `null`
| `=notnull=`, `=nonnull=` | `propName=notnull=`, `propName=nonnull=` | Performs a **not null** query. Returns all entries  where values in `propName` are __not__ `null`


### Value Converters

Since an RSQL query is basically a `String`, we need converters to 
transform criteria arguments to their intended type.

For that purpose, the second parameter of the `withRsql` extension function 
accepts an `RsqlArgumentsConverterFactory` instance.

You can create your own, custom `RsqlArgumentsConverter` 
and `RsqlArgumentsConverterFactory` types or use one already provided:

| Converter Class                                	| Nested Factory 	| Description                                                                                                              	|
|------------------------------------------------	|----------------	|--------------------------------------------------------------------------------------------------------------------------	|
| AbstractRsqlArgumentsConverter                 	| No             	| Abstract base implementation, (optionally) extend to create your custom converter                                        	|
| SimpleRsqlArgumentsConverter                   	| Yes            	| Simple, extensible, dependency-free  implementation                                                                      	|
| ObjectMapperAdapterRsqlArgumentsConverter      	| Yes            	| An implementation suitable for applications that make use of Jackson. Used as adapter for an `ObjectMapper` instance.    	|
| ConversionServiceAdapterRsqlArgumentsConverter 	| Yes            	| An implementation suitable for applications that make use of Spring. Used as adapter for a `ConversionService` instance. 	|

