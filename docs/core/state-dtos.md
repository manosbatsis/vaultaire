# State DTOs

## Overview

Maintaining Data Transfer Objects for your contract states can be a mundane, error-prone task. 
Vaultaire’s annotation processing automates this by (re)generating those DTOs for you.

## Usage Patterns

A typical use for generated DTOs is messaging over HTTP REST or RPC as 
input and output of Corda Flows. The provided conversion utilities 
can be used to create, update or even patch `ContractState` types 
they correspond to.  
  

### DTO to State

To convert from DTO to state, use the DTO's `toTargetType()` method:

```kotlin
// Using default strategy
// ----------------------
// Get the DTO
val dto1: BookStateClientDto = //...
// Convert to State
val state1: BookState = dto1.toTargetType()

// Using the default strategy
// ----------------------
// Get the Service
val stateService: BookStateService = //...
// Get the 'client' DTO
val dto2: BookStateClientDto = // ...
// Convert to State
val state2: BookState = dto2.toTargetType(stateService)
```
### DTO as Patch Update

DTOs can be used to transfer and apply a "patch" to update
an existing state:

```kotlin
// Get the Service
val stateService = BookStateService(serviceHub_or_RPCOps)

// Load state from Node Vault
val state: BookState = stateService.getByLinearId(id)

// Apply DTO as patch
// ----------------------
val patchedState1: BookState = dto1.toPatched(state)

// Apply 'client' DTO as patch
// ----------------------
val patchedState2: BookState = dto2.toPatched(state, stateService)
```

### State to DTO

To convert from state to DTO, use the DTO's latter's alternative, state-based constructor:

```kotlin
// Get the state
val state: BookState = stateService.getByLinearId(id)
// Convert to DTO
val dto = BookStateClientDto.mapToDto(state)
```

## DTO Generation

This section explains the annotations and strategies involved in generating DTOs.

### Annotations

#### Local States

To have Vaultaire generate DTOs for `ContractState`s within local (Gradle module) sources,
annotate them with `@VaultaireStateDto`:

```kotlin
@VaultaireStateDto(
    // optional: properties to ignore
    ignoreProperties = ["foo"],
    // optional, default is false
    includeParticipants = false
)
data class BookState(
    val publisher: Party,
    val author: Party,
    val price: BigDecimal,
    val genre: Genre,
    @DefaultValue("1")
    val editions: Int = 1,
    val title: String = "Uknown",
    val published: Date = Date(),
    @field:JsonProperty("alias")
    val alternativeTitle: String? = null,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {/* ... */}
```

#### Dependency States

To generate DTOs for `ContractState`s outside the sources in context,
e.g. from a contract states module or a project dependency,
create a "mixin" class as a placeholder and annotate it with
`@VaultaireStateDtoMixin`.

This approach might be preferred or necessary even for state sources in context or under your control,
e.g. when having (the good practice of) separate cordapp modules for contracts/states and flows.

Mixin example:

```kotlin
@VaultaireStateDtoMixin(
    persistentStateType = PersistentBookState::class,
    contractStateType = BookState::class,
    // optional: properties to ignore
    ignoreProperties = ["foo"]
)
class BookStateMixin // just a placeholder for our annotation
```


#### Non-State Models

`@VaultaireModelDto` and `@VaultaireModelDtoMixin` cab be used for 
regular, non-ContractState data classes to similarly generate REST-friendly 
DTOs and conversion utils focused op Corda-related types like accounts etc.

```kotlin
@VaultaireModelDtoMixin(baseType = MagazineModel::class)
data class MagazineModelMixin
```

#### Utility Annotations

The`@DefaultValue` can be used to provide default property initializers. 
It can be used equally on either `ContractState` or "mixin" properties:


```kotlin
@VaultaireStateUtilsMixin(/*...*/)
@VaultaireStateDtoMixin(/*...*/)
data class MagazineMixin(
        @DefaultValue("1")
        var issues: Int,
        @DefaultValue("Date()")
        val published: Date,
        @DefaultValue("UniqueIdentifier()")
        val linearId: UniqueIdentifier
)
```
  

#### Sample DTO

In both cases above the following DTO will be generated.
Note the nullable var members and utilities to convert between the two types
or to patch an existing state:

