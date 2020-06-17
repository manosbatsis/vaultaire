package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.CompositeDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoNameStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategyComposition
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoTypeStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoNameStrategy
import javax.annotation.processing.ProcessingEnvironment

/** Vaultaire-specific overrides for building a DTO type spec */
open class VaultaireDtoStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext,
        composition: DtoStrategyComposition
) : CompositeDtoStrategy(
        processingEnvironment = processingEnvironment,
        dtoInputContext = dtoInputContext,
        composition = composition
)


object VaultaireDtoStrategyComposition: DtoStrategyComposition {
    override fun dtoNameStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoNameStrategy = SimpleDtoNameStrategy(
            processingEnvironment, dtoInputContext
    )
    override fun dtoMembersStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoMembersStrategy = VaultaireDtoMemberStrategy(
            processingEnvironment, dtoInputContext
    )
    override fun dtoTypeStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoTypeStrategy = VaultaireDtoTypeStrategy(
            processingEnvironment, dtoInputContext
    )
}
object VaultaireRestDtoStrategyComposition: DtoStrategyComposition {
    override fun dtoNameStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoNameStrategy = VaultaireRestDtoNameStrategy(
            processingEnvironment, dtoInputContext
    )
    override fun dtoMembersStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoMembersStrategy = VaultaireRestDtoMemberStrategy(
            processingEnvironment, dtoInputContext
    )
    override fun dtoTypeStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoTypeStrategy = VaultaireRestDtoTypeStrategy(
            processingEnvironment, dtoInputContext
    )
}