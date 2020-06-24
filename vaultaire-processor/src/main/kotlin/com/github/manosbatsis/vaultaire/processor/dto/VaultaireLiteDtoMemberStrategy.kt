package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manotbatsis.kotlin.utils.kapt.dto.DtoInputContext
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy
import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.SimpleDtoMembersStrategy
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.VariableElement

class VaultaireLiteDtoMemberStrategy(
        processingEnvironment: ProcessingEnvironment,
        dtoInputContext: DtoInputContext
): SimpleDtoMembersStrategy(processingEnvironment, dtoInputContext){

    override fun toMapStatement(variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {
        return if(variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            val propertyName = toPropertyName(variableElement)
            if (variableElement.isNullable())
                DtoMembersStrategy.Statement("      $propertyName = toPartyOrNull(this.$propertyName, stateService, %S)$commaOrEmpty", arrayOf(propertyName))
            else DtoMembersStrategy.Statement("      $propertyName = toParty(this.$propertyName, stateService, %S)$commaOrEmpty", arrayOf(propertyName))
        }
        else super.toMapStatement(variableElement, commaOrEmpty)
    }

    override fun toPatchStatement(variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement? {
        return if(variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            val propertyName = toPropertyName(variableElement)
            if (variableElement.isNullable())
                DtoMembersStrategy.Statement("      $propertyName = toPartyOrDefaultNullable(this.$propertyName, original.$propertyName, stateService, %S)$commaOrEmpty", arrayOf(propertyName))
            else DtoMembersStrategy.Statement("      $propertyName = toPartyOrDefault(this.$propertyName, original.$propertyName, stateService, %S)$commaOrEmpty", arrayOf(propertyName))
        }
        else super.toPatchStatement(variableElement, commaOrEmpty)
    }

    override fun getToPatchedFunctionBuilder(
            originalTypeParameter: ParameterSpec
    ): FunSpec.Builder {
        val functionBuilder = super.getToPatchedFunctionBuilder(originalTypeParameter)
        addStateServiceParameter(functionBuilder)
        return functionBuilder
    }

    override fun toAltConstructorStatement(
            index: Int, variableElement: VariableElement, propertyName: String, propertyType: TypeName, commaOrEmpty: String
    ): DtoMembersStrategy.Statement? {
        return if(variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName()) {
            DtoMembersStrategy.Statement(
                    if(variableElement.isNullable())
                        "      $propertyName = original.$propertyName?.name$commaOrEmpty"
                    else  "      $propertyName = original.$propertyName.name$commaOrEmpty"
            )
        }
        else super.toAltConstructorStatement(index, variableElement, propertyName, propertyType, commaOrEmpty)
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

    override fun toPropertyTypeName(variableElement: VariableElement): TypeName {
        return if(variableElement.asType().asTypeElement().asClassName() == Party::class.java.asClassName())
            CordaX500Name::class.java.asTypeName().copy(nullable = true)
        else super.toPropertyTypeName(variableElement)
    }
    /*
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

     */
}
