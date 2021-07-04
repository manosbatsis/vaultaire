package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoMembersStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoStrategyLesserComposition
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.SimpleDtoMembersStrategy
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


    override fun getToPatchedFunctionBuilder(
            originalTypeParameter: ParameterSpec
    ): FunSpec.Builder {
        val patchFunctionBuilder = FunSpec.builder("toPatched")
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


    override fun toTargetTypeStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {
        val propertyName = rootDtoMembersStrategy.toPropertyName(variableElement)
        return DtoMembersStrategy.Statement("      $propertyName = this.$propertyName$commaOrEmpty")
    }

    override fun toPatchStatement(fieldIndex: Int, variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {
        val propertyName = rootDtoMembersStrategy.toPropertyName(variableElement)
        val isNullable = rootDtoMembersStrategy.toPropertyTypeName(variableElement).isNullable
        val nullableSuffix = if(isNullable) "?: original.$propertyName$commaOrEmpty" else ""
        return DtoMembersStrategy.Statement("      $propertyName = this.$propertyName $nullableSuffix")
    }

}