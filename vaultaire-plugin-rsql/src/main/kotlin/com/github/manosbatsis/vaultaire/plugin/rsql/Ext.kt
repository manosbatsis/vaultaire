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

import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.rsql.support.SimpleRsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.util.Fields
import cz.jirutka.rsql.parser.RSQLParser
import net.corda.core.schemas.StatePersistable

/**
 * [VaultQueryCriteriaCondition] extension that
 * adds the given RSQL filter to the current instance,
 * then returns it for chaining.
 *
 * Example:
 * ```kotlin
 * bookQuery{
 *    status = Vault.StateStatus.ALL
 *    // ...
 *    orderBy {
 *        fields.title sort DESC
 *    }
 * }
 * // Add the RSQL query filter
 * .withRsql("authorFirst==john;authorLast==doe")
 * // Convert all the above
 * // to Corda QueryCriteria
 * .toCriteria()
 * ```
 */
fun <P : StatePersistable, F : Fields<P>, C: VaultQueryCriteriaCondition<P, F>> C.withRsql(
    rsql: String?,
    converterFactory: RsqlArgumentsConverterFactory<P, F> =
        SimpleRsqlArgumentsConverter.Factory()
): C {
    if(!rsql.isNullOrBlank()) {
        RSQLParser(RsqlSearchOperation.asSimpleOperators)
            .parse(CustomOperators.preProcessRsql(rsql!!))
            .accept(RsqlConditionsVisitor(
                this,
                converterFactory.create(this)))
            ?.also { this.addCondition(it) }
    }
    return this
}
