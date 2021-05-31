package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

/** REST friendly, i.e. "lite"  overrides for building a Vaultaire-specific DTO type spec */
open class StateClientDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : BaseVaultaireDtoStrategy<StateClientDtoNameStrategy, StateClientDtoTypeStrategy, StateClientDtoMembersStrategy>(
        annotatedElementInfo = annotatedElementInfo,
        dtoNameStrategyConstructor = ::StateClientDtoNameStrategy,
        dtoTypeStrategyConstructor = ::StateClientDtoTypeStrategy,
        dtoMembersStrategyConstructor = ::StateClientDtoMembersStrategy
){
    override fun with(annotatedElementInfo: AnnotatedElementInfo): StateClientDtoStrategy {
        return StateClientDtoStrategy(annotatedElementInfo)
    }
}