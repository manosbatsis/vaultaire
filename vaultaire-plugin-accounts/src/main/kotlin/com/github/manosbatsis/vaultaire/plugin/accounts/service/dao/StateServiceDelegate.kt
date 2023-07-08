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
package com.github.manosbatsis.vaultaire.plugin.accounts.service.dao

import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyConnection
import com.github.manosbatsis.corda.rpc.poolboy.connection.NodeRpcConnection
import com.github.manosbatsis.vaultaire.plugin.accounts.service.node.AccountsAwareNodeCordaServiceDelegate
import com.github.manosbatsis.vaultaire.plugin.accounts.service.node.AccountsAwareNodeServiceDelegate
import com.github.manosbatsis.vaultaire.plugin.accounts.service.node.AccountsAwareNodeServicePoolBoyDelegate
import com.github.manosbatsis.vaultaire.plugin.accounts.service.node.AccountsAwareNodeServiceRpcConnectionDelegate
import com.github.manosbatsis.vaultaire.plugin.accounts.service.node.AccountsAwareNodeServiceRpcDelegate
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manosbatsis.vaultaire.service.dao.StateServiceDelegate
import net.corda.core.contracts.ContractState
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub


/** [StateService] delegate for vault operations */
interface AccountsAwareStateServiceDelegate<T : ContractState> :
        AccountsAwareNodeServiceDelegate, StateServiceDelegate<T>


open class AccountsAwareStateServicePoolBoyDelegate<T : ContractState>(
        poolBoy: PoolBoyConnection,
        override val contractStateType: Class<T>
) : AccountsAwareNodeServicePoolBoyDelegate(poolBoy), AccountsAwareStateServiceDelegate<T>

/** [CordaRPCOps]-based [StateServiceDelegate] implementation */
open class AccountsAwareStateServiceRpcDelegate<T : ContractState>(
        rpcOps: CordaRPCOps,
        override val contractStateType: Class<T>
) : AccountsAwareNodeServiceRpcDelegate(rpcOps), AccountsAwareStateServiceDelegate<T>

/** [NodeRpcConnection]-based [StateServiceDelegate] implementation */
class AccountsAwareStateServiceRpcConnectionDelegate<T : ContractState>(
        nodeRpcConnection: NodeRpcConnection,
        override val contractStateType: Class<T>
) : AccountsAwareNodeServiceRpcConnectionDelegate(nodeRpcConnection), AccountsAwareStateServiceDelegate<T>

/** [ServiceHub]-based [StateServiceDelegate] implementation */
abstract class AccountsAwareStateCordaServiceDelegate<T : ContractState>(
        serviceHub: AppServiceHub,
        override val contractStateType: Class<T>
) : AccountsAwareNodeCordaServiceDelegate(serviceHub), AccountsAwareStateServiceDelegate<T>
