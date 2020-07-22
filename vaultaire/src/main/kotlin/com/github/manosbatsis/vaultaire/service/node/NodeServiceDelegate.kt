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
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.concurrent.doneFuture
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.contextLogger


/** [NodeService] delegate for vault operations */
interface NodeServiceDelegate {

    companion object{
        private val logger = contextLogger()
    }

    val defaults: ServiceDefaults
    val nodeLegalName: CordaX500Name
    val nodeIdentity: Party
    val nodeIdentityCriteria: QueryCriteria.LinearStateQueryCriteria

    @Suspendable
    fun toCordaX500Name(query: String) = CordaX500Name.parse(query)

    /**
     * Returns a list of candidate matches for a given string, with optional fuzzy(ish) matching. Fuzzy matching may
     * get smarter with time e.g. to correct spelling errors, so you should not hard-code indexes into the results
     * but rather show them via a user interface and let the user pick the one they wanted.
     *
     * @param query The string to check against the X.500 name components
     * @param exactMatch If true, a case sensitive match is done against each component of each X.500 name.
     */
    @Suspendable
    fun partiesFromName(query: String, exactMatch: Boolean): Set<Party>

    /**
     * Returns a [Party] match for the given name string, trying exact and if needed fuzzy matching.
     * @param name The name to convert to a party
     */
    @Suspendable
    fun findPartyFromName(query: String): Party? {
        return this.partiesFromName(query, true).firstOrNull()
                ?: this.partiesFromName(query, true).firstOrNull()
    }

    /**
     * Returns a [Party] match for the given [CordaX500Name] if found, null otherwise
     * @param name The name to convert to a party
     */
    @Suspendable
    fun wellKnownPartyFromX500Name(name: CordaX500Name): Party?


    /**
     * Returns a [Party] match for the given name string, trying exact and if needed fuzzy matching.
     * If not exactly one match is found an error will be thrown.
     * @param name The name to convert to a party
     */
    @Suspendable
    fun getPartyFromName(query: String): Party {
        return if (query.contains("O=")) wellKnownPartyFromX500Name(CordaX500Name.parse(query))
                ?: throw IllegalArgumentException("No party found for query treated as an x500 name: ${query}")
        else this.partiesFromName(query, true).firstOrNull()
                ?: this.partiesFromName(query, false).single()
    }


    fun <T: ContractState> isLinearState(contractStateType: Class<T>): Boolean {
        return LinearState::class.java.isAssignableFrom(contractStateType)
    }

    fun <T: ContractState> isQueryableState(contractStateType: Class<T>): Boolean {
        return QueryableState::class.java.isAssignableFrom(contractStateType)
    }

    /**
     * Query the vault for states matching the given criteria,
     * applying the given paging and sorting specifications if any
     */
    @Suspendable
    fun <T: ContractState> queryBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): Vault.Page<T>

    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given paging and sorting specifications if any
     */
    @Suspendable
    fun <T: ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>>
}


/** RPC implementation base */
abstract class NodeServiceRpcDelegateBase(
        override val defaults: ServiceDefaults = SimpleServiceDefaults()
): NodeServiceDelegate {

    companion object{
        private val logger = contextLogger()
    }

    abstract val rpcOps: CordaRPCOps

    override val nodeLegalName: CordaX500Name by lazy {
        nodeIdentity.name
    }

    override val nodeIdentity: Party by lazy {
        rpcOps.nodeInfo().legalIdentities.first()
    }

    override val nodeIdentityCriteria: QueryCriteria.LinearStateQueryCriteria by lazy {
        QueryCriteria.LinearStateQueryCriteria(participants = listOf(nodeIdentity))
    }

    override fun partiesFromName(query: String, exactMatch: Boolean): Set<Party> =
            rpcOps.partiesFromName(query, exactMatch)

    override fun wellKnownPartyFromX500Name(name: CordaX500Name): Party? =
            rpcOps.wellKnownPartyFromX500Name(name)

    override fun <T: ContractState> queryBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): Vault.Page<T> {
        return rpcOps.vaultQueryBy(criteria, paging, sort, contractStateType)
    }

    override fun <T: ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return rpcOps.vaultTrackBy(criteria, paging, sort, contractStateType)
    }
}
/** [CordaRPCOps]-based [NodeServiceDelegate] implementation */
open class NodeServiceRpcDelegate(
        override val rpcOps: CordaRPCOps,
        defaults : ServiceDefaults = SimpleServiceDefaults()
): NodeServiceRpcDelegateBase(defaults) {
}

