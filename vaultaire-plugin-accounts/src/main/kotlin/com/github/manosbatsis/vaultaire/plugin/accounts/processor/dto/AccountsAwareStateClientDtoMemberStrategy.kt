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
package com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.*
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoMembersStrategy.Statement
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.util.FieldContext
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoStateClientDto
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.AccountInfoHelper
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ABSTRACT_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ACCOUNT_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ANONYMOUS_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_PUBLIC_KEY
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateService
import com.github.manosbatsis.vaultaire.processor.dto.ClientDtoMembersStrategyBase
import com.github.manosbatsis.vaultaire.processor.dto.StateClientDtoNameStrategy
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import net.corda.core.identity.Party
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass

open class AccountsAwareClientDtoMemberStrategy(
        rootDtoStrategy: DtoStrategyLesserComposition
):AccountsAwareClientDtoMemberStrategyBase<StateClientDtoNameStrategy, AccountsAwareStateClientDtoTypeStrategy>(
        rootDtoStrategy
)

open class AccountsAwareClientDtoMemberStrategyBase<N: DtoNameStrategy, T: DtoTypeStrategy>(
        rootDtoStrategy: DtoStrategyLesserComposition
) : ClientDtoMembersStrategyBase(rootDtoStrategy) {

    val accountInfoHelper = AccountInfoHelper(annotatedElementInfo)

    override fun toPropertyTypeName(variableElement: VariableElement): TypeName {
        return when{
            accountInfoHelper.isAccountInfo(variableElement) -> AccountInfoStateClientDto::class.java.asTypeName().copy(nullable = true)
            accountInfoHelper.isAccountInfos(variableElement) -> variableElement.asType().asTypeElement().asKotlinClassName()
                .parameterizedBy(AccountInfoStateClientDto::class.asTypeName()).copy(nullable = true)
            else -> super.toPropertyTypeName(variableElement)
        }


    }

    override fun toTargetTypeStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val fieldClassName = variableElement.asType().asTypeElement().asKotlinClassName()
        val partyCollection = accountPartyCollection(variableElement)

        val propertyName = toPropertyName(variableElement)

        val maybeNullFallback = maybeCheckForNull(variableElement, assignmentCtxForToTargetType(propertyName) )
        return if (partyCollection != null) {
            targetTypeFunctionBuilder.addStatement("val ${propertyName}Resolved = (this.$propertyName${maybeNullFallback.fallbackValue})")
            targetTypeFunctionBuilder.addStatement("     .filterNotNull()")
            targetTypeFunctionBuilder.addStatement("     .mapNotNull{ service.toAccountParty(it, null, false, %S) } ", propertyName)
            targetTypeFunctionBuilder.addStatement("     ")
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
        }
        else if (accountInfoHelper.isAccountInfo(variableElement)) {
            when (fieldClassName) {
                CLASSNAME_ACCOUNT_PARTY -> targetTypeFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = service.toAccountPartyOrNull(this.$propertyName, null, false, %S)", propertyName)
                CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY -> targetTypeFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = service.toAbstractPartyOrNull(this.$propertyName, null, false, %S)", propertyName)
                CLASSNAME_PUBLIC_KEY -> targetTypeFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = service.toPublicKeyOrNull(this.$propertyName)")
                else -> throw IllegalStateException("Unhandled patch statement case for AccountInfo compatible type: $fieldClassName")
            }
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved${maybeNullFallback.fallbackValue}$commaOrEmpty", maybeNullFallback.fallbackArgs)
        } else super.toTargetTypeStatement(fieldIndex, variableElement, commaOrEmpty)
    }

    override fun toPatchStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val fieldClassName = variableElement.asType().asTypeElement().asKotlinClassName()
        val partyCollection = accountPartyCollection(variableElement)

        val propertyName = toPropertyName(variableElement)
        val maybeNullFallback = maybeCheckForNull(variableElement, assignmentCtxForToPatched(propertyName))
        return if (partyCollection != null) {
            patchFunctionBuilder.addStatement("val ${propertyName}Resolved = $propertyName?.filterNotNull()", propertyName)
            patchFunctionBuilder.addStatement("     ?.mapNotNull{ service.toAccountPartyOrNull(it, null, false, %S) } ", propertyName)
            patchFunctionBuilder.addStatement("     ?.let{ if(it.isNotEmpty()) it else null } ")
            patchFunctionBuilder.addStatement("     ${maybeNullFallback.fallbackValue}", maybeNullFallback.fallbackArgs)
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
        }
        else if (accountInfoHelper.isAccountInfo(variableElement)) {
            when (fieldClassName) {
                CLASSNAME_ACCOUNT_PARTY -> patchFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = service.toAccountPartyOrNull(this.$propertyName, original.$propertyName)")
                CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY -> patchFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = service.toAbstractPartyOrNull(this.$propertyName, original.$propertyName)")
                CLASSNAME_PUBLIC_KEY -> patchFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = service.toPublicKeyOrNull(this.$propertyName, original.$propertyName)")
                else -> throw IllegalStateException("Unhandled patch statement case for AccountInfo compatible type: $fieldClassName")
            }
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved${maybeNullFallback.fallbackValue}$commaOrEmpty", maybeNullFallback.fallbackArgs)
        } else super.toPatchStatement(fieldIndex, variableElement, commaOrEmpty)
    }

    private fun accountPartyCollection(variableElement: VariableElement): KClass<out Any>? {
        val variableElementSig = "${variableElement.asKotlinTypeName()}"
        val partyParam = "<${AccountParty::class.java.canonicalName}>"
        return listOf(List::class, Set::class, Sequence::class)
            .find { variableElementSig.endsWith("${it.simpleName}$partyParam") }
    }

    override fun toCreatorStatement(
            fieldIndex: Int, variableElement: VariableElement,
            propertyName: String, propertyType: TypeName,
            commaOrEmpty: String
    ): Statement? {
        val maybeNullFallback = maybeCheckForNull(variableElement, assignmentCtxForOwnCreator(propertyName) )
        val partyCollection = accountPartyCollection(variableElement)
        val safeDot = if(isNullable(variableElement, FieldContext.TARGET_TYPE)) "?." else "."
        return if (partyCollection != null) {
            Statement("      $propertyName = original.$propertyName${safeDot}map{service.toAccountInfoClientDto(it)}${maybeNullFallback.fallbackValue}$commaOrEmpty",
                maybeNullFallback.fallbackArgs)
        } else if (accountInfoHelper.isAccountInfo(variableElement)) {
            creatorFunctionBuilder.addStatement("      val ${propertyName}Resolved = service.toAccountInfoClientDtoOrNull(original.$propertyName)")
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved${maybeNullFallback.fallbackValue}$commaOrEmpty", maybeNullFallback.fallbackArgs)
        } else return super.toCreatorStatement(fieldIndex, variableElement, propertyName, propertyType, commaOrEmpty)
    }

    override fun toAltConstructorStatement(
            fieldIndex: Int, variableElement: VariableElement,
            propertyName: String, propertyType: TypeName,
            commaOrEmpty: String
    ): DtoMembersStrategy.Statement? {
        val maybeNullFallback = maybeCheckForNull(variableElement, assignmentCtxForToAltConstructor(propertyName))
        val partyCollection = accountPartyCollection(variableElement)
        val safeDot = if(isNullable(variableElement, FieldContext.TARGET_TYPE)) "?." else "."
        return if (partyCollection != null) {
            Statement("      $propertyName = original.$propertyName${safeDot}map{service.toAccountInfoClientDto(it)}${maybeNullFallback.fallbackValue}$commaOrEmpty",
                maybeNullFallback.fallbackArgs)
        } else if (accountInfoHelper.isAccountInfo(variableElement)) {
            dtoAltConstructorCodeBuilder.addStatement("      val ${propertyName}Resolved = service.toAccountInfoClientDtoOrNull(original.$propertyName)")
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved${maybeNullFallback.fallbackValue}$commaOrEmpty", maybeNullFallback.fallbackArgs)
        } else super.toAltConstructorStatement(fieldIndex, variableElement, propertyName, propertyType, commaOrEmpty)
    }


    override fun addStateServiceParameter(functionBuilder: FunSpec.Builder) {
        functionBuilder.addParameter(
                "service",
                AccountsAwareStateService::class.asClassName().parameterizedBy(
                        annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()))
    }

}
