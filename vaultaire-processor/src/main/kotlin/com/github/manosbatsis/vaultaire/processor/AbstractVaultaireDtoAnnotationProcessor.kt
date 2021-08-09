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
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.ConstructorRefsCompositeDtoStrategy
import com.github.manosbatsis.kotlin.utils.kapt.processor.AbstractAnnotatedModelInfoProcessor
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.annotation.*
import com.github.manosbatsis.vaultaire.processor.dto.DtoViewStrategy
import com.squareup.kotlinpoet.asClassName

abstract class AbstractVaultaireDtoAnnotationProcessor(
        primaryTargetRefAnnotationName: String,
        secondaryTargetRefAnnotationName: String
) : AbstractAnnotatedModelInfoProcessor(primaryTargetRefAnnotationName, secondaryTargetRefAnnotationName){

    companion object{
        internal var addedViewHints = false
    }

    /** Get a list of DTO strategies to apply per annotated element */
    abstract fun getDtoStrategies(annotatedElementInfo: AnnotatedElementInfo): Map<String, ConstructorRefsCompositeDtoStrategy<*, *, *>>


    open fun getSelectedStrategyKeys(annotatedElementInfo: AnnotatedElementInfo) = annotatedElementInfo.annotation
            .findAnnotationValueListEnum("strategies", VaultaireDtoStrategyKeys::class.java)
            ?: error("Could not find annotation member: strategies")

    open fun processElementInfo(elementInfo: AnnotatedElementInfo) {
        val originalStrategies = getDtoStrategies(elementInfo)
        val viewInfos = getViewInfos(elementInfo)
        val viewStrategies = viewInfos.flatMap { viewInfo ->
            originalStrategies.filter { viewInfo.allowsStrategy(it.key) }
                .map { DtoViewStrategy(viewInfo, it.value) }
        }.associateBy { it.getClassName() }

        val allStrategies = originalStrategies + viewStrategies
        allStrategies.map { (_, strategy) ->
            val dtoStrategyBuilder = strategy.dtoTypeSpecBuilder()
            val dto = dtoStrategyBuilder.build()
            val dtoClassName = strategy.getClassName()
            val fileName = dtoClassName.simpleName
            val packageName = dtoClassName.packageName
            val annElem = strategy.annotatedElementInfo
            val fileBuilder = getFileSpecBuilder(packageName, fileName)
            strategy.onBeforeFileWrite(fileBuilder)
            fileBuilder.addComment("\n")
                    .addComment("----------------------------------------------------\n")
                    .addComment("Vaultaire Annotation Processing Info\n")
                    .addComment("----------------------------------------------------\n")
                    .addComment("Annotation: ${annElem.annotation.annotationType}\n")
                    .addComment("Source Elements\n")
                    .addComment("   Primary:   ${annElem.primaryTargetTypeElement.asClassName().canonicalName}\n")
                    .addComment("   Secondary: ${annElem.secondaryTargetTypeElement?.asClassName()?.canonicalName?:"none"}\n")
                    .addComment("   Mixin:     ${annElem.mixinTypeElement?.asClassName()?.canonicalName?:"none"}\n")
                    .addComment("Generator Strategies\n")
                    .addComment("   Main:    ${strategy.javaClass.simpleName}\n")
                    .addComment("   Name:    ${strategy.dtoNameStrategy.javaClass.canonicalName}\n")
                    .addComment("   Type:    ${strategy.dtoTypeStrategy.javaClass.canonicalName}\n")
                    .addComment("   Members: ${strategy.dtoMembersStrategy.javaClass.canonicalName}\n")
                    .addComment("----------------------------------------------------\n")
                    .addType(dto)
                    .build()
                    .writeTo(sourceRootFile)
        }
    }

    data class ViewInfo(
            val viewAnnotation: VaultaireView,
            val processingEnvironmentAware: ProcessingEnvironmentAware
    ): ProcessingEnvironmentAware by processingEnvironmentAware {

        val targetName: String
            get() = viewAnnotation.name
        val targetNameSuffix: String
            get() = viewAnnotation.nameSuffix


        val strategies: List<String> = viewAnnotation.strategies.map{ "$it" }

        fun allowsStrategy(strategyKey: String): Boolean{
            return strategies.contains(strategyKey)
        }


    }

    override fun processElementInfos(elementInfos: List<AnnotatedElementInfo>) {
        elementInfos.forEach {
            processElementInfo(it)
        }
    }

    private fun getViewInfos(
            elementInfo: AnnotatedElementInfo, packageName: String = elementInfo.generatedPackageName
    ): List<ViewInfo> {
        val processor = this
        return listOfNotNull(elementInfo.mixinTypeElement, elementInfo.primaryTargetTypeElement, elementInfo.secondaryTargetTypeElement)
                .mapNotNull { elem ->
                    when {
                        elem.hasAnnotationDirectly(VaultaireStateDto::class.java) ->
                            elem.getAnnotation(VaultaireStateDto::class.java).views
                        elem.hasAnnotationDirectly(VaultaireStateDtoMixin::class.java) ->
                            elem.getAnnotation(VaultaireStateDtoMixin::class.java).views
                        elem.hasAnnotationDirectly(VaultaireModelDto::class.java) ->
                            elem.getAnnotation(VaultaireModelDto::class.java).views
                        elem.hasAnnotationDirectly(VaultaireModelDtoMixin::class.java) ->
                            elem.getAnnotation(VaultaireModelDtoMixin::class.java).views
                        else -> emptyArray()
                    }.let{ if (it.isEmpty()) null else it.toList() }
                }
                .flatten()
                .map { ViewInfo(it, processor) }

    }


}