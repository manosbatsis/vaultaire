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
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateResponder
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.TypeElement

/**
 * Kapt processor for the `@VaultaireGenerate` annotation.
 * Constructs a VaultaireGenerate for the annotated class.
 */
@SupportedAnnotationTypes("com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateResponder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
        KAPT_KOTLIN_GENERATED_OPTION_NAME,
        KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME)
class VaultaireInitiatingFlowAnnotationProcessor : BaseAnnotationProcessor() {

    companion object {
        const val BASE_TYPE = "value"
        const val COMMENT = "comment"
    }

    val sourcesAnnotation = VaultaireGenerateResponder::class.java


    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        // Group any target responders to generate by package name
        val responderTargets = roundEnv.getElementsAnnotatedWith(sourcesAnnotation).groupBy { it.asType().asTypeElement().getPackageName() }
        // Return if nothing to do
        if(responderTargets.keys.isEmpty()) return false
        // Create responders
        responderTargets.entries.forEach { it ->
            val packageName = "${it.key}.generated"
            // Generate the Kotlin file
            val fileSpecBuilder = getFileSpecBuilder(packageName, "VaultaireGeneratedResponders")
            // Add each responder
            it.value .forEach {
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
        if(baseFlowTypeElement.modifiers.contains(FINAL)){
            responderSpec.superclass(FlowLogic::class.asClassName().parameterizedBy(Unit::class.asTypeName()))
                    .addProperty(PropertySpec.builder("otherPartySession", FlowSession::class)
                            .initializer("otherPartySession")
                            .build())
                    .addFunction(FunSpec.builder("call")
                            .addAnnotation(Suspendable::class)
                            .addModifiers(OVERRIDE)
                            .addStatement("subFlow(%T(otherPartySession))", baseFlowTypeElement)
                            .build())
        }
        else {
            responderSpec.superclass(baseFlowTypeElement.asKotlinTypeName())
                    .addSuperclassConstructorParameter("otherPartySession")
        }
        if(comment != null) responderSpec.addKdoc(comment.value.toString())

        return responderSpec.build()
    }


}
