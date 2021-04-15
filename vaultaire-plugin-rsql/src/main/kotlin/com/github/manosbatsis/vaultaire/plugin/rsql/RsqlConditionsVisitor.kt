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
import com.github.manosbatsis.vaultaire.plugin.rsql.support.SimpleRsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.util.Fields
import cz.jirutka.rsql.parser.ast.AndNode
import cz.jirutka.rsql.parser.ast.ComparisonNode
import cz.jirutka.rsql.parser.ast.OrNode
import cz.jirutka.rsql.parser.ast.RSQLVisitor
import net.corda.core.schemas.StatePersistable

/**
 * Parses RSQL (a superset of FIQL) into [Condition]s that can be added to
 * [VaultQueryCriteriaCondition]. The latter can then generate complete
 * Vault query criteria that takes the RSQL string into account.
 *
 * See also:
 * - [REST Query Language with RSQL](https://www.baeldung.com/rest-api-search-language-rsql-fiql)
 * - [RSQL Parser](https://github.com/jirutka/rsql-parser)
 * - [FIQL: The Feed Item Query Language](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00)
 */
@Suppress("MemberVisibilityCanBePrivate")
open class RsqlConditionsVisitor<P : StatePersistable, out F : Fields<P>>(
    protected val rootCondition: VaultQueryCriteriaCondition<P, F>,
    protected val argumentsConverter: RsqlArgumentsConverter<P, F> =
        SimpleRsqlArgumentsConverter(rootCondition)
): RSQLVisitor<Condition?, Unit> {

    val builder: RsqlConditionBuilder<P, F> =
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