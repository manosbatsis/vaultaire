/*
 * Vaultaire: query DSL and data access utilities for Corda developers.
 * Copyright (C) 2018 Manos Batsis
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package com.github.manosbatsis.vaultaire.plugin.accounts.processor

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.github.manosbatsis.kotlin.utils.kapt.plugins.AbstractDtoStrategyFactoryProcessorPlugin
import com.github.manosbatsis.kotlin.utils.kapt.plugins.DtoStrategyFactoryProcessorPlugin
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.plugin.BaseTypesConfigAnnotationProcessorPlugin
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto.AccountsAwareModelClientDtoStrategy
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.dto.AccountsAwareStateClientDtoStrategy
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.*
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName


@AutoService(BaseTypesConfigAnnotationProcessorPlugin::class)
class AccountsAwareBaseTypesConfigAnnotationProcessorPlugin : BaseTypesConfigAnnotationProcessorPlugin {

    override val stateServiceClassName = ExtendedAccountsAwareStateService::class.asClassName()
    override val stateServiceDelegateClassName = AccountsAwareStateServiceDelegate::class.asClassName()

    override val poolBoyDelegateClassName: ClassName = AccountsAwareStateServicePoolBoyDelegate::class.java.asClassName()
    override val rpcDelegateClassName = AccountsAwareStateServiceRpcDelegate::class.asClassName()
    override val rpcConnectionDelegateClassName = AccountsAwareStateServiceRpcConnectionDelegate::class.asClassName()
    override val serviceHubDelegateClassName = AccountsAwareStateCordaServiceDelegate::class.asClassName()

    override fun getSupportPriority(annotatedElementInfo: AnnotatedElementInfo, strategy: String?) =
            if (AccountInfoHelper(annotatedElementInfo).useAccountInfo()) 10 else 0
}

// TODO refactor to DTO property mappers config?
@AutoService(DtoStrategyFactoryProcessorPlugin::class)
class AccountsAwarDtoStrategyFactoryProcessorPlugin : AbstractDtoStrategyFactoryProcessorPlugin() {

    companion object {
        private val strategies = mapOf(
                VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO.toString() to AccountsAwareStateClientDtoStrategy::class.java,
                AccountsAwareModelClientDtoStrategy.STRATEGY_KEY to AccountsAwareModelClientDtoStrategy::class.java
        )
    }

    override fun getStrategyClass(strategy: String): Class<out DtoStrategy> {
        return strategies[strategy] as Class<out DtoStrategy>?
                ?: error("Strategy $strategy not supported by factory ${this.javaClass.simpleName}")
    }

    override fun getSupportPriority(annotatedElementInfo: AnnotatedElementInfo, strategy: String?): Int {
        if (strategy == null) return 0
        return if (strategies[strategy] != null
                && AccountInfoHelper(annotatedElementInfo).useAccountInfo()) 10 else 0
    }
}

