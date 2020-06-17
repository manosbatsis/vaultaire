package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoMembersStrategy
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.VariableElement

class VaultaireRestDtoMemberStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext
): SimpleDtoMembersStrategy(processingEnvironment, dtoInputContext){

    override fun getToPatchedFunctionBuilder(
            originalTypeParameter: ParameterSpec
    ): FunSpec.Builder {
        val functionBuilder = super.getToPatchedFunctionBuilder(originalTypeParameter)
        addStateServiceParameter(functionBuilder)
        return functionBuilder
    }

    override fun getToTargetTypeFunctionBuilder(): FunSpec.Builder {
        val functionBuilder = super.getToTargetTypeFunctionBuilder()
        addStateServiceParameter(functionBuilder)
        return functionBuilder
    }

    private fun addStateServiceParameter(functionBuilder: FunSpec.Builder) {
        functionBuilder.addParameter(
                "stateService",
                StateService::class.asClassName().parameterizedBy(
                        dtoInputContext.originalTypeName))
    }

    override fun processFields(
            typeSpecBuilder: TypeSpec.Builder,
            fields: List<VariableElement>) {
        fields.forEachIndexed { index, originalProperty ->
            val commaOrEmpty = if (index + 1 < fields.size) "," else ""
            // Tell KotlinPoet that the property is initialized via the constructor parameter,
            // by creating both a constructor param and member property
            val propertyName = toPropertyName(originalProperty)
            val propertyType = toPropertyTypeName(originalProperty)
            val propertyDefaultValue = toDefaultValueExpression(originalProperty)
            dtoConstructorBuilder.addParameter(ParameterSpec.builder(propertyName, propertyType)
                    .defaultValue(propertyDefaultValue)
                    .build())
            val propertySpecBuilder = PropertySpec.builder(propertyName, propertyType)
                    .mutable()
                    .addModifiers(PUBLIC)
                    .initializer(propertyName)
            addPropertyAnnotations(propertySpecBuilder, originalProperty)
            typeSpecBuilder.addProperty(propertySpecBuilder.build())
            // Add line to patch function
            patchFunctionCodeBuilder.addStatement(toPatchStatement(originalProperty, commaOrEmpty))
            // Add line to map function
            toStateFunctionCodeBuilder.addStatement(toMapStatement(originalProperty, commaOrEmpty))
            // Add line to alt constructor
            dtoAltConstructorCodeBuilder.addStatement(toAltConstructorStatement(originalProperty, commaOrEmpty))
        }
        finalize(typeSpecBuilder)
    }
}