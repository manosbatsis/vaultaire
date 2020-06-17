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

import com.github.manosbatsis.vaultaire.plugin.accounts.service.node.AccountsAwareNodeServiceDelegate
import com.github.manosbatsis.vaultaire.rpc.NodeRpcConnection
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.dao.StateServiceDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServiceHubDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServiceRpcConnectionDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServiceRpcDelegate
import net.corda.core.contracts.ContractState
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort


/** [StateService] delegate for vault operations */
interface NodeAwareStateServiceDelegate<T: ContractState>: AccountsAwareNodeServiceDelegate, StateServiceDelegate<T>

/** [CordaRPCOps]-based [StateServiceDelegate] implementation */
class NodeAwareStateServiceRpcDelegate<T: ContractState>(
        rpcOps: CordaRPCOps,
        contractStateType: Class<T>,
        defaults: ServiceDefaults = ServiceDefaults()
): StateServiceRpcDelegate<T>(rpcOps, contractStateType, defaults), NodeAwareStateServiceDelegate<T>

/** [NodeRpcConnection]-based [StateServiceDelegate] implementation */
class NodeAwareStateServiceRpcConnectionDelegate<T: ContractState>(
        nodeRpcConnection: NodeRpcConnection,
        contractStateType: Class<T>,
        defaults: ServiceDefaults = ServiceDefaults()
): StateServiceRpcConnectionDelegate<T>(nodeRpcConnection, contractStateType, defaults), NodeAwareStateServiceDelegate<T>

/** [ServiceHub]-based [StateServiceDelegate] implementation */
class NodeAwareStateServiceHubDelegate<T: ContractState>(
        serviceHub: ServiceHub,
        contractStateType: Class<T>,
        defaults: ServiceDefaults = ServiceDefaults()
) : StateServiceHubDelegate<T>(serviceHub, contractStateType, defaults), NodeAwareStateServiceDelegate<T>
