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

import com.github.manosbatsis.kotlin.utils.DtoInfo
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDto
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.github.manosbatsis.vaultaire.processor.BaseAnnotationProcessor.Companion.KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME
import com.squareup.kotlinpoet.TypeSpec
import net.corda.core.contracts.ContractState
import net.corda.core.serialization.CordaSerializable
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement

/**
 * Kapt processor for the `@VaultaireGenerate` annotation.
 * Constructs a VaultaireGenerate for the annotated class.
 */
@SupportedAnnotationTypes(
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDto",
        "com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
        KAPT_KOTLIN_GENERATED_OPTION_NAME,
        KAPT_KOTLIN_VAULTAIRE_GENERATED_OPTION_NAME)
class VaultaireDtoAnnotationProcessor : BaseStateInfoAnnotationProcessor() {

    override val sourcesAnnotation = VaultaireGenerateDto::class.java
    override val dependenciesAnnotation = VaultaireGenerateDtoForDependency::class.java


    /** Write a DTO for the given [ContractState] . */
    override fun process(stateInfo: StateInfo) {

        // Generate the Kotlin file
        getFileSpecBuilder(stateInfo.generatedPackageName, "${stateInfo.contractStateTypeElement.simpleName}VaultaireGeneratedDto")
                .addType(contractStateDtoSpecBuilder(stateInfo).build())
                .build()
                .writeTo(sourceRootFile)
    }

    private fun contractStateDtoSpecBuilder(stateInfo: StateInfo): TypeSpec.Builder {
        val contractState = stateInfo.contractStateTypeElement as TypeElement
        val dtoGenAnnotation = contractState.findAnnotationMirror(VaultaireGenerateDto::class.java) ?: contractState.findAnnotationMirror(VaultaireGenerateDtoForDependency::class.java)
        val copyAnnotationPackages: List<String> = getStringValuesList(dtoGenAnnotation, "copyAnnotationPackages")
        val ignoredProperties: List<String> = getStringValuesList(dtoGenAnnotation, "ignoreProperties")

        processingEnv.noteMessage { "Ignoring properties: $ignoredProperties" }
        return dtoSpecBuilder(DtoInfo(
                stateInfo.contractStateTypeElement as TypeElement,
                stateInfo.contractStateFields.filterNot { ignoredProperties.contains(it.simpleName.toString()) },
                stateInfo.generatedPackageName,
                copyAnnotationPackages))
                .addAnnotation(CordaSerializable::class.java)
    }

    // TODO: move to utils repo
    private fun getStringValuesList(annotationMirror: AnnotationMirror?, memberName: String): List<String> {
        return if (annotationMirror == null) emptyList()
        else annotationMirror.findAnnotationValueList(memberName)?.mapNotNull { it.value.toString() } ?: emptyList()
    }


}
