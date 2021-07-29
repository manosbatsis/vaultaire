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

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.ConstructorRefsCompositeDtoStrategy
import com.github.manosbatsis.kotlin.utils.kapt.plugins.AnnotationProcessorPluginService
import com.github.manosbatsis.kotlin.utils.kapt.plugins.DtoStrategyFactoryProcessorPlugin
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotationProcessorBase
import com.github.manosbatsis.vaultaire.processor.contractstate.ContractStateStrategy
import com.github.manosbatsis.vaultaire.processor.contractstate.PersistentStateStrategy
import com.github.manosbatsis.vaultaire.processor.dto.ModelClientDtoStrategy
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion

/**
 * Kapt processor for generating (Corda) state-based DTOs.
 */
@SupportedAnnotationTypes(
        "com.github.manosbatsis.vaultaire.annotation.VaultaireStateType")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(AnnotationProcessorBase.KAPT_OPTION_NAME_KAPT_KOTLIN_GENERATED)
class VaultaireStateTypeAnnotationProcessor : AbstractVaultaireDtoAnnotationProcessor(
        primaryTargetRefAnnotationName = "baseType",
        secondaryTargetRefAnnotationName = ""
) {

    override fun getFieldNameExclusions(): Set<String> = setOf("participants")
    /** Get a list of DTO strategies to apply per annotated element */
    override fun getDtoStrategies(annotatedElementInfo: AnnotatedElementInfo): Map<String, ConstructorRefsCompositeDtoStrategy<*, *, *>> {
        val pluginServiceLoader = AnnotationProcessorPluginService.getInstance()
        val strategyKeys = listOfNotNull(
                ContractStateStrategy.STRATEGY_KEY,
                if(annotatedElementInfo.primaryTargetTypeElement.isAssignableTo(QueryableState::class.java))
                    PersistentStateStrategy.STRATEGY_KEY
                else null)
        return strategyKeys.map { strategyKey ->
            val pluginLoader = pluginServiceLoader.getPlugin(
                    DtoStrategyFactoryProcessorPlugin::class.java,
                    annotatedElementInfo, strategyKey)
            val strategy = pluginLoader.createStrategy(annotatedElementInfo, strategyKey)
                    as ConstructorRefsCompositeDtoStrategy<*, *, *>
            strategyKey to strategy
        }.toMap()
    }

}


