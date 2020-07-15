package com.github.manosbatsis.vaultaire.plugin.accounts.service

import com.github.manotbatsis.kotlin.utils.api.AnnotationProcessorPlugin
import com.squareup.kotlinpoet.ClassName

/** Used to override base types used by the annotation processor */
interface VaultaireBaseTypesConfigAnnotationProcessorPlugin: AnnotationProcessorPlugin {
    val stateServiceClassName: ClassName
    val stateServiceDelegateClassName: ClassName

    val rpcDelegateClassName: ClassName
    val rpcConnectionDelegateClassName: ClassName
    val serviceHubDelegateClassName: ClassName
}

