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
        println("DtoViewStrategy.getFieldsToProcess(): ${fields.joinToString { it.simpleName.toString() }}")
        return fields
    }

    override fun getExtraFieldsFromMixin(): List<VariableElement> {
        val fields = originalStrategy.getExtraFieldsFromMixin()
                .includeNames(getFieldIncludes()).excludeNames(getFieldExcludes())
        println("DtoViewStrategy.getExtraFieldsFromMixin(): ${fields.joinToString { it.simpleName.toString() }}")
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
        println("DtoViewStrategy.getFieldIncludes(): ${names.joinToString()}")
        return names
    }

    override fun getFieldExcludes(): List<String> {
        val names = super.getFieldExcludes().toMutableSet().also {
            it.addAll(viewInfo.viewAnnotation.excludeNamedFields)
        }.toList()
        println("DtoViewStrategy.getFieldExcludes(): ${names.joinToString()}")
        return names
    }

    override fun getClassName(): ClassName {
        val mappedPackageName = mapPackageName(annotatedElementInfo.generatedPackageName)
        val simpleName = when {
            viewInfo.targetName != null -> "${viewInfo.targetName}${viewInfo.targetNameSuffix}"
            viewInfo.targetNameSuffix != null -> "${annotatedElementInfo.primaryTargetTypeElement.simpleName}${viewInfo.targetNameSuffix}"
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