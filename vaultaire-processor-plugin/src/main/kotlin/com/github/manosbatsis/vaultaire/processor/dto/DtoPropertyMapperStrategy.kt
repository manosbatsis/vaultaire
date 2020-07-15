package com.github.manosbatsis.vaultaire.processor.dto

import com.github.manotbatsis.kotlin.utils.kapt.dto.strategy.DtoMembersStrategy
import com.squareup.kotlinpoet.TypeName
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement


interface DtoPropertyMapper{

    fun supportsSourceType(strategy: Class<*>): Boolean

}
interface DtoPropertyMapperStrategy {

    /** Level 1 filtering */
    fun supportsMapping(strategy: Class<*>): Boolean
    /** Level 1 filtering */
    fun supportsDtoStrategy(strategy: Class<*>): Boolean

    /** Level 2 filtering */
    fun supportsEnclosingTypeElement(typeElement: TypeElement): Boolean

    /** Level 3 filtering */
    fun supportsVariableElement(variableElement: VariableElement): Boolean
    fun toAltConstructorStatement(
            index: Int, variableElement: VariableElement, propertyName: String, propertyType: TypeName, commaOrEmpty: String
    ): DtoMembersStrategy.Statement?

    fun toPatchStatement(variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement?
    fun toMapStatement(variableElement: VariableElement, commaOrEmpty: String): DtoMembersStrategy.Statement?
}
