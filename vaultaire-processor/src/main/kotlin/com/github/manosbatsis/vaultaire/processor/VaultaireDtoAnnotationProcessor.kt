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
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireDefaultDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireLiteDtoStrategy
import com.github.manosbatsis.vaultaire.processor.plugins.AnnotationProcessorPluginService
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.plugins.DefaultDtoStrategyFactoryProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.plugins.DtoStrategyFactoryProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.processor.AbstractAnnotatedModelInfoProcessor
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotationProcessorBase
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion

/**
 * Kapt processor for generating (Corda) state-based DTOs.
 */
@SupportedAnnotationTypes(
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDto",
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(AnnotationProcessorBase.KAPT_OPTION_NAME_KAPT_KOTLIN_GENERATED)
class VaultaireDtoAnnotationProcessor : AbstractAnnotatedModelInfoProcessor(
        primaryTargetRefAnnotationName = "contractStateType",
        secondaryTargetRefAnnotationName = "persistentStateType"
) {

    // TODO: clean up this crap
    val profileStrategies  = mapOf(
            VaultaireDtoStrategyKeys.DEFAULT to VaultaireDefaultDtoStrategy::class.java,
            VaultaireDtoStrategyKeys.LITE to VaultaireLiteDtoStrategy::class.java
    )

    private fun getDtoStrategies(annotatedElementInfo: AnnotatedElementInfo): List<Class<out DtoStrategy>> {
        return annotatedElementInfo.annotation.findAnnotationValueList("strategies")
                ?.map {
            profileStrategies[it.value.toString()] ?: throw IllegalArgumentException("Not a valid strategy: $it")
        } ?: listOf(VaultaireDtoStrategy::class.java)
    }

    //val defaultDtoInputContextFactory: DtoInputContextFactoryProcessorPlugin
    val dtoInputContextFactory by lazy {
        AnnotationProcessorPluginService
                .forClassLoader(VaultaireDtoAnnotationProcessor::class.java.classLoader)
                .forServiceType(
                        DtoStrategyFactoryProcessorPlugin::class.java,
                        DefaultDtoStrategyFactoryProcessorPlugin()
                )
    }

    override fun processElementInfos(elementInfos: List<AnnotatedElementInfo>) =
            elementInfos.forEach{processElementInfo(it)}
    
    private fun processElementInfo(elementInfo: AnnotatedElementInfo) {
        println("processElementInfo, elementInfo: $elementInfo")
        getDtoStrategies(elementInfo).toSet().forEach {

            println("processElementInfo, strategy type: ${it.simpleName}")
            val dtoStrategy = dtoInputContextFactory
                    .buildDtoInputContext(elementInfo, it)
            println("processElementInfo, strategy: ${dtoStrategy}")
            val  dtoStrategyBuilder = dtoStrategy.dtoTypeSpecBuilder()
            // TODO: remove
            val suffix = if(dtoStrategy::class.java.simpleName.toUpperCase().contains("LITE"))
                "LiteDto"
            else "Dto"
            println("processElementInfo, suffix: ${suffix}")
            val dto = dtoStrategyBuilder.build()
            val packageName = elementInfo.generatedPackageName
            val fileName = "${elementInfo.primaryTargetTypeElement.simpleName}VaultaireGenerated${suffix}"
            println("processElementInfo, dto: ${dto}")
            println("processElementInfo, packageName: ${packageName}")
            println("processElementInfo, fileName: ${fileName}")
            println("processElementInfo, sourceRootFile: ${sourceRootFile}")
            // Generate the Kotlin file
            getFileSpecBuilder(packageName, fileName)
                    .addType(dto)
                    .build()
                    .writeTo(sourceRootFile)
        }
    }

}


