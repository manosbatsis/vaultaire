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
package com.github.manosbatsis.vaultaire.plugin.accounts.processor

import com.github.manosbatsis.kotlin.utils.ProcessingEnvironmentAware
import com.github.manosbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.github.manosbatsis.vaultaire.annotation.VaultaireAccountInfo
import com.github.manosbatsis.vaultaire.annotation.VaultaireAccountInfos
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.plugin.accounts.processor.Util.Companion.CLASSNAME_ACCOUNT_INFO
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.squareup.kotlinpoet.asClassName
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.security.PublicKey
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import com.squareup.kotlinpoet.TypeVariableName

import com.squareup.kotlinpoet.TypeName

import javax.lang.model.element.TypeParameterElement

import java.util.ArrayList
import javax.lang.model.type.DeclaredType
import javax.tools.Diagnostic


class Util {
    companion object {

        val CLASSNAME_ACCOUNT_INFO = AccountInfo::class.java.asClassName()
        val CLASSNAME_PARTY = Party::class.java.asClassName()
        val CLASSNAME_ABSTRACT_PARTY = AbstractParty::class.java.asClassName()
        val CLASSNAME_ANONYMOUS_PARTY = AnonymousParty::class.java.asClassName()
        val CLASSNAME_PUBLIC_KEY = PublicKey::class.java.asClassName()
        val CLASSNAME_ACCOUNT_PARTY = AccountParty::class.java.asClassName()

    }

}

class AccountInfoHelper(
        val annotatedElementInfo: AnnotatedElementInfo
) : ProcessingEnvironmentAware {

    override val processingEnvironment: ProcessingEnvironment = annotatedElementInfo.processingEnvironment

    private fun hasAccountInfoFields(): Boolean {
        with(annotatedElementInfo) {
            val relevantFields = primaryTargetTypeElementFields + secondaryTargetTypeElementFields
            val result = relevantFields.find {
                // Not ignored and mapped to AccountInfo
                !annotatedElementInfo.ignoreProperties.contains(it.simpleName.toString())
                        && (isAccountInfo(it) || isAccountInfos(it))
            }
            return result != null
        }
    }

    fun useAccountInfo(): Boolean = isAccountInfo() || hasAccountInfoFields()

    private fun isAccountInfo(): Boolean =
            isAccountInfo(annotatedElementInfo.primaryTargetTypeElement)
                    || isAccountInfo(annotatedElementInfo.secondaryTargetTypeElement)

    private fun isAccountInfo(targetTypeElement: TypeElement?): Boolean =
            CLASSNAME_ACCOUNT_INFO == targetTypeElement?.asClassName()


    fun isAccountInfo(variableElement: VariableElement): Boolean {
        val fieldTypeElement = variableElement.asType().asTypeElement()
        val fieldClassName = fieldTypeElement.asClassName()
        return when {
            Util.CLASSNAME_ACCOUNT_PARTY == fieldClassName -> true
            listOf(Util.CLASSNAME_ABSTRACT_PARTY, Util.CLASSNAME_ANONYMOUS_PARTY, Util.CLASSNAME_PUBLIC_KEY).contains(fieldClassName)
                    && variableElement.hasAnnotation(VaultaireAccountInfo::class.java) -> true
            !listOf(Util.CLASSNAME_ABSTRACT_PARTY, Util.CLASSNAME_ANONYMOUS_PARTY, Util.CLASSNAME_PUBLIC_KEY).contains(fieldClassName)
                    && variableElement.hasAnnotation(VaultaireAccountInfo::class.java) -> throw IllegalArgumentException(
                    "VaultaireAccountInfo annotation does not support type: $fieldClassName")
            else -> false
        }

    }

    fun isAccountInfos(variableElement: VariableElement): Boolean {
        val fieldType = variableElement.asType()
        val fieldTypeElement = fieldType.asTypeElement()
        return if(fieldTypeElement.isAssignableTo(Iterable::class.java, true) && fieldType is DeclaredType){
            fieldType.typeArguments.any {
                it is DeclaredType && it.asTypeElement().asClassName() == Util.CLASSNAME_ACCOUNT_PARTY
            }
        } else false
    }
}
