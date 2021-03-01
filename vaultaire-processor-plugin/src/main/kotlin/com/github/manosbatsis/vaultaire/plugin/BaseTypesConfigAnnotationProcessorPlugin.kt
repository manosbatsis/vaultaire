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

import com.github.manosbatsis.kotlin.utils.kapt.plugins.AnnotationProcessorPlugin
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.service.dao.DefaultExtendedStateService
import com.github.manosbatsis.vaultaire.service.dao.StateCordaServiceDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServiceDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServicePoolBoyDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServiceRpcConnectionDelegate
import com.github.manosbatsis.vaultaire.service.dao.StateServiceRpcDelegate
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

/** Used to override base types used by the annotation processor */
interface BaseTypesConfigAnnotationProcessorPlugin : AnnotationProcessorPlugin {

    val stateServiceClassName: ClassName
    val stateServiceDelegateClassName: ClassName

    val rpcDelegateClassName: ClassName
    val rpcConnectionDelegateClassName: ClassName
    val serviceHubDelegateClassName: ClassName
    val poolBoyDelegateClassName: ClassName
}


@AutoService(BaseTypesConfigAnnotationProcessorPlugin::class)
class DefaultBaseTypesConfigAnnotationProcessorPlugin : BaseTypesConfigAnnotationProcessorPlugin {
    override val stateServiceClassName = DefaultExtendedStateService::class.asClassName()
    override val stateServiceDelegateClassName = StateServiceDelegate::class.asClassName()
    override val poolBoyDelegateClassName = StateServicePoolBoyDelegate::class.asClassName()
    override val rpcDelegateClassName = StateServiceRpcDelegate::class.asClassName()
    override val rpcConnectionDelegateClassName = StateServiceRpcConnectionDelegate::class.asClassName()
    override val serviceHubDelegateClassName = StateCordaServiceDelegate::class.asClassName()
    override fun getSupportPriority(annotatedElementInfo: AnnotatedElementInfo, strategy: String?): Int = 1
}
