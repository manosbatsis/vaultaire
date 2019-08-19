Vaultaire: query DSL and data access utilities for Corda developers. [![Maven Central](https://img.shields.io/maven-central/v/com.github.manosbatsis.vaultaire/vaultaire.svg)](https://mvnrepository.com/artifact/com.github.manosbatsis.vaultaire/vaultaire) 

<!-- TOC depthFrom:2 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Installation](#installation)
- [Query DSL](#query-dsl)
	- [Quick Example](#quick-example)
	- [Query Settings](#query-settings)
	- [Adding Criteria](#adding-criteria)
		- [Accessing Fields](#accessing-fields)
	- [Functions and Operators](#functions-and-operators)
	- [Sorting](#sorting)
- [State Service](#state-service)

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


// Use Vaultaire's DSL generation!
@VaultQueryDsl(name = "booksQuery", constractStateType = BookState::class)
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

With Vaultaire's `@VaultQueryDsl` and the generated DSL this becomes:

```kotlin
// Use the generated DSL to create query criteria
val query = booksQuery {
    status = Vault.StateStatus.ALL
    where {
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
            fields.foo1.isNullValue()
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
        <code>fields.foo isNull</code><br>
        <code>fields.foo.isNull()</code></td>
  </tr>
  <tr>
    <td>notNull</td>
    <td></td>
    <td>
        <code>fields.foo notNull</code><br>
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
    <td>lt</td>
    <td>
        <code>fields.foo gt bar</code><br>
        <code>fields.foo greaterThan bar</code><br>
        <code>fields.foo.greaterThan(bar)</code>
    </td>
  </tr>
  <tr>
    <td>greaterThanOrEqual</td>
    <td>lte</td>
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
</table>                                           	|

### Sorting

Sorting is defined using the

```kotlin
val criteria = bookConditions {
    // settings and criteria...

    // sorting
    orderBy {
        fields.title sort ASC
        fields.published sort DESC
    }
```


## State Service

Vaultaire's `StateService` provides a `ContractState`-specific interface for querying the Vault,  
while decoupling data access or business logic code from Corda's `ServiceHub` and `CordaRPCOps`.

```kotlin
val bookSearchResult = StateService(b.services, BookContract.BookState::class.java)
        .queryBy(criteria, paging, sort)
```

You can also subclass `StateService` to create custom components for reuse both inside and outside a node.
