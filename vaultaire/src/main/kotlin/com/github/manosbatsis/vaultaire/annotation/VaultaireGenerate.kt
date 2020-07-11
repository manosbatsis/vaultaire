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
package com.github.manosbatsis.vaultaire.annotation

import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.schemas.PersistentState
import kotlin.reflect.KClass



interface VaultaireDependencyAnnotationConvention {
    val contractStateType: KClass<out ContractState>
    val persistentStateType: KClass<out PersistentState>
}

/**
 * Generate a conditions DSL and a state-specific [FieldsAwareStateService]
 * for the annotated [PersistentState] class or constructor.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireGenerate(
        val name: String = "",
        val contractStateType: KClass<out ContractState>
)

/**
 * Generate a conditions DSL and a state-specific [FieldsAwareStateService]
 * for the [PersistentState] and  [ContractState] of a project dependency.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireGenerateForDependency(
        val name: String = "",
        val contractStateType: KClass<out ContractState>,
        val persistentStateType: KClass<out PersistentState>
)


object VaultaireDtoStrategyKeys {
    const val DEFAULT = "com.github.manosbatsis.vaultaire.processor.dto.VaultaireDefaultDtoStrategy"
    const val LITE = "com.github.manosbatsis.vaultaire.processor.dto.VaultaireLiteDtoStrategy"
}

/** Generate a DTO for the annotated [ContractState] class or constructor. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireGenerateDto(
        val ignoreProperties: Array<String> = [],
        val copyAnnotationPackages: Array<String> = [],
        val strategies: Array<String> = [VaultaireDtoStrategyKeys.DEFAULT]
)

/**
 * Generate a DTO for the [ContractState] of a project dependency.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireGenerateDtoForDependency(
        val ignoreProperties: Array<String> = [],
        val contractStateType: KClass<out ContractState>,
        val persistentStateType: KClass<out PersistentState>,
        val copyAnnotationPackages: Array<String> = [],
        val strategies: Array<String> = [VaultaireDtoStrategyKeys.DEFAULT]
)

/**
 * Generate a responser flow that extends the given type.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireGenerateResponder(
        val value: KClass<out FlowLogic<*>>,
        val comment: String = ""
)

