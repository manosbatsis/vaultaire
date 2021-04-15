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

import com.github.manosbatsis.vaultaire.dsl.query.AndCondition
import com.github.manosbatsis.vaultaire.dsl.query.Condition
import com.github.manosbatsis.vaultaire.dsl.query.OrCondition
import com.github.manosbatsis.vaultaire.dsl.query.VaultCustomQueryCondition
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.EQUAL
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.GREATER_THAN
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.GREATER_THAN_OR_EQUAL
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.IN
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.IS_NULL
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.LESS_THAN
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.LESS_THAN_OR_EQUAL
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.LIKE
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.NOT_EQUAL
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.NOT_IN
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.NOT_LIKE
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.NOT_NULL
import com.github.manosbatsis.vaultaire.util.FieldWrapper
import com.github.manosbatsis.vaultaire.util.Fields
import com.github.manosbatsis.vaultaire.util.TypedFieldWrapper
import cz.jirutka.rsql.parser.ast.ComparisonNode
import cz.jirutka.rsql.parser.ast.LogicalNode
import cz.jirutka.rsql.parser.ast.LogicalOperator.AND
import cz.jirutka.rsql.parser.ast.LogicalOperator.OR
import cz.jirutka.rsql.parser.ast.Node
import net.corda.core.node.services.vault.Builder.`in`
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.Builder.greaterThan
import net.corda.core.node.services.vault.Builder.greaterThanOrEqual
import net.corda.core.node.services.vault.Builder.isNull
import net.corda.core.node.services.vault.Builder.lessThan
import net.corda.core.node.services.vault.Builder.lessThanOrEqual
import net.corda.core.node.services.vault.Builder.like
import net.corda.core.node.services.vault.Builder.notEqual
import net.corda.core.node.services.vault.Builder.notIn
import net.corda.core.node.services.vault.Builder.notLike
import net.corda.core.node.services.vault.Builder.notNull
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.schemas.StatePersistable
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty1

/**
 * Used by [RsqlConditionsVisitor] to build the actual [Condition]s.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class RsqlConditionBuilder<P : StatePersistable, out F : Fields<P>>(
    val rootCondition: VaultQueryCriteriaCondition<P, F>,
    val argumentsConverter: RsqlArgumentsConverter<P, F>
){
    companion object{
        private val logger = LoggerFactory.getLogger(RsqlConditionBuilder::class.java)
    }

    /**
     * Create a condition for any node type.
     * Delegates to the appropriate function.
     */
    fun createCondition(node: Node): Condition? =
        when(node){
            is LogicalNode -> createCondition(node)
            is ComparisonNode -> createCondition(node)
            else -> null
        }

    /**
     * Create a condition for a logical operator node.
     */
    fun createCondition(logicalNode: LogicalNode): Condition? {
        // Create the logical operator
        val compositeChildCondition = when(logicalNode.operator){
            AND -> AndCondition(rootCondition.fields, rootCondition.rootCondition)
            OR -> OrCondition(rootCondition.fields, rootCondition.rootCondition)
        }
        // Add child nodes
        logicalNode.children
            .mapNotNull { it?.let { createCondition(it) } }
            .forEach { compositeChildCondition.addCondition(it) }
        return compositeChildCondition
    }

    /**
     * Create a condition for a comparison operator node.
     */
    @Suppress("UNCHECKED_CAST")
    fun createCondition(comparisonNode: ComparisonNode): Condition? {
        val criterion = with(comparisonNode){
            RsqlCriterion(selector, operator, arguments)
        }
        val args: List<Any?> = argumentsConverter.convertArguments(criterion)
        val operator = RsqlSearchOperation.getSimpleOperator(criterion.operator)
        val field: FieldWrapper<P> = rootCondition.fields.fieldsByName[comparisonNode.selector]!!
        val expression: CriteriaExpression<P, Boolean> = when (operator) {
            EQUAL -> field.property.equal(args.single())
            NOT_EQUAL -> field.property.notEqual(args.single())
            LIKE -> field.like(args.single())
            NOT_LIKE -> field.notLike(args.single())
            GREATER_THAN -> field.greaterThan(args.single())
            GREATER_THAN_OR_EQUAL -> field.greaterThanOrEqual(args.single())
            LESS_THAN -> field.lessThan(args.single())
            LESS_THAN_OR_EQUAL -> field.lessThanOrEqual(args.single())
            IN -> field.isIn(args)
            NOT_IN -> field.notIn(args)
            IS_NULL -> field.property.isNull()
            NOT_NULL -> field.property.notNull()
        }
        return VaultCustomQueryCondition(expression, rootCondition.status)
    }

    fun FieldWrapper<P>.greaterThan(value: Any?): CriteriaExpression<P, Boolean> =
        with(toComparable(property, value)){ first.greaterThan(second) }

    fun FieldWrapper<P>.lessThan(value: Any?): CriteriaExpression<P, Boolean> =
        with(toComparable(property, value)){ first.lessThan(second) }

    fun FieldWrapper<P>.greaterThanOrEqual(value: Any?): CriteriaExpression<P, Boolean> =
        with(toComparable(property, value)){ first.greaterThanOrEqual(second) }

    fun FieldWrapper<P>.lessThanOrEqual(value: Any?): CriteriaExpression<P, Boolean> =
        with(toComparable(property, value)){ first.lessThanOrEqual(second) }

    fun FieldWrapper<P>.isIn(value: Collection<*>): CriteriaExpression<P, Boolean> =
        with(toComparables(property, value)){ first.`in`(second) }

    fun FieldWrapper<P>.notIn(value: Collection<*>): CriteriaExpression<P, Boolean> =
        with(toComparables(property, value)){ first.notIn(second) }

    fun FieldWrapper<P>.like(value: Any?): CriteriaExpression<P, Boolean> =
        if (value is String) toStringField().property.like(processWildcards(value))
        else error("The like operator requires a non-null string argument")

    fun FieldWrapper<P>.notLike(value: Any?): CriteriaExpression<P, Boolean> =
        if (value is String) toStringField().property.notLike(processWildcards(value))
        else error("The notlike operator requires a non-null string argument")

    private fun processWildcards(value: String) = value.replace('*', '%')

    @Suppress("UNCHECKED_CAST")
    private fun FieldWrapper<P>.toStringField() = (this as TypedFieldWrapper<P, String>)

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified S : Any, C: Comparable<S>> toComparable(
        property: KProperty1<P, *>, value: S?
    ) = if(value is Comparable<*>)
        Pair(property as KProperty1<P, C?>, value as C)
    else error("Input does not implement Comparable: $value")

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified S : Any?, C: Comparable<S>> toComparables(
        property: KProperty1<P, *>, value: Collection<S>
    ): Pair<KProperty1<P, C?>, Collection<C>> =
        Pair(property as KProperty1<P, C?>, value as Collection<C>)
}