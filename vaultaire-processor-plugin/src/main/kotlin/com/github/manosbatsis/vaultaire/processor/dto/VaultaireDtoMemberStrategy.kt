package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoNameStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoTypeStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoMembersStrategy
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec.Builder

open class VaultaireDtoMemberStrategy(
        annotatedElementInfo: AnnotatedElementInfo,
        dtoNameStrategy: DtoNameStrategy,
        dtoTypeStrategy: DtoTypeStrategy
): SimpleDtoMembersStrategy(
        annotatedElementInfo, dtoNameStrategy, dtoTypeStrategy
){

    override fun addAltConstructor(typeSpecBuilder: Builder, dtoAltConstructorBuilder: FunSpec.Builder){
        // NO-OP
    }
}
