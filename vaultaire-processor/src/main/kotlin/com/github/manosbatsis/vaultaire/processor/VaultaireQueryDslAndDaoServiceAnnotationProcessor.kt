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

import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyConnection
import com.github.manosbatsis.corda.rpc.poolboy.connection.NodeRpcConnection
import com.github.manosbatsis.kotlin.utils.kapt.plugins.AnnotationProcessorPluginService
import com.github.manosbatsis.kotlin.utils.kapt.processor.AbstractAnnotatedModelInfoProcessor
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotationProcessorBase
import com.github.manosbatsis.vaultaire.annotation.ExtendedStateServiceBean
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerate
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateForDependency
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.plugin.BaseTypesConfigAnnotationProcessorPlugin
import com.github.manosbatsis.vaultaire.registry.Registry
import com.github.manosbatsis.vaultaire.service.ServiceDefaults
import com.github.manosbatsis.vaultaire.service.SimpleServiceDefaults
import com.github.manosbatsis.vaultaire.util.FieldWrapper
import com.github.manosbatsis.vaultaire.util.Fields
import com.github.manosbatsis.vaultaire.util.GenericFieldWrapper
import com.github.manosbatsis.vaultaire.util.NullableGenericFieldWrapper
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.FunSpec.Builder
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import net.corda.core.contracts.ContractState
import net.corda.core.internal.packageName
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.schemas.StatePersistable
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
@SupportedOptions(AnnotationProcessorBase.KAPT_OPTION_NAME_KAPT_KOTLIN_GENERATED)
class VaultaireQueryDslAndDaoServiceAnnotationProcessor : AbstractAnnotatedModelInfoProcessor(
        primaryTargetRefAnnotationName = "persistentStateType",
        secondaryTargetRefAnnotationName = "contractStateType"
) {

    companion object {

        const val ANN_ATTR_CONTRACT_STATE = "contractStateType"
        const val ANN_ATTR_PERSISTENT_STATE = "persistentStateType"
        const val ANN_ATTR_COPY_ANNOTATION_PACKAGES = "copyAnnotationPackages"
        val FIELDS_CLASSNAME = ClassName(Fields::class.packageName, Fields::class.simpleName!!)

        val TYPE_PARAMETER_STAR = WildcardTypeName.producerOf(Any::class.asTypeName().copy(nullable = true))
        val CONTRACT_STATE_CLASSNAME = ClassName(ContractState::class.packageName, ContractState::class.simpleName!!)
        val STATE_PERSISTABLE_CLASSNAME = ClassName(StatePersistable::class.packageName, StatePersistable::class.simpleName!!)


    }

    override fun processElementInfos(elementInfos: List<AnnotatedElementInfo>) =
            elementInfos.forEach { processElementInfo(it) }

    //override val sourcesAnnotation = VaultaireGenerate::class.java
    //override val dependenciesAnnotation = VaultaireGenerateForDependency::class.java

    fun getBaseClassesConfigService(annotatedElementInfo: AnnotatedElementInfo) =
            AnnotationProcessorPluginService.getInstance()
                    .getPlugin(BaseTypesConfigAnnotationProcessorPlugin::class.java, annotatedElementInfo)

    fun processElementInfo(annotatedElementInfo: AnnotatedElementInfo) {
        val generatedConditionsClassName = ClassName(
                annotatedElementInfo.generatedPackageName,
                "${annotatedElementInfo.primaryTargetTypeElementSimpleName}Conditions")
        var persistentStateFieldsClassName = ClassName(
                annotatedElementInfo.generatedPackageName,
                "${annotatedElementInfo.primaryTargetTypeElementSimpleName}Fields")
        var contractStateFieldsClassName = ClassName(
                annotatedElementInfo.generatedPackageName,
                "${annotatedElementInfo.secondaryTargetTypeElementSimpleName}Fields")
        val baseTypesConfig = getBaseClassesConfigService(annotatedElementInfo)
        // The fields interface and object specs for the contract/persistent state pair being processed
        processingEnv.noteMessage { "Prepare persistentStateFieldsSpec: $persistentStateFieldsClassName" }
        val persistentStateFieldsSpec = buildFieldsObjectSpec(
                annotatedElementInfo.primaryTargetTypeElement,
                annotatedElementInfo.primaryTargetTypeElementFields,
                persistentStateFieldsClassName)
        processingEnv.noteMessage { "Prepare contractStateFieldsSpec: $contractStateFieldsClassName" }
        val contractStateFieldsSpec = buildFieldsObjectSpec(
                annotatedElementInfo.secondaryTargetTypeElement!!,
                annotatedElementInfo.secondaryTargetTypeElementFields,
                contractStateFieldsClassName)

        // The DSL, i.e. Conditions Class spec for the annotated element being processed
        val conditionsSpec = buildVaultQueryCriteriaConditionsBuilder(
                annotatedElementInfo, generatedConditionsClassName, persistentStateFieldsClassName)

        // The state CordaService delegate
        val stateCordaServiceDelegateSpec =
                buildStateCordaServiceDelegateSpecBuilder(baseTypesConfig, annotatedElementInfo).build()

        // The state service
        val stateServiceSpecBuilder = buildStateServiceSpecBuilder(
                baseTypesConfig, annotatedElementInfo, generatedConditionsClassName,
                persistentStateFieldsClassName, stateCordaServiceDelegateSpec)


        // Generate the Kotlin file
        getFileSpecBuilder(annotatedElementInfo.generatedPackageName, "${annotatedElementInfo.secondaryTargetTypeElementSimpleName}VaultaireGenerated")
                .addType(persistentStateFieldsSpec.build())
                .addType(contractStateFieldsSpec.build())
                .addType(conditionsSpec.build())
                .addType(stateCordaServiceDelegateSpec)
                .addType(stateServiceSpecBuilder.build())
                .addFunction(buildTopLevelDslFunSpec(generatedConditionsClassName,
                        annotatedElementInfo.primaryTargetTypeElement, annotatedElementInfo.secondaryTargetTypeElement!!))
                .build()
                .writeTo(sourceRootFile)
    }

    /** Create the DSL entry point for the given [annotatedElement] */
    fun buildTopLevelDslFunSpec(
            generatedConditionsClassName: ClassName, annotatedElement: TypeElement, contractStateTypeElement: Element
    ): FunSpec {

        var extFunName: String = annotatedElement.findAnnotationMirror(VaultaireGenerate::class.java)?.findAnnotationValue("name").toString()
        if (extFunName.isNotBlank()) extFunName =
                annotatedElement.findAnnotationMirror(VaultaireGenerateForDependency::class.java)?.findAnnotationValue("name").toString()
        if (extFunName.isNotBlank()) extFunName = contractStateTypeElement.simpleName.toString().decapitalize() + "Query"

        return buildDslFunSpec(extFunName, generatedConditionsClassName)
    }

    /** Create the DSL entry point for the given [annotatedElement] */
    fun buildDslFunSpec(
            funcName: String, generatedConditionsClassName: ClassName, vararg modifiers: KModifier
    ): FunSpec {
        val extensionFunParams = ParameterSpec.builder(AnnotationProcessorBase.BLOCK_FUN_NAME, LambdaTypeName.get(
                receiver = generatedConditionsClassName,
                returnType = Unit::class.asTypeName())).build()

        val funSpec = FunSpec.builder(funcName)
                .addParameter(extensionFunParams)
                .returns(generatedConditionsClassName)
                .addStatement("return ${generatedConditionsClassName.simpleName}().apply(${AnnotationProcessorBase.BLOCK_FUN_NAME})")
                .addKdoc("DSL entry point function for [%T]", generatedConditionsClassName)
        if (modifiers.isNotEmpty()) funSpec.addModifiers(*modifiers)
        return funSpec.build()
    }

    /** Create a VaultQueryCriteriaConditions subclass builder */
    fun buildVaultQueryCriteriaConditionsBuilder(annotatedElementInfo: AnnotatedElementInfo, generatedConditionsName: ClassName, generatedFieldsClassName: ClassName): TypeSpec.Builder {
        val conditionsSpec = TypeSpec.classBuilder(generatedConditionsName)
                .superclass(VaultQueryCriteriaCondition::class.asClassName()
                        .parameterizedBy(annotatedElementInfo.primaryTargetTypeElement!!.asKotlinTypeName(), generatedFieldsClassName))
        conditionsSpec.addKdoc("Generated helper for creating [%T] query conditions/criteria", annotatedElementInfo.secondaryTargetTypeElement!!)
        // Register
        conditionsSpec.addType(TypeSpec.companionObjectBuilder().addInitializerBlock(
                CodeBlock.of("%T.registerQueryDsl(%T::class, %M::class)",
                        Registry::class, annotatedElementInfo.primaryTargetTypeElement, MemberName("", generatedConditionsName.simpleName))
        ).build())
        // Specify the contractStateType property
        conditionsSpec.addProperty(buildContractStateTypePropertySpec(annotatedElementInfo.secondaryTargetTypeElement!!))
        // Specify the statePersistableType property
        conditionsSpec.addProperty(buildStatePersistableTypePropertySpec(annotatedElementInfo.primaryTargetTypeElement))
        // Specify the fields property
        conditionsSpec.addProperty(buildFieldsPropertySpec(annotatedElementInfo.primaryTargetTypeElement, generatedFieldsClassName))
        return conditionsSpec
    }

    /** Create a state CordaService delegate builder */
    fun buildStateCordaServiceDelegateSpecBuilder(
            baseClassesConfig: BaseTypesConfigAnnotationProcessorPlugin,
            annotatedElementInfo: AnnotatedElementInfo
    ): TypeSpec.Builder {
        val generatedSimpleName = "${annotatedElementInfo.secondaryTargetTypeElementSimpleName}CordaServiceDelegate"
        return TypeSpec.classBuilder(generatedSimpleName)
                .addKdoc("A [%T]-specific [%T]", annotatedElementInfo.secondaryTargetTypeElement!!, baseClassesConfig.serviceHubDelegateClassName)
                .addModifiers(KModifier.OPEN)
                .addAnnotation(CordaService::class)
                .superclass(baseClassesConfig.serviceHubDelegateClassName
                        .parameterizedBy(annotatedElementInfo.secondaryTargetTypeElement!!.asKotlinTypeName()))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("serviceHub", AppServiceHub::class.java.asClassName())
                        .build())
                .addSuperclassConstructorParameter("serviceHub")
                .addSuperclassConstructorParameter("contractStateType = %T::class.java",
                        annotatedElementInfo.secondaryTargetTypeElement!!)

    }

    /** Create a StateService subclass builder *///annotatedElementInfo: AnnotatedElementInfo
    fun buildStateServiceSpecBuilder(
            baseClassesConfig: BaseTypesConfigAnnotationProcessorPlugin,
            annotatedElementInfo: AnnotatedElementInfo, conditionsClassName: ClassName,
            generatedFieldsClassName: ClassName, stateCordaServiceDelegateSpec: TypeSpec
    ): TypeSpec.Builder {
        val generatedSimpleName = "${annotatedElementInfo.secondaryTargetTypeElementSimpleName}Service"
        val legacyRpcDelegateConstructorKdoc = CodeBlock.of("Legacy constructor without pool support")
        val legacyRpcDelegateConstructorAnnotation = AnnotationSpec
                .builder(Deprecated::class.java)
                .addMember("message = %S", "Legacy constructor without pool support, use pool boy constructor instead").build()
        //val conditionsLambda = LambdaTypeName.get(returnType = processingEnv.typeUtils.a.getTypeElement(conditionsSimpleName).asKotlinTypeName())
        val stateServiceSpecBuilder = TypeSpec.classBuilder(generatedSimpleName)
                .addKdoc("A [%T]-specific [%T]", annotatedElementInfo.secondaryTargetTypeElement!!, baseClassesConfig.stateServiceClassName)
                .addModifiers(KModifier.OPEN)
                .addAnnotation(ExtendedStateServiceBean::class)
                .superclass(baseClassesConfig.stateServiceClassName
                        .parameterizedBy(
                                annotatedElementInfo.secondaryTargetTypeElement!!.asKotlinTypeName(),
                                annotatedElementInfo.primaryTargetTypeElement.asKotlinTypeName(),
                                generatedFieldsClassName,
                                conditionsClassName))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("delegate", baseClassesConfig.stateServiceDelegateClassName
                                .parameterizedBy(annotatedElementInfo.secondaryTargetTypeElement!!.asKotlinTypeName()))
                        .build())
                .addSuperclassConstructorParameter("delegate")
                .addType(TypeSpec.companionObjectBuilder().addInitializerBlock(
                        CodeBlock.of("%T.registerService(%T::class, %M::class)",
                                Registry::class, annotatedElementInfo.secondaryTargetTypeElement!!, MemberName("", generatedSimpleName))
                ).build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("serviceHub", ServiceHub::class.java)
                        .addParameter(ParameterSpec
                                .builder("defaults", ServiceDefaults::class.java)
                                .defaultValue("%T()", SimpleServiceDefaults::class.java).build())
                        .addKdoc("ServiceHub-based constructor, creates a Corda Service delegate")
                        .callThisConstructor(CodeBlock.builder()
                                .add("serviceHub.cordaService(%N::class.java)",
                                        stateCordaServiceDelegateSpec).build()).build())
                .addProperty(buildFieldsPropertySpec(annotatedElementInfo.primaryTargetTypeElement, generatedFieldsClassName))
                .addProperty(buildStatePersistableTypePropertySpec(annotatedElementInfo.primaryTargetTypeElement))
                .addInitializerBlock(CodeBlock.of("criteriaConditionsType =  %N::class.java", "${annotatedElementInfo.primaryTargetTypeElementSimpleName}Conditions"))
                .addFunction(delegateBasedConstructorBuilder(
                        annotatedElementInfo = annotatedElementInfo,
                        paramName = "poolBoy",
                        paramType = PoolBoyConnection::class.java,
                        delegateClassName = baseClassesConfig.poolBoyDelegateClassName,
                        kdoc = CodeBlock.of("PoolBopy-based RPC connection pool constructor")).build())
                .addFunction(delegateBasedConstructorBuilder(
                        annotatedElementInfo = annotatedElementInfo,
                        paramName = "rpcOps",
                        paramType = CordaRPCOps::class.java,
                        delegateClassName = baseClassesConfig.rpcDelegateClassName,
                        kdoc = legacyRpcDelegateConstructorKdoc,
                        annotations = listOf(legacyRpcDelegateConstructorAnnotation)).build())
                .addFunction(delegateBasedConstructorBuilder(
                        annotatedElementInfo = annotatedElementInfo,
                        paramName = "nodeRpcConnection",
                        paramType = NodeRpcConnection::class.java,
                        delegateClassName = baseClassesConfig.rpcConnectionDelegateClassName,
                        kdoc = legacyRpcDelegateConstructorKdoc,
                        annotations = listOf(legacyRpcDelegateConstructorAnnotation)
                ).build())
                .addFunction(buildDslFunSpec("buildQuery", conditionsClassName, KModifier.OVERRIDE))
        return stateServiceSpecBuilder
    }

    fun delegateBasedConstructorBuilder(
            annotatedElementInfo: AnnotatedElementInfo,
            paramName: String,
            paramType: Class<*>,
            delegateClassName: ClassName,
            kdoc: CodeBlock? = null,
            annotations: Iterable<AnnotationSpec>? = null
    ): Builder {
        val builder = FunSpec.constructorBuilder()
                .addParameter(paramName, paramType)
                .addParameter(ParameterSpec.builder("defaults", ServiceDefaults::class)
                        .defaultValue("%T()", SimpleServiceDefaults::class.java)
                        .build())
                .callThisConstructor(CodeBlock.builder()
                        .add("%T(%N, %T::class.java, %N)",
                                delegateClassName, paramName,
                                annotatedElementInfo.secondaryTargetTypeElement!!, "defaults").build()
                )
        if(kdoc != null) builder.addKdoc(kdoc)
        if(annotations != null) builder.addAnnotations(annotations)
        return builder
    }

    /** Create the fields object spec for the annotated element being processed */
    fun buildFieldsObjectSpec(typeElement: TypeElement, fields: List<VariableElement>, fieldsClassName: ClassName): TypeSpec.Builder {
        val fieldsSpec = TypeSpec.objectBuilder(fieldsClassName)
                .addSuperinterface(FIELDS_CLASSNAME.parameterizedBy(
                        if (typeElement.typeParameters.isEmpty()) typeElement.asKotlinTypeName()
                        else typeElement.asKotlinClassName().parameterizedBy(
                                typeElement.typeParameters.map {
                                    it.bounds.single().asKotlinTypeName()
                                })
                ))
                .addKdoc("Provides easy access to fields of [%T]", typeElement)
        if (typeElement.typeParameters.isNotEmpty()) {
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
        processingEnv.noteMessage { "buildPersistentStateFieldWrapperPropertySpec, field name: ${field.simpleName},  type: ${enclosingType}" }
        println("buildPersistentStateFieldWrapperPropertySpec, field name: ${field.simpleName},  type: ${enclosingType}")

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

