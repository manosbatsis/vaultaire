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

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy.Statement
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.DtoNameStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.DtoTypeStrategy
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoLiteDto
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.AccountInfoHelper
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ABSTRACT_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ACCOUNT_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ANONYMOUS_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_PUBLIC_KEY
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateService
import com.github.manosbatsis.vaultaire.processor.dto.LiteDtoMemberStrategy
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.VariableElement

open class AcountsAwareLiteDtoMemberStrategy(
        annotatedElementInfo: AnnotatedElementInfo,
        dtoNameStrategy: DtoNameStrategy,
        dtoTypeStrategy: DtoTypeStrategy
) : LiteDtoMemberStrategy(annotatedElementInfo, dtoNameStrategy, dtoTypeStrategy) {

    val accountInfoHelper = AccountInfoHelper(annotatedElementInfo)
    fun isAccountInfo(variableElement: VariableElement): Boolean = accountInfoHelper.isAccountInfo(variableElement)

    override fun toPropertyTypeName(variableElement: VariableElement): TypeName {
        return if (isAccountInfo(variableElement))
            AccountInfoLiteDto::class.java.asTypeName().copy(nullable = true)
        else super.toPropertyTypeName(variableElement)
    }

    override fun toTargetTypeStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val fieldClassName = variableElement.asType().asTypeElement().asClassName()
        val propertyName = toPropertyName(variableElement)
        return if (isAccountInfo(variableElement)) {
            when (fieldClassName) {
                CLASSNAME_ACCOUNT_PARTY -> targetTypeFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = stateService.toAccountParty${if (variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, null, false, %S)", propertyName)
                CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY -> targetTypeFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = stateService.toAbstractParty${if (variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, null, false, %S)", propertyName)
                CLASSNAME_PUBLIC_KEY -> targetTypeFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = stateService.toPublicKey${if (variableElement.isNullable()) "OrNull" else ""}(this.$propertyName)")
                else -> throw IllegalStateException("Unhandled patch statement case for AccountInfo compatible type: $fieldClassName")
            }
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
        } else super.toTargetTypeStatement(fieldIndex, variableElement, commaOrEmpty)
    }

    override fun toPatchStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val fieldClassName = variableElement.asType().asTypeElement().asClassName()
        val propertyName = toPropertyName(variableElement)
        return if (isAccountInfo(variableElement)) {
            when (fieldClassName) {
                CLASSNAME_ACCOUNT_PARTY -> patchFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = stateService.toAccountParty${if (variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName)")
                CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY -> patchFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = stateService.toAbstractParty${if (variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName)")
                CLASSNAME_PUBLIC_KEY -> patchFunctionBuilder
                        .addStatement("      val ${propertyName}Resolved = stateService.toPublicKey${if (variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName)")
                else -> throw IllegalStateException("Unhandled patch statement case for AccountInfo compatible type: $fieldClassName")
            }
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
        } else super.toPatchStatement(fieldIndex, variableElement, commaOrEmpty)
    }

    override fun toCreatorStatement(
            fieldIndex: Int, variableElement: VariableElement,
            propertyName: String, propertyType: TypeName,
            commaOrEmpty: String
    ): Statement? {
        return if (isAccountInfo(variableElement)) {
            creatorFunctionBuilder.addStatement("      val ${propertyName}Resolved = stateService.toAccountInfoLiteDto${if (variableElement.isNullable()) "OrNull" else ""}(original.$propertyName)")
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
        } else return super.toCreatorStatement(fieldIndex, variableElement, propertyName, propertyType, commaOrEmpty)
    }

    override fun toAltConstructorStatement(
            fieldIndex: Int, variableElement: VariableElement,
            propertyName: String, propertyType: TypeName,
            commaOrEmpty: String
    ): DtoMembersStrategy.Statement? {
        return if (isAccountInfo(variableElement)) {
            dtoAltConstructorCodeBuilder.addStatement("      val ${propertyName}Resolved = stateService.toAccountInfoLiteDto${if (variableElement.isNullable()) "OrNull" else ""}(original.$propertyName)")
            DtoMembersStrategy.Statement("      $propertyName = ${propertyName}Resolved$commaOrEmpty")
        } else super.toAltConstructorStatement(fieldIndex, variableElement, propertyName, propertyType, commaOrEmpty)
    }


    override fun addStateServiceParameter(functionBuilder: FunSpec.Builder) {
        functionBuilder.addParameter(
                "stateService",
                AccountsAwareStateService::class.asClassName().parameterizedBy(
                        annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()))
    }

}
