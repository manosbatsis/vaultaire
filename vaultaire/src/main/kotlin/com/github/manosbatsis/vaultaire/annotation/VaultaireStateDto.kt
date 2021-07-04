package com.github.manosbatsis.vaultaire.annotation

import net.corda.core.contracts.ContractState
import net.corda.core.schemas.PersistentState
import kotlin.reflect.KClass

/** Generate a DTO for the annotated [ContractState] class or constructor. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class VaultaireStateDto(
        val ignoreProperties: Array<String> = [],
        val copyAnnotationPackages: Array<String> = [],
        val strategies: Array<VaultaireDtoStrategyKeys> = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO, VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO],
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
        val strategies: Array<VaultaireDtoStrategyKeys> = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO, VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO],
        val views: Array<VaultaireView> = [],
        val includeParticipants: Boolean = false,
        val nonDataClass: Boolean = false
)