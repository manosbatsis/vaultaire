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

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.ConstructorRefsCompositeDtoStrategy
import com.github.manosbatsis.vaultaire.dto.VaultaireDto
import com.github.manosbatsis.vaultaire.processor.AbstractVaultaireDtoAnnotationProcessor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import javax.lang.model.element.VariableElement

class DtoViewStrategy(
        val viewInfo: AbstractVaultaireDtoAnnotationProcessor.ViewInfo,
        val originalStrategy: ConstructorRefsCompositeDtoStrategy<*, *, *>
    ): ConstructorRefsCompositeDtoStrategyCopycat(
        originalStrategy = originalStrategy,
        dtoMembersStrategyConstructor = ::ViewDtoMembersStrategy
){

    val viewFieldSpecs = viewInfo.viewAnnotation.viewFields.associateBy { it.name }

    override fun dtoTypeSpecBuilder(): TypeSpec.Builder {
        val dtoTypeSpecBuilder = TypeSpec.classBuilder(getClassName())
        addSuperTypes(dtoTypeSpecBuilder)
        addModifiers(dtoTypeSpecBuilder)
        addKdoc(dtoTypeSpecBuilder)
        addAnnotations(dtoTypeSpecBuilder)
        addMembers(dtoTypeSpecBuilder)

        return dtoTypeSpecBuilder
    }

    override fun addMembers(typeSpecBuilder: TypeSpec.Builder) {
        processFields(typeSpecBuilder, getFieldsToProcess())
        processDtoOnlyFields(typeSpecBuilder, getExtraFieldsFromMixin())
        finalize(typeSpecBuilder)
    }

    override fun getFieldsToProcess(): List<VariableElement> {
        val fields = originalStrategy.getFieldsToProcess()
                .includeNames(getFieldIncludes()).excludeNames(getFieldExcludes())
        return fields
    }

    override fun getExtraFieldsFromMixin(): List<VariableElement> {
        val fields = originalStrategy.getExtraFieldsFromMixin()
                .includeNames(getFieldIncludes()).excludeNames(getFieldExcludes())
        return fields
    }

    override fun getFieldIncludes(): List<String> {
        val names = super.getFieldIncludes().toMutableSet().also {
            val includeNamedFields = viewInfo.viewAnnotation.includeNamedFields
            it.addAll(includeNamedFields)
            it.addAll(viewInfo.viewAnnotation.viewFields.filter{
                it.ignoreIfNotIncludeNamedField == false || includeNamedFields.contains(it.name)
            }.map{it.name})
        }.toList()
        return names
    }

    override fun getFieldExcludes(): List<String> {
        val names = super.getFieldExcludes().toMutableSet().also {
            it.addAll(viewInfo.viewAnnotation.excludeNamedFields)
        }.toList()
        return names
    }

    override fun getClassName(): ClassName {
        val mappedPackageName = mapPackageName(annotatedElementInfo.generatedPackageName)
        val simpleName = when {
            viewInfo.targetName.isNotBlank() -> "${viewInfo.targetName}${viewInfo.targetNameSuffix}"
            viewInfo.targetNameSuffix.isNotBlank() -> "${annotatedElementInfo.primaryTargetTypeElement.simpleName}${viewInfo.targetNameSuffix}"
            else -> throw IllegalArgumentException("A @VaultaireView must define either a name, a nameSuffix, or both ")
        }
        return ClassName(mappedPackageName, simpleName)
    }

    fun getAllProcessedFieldNames(): Set<String> {
        val fields = getFieldsToProcess() + getExtraFieldsFromMixin()
        return fields.map{ it.simpleName.toString() }.toSet()
    }

    override fun addKdoc(typeSpecBuilder: TypeSpec.Builder) {
        val fieldNames = getAllProcessedFieldNames().joinToString()
        typeSpecBuilder.addKdoc("A [%T]-based view subset with fields: $fieldNames",
                getDtoTarget())
    }

    /** Override to change the super types the DTO extends or implements  */
    override fun addSuperTypes(typeSpecBuilder: TypeSpec.Builder){
        typeSpecBuilder.addSuperinterface(VaultaireDto::class.java.asClassName()
                .parameterizedBy(originalStrategy.getClassName()))
    }

    /** Override to change how the DTO target type is resolved  */
    override fun getDtoTarget(): TypeName {
        return originalStrategy.getClassName()
    }

    override fun toPropertyTypeName(variableElement: VariableElement): TypeName =
            originalStrategy.toPropertyTypeName(variableElement)
                    .let{ typeName ->
                        val viewField = viewFieldSpecs[variableElement.simpleName.toString()]
                        if(viewField != null) typeName.copy(nullable = !viewField.nonNull)
                        else typeName
                    }
}