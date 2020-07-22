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
package com.github.manosbatsis.vaultaire.service.node

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.vaultaire.rpc.NodeRpcConnection
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
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
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.contextLogger
import java.util.UUID

class StateNotFoundException(id: String, stateType: Class<*>) : RuntimeException("Could not find a ${stateType.javaClass.simpleName} with id ${id}")


/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
interface NodeService: NodeServiceDelegate {

    companion object{
        private val logger = contextLogger()
    }

    /**
     * Find the state of type [T] matching the given [UUID] if any, throw an error otherwise
     * @throws StateNotFoundException if no match is found
     */
    @Suspendable
    fun <T: ContractState> getByLinearId(
            contractStateType: Class<T>,
            linearId: UUID,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
            externalIds: List<UUID> = emptyList()
    ): StateAndRef<T> {
        return getByLinearId(
                contractStateType, UniqueIdentifier(id = linearId), relevancyStatus)
    }

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any, throw an error otherwise
     * @throws StateNotFoundException if no match is found
     */
    @Suspendable
    fun <T: ContractState> getByLinearId(
            contractStateType: Class<T>,
            linearId: String,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T> {
        return getByLinearId(contractStateType, linearId.asUniqueIdentifier(), relevancyStatus)
    }

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any, throw an error otherwise
     * @throws StateNotFoundException if no match is found
     */
    @Suspendable
    fun <T: ContractState> getByLinearId(
            contractStateType: Class<T>,
            linearId: UniqueIdentifier,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T> {
        return findByLinearId(contractStateType, linearId, relevancyStatus)
                ?: throw StateNotFoundException(linearId.toString(), contractStateType)
    }

    /**
     * Find the state of type [T] matching the given [UUID] if any
     */
    @Suspendable
    fun <T: ContractState> findByLinearId(
            contractStateType: Class<T>,
            linearId: UUID,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T>? {
        return findByLinearId(contractStateType, UniqueIdentifier(id = linearId), relevancyStatus)
    }

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any
     */
    @Suspendable
    fun <T: ContractState> findByLinearId(
            contractStateType: Class<T>,
            linearId: String,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T>? {
        return findByLinearId(contractStateType, linearId.asUniqueIdentifier(), relevancyStatus)
    }

    /**
     * Find the state of type [T] matching the given [UniqueIdentifier] if any
     */
    @Suspendable
    fun <T: ContractState> findByLinearId(
            contractStateType: Class<T>,
            linearId: UniqueIdentifier,
            relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL): StateAndRef<T>?

    /**
     * Find the [Vault.StateStatus.UNCONSUMED] state of type [T]
     * matching the given [UniqueIdentifier.externalId] if any, throw an error otherwise
     * @throws StateNotFoundException if no match is found
     */
    @Suspendable
    fun <T: ContractState> getByExternalId(
            contractStateType: Class<T>, externalId: String): StateAndRef<T> {
        return findByExternalId(contractStateType, externalId)
                ?: throw StateNotFoundException(externalId, contractStateType)
    }

    /**
     * Find the [Vault.StateStatus.UNCONSUMED] state of type [T]
     * matching the given [UniqueIdentifier.externalId] if any
     */
    fun <T: ContractState> findByExternalId(
            contractStateType: Class<T>, externalId: String): StateAndRef<T>?

    /** Count states of type [T] matching stored in the vault and matching any given criteria */
    fun <T: ContractState> countBy(
            contractStateType: Class<T>, criteria: QueryCriteria = defaults.criteria): Long

    /**
     * Query the vault for states of type [T] matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun <T: ContractState> queryBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            sort: Sort = defaults.sort
    ): Vault.Page<T>

    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun <T: ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria = defaults.criteria,
            pageNumber: Int = defaults.pageNumber,
            pageSize: Int = defaults.pageSize,
            sort: Sort = defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>>
}



/**
 * Basic [NodeService] implementation
 */
open class BasicNodeService(
        open val delegate: NodeServiceDelegate
) : NodeServiceDelegate by delegate, NodeService {

    /** [NodeRpcConnection]-based constructor */
    constructor(
            nodeRpcConnection: NodeRpcConnection, defaults: SimpleServiceDefaults = SimpleServiceDefaults()
    ) : this(NodeServiceRpcConnectionDelegate(nodeRpcConnection, defaults))

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, defaults: SimpleServiceDefaults = SimpleServiceDefaults()
    ) : this(NodeServiceRpcDelegate(rpcOps, defaults))

    /** [ServiceHub]-based constructor, initiaalizes a Corda Service delegate */
    constructor(
            serviceHub: ServiceHub, defaults: SimpleServiceDefaults = SimpleServiceDefaults()
    ) : this(serviceHub.cordaService(NodeServiceHubDelegate::class.java))

    @Suspendable
    override fun <T: ContractState> findByLinearId(
            contractStateType: Class<T>, linearId: UniqueIdentifier, relevancyStatus: Vault.RelevancyStatus): StateAndRef<T>? {
        return if (isLinearState(contractStateType)) this.queryBy(contractStateType, LinearStateQueryCriteria(
                linearId = listOf(linearId),
                relevancyStatus = relevancyStatus), 1, 1).states.firstOrNull()
        else throw IllegalStateException("Type is not a LinearState: ${contractStateType.simpleName}")
    }

    @Suspendable
    override fun <T: ContractState> findByExternalId(
            contractStateType: Class<T>,
            externalId: String): StateAndRef<T>? {
        return if (isLinearState(contractStateType)) this.queryBy(contractStateType, LinearStateQueryCriteria(
                externalId = listOf(externalId),
                status = Vault.StateStatus.UNCONSUMED), 1, 1)
                .states.firstOrNull()
        else throw IllegalStateException("Type is not a LinearState: ${contractStateType.simpleName}")
    }

    @Suspendable
    override fun <T: ContractState> countBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria): Long =
            queryBy(contractStateType, criteria, 1, 1).totalStatesAvailable

    @Suspendable
    override fun <T: ContractState> queryBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            pageNumber: Int,
            pageSize: Int,
            sort: Sort
    ): Vault.Page<T> {
        return queryBy(contractStateType, criteria, PageSpecification(pageNumber, pageSize), sort)
    }

    @Suspendable
    override fun <T: ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            pageNumber: Int,
            pageSize: Int,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return this.trackBy(contractStateType, criteria, PageSpecification(pageNumber, pageSize), sort)
    }

}

