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
            getFileSpecBuilder(packageName, fileName)
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