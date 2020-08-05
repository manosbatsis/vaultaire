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
package com.github.manosbatsis.vaultaire.plugin.accounts.service.node

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyConnection
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyNonPooledConnection
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyNonPooledRawRpcConnection
import com.github.manosbatsis.corda.rpc.poolboy.connection.NodeRpcConnection
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import com.github.manosbatsis.vaultaire.service.node.NodeCordaServiceDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeService
import com.github.manosbatsis.vaultaire.service.node.NodeServiceDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcPoolBoyDelegate
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountBaseCriteria
import com.r3.corda.lib.accounts.workflows.accountHostCriteria
import com.r3.corda.lib.accounts.workflows.accountNameCriteria
import com.r3.corda.lib.accounts.workflows.accountUUIDCriteria
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByKey
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestAccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.accountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.util.UUID


/** [NodeService] delegate for vault operations */
interface AccountsAwareNodeServiceDelegate : NodeServiceDelegate {

    companion object {
        val logger = LoggerFactory.getLogger(AccountsAwareNodeServiceDelegate::class.java)
    }

    /** Find accounts that are already stored locally and hosted by the node matching the given [host] */
    @Suspendable
    fun findStoredAccounts(
            host: Party,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): Vault.Page<AccountInfo> {
        return queryBy(AccountInfo::class.java, accountBaseCriteria.and(accountHostCriteria(host)), paging, sort)
    }

    /** Find accounts that belong to the host (node) in context */
    @Suspendable
    fun findHostedAccounts(
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): Vault.Page<AccountInfo> {
        return findStoredAccounts(nodeIdentity, paging, sort)
    }

    /** Find accounts that are already stored locally and match the given [criteria] */
    @Suspendable
    fun findStoredAccounts(
            criteria: QueryCriteria = accountBaseCriteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): Vault.Page<AccountInfo> {
        return queryBy(AccountInfo::class.java, criteria)
    }

    /**
     * Get the account that is already stored locally
     * and matching the given [id] if found, null otherwise
     */
    @Suspendable
    fun findStoredAccountOrNull(id: UUID): StateAndRef<AccountInfo>? {
        val results = queryBy(
                AccountInfo::class.java,
                accountBaseCriteria.and(accountUUIDCriteria(id)),
                PageSpecification(1, 1))
        return results.states.singleOrNull()
    }

    /**
     * Get the account that is already stored locally
     * and matching the given [id]
     */
    @Suspendable
    fun findStoredAccount(id: UUID): StateAndRef<AccountInfo> {
        return findStoredAccountOrNull(id)
                ?: throw IllegalStateException("No well known party found matching the given id")
    }

    /**
     * Get the account that is already stored locally
     * with the given [name] and [host] if found, null otherwise
     */
    @Suspendable
    fun findStoredAccountOrNull(name: String, host: Party): StateAndRef<AccountInfo>? {
        val nameCriteria = accountNameCriteria(name)
        val results = queryBy(
                AccountInfo::class.java,
                accountBaseCriteria
                        .and(nameCriteria)
                        .and(accountHostCriteria(host)),
                PageSpecification(1, 1))
        return when (results.totalStatesAvailable.toInt()) {
            0 -> null
            1 -> results.states.single()
            else -> throw IllegalStateException("Query for account by name and host returned multiple results")
        }
    }

    /**
     * Get the account that is already stored locally
     * with the given [name] and [host] if found, null otherwise
     */
    @Suspendable
    fun findStoredAccountOrNull(name: String, host: CordaX500Name): StateAndRef<AccountInfo>? {
        return findStoredAccountOrNull(name, wellKnownPartyFromX500Name(host)
                ?: throw IllegalStateException("No well known party found matching the given X500"))
    }

    /**
     * Get the account that is already stored locally
     * with the given [name] and [host]
     */
    @Suspendable
    fun findStoredAccount(name: String, host: Party): StateAndRef<AccountInfo> {
        return findStoredAccountOrNull(name, host)
                ?: throw IllegalStateException("No account found matching the given name and host")
    }

