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

import com.github.manosbatsis.kotlin.utils.ProcessingEnvironmentAware
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.accounts.service.VaultaireBaseTypesConfigAnnotationProcessorPlugin
import com.github.manosbatsis.vaultaire.processor.plugin.VaultaireDefaultBaseTypesConfigAnnotationProcessorPlugin
import com.github.manosbatsis.vaultaire.processor.plugins.AnnotationProcessorPluginService
import com.github.manosbatsis.vaultaire.util.GenericFieldWrapper
import com.github.manosbatsis.vaultaire.util.NullableGenericFieldWrapper
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.corda.core.contracts.ContractState
import net.corda.core.schemas.StatePersistable
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/**
 * Baee processor implementation.
 */
abstract class BaseAnnotationProcessor : AbstractProcessor(), ProcessingEnvironmentAware {

    companion object {
        const val ANN_ATTR_CONTRACT_STATE = "contractStateType"
        const val ANN_ATTR_PERSISTENT_STATE = "persistentStateType"
        const val ANN_ATTR_COPY_ANNOTATION_PACKAGES = "copyAnnotationPackages"

        const val BLOCK_FUN_NAME = "block"
        const val KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME = "kapt.kotlin.vaultaire.generated"
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        val TYPE_PARAMETER_STAR = WildcardTypeName.producerOf(Any::class.asTypeName().copy(nullable = true))
        val CLASSNAME_CONTRACT_STATE = ContractState::class.java.asClassName()
        val CLASSNAME_STATE_PERSISTABLE = StatePersistable::class.java.asClassName()


    }

    val baseClassesConfig: VaultaireBaseTypesConfigAnnotationProcessorPlugin by lazy {
        AnnotationProcessorPluginService
                .forClassLoader(BaseAnnotationProcessor::class.java.classLoader)
                .forServiceType(
                        VaultaireBaseTypesConfigAnnotationProcessorPlugin::class.java,
                        VaultaireDefaultBaseTypesConfigAnnotationProcessorPlugin()
                )
    }

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


    fun getFileSpecBuilder(packageName: String, fileName: String) =
            FileSpec.builder(packageName, fileName)
                    .addComment("-------------------- DO NOT EDIT -------------------\n")
                    .addComment(" This file is automatically generated by Vaultaire,\n")
                    .addComment(" see https://manosbatsis.github.io/vaultaire\n")
                    .addComment("----------------------------------------------------")


    fun Class<*>.getParentPackageName(): String = this.canonicalName.getParentPackageName().getParentPackageName()

    fun String.getParentPackageName(): String {
        var name = this
        if (name.contains(".")) name = name.substring(0, name.lastIndexOf("."))
        return name
    }

    /** Create property that wraps a pesistent state field */
    fun buildPersistentStateFieldWrapperPropertySpec(field: VariableElement, annotatedElement: TypeElement): PropertySpec {
        val fieldWrapperClass = if (field.asKotlinTypeName().isNullable) NullableGenericFieldWrapper::class else GenericFieldWrapper::class
        val enclosingType = field.enclosingElement.asType()
        val enclosingTypeElement = enclosingType.asTypeElement()
        val enclosingParameterisedType =
                if (enclosingTypeElement.typeParameters.isNotEmpty())
                    enclosingTypeElement.asKotlinClassName()
                            .parameterizedBy(enclosingTypeElement.typeParameters
                                    .map { TYPE_PARAMETER_STAR })
                else enclosingTypeElement.asKotlinClassName()

        val fieldType = fieldWrapperClass.asClassName().parameterizedBy(
                enclosingParameterisedType,
                field.asKotlinTypeName())
        return PropertySpec.builder(field.simpleName.toString(), fieldType, KModifier.PUBLIC)
                .initializer("%T(%T::${field.simpleName})", fieldWrapperClass, enclosingParameterisedType)
                .addKdoc("Wraps [%T.${field.simpleName}]", enclosingParameterisedType)
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
                            CLASSNAME_CONTRACT_STATE,
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
                            CLASSNAME_STATE_PERSISTABLE,
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

