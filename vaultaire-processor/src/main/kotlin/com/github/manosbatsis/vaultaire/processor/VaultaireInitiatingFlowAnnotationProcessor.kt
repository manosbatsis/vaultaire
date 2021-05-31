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

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.kotlin.utils.ProcessingEnvironmentAware
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotationProcessorBase
import com.github.manosbatsis.vaultaire.annotation.VaultaireFlowResponder
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.TypeElement

/**
 * Kapt processor for the `@VaultaireStateUtils` annotation.
 * Constructs a VaultaireStateUtils for the annotated class.
 */
@SupportedAnnotationTypes("com.github.manosbatsis.vaultaire.annotation.VaultaireFlowResponder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(AnnotationProcessorBase.KAPT_OPTION_NAME_KAPT_KOTLIN_GENERATED)
class VaultaireInitiatingFlowAnnotationProcessor : AbstractProcessor(), ProcessingEnvironmentAware {


    companion object {
        const val BASE_TYPE = "value"
        const val COMMENT = "comment"
    }

    /** Implement [ProcessingEnvironment] access */
    override val processingEnvironment by lazy {
        processingEnv
    }
    val sourcesAnnotation = VaultaireFlowResponder::class.java

    val generatedSourcesRoot: String by lazy {
        processingEnv.options[AnnotationProcessorBase.KAPT_OPTION_NAME_KAPT_KOTLIN_GENERATED]
                ?: processingEnv.options[AnnotationProcessorBase.KAPT_OPTION_NAME_KAPT_KOTLIN_GENERATED]
                ?: throw IllegalStateException("Can't find the target directory for generated Kotlin files.")
    }

    val sourceRootFile by lazy {
        val sourceRootFile = File(generatedSourcesRoot)
        sourceRootFile.mkdir()
        sourceRootFile
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        // Group any target responders to generate by package name
        val responderTargets = roundEnv.getElementsAnnotatedWith(sourcesAnnotation).groupBy { it.asType().asTypeElement().getPackageName() }
        // Return if nothing to do
        if (responderTargets.keys.isEmpty()) return false
        // Create responders
        responderTargets.entries.forEach { it ->
            val packageName = "${it.key}.generated"
            // Generate the Kotlin file
            val fileSpecBuilder = getFileSpecBuilder(packageName, "VaultaireGeneratedResponders")
            // Add each responder
            it.value.forEach {
                fileSpecBuilder.addType(generateResponderFlow(packageName, it as TypeElement))
            }
            // Write responders file
            fileSpecBuilder.build()
                    .writeTo(sourceRootFile)
        }
        return false
    }

    /** Create the responder flow for the given [annotatedElement] initiating flow */
    fun generateResponderFlow(packageName: String, annotatedElement: TypeElement): TypeSpec {
        val responderAnnotation = annotatedElement.getAnnotationMirror(sourcesAnnotation)
        val comment = responderAnnotation.findAnnotationValue(COMMENT)
        val baseFlowTypeElement: TypeElement = responderAnnotation.getValueAsTypeElement(BASE_TYPE)
        val responderClassName = ClassName(packageName, "${annotatedElement.simpleName}Responder")
        // Create responder type
        val responderSpec = TypeSpec.classBuilder(responderClassName)
                .addAnnotation(AnnotationSpec.builder(InitiatedBy::class)
                        .addMember("value = %T::class", annotatedElement.asType()).build())
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("otherPartySession", FlowSession::class.asClassName())
                        .build())
        // extend or inline based on open/final base responder
        if (baseFlowTypeElement.modifiers.contains(FINAL)) {
            responderSpec.superclass(FlowLogic::class.asClassName().parameterizedBy(Unit::class.asTypeName()))
                    .addProperty(PropertySpec.builder("otherPartySession", FlowSession::class)
                            .initializer("otherPartySession")
                            .build())
                    .addFunction(FunSpec.builder("call")
                            .addAnnotation(Suspendable::class)
                            .addModifiers(OVERRIDE)
                            .addStatement("subFlow(%T(otherPartySession))", baseFlowTypeElement)
                            .build())
        } else {
            responderSpec.superclass(baseFlowTypeElement.asKotlinTypeName())
                    .addSuperclassConstructorParameter("otherPartySession")
        }
        if (comment != null) responderSpec.addKdoc(comment.value.toString())

        return responderSpec.build()
    }


}
