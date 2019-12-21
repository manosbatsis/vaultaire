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

The above will automatically generate:


```kotlin
**
 * A basic responder to listen for finality
 */
@InitiatedBy(value = CreateBookFlow::class)
class CreateBookFlowResponder(
  otherPartySession: FlowSession
) : BaseBookFlowResponder(otherPartySession)

```