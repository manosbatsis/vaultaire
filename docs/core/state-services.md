# State Services

## Overview

Vaultaire's `StateService`s provide a simple, consistent API to
load, query and track vault states.

`StateService` implementations are usually auto-generated at build time
and specific to a single `ContractState` type.

State Services can also decouple you code from `ServiceHub` and `CordaRPCOps`
by providing constructors for either, thus helping increase code reuse 
between cordapps and their clients. They also support RPC connection pooling, 
i.e. provide a constructor that accepts a [PoolBoy](https://manosbatsis.github.io/corda-rpc-poolboy/) 
`PoolBoyConnection`. 

## Basic Services

Basic implementations of `NodeService` and `StateService` are more limited and/or
less easy to use than generated ones but functional nevertheless:

```kotlin
val bookStateService = BasicStateService(
	serviceHubOrRpcOpsOrPoolBoy,
	BookContract.BookState::class.java)
```

## State Services

Vaultaire's annotation processor  will generate optimal `StateService` implementations,
each specific to one of the `ContractState`-`PersistentState` pairs found at build-time
based on the annotations found in your code. Service classnames are based on the contract state
classname, suffixed by "Service".

Usage example:

```kotlin
// Create an instance of the generated service type,
// passing a ServiceHub, CordaRPCOps, NodeRpcConnection
// or even a custom StateServiceDelegate
val bookStateService = BookStateService(serviceHub)

// Load a book
bookStateService.getByLinearId(identifier)

// Try finding one by UniqueIdentifier.externalId,
// in this case an ISBN
bookStateService.findByExternalId(isbn)

// Count the book states that match our query criteria
val booksCount = bookStateService.countBy(queryCriteria)

// Query the vault for a results page of the same books
val searchResultsPage: Vault.Page<BookState> = bookStateService.queryBy(
    queryCriteria,
    // Optional PageSpecification or page number/size params
    page,
    // Optional Sort or vararg of Pair<String, Sort.Direction>
    sort)

// Track the vault for book events
val trackResults = bookStateService.queryBy(criteria)
```


## Custom Services

You can also subclass (or, perhaps preferably, use as delegates) generated service types
or `BasicNodeService`, `BasicStateService`, `ExtendedStateService` etc.
to create custom  components.

```kotlin
/** Extend the generated [BookStateService] */
class MyExtendedBookStateService(
        delegate: StateServiceDelegate<BookState>
) : BookStateService(delegate){

    // Add the appropriate constructors
    // to initialize per delegate type:

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceRpcDelegate(rpcOps, BookState::class.java, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceHubDelegate(serviceHub, BookState::class.java, defaults))

    // Custom business methods...
}
```
