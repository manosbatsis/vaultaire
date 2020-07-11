package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.VariableElement


/** Vaultaire-specific overrides for building a DTO type spec */
open class VaultaireDtoStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext,
        composition: VaultaireDtoStrategyComposition,
        val nameSuffix: String
) : CompositeDtoStrategy(
        processingEnvironment = processingEnvironment,
        dtoInputContext = dtoInputContext,
        composition = composition
){

    /**
     * Obtain fields with `VaultaireGenerateDto.ignoreProperties`
     * (or `VaultaireGenerateDtoForDependency.ignoreProperties`) filtered out
     */
    override fun getFieldsToProcess(): List<VariableElement> =
        dtoInputContext.fields

}

/** Vaultaire-specific overrides for building a DTO type spec */
open class VaultaireDefaultDtoStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext
) : VaultaireDtoStrategy(
        processingEnvironment = processingEnvironment,
        dtoInputContext = dtoInputContext,
        composition = VaultaireDefaultDtoStrategyComposition,
        nameSuffix = "Dto"
)

/** Vaultaire-specific overrides for building a "lite" DTO type spec */
open class VaultaireLiteDtoStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext
) : VaultaireDtoStrategy(
        processingEnvironment = processingEnvironment,
        dtoInputContext = dtoInputContext,
        composition = VaultaireLiteDtoStrategyComposition,
        nameSuffix = "LiteDto"
)

object VaultaireDefaultDtoStrategyComposition: VaultaireDtoStrategyComposition {
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

object VaultaireLiteDtoStrategyComposition: VaultaireDtoStrategyComposition {
    override fun dtoNameStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoNameStrategy = VaultaireLiteDtoNameStrategy(
            processingEnvironment, dtoInputContext
    )
    override fun dtoMembersStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoMembersStrategy = VaultaireLiteDtoMemberStrategy(
            processingEnvironment, dtoInputContext
    )
    override fun dtoTypeStrategy(
            processingEnvironment: ProcessingEnvironment,
            dtoInputContext: DtoInputContext
    ): DtoTypeStrategy = VaultaireLiteDtoTypeStrategy(
            processingEnvironment, dtoInputContext
    )
}
