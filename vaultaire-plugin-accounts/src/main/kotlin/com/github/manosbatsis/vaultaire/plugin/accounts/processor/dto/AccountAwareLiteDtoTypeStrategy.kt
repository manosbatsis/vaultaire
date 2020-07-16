package com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto

import com.github.manosbatsis.vaultaire.plugin.accounts.service.dto.AccountsAwareLiteDto
import com.github.manosbatsis.vaultaire.processor.dto.LiteDtoTypeStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

open class AccountAwareLiteDtoTypeStrategy(
        annotatedElementInfo: AnnotatedElementInfo
): LiteDtoTypeStrategy(annotatedElementInfo){

    override fun addSuperTypes(typeSpecBuilder: TypeSpec.Builder) {
        typeSpecBuilder.addSuperinterface(
                AccountsAwareLiteDto::class.asClassName()
                        .parameterizedBy(annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()))
    }
}
