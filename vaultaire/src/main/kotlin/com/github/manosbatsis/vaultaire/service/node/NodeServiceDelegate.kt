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
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyConnection
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyNonPooledConnection
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyNonPooledRawRpcConnection
import com.github.manosbatsis.corda.rpc.poolboy.connection.NodeRpcConnection
import com.github.manosbatsis.vaultaire.dto.attachment.Attachment
import com.github.manosbatsis.vaultaire.dto.attachment.AttachmentReceipt
import com.github.manosbatsis.vaultaire.dto.info.ExtendedNodeInfo
import com.github.manosbatsis.vaultaire.registry.Registry
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import com.github.manosbatsis.vaultaire.service.dao.StateService
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.concurrent.doneFuture
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.AppServiceHub
import net.corda.core.node.NodeInfo
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.contextLogger
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date


/** [NodeService] delegate for vault operations */
interface NodeServiceDelegate {

    companion object {
        private val logger = contextLogger()
    }

    val defaults: ServiceDefaults
    val nodeLegalName: CordaX500Name
    val nodeIdentity: Party
    val nodeIdentityCriteria: QueryCriteria.LinearStateQueryCriteria

    /** Get information about the node in context as an [NodeInfo] */
    fun getNodeInfo(): NodeInfo

    /** Get information about the node in context as an [ExtendedNodeInfo] */
    fun getExtendedInfo(): ExtendedNodeInfo {
        val nodeInfo = getNodeInfo()
        return ExtendedNodeInfo(
                identity = nodeInfo.legalIdentities.first(),
                identities = nodeInfo.legalIdentities,
                platformVersion = nodeInfo.platformVersion,
                notaries = notaries(),
                flows = flows(),
                addresses = nodeInfo.addresses)
    }

    /** Get a list of nodes in the network, including self and notaries. */
    fun nodes(): List<Party>

    /** Returns the node's network peers, excluding self and notaries. */
    fun peers(): List<Party>
    fun serverTime(): LocalDateTime
    fun flows(): List<String>
    fun notaries(): List<Party>
    fun platformVersion(): Int
    fun identities(): List<Party>
    fun addresses(): List<NetworkHostAndPort>

    /** Retrieve the attachment matching the given secure hash from the vault  */
    fun openAttachment(hash: SecureHash): InputStream

    /** Retrieve the attachment matching the given hash string from the vault  */
    fun openAttachment(hash: String): InputStream = openAttachment(SecureHash.parse(hash))

    @Suspendable
    fun toCordaX500Name(query: String) = CordaX500Name.parse(query)

    /** Persist the given attachment to the vault  */
    fun saveAttachment(attachment: Attachment): AttachmentReceipt {
        // Upload to vault
        val hash = attachment.use {
            it.inputStream.use {
                uploadAttachment(it).toString()
            }
        }
        // Return receipt
        return AttachmentReceipt(
                date = Date(),
                hash = hash,
                files = attachment.filenames,
                // TODO: resolve identity from new API
                author = nodeIdentity.name.toString(),
                savedOriginal = attachment.original
        )
    }

    /**
     * Get a state service targeting the given `ContractState` type.
     * Default implementations assume a [Registry] has been properly initialized.
      */
    fun <T : ContractState, S : StateService<T>> createStateService(
            contractStateType: Class<T>
    ): S

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


    fun <T : ContractState> isLinearState(contractStateType: Class<T>): Boolean {
        return LinearState::class.java.isAssignableFrom(contractStateType)
    }

    fun <T : ContractState> isQueryableState(contractStateType: Class<T>): Boolean {
        return QueryableState::class.java.isAssignableFrom(contractStateType)
    }

