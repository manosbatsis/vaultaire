# Query DSL

Vaultaire uses an annotation processor at build-time to create a query DSL for your contract/persistent states.

## Quick Example

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
@VaultaireStateUtils(name = "booksQuery", contractStateType = BookState::class)
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

## Before Vaultaire

_Before_ Vaultaire, you probably had to create query criteria with something like:

```kotlin
val query = VaultQueryCriteria(
		 contractStateTypes = setOf(BookState::class.java),
		 status = Vault.StateStatus.ALL
 )
 // Check publisher?
 if(checkPublisher)  query = query.and(VaultCustomQueryCriteria(PersistentBookState::publisher.equal("Corda Books Ltd.")))
 // Add more criteria
 query.and(VaultCustomQueryCriteria(PersistentBookState::title.equal("A book on Corda"))
	 .or(VaultCustomQueryCriteria(PersistentBookState::author.notEqual("John Doe"))))

 val sort = Sort(listOf(Sort.SortColumn(
	 SortAttribute.Custom(PersistentBookState::class.java, "published"), Sort.Direction.DESC)))
// Finally!
 queryBy(query, sort)
```

A bit verbose so get's difficult to read as the query becomes more complex. Let's try to simplify things bellow.

## With Vaultaire DSL

With Vaultaire's `@VaultaireStateUtils` and the generated DSL this becomes:

```kotlin
// Use the generated DSL to create query criteria
val query = booksQuery {
    status = Vault.StateStatus.ALL
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
}

queryBy(query.toCriteria(), query.toSort())

```

## Create a DSL

### Project Module States

To create a query DSL for your state after [installing](#installation) Vaultaire, 
annotate the corresponding `PersistentState` with `@VaultaireStateUtils`:

```kotlin
// Use Vaultaire's DSL generation!
@VaultaireStateUtils(
  // If you omit the name, the DSL function will be named by appending "Query"
  // to the decapitalized contract state name, e.g. "bookStateQuery"
  name = "booksQuery",
  contractStateType = BookState::class)
@Entity
@Table(name = "books")
data class PersistentBookState(
    // state properties...
) : PersistentState()
```

### Project Dependency States

To create a query DSL for a state from your project dependencies, annotate the
any class in your project using the special `@VaultaireStateUtilsFor` annotation, 
providing the state's  `ContractState` and `PersistentState`:

```kotlin
@VaultaireStateUtilsMixin(name = "fungibleTokenConditions",
        persistentStateType = PersistentFungibleToken::class,
        contractStateType = FungibleToken::class)
class FungibleMixin
```

## Query Settings

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

## Adding Criteria

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

## Adding Aggregates

Aggregates can be specified within the `and` / `or` functions:

```kotlin
val bookStateQuery = bookStateQuery {
    // settings...
    
    // criteria
    and {
        fields.title `like` "%Corda Foundation%"
        fields.genre `==` BookContract.BookGenre.SCIENCE_FICTION
    }
    // aggregates
    aggregate {
        // add some aggregates
        fields.externalId.count()
        fields.id.count()
        fields.editions.sum()
        fields.price.min()
        fields.price.avg()
        fields.price.max()
    }
}
```

> **Note**: Corda paged queries can include either query results or "other" results based on the above aggregates.
For that purpose, the `toCriteria` functions accepts an optional boolean to ignore aggregates, thus allowing
the reuse of the same query to obtain either paged state or aggregate results. 

## Accessing Fields

Fields can be accessed via the generated DSL's `fields` object within `and`, `or`, or `orderBy`
functions using dot notation e.g. `fields.foo`.

> You can also retrieve fields by name with e.g. `fields["foo"]` or  `fields.get("foo")` and use
non typesafe functions like `_equal`, `_notEqual`, `_like`, `_notLike` but this may change in a future release.

## Functions and Operators

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

## Aggregate Functions

<table>
  <tr>
    <th>Name</th>
    <th>Examples</th>
  </tr>
  <tr>
    <td>avg</td>
    <td>
        <code>fields.foo.avg()</code><br>
        <code>fields.foo.avg(groupByColumns)</code><br>
        <code>fields.foo.avg(groupByColumns, sortDirection)</code><br>
    </td>
  </tr>
  <tr>
    <td>count</td>
    <td>
        <code>fields.foo.count()</code><br>
    </td>
  </tr>
  <tr>
    <td>max</td>
    <td>
        <code>fields.foo.max()</code><br>
        <code>fields.foo.max(groupByColumns)</code><br>
        <code>fields.foo.max(groupByColumns, sortDirection)</code><br>
    </td>
  </tr>
  <tr>
    <td>min</td>
    <td>
        <code>fields.foo.min()</code><br>
        <code>fields.foo.min(groupByColumns)</code><br>
        <code>fields.foo.min(groupByColumns, sortDirection)</code><br>
    </td>
  </tr>
  <tr>
    <td>sum</td>
    <td>
        <code>fields.foo.sum()</code><br>
        <code>fields.foo.sum(groupByColumns)</code><br>
        <code>fields.foo.sum(groupByColumns, sortDirection)</code><br>
    </td>
  </tr>
</table>

## Sorting

Sorting is defined using the `orderBy` function. Both custom fields and 
standard attributes are supported, while aliases for standard attributes 
are provided for convenience:

```kotlin
val criteria = bookConditions {
    // settings and criteria...

    // sorting
    orderBy {
        // Sort by standard attribute alias, same as
        // Sort.VaultStateAttribute.RECORDED_TIME sort ASC
        recordedTime sort ASC
        // Sort by custom field
        fields.title sort DESC
    }
}
```

The following standard attribute aliases are provided:

| Alias              	| Standard Attribute                           	|
|-------------------	|----------------------------------------------	|
| stateRef          	| Sort.CommonStateAttribute.STATE_REF          	|
| stateRefTxnId     	| Sort.CommonStateAttribute.STATE_REF_TXN_ID   	|
| stateRefIndex     	| Sort.CommonStateAttribute.STATE_REF_INDEX    	|
| notaryName        	| Sort.VaultStateAttribute.NOTARY_NAME         	|
| contractStateType 	| Sort.VaultStateAttribute.CONTRACT_STATE_TYPE 	|
| stateStatus       	| Sort.VaultStateAttribute.STATE_STATUS        	|
| recordedTime      	| Sort.VaultStateAttribute.RECORDED_TIME       	|
| consumedTime      	| Sort.VaultStateAttribute.CONSUMED_TIME       	|
| lockId            	| Sort.VaultStateAttribute.LOCK_ID             	|
| constraintType    	| Sort.VaultStateAttribute.CONSTRAINT_TYPE     	|
| uuid              	| Sort.LinearStateAttribute.UUID               	|
| externalId        	| Sort.LinearStateAttribute.EXTERNAL_ID        	|
| quantity          	| Sort.FungibleStateAttribute.QUANTITY         	|
| issuerRef         	| Sort.FungibleStateAttribute.ISSUER_REF       	|