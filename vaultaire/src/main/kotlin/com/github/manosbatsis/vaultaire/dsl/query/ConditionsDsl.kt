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
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.schemas.StatePersistable
import kotlin.reflect.KProperty1
import kotlin.Suppress as supress


/** Condition interface */
interface Condition {

    /** Obtain the internal state as [QueryCriteria] if any, `null` otherwise */
    fun toCriteria(): QueryCriteria?
}

/** A [Condition] that contains other conditions. Allows for nested and/or condition groups */
abstract class ConditionsCondition<P : StatePersistable, out F: Fields<P>>: Condition {

    /** The fields of the target [StatePersistable] type `P` */
    abstract val fields: F

    internal val conditions: MutableList<Condition> = mutableListOf()

    fun addCondition(condition: Condition) {
        conditions.add(condition)
    }

    fun and(initializer: CompositeCondition<P, F>.() -> Unit) = addCondition(AndCondition(fields).apply(initializer))

    fun or(initializer: CompositeCondition<P, F>.() -> Unit) = addCondition(OrCondition(fields).apply(initializer))

}

/**
 * A [ConditionsCondition], base implementation for and/or condition group containers.
 *  Allows for query criteria conditions.
 */
abstract class CompositeCondition<P : StatePersistable, out F: Fields<P>>(
        override val fields: F
) : ConditionsCondition<P, F>() {

    fun <S> NullableGenericFieldWrapper<P, S>.isNull() =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.isNull())))

    fun <S> NullableGenericFieldWrapper<P, S>.notNull() =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.notNull())))

    infix fun <S> TypedFieldWrapper<P, S>.equal(value: S) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.equal(value))))

    infix fun <S> TypedFieldWrapper<P, S>.`==`(value: S) = equal(value)

    infix fun <S> TypedFieldWrapper<P, S>.notEqual(value: S) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.notEqual(value))))

    infix fun <S> TypedFieldWrapper<P, S>.`!=`(value: S) = notEqual(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.lessThan(value: S) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.lessThan(value))))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.lt(value: S) = lessThan(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.lessThanOrEqual(value: S) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.lessThanOrEqual(value))))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.lte(value: S) = lessThanOrEqual(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.greaterThan(value: S) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.greaterThan(value))))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.gt(value: S) = greaterThan(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.greaterThanOrEqual(value: S) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.greaterThanOrEqual(value))))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.gte(value: S) = greaterThanOrEqual(value)

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.between(from: S, to: S) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.between(from, to))))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.between(value: Pair<S, S>) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.between(value.first, value.second))))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.btw(value: Pair<S, S>) = between(value)

    infix fun <T : StatePersistable> TypedFieldWrapper<T, String>.like(value: String) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.like(value))))

    fun <T : StatePersistable> TypedFieldWrapper<T, String>.like(value: String, exactMatch: Boolean = true) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.like(value, exactMatch))))

    infix fun <T : StatePersistable> TypedFieldWrapper<T, String>.notLike(value: String) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.notLike(value))))

    fun <T : StatePersistable> TypedFieldWrapper<T, String>.notLike(value: String, exactMatch: Boolean = true) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.notLike(value, exactMatch))))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.isIn(value: Collection<S>) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.`in`(value))))

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.`in`(value: Collection<S>) = isIn(value)

    infix fun <S : Comparable<S>> TypedFieldWrapper<P, S>.notIn(value: Collection<S>) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.notIn(value))))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.`!in`(value: Collection<S>) = notIn(value)

    // non type safe versions
    infix fun FieldWrapper<P>._equal(value: Any) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.equal(value))))

    infix fun FieldWrapper<P>._notEqual(value: Any) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.property.notEqual(value))))

    infix fun FieldWrapper<P>._like(value: String) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.asStringProperty().like(value))))

    infix fun FieldWrapper<P>._notLike(value: String) =
            addCondition(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(this.asStringProperty().notLike(value))))

    @supress("UNCHECKED_CAST")
    protected fun FieldWrapper<P>.asStringProperty() = this.property as KProperty1<P, String>

}

/** Defines a set of conditions where all items must be matched */
class AndCondition<P : StatePersistable, out F: Fields<P>>(
        fields: F
) : CompositeCondition<P, F>(fields) {


    override fun toCriteria(): QueryCriteria? {
        var criteria: QueryCriteria? = null
        this.conditions.forEach {
            val childCriteria = it.toCriteria()
            if(childCriteria != null) {
                if(criteria == null ) criteria = childCriteria
                else criteria = criteria!!.and(childCriteria)
            }
        }
        return criteria
    }
}

/** Defines a set of conditions where at least a single item must be matched */
class OrCondition<P : StatePersistable, out F: Fields<P>>(
        fields: F
) : CompositeCondition<P, F>(fields) {

    override fun toCriteria(): QueryCriteria? {
        var criteria: QueryCriteria? = null
        this.conditions.forEach {
            val childCriteria = it.toCriteria()
            if(childCriteria != null) {
                if(criteria == null ) criteria = childCriteria
                else criteria = criteria!!.or(childCriteria)
            }
        }
        return criteria
    }
}

