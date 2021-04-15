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
package com.github.manosbatsis.vaultaire.plugin.rsql.support

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverterFactory
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlCriterion
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.schemas.StatePersistable
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import kotlin.reflect.jvm.javaField



/**
 * An [RsqlArgumentsConverter] implementation suitable for
 * Spring and Spring Boot applications. Used as adapter for
 * an [ConversionService] instance.
 */
class ConversionServiceAdapterRsqlArgumentsConverter<P : StatePersistable, out F : Fields<P>>(
    val rootCondition: VaultQueryCriteriaCondition<P, F>,
    val conversionService: ConversionService
): RsqlArgumentsConverter<P, F> {

    companion object {
        private val logger = LoggerFactory.getLogger(RsqlArgumentsConverter::class.java)
        val stringTypeDescriptor = TypeDescriptor.valueOf(String::class.java)
    }

    class Factory<P : StatePersistable, F : Fields<P>>(
        val conversionService: ConversionService
    ) :RsqlArgumentsConverterFactory<P, F> {
        override fun create(rootCondition: VaultQueryCriteriaCondition<P, F>) =
            ConversionServiceAdapterRsqlArgumentsConverter(
                rootCondition, conversionService)
    }

    override fun convertArguments(criterion: RsqlCriterion): List<Any?> {
        val targetTypeDescriptor: TypeDescriptor = rootCondition.fields
            .fieldsByName[criterion.property]
            ?.let { wrapper ->
                wrapper.property.javaField?.let { TypeDescriptor(it) }
            }
            ?: error("Cannot convert arguments for non-existing property or field ${criterion.property}")
        return criterion.arguments.map { arg ->
            conversionService.convert(arg, stringTypeDescriptor, targetTypeDescriptor)
        }
    }

}