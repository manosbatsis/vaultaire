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
package com.github.manosbatsis.vaultaire.dao

import com.github.manosbatsis.vaultaire.rpc.NodeRpcConnection
import net.corda.core.contracts.ContractState
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort


/** [StateService] delegate for vault operations */
interface StateServiceDelegate<T: ContractState> {

    val contractStateType: Class<T>
    val defaults: StateServiceDefaults

    /**
     * Query the vault for states matching the given criteria,
     * applying the given paging and sorting specifications if any
     */
    fun queryBy(
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): Vault.Page<T>

    /**
     * Track the vault for events of `T` states matching the given criteria,
     * applying the given paging and sorting specifications if any
     */
    fun trackBy(
            criteria: QueryCriteria = defaults.criteria,
            paging: PageSpecification = defaults.paging,
            sort: Sort = defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>>
}

/** [CordaRPCOps]-based [StateServiceDelegate] implementation */
class StateServiceRpcDelegate<T: ContractState>(
        private val rpcOps: CordaRPCOps,
        override val contractStateType: Class<T>,
        override val defaults: StateServiceDefaults = StateServiceDefaults()): StateServiceDelegate<T> {

    override fun queryBy(
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): Vault.Page<T> = rpcOps.vaultQueryBy(criteria, paging, sort, contractStateType)

    override fun trackBy(
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> = rpcOps.vaultTrackBy(criteria, paging, sort, contractStateType)
}

/** [NodeRpcConnection]-based [StateServiceDelegate] implementation */
class StateServiceRpcConnectionDelegate<T: ContractState>(
        private val nodeRpcConnection: NodeRpcConnection,
        override val contractStateType: Class<T>,
        override val defaults: StateServiceDefaults = StateServiceDefaults()): StateServiceDelegate<T> {

    val rpcOps: CordaRPCOps by lazy { nodeRpcConnection.proxy }

    override fun queryBy(
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): Vault.Page<T> = rpcOps.vaultQueryBy(criteria, paging, sort, contractStateType)

    override fun trackBy(
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> = rpcOps.vaultTrackBy(criteria, paging, sort, contractStateType)
}

/** [ServiceHub]-based [StateServiceDelegate] implementation */
class StateServiceHubDelegate<T: ContractState>(
        private val serviceHub: ServiceHub,
        override val contractStateType: Class<T>,
        override val defaults: StateServiceDefaults = StateServiceDefaults()) : StateServiceDelegate<T> {

    override fun queryBy(
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): Vault.Page<T> = serviceHub.vaultService.queryBy(contractStateType, criteria, paging, sort)

    override fun trackBy(
            criteria: QueryCriteria,
            paging: PageSpecification,
            sort: Sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> =
            serviceHub.vaultService.trackBy(contractStateType, criteria, paging, sort)
}
