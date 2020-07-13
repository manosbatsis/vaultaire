# Vaultaire [![Maven Central](https://img.shields.io/maven-central/v/com.github.manosbatsis.vaultaire/vaultaire.svg)](https://repo1.maven.org/maven2/com/github/manosbatsis/vaultaire/vaultaire/) [![Build Status](https://travis-ci.org/manosbatsis/vaultaire.svg?branch=master)](https://travis-ci.org/manosbatsis/vaultaire)

Query DSL and data access utilities for Corda developers.   

See complete documentation at https://manosbatsis.github.io/vaultaire

## Query DSL

Use DSL will make your queries much easier to read and maintain. 
Each query DSL is automatically (re)generated at build time using annotation
processing.

Usage example:

```kotlin
val queryCriteria: QueryCriteria = booksQuery {
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

For more details see [Query DSL](docs/query-dsl.md).

## State Services

Vaultaire's `StateService` interface provide a simple, consistent API to
load, query and track vault states.

`StateService` implementations are usually auto-generated at build time
and specific to a single `ContractState` type.

State Services can also decouple you code from `ServiceHub` and `CordaRPCOps`
amd help increase code reuse between cordapps and their clients.

For more details see [State Services](https://manosbatsis.github.io/vaultaire/query-dsl).

## Other Utils

Vaultaire includes a few other utilities like annotations to generate Data Transfer Objects for 
your contract states and responding flows from commonly used supertypes.

For more info on these utilities, checkout https://manosbatsis.github.io/vaultaire/other-utils/
