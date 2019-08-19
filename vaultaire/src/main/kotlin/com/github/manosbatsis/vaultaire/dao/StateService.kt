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

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.DataFeed
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.schemas.QueryableState

/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
open class StateService<T: ContractState>(
        private val delegate: StateServiceDelegate<T>
) : StateServiceDelegate<T> by delegate  {

    /** [CordaRPCOps]-based constructor */
    constructor(
            rpcOps: CordaRPCOps, contractStateType: Class<T>, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceRpcDelegate(rpcOps, contractStateType, defaults))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub, contractStateType: Class<T>, defaults: StateServiceDefaults = StateServiceDefaults()
    ) : this(StateServiceHubDelegate(serviceHub, contractStateType, defaults))

    val ofLinearState: Boolean = delegate.contractStateType is LinearState
    val ofQueryableState: Boolean = delegate.contractStateType is QueryableState

    /** Find the state matching the given [UniqueIdentifier] if any */
    fun getByLinearId(
            linearId: UniqueIdentifier, relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL
    ): StateAndRef<T> =
            findByLinearId(linearId, relevancyStatus) ?: throw IllegalArgumentException("No state found with id $linearId")


    /** Find the state matching the given [UniqueIdentifier] if any */
    fun findByLinearId(
            linearId: UniqueIdentifier, relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL
    ): StateAndRef<T>? = if(ofLinearState) this.queryBy(LinearStateQueryCriteria(
            linearId = listOf(linearId),
            relevancyStatus = relevancyStatus), 1, 1).states.firstOrNull()
    else throw IllegalStateException("Type is not a LinearState: ${delegate.contractStateType.simpleName}")

    /** Count states stored in the vault and matching any given criteria */
    fun countBy(criteria: QueryCriteria = delegate.defaults.criteria): Long =
            queryBy(criteria, 1, 1).totalStatesAvailable

    /**
     * Query the vault for states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun queryBy(
            criteria: QueryCriteria = delegate.defaults.criteria,
            pageNumber: Int = delegate.defaults.pageNumber,
            pageSize: Int = delegate.defaults.pageSize,
            sort: Sort = delegate.defaults.sort
    ): Vault.Page<T> = queryBy(criteria, PageSpecification(pageNumber, pageSize), sort)


    /**
     * Track the vault for events of `T` states matching the given criteria,
     * applying the given page number, size and sorting specifications if any
     */
    fun trackBy(
            criteria: QueryCriteria = delegate.defaults.criteria,
            pageNumber: Int = delegate.defaults.pageNumber,
            pageSize: Int = delegate.defaults.pageSize,
            sort: Sort = delegate.defaults.sort
    ): DataFeed<Vault.Page<T>, Vault.Update<T>> =
        this.trackBy(criteria, PageSpecification(pageNumber, pageSize), sort)

}