    /**
     * Query the vault for states matching the given criteria,
     * applying the given paging and sorting specifications if any
     */
    @Suspendable
    fun <T : ContractState> queryBy(
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
    fun <T : ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>>

    /** Uploads a jar to the node, returns it's hash. */
    fun uploadAttachment(inputStream: InputStream): SecureHash

    /** Uploads a jar including metadata to the node, returns it's hash. */
    fun uploadAttachment(
            inputStream: InputStream, uploader: String, filename: String
    ): SecureHash
}


/** RPC implementation base */
open class NodeServiceRpcPoolBoyDelegate(
        val poolBoy: PoolBoyConnection,
        override val defaults: ServiceDefaults = SimpleServiceDefaults()
) : NodeServiceDelegate {

    companion object {
        private val logger = contextLogger()
    }

    override val nodeLegalName: CordaX500Name by lazy {
        nodeIdentity.name
    }

    override val nodeIdentity: Party by lazy {
        poolBoy.withConnection { connection ->
            connection.proxy.nodeInfo().legalIdentities.first()
        }
    }

    override val nodeIdentityCriteria: QueryCriteria.LinearStateQueryCriteria by lazy {
        QueryCriteria.LinearStateQueryCriteria(participants = listOf(nodeIdentity))
    }

    override fun getNodeInfo(): NodeInfo = poolBoy.withConnection { connection ->
        connection.proxy.nodeInfo()
    }

    override fun nodes(): List<Party> = poolBoy.withConnection { connection ->
        connection.proxy.networkMapSnapshot().map { it.legalIdentities.first() }
    }

    override fun peers(): List<Party> {
        val notaries = this.notaries()
        return poolBoy.withConnection { connection ->
            connection.proxy.networkMapSnapshot()
                    .filter { nodeInfo ->
                        // Filter out self and notaries
                        nodeInfo.legalIdentities
                                .find {
                                    it == nodeIdentity || notaries.contains(it)
                                } == null
                    }
                    .map { it.legalIdentities.first() }
        }
    }

    override fun partiesFromName(query: String, exactMatch: Boolean): Set<Party> =
            poolBoy.withConnection { connection ->
                connection.proxy.partiesFromName(query, exactMatch)
            }

    override fun wellKnownPartyFromX500Name(name: CordaX500Name): Party? =
            poolBoy.withConnection { connection ->
                connection.proxy.wellKnownPartyFromX500Name(name)
            }

    override fun <T : ContractState> queryBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): Vault.Page<T> {
        return poolBoy.withConnection { connection ->
            connection.proxy.vaultQueryBy(criteria, paging, sort, contractStateType)
        }
    }

    override fun <T : ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return poolBoy.withConnection { connection ->
            connection.proxy.vaultTrackBy(criteria, paging, sort, contractStateType)
        }
    }

    override fun serverTime(): LocalDateTime = poolBoy.withConnection { connection ->
        LocalDateTime.ofInstant(connection.proxy.currentNodeTime(), ZoneId.of("UTC"))
    }

    override fun addresses() = poolBoy.withConnection { connection ->
        connection.proxy.nodeInfo().addresses
    }

    override fun identities() = poolBoy.withConnection { connection ->
        connection.proxy.nodeInfo().legalIdentities
    }

    override fun platformVersion() = poolBoy.withConnection { connection ->
        connection.proxy.nodeInfo().platformVersion
    }

    override fun notaries() = poolBoy.withConnection { connection ->
        connection.proxy.notaryIdentities()
    }

    override fun flows() = poolBoy.withConnection { connection ->
        connection.proxy.registeredFlows()
    }

    override fun <T : ContractState, S : StateService<T>> createStateService(
            contractStateType: Class<T>
    ): S {
        val stateServiceType = Registry.getStateServiceType(contractStateType)
        stateServiceType!!.constructors.forEach { constructor ->
            println("createStateService constructor: ${constructor.parameterTypes.joinToString(",") { it.canonicalName } }")
        }
        return stateServiceType
                ?.getConstructor(PoolBoyConnection::class.java, ServiceDefaults::class.java)
                ?.newInstance(poolBoy, SimpleServiceDefaults()) as S?
                ?: error("No state service type found for type ${contractStateType.canonicalName}")
    }

    /** Retrieve the attachment matching the given secure hash from the vault  */
    override fun openAttachment(hash: SecureHash): InputStream {
        return poolBoy.withConnection { connection ->
             if (connection.proxy.attachmentExists(hash))
                connection.proxy.openAttachment(hash)
            else throw NotFoundException(
                    "Unable to find attachment ${hash}",
                    net.corda.core.contracts.Attachment::class.java)
        }
    }

    override fun uploadAttachment(inputStream: InputStream): SecureHash {
        return poolBoy.withConnection { connection ->
            connection.proxy.uploadAttachment(inputStream)
        }
    }

    override fun uploadAttachment(
            inputStream: InputStream, uploader: String, filename: String): SecureHash {
        return poolBoy.withConnection { connection ->
            connection.proxy.uploadAttachmentWithMetadata(inputStream, uploader, filename)
        }
    }

}

/** [CordaRPCOps]-based [NodeServiceDelegate] implementation */
@Deprecated(message = "Use [com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcPoolBoyDelegate] with a pool boy connection pool instead")
open class NodeServiceRpcDelegate(
        rpcOps: CordaRPCOps,
        defaults: ServiceDefaults = SimpleServiceDefaults()
) : NodeServiceRpcPoolBoyDelegate(PoolBoyNonPooledRawRpcConnection(rpcOps), defaults)


