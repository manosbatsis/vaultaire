package com.github.manosbatsis.vaultaire

import com.github.manosbatsis.vaultaire.dto.AccountIdAndParty
import com.github.manosbatsis.vaultaire.dto.AccountNameAndParty
import com.squareup.kotlinpoet.asClassName
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.security.PublicKey

class Util {
    companion object{

        val CLASSNAME_PARTY = Party::class.java.asClassName()
        val CLASSNAME_ABSTRACT_PARTY = AbstractParty::class.java.asClassName()
        val CLASSNAME_ANONYMOUS_PARTY = AnonymousParty::class.java.asClassName()
        val CLASSNAME_PUBLIC_KEY = PublicKey::class.java.asClassName()
        val CLASSNAME_ACCOUNT_ID_AND_PARTY = AccountIdAndParty::class.java.asClassName()
        val CLASSNAME_ACCOUNT_NAME_AND_PARTY = AccountNameAndParty::class.java.asClassName()
    }
}
