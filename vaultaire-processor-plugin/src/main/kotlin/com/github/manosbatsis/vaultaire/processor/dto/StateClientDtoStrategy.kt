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
package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo

/** REST friendly, i.e. "lite"  overrides for building a Vaultaire-specific DTO type spec */
open class StateClientDtoStrategy(
        annotatedElementInfo: AnnotatedElementInfo
) : BaseVaultaireDtoStrategy<StateClientDtoNameStrategy, StateClientDtoTypeStrategy, StateClientDtoMembersStrategy>(
        annotatedElementInfo = annotatedElementInfo,
        dtoNameStrategyConstructor = ::StateClientDtoNameStrategy,
        dtoTypeStrategyConstructor = ::StateClientDtoTypeStrategy,
        dtoMembersStrategyConstructor = ::StateClientDtoMembersStrategy
){
    override fun with(annotatedElementInfo: AnnotatedElementInfo): StateClientDtoStrategy {
        return StateClientDtoStrategy(annotatedElementInfo)
    }
}