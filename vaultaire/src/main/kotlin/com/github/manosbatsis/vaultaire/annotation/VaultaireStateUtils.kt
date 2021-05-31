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

import com.github.manosbatsis.vaultaire.service.dao.ExtendedStateService
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.schemas.PersistentState
import kotlin.reflect.KClass


interface VaultaireDependencyAnnotationConvention {
    val contractStateType: KClass<out ContractState>
    val persistentStateType: KClass<out PersistentState>
}

/**
 * Generate state-specific data access utilities,
 * including a Query DSL and [ExtendedStateService] variants
 * for the annotated [PersistentState] class or constructor.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireStateUtils(
        val name: String = "",
        val contractStateType: KClass<out ContractState>
)

/**
 * Generate state-specific data access utilities,
 * including a Query DSL and [ExtendedStateService] variants
 * for a target [PersistentState] and [ContractState] pair of a project dependency.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireStateUtilsMixin(
        val name: String = "",
        val contractStateType: KClass<out ContractState>,
        val persistentStateType: KClass<out PersistentState>
)


enum class VaultaireDtoStrategyKeys(val classNameSuffix: String) {
    /** For [ContractState]-based DTOs without any type conversions */
    CORDAPP_LOCAL_DTO("StateDto"),
    /** For [ContractState]-based DTOs with REST or otherwise client-friendly type conversions */
    CORDAPP_CLIENT_DTO("StateClientDto");

    override fun toString(): String {
        return this.classNameSuffix
    }

    companion object {
        fun findFromString(s: String): VaultaireDtoStrategyKeys? {
            val sUpper = s.toUpperCase()
            return VaultaireDtoStrategyKeys.values()
                    .find {
                        it.name.toUpperCase() == sUpper
                                || it.classNameSuffix.toUpperCase() == sUpper
                    }
        }

        fun getFromString(s: String): VaultaireDtoStrategyKeys = findFromString(s)
                ?: error("Could not match input $s to VaultaireDtoStrategyKeys entry")

    }

}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireViews(
        val value: Array<VaultaireView>,
        val strategies: Array<VaultaireDtoStrategyKeys> = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO],
        val nonDataClass: Boolean = false

)

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class VaultaireView(
        val name: String,
        val namedFields: Array<String> = [],
        val viewFields: Array<VaultaireViewField> = []
)

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class VaultaireViewField(
        val name: String,
        val immutable: Boolean = false,
        val nonNull: Boolean = false
)


/** Generate a REST-friendly DTO for the annotated input model class or constructor. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireModelDto(
        val ignoreProperties: Array<String> = [],
        val copyAnnotationPackages: Array<String> = [],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)

/** Generate a REST-friendly DTO for the target [baseType] input model class of a project dependency. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireModelDtoMixin(
        val ignoreProperties: Array<String> = [],
        val baseType: KClass<out Any>,
        val copyAnnotationPackages: Array<String> = [],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)

/** Generate a DTO for the annotated [ContractState] class or constructor. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireStateDto(
        val ignoreProperties: Array<String> = [],
        val copyAnnotationPackages: Array<String> = [],
        val strategies: Array<VaultaireDtoStrategyKeys> = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)

/**
 * Generate a DTO for the [ContractState] of a project dependency.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireStateDtoMixin(
        val ignoreProperties: Array<String> = [],
        val contractStateType: KClass<out ContractState>,
        val persistentStateType: KClass<out PersistentState>,
        val copyAnnotationPackages: Array<String> = [],
        val strategies: Array<VaultaireDtoStrategyKeys> = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)

/**
 * Generate a responser flow that extends the given type.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class VaultaireFlowResponder(
        val value: KClass<out FlowLogic<*>>,
        val comment: String = ""
)

