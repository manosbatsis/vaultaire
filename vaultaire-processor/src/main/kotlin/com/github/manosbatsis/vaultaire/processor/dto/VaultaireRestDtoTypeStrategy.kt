package com.github.manosbatsis.vaultaire.processor.dto


import com.github.manosbatsis.vaultaire.dto.VaultaireDto
import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoTypeStrategy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment

class VaultaireRestDtoTypeStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext
): SimpleDtoTypeStrategy(processingEnvironment, dtoInputContext){

    override fun addSuperTypes(typeSpecBuilder: Builder) {
        typeSpecBuilder.addSuperinterface(
                VaultaireDto::class.asClassName()
                        .parameterizedBy(dtoInputContext.originalTypeName))
    }
}