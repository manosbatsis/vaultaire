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

import com.github.manosbatsis.kotlinpoet.utils.ProcessingEnvironmentAware
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.util.Fields
import com.github.manosbatsis.vaultaire.util.GenericFieldWrapper
import com.github.manosbatsis.vaultaire.util.NullableGenericFieldWrapper
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import net.corda.core.contracts.ContractState
import net.corda.core.internal.packageName
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.StatePersistable
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter.fieldsIn

/**
 * Baee processor implementation.
 */
abstract class BaseAnnotationProcessor : AbstractProcessor(), ProcessingEnvironmentAware {

    companion object {
        const val ANN_ATTR_CONTRACT_STATE = "contractStateType"
        const val ANN_ATTR_PERSISTENT_STATE = "persistentStateType"
        
        const val BLOCK_FUN_NAME = "block"
        const val KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME = "kapt.kotlin.vaultaire.generated"
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        val TYPE_PARAMETER_STAR = WildcardTypeName.producerOf(Any::class.asTypeName().copy(nullable = true))
        val CONTRACT_STATE_CLASSNAME = ClassName(ContractState::class.packageName, ContractState::class.simpleName!!)
        val STATE_PERSISTABLE_CLASSNAME = ClassName(StatePersistable::class.packageName, StatePersistable::class.simpleName!!)
        val FIELDS_CLASSNAME = ClassName(Fields::class.packageName, Fields::class.simpleName!!)

    }

    abstract val sourcesAnnotation: Class<out Annotation>
    abstract val dependenciesAnnotation: Class<out Annotation>


    val generatedSourcesRoot: String by lazy {
        processingEnv.options[KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME]
                ?: processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?: throw IllegalStateException("Can't find the target directory for generated Kotlin files.")
    }

    val sourceRootFile by lazy {
        val sourceRootFile = File(generatedSourcesRoot)
        sourceRootFile.mkdir()
        sourceRootFile
    }

    /** Implement [ProcessingEnvironment] access */
    override val processingEnvironment by lazy {
        processingEnv
    }

    abstract fun process(stateInfo: StateInfo)

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val annotatedSourceElements = roundEnv.getElementsAnnotatedWith(sourcesAnnotation)
        val annotatedForElements = roundEnv.getElementsAnnotatedWith(dependenciesAnnotation)
        if (annotatedSourceElements.isEmpty() && annotatedForElements.isEmpty()) {
            processingEnv.noteMessage { "No classes annotated with Vaultaire annotations in this round ($roundEnv)" }
            return false
        }

        // Process own targets
        annotatedSourceElements.forEach { annotatedElement ->
            when (annotatedElement.kind) {
                ElementKind.CLASS -> process(stateInfoForAnnotatedPersistentStateSourceClass(annotatedElement as TypeElement))
                ElementKind.CONSTRUCTOR -> process(stateInfoForAnnotatedPersistentStateSourceConstructor(annotatedElement as ExecutableElement))
                else -> annotatedElement.errorMessage { "Invalid element type, expected a class or constructor" }
            }
        }
        // Process targets for dependencies
        annotatedForElements.forEach { annotated -> process(stateInfoForDependency(annotated)) }

        return false
    }

    /** Construct a [StateInfo] from an annotated [PersistentState] source */
    fun stateInfoForAnnotatedPersistentStateSource(element: Element): StateInfo {
        return when (element.kind) {
            ElementKind.CLASS -> stateInfoForAnnotatedPersistentStateSourceClass(element as TypeElement)
            ElementKind.CONSTRUCTOR -> stateInfoForAnnotatedPersistentStateSourceConstructor(element as ExecutableElement)
            else -> throw IllegalArgumentException("Invalid element type, expected a class or constructor")
        }
    }

    /** Construct a [StateInfo] from an annotated [PersistentState] class source */
    fun stateInfoForAnnotatedPersistentStateSourceClass(classElement: TypeElement): StateInfo {
        // TODO: use declared/accessible fields instead of constructor params?
        return stateInfoForAnnotatedPersistentState(classElement, classElement.accessibleConstructorParameterFields())
    }

    /** Construct a [StateInfo] [PersistentState] constructor source */
    fun stateInfoForAnnotatedPersistentStateSourceConstructor(constructor: ExecutableElement): StateInfo {
        return stateInfoForAnnotatedPersistentState(constructor.enclosingElement as TypeElement, constructor.parameters)
    }


    /** Handle an annotated [PersistentState] source */
    fun stateInfoForAnnotatedPersistentState(persistentStateTypeElement: TypeElement, fields: List<VariableElement>): StateInfo {
        val annotation = persistentStateTypeElement.getAnnotationMirror(sourcesAnnotation)
        val contractStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_CONTRACT_STATE)
        val contractStateTypeElement: Element = processingEnv.typeUtils.asElement(contractStateTypeAnnotationValue.value as TypeMirror)
        return stateInfo(persistentStateTypeElement, contractStateTypeElement, fields)
    }

    /** Invokes [writeBuilderForAnnotatedPersistentState] to create a builder for the given [persistentStateTypeElement]. */
    fun stateInfoForDependency(annotatedElement: Element): StateInfo {
        val annotation = annotatedElement.getAnnotationMirror(dependenciesAnnotation)
        val persistentStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_PERSISTENT_STATE)
        val persistentStateTypeElement: TypeElement = processingEnv.typeUtils
                .asElement(persistentStateTypeAnnotationValue.value as TypeMirror).asType().asTypeElement()
        val persistentStateFields = fieldsIn(processingEnv.elementUtils.getAllMembers(persistentStateTypeElement))

        val contractStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_CONTRACT_STATE)
        val contractStateTypeElement: Element = processingEnv.typeUtils.asElement(contractStateTypeAnnotationValue.value as TypeMirror)
        return stateInfo(persistentStateTypeElement, contractStateTypeElement, persistentStateFields)
    }


    fun stateInfo(persistentStateTypeElement: TypeElement, contractStateTypeElement: Element, fields: List<VariableElement>): StateInfo {
        val contractStateFields = fieldsIn(processingEnv.elementUtils.getAllMembers(contractStateTypeElement.asType().asTypeElement()))
        return StateInfoBuilder()
                .contractStateTypeElement(contractStateTypeElement)
                .contractStateFields(contractStateFields)
                .persistentStateTypeElement(persistentStateTypeElement)
                .persistentStateFields(fields)
                .generatedPackageName(contractStateTypeElement.asType()
                        .asTypeElement().asKotlinClassName().topLevelClassName().packageName.getParentPackageName() + ".generated")
                .sourceRoot(sourceRootFile).build()
    }

    fun TypeElement.findAnnotationValue(attribute: String, default: AnnotationValue, vararg annotations: Class<out Annotation>): AnnotationValue? {
        var value: AnnotationValue? = default
        for(annotation in annotations){
            val tmp = this.findAnnotationMirror(annotation)?.findAnnotationValue(attribute)
            if(tmp != null && tmp.toString().isNotBlank()){
                value = tmp
                break
            }
        }
        return value
    }

    fun Class<*>.getParentPackageName(): String = this.canonicalName.getParentPackageName().getParentPackageName()

    fun String.getParentPackageName(): String{
        var name = this
        if(name.contains(".")) name = name.substring(0, name.lastIndexOf("."))
        return name
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
}
