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
package com.github.manosbatsis.vaultaire.service.dao

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyConnection
import com.github.manosbatsis.corda.rpc.poolboy.connection.NodeRpcConnection
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import com.github.manosbatsis.vaultaire.service.node.NodeCordaServiceDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceHubDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcConnectionDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcDelegate
import com.github.manosbatsis.vaultaire.service.node.NodeServiceRpcPoolBoyDelegate
import net.corda.core.contracts.ContractState
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.contextLogger


/** [StateService] delegate for vault operations */
interface StateServiceDelegate<T : ContractState> : NodeServiceDelegate {

    companion object {
        private val logger = contextLogger()
    }

    val contractStateType: Class<T>

    /**
     * Query the vault for [T] states matching the given criteria,
     * applying the given paging and sorting specifications if any
     */

    @Suspendable
    fun queryBy(
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): Vault.Page<T> {
        return queryBy(contractStateType, criteria, paging, sort)
    }

    /**
     * Track the vault for events of [T] states matching the given criteria,
     * applying the given paging and sorting specifications if any
     */
    @Suspendable
    fun trackBy(
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> {
        return trackBy(contractStateType, criteria, paging, sort)
    }
}

open class StateServicePoolBoyDelegate<T : ContractState>(
        poolBoy: PoolBoyConnection,
        override val contractStateType: Class<T>,
        defaults: ServiceDefaults = SimpleServiceDefaults()
) : NodeServiceRpcPoolBoyDelegate(poolBoy, defaults), StateServiceDelegate<T>

/** [CordaRPCOps]-based [StateServiceDelegate] implementation */
@Deprecated(message = "Use [com.github.manosbatsis.vaultaire.service.dao.StateServicePoolBoyDelegate] with a pool boy connection pool instead")
open class StateServiceRpcDelegate<T : ContractState>(
        rpcOps: CordaRPCOps,
        override val contractStateType: Class<T>,
        defaults: ServiceDefaults = SimpleServiceDefaults()
) : NodeServiceRpcDelegate(rpcOps, defaults), StateServiceDelegate<T>

/** [NodeRpcConnection]-based [StateServiceDelegate] implementation */
open class StateServiceRpcConnectionDelegate<T : ContractState>(
        nodeRpcConnection: NodeRpcConnection,
        override val contractStateType: Class<T>,
        defaults: ServiceDefaults = SimpleServiceDefaults()
) : NodeServiceRpcConnectionDelegate(nodeRpcConnection, defaults), StateServiceDelegate<T>

/** [ServiceHub]-based [StateServiceDelegate] implementation */
open class StateServiceHubDelegate<T : ContractState>(
        serviceHub: ServiceHub,
        override val contractStateType: Class<T>,
        defaults: ServiceDefaults = SimpleServiceDefaults()
) : NodeServiceHubDelegate(serviceHub, defaults), StateServiceDelegate<T>

/** Implementation of [StateServiceDelegate] as a CordaService */
abstract class StateCordaServiceDelegate<T : ContractState>(
        serviceHub: AppServiceHub,
        override val contractStateType: Class<T>,
        defaults: ServiceDefaults = SimpleServiceDefaults()
) : NodeCordaServiceDelegate(serviceHub), StateServiceDelegate<T>
