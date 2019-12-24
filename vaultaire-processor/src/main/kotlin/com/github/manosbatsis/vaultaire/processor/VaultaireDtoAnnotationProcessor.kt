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
package com.github.manosbatsis.vaultaire.processor

import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDto
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency
import com.github.manosbatsis.vaultaire.dto.Dto
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME
import com.github.manosbatsis.vaultaire.util.DtoInsufficientStateMappingException
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import net.corda.core.contracts.ContractState
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion

/**
 * Kapt processor for the `@VaultaireGenerate` annotation.
 * Constructs a VaultaireGenerate for the annotated class.
 */
@SupportedAnnotationTypes(
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDto",
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
        KAPT_KOTLIN_GENERATED_OPTION_NAME,
        KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME)
class VaultaireDtoAnnotationProcessor : BaseStateInfoAnnotationProcessor() {

    override val sourcesAnnotation = VaultaireGenerateDto::class.java
    override val dependenciesAnnotation = VaultaireGenerateDtoForDependency::class.java


    /** Write a DTO for the given [ContractState] . */
    override fun process(stateInfo: StateInfo) {

        // Generate the Kotlin file
        getFileSpecBuilder(stateInfo.generatedPackageName, "${stateInfo.contractStateTypeElement.simpleName}VaultaireGeneratedDto")
                .addType(contractStateDtoSpecBuilder(stateInfo).build())
                .build()
                .writeTo(sourceRootFile)
    }

    private fun contractStateDtoSpecBuilder(stateInfo: StateInfo): TypeSpec.Builder {
        val contractStateTypeName = stateInfo.contractStateTypeElement.asKotlinTypeName()
        // Create DTO type
        val dtoTypeSpecBuilder = TypeSpec.classBuilder(
                ClassName(stateInfo.generatedPackageName, "${stateInfo.contractStateSimpleName}Dto"))
                .addSuperinterface(Dto::class.asClassName().parameterizedBy(contractStateTypeName))
                .addModifiers(DATA)
                .addKdoc("A [%T]-specific [%T] implementation", contractStateTypeName, Dto::class)
        // Contract state parameter, used in alt constructor and util functions
        val stateParameter = ParameterSpec.builder("original", contractStateTypeName).build()
        // Create DTO primary constructor
        val dtoConstructorBuilder = FunSpec.constructorBuilder()
        // Create DTO alternative constructor
        val dtoAltConstructorBuilder = FunSpec.constructorBuilder().addParameter(stateParameter)
                .addKdoc(CodeBlock.builder()
                        .addStatement("Alternative constructor, used to map ")
                        .addStatement("from the given [%T] instance.", contractStateTypeName).build())
        val dtoAltConstructorCodeBuilder = CodeBlock.builder().addStatement("")
        // Create patch function
        val patchFunctionBuilder = FunSpec.builder("toPatched")
                .addModifiers(OVERRIDE)
                .addKdoc(CodeBlock.builder()
                        .addStatement("Create a patched copy of the given [%T] instance,", contractStateTypeName)
                        .addStatement("updated using this DTO's non-null properties.").build())
                .addParameter(stateParameter)
                .returns(contractStateTypeName)
        val patchFunctionCodeBuilder = CodeBlock.builder().addStatement("val patched = %T(", contractStateTypeName)
        // Create mapping function
        val toStateFunctionBuilder = FunSpec.builder("toState")
                .addModifiers(OVERRIDE)
                .addKdoc(CodeBlock.builder()
                        .addStatement("Create an instance of [%T], using this DTO's properties.", contractStateTypeName)
                        .addStatement("May throw a [DtoInsufficientStateMappingException] ")
                        .addStatement("if there is mot enough information to do so.").build())
                .returns(contractStateTypeName)
        val toStateFunctionCodeBuilder = CodeBlock.builder()
                .addStatement("try {")
                .addStatement("   val state = %T(", contractStateTypeName)

        stateInfo.contractStateFields.forEachIndexed { index, variableElement ->
            val commaOrEmpty = if(index + 1 < stateInfo.contractStateFields.size) "," else ""
            // Tell KotlinPoet that the property is initialized via the constructor parameter,
            // by creating both a constructor param and member property
            val propertyName = variableElement.simpleName.toString()
            val propertyType = variableElement.asKotlinTypeName().copy(nullable = true)
            dtoConstructorBuilder.addParameter(ParameterSpec.builder(propertyName, propertyType)
                    .defaultValue("null")
                    .build())
            dtoTypeSpecBuilder.addProperty(PropertySpec.builder(propertyName, propertyType)
                    .mutable()
                    .addModifiers(PUBLIC)
                    .initializer(propertyName).build())
            // Add line to path function
            patchFunctionCodeBuilder.addStatement("      $propertyName = this.$propertyName ?: original.$propertyName$commaOrEmpty")
            // Add line to map function
            val nullableOrNot = if(variableElement.isNullable()) "" else "!!"
            toStateFunctionCodeBuilder.addStatement("      $propertyName = this.$propertyName$nullableOrNot$commaOrEmpty")
            // Add line to alt constructor
            dtoAltConstructorCodeBuilder.addStatement("      $propertyName = original.$propertyName$commaOrEmpty")
        }

        // Complete alt constructor
        dtoAltConstructorBuilder.callThisConstructor(dtoAltConstructorCodeBuilder.build())
        // Complete patch function
        patchFunctionCodeBuilder.addStatement(")")
        patchFunctionCodeBuilder.addStatement("return patched")
        // Complete mappiong function

        toStateFunctionCodeBuilder.addStatement("   )")
        toStateFunctionCodeBuilder.addStatement("   return state")
        toStateFunctionCodeBuilder.addStatement("}")
        toStateFunctionCodeBuilder.addStatement("catch(e: Exception) {")
        toStateFunctionCodeBuilder.addStatement("   throw %T(exception = e)", DtoInsufficientStateMappingException::class)
        toStateFunctionCodeBuilder.addStatement("}", DtoInsufficientStateMappingException::class)

        return dtoTypeSpecBuilder
                .primaryConstructor(dtoConstructorBuilder.build())
                .addFunction(dtoAltConstructorBuilder.build())
                .addFunction(patchFunctionBuilder.addCode(patchFunctionCodeBuilder.build()).build())
                .addFunction(toStateFunctionBuilder.addCode(toStateFunctionCodeBuilder.build()).build())
    }
}
