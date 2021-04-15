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
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.NOT_EQUAL
import com.github.manosbatsis.vaultaire.plugin.rsql.RsqlSearchOperation.NOT_IN
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
    
    fun createCondition(node: Node): Condition? =
        when(node){
            is LogicalNode -> createCondition(node)
            is ComparisonNode -> createCondition(node)
            else -> null
        }
    
    fun createCondition(logicalNode: LogicalNode): Condition? {
        val childNodes: List<Condition> = logicalNode.children
            .mapNotNull { node: Node? ->
                node?.let { createCondition(node) }
            }

        val compositChildCondition = when(logicalNode.operator){
            AND -> AndCondition(rootCondition.fields, rootCondition.rootCondition)
            OR -> OrCondition(rootCondition.fields, rootCondition.rootCondition)
        }

        childNodes.forEach{
            compositChildCondition.addCondition(it)
        }
        return compositChildCondition
    }

    @Suppress("UNCHECKED_CAST")
    fun createCondition(comparisonNode: ComparisonNode): Condition? {
        val criterion = with(comparisonNode){
            RsqlCriterion(selector, operator, arguments)
        }
        val args: List<Any?> = argumentsConverter.convertArguments(criterion)
        val operator =
            RsqlSearchOperation.getSimpleOperator(criterion.operator)
        val argument = args[0]
        val field: FieldWrapper<P> = rootCondition.fields.fieldsByName[comparisonNode.selector]!!
        val expression: CriteriaExpression<P, Boolean> = when (operator) {
            EQUAL -> {
                if (argument == null) {
                    field.property.isNull()
                } else if (argument is String) {
                    (field as TypedFieldWrapper<P, String>)
                        .property.like(argument.toString().replace('*', '%'))
                } else {
                    field.property.equal(argument)
                }
            }
            NOT_EQUAL -> {
                if (argument == null) {
                    field.property.notNull()
                } else if (argument is String) {
                    (field as TypedFieldWrapper<P, String>).property.notLike(argument.toString().replace('*', '%'))
                } else {
                    field.property.notEqual(argument)
                }
            }
            GREATER_THAN -> field.greaterThan(argument)
            GREATER_THAN_OR_EQUAL -> field.greaterThanOrEqual(argument)
            LESS_THAN -> field.lessThan(argument)
            LESS_THAN_OR_EQUAL -> field.lessThanOrEqual(argument)
            IN -> field.isIn(argument as Collection<*>)
            NOT_IN -> field.notIn(argument as Collection<*>)
            IS_NULL -> field.property.isNull()
            NOT_NULL -> field.property.notNull()
        }

        return toCondition(expression)
    }


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

    private fun toCondition(expression: CriteriaExpression<P, Boolean>) =
        VaultCustomQueryCondition<P>(expression, rootCondition.status)

}