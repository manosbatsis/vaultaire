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
package com.github.manosbatsis.vaultaire.processor

import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDto
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireDefaultDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireLiteDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import net.corda.core.contracts.ContractState
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * Kapt processor for generating (Corda) state-based DTOs.
 */
@SupportedAnnotationTypes(
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDto",
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
        KAPT_KOTLIN_GENERATED_OPTION_NAME,
        KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME)
class VaultaireDtoAnnotationProcessor : BaseStateInfoAnnotationProcessor() {

    override val sourcesAnnotation = VaultaireGenerateDto::class.java
    override val dependenciesAnnotation = VaultaireGenerateDtoForDependency::class.java

    val profileStrategies = mapOf(
            VaultaireDtoStrategyKeys.DEFAULT to VaultaireDefaultDtoStrategy::class.java,
            VaultaireDtoStrategyKeys.LITE to VaultaireLiteDtoStrategy::class.java
    )

    private fun getDtoStrategies(stateInfo: StateInfo): List<Class<out VaultaireDtoStrategy>> {
        return stateInfo.annotation.findAnnotationValueList("strategies")?.map {
            profileStrategies[it.value.toString()] ?: throw IllegalArgumentException("Not a valid strategy: $it")
        } ?: listOf(VaultaireDtoStrategy::class.java)
    }

    /** Write a DTO for the given [ContractState] . */
    override fun process(stateInfo: StateInfo) {
        getDtoStrategies(stateInfo).toSet().forEach {
            val copyAnnotationPackages: List<String> = getStringValuesList(stateInfo.annotation, "copyAnnotationPackages")
            val ignoredProperties: List<String> = getStringValuesList(stateInfo.annotation, "ignoreProperties")

            processingEnv.noteMessage { "Ignoring properties: $ignoredProperties" }
            val dtoInputContext =  DtoInputContext(
                    processingEnvironment,
                    stateInfo.contractStateTypeElement as TypeElement,
                    stateInfo.contractStateFields.filterNot { ignoredProperties.contains(it.simpleName.toString()) },
                    copyAnnotationPackages,
                    it)

            val dtoStrategy = dtoInputContext.dtoStrategy as VaultaireDtoStrategy
                    //.builder()
            // Generate the Kotlin file
            getFileSpecBuilder(stateInfo.generatedPackageName, "${stateInfo.contractStateTypeElement.simpleName}VaultaireGenerated${dtoStrategy.nameSuffix}")
                    .addType(dtoInputContext.builder().build())
                    .build()
                    .writeTo(sourceRootFile)
        }

    }


}


