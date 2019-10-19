## State Services

Vaultaire's state services provide a simple API for DAO-style loading, querying  
and event tracking of vault states. State services will help you decouple your  
your components from Corda's `ServiceHub` and `CordaRPCOps` and reuse them both 
inb and or out of a Corda node.

### Generated State Service

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

You can see the [basic state services API](/vaultaire/0.x/com.github.manosbatsis.vaultaire.dao/-basic-state-service/) 
for details. 

### Custom Services

You can also subclass `BasicStateService`, `ExtendedStateService` or even generated service types  
to create custom components.

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