/** [NodeRpcConnection]-based [NodeServiceDelegate] implementation */
open class NodeServiceRpcConnectionDelegate(
        private val nodeRpcConnection: NodeRpcConnection,
        override val defaults: SimpleServiceDefaults = SimpleServiceDefaults()
): NodeServiceRpcDelegateBase(defaults) {

    override val rpcOps: CordaRPCOps by lazy { nodeRpcConnection.proxy }

}

/**
 * Simple [ServiceHub]-based [NodeServiceDelegate] implementation
 */
@Deprecated(
        message = "Deprecated",
        replaceWith = ReplaceWith(
                "NodeCordaServiceDelegate(AppServiceHub)"
        )
)
open class NodeServiceHubDelegate(
        serviceHub: ServiceHub,
        override val defaults: ServiceDefaults = SimpleServiceDefaults()
): AbstractNodeServiceHubDelegate<ServiceHub>(serviceHub)

/** Abstract [AppServiceHub]-based implementation of [NodeServiceDelegate] as a CordaService */
abstract class NodeCordaServiceDelegate(
        serviceHub: AppServiceHub
): AbstractNodeServiceHubDelegate<AppServiceHub>(serviceHub) {
    override val defaults: ServiceDefaults = SimpleServiceDefaults()

    /**
     * Will start the given flow as a subflow of the current
     * top-level [FlowLogic] if any, or using the [AppServiceHub]
     * otherwise.
     */
    @Suspendable
    protected fun <T> flowAwareStartFlow(flowLogic: FlowLogic<T>): CordaFuture<T> {
        val currentFlow = FlowLogic.currentTopLevel
        return if (currentFlow != null) {
            val result = currentFlow.subFlow(flowLogic)
            doneFuture(result)
        } else {
            serviceHub.startFlow(flowLogic).returnValue
        }
    }
}

/** [ServiceHub]-based [NodeServiceDelegate] implementation */
abstract class AbstractNodeServiceHubDelegate<S: ServiceHub>(
         val serviceHub: S
) : SingletonSerializeAsToken(), NodeServiceDelegate {

    companion object{
        private val logger = contextLogger()
    }

    override val nodeLegalName: CordaX500Name by lazy {
        nodeIdentity.name
    }

    override val nodeIdentity: Party by lazy {
        serviceHub.myInfo.legalIdentities.first()
    }

    override val nodeIdentityCriteria: QueryCriteria.LinearStateQueryCriteria by lazy {
        QueryCriteria.LinearStateQueryCriteria(participants = listOf(nodeIdentity))
    }

    @Suspendable
    override fun partiesFromName(query: String, exactMatch: Boolean): Set<Party> {
        return serviceHub.identityService.partiesFromName(query, exactMatch)
    }

    @Suspendable
    override fun wellKnownPartyFromX500Name(name: CordaX500Name): Party? {
        return serviceHub.identityService.wellKnownPartyFromX500Name(name)
    }

    @Suspendable
    override fun <T: ContractState> queryBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): Vault.Page<T> {
        return serviceHub.vaultService.queryBy(contractStateType, criteria, paging, sort)
    }

    @Suspendable
    override fun <T: ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return serviceHub.vaultService.trackBy(contractStateType, criteria, paging, sort)
    }

}
