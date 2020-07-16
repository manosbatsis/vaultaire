package com.github.manosbatsis.vaultaire.plugin

import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.processor.dto.DefaultDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.LiteDtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.github.manotbatsis.kotlin.utils.kapt.plugins.AbstractDtoStrategyFactoryProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.plugins.DtoStrategyFactoryProcessorPlugin
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.google.auto.service.AutoService


@AutoService(DtoStrategyFactoryProcessorPlugin::class)
class BasicDtoStrategyFactoryProcessorPlugin: AbstractDtoStrategyFactoryProcessorPlugin() {

    companion object{
        private val strategies = mapOf(
                VaultaireDtoStrategyKeys.DEFAULT to DefaultDtoStrategy::class.java,
                VaultaireDtoStrategyKeys.LITE to LiteDtoStrategy::class.java
        )
    }

    override fun getStrategyClass(strategy: String): Class<out DtoStrategy> {
        val strategyKey = VaultaireDtoStrategyKeys.getFromString(strategy)
        return strategies[strategyKey] ?: error("Strategy $strategy not supported by factory ${this.javaClass.simpleName}")
    }

    override fun getSupportPriority(annotatedElementInfo: AnnotatedElementInfo, strategy: String?): Int {
        if(strategy == null) return 0
        val strategyKey = VaultaireDtoStrategyKeys.findFromString(strategy) ?: return 0
        return if(strategies[strategyKey] != null) 1 else 0
    }
}
