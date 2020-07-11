package com.github.manosbatsis.vaultaire.processor.plugin

import com.github.manosbatsis.vaultaire.plugin.accounts.service.DtoInputContextFactoryProcessorPlugin
import com.github.manosbatsis.vaultaire.plugin.accounts.service.VaultaireBaseTypesConfigAnnotationProcessorPlugin
import com.github.manosbatsis.vaultaire.service.dao.*
import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement


class VaultaireDefaultBaseTypesConfigAnnotationProcessorPlugin: VaultaireBaseTypesConfigAnnotationProcessorPlugin {
    override val stateServiceClassName = DefaultExtendedStateService::class.asClassName()
    override val stateServiceDelegateClassName = StateServiceDelegate::class.asClassName()
    override val rpcDelegateClassName = StateServiceRpcDelegate::class.asClassName()
    override val rpcConnectionDelegateClassName = StateServiceRpcConnectionDelegate::class.asClassName()
    override val serviceHubDelegateClassName = StateCordaServiceDelegate::class.asClassName()
}

class VaultaireDefaultDtoStrategyFactoryProcessorPlugin: DtoInputContextFactoryProcessorPlugin {
    override fun buildDtoInputContext(
            processingEnvironment: ProcessingEnvironment,
            contractStateTypeElement: TypeElement,
            fields: List<VariableElement>,
            copyAnnotationPackages: Iterable<String>,
            dtoStrategyClass: Class<out DtoStrategy>
    ): DtoInputContext {
        val dtoInputContext =  DtoInputContext(
                processingEnvironment,
                contractStateTypeElement,
                fields,
                copyAnnotationPackages,
                dtoStrategyClass)
        return dtoInputContext
    }

}
