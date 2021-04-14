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
package com.github.manosbatsis.vaultaire.plugin.rsql

import com.github.manosbatsis.vaultaire.dsl.query.Condition
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.util.Fields
import cz.jirutka.rsql.parser.ast.AndNode
import cz.jirutka.rsql.parser.ast.ComparisonNode
import cz.jirutka.rsql.parser.ast.OrNode
import cz.jirutka.rsql.parser.ast.RSQLVisitor
import net.corda.core.schemas.StatePersistable

class RootConditionWrappingRsqlVisitor<P : StatePersistable, out F : Fields<P>>(
    val rootCondition: VaultQueryCriteriaCondition<P, F>,
    val argumentsConverter: RsqlConditionArgumentsConverter<P, F> =
        SimpleRsqlConditionArgumentsConverter(rootCondition)
): RSQLVisitor<Condition?, Unit> {

    private val builder: RsqlConditionBuilder<P, F> =
        RsqlConditionBuilder(rootCondition, argumentsConverter)

    override fun visit(node: AndNode, param: Unit?): Condition? {
        return builder.createCondition(node)
    }

    override fun visit(node: OrNode, param: Unit?): Condition? {
        return builder.createCondition(node)
    }

    override fun visit(node: ComparisonNode, param: Unit?): Condition? {
        return builder.createCondition(node)
    }

}