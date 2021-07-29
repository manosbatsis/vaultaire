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
package com.github.manosbatsis.vaultaire.processor.contractstate

import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoMembersStrategy
import com.github.manosbatsis.kotlin.utils.kapt.dto.strategy.composition.DtoStrategyLesserComposition
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.processor.contractstate.base.BaseStateMembersStrategy
import com.github.manosbatsis.vaultaire.processor.contractstate.base.MappedProperty
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import java.security.PublicKey
import javax.lang.model.element.VariableElement

open class ContractStateMembersStrategy(
        rootDtoStrategy: DtoStrategyLesserComposition
) : DtoMembersStrategy, BaseStateMembersStrategy(
        rootDtoStrategy
) {

    companion object{
        val participantClasses = listOf<Class<*>>(
                Party::class.java,
                AnonymousParty::class.java,
                AbstractParty::class.java,
                AccountParty::class.java,
                PublicKey::class.java
        )
    }

    val generateMappedObjectFuncSpecBuilder = FunSpec.builder("generateMappedObject")
            .addParameter("schema", MappedSchema::class.java)
    val generateMappedObjectFuncCodeBuilder by lazy {
        CodeBlock.builder().addStatement("return %T(", PersistentStateNameStrategy(rootDtoStrategy).getClassName())
    }

    override fun toDefaultValueExpression(variableElement: VariableElement): Pair<String, Boolean>? =
            if("${variableElement.simpleName}" == "linearId") "UniqueIdentifier()" to false
            else super.toDefaultValueExpression(variableElement)



    override fun addParamAndProperty1(
            typeSpecBuilder: TypeSpec.Builder, baseMappedProperty: MappedProperty
    ): TypeSpec.Builder {
        persistentPropertyMapperCache.toPersistentProperties(baseMappedProperty).forEach { mappedProperty ->

            dtoConstructorBuilder.addParameter(
                    ParameterSpec.builder(mappedProperty.propertyName, mappedProperty.propertyType)
                            .apply { mappedProperty.propertyDefaults?.first?.let { defaultValue(it) } }.build()
            )
            val propertySpecBuilder = rootDtoMembersStrategy.toPropertySpecBuilder(
                    mappedProperty.fieldIndex, mappedProperty.variableElement, mappedProperty.propertyName, mappedProperty.propertyType
            )
            typeSpecBuilder.addProperty(propertySpecBuilder.build())
        }

        return typeSpecBuilder
    }
    override fun addParamAndProperty(
            typeSpecBuilder: TypeSpec.Builder, mappedProperty: MappedProperty
    ): TypeSpec.Builder {
        dtoConstructorBuilder.addParameter(
                ParameterSpec.builder(mappedProperty.propertyName, mappedProperty.propertyType)
                        .apply { mappedProperty.propertyDefaults?.first?.let { defaultValue(it) } }.build()
        )
        val propertySpecBuilder = rootDtoMembersStrategy.toPropertySpecBuilder(
                mappedProperty.fieldIndex,
                mappedProperty.variableElement,
                mappedProperty.propertyName,
                mappedProperty.propertyType)
        typeSpecBuilder.addProperty(propertySpecBuilder.build())
        return typeSpecBuilder
    }

    fun toMappedObjectPropertyStatement(mappedProperty: MappedProperty): DtoMembersStrategy.Statement {
        val commaOrEmpty = if(mappedProperty.fieldIndex > 0) "," else ""
        val propertyPath = mappedProperty.propertyPath.joinToString(".")
        return DtoMembersStrategy.Statement("${mappedProperty.propertyName} = $propertyPath$commaOrEmpty")
    }

    override fun toPropertySpecModifiers(
            variableElement: VariableElement, propertyName: String, propertyType: TypeName
    ): Array<KModifier> = arrayOf(KModifier.OVERRIDE, KModifier.PUBLIC)

    override fun finalize(typeSpecBuilder: TypeSpec.Builder) {
        // Add participants field if missing
        if(!declaresMethod("getParticipants", annotatedElementInfo.primaryTargetTypeElement)){
            val participantTypes = participantClasses.map { it.asTypeName().copy(nullable = false).toString() }
            val participantFields = annotatedElementInfo.primaryTargetTypeElementFields
                    .filter { participantTypes.contains(it.asType().asTypeName().copy(nullable = false).toString()) }

            val participantsSpec = PropertySpec.builder(
                    "participants", List::class.parameterizedBy(AbstractParty::class), KModifier.OVERRIDE)
                    .getter(FunSpec.getterBuilder()
                            .addStatement("return toParticipants(${participantFields.joinToString(",") { it.simpleName }})")
                            .build())
                    .build()
            typeSpecBuilder.addProperty(participantsSpec)

        }

        typeSpecBuilder.primaryConstructor(dtoConstructorBuilder.build())
    }
}
