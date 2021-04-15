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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlArgumentsConverterFactory
import com.github.manosbatsis.vaultaire.util.Fields
import net.corda.core.schemas.StatePersistable


/**
 * An [RsqlArgumentsConverter] implementation suitable for
 * applications that make use of Jackson. Used as adapter for
 * an [ObjectMapper] instance.
 */
class ObjectMapperAdapterRsqlArgumentsConverter<P : StatePersistable, out F : Fields<P>>(
    rootCondition: VaultQueryCriteriaCondition<P, F>,
    private val objectMapper: ObjectMapper
): AbstractRsqlArgumentsConverter<P, F>(rootCondition) {

    class Factory<P : StatePersistable, F : Fields<P>>(
        val objectMapper: ObjectMapper
    ) : RsqlArgumentsConverterFactory<P, F> {
        override fun create(rootCondition: VaultQueryCriteriaCondition<P, F>) =
            ObjectMapperAdapterRsqlArgumentsConverter(
                rootCondition, objectMapper)
    }

    override fun convertItem(fieldType: Class<*>, arg: String): Any =
        objectMapper.readValue(arg, fieldType)
}