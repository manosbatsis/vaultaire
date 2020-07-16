package com.github.manosbatsis.vaultaire.plugin.accounts.processor

import com.github.manosbatsis.kotlin.utils.ProcessingEnvironmentAware
import com.github.manosbatsis.vaultaire.annotation.VaultaireAccountInfo
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manotbatsis.kotlin.utils.kapt.processor.AnnotatedElementInfo
import com.squareup.kotlinpoet.asClassName
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.security.PublicKey
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.VariableElement

class Util {
    companion object{

        val CLASSNAME_PARTY = Party::class.java.asClassName()
        val CLASSNAME_ABSTRACT_PARTY = AbstractParty::class.java.asClassName()
        val CLASSNAME_ANONYMOUS_PARTY = AnonymousParty::class.java.asClassName()
        val CLASSNAME_PUBLIC_KEY = PublicKey::class.java.asClassName()
        val CLASSNAME_ACCOUNT_PARTY = AccountParty::class.java.asClassName()

    }

}

class AccountInfoHelper(
        val annotatedElementInfo: AnnotatedElementInfo
): ProcessingEnvironmentAware {

    override val processingEnvironment: ProcessingEnvironment = annotatedElementInfo.processingEnvironment

    fun hasAccountInfoMember(): Boolean {
        val result =
                (annotatedElementInfo.primaryTargetTypeElementFields + annotatedElementInfo.secondaryTargetTypeElementFields)
                .find {
                    // Not ignored and mapped to AccountInfo
                    !annotatedElementInfo.ignoreProperties.contains(it.simpleName.toString())
                            && isAccountInfo(it)
                }
        return result != null
    }

    fun isAccountInfo(variableElement: VariableElement): Boolean{
        val fieldClassName = variableElement.asType().asTypeElement().asClassName()
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
}
