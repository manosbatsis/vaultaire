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

import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerate
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateFor
import com.github.manosbatsis.vaultaire.dao.*
import com.github.manosbatsis.vaultaire.dsl.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.util.FieldWrapper
import com.github.manosbatsis.vaultaire.util.Fields
import com.github.manosbatsis.vaultaire.util.GenericFieldWrapper
import com.github.manosbatsis.vaultaire.util.NullableGenericFieldWrapper
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.corda.core.contracts.ContractState
import net.corda.core.internal.mapNotNull
import net.corda.core.internal.packageName
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.StatePersistable
import org.jetbrains.annotations.NotNull
import java.io.File
import java.util.stream.Collectors
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter.constructorsIn
import javax.lang.model.util.ElementFilter.fieldsIn
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE

/**
 * Kapt processor for the `@VaultaireGenerate` annotation.
 * Constructs a VaultaireGenerate for the annotated class.
 */
@SupportedAnnotationTypes(
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerate",
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateFor")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
        VaultaireAnnotationProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME,
        VaultaireAnnotationProcessor.KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME)
class VaultaireAnnotationProcessor : AbstractProcessor() {

    companion object {
        const val ANN_ATTR_CONTRACT_STATE = "contractStateType"
        const val ANN_ATTR_PERSISTENT_STATE = "persistentStateType"
        
        const val BLOCK_FUN_NAME = "block"
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        const val KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME = "kapt.kotlin.vaultaire.generated"
        val TYPE_PARAMETER_STAR = WildcardTypeName.producerOf(Any::class.asTypeName().copy(nullable = true))
        val CONTRACT_STATE_CLASSNAME = ClassName(ContractState::class.packageName, ContractState::class.simpleName!!)
        val STATE_PERSISTABLE_CLASSNAME = ClassName(StatePersistable::class.packageName, StatePersistable::class.simpleName!!)
        val FIELDS_CLASSNAME = ClassName(Fields::class.packageName, Fields::class.simpleName!!)

    }


    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val annotatedSourceElements = roundEnv.getElementsAnnotatedWith(VaultaireGenerate::class.java)
        val annotatedForElements = roundEnv.getElementsAnnotatedWith(VaultaireGenerateFor::class.java)
        if (annotatedSourceElements.isEmpty() && annotatedForElements.isEmpty()) {
            processingEnv.noteMessage { "No classes annotated with Vaultaire annotations in this round ($roundEnv)" }
            return false
        }

