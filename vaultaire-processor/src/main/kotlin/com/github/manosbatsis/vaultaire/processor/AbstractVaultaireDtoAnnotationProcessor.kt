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

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.ConstructorRefsCompositeDtoStrategy
import com.github.manosbatsis.kotlin.utils.kapt.processor.AbstractAnnotatedModelInfoProcessor
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.kotlin.utils.kapt.processor.SimpleAnnotatedElementInfo
import com.github.manosbatsis.vaultaire.annotation.*
import javax.lang.model.element.VariableElement

abstract class AbstractVaultaireDtoAnnotationProcessor(
        primaryTargetRefAnnotationName: String,
        secondaryTargetRefAnnotationName: String
) : AbstractAnnotatedModelInfoProcessor(primaryTargetRefAnnotationName, secondaryTargetRefAnnotationName){

    companion object{
        internal var addedViewHints = false
    }

    /** Get a list of DTO strategies to apply per annotated element */
    abstract fun getDtoStrategies(annotatedElementInfo: AnnotatedElementInfo): Map<String, ConstructorRefsCompositeDtoStrategy<*, *, *>>

    open fun processElementInfo(elementInfo: AnnotatedElementInfo) {

        val originalStrategies = getDtoStrategies(elementInfo)
        val viewInfos = getViewInfos(elementInfo)
        val viewStrategies = originalStrategies.map { (_, dtoStrategy) ->
            viewInfos.map { (viewName, viewElementInfo)  ->
                // Set unique file name
                viewElementInfo.overrideClassNameSuffix =
                        "${dtoStrategy.dtoNameStrategy.getClassNameSuffix()}${viewName}View"
                viewName to dtoStrategy.with(viewElementInfo)
            }
        }.flatten().toMap()
        val allStrategies = originalStrategies + viewStrategies
        allStrategies.map { (_, strategy) ->
            val dtoStrategyBuilder = strategy.dtoTypeSpecBuilder()
            val dto = dtoStrategyBuilder.build()
            val dtoClassName = strategy.dtoNameStrategy.getClassName()
            val fileName = dtoClassName.simpleName
            val packageName = dtoClassName.packageName
            // Generate the Kotlin file

            val annElem = strategy.annotatedElementInfo
            val fileBuilder = getFileSpecBuilder(packageName, fileName)
            fileBuilder.addComment("\n")
                    .addComment("----------------------------------------------------\n")
                    .addComment("Strategies\n")
                    .addComment("Main Strategy: ${strategy.javaClass.simpleName}\n")
                    .addComment("Name: ${strategy.dtoNameStrategy.javaClass.simpleName}\n")
                    .addComment("Type: ${strategy.dtoTypeStrategy.javaClass.simpleName}\n")
                    .addComment("Members: ${strategy.dtoMembersStrategy.javaClass.simpleName}\n")
                    .addComment("----------------------------------------------------\n")
                    .addComment("\n")
                    .addComment("----------------------------------------------------\n")
                    .addComment("Annotation: ${annElem.annotation.annotationType}\n")
                    .addComment("Primary Element: ${annElem.primaryTargetTypeElement.javaClass.canonicalName}\n")
                    .addComment("Second Element: ${annElem.secondaryTargetTypeElement?.javaClass?.canonicalName?:"none"}\n")
                    .addComment("Mixin Element: ${annElem.mixinTypeElement?.javaClass?.canonicalName?:"none"}\n")
                    .addComment("----------------------------------------------------\n")
                    .addType(dto)
                    .build()
                    .writeTo(sourceRootFile)
        }
    }

    data class ViewInfo(
            val name: String,
            val fields: List<String>,
            val strategies: List<VaultaireDtoStrategyKeys> = emptyList()
    ){
        fun List<VariableElement>.filterFields(): List<VariableElement>{
            return this.filter { field ->
                fields.contains(field.simpleName.toString())
            }
        }

        fun cloneElementInfo(source: AnnotatedElementInfo): AnnotatedElementInfo {
            return SimpleAnnotatedElementInfo(
                    processingEnvironment = source.processingEnvironment,
                    annotation = source.annotation,
                    primaryTargetTypeElement = source.primaryTargetTypeElement,
                    primaryTargetTypeElementFields = source.primaryTargetTypeElementFields.filterFields(),
                    secondaryTargetTypeElement = source.secondaryTargetTypeElement,
                    secondaryTargetTypeElementFields = source.secondaryTargetTypeElementFields.filterFields(),
                    mixinTypeElement = source.mixinTypeElement,
                    mixinTypeElementFields = source.mixinTypeElementFields.filterFields(),
                    copyAnnotationPackages = source.copyAnnotationPackages,
                    ignoreProperties = source.ignoreProperties,
                    generatedPackageName = source.generatedPackageName,
                    sourceRoot = source.sourceRoot,
                    primaryTargetTypeElementSimpleName = source.primaryTargetTypeElementSimpleName,
                    secondaryTargetTypeElementSimpleName = source.secondaryTargetTypeElementSimpleName,
                    mixinTypeElementSimpleName = source.mixinTypeElementSimpleName,
                    skipToTargetTypeFunction = true,
                    isNonDataClass = source.isNonDataClass
            )
        }
    }

    override fun processElementInfos(elementInfos: List<AnnotatedElementInfo>) {
        elementInfos.forEach {
            processElementInfo(it)
        }
    }

    fun getViewInfos(
            elementInfo: AnnotatedElementInfo, packageName: String = elementInfo.generatedPackageName
    ): Map<String, AnnotatedElementInfo> {
        return listOfNotNull(elementInfo.mixinTypeElement, elementInfo.primaryTargetTypeElement, elementInfo.secondaryTargetTypeElement)
                .mapNotNull { elem ->
                    val views: List<VaultaireView> = when {
                            elem.hasAnnotationDirectly(VaultaireStateDto::class.java) ->
                                elem.getAnnotation(VaultaireStateDto::class.java).views
                            elem.hasAnnotationDirectly(VaultaireStateDtoMixin::class.java) ->
                                elem.getAnnotation(VaultaireStateDtoMixin::class.java).views
                            elem.hasAnnotationDirectly(VaultaireModelDto::class.java) ->
                                elem.getAnnotation(VaultaireModelDto::class.java).views
                            elem.hasAnnotationDirectly(VaultaireModelDtoMixin::class.java) ->
                                elem.getAnnotation(VaultaireModelDtoMixin::class.java).views
                            else -> emptyArray()
                        }.toList()


                    if (views.isNotEmpty()) views else null

                }
                .flatten()
                .map {
                    println("getViewInfos, view ann: $it")
                    val view = ViewInfo(
                        it.name,
                        it.namedFields.toList() + it.viewFields.map { it.name })

                    println("getViewInfos, view info: $view")
                    view.name to view.cloneElementInfo(elementInfo)
                }.toMap()

    }


}