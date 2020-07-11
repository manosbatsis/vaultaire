package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoNameStrategy
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment

open class VaultaireLiteDtoNameStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext
): SimpleDtoNameStrategy(processingEnvironment, dtoInputContext) {

    override fun getClassName() = ClassName(
            mapPackageName(dtoInputContext.originalTypeElement.asClassName().packageName),
            "${dtoInputContext.originalTypeElement.simpleName}LiteDto")

}
