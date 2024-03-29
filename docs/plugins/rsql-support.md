
# RSQL Plugin

Creating search endpoints in webapp is a tedious process, 
even with Vaultaire's query DSL. The main maintenance effort 
goes to mapping URL parameters to query criteria and operators. 
Why not evaluate those dynamically?

This plugin uses [jirutka/rsql-parser](https://github.com/jirutka/rsql-parser) 
to provide support for 
[RSQL](https://www.baeldung.com/rest-api-search-language-rsql-fiql) 
and [FIQL](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00) 
queries, which is great for REST:

- It's simple, compact and URL-friendly.
- There's no need to maintain endpoints as you add/modify state propperties/schemas.
- Supports complex queries with logical and comparison operators.
- Being able to store queries as strings allows to e.g. easily manage reports.

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
    // blah
    and {
        fields.title `like` "%Corda%"
        fields.price gte 12
    }
    // blah
}

bookStateService.queryBy(query.toCriteria(), query.toSort())
```

The equivalent using RSQL:


```kotlin
// Use the generated DSL to create query criteria
val query = bookStateService.buildQuery {
    // blah
}
// Use the RSQL extension function
.withRsql(
    "title=like=*Corda*;price>=12",
    // Optional, default is 
    // SimpleRsqlArgumentsConverter.Factory()
    converterFactory  
)

bookStateService.queryBy(query.toCriteria(), query.toSort())
```

If you're wondering about `converterFactory`, see the 
[Value Converters](#value-converters) section bellow.

For an actual endpoint example that supports both 
"regular" and RSQL approaches above have a look at 
[YoController.findPaged](https://github.com/manosbatsis/corbeans-yo-cordapp/blob/master/bootapp-webserver/src/main/kotlin/mypackage/server/yo/YoController.kt#L143) 
in the corbeans-yo-cordapp on Github.

### Operators

The plugin supports the following operators:

| Operator(s) | Examples | Description |
|----------|---------|-------------|
| `==`     | `propName==queryValue` | Performs an **equals** query. Returns all entries  where values in `propName` exactly equal *queryValue* |  
| `!=`     | `propName!=queryValue` | Performs a **not equals** query. Returns all entries  where values in `propName` do not equal *queryValue* |
| `=like=` | `propName=like=queryValue` | Performs an **like** query using `*` as a wildcard. Returns all entries  where values in `propName` are like *queryValue* |
| `=unlike=`, `=notlike=`, `=nonlike=` | `propName=unlike=queryValue` | Performs a **not like** query using `*` as a wildcard. Returns all entries  where values in `propName` are not like *queryValue* |
| `=in=`   | `propName=in=(valueA, valueB)` | Performs an **in** query. Returns all entries  where `propName` contains *valueA* OR *valueB* |
| `=out=`   | `propName=out=(valueA, valueB)` | Performs an **not in** query. Returns all entries  where `propName` contains *valueA* OR *valueB* |
| `<`, `=lt=` | `propName<queryValue`, `propName=lt=queryValue` | Performs a **lesser than** query. Returns all entries  where values in `propName` are lesser than *queryValue* |
| `<=`, `=le=` | `propName<=queryValue`, `propName=le=queryValue` | Performs a **lesser than or equal to** query. Returns all entries  where values in `propName` are lesser than or equal to *queryValue* |
| `<`, `=gt=` | `propName>queryValue`, `propName=gt=queryValue` | Performs a **greater than** query. Returns all entries  where values in `propName` are greater than *queryValue* |
| `>=`, `=ge=` | `propName>=queryValue`, `propName=ge=queryValue` | Performs a **greater than or equal** query. Returns all entries  where values in `propName` are equal to or greater than *queryValue* |
| `=null=`, `=isnull=` | `propName=null=true`, `propName=isnull=true` | Performs an **is null** or **is not null** query. Returns all entries  where values in `propName` are `null` or not, based on the boolean argument.

### Argument Converters

Since an RSQL query is basically a `String`, we need converters to 
transform criteria arguments to their intended type.

For that purpose, the second parameter of the `withRsql` extension function 
accepts an optional `RsqlArgumentsConverterFactory` instance, with  
`SimpleRsqlArgumentsConverter.Factory()` being the default.

You can create your own, custom `RsqlArgumentsConverter` 
and `RsqlArgumentsConverterFactory` types or use one already provided:

| Converter Class                                	| Nested Factory 	| Description                                                                                                              	|
|------------------------------------------------	|----------------	|--------------------------------------------------------------------------------------------------------------------------	|
| AbstractRsqlArgumentsConverter                 	| No             	| Abstract base implementation, (optionally) extend to create your custom converter                                        	|
| SimpleRsqlArgumentsConverter                   	| Yes            	| The default. Simple, extensible, dependency-free  implementation                                                                      	|
| ObjectMapperAdapterRsqlArgumentsConverter      	| Yes            	| An implementation suitable for applications that make use of Jackson. Used as adapter for an `ObjectMapper` instance.    	|
| ConversionServiceAdapterRsqlArgumentsConverter 	| Yes            	| An implementation suitable for applications that make use of Spring. Used as adapter for a `ConversionService` instance. 	|


