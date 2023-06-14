/*
 * Vaultaire: query DSL and data access utilities for Corda developers.
 * Copyright (C) 2018 Manos Batsis
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package com.github.manosbatsis.vaultaire.dsl.query

import com.github.manosbatsis.vaultaire.util.FieldWrapper
import com.github.manosbatsis.vaultaire.util.Fields
import com.github.manosbatsis.vaultaire.util.NullableGenericFieldWrapper
import com.github.manosbatsis.vaultaire.util.TypedFieldWrapper
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.BinaryComparisonOperator
import net.corda.core.node.services.vault.BinaryComparisonOperator.GREATER_THAN
import net.corda.core.node.services.vault.BinaryComparisonOperator.GREATER_THAN_OR_EQUAL
import net.corda.core.node.services.vault.BinaryComparisonOperator.LESS_THAN
import net.corda.core.node.services.vault.BinaryComparisonOperator.LESS_THAN_OR_EQUAL
import net.corda.core.node.services.vault.Builder.`in`
import net.corda.core.node.services.vault.Builder.avg
import net.corda.core.node.services.vault.Builder.between
import net.corda.core.node.services.vault.Builder.count
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.Builder.greaterThan
import net.corda.core.node.services.vault.Builder.greaterThanOrEqual
import net.corda.core.node.services.vault.Builder.isNull
import net.corda.core.node.services.vault.Builder.lessThan
import net.corda.core.node.services.vault.Builder.lessThanOrEqual
import net.corda.core.node.services.vault.Builder.like
import net.corda.core.node.services.vault.Builder.max
import net.corda.core.node.services.vault.Builder.min
import net.corda.core.node.services.vault.Builder.notEqual
import net.corda.core.node.services.vault.Builder.notIn
import net.corda.core.node.services.vault.Builder.notLike
import net.corda.core.node.services.vault.Builder.notNull
import net.corda.core.node.services.vault.Builder.sum
import net.corda.core.node.services.vault.ColumnPredicate.BinaryComparison
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.TimeCondition
import net.corda.core.node.services.vault.QueryCriteria.TimeInstantType
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.schemas.StatePersistable
import java.time.Instant
import java.util.LinkedHashSet
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.Suppress as supress

/** Condition interface */
interface Condition {

    /** Obtain the internal state as [QueryCriteria] if any, `null` otherwise */
    fun toCriteria(): QueryCriteria?
}

interface RootCondition<P : StatePersistable> : Condition {
    var status: Vault.StateStatus
    var stateRefs: List<StateRef>?
    var notary: List<AbstractParty>?
    var softLockingCondition: QueryCriteria.SoftLockingCondition?
    var timeCondition: QueryCriteria.TimeCondition?
    var relevancyStatus: Vault.RelevancyStatus
    var constraintTypes: Set<Vault.ConstraintInfo.Type>
    var constraints: Set<Vault.ConstraintInfo>
    var participants: List<AbstractParty>?
    var externalIds: List<UUID>
    val exactParticipants: List<AbstractParty>?
}

/** A [Condition] that contains other conditions. Allows for nested and/or condition groups */
abstract class ConditionsCondition<P : StatePersistable, out F : Fields<P>>() : Condition {

    /** The root condition */
    abstract val rootCondition: RootCondition<P>

    /** The fields of the target [StatePersistable] type `P` */
    abstract val fields: F

    /** The child conditions*/
    internal val conditions: MutableList<Condition> = mutableListOf()

    fun addCondition(condition: Condition) {
        conditions.add(condition)
    }

    fun and(initializer: CompositeCondition<P, F>.() -> Unit) = addCondition(AndCondition(fields, rootCondition).apply(initializer))

    fun or(initializer: CompositeCondition<P, F>.() -> Unit) = addCondition(OrCondition(fields, rootCondition).apply(initializer))

}

/**
 * A [ConditionsCondition], base implementation for and/or condition group containers.
 *  Allows for query criteria conditions.
 */
