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
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.*
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoMembersStrategy.Statement
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.util.FieldContext
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec.Builder
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass


open class StateClientDtoMembersStrategy(
        rootDtoStrategy: DtoStrategyLesserComposition
): ClientDtoMembersStrategyBase(rootDtoStrategy)

open class ClientDtoMembersStrategyBase(
        rootDtoStrategy: DtoStrategyLesserComposition
) : SimpleDtoMembersStrategy(rootDtoStrategy) {

    override fun toTargetTypeStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val propertyName = toPropertyName(variableElement)
        val partyCollection = partyCollection(variableElement)

        val maybeNullFallback = maybeCheckForNull(variableElement, assignmentCtxForToTargetType(propertyName) )

        return if (partyCollection != null) {
            targetTypeFunctionBuilder.addStatement("val ${propertyName}Resolved = $propertyName?.filterNotNull()", propertyName)
            targetTypeFunctionBuilder.addStatement("     ?.mapNotNull{ toPartyOrNull(it, service, %S) } ", propertyName)
            targetTypeFunctionBuilder.addStatement("     ")
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved${maybeNullFallback.fallbackValue}$commaOrEmpty", maybeNullFallback.fallbackArgs)
        }else if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            targetTypeFunctionBuilder.addStatement("val ${propertyName}Resolved = toPartyOrNull(this.$propertyName, service, %S)", propertyName)
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved${maybeNullFallback.fallbackValue}$commaOrEmpty", maybeNullFallback.fallbackArgs)
        } else super.toTargetTypeStatement(fieldIndex, variableElement, commaOrEmpty)
    }

    override fun toPatchStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val propertyName = toPropertyName(variableElement)
        val partyCollection = partyCollection(variableElement)

        val maybeNullFallback = maybeCheckForNull(variableElement, assignmentCtxForToPatched(propertyName) )
        return if (partyCollection != null) {
            patchFunctionBuilder.addStatement("val ${propertyName}Resolved = $propertyName?.filterNotNull()", propertyName)
            patchFunctionBuilder.addStatement("     ?.mapNotNull{ toPartyOrNull(it, service, %S) } ", propertyName)
            patchFunctionBuilder.addStatement("     ?.let{ if(it.isNotEmpty()) it else null } ")
            patchFunctionBuilder.addStatement("     ${maybeNullFallback.fallbackValue}", maybeNullFallback.fallbackArgs)
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
        }
        else if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            patchFunctionBuilder.addStatement("val ${propertyName}Resolved = toPartyOrDefault(this.$propertyName, original.$propertyName, service, %S)", propertyName)
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved${maybeNullFallback.fallbackValue}$commaOrEmpty", maybeNullFallback.fallbackArgs)
        } else super.toPatchStatement(fieldIndex, variableElement, commaOrEmpty)
    }

    private fun partyCollection(variableElement: VariableElement): KClass<out Any>? {
        val variableElementSig = "${variableElement.asKotlinTypeName()}"
        val partyParam = "<${Party::class.java.canonicalName}>"
        return listOf(List::class, Set::class, Sequence::class)
                .find { variableElementSig.endsWith("${it.simpleName}$partyParam") }
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

        val maybeNullFallback = maybeCheckForNull(variableElement, assignmentCtxForOwnCreator(propertyName) )
        val partyCollection = partyCollection(variableElement)
        val safeDot = if(isNullable(variableElement, FieldContext.TARGET_TYPE)) "?." else "."
        return if (partyCollection != null) {
            Statement("      $propertyName = original.$propertyName${safeDot}map{it.name}${maybeNullFallback.fallbackValue}$commaOrEmpty",
                    maybeNullFallback.fallbackArgs)
        }
        else if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            Statement("      $propertyName = original.$propertyName${safeDot}name${maybeNullFallback.fallbackValue}$commaOrEmpty",
                    maybeNullFallback.fallbackArgs)
        } else return super.toCreatorStatement(index, variableElement, propertyName, propertyType, commaOrEmpty)
    }

    override fun toAltConstructorStatement(
            index: Int, variableElement: VariableElement,
            propertyName: String, propertyType: TypeName,
            commaOrEmpty: String
    ): DtoMembersStrategy.Statement? {

        val maybeNullFallback = maybeCheckForNull(variableElement, assignmentCtxForToAltConstructor(propertyName) )
        val partyCollection = partyCollection(variableElement)
        val safeDot = if(isNullable(variableElement, FieldContext.TARGET_TYPE)) "?." else "."
        return if (partyCollection != null) {
            Statement("      $propertyName = original.$propertyName${safeDot}map{it.name}$commaOrEmpty")
        }
        else if (variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            Statement("      $propertyName = original.$propertyName${safeDot}name${maybeNullFallback.fallbackValue}$commaOrEmpty",
                    maybeNullFallback.fallbackArgs)
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
                "service",
                StateService::class.java.asClassName()
                        .parameterizedBy(annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()))
    }

    // TODO: handle party/account collections
    override fun toPropertyTypeName(variableElement: VariableElement): TypeName {
        val partyCollection = partyCollection(variableElement)
        val params = variableElement.asType().asTypeElement().typeParameters
        return partyCollection?.asClassName()
                ?.parameterizedBy(CordaX500Name::class.java.asTypeName())
                ?.copy(nullable = true)
                ?: if (variableElement.asType().asTypeElement().asClassName() .canonicalName == Party::class.java.asClassName().canonicalName)
                    CordaX500Name::class.java.asTypeName().copy(nullable = true)
                else super.toPropertyTypeName(variableElement)
    }

    override fun addAltConstructor(typeSpecBuilder: Builder, dtoAltConstructorBuilder: FunSpec.Builder) {
        // NO-OP
    }
}