/**
 * A condition that wraps a [VaultCustomQueryCriteria] instance.
 * Instances of this type are typically created by
 * [FieldWrapper] extension functions defined by [CompositeCondition]
 * as shorthands (infix or regular), i.e. expression-only criteria without a contract state type,
 * vault state status or relevance status of their own.
 */
open class VaultCustomQueryCriteriaWrapperCondition(
        private val criterion: VaultCustomQueryCriteria<*>
) : Condition {
    final override fun toCriteria(): QueryCriteria = criterion
}

/** Used to define aggregation criteria */
class Aggregates<P : StatePersistable>(val statePersistableType: Class<P>) {

    internal val aggregates: MutableList<Condition> = mutableListOf()

    private fun addAggregate(condition: Condition)  = aggregates.add(condition)

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.sum(
            groupColumns: List<FieldWrapper<P>>? = null, orderBy: Sort.Direction? = null) =
            addAggregate(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(
                    this.property.sum(if (groupColumns != null) groupColumns.map { it.property } else null, orderBy))))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.avg(
            groupColumns: List<FieldWrapper<P>>? = null, orderBy: Sort.Direction? = null) =
            addAggregate(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(
                    this.property.avg(if (groupColumns != null) groupColumns.map { it.property } else null, orderBy))))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.min(
            groupColumns: List<FieldWrapper<P>>? = null, orderBy: Sort.Direction? = null) =
            addAggregate(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(
                    this.property.min(if (groupColumns != null) groupColumns.map { it.property } else null, orderBy))))

    fun <S : Comparable<S>> TypedFieldWrapper<P, S>.max(
            groupColumns: List<FieldWrapper<P>>? = null, orderBy: Sort.Direction? = null) =
            addAggregate(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(
                    this.property.max(if (groupColumns != null) groupColumns.map { it.property } else null, orderBy))))

    fun FieldWrapper<P>.count() =
            addAggregate(VaultCustomQueryCriteriaWrapperCondition(VaultCustomQueryCriteria(
                    this.property.count())))
}

/** Used to define [Sort.SortColumn]s */
class SortColumns<P : StatePersistable>(val statePersistableType: Class<P>){

    val ASC = Sort.Direction.ASC
    val DESC = Sort.Direction.DESC
    val entries: LinkedHashSet<Sort.SortColumn> = linkedSetOf()

    infix fun FieldWrapper<P>.sort(value: Sort.Direction) {
        entries.add(Sort.SortColumn(SortAttribute.Custom(statePersistableType, this.property.name), value))
    }
}


/**
 * A [ConditionsCondition] extended by Vaultaire's annotation processing to create a condition DSL specific to a [ContractState] type.
 * Defines a root [QueryCriteria.VaultQueryCriteria]. Allows for defining `and`/`or` condition groups, as well as a [Sort]
 */
abstract class VaultQueryCriteriaCondition<P : StatePersistable, out F: Fields<P>>(
        var status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
        var stateRefs: List<StateRef>? = null,
        var notary: List<AbstractParty>? = null,
        var softLockingCondition: QueryCriteria.SoftLockingCondition? = null,
        var timeCondition: QueryCriteria.TimeCondition? = null,
        var relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
        var constraintTypes: Set<Vault.ConstraintInfo.Type> = emptySet(),
        var constraints: Set<Vault.ConstraintInfo> = emptySet(),
        var participants: List<AbstractParty>? = null
) : ConditionsCondition<P, F>() {

    /** The target [ContractState] type */
    abstract val contractStateType: Class<out ContractState>

    /** The target [StatePersistable] type */
    abstract val statePersistableType: Class<P>

    lateinit private var aggregates: Aggregates<P>
    lateinit private var sortColumns: SortColumns<P>

    fun aggregate(initializer: Aggregates<P>.() -> Unit) {
        aggregates = Aggregates(statePersistableType).apply(initializer)
    }

    fun orderBy(initializer: SortColumns<P>.() -> Unit) {
        sortColumns = SortColumns(statePersistableType).apply(initializer)
    }

    fun toSort() = Sort( if(::sortColumns.isInitialized) sortColumns.entries else emptySet() )

    override fun toCriteria(): QueryCriteria {
        var criteria: QueryCriteria = QueryCriteria.VaultQueryCriteria(
                status, setOf(contractStateType), stateRefs, notary, softLockingCondition, timeCondition,
                relevancyStatus, constraintTypes, constraints, participants)

        this.conditions.forEach {
            val childCriteria = it.toCriteria()
            if(childCriteria != null) criteria = criteria.and(childCriteria)
        }

        return criteria
    }

    /**
     * Obtain the internal state as [QueryCriteria] if any, `null` otherwise
     * @param ignoreAggregates whether to ignore aggregate functions.
     * Corda paged queries can have either state or aggregate results, but not both.
     */
    fun toCriteria(ignoreAggregates: Boolean): QueryCriteria {
        var criteria: QueryCriteria = toCriteria()
        if(!ignoreAggregates && ::aggregates.isInitialized) {
            aggregates.aggregates.forEach {
                val childCriteria = it.toCriteria()
                if(childCriteria != null) criteria = criteria.and(childCriteria)
            }
        }
        return criteria
    }
}
