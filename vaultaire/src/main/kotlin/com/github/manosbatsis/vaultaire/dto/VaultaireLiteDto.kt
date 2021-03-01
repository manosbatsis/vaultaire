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
package com.github.manosbatsis.vaultaire.dto

import com.github.manosbatsis.kotlin.utils.api.DtoInsufficientMappingException
import com.github.manosbatsis.vaultaire.service.dao.StateService
import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party


interface VaultaireLiteDto<T : ContractState> : VaultaireBaseLiteDto<T, StateService<T>>

/**
 * Modeled after [com.github.manosbatsis.kotlin.utils.api.Dto]
 * only bringing a [StateService] in-context for [toTargetType] and [toPatched].
 */
interface VaultaireBaseLiteDto<T : ContractState, S : StateService<T>> {
    /**
     * Create a patched copy of the given [T] instance,
     * updated using this DTO's non-null properties
     */
    fun toPatched(original: T, stateService: S): T

    /**
     * Create an instance of [T], using this DTO's properties.
     * May throw a [DtoInsufficientMappingException]
     * if there is mot enough information to do so.
     */
    fun toTargetType(stateService: S): T

    fun toName(party: Party?, propertyName: String = "unknown"): CordaX500Name = party?.name
            ?: throw DtoInsufficientMappingException("Required property: $propertyName was null")

    fun toNameOrNull(party: Party?): CordaX500Name? = party?.name

    fun toParty(
            partyName: CordaX500Name?,
            stateService: S,
            propertyName: String = "unknown"
    ): Party = if (partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
            ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
    else throw DtoInsufficientMappingException("Required property: $propertyName was null")

    fun toPartyOrNull(
            partyName: CordaX500Name?,
            stateService: S,
            propertyName: String = "unknown"
    ): Party? = if (partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
            ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
    else null

    fun toPartyOrDefaultNullable(
            partyName: CordaX500Name?,
            defaultValue: Party?,
            stateService: S,
            propertyName: String = "unknown"
    ): Party? = if (partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
            ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
    else defaultValue

    fun toPartyOrDefault(
            partyName: CordaX500Name?,
            defaultValue: Party?,
            stateService: S,
            propertyName: String = "unknown"
    ): Party = if (partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
            ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
    else defaultValue
            ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")


}
