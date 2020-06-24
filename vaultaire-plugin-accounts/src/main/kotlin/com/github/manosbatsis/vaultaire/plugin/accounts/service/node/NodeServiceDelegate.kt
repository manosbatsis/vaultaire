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

import com.github.manosbatsis.vaultaire.rpc.NodeRpcConnection
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.node.NodeService
import com.github.manosbatsis.vaultaire.service.node.NodeServiceDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceHubDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcDelegateBase
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountBaseCriteria
import com.r3.corda.lib.accounts.workflows.accountHostCriteria
import com.r3.corda.lib.accounts.workflows.accountNameCriteria
import com.r3.corda.lib.accounts.workflows.accountUUIDCriteria
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub
import org.slf4j.LoggerFactory
import java.util.UUID


/** [NodeService] delegate for vault operations */
interface AccountsAwareNodeServiceDelegate: NodeServiceDelegate {

    companion object{
        val logger = LoggerFactory.getLogger(AccountsAwareNodeServiceDelegate::class.java)
    }

    fun accountsForHost(host: Party): List<StateAndRef<AccountInfo>> {
        return queryBy(AccountInfo::class.java, accountBaseCriteria.and(accountHostCriteria(host))).states
    }

    fun ourAccounts(): List<StateAndRef<AccountInfo>> {
        return accountsForHost(nodeIdentity)
    }

    fun allAccounts(): List<StateAndRef<AccountInfo>> {
        return queryBy(AccountInfo::class.java, accountBaseCriteria).states
    }

    fun accountInfo(id: UUID): StateAndRef<AccountInfo>? {
        val uuidCriteria = accountUUIDCriteria(id)
        return queryBy(AccountInfo::class.java, accountBaseCriteria.and(uuidCriteria)).states.singleOrNull()
    }

    fun accountInfo(name: String, host: Party): List<StateAndRef<AccountInfo>> {
        val nameCriteria = accountNameCriteria(name)
        val results = queryBy(
                AccountInfo::class.java,
                accountBaseCriteria
                    .and(nameCriteria)
                    .and(accountHostCriteria(host)))
                .states
        return when (results.size) {
            0 -> emptyList()
            1 -> listOf(results.single())
            else -> throw IllegalStateException("WARNING: Querying for account by name and host returned more than one account")
        }
    }

}


/** RPC implementation base */
abstract class AccountsAwareNodeServiceRpcDelegateBase(
        defaults: ServiceDefaults = ServiceDefaults()
): NodeServiceRpcDelegateBase(defaults), AccountsAwareNodeServiceDelegate

/** [CordaRPCOps]-based [NodeServiceDelegate] implementation */
open class AccountsAwareNodeServiceRpcDelegate(
        override val rpcOps: CordaRPCOps,
        defaults: ServiceDefaults = ServiceDefaults()
): AccountsAwareNodeServiceRpcDelegateBase(defaults)


/** [NodeRpcConnection]-based [NodeServiceDelegate] implementation */
open class AccountsAwareNodeServiceRpcConnectionDelegate(
        private val nodeRpcConnection: NodeRpcConnection,
        override val defaults: ServiceDefaults = ServiceDefaults()
): AccountsAwareNodeServiceRpcDelegateBase(defaults) {

    override val rpcOps: CordaRPCOps by lazy { nodeRpcConnection.proxy }

}

/** [ServiceHub]-based [NodeServiceDelegate] implementation */
open class AccountsAwareNodeServiceHubDelegate(
        serviceHub: ServiceHub,
        defaults: ServiceDefaults = ServiceDefaults()
) : NodeServiceHubDelegate(serviceHub, defaults), AccountsAwareNodeServiceDelegate