```kotlin
/**
 * A [BookState]-specific [Dto] implementation
 */
@CordaSerializable
data class BookStateClientDto(
  var publisher: Party? = null,
  var author: Party? = null,
  var price: BigDecimal? = null,
  var genre: BookContract.Genre? = null,
  var editions: Int? = 1,
  var title: String? = null,
  var published: Date? = null,
  @field:JsonProperty(value = "alias")
  var alternativeTitle: String? = null,
  var linearId: UniqueIdentifier? = null
) : Dto<BookState> {
  /**
   * Alternative constructor, used to map 
   * from the given [BookState] instance.
   */
  constructor(original: BookState) : this(
        publisher = original.publisher,
        author = original.author,
        price = original.price,
        genre = original.genre,
        editions = original.editions,
        title = original.title,
        published = original.published,
        alternativeTitle = original.alternativeTitle,
        linearId = original.linearId
  )

  /**
   * Create a patched copy of the given [BookState] instance,
   * updated using this DTO's non-null properties.
   */
  override fun toPatched(original: BookState): BookState {
    val patched = BookState(
          publisher = this.publisher ?: original.publisher,
          author = this.author ?: original.author,
          price = this.price ?: original.price,
          genre = this.genre ?: original.genre,
          editions = this.editions ?: original.editions,
          title = this.title ?: original.title,
          published = this.published ?: original.published,
          alternativeTitle = this.alternativeTitle ?: original.alternativeTitle,
          linearId = this.linearId ?: original.linearId
    )
    return patched
  }

  /**
   * Create an instance of [BookState], using this DTO's properties.
   * May throw a [DtoInsufficientStateMappingException] 
   * if there is mot enough information to do so.
   */
  override fun toState(): BookState {
    try {
       val state = BookState(
          publisher = this.publisher!!,
          author = this.author!!,
          price = this.price!!,
          genre = this.genre!!,
          editions = this.editions!!,
          title = this.title!!,
          published = this.published!!,
          alternativeTitle = this.alternativeTitle,
          linearId = this.linearId!!
       )
       return state
    }
    catch(e: Exception) {
       throw DtoInsufficientStateMappingException(exception = e)
    }
  }
}

```


### Strategies

Both `@VaultaireStateDto` and `@VaultaireStateDtoMixin` support generation strategy hints.
By default the strategy used is the REST-friendly `VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO`. 
The only additional strategy provided is `VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO` that supports no 
type conversions. Using both, as in the following example, will generate separate DTOs for each.

```kotlin
@VaultaireStateDto(
    ignoreProperties = ["foo"],
    strategies = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO, VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO]
)
data class BookState(
		//...
) : LinearState, QueryableState{
		//...
}
```

The "client" strategy is provided to help where deserialization would normally require
either a `ServiceHub` or `RpcOps`, e.g. when the target property is a `Party`,
in which case the DTO will use a more manageable type like `CordaX500Name`,
and require a service to convert to or patch a `ContractState` instance.
Note that client DTO classnames will also have a `StateClientDto` suffix. Here's the "client"
DTO generated for the above example:

```kotlin
data class BookStateClientDto(
  var publisher: CordaX500Name? = null,
  var author: CordaX500Name? = null,
  var price: BigDecimal? = null,
  var genre: BookContract.Genre? = null,
  var editions: Int? = 1,
  var title: String? = null,
  var published: Date? = null,
  @field:JsonProperty(value = "alias")
  var alternativeTitle: String? = null,
  var linearId: UniqueIdentifier? = null
) : VaultaireDto<BookContract.BookState> {
  

  /**
   * Create a patched copy of the given [BookContract.BookState] instance,
   * updated using this DTO's non-null properties.
   */
  override fun toPatched(original: BookContract.BookState,
      stateService: StateService<BookContract.BookState>): BookContract.BookState {
    val patched = //... path the original, applying non-null values from this DTO
    return patched
  }

  /**
   * Create an instance of [BookContract.BookState], using this DTO's properties.
   * May throw a [DtoInsufficientStateMappingException]
   * if there is mot enough information to do so.
   */
  override fun toTargetType(stateService: StateService<BookContract.BookState>):
      BookContract.BookState {
    val originalTypeInstance = BookContract.BookState(
        // Use this DTO's values to create an new 
        // instance of the target type
    )
    return originalTypeInstance
  }
}
```
