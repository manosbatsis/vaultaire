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

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.ConstructorRefsCompositeDtoStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.*
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.*
import kotlin.reflect.KFunction1


/** Used for flexible cloning of [ConstructorRefsCompositeDtoStrategy] at runtime, e.g. for views */
abstract class ConstructorRefsCompositeDtoStrategyCopycat(
        originalStrategy: ConstructorRefsCompositeDtoStrategy<*,*,*>,
        annotatedElementInfo: AnnotatedElementInfo = originalStrategy.annotatedElementInfo,
        dtoNameStrategyConstructor: KFunction1<DtoStrategyLesserComposition, DtoNameStrategy> = originalStrategy.dtoNameStrategyConstructor,
        dtoTypeStrategyConstructor: KFunction1<DtoStrategyLesserComposition, DtoTypeStrategy> = originalStrategy.dtoTypeStrategyConstructor,
        dtoMembersStrategyConstructor: KFunction1<DtoStrategyLesserComposition, DtoMembersStrategy> = originalStrategy.dtoMembersStrategyConstructor
): ConstructorRefsCompositeDtoStrategy<DtoNameStrategy, DtoTypeStrategy, DtoMembersStrategy>(
        annotatedElementInfo, dtoNameStrategyConstructor, dtoTypeStrategyConstructor, dtoMembersStrategyConstructor
){

    override fun dtoTypeSpecBuilder(): TypeSpec.Builder {
        val dtoTypeSpecBuilder = TypeSpec.classBuilder(getClassName())
        addSuperTypes(dtoTypeSpecBuilder)
        addModifiers(dtoTypeSpecBuilder)
        addKdoc(dtoTypeSpecBuilder)
        addAnnotations(dtoTypeSpecBuilder)
        addMembers(dtoTypeSpecBuilder)
        annotatedElementInfo.primaryTargetTypeElement.typeParameters.forEach {
            dtoTypeSpecBuilder.addTypeVariable(
                    TypeVariableName.invoke(it.simpleName.toString(), *it.bounds.map { it.asTypeName() }.toTypedArray()))
        }

        return dtoTypeSpecBuilder
    }

}