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

import net.corda.core.schemas.PersistentState
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.ElementFilter.fieldsIn

/**
 * Baee processor implementation.
 */
abstract class BaseStateInfoAnnotationProcessor : BaseAnnotationProcessor() {

    companion object {

    }

    abstract val sourcesAnnotation: Class<out Annotation>
    abstract val dependenciesAnnotation: Class<out Annotation>
    abstract fun process(stateInfo: StateInfo)

    /** Construct a [StateInfo] from an annotated [PersistentState] class source */
    fun stateInfoForAnnotatedPersistentStateSourceClass(classElement: TypeElement): StateInfo {
        // TODO: use declared/accessible fields instead of constructor params?
        return stateInfoForAnnotatedPersistentState(classElement, classElement.accessibleConstructorParameterFields())
    }

    /** Construct a [StateInfo] [PersistentState] constructor source */
    fun stateInfoForAnnotatedPersistentStateSourceConstructor(constructor: ExecutableElement): StateInfo {
        return stateInfoForAnnotatedPersistentState(constructor.enclosingElement as TypeElement, constructor.parameters)
    }


    /** Handle an annotated [PersistentState] source */
    fun stateInfoForAnnotatedPersistentState(persistentStateTypeElement: TypeElement, fields: List<VariableElement>): StateInfo {
        val annotation = persistentStateTypeElement.getAnnotationMirror(sourcesAnnotation)
        val contractStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_CONTRACT_STATE)
        val contractStateTypeElement: Element = processingEnv.typeUtils.asElement(contractStateTypeAnnotationValue.value as TypeMirror)
        return stateInfo(persistentStateTypeElement, contractStateTypeElement, fields)
    }

    /** Invokes [writeBuilderForAnnotatedPersistentState] to create a builder for the given [persistentStateTypeElement]. */
    fun stateInfoForDependency(annotatedElement: Element): StateInfo {
        val annotation = annotatedElement.getAnnotationMirror(dependenciesAnnotation)
        val persistentStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_PERSISTENT_STATE)
        val persistentStateTypeElement: TypeElement = processingEnv.typeUtils
                .asElement(persistentStateTypeAnnotationValue.value as TypeMirror).asType().asTypeElement()
        val persistentStateFields = fieldsIn(processingEnv.elementUtils.getAllMembers(persistentStateTypeElement))

        val contractStateTypeAnnotationValue = annotation.getAnnotationValue(ANN_ATTR_CONTRACT_STATE)
        val contractStateTypeElement: Element = processingEnv.typeUtils.asElement(contractStateTypeAnnotationValue.value as TypeMirror)
        return stateInfo(persistentStateTypeElement, contractStateTypeElement, persistentStateFields)
    }

    fun stateInfo(persistentStateTypeElement: TypeElement, contractStateTypeElement: Element, fields: List<VariableElement>): StateInfo {
        val contractStateFields = ElementFilter.fieldsIn(processingEnv.elementUtils.getAllMembers(contractStateTypeElement.asType().asTypeElement()))
        return StateInfoBuilder()
                .contractStateTypeElement(contractStateTypeElement)
                .contractStateFields(contractStateFields)
                .persistentStateTypeElement(persistentStateTypeElement)
                .persistentStateFields(fields)
                .generatedPackageName(contractStateTypeElement.asType()
                        .asTypeElement().asKotlinClassName().topLevelClassName().packageName.getParentPackageName() + ".generated")
                .sourceRoot(sourceRootFile).build()
    }
}