    /**
     * Get the account that is already stored locally
     * with the given [name] and [host]
     */
    @Suspendable
    fun findStoredAccount(name: String, host: CordaX500Name): StateAndRef<AccountInfo> {
        return findStoredAccount(name, wellKnownPartyFromX500Name(host)
                ?: throw IllegalStateException("No well known host found matching the given party name"))
    }

    /** Create a public key for the given [accountInfo] */
    @Suspendable
    fun createPublicKey(accountInfo: AccountInfo): AnonymousParty

    /** Create a Corda Account using the current node as the host */
    @Suspendable
    fun createAccount(key: String): StateAndRef<AccountInfo>

    /**
     * Get the account that is already stored locally
     * and matching the optional [owningKey] if found, null otherwise
     */
    @Suspendable
    fun findStoredAccountOrNull(owningKey: PublicKey?): StateAndRef<AccountInfo>?

    /**
     * Get the account that is already stored locally
     * and matching the given [owningKey]
     */
    @Suspendable
    fun findStoredAccount(owningKey: PublicKey): StateAndRef<AccountInfo> {
        return findStoredAccountOrNull(owningKey)
                ?: throw IllegalStateException("No account found matching the given public key")
    }


    /**
     * Get the account that is already stored locally
     * and matching the optional [owningKey] if found,
     * request from host otherwise.
     */
    @Suspendable
    fun findAccountOrNull(identifier: UUID, host: CordaX500Name): AccountInfo?

    /**
     * Get the account that is already stored locally
     * and matching the optional [owningKey] if found,
     * request from host otherwise
     */
    @Suspendable
    fun findAccount(identifier: UUID, host: CordaX500Name): AccountInfo {
        return findAccountOrNull(identifier, host)
                ?: throw IllegalStateException("No account could be resolved matching the given identifier and host")
    }

    @Suspendable
    fun requestAccount(identifier: UUID, host: Party): AccountInfo?

    @Suspendable
    fun toParty(owningKey: PublicKey): Party
}

/** RPC implementation base that uses a [PoolBoyConnection] RPC connection pool */
open class AccountsAwareNodeServicePoolBoyDelegate(
        poolBoy: PoolBoyConnection,
        defaults: SimpleServiceDefaults = SimpleServiceDefaults()
) : NodeServiceRpcPoolBoyDelegate(poolBoy, defaults), AccountsAwareNodeServiceDelegate {

    override fun findStoredAccountOrNull(owningKey: PublicKey?): StateAndRef<AccountInfo>? {
        return if (owningKey != null) poolBoy.withConnection { connection ->
            connection.proxy.startFlow(::AccountInfoByKey, owningKey).returnValue.get()
        }
        else null
    }

    override fun createPublicKey(accountInfo: AccountInfo): AnonymousParty =
            poolBoy.withConnection { connection ->
                connection.proxy.startFlow(::RequestKeyForAccount, accountInfo).returnValue.get()
            }


    override fun createAccount(key: String): StateAndRef<AccountInfo> =
            poolBoy.withConnection { connection ->
                connection.proxy.startFlow(::CreateAccount, key).returnValue.get()
            }

    override fun findAccountOrNull(identifier: UUID, host: CordaX500Name): AccountInfo? {
        var account = findStoredAccountOrNull(identifier)?.state?.data
        if (account == null) {
            val host = wellKnownPartyFromX500Name(host)
                    ?: error("Failed resolving well known party")
            account = requestAccount(identifier, host)
        }
        return account
    }

    override fun findAccount(identifier: UUID, host: CordaX500Name): AccountInfo =
            findAccountOrNull(identifier, host) ?: error("Failed resolving well known party")

    override fun requestAccount(identifier: UUID, host: Party): AccountInfo? =
            poolBoy.withConnection { connection ->
                connection.proxy.startFlow(::RequestAccountInfo, identifier, host).returnValue.get()
            }

    override fun toParty(owningKey: PublicKey): Party {
        return poolBoy.withConnection { connection ->
            connection.proxy.partyFromKey(owningKey)
        }
        ?: throw IllegalStateException("Could not deanonymise party ${owningKey.toStringShort()}")
    }
}

