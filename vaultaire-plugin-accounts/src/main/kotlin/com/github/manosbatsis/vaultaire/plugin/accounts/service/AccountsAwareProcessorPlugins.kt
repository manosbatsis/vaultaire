package com.github.manosbatsis.vaultaire.plugin.accounts.service

import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.*
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dto.VaultaireAccountsAwareLiteDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireLiteDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.plugins.DefaultDtoStrategyFactoryProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.plugins.DtoStrategyFactoryProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.asClassName


@AutoService(VaultaireBaseTypesConfigAnnotationProcessorPlugin::class)
class VaultaireAccountsAwareBaseTypesConfigAnnotationProcessorPlugin: VaultaireBaseTypesConfigAnnotationProcessorPlugin {
    override val stateServiceClassName = ExtendedAccountsAwareStateService::class.asClassName()
    override val stateServiceDelegateClassName = AccountsAwareStateServiceDelegate::class.asClassName()

    override val rpcDelegateClassName = AccountsAwareStateServiceRpcDelegate::class.asClassName()
    override val rpcConnectionDelegateClassName = AccountsAwareStateServiceRpcConnectionDelegate::class.asClassName()
    override val serviceHubDelegateClassName = AccountsAwareStateCordaServiceDelegate::class.asClassName()
}

// TODO
// refaxtor to DTO property mappers config
@AutoService(DtoStrategyFactoryProcessorPlugin::class)
class VaultaireAccountsAwarDtoStrategyFactoryProcessorPlugin: DefaultDtoStrategyFactoryProcessorPlugin() {
    override fun buildDtoInputContext(
            annotatedElementInfo: AnnotatedElementInfo,
            dtoStrategyClass: Class<out DtoStrategy>
    ): DtoStrategy =
            if(dtoStrategyClass == VaultaireLiteDtoStrategy::class.java) VaultaireAccountsAwareLiteDtoStrategy(annotatedElementInfo)
            else super.buildDtoInputContext(annotatedElementInfo, dtoStrategyClass)


}
