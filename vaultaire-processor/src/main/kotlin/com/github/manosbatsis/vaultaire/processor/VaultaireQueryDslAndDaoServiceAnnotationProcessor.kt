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

import com.github.manosbatsis.vaultaire.annotation.ExtendedStateServiceBean
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerate
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateForDependency
import com.github.manosbatsis.vaultaire.dao.ExtendedStateService
import com.github.manosbatsis.vaultaire.dao.StateServiceDefaults
import com.github.manosbatsis.vaultaire.dao.StateServiceDelegate
import com.github.manosbatsis.vaultaire.dao.StateServiceHubDelegate
import com.github.manosbatsis.vaultaire.dao.StateServiceRpcConnectionDelegate
import com.github.manosbatsis.vaultaire.dao.StateServiceRpcDelegate
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME
import com.github.manosbatsis.vaultaire.registry.Registry
import com.github.manosbatsis.vaultaire.rpc.NodeRpcConnection
import com.github.manosbatsis.vaultaire.util.FieldWrapper
import com.github.manosbatsis.vaultaire.util.Fields
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import net.corda.core.contracts.ContractState
import net.corda.core.internal.packageName
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub
import net.corda.core.schemas.PersistentState
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/**
 * Kapt processor for the `@VaultaireGenerate` annotation.
 * Constructs a VaultaireGenerate for the annotated class.
 */
@SupportedAnnotationTypes(
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerate",
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateForDependency")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
        KAPT_KOTLIN_GENERATED_OPTION_NAME,
        KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME)
class VaultaireQueryDslAndDaoServiceAnnotationProcessor : BaseStateInfoAnnotationProcessor() {

    companion object {
        val FIELDS_CLASSNAME = ClassName(Fields::class.packageName, Fields::class.simpleName!!)

    }

    override val sourcesAnnotation = VaultaireGenerate::class.java
    override val dependenciesAnnotation = VaultaireGenerateForDependency::class.java


    /** Write a builder and services for the given [PersistentState] and [ContractState] . */
    override fun process(stateInfo: StateInfo) {
        val generatedConditionsClassName = ClassName(stateInfo.generatedPackageName, "${stateInfo.persistentStateSimpleName}Conditions")
        var persistentStateFieldsClassName = ClassName(stateInfo.generatedPackageName, "${stateInfo.persistentStateSimpleName}Fields")
        var contractStateFieldsClassName = ClassName(stateInfo.generatedPackageName, "${stateInfo.contractStateSimpleName}Fields")
        processingEnv.noteMessage { "Writing $generatedConditionsClassName" }

        // The fields interface and object specs for the contract/persistent state pair being processed
        val persistentStateFieldsSpec = buildFieldsObjectSpec(stateInfo.persistentStateTypeElement!!,
                stateInfo.persistentStateFields!!, persistentStateFieldsClassName)
        val contractStateFieldsSpec = buildFieldsObjectSpec(stateInfo.contractStateTypeElement.asType().asTypeElement(),
                stateInfo.contractStateFields, contractStateFieldsClassName)

        // The DSL, i.e. Conditions Class spec for the annotated element being processed
        val conditionsSpec = buildVaultQueryCriteriaConditionsBuilder(stateInfo, generatedConditionsClassName, persistentStateFieldsClassName)

        // The state service
        val stateServiceSpecBuilder = buildStateServiceSpecBuilder(stateInfo, generatedConditionsClassName, persistentStateFieldsClassName)


        // Generate the Kotlin file
        getFileSpecBuilder(stateInfo.generatedPackageName, "${stateInfo.contractStateTypeElement.simpleName}VaultaireGenerated")
                .addType(persistentStateFieldsSpec.build())
                .addType(contractStateFieldsSpec.build())
                .addType(conditionsSpec.build())
                .addType(stateServiceSpecBuilder.build())
                .addFunction(buildTopLevelDslFunSpec(generatedConditionsClassName, stateInfo.persistentStateTypeElement, stateInfo.contractStateTypeElement))
                .build()
                .writeTo(sourceRootFile)
    }

