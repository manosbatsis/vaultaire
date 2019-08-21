# Vaultaire [![Maven Central](https://img.shields.io/maven-central/v/com.github.manosbatsis.vaultaire/vaultaire.svg)](http://central.maven.org/maven2/com/github/manosbatsis/vaultaire/) [![Build Status](https://travis-ci.org/manosbatsis/vaultaire.svg?branch=master)](https://travis-ci.org/manosbatsis/vaultaire)

Query DSL and data access utilities for Corda developers.

<!-- TOC depthFrom:2 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Installation](#installation)
- [Query DSL](#query-dsl)
	- [Quick Example](#quick-example)
		- [Without Vaultaire](#without-vaultaire)
		- [With Vaultaire DSL](#with-vaultaire-dsl)
	- [Create a DSL](#create-a-dsl)
	- [Query Settings](#query-settings)
	- [Adding Criteria](#adding-criteria)
		- [Accessing Fields](#accessing-fields)
	- [Functions and Operators](#functions-and-operators)
	- [Sorting](#sorting)
- [State Services](#state-services)
	- [Generated State Service](#generated-state-service)
	- [Custom Services](#custom-services)

<!-- /TOC -->

## Installation

Add to your Cordapp's Gradle dependencies:

```groovy
    // Core dependency
    cordaCompile "com.github.manosbatsis.vaultaire:vaultaire:$vaultaire_version"
    // Annotation processing
    kapt "com.github.manosbatsis.vaultaire:vaultaire-processor:$vaultaire_version"

    // Corda dependencies etc.
    // ...
```

The core module can also be useful outside a cordapp, e.g. in a Spring application
interacting with Corda nodes via RPC:

```groovy
    // Core dependency
    compile "com.github.manosbatsis.vaultaire:vaultaire:$vaultaire_version"

    // Corda dependencies etc.
    // ...
```


## Query DSL

Vaultaire uses an annotation processor at build-time to create a query DSL for your contract/persistent states.

### Quick Example

Consider the following Contract/Persistent States:

```kotlin
/** Book ContractState */
data class BookState(
    val publisher: Party,
    val author: Party,
    val title: String,
    val published: Date = Date()
) : ContractState, QueryableState {
     // ...
}


// Use Vaultaire's code generation!
@VaultaireGenerate(name = "booksQuery", constractStateType = BookState::class)
@Entity
@Table(name = "books")
data class PersistentBookState(
    @Column(name = "publisher")
    var publisher: String = "",
    @Column(name = "author")
    var author: String = "",
    @Column(name = "title")
    var title: String = "",
    @Column(name = "published")
    var published: Date
) : PersistentState()
```

#### Without Vaultaire

_Before_ Vaultaire, you probably had to create query criteria with something like:

```kotlin
val query = VaultQueryCriteria(
        contractStateTypes = setOf(BookState::class.java),
        status = Vault.StateStatus.ALL
    )
    .and(VaultCustomQueryCriteria(PersistentBookState::publisher.equal("Corda Books Ltd.")))
    .and(VaultCustomQueryCriteria(PersistentBookState::title.equal("A book on Corda"))
        .or(VaultCustomQueryCriteria(PersistentBookState::author.notEqual("John Doe"))))

val sort = Sort(listOf(Sort.SortColumn(
    SortAttribute.Custom(PersistentBookState::class.java, "published"), Sort.Direction.DESC)))

queryBy(query, sort)
```
#### With Vaultaire DSL

With Vaultaire's `@VaultaireGenerate` and the generated DSL this becomes:

```kotlin
// Use the generated DSL to create query criteria
val query = booksQuery {
    status = Vault.StateStatus.ALL
    and {
        fields.publisher `==` "Corda Books Ltd."
        or {
            fields.title  `==` "A book on Corda"
            fields.author `!=` "John Doe"
        }
    }
    orderBy {
        fields.title sort DESC
    }
}.toCriteria()

queryBy(query.toCriteria(), query.toSort())

```

### Create a DSL

To create a query DSL for your state after [installing](#installation) Vaultaire, annotate the
corresponding `PersistentState` with `@VaultaireGenerate`:

```kotlin
// Use Vaultaire's DSL generation!
@VaultaireGenerate(
  // If you omit the name, the DSL function will be named by appending "Query"
  // to the decapitalized contract state name, e.g. "bookStateQuery"
  name = "booksQuery",
  constractStateType = BookState::class)
@Entity
@Table(name = "books")
data class PersistentBookState(
    // state properties...
) : PersistentState()
```

### Query Settings

The generated DSL allows setting `QueryCriteria.VaultQueryCriteria` members. Here's an example
using the defaults:

```kotlin
val query = booksQuery {
    // settings
    status = Vault.StateStatus.UNCONSUMED
    stateRefs = null
    notary = null
    softLockingCondition = null
    timeCondition = null
    relevancyStatus = Vault.RelevancyStatus.ALL
    constraintTypes = emptySet()
    constraints = emptySet()
    participants = null

    // criteria and sorting...
}
```

### Adding Criteria

Query riteria are defined within the `and` / `or` functions. Both functions can be nested and mixed
with criteria like:

```kotlin
val query = booksQuery {
    // settings...

    // criteria
    or { // Match at least one
        fields.foo1 `==` someValue
        and { // Match all
            fields.foo1.isNull()
            fields.foo2 `==` someOtherValue
            and {
                // ...
            }
            or {
                // ...
            }
        }
    }

    // sorting...
}
```

#### Accessing Fields

Fields can be accessed via the generated DSL's `fields` object within `and`, `or`, or `orderBy`
functions using dot notation e.g. `fields.foo`.

> You can also retrieve fields by name with e.g. `fields["foo"]` or  `fields.get("foo")` and use
non typesafe functions like `_equal`, `_notEqual`, `_like`, `_notLike` but this may change in a future release.

### Functions and Operators

<table>
  <tr>
    <th>Name</th>
    <th>Aliases</th>
    <th>Examples</th>
  </tr>
  <tr>
    <td>isNull</td>
    <td></td>
    <td>
        <code>fields.foo.isNull()</code></td>
  </tr>
  <tr>
    <td>notNull</td>
    <td></td>
    <td>
        <code>fields.foo.notNull()</code>
    </td>
  </tr>
  <tr>
    <td>equal</td>
    <td>`==`</td>
    <td>
        <code>fields.foo `==` bar</code><br>
        <code>fields.foo equal bar</code><br>
        <code>fields.foo.equal(bar)</code>
    </td>
  </tr>
  <tr>
    <td>notEqual</td>
    <td>`!=`</td>
    <td>
        <code>fields.foo `!=` bar</code><br>
        <code>fields.foo notEqual bar</code><br>
        <code>fields.foo.notEqual(bar)</code>
    </td>
  </tr>
  <tr>
    <td>lessThan</td>
    <td>lt</td>
    <td>
        <code>fields.foo lt bar</code><br>
        <code>fields.foo lessThan bar</code><br>
        <code>fields.foo.lessThan(bar)</code>
    </td>
  </tr>
  <tr>
    <td>lessThanOrEqual</td>
    <td>lte</td>
    <td>
        <code>fields.foo lte bar</code><br>
        <code>fields.foo lessThanOrEqual bar</code><br>
        <code>fields.foo.lessThanOrEqual(bar)</code>
    </td>
  </tr>
  <tr>
    <td>greaterThan</td>
    <td>gt</td>
    <td>
        <code>fields.foo gt bar</code><br>
        <code>fields.foo greaterThan bar</code><br>
        <code>fields.foo.greaterThan(bar)</code>
    </td>
  </tr>
  <tr>
    <td>greaterThanOrEqual</td>
    <td>gte</td>
    <td>
        <code>fields.foo gte bar</code><br>
        <code>fields.foo greaterThanOrEqual bar</code><br>
        <code>fields.foo.greaterThanOrEqual(bar)</code>
    </td>
  </tr>
  <tr>
    <td>between</td>
    <td>btw</td>
    <td>
        <code>fields.foo btw Pair(bar1, bar2)</code><br>
        <code>fields.foo between Pair(bar1, bar2)</code><br>
        <code>fields.foo.between(bar1, bar2)</code>
    </td>
  </tr>
  <tr>
    <td>like</td>
    <td></td>
    <td>
        <code>fields.foo like bar</code><br>
        <code>fields.foo.like(bar)</code>
    </td>
  </tr>
  <tr>
    <td>notLike</td>
    <td></td>
    <td>
        <code>fields.foo notLike bar</code><br>
        <code>fields.foo.notLike(bar)</code>
    </td>
  </tr>
  <tr>
    <td>isIn</td>
    <td>`in`</td>
    <td>
        <code>fields.foo `in` bars</code><br>
        <code>fields.foo isIn bars</code><br>
        <code>fields.foo.isIn(bars)</code>
    </td>
  </tr>
  <tr>
    <td>notIn</td>
    <td>`!in`</td>
    <td>
        <code>fields.foo `!in` bars</code><br>
        <code>fields.foo notIn bars</code><br>
        <code>fields.foo.notIn(bars)</code>
    </td>
  </tr>
</table>


### Sorting

Sorting is defined using the `orderBy` function:

```kotlin
val criteria = bookConditions {
    // settings and criteria...

    // sorting
    orderBy {
        fields.title sort ASC
        fields.published sort DESC
    }
}
```


## State Services

Vaultaire's state services provide an interface for querying states and tracking events
from the Vault (`queryBy`, `trackBy`), while decoupling data access or business logic code
from Corda's `ServiceHub` and `CordaRPCOps`.

### Generated State Service

Vaultaire will automatically subclass `StateService` to generate an extended sercice per annotated element.
The generated service name is "${contractStateTypeName}Service":  


```kotlin
// Create an instance of the generated service type
val bookStateService = BookStateService(
    serviceHub,        // Service hub or RPC ops
    serviceDefaults)   // Optional: criteria, paging, sort defaults

// query the vault for books
val searchResults = bookStateService.queryBy(
    criteria, paging, Pair("published", DESC), Pair("title", DESC))
```


### Custom Services

You can also subclass `BasicStateService`, `StateService` or even generated service types  
to create custom components.

```kotlin
class MyExtendedBookStateService(
        delegate: StateServiceDelegate<BookState>
) : BookStateService<BookState>(delegate){

    // Add the appropriate constructors
    // to initialize per delegate type:

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceRpcDelegate(rpcOps, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceHubDelegate(serviceHub, defaults))

    // Custom business methods...
}
```
