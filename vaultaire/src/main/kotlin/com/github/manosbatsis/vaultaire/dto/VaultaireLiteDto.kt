package com.github.manosbatsis.vaultaire.dto

import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manotbatsis.kotlin.utils.api.DtoInsufficientMappingException
import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party

/**
 * Modeled after [com.github.manosbatsis.kotlin.utils.api.Dto]
 * only bringing a [StateService] in-context for [toTargetType] and [toPatched].
 */
interface VaultaireLiteDto<T : ContractState> {
    /**
     * Create a patched copy of the given [T] instance,
     * updated using this DTO's non-null properties
     */
    fun toPatched(original: T, stateService: StateService<T>): T

    /**
     * Create an instance of [T], using this DTO's properties.
     * May throw a [DtoInsufficientMappingException]
     * if there is mot enough information to do so.
     */
    fun toTargetType(stateService: StateService<T>): T

    fun toName(party: Party?, propertyName: String = "unknown"): CordaX500Name = party?.name
            ?: throw DtoInsufficientMappingException("Required property: $propertyName was null")

    fun toNameOrNull(party: Party?): CordaX500Name? = party?.name

    fun toParty(
            partyName: CordaX500Name?,
            stateService: StateService<T>,
            propertyName: String = "unknown"
    ): Party = if(partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
                    ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
            else throw DtoInsufficientMappingException("Required property: $propertyName was null")

    fun toPartyOrNull(
            partyName: CordaX500Name?,
            stateService: StateService<T>,
            propertyName: String = "unknown"
    ): Party? = if(partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
            ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
    else null

    fun toPartyOrDefaultNullable(
            partyName: CordaX500Name?,
            defaultValue: Party?,
            stateService: StateService<T>,
            propertyName: String = "unknown"
    ): Party? = if(partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
            ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
    else defaultValue

    fun toPartyOrDefault(
            partyName: CordaX500Name?,
            defaultValue: Party?,
            stateService: StateService<T>,
            propertyName: String = "unknown"
    ): Party = if(partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
            ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
    else defaultValue ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")


}
