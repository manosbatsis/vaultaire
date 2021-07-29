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
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.ConstructorRefsCompositeDtoStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoMembersStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoNameStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoStrategyLesserComposition
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoTypeStrategy
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

/** Base Vaultaire-specific class for building a DTO type spec */
abstract class BaseVaultaireDtoStrategy<N: DtoNameStrategy, T: DtoTypeStrategy, M: DtoMembersStrategy>(
        annotatedElementInfo: AnnotatedElementInfo,
        dtoNameStrategyConstructor: KFunction1<DtoStrategyLesserComposition, N>,
        dtoTypeStrategyConstructor: KFunction1<DtoStrategyLesserComposition, T>,
        dtoMembersStrategyConstructor: KFunction1<DtoStrategyLesserComposition, M>
) : ConstructorRefsCompositeDtoStrategy<N, T, M>(
        annotatedElementInfo, dtoNameStrategyConstructor, dtoTypeStrategyConstructor, dtoMembersStrategyConstructor
), ProcessingEnvironmentAware, AnnotatedElementInfo by annotatedElementInfo {

    private fun ignoreParticipants() = annotatedElementInfo.annotation
        .findAnnotationValue("includeParticipants")?.value as Boolean?
            ?: false

    override fun getFieldExcludes(): List<String> =
        super.getFieldExcludes().run {
            if (ignoreParticipants())  plusElement( "participants")
            else this
        }
}

