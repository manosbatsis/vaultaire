# Vaultaire [![Maven Central](https://img.shields.io/maven-central/v/com.github.manosbatsis.vaultaire/vaultaire.svg)](http://central.maven.org/maven2/com/github/manosbatsis/vaultaire/) [![Build Status](https://travis-ci.org/manosbatsis/vaultaire.svg?branch=master)](https://travis-ci.org/manosbatsis/vaultaire)

Query DSL and data access utilities for Corda developers.  
 If you haven''t already, see [Installation](#installation). 

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

### State Services

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

