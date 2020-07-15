package com.github.manosbatsis.vaultaire.plugin.accounts.service.dto

import com.github.manosbatsis.vaultaire.Util.Companion.CLASSNAME_ABSTRACT_PARTY
import com.github.manosbatsis.vaultaire.Util.Companion.CLASSNAME_ACCOUNT_ID_AND_PARTY
import com.github.manosbatsis.vaultaire.Util.Companion.CLASSNAME_ANONYMOUS_PARTY
import com.github.manosbatsis.vaultaire.Util.Companion.CLASSNAME_PUBLIC_KEY
import com.github.manosbatsis.vaultaire.annotation.VaultaireAccountInfo
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateService
import com.github.manosbatsis.vaultaire.processor.dto.LiteDtoNameStrategy
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireLiteDtoMemberStrategy
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireLiteDtoTypeStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.*
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import javax.lang.model.element.VariableElement

/** Vaultaire-specific overrides for building a "lite" DTO type spec */
open class VaultaireAccountsAwareLiteDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : CompositeDtoStrategy(
        annotatedElementInfo = annotatedElementInfo,
        composition = VaultaireAccountsAwareLiteDtoStrategyComposition
)

object VaultaireAccountsAwareLiteDtoStrategyComposition: DtoStrategyComposition {
    override fun dtoNameStrategy(
            annotatedElementInfo: AnnotatedElementInfo
    ): DtoNameStrategy = LiteDtoNameStrategy(
            annotatedElementInfo
    )
    override fun dtoMembersStrategy(
            annotatedElementInfo: AnnotatedElementInfo
    ): DtoMembersStrategy = VaultaireAcountsawareLiteDtoMemberStrategy(
            annotatedElementInfo
    )
    override fun dtoTypeStrategy(
            annotatedElementInfo: AnnotatedElementInfo
    ): DtoTypeStrategy = VaultaireAccountAwareLiteDtoTypeStrategy(
            annotatedElementInfo
    )
}

open class VaultaireAccountAwareLiteDtoTypeStrategy(
        annotatedElementInfo: AnnotatedElementInfo
): VaultaireLiteDtoTypeStrategy(annotatedElementInfo){

    override fun addSuperTypes(typeSpecBuilder: TypeSpec.Builder) {
        typeSpecBuilder.addSuperinterface(
                VaultaireAccountsAwareLiteDto::class.asClassName()
                        .parameterizedBy(annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()))
    }
}

open class VaultaireAcountsawareLiteDtoMemberStrategy(
        annotatedElementInfo: AnnotatedElementInfo
): VaultaireLiteDtoMemberStrategy(annotatedElementInfo){

    override fun toMapStatement(variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {

        val fieldClassName = variableElement.asType().asTypeElement().asClassName()
        val propertyName = toPropertyName(variableElement)
        return if(isAccountInfo(variableElement)) {
            when(fieldClassName){
                CLASSNAME_ACCOUNT_ID_AND_PARTY -> DtoMembersStrategy.Statement("      $propertyName = toAccountIdAndParty${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName, stateService)$commaOrEmpty")
                CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY -> DtoMembersStrategy.Statement("      $propertyName = toAbstractParty${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName, stateService)$commaOrEmpty")
                CLASSNAME_PUBLIC_KEY -> DtoMembersStrategy.Statement("      $propertyName = toPublicKey${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName, stateService)$commaOrEmpty")
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
                CLASSNAME_ACCOUNT_ID_AND_PARTY -> DtoMembersStrategy.Statement("      $propertyName = toAccountIdAndPartyOrDefault${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName, stateService)$commaOrEmpty")
                CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY -> DtoMembersStrategy.Statement("      $propertyName = toAbstractPartyOrDefault${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName, stateService)$commaOrEmpty")
                CLASSNAME_PUBLIC_KEY -> DtoMembersStrategy.Statement("      $propertyName = toPublicKeyOrDefault${if(variableElement.isNullable()) "OrNull" else ""}(this.$propertyName, original.$propertyName, stateService)$commaOrEmpty")
                else -> throw IllegalStateException("Unhandled patch statement case for AccountInfo compatible type: $fieldClassName")
            }
        }
        else super.toPatchStatement(variableElement, commaOrEmpty)
    }

    override fun toAltConstructorStatement(
            index: Int, variableElement: VariableElement, propertyName: String, propertyType: TypeName, commaOrEmpty: String
    ): DtoMembersStrategy.Statement? {
        return if(isAccountInfo(variableElement)) {
            DtoMembersStrategy.Statement("      $propertyName = toAccountInfoDto${if(variableElement.isNullable()) "Nullable" else ""}(original.$propertyName, stateService)$commaOrEmpty")
        }
        else super.toAltConstructorStatement(index, variableElement, propertyName, propertyType, commaOrEmpty)
    }

    fun isAccountInfo(variableElement: VariableElement): Boolean{
        val fieldClassName = variableElement.asType().asTypeElement().asClassName()
        return when {
            listOf(CLASSNAME_ACCOUNT_ID_AND_PARTY /* TODO , CLASSNAME_ACCOUNT_NAME_AND_PARTY */).contains(fieldClassName) -> true
            listOf(CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY, CLASSNAME_PUBLIC_KEY).contains(fieldClassName)
                    && variableElement.hasAnnotation(VaultaireAccountInfo::class.java) -> true
            !listOf(CLASSNAME_ABSTRACT_PARTY, CLASSNAME_ANONYMOUS_PARTY, CLASSNAME_PUBLIC_KEY).contains(fieldClassName)
                    && variableElement.hasAnnotation(VaultaireAccountInfo::class.java) -> throw IllegalArgumentException(
                    "VaultaireAccountInfo annotation does not support type: $fieldClassName")
            else -> false
        }
    }

    override fun addStateServiceParameter(functionBuilder: FunSpec.Builder) {
        functionBuilder.addParameter(
                "stateService",
                AccountsAwareStateService::class.asClassName().parameterizedBy(
                        annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()))
    }

}
