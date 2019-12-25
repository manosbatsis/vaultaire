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

import net.corda.core.contracts.ContractState
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.StatePersistable
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.ElementFilter.fieldsIn

/**
 * Base [StateInfo]-based processor implementation. Utilizes both annotates sources and, optionally,
 * annotations targeting states coming from project dependencies
 */
abstract class BaseStateInfoAnnotationProcessor : BaseAnnotationProcessor() {

    abstract val sourcesAnnotation: Class<out Annotation>
    abstract val dependenciesAnnotation: Class<out Annotation>
    abstract fun process(stateInfo: StateInfo)

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        // Get the annotated and dependency targets
        val annotatedSourceElements = roundEnv.getElementsAnnotatedWith(sourcesAnnotation)
        val annotatedForElements = roundEnv.getElementsAnnotatedWith(dependenciesAnnotation)
        // Return if there's nothing to process
        if (nothingToProcess(annotatedSourceElements, annotatedForElements)) return false

        // Process own targets
        annotatedSourceElements.forEach { annotatedElement ->
            when (annotatedElement.kind) {
                ElementKind.CLASS -> process(stateInfoForAnnotatedStateSourceClass(annotatedElement as TypeElement))
                ElementKind.CONSTRUCTOR -> process(stateInfoForAnnotatedStateSourceConstructor(annotatedElement as ExecutableElement))
                else -> annotatedElement.errorMessage { "Invalid element type, expected a class or constructor" }
            }
        }
        // Process targets for dependencies
        annotatedForElements.forEach { annotated ->
            process(stateInfoForDependency(annotated))
        }
        return false
    }

    /** Returns `true` if there's nothing to process, `false` otherwise */
    protected fun nothingToProcess(
            annotatedSourceElements: Set<out Element>,
            annotatedForElements: Set<out Element>? = null
    ): Boolean {
        return if (annotatedSourceElements.isEmpty() && (annotatedForElements == null || annotatedForElements.isEmpty())) true
        else false
    }

    /** Construct a [StateInfo] from an annotated [PersistentState] class source */
    fun stateInfoForAnnotatedStateSourceClass(classElement: TypeElement): StateInfo {
        // TODO: use declared/accessible fields instead of constructor params?
        return stateInfoForAnnotatedState(classElement, classElement.accessibleConstructorParameterFields())
    }

    /** Construct a [StateInfo] [PersistentState] constructor source */
    fun stateInfoForAnnotatedStateSourceConstructor(constructor: ExecutableElement): StateInfo {
        return stateInfoForAnnotatedState(constructor.enclosingElement as TypeElement, constructor.parameters)
    }


    /** Handle an annotated [PersistentState] source */
    fun stateInfoForAnnotatedState(typeElement: TypeElement, fields: List<VariableElement>): StateInfo {
        val persistableStateTypeElement = if(typeElement.isAssignableTo(StatePersistable::class.java)) typeElement
        else typeElement.findAnnotationValueAsTypeElement(sourcesAnnotation, ANN_ATTR_PERSISTENT_STATE)
        val contractStateTypeElement = if(typeElement.isAssignableTo(ContractState::class.java)) typeElement
        else typeElement.getAnnotationValueAsTypeElement(sourcesAnnotation, ANN_ATTR_CONTRACT_STATE)!!
        return stateInfo(typeElement.getAnnotationMirror(sourcesAnnotation), persistableStateTypeElement, contractStateTypeElement, fields)
    }

    /** Get a [StateInfo] for the given annotation. */
    fun stateInfoForDependency(annotated: Element): StateInfo {
        val annotation = annotated.getAnnotationMirror(dependenciesAnnotation)
        val persistentStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_PERSISTENT_STATE)
        val persistentStateTypeElement: TypeElement = processingEnv.typeUtils
                .asElement(persistentStateTypeAnnotationValue.value as TypeMirror).asType().asTypeElement()
        val persistentStateFields = fieldsIn(processingEnv.elementUtils.getAllMembers(persistentStateTypeElement))

        val contractStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_CONTRACT_STATE)
        val contractStateTypeElement: Element = processingEnv.typeUtils.asElement(contractStateTypeAnnotationValue.value as TypeMirror)
        return stateInfo(annotation, persistentStateTypeElement, contractStateTypeElement, persistentStateFields,
                processingEnv.elementUtils.getPackageOf(annotated).toString())
    }

    fun stateInfo(
            annotation: AnnotationMirror,
            persistentStateTypeElement: TypeElement?,
            contractStateTypeElement: Element,
            fields: List<VariableElement>,
            basePackage: String = contractStateTypeElement.asType()
                    .asTypeElement().asKotlinClassName().topLevelClassName().packageName.getParentPackageName()
    ): StateInfo {
        val contractStateFields = ElementFilter.fieldsIn(processingEnv.elementUtils.getAllMembers(contractStateTypeElement.asType().asTypeElement()))
        val stateInfo = StateInfoBuilder()
                .annotation(annotation)
                .contractStateTypeElement(contractStateTypeElement)
                .contractStateFields(contractStateFields)
                .persistentStateTypeElement(persistentStateTypeElement)
                .persistentStateFields(fields)
                .generatedPackageName(basePackage + ".generated")
                .sourceRoot(sourceRootFile).build()
        return stateInfo
    }
}
