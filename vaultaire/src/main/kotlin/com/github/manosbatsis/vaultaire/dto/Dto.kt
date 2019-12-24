package com.github.manosbatsis.vaultaire.dto

import com.github.manosbatsis.vaultaire.util.DtoInsufficientStateMappingException
import net.corda.core.contracts.ContractState

interface Dto<T: ContractState> {
    /**
     * Create a patched copy of the given [T] instance,
     * updated using this DTO's non-null properties
     */
    fun toPatched(original:T): T

    /**
     * Create an instance of [T], using this DTO's properties.
     * May throw a [DtoInsufficientStateMappingException]
     * if there is mot enough information to do so.
     */
    fun toState(): T
}