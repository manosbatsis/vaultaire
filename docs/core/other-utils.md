# Other Utilities

Miscellaneous utilities provides by Vaultaire 

## Generated Responders

Some times you might want to use the same responding flow with multiple initiating flows.
Since `@InitiatedBy` is not a repeatable annotation, the only option would be to subclass 
the same responder for each initiating flow, adding the appropriate `@InitiatedBy`.

Vaultaire's annotation processor can help you automate this using a `@VaultaireFlowResponder` instead of 
maintaning such responders manually. Usage example:


```kotlin
// or simply: @VaultaireFlowResponder(BaseBookFlowResponder::class) 
@VaultaireFlowResponder(
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
        MyStateClientDto
            .from(it.state.data, stateService)
    }
}
```

## Vaultaire JAR Attachment

In some cases you may want to attach Vaultaire's JAR to a 
Corda transaction when creating a new accounts-aware state. 
`VaultaireAttachmentService` is a Corda service 
to do just that:

```kotlin
// Obtain Vaultaire's JAR hash
val vaultaireJarAttachment = serviceHub
    .cordaService(VaultaireAttachmentService::class.java)
    .vaultaireSecureHash
// Attach JAR to TX
transactionBuilder.addAttachment(vaultaireJarAttachment)
```