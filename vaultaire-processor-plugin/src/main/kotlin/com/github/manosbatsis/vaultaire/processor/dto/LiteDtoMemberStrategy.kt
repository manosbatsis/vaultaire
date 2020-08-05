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

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy.Statement
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoNameStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoTypeStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoMembersStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import javax.lang.model.element.VariableElement

open class LiteDtoMemberStrategy(
        annotatedElementInfo: AnnotatedElementInfo,
        dtoNameStrategy: DtoNameStrategy,
        dtoTypeStrategy: DtoTypeStrategy
) : SimpleDtoMembersStrategy(
        annotatedElementInfo, dtoNameStrategy, dtoTypeStrategy
) {


    override fun toTargetTypeStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {
        return if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {

            val propertyName = toPropertyName(variableElement)
            if (variableElement.isNullable()) {
                targetTypeFunctionBuilder.addStatement("val ${propertyName}Resolved = toPartyOrNull(this.$propertyName, stateService, %S)", propertyName)
                DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
            } else {
                targetTypeFunctionBuilder.addStatement("val ${propertyName}Resolved = toParty(this.$propertyName, stateService, %S)", propertyName)
                DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
            }
        } else super.toTargetTypeStatement(fieldIndex, variableElement, commaOrEmpty)
    }

    override fun toPatchStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {
        return if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            val propertyName = toPropertyName(variableElement)
            if (variableElement.isNullable()) {
                patchFunctionBuilder.addStatement("val ${propertyName}Resolved = toPartyOrDefaultNullable(this.$propertyName, original.$propertyName, stateService, %S)", propertyName)
                DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
            } else {
                patchFunctionBuilder.addStatement("val ${propertyName}Resolved = toPartyOrDefault(this.$propertyName, original.$propertyName, stateService, %S)", arrayOf(propertyName))
                DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
            }
        } else super.toPatchStatement(fieldIndex, variableElement, commaOrEmpty)
    }

    override fun getCreatorFunctionBuilder(originalTypeParameter: ParameterSpec): FunSpec.Builder {
        val creator = super.getCreatorFunctionBuilder(originalTypeParameter)
                .addAnnotation(Suspendable::class.java)
        addStateServiceParameter(creator)
        return creator
    }

    override fun getToPatchedFunctionBuilder(
            originalTypeParameter: ParameterSpec
    ): FunSpec.Builder {
        val functionBuilder = super.getToPatchedFunctionBuilder(originalTypeParameter)
                .addAnnotation(Suspendable::class.java)
        addStateServiceParameter(functionBuilder)
        return functionBuilder
    }

    override fun toCreatorStatement(
            index: Int, variableElement: VariableElement,
            propertyName: String, propertyType: TypeName,
            commaOrEmpty: String
    ): Statement? {
        return if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            DtoMembersStrategy.Statement(
                    if (variableElement.isNullable())
                        "      $propertyName = original.$propertyName?.name$commaOrEmpty"
                    else "      $propertyName = original.$propertyName.name$commaOrEmpty"
            )
        } else return super.toCreatorStatement(index, variableElement, propertyName, propertyType, commaOrEmpty)
    }

    override fun toAltConstructorStatement(
            index: Int, variableElement: VariableElement,
            propertyName: String, propertyType: TypeName,
            commaOrEmpty: String
    ): DtoMembersStrategy.Statement? {
        return if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            DtoMembersStrategy.Statement(
                    if (variableElement.isNullable())
                        "      $propertyName = original.$propertyName?.name$commaOrEmpty"
                    else "      $propertyName = original.$propertyName.name$commaOrEmpty"
            )
        } else super.toAltConstructorStatement(index, variableElement, propertyName, propertyType, commaOrEmpty)
    }

    // Create DTO alternative constructor
    override fun getAltConstructorBuilder(): FunSpec.Builder {
        val functionBuilder = super.getAltConstructorBuilder()
        addStateServiceParameter(functionBuilder)
        return functionBuilder
    }

    override fun getToTargetTypeFunctionBuilder(): FunSpec.Builder {
        val functionBuilder = super.getToTargetTypeFunctionBuilder()
                .addAnnotation(Suspendable::class.java)
        addStateServiceParameter(functionBuilder)
        return functionBuilder
    }

    open fun addStateServiceParameter(functionBuilder: FunSpec.Builder) {
        functionBuilder.addParameter(
                "stateService",
                StateService::class.java.asClassName()
                        .parameterizedBy(annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()))
    }

    override fun toPropertyTypeName(variableElement: VariableElement): TypeName {
        return if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName())
            CordaX500Name::class.java.asTypeName().copy(nullable = true)
        else super.toPropertyTypeName(variableElement)
    }

    override fun addAltConstructor(typeSpecBuilder: Builder, dtoAltConstructorBuilder: FunSpec.Builder) {
        // NO-OP
    }
}
