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

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.Sort.Direction
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.serialization.CordaSerializable

/**
 * A wrapper for page results. Typically used as a more
 * REST-friendly alternative to [Vault.Page] combined
 * with mapping to states or DTOs.
 */
@CordaSerializable
open class ResultsPage<T>(
        var content: List<T> = emptyList(),
        var pageNumber: Int = 1,
        var pageSize: Int = 10,
        var totalResults: Long = 0,
        var sort: Map<String, Direction> = emptyMap()
) {
    companion object{

        /** Build a ResultsPage using the states from the given [Vault.Page] */
        fun <C: ContractState> from(
                vaultPage: Vault.Page<C>,
                pageSpecification: PageSpecification,
                sort: Sort
        ) = ResultsPage<C>(
                content = vaultPage.states.map { it.state.data },
                pageSpecification = pageSpecification,
                totalResults = vaultPage.totalStatesAvailable,
                sort = sort)

        /** Build a ResultsPage by applying the [mapper] to the given [Vault.Page]s states */
        fun <C: ContractState, T> from(
                vaultPage: Vault.Page<C>,
                pageSpecification: PageSpecification,
                sort: Sort,
                mapper: (original: Iterable<StateAndRef<C>>) -> List<T>// = {it as Iterable<T>}
        ) = ResultsPage<T>(
                content = mapper(vaultPage.states),
                pageSpecification = pageSpecification,
                totalResults = vaultPage.totalStatesAvailable,
                sort = sort)
    }

    constructor(
            content: List<T> = emptyList(),
            pageSpecification: PageSpecification,
            totalResults: Long,
            sort: Sort
    ) : this(
            content = content,
            pageNumber = pageSpecification.pageNumber,
            pageSize = pageSpecification.pageSize,
            totalResults = totalResults,
            sort = sort.columns.map {
                val sortAttribute = it.sortAttribute
                val colName = when(sortAttribute){
                    is SortAttribute.Standard -> sortAttribute.attribute.toString()
                    is SortAttribute.Custom -> sortAttribute.entityStateColumnName
                    else -> sortAttribute.toString()
                }
                colName to it.direction
            }.toMap())
}
