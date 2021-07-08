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

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoMembersStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoStrategyLesserComposition
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.SimpleDtoMembersStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.util.FieldContext
import com.github.manosbatsis.kotlin.utils.kapt.processor.ToTargetTypeFunctionConfig
import com.squareup.kotlinpoet.*
import javax.lang.model.element.VariableElement

open class ViewDtoMembersStrategy(
        rootDtoStrategy: DtoStrategyLesserComposition
) : DtoMembersStrategy, SimpleDtoMembersStrategy(
        rootDtoStrategy
) {

    override fun addAltConstructor(typeSpecBuilder: TypeSpec.Builder, dtoAltConstructorBuilder: FunSpec.Builder) {
        // NO-OP
    }


    override fun isNullable(
            variableElement: VariableElement, fieldContext: FieldContext
    ): Boolean = when (fieldContext) {
        FieldContext.GENERATED_TYPE -> rootDtoMembersStrategy.toPropertyTypeName(variableElement).isNullable
        FieldContext.TARGET_TYPE -> true
        FieldContext.MIXIN_TYPE -> true
    }

    override fun getToPatchedFunctionBuilder(
            originalTypeParameter: ParameterSpec
    ): FunSpec.Builder {
        val patchFunctionBuilder = FunSpec.builder("toPatched")
                .addAnnotation(Suspendable::class)
                .addModifiers(KModifier.OVERRIDE)
                .addKdoc(CodeBlock.builder()
                        .addStatement("Create a patched copy of the given [%T] instance,", dtoTypeStrategy.getDtoTarget())
                        .addStatement("updated using this view's non-null properties.").build())
                .addParameter(originalTypeParameter)
                .returns(dtoTypeStrategy.getDtoTarget())
        return patchFunctionBuilder
    }

    override fun getToTargetTypeFunctionBuilder(): FunSpec.Builder {

        with(annotatedElementInfo.toTargetTypeFunctionConfig) {
            val useTargetTypeName = targetTypeNameOverride ?: dtoTypeStrategy.getDtoTarget()
            val toStateFunctionBuilder = FunSpec.builder("toTargetType")
                    .addAnnotation(Suspendable::class)
                    .addModifiers(KModifier.OVERRIDE)
                    .addKdoc(if (skip)
                        CodeBlock.builder().addStatement("Not yet implemented").build()
                    else CodeBlock.builder()
                            .addStatement("Create an instance of [%T], using this view's properties.", useTargetTypeName).build())
                    .returns(useTargetTypeName)
            params.forEach { toStateFunctionBuilder.addParameter(it) }
            return toStateFunctionBuilder
        }
    }

/*
    override fun toTargetTypeStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {
        val propertyName = rootDtoMembersStrategy.toPropertyName(variableElement)
        return DtoMembersStrategy.Statement("      $propertyName = this.$propertyName$commaOrEmpty")
    }

    override fun toPatchStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {
        val propertyName = rootDtoMembersStrategy.toPropertyName(variableElement)
        val isNullable = rootDtoMembersStrategy.toPropertyTypeName(variableElement).isNullable
        val nullableSuffix = if(isNullable) " ?: original.$propertyName" else ""
        return DtoMembersStrategy.Statement("      $propertyName = this.$propertyName$nullableSuffix$commaOrEmpty")
    }
*/
}