    /** Create the DSL entry point for the given [annotatedElement] */
    fun buildTopLevelDslFunSpec(
            generatedConditionsClassName: ClassName, annotatedElement: TypeElement, contractStateTypeElement: Element
    ): FunSpec {

        var extFunName: String = annotatedElement.findAnnotationMirror(VaultaireGenerate::class.java)?.findAnnotationValue("name").toString()
        if(extFunName.isNotBlank()) extFunName =
                annotatedElement.findAnnotationMirror(VaultaireGenerateForDependency::class.java)?.findAnnotationValue("name").toString()
        if(extFunName.isNotBlank()) extFunName = contractStateTypeElement.simpleName.toString().decapitalize() + "Query"

        return buildDslFunSpec(extFunName, generatedConditionsClassName)
    }

    /** Create the DSL entry point for the given [annotatedElement] */
    fun buildDslFunSpec(
            funcName: String, generatedConditionsClassName: ClassName, vararg modifiers: KModifier
    ): FunSpec {
        val extensionFunParams = ParameterSpec.builder(BaseAnnotationProcessor.BLOCK_FUN_NAME, LambdaTypeName.get(
                receiver = generatedConditionsClassName,
                returnType = Unit::class.asTypeName())).build()

        val funSpec =  FunSpec.builder(funcName)
                .addParameter(extensionFunParams)
                .returns(generatedConditionsClassName)
                .addStatement("return ${generatedConditionsClassName.simpleName}().apply(${BaseAnnotationProcessor.BLOCK_FUN_NAME})")
                .addKdoc("DSL entry point function for [%T]", generatedConditionsClassName)
        if(modifiers.isNotEmpty())funSpec.addModifiers(*modifiers)
        return funSpec.build()
    }

    /** Create a VaultQueryCriteriaConditions subclass builder */
    fun buildVaultQueryCriteriaConditionsBuilder(stateInfo: StateInfo, generatedConditionsName: ClassName, generatedFieldsClassName: ClassName): TypeSpec.Builder {
        val conditionsSpec = TypeSpec.classBuilder(generatedConditionsName)
                .superclass(VaultQueryCriteriaCondition::class.asClassName()
                        .parameterizedBy(stateInfo.persistentStateTypeElement!!.asKotlinTypeName(), generatedFieldsClassName))
        conditionsSpec.addKdoc("Generated helper for creating [%T] query conditions/criteria", stateInfo.contractStateTypeElement)
        // Register
        conditionsSpec.addType(TypeSpec.companionObjectBuilder().addInitializerBlock(
                        CodeBlock.of("%T.registerQueryDsl(%T::class, %M::class)",
                                Registry::class, stateInfo.persistentStateTypeElement,  MemberName("", generatedConditionsName.simpleName))
                ).build())
        // Specify the contractStateType property
        conditionsSpec.addProperty(buildContractStateTypePropertySpec(stateInfo.contractStateTypeElement))
        // Specify the statePersistableType property
        conditionsSpec.addProperty(buildStatePersistableTypePropertySpec(stateInfo.persistentStateTypeElement))
        // Specify the fields property
        conditionsSpec.addProperty(buildFieldsPropertySpec(stateInfo.persistentStateTypeElement, generatedFieldsClassName))
        return conditionsSpec
    }