        val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME]
                ?: processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?: run {
            processingEnv.errorMessage { "Can't find the target directory for generated Kotlin files." }
            return false
        }

        val sourceRootFile = File(generatedSourcesRoot)
        sourceRootFile.mkdir()

        // Process own targets
        annotatedSourceElements.forEach { annotatedElement ->
            when (annotatedElement.kind) {
                ElementKind.CLASS -> writeBuilderForAnnotatedPersistentStateClass(annotatedElement as TypeElement, sourceRootFile)
                ElementKind.CONSTRUCTOR -> writeBuilderForAnnotatedPersistentStateConstructor(annotatedElement as ExecutableElement, sourceRootFile)
                else -> annotatedElement.errorMessage { "Invalid element type, expected a class or constructor" }
            }
        }
        // Process targets for dependencies
        annotatedForElements.forEach { annotated -> writeBuilderForDependency(annotated, sourceRootFile) }

        return false
    }

    /** Handle an annotated [PersistentState] class source */
    fun writeBuilderForAnnotatedPersistentStateClass(classElement: TypeElement, sourceRootFile: File) {
        // TODO: use declared/accessible fields instead of constructor params?
        writeBuilderForAnnotatedPersistentState(classElement, classElement.accessibleConstructorParameterFields(), sourceRootFile)
    }

    /** Handle an annotated [PersistentState] constructor source */
    fun writeBuilderForAnnotatedPersistentStateConstructor(constructor: ExecutableElement, sourceRootFile: File) {
        writeBuilderForAnnotatedPersistentState(constructor.enclosingElement as TypeElement, constructor.parameters, sourceRootFile)
    }

    /** Handle an annotated [PersistentState] source */
    fun writeBuilderForAnnotatedPersistentState(persistentStateTypeElement: TypeElement, fields: List<VariableElement>, sourceRoot: File) {
        val annotation = persistentStateTypeElement.getAnnotationMirror(VaultaireGenerate::class.java)
        val contractStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_CONTRACT_STATE)
        val contractStateTypeElement: Element = processingEnv.typeUtils.asElement(contractStateTypeAnnotationValue.value as TypeMirror)
        writeBuilder(persistentStateTypeElement, contractStateTypeElement, fields, sourceRoot)
    }

    /** Invokes [writeBuilderForAnnotatedPersistentState] to create a builder for the given [persistentStateTypeElement]. */
    fun writeBuilderForDependency(annotatedElement: Element, sourceRootFile: File) {
        val annotation = annotatedElement.getAnnotationMirror(VaultaireGenerateFor::class.java)
        val persistentStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_PERSISTENT_STATE)
        val persistentStateTypeElement: TypeElement = processingEnv.typeUtils
                .asElement(persistentStateTypeAnnotationValue.value as TypeMirror).asType().asTypeElement()
        val contractStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_CONTRACT_STATE)
        val contractStateTypeElement: Element = processingEnv.typeUtils.asElement(contractStateTypeAnnotationValue.value as TypeMirror)
        val allMembers = processingEnv.elementUtils.getAllMembers(persistentStateTypeElement)
        val fields = fieldsIn(allMembers)
        writeBuilder(persistentStateTypeElement, contractStateTypeElement, fields, sourceRootFile)
    }

    fun Class<*>.getParentPackageName(): String = this.canonicalName.getParentPackageName().getParentPackageName()

    fun String.getParentPackageName(): String{
        var name = this
        if(name.contains(".")) name = name.substring(0, name.lastIndexOf("."))
        return name
    }

    /** Write a builder and services for the given [PersistentState] and [ContractState] . */
    fun writeBuilder(persistentStateTypeElement: TypeElement, contractStateTypeElement: Element, fields: List<VariableElement>, sourceRoot: File) {
        val generatedPackageName = contractStateTypeElement.asType()
                .asTypeElement().asKotlinClassName().topLevelClassName().packageName.getParentPackageName() + ".generated"
        val annotatedSimpleName = persistentStateTypeElement.simpleName.toString()
        val generatedConditionsSimpleName = "${annotatedSimpleName}Conditions"
        val generatedConditionsClassName = ClassName(generatedPackageName, generatedConditionsSimpleName)
        val generatedFieldsSimpleName = "${annotatedSimpleName}Fields"
        val generatedFieldsClassName = ClassName(generatedPackageName, generatedFieldsSimpleName)
        val generatedStateServiceSimpleName = "${contractStateTypeElement.simpleName}Service"

        processingEnv.noteMessage { "Writing $generatedPackageName.$generatedConditionsSimpleName" }

        // The fields interface and object specs for the annotated element being processed
        val fieldsSpec = buildFieldsObjectSpec(generatedFieldsSimpleName, persistentStateTypeElement, fields)

        // The DSL, i.e. Conditions Class spec for the annotated element being processed
        val conditionsSpec = buildVaultQueryCriteriaConditionsBuilder(
                generatedConditionsSimpleName, persistentStateTypeElement, generatedFieldsClassName, contractStateTypeElement)

        // The state service
        val stateServiceSpecBuilder = buildStateServiceSpecBuilder(
                generatedStateServiceSimpleName, contractStateTypeElement, persistentStateTypeElement, generatedFieldsClassName)


        // Generate the Kotlin file
        FileSpec.builder(generatedPackageName, "${contractStateTypeElement.simpleName}VaultaireGenerated")
                .addComment("-------------------- DO NOT EDIT -------------------\n")
                .addComment(" This file is automatically generated by Vaultaire,\n")
                .addComment(" see https://manosbatsis.github.io/vaultaire\n")
                .addComment("----------------------------------------------------")
                .addType(fieldsSpec.build())
                .addType(conditionsSpec.build())
                .addType(stateServiceSpecBuilder.build())
                .addFunction(buildDslFunSpec(generatedConditionsClassName, persistentStateTypeElement, contractStateTypeElement))
                .build()
                .writeTo(sourceRoot)
    }

    /** Create a VaultQueryCriteriaConditions subclass builder */
    fun buildVaultQueryCriteriaConditionsBuilder(generatedConditionsSimpleName: String, annotatedElement: TypeElement, generatedFieldsClassName: ClassName, contractStateTypeElement: Element): TypeSpec.Builder {
        val conditionsSpec = TypeSpec.classBuilder(generatedConditionsSimpleName)
                .superclass(VaultQueryCriteriaCondition::class.asClassName()
                        .parameterizedBy(annotatedElement.asKotlinTypeName(), generatedFieldsClassName))
        conditionsSpec.addKdoc("Generated helper for creating [%T] query conditions/criteria", contractStateTypeElement)
        // Specify the contractStateType property
        conditionsSpec.addProperty(buildContractStateTypePropertySpec(contractStateTypeElement))
        // Specify the statePersistableType property
        conditionsSpec.addProperty(buildStatePersistableTypePropertySpec(annotatedElement))
        // Specify the fields property
        conditionsSpec.addProperty(buildFieldsPropertySpec(annotatedElement, generatedFieldsClassName))
        return conditionsSpec
    }

    /** Create a StateService subclass builder */
    fun buildStateServiceSpecBuilder(generatedStateServiceSimpleName: String, contractStateTypeElement: Element, annotatedElement: TypeElement, generatedFieldsClassName: ClassName): TypeSpec.Builder {
        val stateServiceSpecBuilder = TypeSpec.classBuilder(generatedStateServiceSimpleName)
                .addKdoc("A [%T]-specific [%T]", contractStateTypeElement, ExtendedStateService::class)
                .addModifiers(KModifier.OPEN)
                .superclass(ExtendedStateService::class.asClassName()
                        .parameterizedBy(
                                contractStateTypeElement.asKotlinTypeName(),
                                annotatedElement.asKotlinTypeName(),
                                generatedFieldsClassName))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("delegate", StateServiceDelegate::class.asClassName()
                                .parameterizedBy(contractStateTypeElement.asKotlinTypeName()))
                        .build())
                .addSuperclassConstructorParameter("delegate")
                .addProperty(buildFieldsPropertySpec(annotatedElement, generatedFieldsClassName))
                .addProperty(buildStatePersistableTypePropertySpec(annotatedElement))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("rpcOps", CordaRPCOps::class)
                        .addParameter(ParameterSpec.builder("defaults", StateServiceDefaults::class)
                                .defaultValue("%T()", StateServiceDefaults::class.java)
                                .build())
                        .callThisConstructor(CodeBlock.builder()
                                .add("%T(%N, %T::class.java, %N)", StateServiceRpcDelegate::class, "rpcOps",
                                        contractStateTypeElement, "defaults").build()
                        ).build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("serviceHub", ServiceHub::class)
                        .addParameter(ParameterSpec.builder("defaults", StateServiceDefaults::class)
                                .defaultValue("%T()", StateServiceDefaults::class.java)
                                .build())
                        .callThisConstructor(CodeBlock.builder()
                                .add("%T(%N, %T::class.java, %N)", StateServiceHubDelegate::class, "serviceHub",
                                        contractStateTypeElement, "defaults").build()
                        ).build())
        return stateServiceSpecBuilder
    }

    /** Create the fields object spec for the annotated element being processed */
    fun buildFieldsObjectSpec(generatedFieldsSimpleName: String, annotatedElement: TypeElement, fields: List<VariableElement>): TypeSpec.Builder {
        val fieldsSpec = TypeSpec.objectBuilder(generatedFieldsSimpleName)
                .addSuperinterface(FIELDS_CLASSNAME.parameterizedBy(annotatedElement.asKotlinTypeName()))
                .addKdoc("Provides easy access to fields of [%T]", annotatedElement)
        // Note fields by name
        val fieldsByNameBuilder = CodeBlock.builder().addStatement("mapOf(")

        // Add a property per KProperty of the annotated StatePersistable being processed
        fields.forEachIndexed { index, field ->
            val fieldProperty = buildPersistentStateFieldWrapperPropertySpec(field, annotatedElement)
            fieldsSpec.addProperty(fieldProperty)
            fieldsByNameBuilder.addStatement("\t%S to ${fieldProperty.name} ${if (index + 1 < fields.size) "," else ""}", fieldProperty.name)
        }
        fieldsByNameBuilder.addStatement(")")

        fieldsSpec.addProperty(PropertySpec.builder(
                "fieldsByName",
                Map::class.asClassName().parameterizedBy(
                        String::class.asTypeName(),
                        FieldWrapper::class.asTypeName().parameterizedBy(
                                annotatedElement.asKotlinTypeName())),
                KModifier.PUBLIC, KModifier.OVERRIDE)
                .initializer(
                        fieldsByNameBuilder.build()
                ).build())

        return fieldsSpec
    }

    /** Create property that wraps a pesistent state field */
    fun buildPersistentStateFieldWrapperPropertySpec(field: VariableElement, annotatedElement: TypeElement): PropertySpec {
        val fieldWrapperClass = if (field.asKotlinTypeName().isNullable) NullableGenericFieldWrapper::class else GenericFieldWrapper::class
        val fieldType = fieldWrapperClass.asClassName().parameterizedBy(
                field.enclosingElement.asKotlinTypeName(),
                field.asKotlinTypeName())

        processingEnv.noteMessage { "Adding field: $field" }
        return PropertySpec.builder(field.simpleName.toString(), fieldType, KModifier.PUBLIC)
                .initializer("%T(${annotatedElement.qualifiedName}::${field.simpleName})", fieldWrapperClass)
                .addKdoc("Wraps [%T.${field.simpleName}]", annotatedElement)
                .build()
    }

    /** Create an implementation of the abstract VaultQueryCriteriaCondition#contractStateType property */
    fun buildContractStateTypePropertySpec(contractStateTypeElement: Element): PropertySpec =
            PropertySpec.builder(
                    ANN_ATTR_CONTRACT_STATE,
                    VaultQueryCriteriaCondition<*, *>::contractStateType.returnType.asTypeName(),
                    KModifier.PUBLIC, KModifier.OVERRIDE)
                    .initializer("%T::class.java", contractStateTypeElement)
                    .addKdoc("The [%T] to create query criteria for, i.e. [%T]",
                            CONTRACT_STATE_CLASSNAME,
                            contractStateTypeElement)
                    .build()


    /** Create an implementation of the abstract VaultQueryCriteriaCondition#contractStateType property */
    fun buildStatePersistableTypePropertySpec(annotatedElement: Element): PropertySpec =
            PropertySpec.builder(
                    "statePersistableType",
                    Class::class.asClassName().parameterizedBy(annotatedElement.asKotlinTypeName()),
                    KModifier.PUBLIC, KModifier.OVERRIDE)
                    .initializer("%T::class.java", annotatedElement)
                    .addKdoc("The [%T] to create query criteria for, i.e. [%T]",
                            STATE_PERSISTABLE_CLASSNAME,
                            annotatedElement)
                    .build()

    /** Create an implementation of the abstract VaultQueryCriteriaCondition#contractStateType property */
    fun buildFieldsPropertySpec(annotatedElement: Element, generatedFieldsClassName: ClassName): PropertySpec =
            PropertySpec.builder(
                    "fields",
                    generatedFieldsClassName,
                    KModifier.PUBLIC, KModifier.OVERRIDE)
                    .initializer("%T", generatedFieldsClassName)
                    .addKdoc("Provides easy access to fields of [%T]", annotatedElement)
                    .build()

    /** Create the DSL entry point for the given [annotatedElement] */
    fun buildDslFunSpec(
            generatedClassName: ClassName, annotatedElement: TypeElement, contractStateTypeElement: Element
    ): FunSpec {
        val extensionFunParams = ParameterSpec.builder(BLOCK_FUN_NAME, LambdaTypeName.get(
                receiver = generatedClassName,
                returnType = Unit::class.asTypeName())).build()

        val extFunName = annotatedElement.getAnnotation(VaultaireGenerate::class.java)
                .getDslNameOrDefault(contractStateTypeElement.simpleName.toString().decapitalize() + "Query")
        return FunSpec.builder(extFunName)
                .addParameter(extensionFunParams)
                .returns(generatedClassName)
                .addStatement("return ${generatedClassName.simpleName}().apply($BLOCK_FUN_NAME)")
                .addKdoc("DSL entry point function for [%T]", generatedClassName)
                .build()
    }

    /** Returns all fields in this type that also appear as a constructor parameter. */
    fun TypeElement.accessibleConstructorParameterFields(): List<VariableElement> {
        val allMembers = processingEnv.elementUtils.getAllMembers(this)
        val fields = fieldsIn(allMembers)
        val constructors = constructorsIn(allMembers)
        val constructorParamNames = constructors
                .flatMap { it.parameters }
                .filterNotNull()
                .filterNot {
                    it.modifiers.contains(Modifier.PRIVATE)
                            || it.modifiers.contains(Modifier.PROTECTED)
                }
                .mapNotNull { it.simpleName.toString() }
                .toSet()
        return fields.filter { constructorParamNames.contains(it.simpleName.toString()) }
    }

    /**
     * Converts this element to a [TypeName], ensuring that java types such as [java.lang.String] are converted to their Kotlin equivalent,
     * also converting the TypeName according to any [CordaoNullableType] and [CordaoMutable] annotations.
     */
    fun Element.asKotlinTypeName(): TypeName {
        var typeName = asType().asKotlinTypeName()
        return typeName
    }

    /**
     * Converts this element to a [TypeName], ensuring that java types such as [java.lang.String] are converted to their Kotlin equivalent,
     * also converting the TypeName according to any [CordaoNullableType] and [CordaoMutable] annotations.
     */
    fun VariableElement.asKotlinTypeName(): TypeName {
        var typeName = asType().asKotlinTypeName()
        return if(this.isNullable()) typeName.copy(nullable = true) else typeName
    }

    /** Converts this TypeMirror to a [TypeName], ensuring that java types such as [java.lang.String] are converted to their Kotlin equivalent. */
    fun TypeMirror.asKotlinTypeName(): TypeName {
        return when (this) {
            is PrimitiveType -> processingEnv.typeUtils.boxedClass(this as PrimitiveType?).asKotlinClassName()
            is ArrayType -> {
                val arrayClass = ClassName("kotlin", "Array")
                return arrayClass.parameterizedBy(this.componentType.asKotlinTypeName())
            }
            is DeclaredType -> {
                val typeName = this.asTypeElement().asKotlinClassName()
                if (!this.typeArguments.isEmpty()) {
                    val kotlinTypeArguments = typeArguments.stream()
                            .mapNotNull { it.asKotlinTypeName() }
                            .collect(Collectors.toList())
                            .toTypedArray()
                    return typeName.parameterizedBy(*kotlinTypeArguments)
                }
                return typeName
            }
            else -> this.asTypeName()
        }
    }

    /** Converts this element to a [ClassName], ensuring that java types such as [java.lang.String] are converted to their Kotlin equivalent. */
    fun TypeElement.asKotlinClassName(): ClassName {
        val className = asClassName()
        return try {
            // ensure that java.lang.* and java.util.* etc classes are converted to their kotlin equivalents
            Class.forName(className.canonicalName).kotlin.asClassName()
        } catch (e: ClassNotFoundException) {
            // probably part of the same source tree as the annotated class
            className
        }
    }

    /** Returns the [TypeElement] represented by this [TypeMirror]. */
    fun TypeMirror.asTypeElement(): TypeElement = processingEnv.typeUtils.asElement(this) as TypeElement

    /** Returns true as long as this [Element] is not a [PrimitiveType] and does not have the [NotNull] core. */
    fun Element.isNullable(): Boolean {
        if (this.asType() is PrimitiveType) {
            return false
        }
        return !hasAnnotation(NotNull::class.java)
    }

    /**
     * Returns true if this element has the specified [annotation], or if the parent class has a matching constructor parameter with the core.
     * (This is necessary because builder annotations can be applied to both fields and constructor parameters - and constructor parameters take precedence.
     * Rather than require clients to specify, for instance, `@field:CordaoNullableType`, this method also checks for annotations of constructor parameters
     * when this element is a field).
     */
    fun Element.hasAnnotation(annotation: Class<*>): Boolean {
        return hasAnnotationDirectly(annotation) || hasAnnotationViaConstructorParameter(annotation)
    }

    /** Return true if this element has the specified [annotation]. */
    fun Element.hasAnnotationDirectly(annotation: Class<*>): Boolean {
        return this.annotationMirrors
                .mapNotNull { it.annotationType.toString() }
                .toSet()
                .contains(annotation.name)
    }

    /** Return true if there is a constructor parameter with the same name as this element that has the specified [annotation]. */
    fun Element.hasAnnotationViaConstructorParameter(annotation: Class<*>): Boolean {
        val parameterAnnotations = getConstructorParameter()?.annotationMirrors ?: listOf()
        return parameterAnnotations
                .mapNotNull { it.annotationType.toString() }
                .toSet()
                .contains(annotation.name)
    }

    /** Returns the first constructor parameter with the same name as this element, if any such exists. */
    fun Element.getConstructorParameter(): VariableElement? {
        val enclosingElement = this.enclosingElement
        return if (enclosingElement is TypeElement) {
            val allMembers = processingEnv.elementUtils.getAllMembers(enclosingElement)
            constructorsIn(allMembers)
                    .flatMap { it.parameters }
                    .filterNotNull()
                    .firstOrNull { it.simpleName == this.simpleName }
        } else {
            null
        }
    }

    fun Element.getAnnotationMirror(annotationClass: Class<out Annotation>): AnnotationMirror =
            findAnnotationMirror(annotationClass) ?: throw IllegalStateException("Annotation value not found for class ${annotationClass.name}")

    fun Element.findAnnotationMirror(annotationClass: Class<out Annotation>): AnnotationMirror? {
        val annotationClassName = annotationClass.name
        return this.annotationMirrors
                .filter { m -> m.annotationType.toString().equals(annotationClassName) }
                .firstOrNull()
    }

    fun AnnotationMirror.getAnnotationValue(name: String): AnnotationValue =
            findAnnotationValue(name) ?: throw IllegalStateException("Annotation value not found for string '$name'")

    fun AnnotationMirror.findAnnotationValue(name: String): AnnotationValue? {
        val elementValues = processingEnv.elementUtils.getElementValuesWithDefaults(this)

        return elementValues.keys
                .filter { k -> k.simpleName.toString() == name }
                .mapNotNull { k -> elementValues[k] }
                .firstOrNull()
    }

    /** Prints an error message using this element as a position hint. */
    fun Element.errorMessage(message: () -> String) {
        processingEnv.messager.printMessage(ERROR, message(), this)
    }
    fun ProcessingEnvironment.errorMessage(message: () -> String) {
        this.messager.printMessage(ERROR, message())
    }

    fun ProcessingEnvironment.noteMessage(message: () -> String) {
        this.messager.printMessage(NOTE, message())
    }

    fun<T: Any> T.accessField(fieldName: String): Any? {
        return this.javaClass.getDeclaredField(fieldName).let { field ->
            field?.isAccessible = true
            return@let field?.get(this)
        }
    }

}
