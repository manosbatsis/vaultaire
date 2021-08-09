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
package com.github.manosbatsis.vaultaire.plugin

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.DtoStrategy
import com.github.manosbatsis.kotlin.utils.kapt.plugins.AbstractDtoStrategyFactoryProcessorPlugin
import com.github.manosbatsis.kotlin.utils.kapt.plugins.DtoStrategyFactoryProcessorPlugin
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.processor.dto.ModelClientDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.StateClientDtoStrategy
import com.github.manosbatsis.vaultaire.processor.dto.StateDtoStrategy
import com.google.auto.service.AutoService


@AutoService(DtoStrategyFactoryProcessorPlugin::class)
class BasicDtoStrategyFactoryProcessorPlugin : AbstractDtoStrategyFactoryProcessorPlugin() {

    companion object {
        private val strategies = mapOf(
                VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO.toString() to StateDtoStrategy::class.java,
                VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO.toString() to StateClientDtoStrategy::class.java,
                ModelClientDtoStrategy.STRATEGY_KEY to ModelClientDtoStrategy::class.java
        )
    }

    override fun getStrategyClass(strategy: String): Class<out DtoStrategy> {
        return strategies[strategy]
                ?: error("Strategy $strategy not supported by factory ${this.javaClass.simpleName}")
    }

    override fun getSupportPriority(annotatedElementInfo: AnnotatedElementInfo, strategy: String?): Int {
        if (strategy == null) return 0
        return if (strategies[strategy] != null) 1 else 0
    }
}
