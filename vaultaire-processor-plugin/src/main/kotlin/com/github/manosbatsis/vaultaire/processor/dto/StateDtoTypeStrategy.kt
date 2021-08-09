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


import com.github.manosbatsis.kotlin.utils.api.Dto
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoStrategyLesserComposition
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.SimpleDtoTypeStrategy
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.dto.VaultaireDto
import com.github.manosbatsis.vaultaire.dto.VaultaireDtoBase
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asTypeName
import net.corda.core.serialization.CordaSerializable

open class StateDtoTypeStrategy(
        rootDtoStrategy: DtoStrategyLesserComposition
) : SimpleDtoTypeStrategy(rootDtoStrategy) {


    override fun getRootDtoType(): TypeName = VaultaireDtoBase::class.java.asTypeName()

    override fun getDtoInterface(): Class<*> = VaultaireDto::class.java

    override fun addAnnotations(typeSpecBuilder: Builder) {
        super.addAnnotations(typeSpecBuilder)
        typeSpecBuilder.addAnnotation(CordaSerializable::class.java)
    }
}
