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
package com.github.manosbatsis.vaultaire.service.dao

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyConnection
import com.github.manosbatsis.corda.rpc.poolboy.connection.NodeRpcConnection
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import com.github.manosbatsis.vaultaire.service.node.BasicNodeService
import com.github.manosbatsis.vaultaire.service.node.NodeService
import com.github.manosbatsis.vaultaire.service.node.NotFoundException
import com.github.manosbatsis.vaultaire.util.Fields
import com.github.manosbatsis.vaultaire.util.asUniqueIdentifier
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.schemas.StatePersistable
import net.corda.core.utilities.contextLogger
import java.util.UUID


/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
interface StateService<T : ContractState> :
        NodeService,
        StateServiceDelegate<T> {

    companion object {
        private val logger = contextLogger()
    }

    val ofLinearState: Boolean
    val ofQueryableState: Boolean

    /**
     * Find the state of type [T] matching the given [UUID] if any, throw an error otherwise
     * @throws NotFoundException if no match is found
     */
    @Suspendable
    fun getByLinearId(
            linearId: UUID,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL
    ): StateAndRef<T> {
        return getByLinearId(contractStateType, UniqueIdentifier(id = linearId), relevancyStatus)
    }

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any, throw an error otherwise
     * @throws NotFoundException if no match is found
     */
    @Suspendable
    fun getByLinearId(
            linearId: String,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL
    ): StateAndRef<T> {
        return getByLinearId(contractStateType, linearId.asUniqueIdentifier(), relevancyStatus)
    }

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any, throw an error otherwise
     * @throws NotFoundException if no match is found
     */
    @Suspendable
    fun getByLinearId(
            linearId: UniqueIdentifier,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL
    ): StateAndRef<T> {
        return findByLinearId(contractStateType, linearId, relevancyStatus)
                ?: throw NotFoundException(linearId.id.toString(), contractStateType)
    }

    /**
     * Find the state of type [T] matching the given [UUID] if any
     */
    @Suspendable
    fun findByLinearId(
            linearId: UUID,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL
    ): StateAndRef<T>? {
        return findByLinearId(contractStateType, UniqueIdentifier(id = linearId), relevancyStatus)
    }

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any
     */
    @Suspendable
    fun findByLinearId(
            linearId: String,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL
    ): StateAndRef<T>? {
        return findByLinearId(contractStateType, linearId.asUniqueIdentifier(), relevancyStatus)
    }

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any
     */
    @Suspendable
    fun findByLinearId(
            linearId: UniqueIdentifier,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL
    ): StateAndRef<T>? {
        return findByLinearId(contractStateType, linearId, relevancyStatus)
    }

    /**
     * Find the [Vault.StateStatus.UNCONSUMED] state of type [T]
     * matching the given [UniqueIdentifier.externalId] if any, throw an error otherwise
     * @throws NotFoundException if no match is found
     */
    @Suspendable
    fun getByExternalId(externalId: String): StateAndRef<T> {
        return findByExternalId(contractStateType, externalId)
                ?: throw NotFoundException(externalId, contractStateType)
    }

    /**
     * Find the [Vault.StateStatus.UNCONSUMED] state of type [T]
     * matching the given [UniqueIdentifier.externalId] if any
     */
    @Suspendable
    fun findByExternalId(externalId: String): StateAndRef<T>? {
        return findByExternalId(contractStateType, externalId)
    }

    /** Count states of type [T] matching stored in the vault and matching any given criteria */
    @Suspendable
    fun countBy(criteria: QueryCriteria = defaults.criteria): Long {
        return countBy(contractStateType, criteria)
    }

    /**
     * Query the vault for states of type [T] matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    @Suspendable
    fun queryBy(
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            sort: Sort = defaults.sort
    ): Vault.Page<T> {
        return queryBy(contractStateType, criteria, pageNumber, pageSize, sort)
    }

    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    @Suspendable
    fun trackBy(
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            sort: Sort = defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return trackBy(contractStateType, criteria, pageNumber, pageSize, sort)
    }
}


/**
 * Basic [StateService] implementation, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
open class BasicStateService<T : ContractState>(
        override val delegate: StateServiceDelegate<T>
) : BasicNodeService(delegate), StateServiceDelegate<T> by delegate, StateService<T> {

    /** [PoolBoyConnection]-based constructor */
    constructor(
            poolBoy: PoolBoyConnection, contractStateType: Class<T>, defaults: SimpleServiceDefaults = SimpleServiceDefaults()
    ) : this(StateServicePoolBoyDelegate(poolBoy, contractStateType, defaults))

    /** [NodeRpcConnection]-based constructor */
    @Deprecated(message = "RPC-based services should use the Pool Boy constructor instead")
    constructor(
            nodeRpcConnection: NodeRpcConnection, contractStateType: Class<T>, defaults: SimpleServiceDefaults = SimpleServiceDefaults()
    ) : this(StateServiceRpcConnectionDelegate(nodeRpcConnection, contractStateType, defaults))

    /** [CordaRPCOps]-based constructor */
    @Deprecated(message = "RPC-based services should use the Pool Boy constructor instead")
    constructor(
            rpcOps: CordaRPCOps, contractStateType: Class<T>, defaults: SimpleServiceDefaults = SimpleServiceDefaults()
    ) : this(StateServiceRpcDelegate(rpcOps, contractStateType, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, contractStateType: Class<T>, defaults: SimpleServiceDefaults = SimpleServiceDefaults()
    ) : this(StateServiceHubDelegate(serviceHub, contractStateType, defaults))


    override val ofLinearState: Boolean by lazy { isLinearState(delegate.contractStateType) }
    override val ofQueryableState: Boolean by lazy { isQueryableState(delegate.contractStateType) }


}


interface ExtendedStateService<
        T : ContractState,
        P : StatePersistable,
        out F : Fields<P>,
        Q : VaultQueryCriteriaCondition<P, F>
        > : StateServiceDelegate<T> {

    /** The type of the target state's [StatePersistable] */
    val statePersistableType: Class<P>

    /** The fields of the target [StatePersistable] type `P` */
    val fields: F

    /** The fields of the target [StatePersistable] type `P` */
    var criteriaConditionsType: Class<Q>

    /**
     * DSL entry point function for a [VaultQueryCriteriaCondition] of type [Q]
     */
    fun buildQuery(block: Q.() -> Unit): Q

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
    @Suspendable
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
    @Suspendable
    fun queryBy(
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            vararg sort: Pair<String, Sort.Direction>
    ): Vault.Page<T> {
        return if (sort.isNotEmpty()) queryBy(criteria, PageSpecification(pageNumber, pageSize), toSort(*sort))
        else queryBy(criteria, PageSpecification(pageNumber, pageSize))
    }

    /**
     * Query the vault for states of type [T] matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    @Suspendable
    fun queryBy(
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            vararg sort: Pair<String, Sort.Direction>
    ): Vault.Page<T> {
        return if (sort.isNotEmpty()) queryBy(criteria, paging, toSort(*sort))
        else queryBy(criteria, paging)
    }


    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    @Suspendable
    fun trackBy(
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            vararg sort: Pair<String, Sort.Direction>
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return if (sort.isNotEmpty()) trackBy(criteria, PageSpecification(pageNumber, pageSize), toSort(*sort))
        else trackBy(criteria, PageSpecification(pageNumber, pageSize))
    }

    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    @Suspendable
    fun trackBy(
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            vararg sort: Pair<String, Sort.Direction>
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return if (sort.isNotEmpty()) trackBy(criteria, paging, toSort(*sort))
        else trackBy(criteria, paging)
    }

    /**
     * Track the vault for states of type [T] matching the given DSL query,
     * applying the given page number and size, ignoring any aggregates.
     */
    @Suspendable
    fun trackBy(
            condition: Q, pageNumber: Int = defaults.pageNumber, pageSize: Int = defaults.pageSize
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        val criteria = condition.toCriteria(true)
        val sort = condition.toSort()
        return if (sort.columns.isNotEmpty()) trackBy(criteria, PageSpecification(pageNumber, pageSize), sort)
        else trackBy(criteria, PageSpecification(pageNumber, pageSize))
    }
}

/**
 * Extends [BasicStateService] to provide a [StateService] aware of the target
 * [ContractState] type's [StatePersistable] and [Fields].
 *
 * Subclassed by Vaultaire's annotation processing to generate service components.
 */
abstract class DefaultExtendedStateService<T : ContractState, P : StatePersistable, out F : Fields<P>, Q : VaultQueryCriteriaCondition<P, F>>(
        delegate: StateServiceDelegate<T>
) : BasicStateService<T>(delegate), ExtendedStateService<T, P, F, Q> {

    /** The fields of the target [StatePersistable] type `P` */
    override lateinit var criteriaConditionsType: Class<Q>

}
