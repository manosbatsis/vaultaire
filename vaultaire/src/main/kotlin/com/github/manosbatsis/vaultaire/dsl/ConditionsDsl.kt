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
package com.github.manosbatsis.vaultaire.dsl

import com.github.manosbatsis.vaultaire.util.FieldWrapper
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.`in`
import net.corda.core.node.services.vault.Builder.between
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.Builder.greaterThan
import net.corda.core.node.services.vault.Builder.greaterThanOrEqual
import net.corda.core.node.services.vault.Builder.isNull
import net.corda.core.node.services.vault.Builder.lessThan
import net.corda.core.node.services.vault.Builder.lessThanOrEqual
import net.corda.core.node.services.vault.Builder.like
import net.corda.core.node.services.vault.Builder.notEqual
import net.corda.core.node.services.vault.Builder.notIn
import net.corda.core.node.services.vault.Builder.notLike
import net.corda.core.node.services.vault.Builder.notNull
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.schemas.StatePersistable
import kotlin.reflect.KProperty1
import kotlin.Suppress as supress


/** Condition interface */
interface Condition {

    /** Implement to expose internal state as criteria */
    abstract fun toCriteria(): QueryCriteria?
}

/** A [Condition] that contains other conditions. Allows for nested and/or condition groups */
abstract class ConditionsCondition<T : StatePersistable, out F: Fields<T>>: Condition {

    /** The fields of the target [StatePersistable] type `T` */
    abstract val fields: F

    val conditions: MutableList<Condition> = mutableListOf()

    fun addCondition(condition: Condition) {
        conditions += condition
    }

    fun and(initializer: CompositeCondition<T, F>.() -> Unit) = addCondition(AndCondition(fields).apply(initializer))

    fun or(initializer: CompositeCondition<T, F>.() -> Unit) = addCondition(OrCondition(fields).apply(initializer))

}

/**
 * A [ConditionsCondition], base implementation for and/or condition group containers.
 *  Allows for query criteria conditions.
 */
abstract class CompositeCondition<T : StatePersistable, out F: Fields<T>>(
        override val fields: F
) : ConditionsCondition<T, F>() {

    fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.isNull() =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.isNull())))

    @supress("unused")
    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.isNull(none: Unit) = isNull()

    fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.notNull() =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.notNull())))

    @supress("unused")
    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.notNull(none: Unit) = notNull()

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.equal(value: S) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.equal(value))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.`==`(value: S) = equal(value)

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.notEqual(value: S) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.notEqual(value))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.`!=`(value: S) = notEqual(value)

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.lessThan(value: S) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.lessThan(value))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.lt(value: S) = lessThan(value)

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.lessThanOrEqual(value: S) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.lessThanOrEqual(value))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.lte(value: S) = lessThanOrEqual(value)

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.greaterThan(value: S) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.greaterThan(value))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.gt(value: S) = greaterThan(value)

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.greaterThanOrEqual(value: S) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.greaterThanOrEqual(value))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.gte(value: S) = greaterThanOrEqual(value)

    fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.between(from: S, to: S) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.between(from, to))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.between(value: Pair<S, S>) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.between(value.first, value.second))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.btw(value: Pair<S, S>) = between(value)

    infix fun <T : StatePersistable> FieldWrapper<T, String>.like(value: String) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.like(value))))

    fun <T : StatePersistable> FieldWrapper<T, String>.like(value: String, exactMatch: Boolean = true) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.like(value, exactMatch))))

    infix fun <T : StatePersistable> FieldWrapper<T, String>.notLike(value: String) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.notLike(value))))

    fun <T : StatePersistable> FieldWrapper<T, String>.notLike(value: String, exactMatch: Boolean = true) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.notLike(value, exactMatch))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.isIn(value: Collection<S>) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.`in`(value))))

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.`in`(value: Collection<S>) = isIn(value)

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.notIn(value: Collection<S>) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.notIn(value))))

    fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.`!in`(value: Collection<S>) = notIn(value)

    // non type safe versions
    infix fun <T : StatePersistable> FieldWrapper<T, *>._equal(value: Any) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.equal(value))))

    infix fun <T : StatePersistable> FieldWrapper<T, *>._notEqual(value: Any) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.property.notEqual(value))))

    infix fun <T : StatePersistable> FieldWrapper<T, *>._like(value: String) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.asStringProperty().like(value))))

    infix fun <T : StatePersistable> FieldWrapper<T, *>._notLike(value: String) =
            addCondition(VaultCustomQueryCriteriaCondition(VaultCustomQueryCriteria(this.asStringProperty().notLike(value))))

    @supress("UNCHECKED_CAST")
    protected fun <T : StatePersistable> FieldWrapper<T, *>.asStringProperty() = this.property as KProperty1<T, String>
}

/** Defines a set of conditions where all items must be matched */
class AndCondition<T : StatePersistable, out F: Fields<T>>(
        fields: F
) : CompositeCondition<T, F>(fields) {

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
class OrCondition<T : StatePersistable, out F: Fields<T>>(
        fields: F
) : CompositeCondition<T, F>(fields) {

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
 * A condition equivalent to a [VaultCustomQueryCriteria] instance.
 * Created by infix and regular extension functions of [FieldWrapper]
 * defined by [CompositeCondition]
 */
class VaultCustomQueryCriteriaCondition(val criterion: VaultCustomQueryCriteria<*>) : Condition {
    override fun toCriteria(): QueryCriteria = criterion
}

/** Used to define [Sort.SortColumn]s */
class SortColumns<T : StatePersistable>(val statePersistableType: Class<T>){

    val ASC = Sort.Direction.ASC
    val DESC = Sort.Direction.DESC
    val entries: LinkedHashSet<Sort.SortColumn> = linkedSetOf()

    infix fun <T : StatePersistable, S : Comparable<S>> FieldWrapper<T, S>.sort(value: Sort.Direction) =
            entries.add(Sort.SortColumn(SortAttribute.Custom(statePersistableType, this.property.name), value))

}



/**
 * A [ConditionsCondition] extended by Vaultaire's annotation processing to create a condition DSL specific to a [ContractState] type.
 * Defines a root [QueryCriteria.VaultQueryCriteria]. Allows for defining `and`/`or` condition groups, as well as a [Sort]
 */
abstract class VaultQueryCriteriaCondition<T : StatePersistable, out F: Fields<T>>(
        var status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
        var stateRefs: List<StateRef>? = null,
        var notary: List<AbstractParty>? = null,
        var softLockingCondition: QueryCriteria.SoftLockingCondition? = null,
        var timeCondition: QueryCriteria.TimeCondition? = null,
        var relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
        var constraintTypes: Set<Vault.ConstraintInfo.Type> = emptySet(),
        var constraints: Set<Vault.ConstraintInfo> = emptySet(),
        var participants: List<AbstractParty>? = null
) : ConditionsCondition<T, F>() {

    /** The target [ContractState] type */
    abstract val contractStateType: Class<out ContractState>

    /** The target [StatePersistable] type */
    abstract val statePersistableType: Class<T>

    lateinit var sortColumns: SortColumns<T>

    fun orderBy(initializer: SortColumns<T>.() -> Unit) {
        sortColumns = SortColumns(statePersistableType).apply(initializer)
    }

    fun toSort(): Sort = Sort(sortColumns.entries)

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
}