    /** Create a StateService subclass builder */
    fun buildStateServiceSpecBuilder(stateInfo: StateInfo, conditionsClassName: ClassName, generatedFieldsClassName: ClassName): TypeSpec.Builder {
        val generatedStateServiceSimpleName = "${stateInfo.contractStateSimpleName}Service"
        //val conditionsLambda = LambdaTypeName.get(returnType = processingEnv.typeUtils.a.getTypeElement(conditionsSimpleName).asKotlinTypeName())
        val stateServiceSpecBuilder = TypeSpec.classBuilder(generatedStateServiceSimpleName)
                .addKdoc("A [%T]-specific [%T]", stateInfo.contractStateTypeElement, ExtendedStateService::class)
                .addModifiers(KModifier.OPEN)
                .addAnnotation(ExtendedStateServiceBean::class)
                .superclass(ExtendedStateService::class.asClassName()
                        .parameterizedBy(
                                stateInfo.contractStateTypeElement.asKotlinTypeName(),
                                stateInfo.persistentStateTypeElement!!.asKotlinTypeName(),
                                generatedFieldsClassName,
                                conditionsClassName))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("delegate", StateServiceDelegate::class.asClassName()
                                .parameterizedBy(stateInfo.contractStateTypeElement.asKotlinTypeName()))
                        .build())
                .addSuperclassConstructorParameter("delegate")
                .addType(TypeSpec.companionObjectBuilder().addInitializerBlock(
                                CodeBlock.of("%T.registerService(%T::class, %M::class)",
                                        Registry::class, stateInfo.contractStateTypeElement,  MemberName("", generatedStateServiceSimpleName))
                        ).build())
                .addProperty(buildFieldsPropertySpec(stateInfo.persistentStateTypeElement, generatedFieldsClassName))
                .addProperty(buildStatePersistableTypePropertySpec(stateInfo.persistentStateTypeElement))
                .addInitializerBlock(CodeBlock.of("criteriaConditionsType =  %N::class.java", "${stateInfo.persistentStateSimpleName}Conditions"))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("rpcOps", CordaRPCOps::class)
                        .addParameter(ParameterSpec.builder("defaults", StateServiceDefaults::class)
                                .defaultValue("%T()", StateServiceDefaults::class.java)
                                .build())
                        .callThisConstructor(CodeBlock.builder()
                                .add("%T(%N, %T::class.java, %N)",
                                        StateServiceRpcDelegate::class, "rpcOps",
                                        stateInfo.contractStateTypeElement, "defaults").build()
                        ).build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("nodeRpcConnection", NodeRpcConnection::class)
                        .addParameter(ParameterSpec.builder("defaults", StateServiceDefaults::class)
                                .defaultValue("%T()", StateServiceDefaults::class.java)
                                .build())
                        .callThisConstructor(CodeBlock.builder()
                                .add("%T(%N, %T::class.java, %N)",
                                        StateServiceRpcConnectionDelegate::class, "nodeRpcConnection",
                                        stateInfo.contractStateTypeElement, "defaults").build()
                        ).build())
                //NodeRpcConnection
                .addFunction(buildDslFunSpec("buildQuery", conditionsClassName, KModifier.OVERRIDE))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("serviceHub", ServiceHub::class)
                        .addParameter(ParameterSpec.builder("defaults", StateServiceDefaults::class)
                                .defaultValue("%T()", StateServiceDefaults::class.java)
                                .build())
                        .callThisConstructor(CodeBlock.builder()
                                .add("%T(%N, %T::class.java, %N)", StateServiceHubDelegate::class, "serviceHub",
                                        stateInfo.contractStateTypeElement, "defaults").build()
                        ).build())


        return stateServiceSpecBuilder
    }

    /** Create the fields object spec for the annotated element being processed */
    fun buildFieldsObjectSpec(typeElement: TypeElement, fields: List<VariableElement>, fieldsClassName: ClassName): TypeSpec.Builder {
        val fieldsSpec = TypeSpec.objectBuilder(fieldsClassName)
                .addSuperinterface(FIELDS_CLASSNAME.parameterizedBy(
                    if(typeElement.typeParameters.isEmpty()) typeElement.asKotlinTypeName()
                    else typeElement.asKotlinClassName().parameterizedBy(
                            typeElement.typeParameters.map {
                                it.bounds.single().asKotlinTypeName()
                            })
                ))
                .addKdoc("Provides easy access to fields of [%T]", typeElement)
        if(typeElement.typeParameters.isNotEmpty()) {
            processingEnv.noteMessage { "typeElement.typeParameters.first: ${typeElement.typeParameters.first()}" }
            processingEnv.noteMessage { "typeElement.typeParameters.first().bounds: ${typeElement.typeParameters.first().bounds}" }
        }
        // Note fields by name
        val fieldsByNameBuilder = CodeBlock.builder().addStatement("mapOf(")

        // Add a property per KProperty of the annotated StatePersistable being processed
        fields.forEachIndexed { index, field ->
            val fieldProperty = buildPersistentStateFieldWrapperPropertySpec(field, typeElement)
            fieldsSpec.addProperty(fieldProperty)
            fieldsByNameBuilder.addStatement("\t%S to ${fieldProperty.name} ${if (index + 1 < fields.size) "," else ""}", fieldProperty.name)
        }
        fieldsByNameBuilder.addStatement(")")

        fieldsSpec.addProperty(PropertySpec.builder(
                "fieldsByName",
                Map::class.asClassName().parameterizedBy(
                        String::class.asTypeName(),
                        FieldWrapper::class.asTypeName().parameterizedBy(
                                typeElement.asKotlinTypeName())),
                KModifier.PUBLIC, KModifier.OVERRIDE)
                .initializer(
                        fieldsByNameBuilder.build()
                ).build())

        return fieldsSpec
    }


}
