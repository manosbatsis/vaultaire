package com.github.manosbatsis.vaultaire.plugin.accounts.processor

import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.plugin.BaseTypesConfigAnnotationProcessorPlugin
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto.AccountsAwareLiteDtoStrategy
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateCordaServiceDelegate
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateServiceDelegate
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateServiceRpcConnectionDelegate
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateServiceRpcDelegate
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.ExtendedAccountsAwareStateService
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.plugins.AbstractDtoStrategyFactoryProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.plugins.DtoStrategyFactoryProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.asClassName


@AutoService(BaseTypesConfigAnnotationProcessorPlugin::class)
class AccountsAwareBaseTypesConfigAnnotationProcessorPlugin: BaseTypesConfigAnnotationProcessorPlugin {

    override val stateServiceClassName = ExtendedAccountsAwareStateService::class.asClassName()
    override val stateServiceDelegateClassName = AccountsAwareStateServiceDelegate::class.asClassName()

    override val rpcDelegateClassName = AccountsAwareStateServiceRpcDelegate::class.asClassName()
    override val rpcConnectionDelegateClassName = AccountsAwareStateServiceRpcConnectionDelegate::class.asClassName()
    override val serviceHubDelegateClassName = AccountsAwareStateCordaServiceDelegate::class.asClassName()

    override fun getSupportPriority(annotatedElementInfo: AnnotatedElementInfo, strategy: String?) =
        if(AccountInfoHelper(annotatedElementInfo).useAccountInfo()) 10 else 0
}

// TODO refactor to DTO property mappers config
@AutoService(DtoStrategyFactoryProcessorPlugin::class)
class AccountsAwarDtoStrategyFactoryProcessorPlugin: AbstractDtoStrategyFactoryProcessorPlugin() {

    companion object{
        private val strategies = mapOf(
                VaultaireDtoStrategyKeys.LITE to AccountsAwareLiteDtoStrategy::class.java
        )
    }

    override fun getStrategyClass(strategy: String): Class<out DtoStrategy> {
        val strategyKey = VaultaireDtoStrategyKeys.getFromString(strategy)
        return strategies[strategyKey] ?: error("Strategy $strategy not supported by factory ${this.javaClass.simpleName}")
    }

    override fun getSupportPriority(annotatedElementInfo: AnnotatedElementInfo, strategy: String?): Int {
        if(strategy == null) return 0
        val strategyKey = VaultaireDtoStrategyKeys.findFromString(strategy) ?: return 0
        return if(strategies[strategyKey] != null
                && AccountInfoHelper(annotatedElementInfo).useAccountInfo()) 10 else 0
    }
}

