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
package com.github.manosbatsis.vaultaire.dao

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.util.Fields
import com.github.manosbatsis.vaultaire.util.asUniqueIdentifier
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.schemas.QueryableState
import net.corda.core.schemas.StatePersistable
import java.util.*

class StateNotFoundException(id: String, stateType: Class<*>) : RuntimeException("Could not find a ${stateType.javaClass.simpleName} with id ${id}")

/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
interface StateService<T : ContractState>: StateServiceDelegate<T> {

    val ofLinearState: Boolean
    val ofQueryableState: Boolean

    /**
     * Find the state of type [T] matching the given [UUID] if any, throw an error otherwise
     * @throws StateNotFoundException if no match is found
     */
    fun getByLinearId(linearId: UUID, relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T> =
            getByLinearId(UniqueIdentifier(id = linearId), relevancyStatus)

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any, throw an error otherwise
     * @throws StateNotFoundException if no match is found
     */
    fun getByLinearId(linearId: String, relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T> =
            getByLinearId(linearId.asUniqueIdentifier(), relevancyStatus)

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any, throw an error otherwise
     * @throws StateNotFoundException if no match is found
     */
    fun getByLinearId(linearId: UniqueIdentifier, relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T> =
            findByLinearId(linearId, relevancyStatus) ?: throw StateNotFoundException(linearId.toString(), contractStateType)

    /**
     * Find the state of type [T] matching the given [UUID] if any
     */
    fun findByLinearId(linearId: UUID, relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T>? =
            findByLinearId(UniqueIdentifier(id = linearId), relevancyStatus)
    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any
     */
    fun findByLinearId(linearId: String, relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T>? =
            findByLinearId(linearId.asUniqueIdentifier(), relevancyStatus)
    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any
     */
    fun findByLinearId(linearId: UniqueIdentifier, relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T>?

    /**
     * Find the [Vault.StateStatus.UNCONSUMED] state of type [T]
     * matching the given [UniqueIdentifier.externalId] if any, throw an error otherwise
     * @throws StateNotFoundException if no match is found
     */
    fun getByExternalId(externalId: String): StateAndRef<T> =
            findByExternalId(externalId) ?: throw StateNotFoundException(externalId, contractStateType)

    /**
     * Find the [Vault.StateStatus.UNCONSUMED] state of type [T]
     * matching the given [UniqueIdentifier.externalId] if any
     */
    fun findByExternalId(externalId: String): StateAndRef<T>?

    /** Count states of type [T] matching stored in the vault and matching any given criteria */
    fun countBy(criteria: QueryCriteria = defaults.criteria): Long

    /**
     * Query the vault for states of type [T] matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun queryBy(
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            sort: Sort = defaults.sort
    ): Vault.Page<T>

    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun trackBy(
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            sort: Sort = defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>>
}

/**
 * Basic [StateService] implementation, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
open class BasicStateService<T: ContractState>(
        private val delegate: StateServiceDelegate<T>
) : StateServiceDelegate<T> by delegate, StateService<T> {

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, contractStateType: Class<T>, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceRpcDelegate(rpcOps, contractStateType, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, contractStateType: Class<T>, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceHubDelegate(serviceHub, contractStateType, defaults))

    override val ofLinearState: Boolean by lazy { LinearState::class.java.isAssignableFrom(delegate.contractStateType) }
    override val ofQueryableState: Boolean by lazy { QueryableState::class.java.isAssignableFrom(delegate.contractStateType) }

    override fun findByLinearId(linearId: UniqueIdentifier, relevancyStatus: Vault.RelevancyStatus): StateAndRef<T>? =
            if(ofLinearState) this.queryBy(LinearStateQueryCriteria(
                    linearId = listOf(linearId),
                    relevancyStatus = relevancyStatus), 1, 1).states.firstOrNull()
            else throw IllegalStateException("Type is not a LinearState: ${delegate.contractStateType.simpleName}")

    override fun findByExternalId(externalId: String): StateAndRef<T>? = if(ofLinearState) this.queryBy(LinearStateQueryCriteria(
            externalId = listOf(externalId),
            status = Vault.StateStatus.UNCONSUMED), 1, 1).states.firstOrNull()
    else throw IllegalStateException("Type is not a LinearState: ${delegate.contractStateType.simpleName}")

    override fun countBy(criteria: QueryCriteria): Long =
            queryBy(criteria, 1, 1).totalStatesAvailable

    override fun queryBy(
            criteria: QueryCriteria,
            pageNumber: Int,
            pageSize: Int,
            sort: Sort
    ): Vault.Page<T> = queryBy(criteria, PageSpecification(pageNumber, pageSize), sort)

    override fun trackBy(
            criteria: QueryCriteria,
            pageNumber: Int,
            pageSize: Int,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> =
        this.trackBy(criteria, PageSpecification(pageNumber, pageSize), sort)

}

/**
 * Extends [BasicStateService] to provide a [StateService] aware of the target
 * [ContractState] type's [StatePersistable] and [Fields].
 *
 * Subclassed by Vaultaire's annotation processing to generate service components.
 */
abstract class ExtendedStateService<T: ContractState, P : StatePersistable, out F: Fields<P>, Q: VaultQueryCriteriaCondition<P, F>>(
        delegate: StateServiceDelegate<T>
) : BasicStateService<T>(delegate) {

    /** The type of the target state's [StatePersistable] */
    abstract val statePersistableType: Class<P>

    /** The fields of the target [StatePersistable] type `P` */
    abstract val fields: F

    /** The fields of the target [StatePersistable] type `P` */
    lateinit var criteriaConditionsType: Class<Q>

    /**
     * DSL entry point function for a [VaultQueryCriteriaCondition] of type [Q]
     */
    abstract fun buildQuery(block: Q.() -> Unit): Q

    /** Build a sort from the given string/direction pairs */
    fun toSort(vararg sort: Pair<String, Sort.Direction>): Sort =
            Sort(sort.map {
                if (!fields.contains(it.first))
                    throw java.lang.IllegalArgumentException("Canot sort on invalid field name: ${it.first}")
                Sort.SortColumn(
                        SortAttribute.Custom(statePersistableType, it.first), it.second)
            })

    /**
     * Query the vault for states of type [T] matching the given DSL query,
     * applying the given page number, size and aggregates flag
     */
    fun queryBy(
            condition: Q, pageNumber: Int = defaults.pageNumber, pageSize: Int = defaults.pageSize, ignoreAggregates: Boolean = false
    ): Vault.Page<T> {
        val criteria = condition.toCriteria(ignoreAggregates)
        val sort = condition.toSort()
        return if (sort.columns.isNotEmpty()) queryBy(criteria, PageSpecification(pageNumber, pageSize), sort)
        else queryBy(criteria, PageSpecification(pageNumber, pageSize))
    }

    /**
     * Query the vault for states of type [T] matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun queryBy(
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            vararg sort: Pair<String, Sort.Direction>
    ): Vault.Page<T> = if (sort.isNotEmpty()) queryBy(criteria, PageSpecification(pageNumber, pageSize), toSort(*sort))
    else queryBy(criteria, PageSpecification(pageNumber, pageSize))

    /**
     * Query the vault for states of type [T] matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun queryBy(
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            vararg sort: Pair<String, Sort.Direction>
    ): Vault.Page<T> = if (sort.isNotEmpty()) queryBy(criteria, paging, toSort(*sort))
    else queryBy(criteria, paging)

    /**
     * Track the vault for states of type [T] matching the given DSL query,
     * applying the given page number and size, ignoring any aggregates.
     */
    fun trackBy(
            condition: Q, pageNumber: Int = defaults.pageNumber, pageSize: Int = defaults.pageSize
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        val criteria = condition.toCriteria(true)
        val sort = condition.toSort()
        return if(sort.columns.isNotEmpty()) trackBy(criteria, PageSpecification(pageNumber, pageSize), sort)
        else trackBy(criteria, PageSpecification(pageNumber, pageSize))
    }

    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun trackBy(
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            vararg sort: Pair<String, Sort.Direction>
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> =
            if(sort.isNotEmpty()) trackBy(criteria, PageSpecification(pageNumber, pageSize), toSort(*sort))
            else trackBy(criteria, PageSpecification(pageNumber, pageSize))

    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun trackBy(
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            vararg sort: Pair<String, Sort.Direction>
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> =
            if(sort.isNotEmpty()) trackBy(criteria, paging, toSort(*sort))
            else trackBy(criteria, paging)
}
