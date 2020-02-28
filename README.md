# Vaultaire [![Maven Central](https://img.shields.io/maven-central/v/com.github.manosbatsis.vaultaire/vaultaire.svg)](https://repo1.maven.org/maven2/com/github/manosbatsis/vaultaire/vaultaire/) [![Build Status](https://travis-ci.org/manosbatsis/vaultaire.svg?branch=master)](https://travis-ci.org/manosbatsis/vaultaire)

Query DSL and data access utilities for Corda developers.   

See complete documentation at https://manosbatsis.github.io/vaultaire

## Query DSL

Use DSL will make your queries much easier to read and maintain. 
Each query DSL is automatically generated and maintaind for you 
based on your states.


```kotlin
al query = booksQuery {
    and {
    	// Check publisher?
        if(checkPublisher) fields.publisher `==` "Corda Books Ltd."
        or {
            fields.title  `==` "A book on Corda"
            fields.author `!=` "John Doe"
        }
    }
    orderBy {
        fields.title sort DESC
    }
}.toCriteria()
```

For more info on query DSL, checkout https://manosbatsis.github.io/vaultaire/query-dsl/

## State Services

Vaultaire's annotation processor  will automatically subclass `ExtendedStateService` to generate
an `Fields` aware state service service per annotated element. The generated service name
is "${contractStateTypeName}Service". Usage example:


```kotlin
// Create an instance of the generated service type
val bookStateService = BookStateService(
    serviceHub,        // Service hub or RPC ops
    serviceDefaults)   // Optional: criteria, paging, sort defaults

// Load a book
bookStateService.getByLinearId(identifier)
// Try finding one by ISBN
bookStateService.findByExternalId(identifier)

// query the vault for books
val searchResults = bookStateService.queryBy(
    criteria, paging, Pair("published", DESC), Pair("title", DESC))
```


For more info on state services, checkout https://manosbatsis.github.io/vaultaire/state-services/

## Other Utils

Vaultaire includes a few other utilities like annotations to generate Data Transfer Objects for 
your contract states and responding flows from commonly used supertypes.

For more info on these utilities, checkout https://manosbatsis.github.io/vaultaire/other-utils/