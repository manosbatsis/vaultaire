package com.github.manosbatsis.vaultaire.processor.plugin

import com.github.manosbatsis.vaultaire.plugin.accounts.service.VaultaireBaseTypesConfigAnnotationProcessorPlugin
import com.github.manosbatsis.vaultaire.service.dao.*
import com.squareup.kotlinpoet.asClassName


class VaultaireDefaultBaseTypesConfigAnnotationProcessorPlugin: VaultaireBaseTypesConfigAnnotationProcessorPlugin {
    override val stateServiceClassName = DefaultExtendedStateService::class.asClassName()
    override val stateServiceDelegateClassName = StateServiceDelegate::class.asClassName()
    override val rpcDelegateClassName = StateServiceRpcDelegate::class.asClassName()
    override val rpcConnectionDelegateClassName = StateServiceRpcConnectionDelegate::class.asClassName()
    override val serviceHubDelegateClassName = StateCordaServiceDelegate::class.asClassName()
}
