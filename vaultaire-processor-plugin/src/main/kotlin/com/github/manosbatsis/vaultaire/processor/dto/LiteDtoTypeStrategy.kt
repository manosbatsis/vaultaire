package com.github.manosbatsis.vaultaire.processor.dto


import com.github.manosbatsis.vaultaire.dto.VaultaireLiteDto
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoTypeStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName

open class LiteDtoTypeStrategy(
        annotatedElementInfo: AnnotatedElementInfo
): SimpleDtoTypeStrategy(annotatedElementInfo){


    override fun addSuperTypes(typeSpecBuilder: Builder) {
        val typeName = annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName()
        typeSpecBuilder.addSuperinterface(
                VaultaireLiteDto::class.asClassName().parameterizedBy(typeName))
    }
}
