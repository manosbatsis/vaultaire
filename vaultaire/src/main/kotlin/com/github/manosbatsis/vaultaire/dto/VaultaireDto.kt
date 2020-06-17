package com.github.manosbatsis.vaultaire.dto

import com.github.manosbatsis.vaultaire.service.dao.StateService
import net.corda.core.contracts.ContractState

/**
 * Modeled after [com.github.manosbatsis.kotlin.utils.api.Dto]
 * only bringing a [StateService] in-context for [toTargetType] and [toPatched].
 */
interface VaultaireDto<T : ContractState> {
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
}