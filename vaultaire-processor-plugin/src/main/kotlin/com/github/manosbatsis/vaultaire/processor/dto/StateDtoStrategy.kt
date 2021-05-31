package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

/** Default overrides for building a Vaultaire-specific DTO type spec */
open class StateDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : BaseVaultaireDtoStrategy<StateDtoNameStrategy, StateDtoTypeStrategy, StateDtoMembersStrategy>(
        annotatedElementInfo = annotatedElementInfo,
        dtoNameStrategyConstructor = ::StateDtoNameStrategy,
        dtoTypeStrategyConstructor = ::StateDtoTypeStrategy,
        dtoMembersStrategyConstructor = ::StateDtoMembersStrategy
){
    override fun with(annotatedElementInfo: AnnotatedElementInfo): StateDtoStrategy {
        return StateDtoStrategy(annotatedElementInfo)
    }
}