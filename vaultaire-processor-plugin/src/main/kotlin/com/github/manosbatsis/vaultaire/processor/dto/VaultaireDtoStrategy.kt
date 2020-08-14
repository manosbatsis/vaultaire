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
package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.kotlin.utils.ProcessingEnvironmentAware
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.CompositeDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategyComposition
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoNameStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import javax.lang.model.element.VariableElement

/** Vaultaire-specific overrides for building a DTO type spec */
open class VaultaireDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo,
        composition: DtoStrategyComposition =
                VaultaireDefaultDtoStrategyComposition(annotatedElementInfo)
) : CompositeDtoStrategy(
        annotatedElementInfo, composition
), ProcessingEnvironmentAware, AnnotatedElementInfo by annotatedElementInfo {


    override fun getFieldsToProcess(): List<VariableElement> {
        val includeParticipants = annotatedElementInfo.annotation
                .getAnnotationValue("includeParticipants").value as Boolean
        processingEnvironment.noteMessage { "\nVaultaireDtoStrategy.getFieldsToProcess, ignoreParticipants: $includeParticipants" }
        processingEnvironment.noteMessage { "\nVaultaireDtoStrategy.getFieldsToProcess, ignoreProperties: $ignoreProperties" }
        val ignored = if (includeParticipants) ignoreProperties else ignoreProperties + "participants"
        processingEnvironment.noteMessage { "\nVaultaireDtoStrategy.getFieldsToProcess, ignored: $ignored" }
        return primaryTargetTypeElementFields.filterNot { ignored.contains(it.simpleName.toString()) }
                .map {
                    processingEnvironment.noteMessage { "\nVaultaireDtoStrategy.getFieldsToProcess, includiong: ${it.simpleName}" }
                    it
                }
    }
}

/** Vaultaire-specific overrides for building a DTO type spec */
open class DefaultDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : VaultaireDtoStrategy(
        annotatedElementInfo = annotatedElementInfo,
        composition = VaultaireDefaultDtoStrategyComposition(annotatedElementInfo)
)

/** Vaultaire-specific overrides for building a "lite" DTO type spec */
class LiteDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : VaultaireDtoStrategy(
        annotatedElementInfo = annotatedElementInfo,
        composition = VaultaireLiteDtoStrategyComposition(annotatedElementInfo)
)

open class VaultaireDefaultDtoStrategyComposition(
        override val annotatedElementInfo: AnnotatedElementInfo
) : DtoStrategyComposition {

    override val dtoNameStrategy = SimpleDtoNameStrategy(annotatedElementInfo)
    override val dtoTypeStrategy = DtoTypeStrategy(annotatedElementInfo)
    override val dtoMembersStrategy = VaultaireDtoMemberStrategy(
            annotatedElementInfo, dtoNameStrategy, dtoTypeStrategy)
}

open class VaultaireLiteDtoStrategyComposition(
        override val annotatedElementInfo: AnnotatedElementInfo
) : DtoStrategyComposition {

    override val dtoNameStrategy = LiteDtoNameStrategy(annotatedElementInfo)
    override val dtoTypeStrategy = LiteDtoTypeStrategy(annotatedElementInfo)
    override val dtoMembersStrategy = LiteDtoMemberStrategy(
            annotatedElementInfo, dtoNameStrategy, dtoTypeStrategy)
}
