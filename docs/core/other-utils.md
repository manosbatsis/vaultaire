# Other Utilities

Miscellaneous utilities provides by Vaultaire 

## Generated Responders

Some times you might want to use the same responding flow with multiple initiating flows.
Since `@InitiatedBy` is not a repeatable annotation, the only option would be to subclass 
the same responder for each initiating flow, adding the appropriate `@InitiatedBy`.

Vaultaire's annotation processor can help you automate this using a `@VaultaireGenerateResponder` instead of 
maintaning such responders manually. Usage example:


```kotlin
// or simply: @VaultaireGenerateResponder(BaseBookFlowResponder::class) 
@VaultaireGenerateResponder(
    value = BaseBookFlowResponder::class,
    comment = "A basic responder to listen for finality"
)
@InitiatingFlow
@StartableByRPC
class CreateBookFlow(input: BookMessage) : FlowLogic<SignedTransaction>
```

The above will automatically generate a responder flow:


```kotlin
/**
 * A basic responder to listen for finality
 */
@InitiatedBy(value = CreateBookFlow::class)
class CreateBookFlowResponder(
  otherPartySession: FlowSession
) : BaseBookFlowResponder(otherPartySession)

```

__Note__: if the base responder flow is a final type, the generated responder will attempt to call it as a 
subflow instead of extending it:

```kotlin
@InitiatedBy(value = CreateBookFlow::class)
class CreateBookFlowResponder(
  val otherPartySession: FlowSession
) : FlowLogic<Unit>() {
  @Suspendable
  override fun call() {
    subFlow(BaseBookFlowResponder(otherPartySession))
  }
}
```

```

... or `@VaultaireGenerateDtoForDependency` when targetting a contract state within your dependencies
e.g. from your contract states module or a third party class:


```kotlin
@VaultaireGenerateDtoForDependency(
    persistentStateType = PersistentBookState::class,
    contractStateType = BookState::class,
    // optional: properties to ignore
    ignoreProperties = ["foo"]
)
class Dummy // just a placeholder for our annotation
```

In both cases the following DTO will be generated, along with proper implementations of [Dto]'s 
mapping/patching utility methods:

```kotlin
/**
 * A [BookState]-specific [Dto] implementation
 */
@CordaSerializable
data class BookStateDto(
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


## Results Page

The `ResultsPage` is a more REST-friendly alternative to `Vault.Page`. 
While it can be used to carry any type of results, it mainly focuses  
on mapping `StateAndRef` query results to either contract states or DTOs.

Sample use:

```kotlin
// Use a generated state service to query
val vaultPage: Vault.Page<MyState> = stateService
    .queryBy(criteria, pageSpecification, sort)

// As states
val statesPage = ResultsPage.from(
    vaultPage, pageSpecification, sort)

// As DTOs
val dtosPage = ResultsPage.from(
    vaultPage, pageSpecification, sort
) { stateAndRefs ->
    stateAndRefs.map {
        MyStateLiteDto
            .mapToDto(it.state.data, stateService)
    }
}
```