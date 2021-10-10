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
package com.github.manosbatsis.vaultaire.util

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.dto.VaultaireDto
import com.github.manosbatsis.vaultaire.dto.VaultaireModelClientDto
import com.github.manosbatsis.vaultaire.service.dao.StateService
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.Sort
import java.util.UUID


@Suspendable
fun <T: ContractState, D> Vault.Page<T>.toResultsPage(
        service: StateService<T>,
        paging: PageSpecification,
        sort: Sort?,
        transform: (original: T, helper: StateService<T>) -> D
): ResultsPage<D> {
    // Must use old-school loops due to Suspendable
    val mappedStates = mutableListOf<D>()
    for (stateAndRef in states) {
        mappedStates.add(transform(stateAndRef.state.data, service))
    }
    return toResultsPage(mappedStates, paging, sort)
}

@Suspendable
fun <T: ContractState, D> Vault.Page<T>.toResultsPage(
        paging: PageSpecification,
        sort: Sort?,
        transform: (original: T) -> D
): ResultsPage<D> {
    // Must use old-school loops due to Suspendable
    val mappedStates = mutableListOf<D>()
    for (stateAndRef in states) {
        mappedStates.add(transform(stateAndRef.state.data))
    }
    return toResultsPage(mappedStates, paging, sort)
}

@Suspendable
private fun <D, T : ContractState> Vault.Page<T>.toResultsPage(
        mappedStates: MutableList<D>, paging: PageSpecification, sort: Sort?
): ResultsPage<D> {
    // Must use old-school loops due to Suspendable
    val mappedSortColumns = mutableMapOf<String, Sort.Direction>()
    if (sort != null) {
        for (col in sort.columns) {
            mappedSortColumns["${col.sortAttribute}"] = col.direction
        }
    }
    return ResultsPage(
            content = mappedStates,
            pageNumber = paging.pageNumber,
            pageSize = paging.pageSize,
            totalResults = totalStatesAvailable,
            sort = mappedSortColumns)
}