/** [NodeRpcConnection]-based [NodeServiceDelegate] implementation */
@Deprecated(message = "Use [com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcPoolBoyDelegate] with a pool boy connection pool instead")
open class NodeServiceRpcConnectionDelegate(
        nodeRpcConnection: NodeRpcConnection,
        override val defaults: ServiceDefaults = SimpleServiceDefaults()
) : NodeServiceRpcPoolBoyDelegate(PoolBoyNonPooledConnection(nodeRpcConnection), defaults)

/**
 * Simple [ServiceHub]-based [NodeServiceDelegate] implementation
 */

@Deprecated(message = "Use [com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcPoolBoyDelegate] with a pool boy connection pool instead")
open class NodeServiceHubDelegate(
        serviceHub: ServiceHub,
        override val defaults: ServiceDefaults = SimpleServiceDefaults()
) : AbstractNodeServiceHubDelegate<ServiceHub>(serviceHub)

/** Abstract [AppServiceHub]-based implementation of [NodeServiceDelegate] as a CordaService */
abstract class NodeCordaServiceDelegate(
        serviceHub: AppServiceHub
) : AbstractNodeServiceHubDelegate<AppServiceHub>(serviceHub) {
    override val defaults: ServiceDefaults = SimpleServiceDefaults()

}

/** [ServiceHub]-based [NodeServiceDelegate] implementation */
abstract class AbstractNodeServiceHubDelegate<S : ServiceHub>(
        val serviceHub: S
) : SingletonSerializeAsToken(), NodeServiceDelegate {

    companion object {
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

    override fun getNodeInfo(): NodeInfo = serviceHub.myInfo

    override fun nodes(): List<Party> =
            serviceHub.networkMapCache.allNodes.map { it.legalIdentities.first() }

    override fun notaries(): List<Party> =
            serviceHub.networkMapCache.notaryIdentities

    override fun peers(): List<Party> {
        val notaries = notaries()
        return serviceHub.networkMapCache.allNodes
                .filter { nodeInfo ->
                    // Filter out self and notaries
                    nodeInfo.legalIdentities.find {
                        it == nodeIdentity || notaries.contains(it)
                    } == null
                }
                .map { it.legalIdentities.first() }
    }

    override fun serverTime(): LocalDateTime =
            LocalDateTime.ofInstant(serviceHub.clock.instant(), ZoneId.of("UTC"))

    override fun flows(): List<String> =
            serviceHub.getAppContext().cordapp.allFlows.map { it.canonicalName }

    override fun platformVersion(): Int =
            serviceHub.diagnosticsService.nodeVersionInfo().platformVersion

    override fun identities(): List<Party> =
            serviceHub.myInfo.legalIdentities

    override fun addresses(): List<NetworkHostAndPort> =
            serviceHub.myInfo.addresses

    override fun openAttachment(hash: SecureHash): InputStream =
            serviceHub.attachments.openAttachment(hash)?.open()
                    ?: throw NotFoundException(
                            "Unable to find attachment ${hash}",
                            net.corda.core.contracts.Attachment::class.java)


    override fun <T : ContractState, S : StateService<T>> createStateService(contractStateType: Class<T>): S {
        return Registry.getStateServiceType(contractStateType)
                ?.getConstructor(ServiceHub::class.java)
                ?.newInstance(serviceHub) as S?
                ?: error("No state service type found for type ${contractStateType.canonicalName}")
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
    override fun <T : ContractState> queryBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): Vault.Page<T> {
        return serviceHub.vaultService.queryBy(contractStateType, criteria, paging, sort)
    }

    @Suspendable
    override fun <T : ContractState> trackBy(
            contractStateType: Class<T>,
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return serviceHub.vaultService.trackBy(contractStateType, criteria, paging, sort)
    }

    override fun uploadAttachment(inputStream: InputStream): SecureHash {
        return serviceHub.attachments.importAttachment(
                inputStream,
                // TODO: resolve identity from new API
                nodeIdentity.name.toString(),
                null)
    }

    override fun uploadAttachment(
            inputStream: InputStream,
            uploader: String,
            filename: String): SecureHash {
        return serviceHub.attachments.importAttachment(inputStream, uploader, filename)
    }

    /**
     * Will start the given flow as a subflow of the current
     * top-level [FlowLogic] if any, or using the [AppServiceHub]
     * otherwise.
     */
    @Suspendable
    fun <T> flowAwareStartFlow(flowLogic: FlowLogic<T>): CordaFuture<T> {
        val currentFlow = FlowLogic.currentTopLevel
        return if (currentFlow != null) {
            val result = currentFlow.subFlow(flowLogic)
            doneFuture(result)
        } else {
            (serviceHub as AppServiceHub).startFlow(flowLogic).returnValue
        }
    }
}
