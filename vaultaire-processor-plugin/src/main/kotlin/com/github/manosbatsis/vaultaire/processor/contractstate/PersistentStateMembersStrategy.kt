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
package com.github.manosbatsis.vaultaire.processor.contractstate

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoMembersStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoStrategyLesserComposition
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.SimpleDtoMembersStrategy
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.processor.contractstate.base.BaseStateMembersStrategy
import com.github.manosbatsis.vaultaire.processor.contractstate.base.MappedProperty
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.security.PublicKey
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter

open class PersistentStateMembersStrategy(
        rootDtoStrategy: DtoStrategyLesserComposition
) : DtoMembersStrategy, BaseStateMembersStrategy(
        rootDtoStrategy
) {

    override fun toDefaultValueExpression(variableElement: VariableElement): Pair<String, Boolean>? =
            if (rootDtoMembersStrategy.toPropertyTypeName(variableElement).isNullable) Pair("null", true) else null

    override fun addParamAndProperty(
            typeSpecBuilder: TypeSpec.Builder, baseMappedProperty: MappedProperty
    ): TypeSpec.Builder {
        persistentPropertyMapperCache.toPersistentProperties(baseMappedProperty).forEach { mappedProperty ->

            dtoConstructorBuilder.addParameter(
                    ParameterSpec.builder(mappedProperty.propertyName, mappedProperty.propertyType)
                            .apply { mappedProperty.propertyDefaults?.first?.let { defaultValue(it) } }.build()
            )
            val propertySpecBuilder = rootDtoMembersStrategy.toPropertySpecBuilder(
                    mappedProperty.fieldIndex, mappedProperty.variableElement, mappedProperty.propertyName, mappedProperty.propertyType
            )
            typeSpecBuilder.addProperty(propertySpecBuilder.build())
        }

        return typeSpecBuilder
    }

    override fun toPropertySpecModifiers(
            variableElement: VariableElement, propertyName: String, propertyType: TypeName
    ): Array<KModifier> = arrayOf(KModifier.PUBLIC)

    override fun finalize(typeSpecBuilder: TypeSpec.Builder) {
        typeSpecBuilder.primaryConstructor(dtoConstructorBuilder.build())
    }
}
