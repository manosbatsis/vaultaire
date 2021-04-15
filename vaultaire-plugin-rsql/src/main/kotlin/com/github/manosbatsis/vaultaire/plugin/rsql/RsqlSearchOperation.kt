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

import cz.jirutka.rsql.parser.ast.ComparisonOperator
import cz.jirutka.rsql.parser.ast.RSQLOperators
import java.util.Arrays

object CustomOperators{

    val LIKE = ComparisonOperator("=like=")
    val NOT_LIKE = ComparisonOperator("=unlike=", "=notlike=", "=nonlike=")
    val IS_NULL = ComparisonOperator("=null=", "=isnull=")
    val NOT_NULL = ComparisonOperator("=notnull=", "=nonnull=")
    private val all = listOf(IS_NULL, NOT_NULL)

    fun preProcessRsql(rsql: String): String {
        var processed = "$rsql;"
        all.forEach { operator ->
            operator.symbols.forEach { symbol ->
                processed = processed.replace("${symbol};", "${symbol}null;")
            }
        }
        return processed.removeSuffix(";")
    }
}
/**
 * Maps from [ComparisonOperator]
 */
enum class RsqlSearchOperation(val operator: ComparisonOperator) {
    // Operators provided by RSQL parser
    EQUAL(RSQLOperators.EQUAL),
    NOT_EQUAL(RSQLOperators.NOT_EQUAL),

    GREATER_THAN(RSQLOperators.GREATER_THAN),
    GREATER_THAN_OR_EQUAL(RSQLOperators.GREATER_THAN_OR_EQUAL),
    LESS_THAN(RSQLOperators.LESS_THAN),
    LESS_THAN_OR_EQUAL(RSQLOperators.LESS_THAN_OR_EQUAL),

    IN(RSQLOperators.IN),
    NOT_IN(RSQLOperators.NOT_IN),

    // Custom operators
    LIKE(CustomOperators.LIKE),
    NOT_LIKE(CustomOperators.NOT_LIKE),

    IS_NULL(CustomOperators.IS_NULL),
    NOT_NULL(CustomOperators.NOT_NULL);

    companion object {
        val asSimpleOperators: Set<ComparisonOperator> = RSQLOperators.defaultOperators() +
                listOf(CustomOperators.LIKE, CustomOperators.NOT_LIKE,
                    CustomOperators.IS_NULL, CustomOperators.NOT_NULL)
        fun getSimpleOperator(operator: ComparisonOperator): RsqlSearchOperation {
            return Arrays.stream(values())
                .filter { operation: RsqlSearchOperation -> operation.operator === operator }
                .findAny().orElse(null)
        }
    }
}