/** [CordaRPCOps]-based [NodeServiceDelegate] implementation */
@Deprecated(message = "Use [AccountsAwareNodeServicePoolBoyDelegate] with a pool boy connection pool instead")
open class AccountsAwareNodeServiceRpcDelegate(
        rpcOps: CordaRPCOps,
        defaults: SimpleServiceDefaults = SimpleServiceDefaults()
) : AccountsAwareNodeServicePoolBoyDelegate(PoolBoyNonPooledRawRpcConnection(rpcOps), defaults)


/** [NodeRpcConnection]-based [NodeServiceDelegate] implementation */
@Deprecated(message = "Use [AccountsAwareNodeServicePoolBoyDelegate] with a pool boy connection pool instead")
open class AccountsAwareNodeServiceRpcConnectionDelegate(
        nodeRpcConnection: NodeRpcConnection,
        override val defaults: SimpleServiceDefaults = SimpleServiceDefaults()
) : AccountsAwareNodeServicePoolBoyDelegate(PoolBoyNonPooledConnection(nodeRpcConnection), defaults)

/** A [NodeServiceDelegate] with extended Corda Accounts support, implemented as a CordaServicen */
open class AccountsAwareNodeCordaServiceDelegate(
        serviceHub: AppServiceHub
) : NodeCordaServiceDelegate(serviceHub), AccountsAwareNodeServiceDelegate {
    companion object {
        val logger = contextLogger()
    }

    @Suspendable
    override fun findStoredAccountOrNull(owningKey: PublicKey?): StateAndRef<AccountInfo>? {
        return if (owningKey != null) serviceHub.accountService.accountInfo(owningKey) else null
    }

    @Suspendable
    override fun toParty(owningKey: PublicKey): Party {
        return serviceHub.identityService
                .requireWellKnownPartyFromAnonymous(AnonymousParty(owningKey))
    }

    @Suspendable
    override fun createPublicKey(accountInfo: AccountInfo): AnonymousParty {
        val requestAccountKeyFlow = RequestKeyForAccount(accountInfo)
        val future = flowAwareStartFlow(requestAccountKeyFlow)
        logger.debug("createPublicKey, return value")
        return future.get()
    }

    @Suspendable
    override fun createAccount(key: String): StateAndRef<AccountInfo> {
        val account = serviceHub.accountService.createAccount(key)
        return account.getOrThrow()
    }

    @Suspendable
    override fun findAccountOrNull(identifier: UUID, host: CordaX500Name): AccountInfo? {
        logger.debug("findAccountOrNull, identifier: $identifier, host: $host")
        val partyHost = serviceHub.identityService.wellKnownPartyFromX500Name(host)
                ?: throw IllegalArgumentException("Could not map name to party: ${host}")
        logger.debug("findAccountOrNull, partyHost: $partyHost")
        var account = findStoredAccountOrNull(identifier)?.state?.data
        logger.debug("findAccountOrNull, stored account: $partyHost")
        if (account == null) {
            logger.debug("findAccountOrNull, requesting account...")
            account = requestAccount(identifier, partyHost)
            logger.debug("findAccountOrNull, requested account: $partyHost")
        }
        logger.debug("findAccountOrNull, returns: $account")
        return account
    }

    @Suspendable
    override fun requestAccount(identifier: UUID, host: Party): AccountInfo? {
        logger.debug("requestAccount, identifier: $identifier, host: ${host.name}")
        val requestAccountInfoFlow = RequestAccountInfo(identifier, host)
        val future = flowAwareStartFlow(requestAccountInfoFlow)
        logger.debug("requestAccount, return value")
        return future.get()
    }
}
