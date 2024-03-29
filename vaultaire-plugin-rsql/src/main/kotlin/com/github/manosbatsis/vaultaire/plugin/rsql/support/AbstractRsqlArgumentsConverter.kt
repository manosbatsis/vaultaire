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
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlCriterion
import com.github.manosbatsis.vaultaire.util.Fields
import cz.jirutka.rsql.parser.ast.RSQLOperators
import net.corda.core.schemas.StatePersistable
import java.lang.reflect.Field
import kotlin.reflect.jvm.javaField

/**
 * Base [RsqlArgumentsConverter] implementation,
 * (optionally) extend to create your custom converter.
 * See [SimpleRsqlArgumentsConverter] and
 * [ObjectMapperAdapterRsqlArgumentsConverter] for examples.
 */
abstract class AbstractRsqlArgumentsConverter<P : StatePersistable, out F : Fields<P>>(
    val rootCondition: VaultQueryCriteriaCondition<P, F>
): RsqlArgumentsConverter<P, F> {

    companion object{
        val collectionOperators = listOf(RSQLOperators.IN, RSQLOperators.NOT_IN)
    }
    abstract fun convertItem(fieldType: Class<*>, arg: String): Any

    override fun convertArguments(criterion: RsqlCriterion): List<*> {
        val field: Field = rootCondition.fields.fieldsByName[criterion.property]?.property?.javaField
            ?: error("Cannot convert arguments for non-existing property or field ${criterion.property}")
        val fieldType: Class<*> = field.type
        return criterion.arguments.map { arg ->
            if(arg == null) arg
            else convertItem(fieldType, arg)
        }
    }
}