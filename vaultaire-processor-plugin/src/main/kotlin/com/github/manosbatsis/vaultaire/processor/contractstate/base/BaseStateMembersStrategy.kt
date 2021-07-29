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
package com.github.manosbatsis.vaultaire.processor.contractstate.base

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoMembersStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoStrategyLesserComposition
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.SimpleDtoMembersStrategy
import com.github.manosbatsis.vaultaire.annotation.PersistenrPropertyMappingMode
import com.github.manosbatsis.vaultaire.annotation.VaultaireStateProperty
import com.github.manosbatsis.vaultaire.annotation.VaultaireStateType
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.security.PublicKey
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter

abstract class BaseStateMembersStrategy(
        rootDtoStrategy: DtoStrategyLesserComposition
) : DtoMembersStrategy, SimpleDtoMembersStrategy(
        rootDtoStrategy
) {

    protected val persistentPropertyMapperCache: PersistentPropertyMapperCache by lazy {
        PersistentPropertyMapperCache(this)
    }

    abstract fun addParamAndProperty(typeSpecBuilder: TypeSpec.Builder, mappedProperty: MappedProperty): TypeSpec.Builder

    fun getPersistentMappingModes(variableElement: VariableElement): List<PersistenrPropertyMappingMode>{
        return variableElement.getAnnotation(VaultaireStateProperty::class.java)?.persistentMappingModes?.toList()
                ?: annotatedElementInfo.primaryTargetTypeElement
                        .getAnnotation(VaultaireStateType::class.java)!!.persistentMappingModes.toList()

    }

    override fun defaultMutable(): Boolean = false

    override fun toPropertyTypeName(variableElement: VariableElement): TypeName =
            variableElement.asKotlinTypeName()

    override fun addProperty(
            originalProperty: VariableElement,
            fieldIndex: Int,
            typeSpecBuilder: TypeSpec.Builder
    ): Pair<String, TypeName> {
        val propertyName = rootDtoMembersStrategy.toPropertyName(originalProperty)
        val propertyDefaults = rootDtoMembersStrategy.toDefaultValueExpression(originalProperty)
        val propertyType = rootDtoMembersStrategy
                .toPropertyTypeName(originalProperty)
                .let { propType ->
                    if (propertyDefaults != null) propType.copy(nullable = propertyDefaults.second)
                    else propType
                }

        addParamAndProperty(typeSpecBuilder, MappedProperty(
                propertyName = propertyName,
                propertyType = propertyType,
                propertyDefaults = propertyDefaults,
                fieldIndex = fieldIndex+1,
                variableElement = originalProperty))
        // TODO
        //rootDtoMembersStrategy.addPropertyAnnotations(propertySpecBuilder, originalProperty)
        //typeSpecBuilder.addProperty(propertySpecBuilder.build())
        return Pair(propertyName, propertyType)
    }

    override fun toPropertySpecBuilder(
            fieldIndex: Int, variableElement: VariableElement, propertyName: String, propertyType: TypeName
    ): PropertySpec.Builder = PropertySpec.builder(propertyName, propertyType)
            .mutable(defaultMutable())
            .addModifiers(*toPropertySpecModifiers(variableElement, propertyName, propertyType))
            .initializer(propertyName)

    abstract fun toPropertySpecModifiers(
            variableElement: VariableElement, propertyName: String, propertyType: TypeName
    ): Array<KModifier>

    fun declaresMethod(methodName: String, typeElement: TypeElement): Boolean{
        val allMembers = processingEnvironment.elementUtils.getAllMembers(typeElement)
        return ElementFilter.methodsIn(allMembers).find {
            "${it.simpleName}" == methodName && "${it.enclosingElement.simpleName}" == "${typeElement.simpleName}"
        } != null
    }



}
