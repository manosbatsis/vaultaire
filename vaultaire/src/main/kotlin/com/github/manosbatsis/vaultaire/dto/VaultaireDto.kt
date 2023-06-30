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

import com.github.manosbatsis.kotlin.utils.api.Dto
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manosbatsis.vaultaire.service.node.NodeService
import com.github.manosbatsis.vaultaire.service.node.NodeServiceDelegate
import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party


/**
 * Implemented by client, REST-friendly DTOs, targeting [ContractState]  types,
 * with type conversion support using a [StateService]  for [toTargetType] and [toPatched].
 */
interface VaultaireStateClientDto<T : ContractState> : VaultaireBaseStateClientDto<T, StateService<T>>


/**
 * Base interface for client, REST-friendly DTOs, targeting [ContractState]  types,
 * with type conversion support using a [StateService]  for [toTargetType] and [toPatched].
 */
interface VaultaireBaseStateClientDto<T : ContractState, S : StateService<T>> : VaultaireBaseModelClientDto<T, S>

/**
 * Implemented by client, REST-friendly DTOs targeting model classes, i.e. non-ContractState types,
 * with type conversion support using a [NodeService]  for [toTargetType] and [toPatched].
 */
interface VaultaireModelClientDto<T : Any>: VaultaireBaseModelClientDto<T, NodeService>

/**
 * Implemented by client, REST-friendly DTOs targeting model classes, i.e. non-ContractState types,
 * with type conversion support using a [NodeService]  for [toTargetType] and [toPatched].
 */
interface VaultaireBaseModelClientDto<T : Any, in S : NodeService>: VaultaireDtoBase{

    /**
     * Create a patched copy of the given [T] instance,
     * updated using this DTO's non-null properties
     */
    fun toPatched(original: T, service: S): T

    /**
     * Create an instance of [T], using this DTO's properties.
     * May throw a [IllegalStateException]
     * if there is mot enough information to do so.
     */
    fun toTargetType(service: S): T

    fun toName(party: Party?, propertyName: String = "unknown"): CordaX500Name = party?.name
            ?: throw IllegalStateException("Required property: $propertyName was null")

    fun toNameOrNull(party: Party?): CordaX500Name? = party?.name

    fun toParty(
            partyName: CordaX500Name?,
            service: S,
            propertyName: String = "unknown"
    ): Party = if (partyName != null) service.wellKnownPartyFromX500Name(partyName)
            ?: throw IllegalStateException("Name ${partyName} not found for property: $propertyName")
    else throw IllegalStateException("Required property: $propertyName was null")

    fun toPartyOrNull(
            partyName: CordaX500Name?,
            service: S,
            propertyName: String = "unknown"
    ): Party? = if (partyName != null) service.wellKnownPartyFromX500Name(partyName)
            ?: throw IllegalStateException("Name ${partyName} not found for property: $propertyName")
    else null

    fun toPartyOrDefaultNullable(
            partyName: CordaX500Name?,
            defaultValue: Party?,
            service: S,
            propertyName: String = "unknown"
    ): Party? = if (partyName != null) service.wellKnownPartyFromX500Name(partyName)
            ?: throw IllegalStateException("Name ${partyName} not found for property: $propertyName")
    else defaultValue

    fun toPartyOrDefault(
            partyName: CordaX500Name?,
            defaultValue: Party?,
            service: S,
            propertyName: String = "unknown"
    ): Party = if (partyName != null) service.wellKnownPartyFromX500Name(partyName)
            ?: throw IllegalStateException("Name ${partyName} not found for property: $propertyName")
    else defaultValue
            ?: throw IllegalStateException("Name ${partyName} not found for property: $propertyName")


}

/** Implemented by DTOs with no support for type conversion */
interface VaultaireDto<T : Any>: VaultaireDtoBase {

    /**
     * Create a patched copy of the given [T] instance,
     * updated using this DTO's non-null properties
     */
    fun toPatched(original: T): T

    /**
     * Create an instance of [T], using this DTO's properties.
     * May throw a [IllegalStateException]
     * if there is mot enough information to do so.
     */
    fun toTargetType(): T
}

interface VaultaireDtoBase{

    companion object {
        protected const val ERR_NULL = "Required property is null: "

        inline fun errNull(fieldName: String): Nothing =
                throw IllegalArgumentException("${ERR_NULL}$fieldName")
    }


    fun errNull(fieldName: String): Nothing = Dto.errNull(fieldName)

}