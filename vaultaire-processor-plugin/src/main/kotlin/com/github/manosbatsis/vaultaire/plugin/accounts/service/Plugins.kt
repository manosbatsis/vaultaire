package com.github.manosbatsis.vaultaire.plugin.accounts.service

import com.github.manotbatsis.kotlin.utils.api.AnnotationProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.CompositeDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.squareup.kotlinpoet.ClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/** Used to override base types used by the annotation processor */
interface VaultaireBaseTypesConfigAnnotationProcessorPlugin: AnnotationProcessorPlugin {
    val stateServiceClassName: ClassName
    val stateServiceDelegateClassName: ClassName

    val rpcDelegateClassName: ClassName
    val rpcConnectionDelegateClassName: ClassName
    val serviceHubDelegateClassName: ClassName
}

/** Used to enhance or otherwise override [DtoInputContext] and [DtoStrategy] initialization by the annotation processor */
interface DtoInputContextFactoryProcessorPlugin: AnnotationProcessorPlugin {
    fun buildDtoInputContext(
            processingEnvironment: ProcessingEnvironment,
            contractStateTypeElement: TypeElement,
            fields: List<VariableElement> = emptyList(),
            copyAnnotationPackages: Iterable<String> = emptyList(),
            dtoStrategyClass: Class<out DtoStrategy> = CompositeDtoStrategy::class.java
    ): DtoInputContext
}
