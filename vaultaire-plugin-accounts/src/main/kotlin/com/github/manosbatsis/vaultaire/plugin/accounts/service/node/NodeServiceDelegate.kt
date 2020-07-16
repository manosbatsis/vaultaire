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
import com.github.manosbatsis.vaultaire.rpc.NodeRpcConnection
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import com.github.manosbatsis.vaultaire.service.node.NodeCordaServiceDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeService
import com.github.manosbatsis.vaultaire.service.node.NodeServiceDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcDelegateBase
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
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.util.*


/** [NodeService] delegate for vault operations */
interface AccountsAwareNodeServiceDelegate: NodeServiceDelegate {

    companion object{
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
    ): Vault.Page<AccountInfo>{
        return findStoredAccounts(nodeIdentity, paging, sort)
    }

    /** Find accounts that are already stored locally and match the given [criteria] */
    @Suspendable
    fun findStoredAccounts(
            criteria: QueryCriteria = accountBaseCriteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): Vault.Page<AccountInfo>{
        return queryBy(AccountInfo::class.java, criteria)
    }

    /**
     * Get the account that is already stored locally
     * and matching the given [id] if found, null otherwise
     */
    @Suspendable
    fun findStoredAccountOrNull(id: UUID): StateAndRef<AccountInfo>? {
        return queryBy(
                AccountInfo::class.java,
                accountBaseCriteria.and(accountUUIDCriteria(id)),
                PageSpecification(1, 1)).states.singleOrNull()
    }
    /**
     * Get the account that is already stored locally
     * and matching the given [id]
     */
    @Suspendable
    fun findStoredAccount(id: UUID): StateAndRef<AccountInfo> =
            findStoredAccountOrNull(id)  ?: throw IllegalStateException("No well known party found matching the given id")

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
    fun findStoredAccountOrNull(name: String, host: CordaX500Name): StateAndRef<AccountInfo>? =
            findStoredAccountOrNull(name, wellKnownPartyFromX500Name(host)
                    ?: throw IllegalStateException("No well known party found matching the given X500"))
    /**
     * Get the account that is already stored locally
     * with the given [name] and [host]
     */
    @Suspendable
    fun findStoredAccount(name: String, host: Party): StateAndRef<AccountInfo> =
            findStoredAccountOrNull(name, host) ?: throw IllegalStateException("No account found matching the given name and host")

    /**
     * Get the account that is already stored locally
     * with the given [name] and [host]
     */
    @Suspendable
    fun findStoredAccount(name: String, host: CordaX500Name): StateAndRef<AccountInfo> =
            findStoredAccount(name, wellKnownPartyFromX500Name(host)
                    ?: throw IllegalStateException("No well known host found matching the given party name"))

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
    fun findStoredAccount(owningKey: PublicKey): StateAndRef<AccountInfo> =
            findStoredAccountOrNull(owningKey) ?: throw IllegalStateException("No account found matching the given public key")


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
    fun findAccount(identifier: UUID, host: CordaX500Name): AccountInfo =
            findAccountOrNull(identifier, host)
                    ?: throw IllegalStateException("No account could be resolved matching the given identifier and host")

}


/** RPC implementation base */
abstract class AccountsAwareNodeServiceRpcDelegateBase(
        defaults: SimpleServiceDefaults = SimpleServiceDefaults()
): NodeServiceRpcDelegateBase(defaults), AccountsAwareNodeServiceDelegate {

    override fun findStoredAccountOrNull(owningKey: PublicKey?): StateAndRef<AccountInfo>? =
            if(owningKey != null ) rpcOps.startFlow(::AccountInfoByKey, owningKey).returnValue.get() else null

    override fun createPublicKey(accountInfo: AccountInfo): AnonymousParty =
        rpcOps.startFlow(::RequestKeyForAccount, accountInfo).returnValue.get()

    override fun createAccount(key: String): StateAndRef<AccountInfo> =
            rpcOps.startFlow(::CreateAccount, key).returnValue.get()

    override fun findAccountOrNull(identifier: UUID, host: CordaX500Name): AccountInfo? =
        findStoredAccountOrNull(identifier)?.state?.data
                ?: requestAccount(identifier,
                        wellKnownPartyFromX500Name(host)
                                ?: error("Failed resolving well known party"))

    override fun findAccount(identifier: UUID, host: CordaX500Name): AccountInfo =
            findAccountOrNull(identifier, host)?: error("Failed resolving well known party")

    private fun requestAccount(identifier: UUID, host: Party): AccountInfo? =
        rpcOps.startFlow(::RequestAccountInfo, identifier, host).returnValue.get()

}

/** [CordaRPCOps]-based [NodeServiceDelegate] implementation */
open class AccountsAwareNodeServiceRpcDelegate(
        override val rpcOps: CordaRPCOps,
        defaults: SimpleServiceDefaults = SimpleServiceDefaults()
): AccountsAwareNodeServiceRpcDelegateBase(defaults)


/** [NodeRpcConnection]-based [NodeServiceDelegate] implementation */
open class AccountsAwareNodeServiceRpcConnectionDelegate(
        private val nodeRpcConnection: NodeRpcConnection,
        override val defaults: SimpleServiceDefaults = SimpleServiceDefaults()
): AccountsAwareNodeServiceRpcDelegateBase(defaults) {
    override val rpcOps: CordaRPCOps by lazy { nodeRpcConnection.proxy }
}

/** A [NodeServiceDelegate] with extended Corda Accounts support, implemented as a CordaServicen */
open class AccountsAwareNodeCordaServiceDelegate(
        serviceHub: AppServiceHub
) : NodeCordaServiceDelegate(serviceHub), AccountsAwareNodeServiceDelegate {

    @Suspendable
    override fun findStoredAccountOrNull(owningKey: PublicKey?): StateAndRef<AccountInfo>? =
            if(owningKey != null ) serviceHub.accountService.accountInfo(owningKey) else null

    @Suspendable
    override fun createPublicKey(accountInfo: AccountInfo): AnonymousParty =
            serviceHub.startFlow(RequestKeyForAccount(accountInfo)).returnValue.get()

    @Suspendable
    override fun createAccount(key: String): StateAndRef<AccountInfo> =
        serviceHub.accountService.createAccount(key).getOrThrow()

    @Suspendable
    override fun findAccountOrNull(identifier: UUID, host: CordaX500Name): AccountInfo?  =
            findStoredAccountOrNull(identifier)?.state?.data
                    ?: requestAccount(identifier, host)

    @Suspendable
    private fun requestAccount(identifier: UUID, host: CordaX500Name): AccountInfo? =
            this.serviceHub.startFlow(RequestAccountInfo(identifier,
                    serviceHub.identityService.wellKnownPartyFromX500Name(host)
                            ?: error("Failed resolving well known party"))).returnValue.get()

}


