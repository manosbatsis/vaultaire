package com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto

import com.github.manosbatsis.vaultaire.dto.AccountInfoDto
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.AccountInfoHelper
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ABSTRACT_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ACCOUNT_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ANONYMOUS_PARTY
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_PUBLIC_KEY
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateService
import com.github.manosbatsis.vaultaire.processor.dto.LiteDtoMemberStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.VariableElement

open class AcountsAwareLiteDtoMemberStrategy(
        annotatedElementInfo: AnnotatedElementInfo
): LiteDtoMemberStrategy(annotatedElementInfo){

    val accountInfoHelper = AccountInfoHelper(annotatedElementInfo)
    fun isAccountInfo(variableElement: VariableElement): Boolean = accountInfoHelper.isAccountInfo(variableElement)

    override fun toPropertyTypeName(variableElement: VariableElement): TypeName {
        return if(isAccountInfo(variableElement))
            AccountInfoDto::class.java.asTypeName().copy(nullable = true)
        else super.toPropertyTypeName(variableElement)
    }

    override fun toMapStatement(variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val fieldClassName = variableElement.asType().asTypeElement().asClassName()
        val propertyName = toPropertyName(variableElement)
        return if(isAccountInfo(variableElement)) {
            when(fieldClassName){
                CLASSNAME_ACCOUNT_PARTY -> DtoMembersStrategy.Statement("      $propertyName = stateService.toAccountParty${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName)$commaOrEmpty")
                CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY -> DtoMembersStrategy.Statement("      $propertyName = stateService.toAbstractParty${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName)$commaOrEmpty")
                CLASSNAME_PUBLIC_KEY -> DtoMembersStrategy.Statement("      $propertyName = stateService.toPublicKey${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName)$commaOrEmpty")
                else -> throw IllegalStateException("Unhandled patch statement case for AccountInfo compatible type: $fieldClassName")
            }
        }
        else super.toMapStatement(variableElement, commaOrEmpty)
    }

    override fun toPatchStatement(variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val fieldClassName = variableElement.asType().asTypeElement().asClassName()
        val propertyName = toPropertyName(variableElement)
        return if(isAccountInfo(variableElement)) {
            when(fieldClassName){
                CLASSNAME_ACCOUNT_PARTY -> DtoMembersStrategy.Statement("      $propertyName = stateService.toAccountParty${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName)$commaOrEmpty")
                CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY -> DtoMembersStrategy.Statement("      $propertyName = stateService.toAbstractParty${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName)$commaOrEmpty")
                CLASSNAME_PUBLIC_KEY -> DtoMembersStrategy.Statement("      $propertyName = stateService.toPublicKey${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName)$commaOrEmpty")
                else -> throw IllegalStateException("Unhandled patch statement case for AccountInfo compatible type: $fieldClassName")
            }
        }
        else super.toPatchStatement(variableElement, commaOrEmpty)
    }

    override fun toAltConstructorStatement(
            index: Int, variableElement: VariableElement, propertyName: String, propertyType: TypeName, commaOrEmpty: String
    ): DtoMembersStrategy.Statement? {
        return if(isAccountInfo(variableElement)) {
            DtoMembersStrategy.Statement("      $propertyName = stateService.toAccountInfoDto${if(variableElement.isNullable()) "OrNull" else ""}(original.$propertyName)$commaOrEmpty")
        }
        else super.toAltConstructorStatement(index, variableElement, propertyName, propertyType, commaOrEmpty)
    }


    override fun addStateServiceParameter(functionBuilder: FunSpec.Builder) {
        functionBuilder.addParameter(
                "stateService",
                AccountsAwareStateService::class.asClassName().parameterizedBy(
                        annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()))
    }

}
