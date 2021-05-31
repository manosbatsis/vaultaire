package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

/**
 * Overrides for building a REST-friendly DTO based on a non-[ContractState] class, i.e. a simple model,
 * to be used as FlowLogic input
 */
class ModelClientDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : BaseVaultaireDtoStrategy<ModelClientDtoNameStrategy, ModelClientDtoTypeStrategy, ModelClientDtoMembersStrategy>(
        annotatedElementInfo = annotatedElementInfo,
        dtoNameStrategyConstructor = ::ModelClientDtoNameStrategy,
        dtoTypeStrategyConstructor = ::ModelClientDtoTypeStrategy,
        dtoMembersStrategyConstructor = ::ModelClientDtoMembersStrategy
){
    companion object{
        const val STRATEGY_KEY = "ModelClientDto"
    }
    override fun with(annotatedElementInfo: AnnotatedElementInfo): ModelClientDtoStrategy {
        return ModelClientDtoStrategy(annotatedElementInfo)
    }
}