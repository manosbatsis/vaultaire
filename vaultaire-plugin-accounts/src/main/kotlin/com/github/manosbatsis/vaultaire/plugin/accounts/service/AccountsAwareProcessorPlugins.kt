package com.github.manosbatsis.vaultaire.plugin.accounts.service

import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.*
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dto.VaultaireAccountsAwareLiteDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.VaultaireLiteDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement


@AutoService(VaultaireBaseTypesConfigAnnotationProcessorPlugin::class)
class VaultaireAccountsAwareBaseTypesConfigAnnotationProcessorPlugin: VaultaireBaseTypesConfigAnnotationProcessorPlugin {
    override val stateServiceClassName = ExtendedAccountsAwareStateService::class.asClassName()
    override val stateServiceDelegateClassName = AccountsAwareStateServiceDelegate::class.asClassName()

    override val rpcDelegateClassName = AccountsAwareStateServiceRpcDelegate::class.asClassName()
    override val rpcConnectionDelegateClassName = AccountsAwareStateServiceRpcConnectionDelegate::class.asClassName()
    override val serviceHubDelegateClassName = AccountsAwareStateCordaServiceDelegate::class.asClassName()
}

@AutoService(DtoInputContextFactoryProcessorPlugin::class)
class VaultaireAccountsAwarDtoStrategyFactoryProcessorPlugin: DtoInputContextFactoryProcessorPlugin {
    override fun buildDtoInputContext(
            processingEnvironment: ProcessingEnvironment,
            contractStateTypeElement: TypeElement,
            fields: List<VariableElement>,
            copyAnnotationPackages: Iterable<String>,
            dtoStrategyClass: Class<out DtoStrategy>
    ): DtoInputContext {
        val dtoInputContext = DtoInputContext(
                processingEnvironment,
                contractStateTypeElement,
                fields,
                copyAnnotationPackages)

        val dtoStrategy = if(dtoStrategyClass == VaultaireLiteDtoStrategy::class.java)
                VaultaireAccountsAwareLiteDtoStrategy(processingEnvironment, dtoInputContext)
            else dtoStrategyClass.getConstructor(
                ProcessingEnvironment::class.java,
                DtoInputContext::class.java)
                .newInstance(processingEnvironment, dtoInputContext)

        dtoInputContext.dtoStrategyInstance = dtoStrategy
        return dtoInputContext
    }
}
