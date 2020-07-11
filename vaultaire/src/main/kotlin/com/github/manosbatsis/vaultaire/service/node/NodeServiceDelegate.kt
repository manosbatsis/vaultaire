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

import com.github.manosbatsis.vaultaire.rpc.NodeRpcConnection
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.SingletonSerializeAsToken


/** [NodeService] delegate for vault operations */
interface NodeServiceDelegate {

    val defaults: ServiceDefaults
    val nodeLegalName: CordaX500Name
    val nodeIdentity: Party
    val nodeIdentityCriteria: QueryCriteria.LinearStateQueryCriteria

    fun toCordaX500Name(query: String) = CordaX500Name.parse(query)

    /**
     * Returns a list of candidate matches for a given string, with optional fuzzy(ish) matching. Fuzzy matching may
     * get smarter with time e.g. to correct spelling errors, so you should not hard-code indexes into the results
     * but rather show them via a user interface and let the user pick the one they wanted.
     *
     * @param query The string to check against the X.500 name components
     * @param exactMatch If true, a case sensitive match is done against each component of each X.500 name.
     */
    fun partiesFromName(query: String, exactMatch: Boolean): Set<Party>

    /**
     * Returns a [Party] match for the given name string, trying exact and if needed fuzzy matching.
     * @param name The name to convert to a party
     */
    fun findPartyFromName(query: String): Party? =
            this.partiesFromName(query, true).firstOrNull()
                    ?: this.partiesFromName(query, true).firstOrNull()
    /**
     * Returns a [Party] match for the given [CordaX500Name] if found, null otherwise
     * @param name The name to convert to a party
     */
    fun wellKnownPartyFromX500Name(name: CordaX500Name): Party?


    /**
     * Returns a [Party] match for the given name string, trying exact and if needed fuzzy matching.
     * If not exactly one match is found an error will be thrown.
     * @param name The name to convert to a party
     */
    fun getPartyFromName(query: String): Party =
            if(query.contains("O=")) wellKnownPartyFromX500Name(CordaX500Name.parse(query))
                    ?: throw IllegalArgumentException("No party found for query treated as an x500 name: ${query}")
            else this.partiesFromName(query, true).firstOrNull()
                    ?: this.partiesFromName(query, false).single()


    fun <T: ContractState> isLinearState(contractStateType: Class<T>): Boolean =
            LinearState::class.java.isAssignableFrom(contractStateType)
    fun <T: ContractState> isQueryableState(contractStateType: Class<T>): Boolean =
            QueryableState::class.java.isAssignableFrom(contractStateType)

    /**
     * Query the vault for states matching the given criteria,
     * applying the given paging and sorting specifications if any
     */
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
    fun <T: ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>>
}


/** RPC implementation base */
abstract class NodeServiceRpcDelegateBase(
        override val defaults: ServiceDefaults = SimpleServiceDefaults()): NodeServiceDelegate {

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
    ): Vault.Page<T> = rpcOps.vaultQueryBy(criteria, paging, sort, contractStateType)

    override fun <T: ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> = rpcOps.vaultTrackBy(criteria, paging, sort, contractStateType)
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
        message = "Deprecated in favor of CordaService-based implementation",
        replaceWith = ReplaceWith("serviceHub.cordaService(NodeCordaServiceDelegate::class.java)",
        imports = ["com.github.manosbatsis.vaultaire.service.node.NodeCordaServiceDelegate"]))
open class NodeServiceHubDelegate(
        serviceHub: ServiceHub,
        override val defaults: ServiceDefaults = SimpleServiceDefaults()
): AbstractNodeServiceHubDelegate<ServiceHub>(serviceHub)

/** Implementation of [NodeServiceDelegate] as a CordaService */
@CordaService
open class NodeCordaServiceDelegate(
        serviceHub: AppServiceHub
): AbstractNodeServiceHubDelegate<AppServiceHub>(serviceHub) {

    override val defaults: ServiceDefaults = SimpleServiceDefaults()
}

/** [ServiceHub]-based [NodeServiceDelegate] implementation */
abstract class AbstractNodeServiceHubDelegate<S: ServiceHub>(
         val serviceHub: S
) : SingletonSerializeAsToken(), NodeServiceDelegate {

    override val nodeLegalName: CordaX500Name by lazy {
        nodeIdentity.name
    }

    override val nodeIdentity: Party by lazy {
        serviceHub.myInfo.legalIdentities.first()
    }

    override val nodeIdentityCriteria: QueryCriteria.LinearStateQueryCriteria by lazy {
        QueryCriteria.LinearStateQueryCriteria(participants = listOf(nodeIdentity))
    }
    override fun partiesFromName(query: String, exactMatch: Boolean): Set<Party> =
            serviceHub.identityService.partiesFromName(query, exactMatch)

    override fun wellKnownPartyFromX500Name(name: CordaX500Name): Party? =
            serviceHub.identityService.wellKnownPartyFromX500Name(name)

    override fun <T: ContractState> queryBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): Vault.Page<T> = serviceHub.vaultService.queryBy(contractStateType, criteria, paging, sort)

    override fun <T: ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> =
            serviceHub.vaultService.trackBy(contractStateType, criteria, paging, sort)
}
