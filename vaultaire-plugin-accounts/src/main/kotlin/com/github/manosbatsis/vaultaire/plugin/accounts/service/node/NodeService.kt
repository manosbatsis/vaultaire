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
import com.github.manosbatsis.vaultaire.service.node.BasicNodeService
import net.corda.core.contracts.ContractState
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub


/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
interface AccountsAwareNodeService: AccountsAwareNodeServiceDelegate {

}

/**
 * Basic [AccountsAwareNodeService] implementation
 */
open class BasicAccountsAwareNodeService(
        delegate: AccountsAwareNodeServiceDelegate
) : BasicNodeService(delegate), AccountsAwareNodeService {

    /** [NodeRpcConnection]-based constructor */
    constructor(
            nodeRpcConnection: NodeRpcConnection, defaults: ServiceDefaults = ServiceDefaults()
    ) : this(AccountsAwareNodeServiceRpcConnectionDelegate(nodeRpcConnection, defaults))

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, defaults: ServiceDefaults = ServiceDefaults()
    ) : this(AccountsAwareNodeServiceRpcDelegate(rpcOps, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, defaults: ServiceDefaults = ServiceDefaults()
    ) : this(AccountsAwareNodeServiceHubDelegate(serviceHub, defaults))
}