abstract class CompositeCondition<P : StatePersistable, out F : Fields<P>>(
        override val fields: F,
        override val rootCondition: RootCondition<P>
) : ConditionsCondition<P, F>() {

    private fun addCondition(expression: CriteriaExpression<P, Boolean>) =
            conditions.add(VaultCustomQueryCondition<P>(expression, rootCondition.status))

    fun <S> NullableGenericFieldWrapper<P, S>.isNull() =
            addCondition(property.isNull())

    fun <S> NullableGenericFieldWrapper<P, S>.notNull() =
            addCondition(property.notNull())

    infix fun <S> TypedFieldWrapper<P, S>.equal(value: S) =
            addCondition(property.equal(value))

    infix fun <S> TypedFieldWrapper<P, S>.`==`(value: S) = equal(value)

    infix fun <S> TypedFieldWrapper<P, S>.notEqual(value: S) =
            addCondition(property.notEqual(value))

    infix fun <S> TypedFieldWrapper<P, S>.`!=`(value: S) = notEqual(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.lessThan(value: S) =
            addCondition(property.lessThan(value))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.lt(value: S) = lessThan(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.lessThanOrEqual(value: S) =
            addCondition(property.lessThanOrEqual(value))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.lte(value: S) = lessThanOrEqual(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.greaterThan(value: S) =
            addCondition(property.greaterThan(value))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.gt(value: S) = greaterThan(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.greaterThanOrEqual(value: S) =
            addCondition(property.greaterThanOrEqual(value))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.gte(value: S) = greaterThanOrEqual(value)

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.between(from: S, to: S) =
            addCondition(property.between(from, to))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.between(value: Pair<S, S>) =
            addCondition(property.between(value.first, value.second))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.btw(value: Pair<S, S>) = between(value)

    infix fun <T : StatePersistable> TypedFieldWrapper<T, String>.like(value: String) =
            addCondition(VaultCustomQueryCondition(property.like(value), rootCondition.status))

    fun <T : StatePersistable> TypedFieldWrapper<T, String>.like(value: String, exactMatch: Boolean = true) =
            addCondition(VaultCustomQueryCondition(property.like(value, exactMatch), rootCondition.status))


    infix fun <T : StatePersistable> TypedFieldWrapper<T, String>.notLike(value: String) =
            addCondition(VaultCustomQueryCondition(property.notLike(value), rootCondition.status))

    fun <T : StatePersistable> TypedFieldWrapper<T, String>.notLike(value: String, exactMatch: Boolean = true) =
            addCondition(VaultCustomQueryCondition(property.notLike(value, exactMatch), rootCondition.status))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.isIn(value: Collection<S>) =
            addCondition(property.`in`(value))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.`in`(value: Collection<S>) = isIn(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.notIn(value: Collection<S>) =
            addCondition(property.notIn(value))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.`!in`(value: Collection<S>) = notIn(value)

    // non type safe versions
    infix fun FieldWrapper<P>._equal(value: Any) =
            addCondition(property.equal(value))

    infix fun FieldWrapper<P>._notEqual(value: Any) =
            addCondition(property.notEqual(value))

    infix fun FieldWrapper<P>._like(value: String) =
            addCondition(asStringProperty().like(value))

    infix fun FieldWrapper<P>._notLike(value: String) =
            addCondition(asStringProperty().notLike(value))

    @supress("UNCHECKED_CAST")
    protected fun FieldWrapper<P>.asStringProperty() = this.property as KProperty1<P, String>

}

/** Defines a set of conditions where all items must be matched */
class AndCondition<P : StatePersistable, out F : Fields<P>>(
        fields: F,
        rootCondition: RootCondition<P>
) : CompositeCondition<P, F>(fields, rootCondition) {


    override fun toCriteria(): QueryCriteria? =
            this.conditions.mapNotNull { it.toCriteria() }
                    .takeIf { it.isNotEmpty() }
                    ?.reduce { chain, link -> chain.and(link) }

}

/** Defines a set of conditions where at least a single item must be matched */
class OrCondition<P : StatePersistable, out F : Fields<P>>(
        fields: F,
        rootCondition: RootCondition<P>
) : CompositeCondition<P, F>(fields, rootCondition) {

    override fun toCriteria(): QueryCriteria? =
            this.conditions.mapNotNull { it.toCriteria() }
                    .takeIf { it.isNotEmpty() }
                    ?.reduce { chain, link -> chain.or(link) }
}

/**
 * A condition that wraps a [VaultCustomQueryCriteria] instance.
 * Instances of this type are typically created by
 * [FieldWrapper] extension functions defined by [CompositeCondition]
 * as shorthands (infix or regular), i.e. expression-only criteria without a contract state type,
 * vault state status or relevance status of their own.
 */
open class VaultCustomQueryCondition<L : StatePersistable>(
        private val criterion: VaultCustomQueryCriteria<L>
) : Condition {
    constructor(
            expression: CriteriaExpression<L, Boolean>,
            status: Vault.StateStatus
    ): this(VaultCustomQueryCriteria(expression, status))
    final override fun toCriteria(): QueryCriteria = criterion
}

/** Used to define aggregation criteria */
class Aggregates<P : StatePersistable>(val rootCondition: RootCondition<P>) {

    internal val aggregates: MutableList<Condition> = mutableListOf()

    private fun addAggregate(expression: CriteriaExpression<P, Boolean>) =
            aggregates.add(VaultCustomQueryCondition<P>(expression, rootCondition.status))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.sum(
            groupColumns: List<FieldWrapper<P>>? = null, orderBy: Sort.Direction? = null) =
            addAggregate(property.sum(groupColumns?.map { it.property }, orderBy))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.avg(
            groupColumns: List<FieldWrapper<P>>? = null, orderBy: Sort.Direction? = null) =
            addAggregate(property.avg(groupColumns?.map { it.property }, orderBy))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.min(
            groupColumns: List<FieldWrapper<P>>? = null, orderBy: Sort.Direction? = null) =
            addAggregate(property.min(groupColumns?.map { it.property }, orderBy))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.max(
            groupColumns: List<FieldWrapper<P>>? = null, orderBy: Sort.Direction? = null) =
            addAggregate(property.max(groupColumns?.map { it.property }, orderBy))

    fun FieldWrapper<P>.count() =
            addAggregate(property.count())
}

/** Used to define [Sort.SortColumn]s */
class SortColumns<P : StatePersistable>(val statePersistableType: Class<P>) {

    val ASC = Sort.Direction.ASC
    val DESC = Sort.Direction.DESC

    // CommonStateAttribute entries
    val stateRef = Sort.CommonStateAttribute.STATE_REF
    val stateRefTxnId = Sort.CommonStateAttribute.STATE_REF_TXN_ID
    val stateRefIndex = Sort.CommonStateAttribute.STATE_REF_INDEX

    // VaultStateAttribute entries
    val notaryName = Sort.VaultStateAttribute.NOTARY_NAME
    val contractStateType = Sort.VaultStateAttribute.CONTRACT_STATE_TYPE
    val stateStatus = Sort.VaultStateAttribute.STATE_STATUS
    val recordedTime = Sort.VaultStateAttribute.RECORDED_TIME
    val consumedTime = Sort.VaultStateAttribute.CONSUMED_TIME
    val lockId = Sort.VaultStateAttribute.LOCK_ID
    val constraintType = Sort.VaultStateAttribute.CONSTRAINT_TYPE

    // LinearStateAttribute entries
    val uuid = Sort.LinearStateAttribute.UUID
    val externalId = Sort.LinearStateAttribute.EXTERNAL_ID

    // FungibleStateAttribute entries
    val quantity = Sort.FungibleStateAttribute.QUANTITY
    val issuerRef = Sort.FungibleStateAttribute.ISSUER_REF

    val entries: LinkedHashSet<Sort.SortColumn> = linkedSetOf()

    infix fun Sort.CommonStateAttribute.sort(value: Sort.Direction) =
            entries.add(Sort.SortColumn(SortAttribute.Standard(this), value))

    infix fun Sort.FungibleStateAttribute.sort(value: Sort.Direction) =
            entries.add(Sort.SortColumn(SortAttribute.Standard(this), value))

    infix fun Sort.LinearStateAttribute.sort(value: Sort.Direction) =
            entries.add(Sort.SortColumn(SortAttribute.Standard(this), value))

    infix fun Sort.VaultStateAttribute.sort(value: Sort.Direction) =
            entries.add(Sort.SortColumn(SortAttribute.Standard(this), value))

    infix fun FieldWrapper<P>.sort(value: Sort.Direction) {
        entries.add(Sort.SortColumn(SortAttribute.Custom(statePersistableType, this.property.name), value))
    }
}

abstract class TimeInstantTypeCondition(internal val type: TimeInstantType)
class TimeRecordedCondition : TimeInstantTypeCondition(TimeInstantType.RECORDED)
class TimeConsumedCondition : TimeInstantTypeCondition(TimeInstantType.CONSUMED)


/**
 * A [ConditionsCondition] extended by Vaultaire's annotation processing to create a condition DSL specific to a [ContractState] type.
 * Defines a root [QueryCriteria.VaultQueryCriteria]. Allows for defining `and`/`or` condition groups, as well as a [Sort]
 */
abstract class VaultQueryCriteriaCondition<P : StatePersistable, out F : Fields<P>>(
        override var status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
        override var stateRefs: List<StateRef>? = null,
        override var notary: List<AbstractParty>? = null,
        override var softLockingCondition: QueryCriteria.SoftLockingCondition? = null,
        override var timeCondition: TimeCondition? = null,
        override var relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
        override var constraintTypes: Set<Vault.ConstraintInfo.Type> = emptySet(),
        override var constraints: Set<Vault.ConstraintInfo> = emptySet(),
        override var participants: List<AbstractParty>? = null,
        override var externalIds: List<UUID> = emptyList(),
        override val exactParticipants: List<AbstractParty>? = null
) : ConditionsCondition<P, F>(), RootCondition<P> {
    override val rootCondition: RootCondition<P>
        get() = this

    /** The target [ContractState] type */
    abstract val contractStateType: Class<out ContractState>

    /** The target [StatePersistable] type */
    abstract val statePersistableType: Class<P>

    private lateinit var aggregates: Aggregates<P>
    private lateinit var sortColumns: SortColumns<P>

    val timeRecorded = TimeRecordedCondition()
    val timeConsumed = TimeConsumedCondition()

    private fun setTimeCondition(instantType: TimeInstantType, operator: BinaryComparisonOperator, instant: Instant) {
        timeCondition = TimeCondition(instantType, BinaryComparison(operator, instant))
    }

    infix fun TimeInstantTypeCondition.greaterThanOrEqual(instant: Instant) = setTimeCondition(this.type, GREATER_THAN_OR_EQUAL, instant)
    infix fun TimeInstantTypeCondition.gtw(instant: Instant) = greaterThanOrEqual(instant)
    infix fun TimeInstantTypeCondition.greaterThan(instant: Instant) = setTimeCondition(this.type, GREATER_THAN, instant)
    infix fun TimeInstantTypeCondition.gt(instant: Instant) = greaterThan(instant)
    infix fun TimeInstantTypeCondition.lessThanOrEqual(instant: Instant) = setTimeCondition(this.type, LESS_THAN_OR_EQUAL, instant)
    infix fun TimeInstantTypeCondition.ltw(instant: Instant) = lessThanOrEqual(instant)
    infix fun TimeInstantTypeCondition.lessThan(instant: Instant) = setTimeCondition(this.type, LESS_THAN, instant)
    infix fun TimeInstantTypeCondition.lt(instant: Instant) = lessThan(instant)


    fun aggregate(initializer: Aggregates<P>.() -> Unit) {
        aggregates = Aggregates(this).apply(initializer)
    }

    fun orderBy(initializer: SortColumns<P>.() -> Unit) {
        sortColumns = SortColumns(statePersistableType).apply(initializer)
    }

    fun toSort() = Sort(if (::sortColumns.isInitialized) sortColumns.entries else emptySet())

    override fun toCriteria(): QueryCriteria {
        // Always applied root last to override status etc.
        val rootCriteria = QueryCriteria.VaultQueryCriteria(
                externalIds = externalIds,
                status = status,
                contractStateTypes = setOf(contractStateType),
                stateRefs = stateRefs,
                notary = notary,
                softLockingCondition = softLockingCondition,
                timeCondition = timeCondition,
                relevancyStatus = relevancyStatus,
                constraintTypes = constraintTypes,
                constraints = constraints,
                participants = participants,
                exactParticipants = exactParticipants
        )
        var criteria = this.conditions.mapNotNull { it.toCriteria() }
                .takeIf { it.isNotEmpty() }
                ?.reduce { chain, link -> chain.and(link) }

        return criteria?.and(rootCriteria) ?: rootCriteria
    }

    /**
     * Obtain the internal state as [QueryCriteria] if any, `null` otherwise
     * @param ignoreAggregates whether to ignore aggregate functions.
     * Corda paged queries can have either state or aggregate results, but not both.
     */
    fun toCriteria(ignoreAggregates: Boolean = false): QueryCriteria {
        val aggregateCriteria = if (!ignoreAggregates && ::aggregates.isInitialized) {
            aggregates.aggregates.mapNotNull { it.toCriteria() }
                    .takeIf { it.isNotEmpty() }
                    ?.reduce { chain, link -> chain.and(link) }
            }else null

        // Always applied (the product of) root last to override status etc.
        return aggregateCriteria?.and(toCriteria()) ?: toCriteria()
    }
}
