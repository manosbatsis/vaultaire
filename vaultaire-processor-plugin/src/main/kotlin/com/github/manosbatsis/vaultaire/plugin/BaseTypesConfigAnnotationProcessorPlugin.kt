package com.github.manosbatsis.vaultaire.plugin

import com.github.manosbatsis.vaultaire.service.dao.*
import com.github.manotbatsis.kotlin.utils.kapt.plugins.AnnotationProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

/** Used to override base types used by the annotation processor */
interface BaseTypesConfigAnnotationProcessorPlugin: AnnotationProcessorPlugin {

    val stateServiceClassName: ClassName
    val stateServiceDelegateClassName: ClassName

    val rpcDelegateClassName: ClassName
    val rpcConnectionDelegateClassName: ClassName
    val serviceHubDelegateClassName: ClassName
}


@AutoService(BaseTypesConfigAnnotationProcessorPlugin::class)
class DefaultBaseTypesConfigAnnotationProcessorPlugin: BaseTypesConfigAnnotationProcessorPlugin {
    override val stateServiceClassName = DefaultExtendedStateService::class.asClassName()
    override val stateServiceDelegateClassName = StateServiceDelegate::class.asClassName()
    override val rpcDelegateClassName = StateServiceRpcDelegate::class.asClassName()
    override val rpcConnectionDelegateClassName = StateServiceRpcConnectionDelegate::class.asClassName()
    override val serviceHubDelegateClassName = StateCordaServiceDelegate::class.asClassName()
    override fun getSupportPriority(annotatedElementInfo: AnnotatedElementInfo, strategy: String?): Int = 1
}
