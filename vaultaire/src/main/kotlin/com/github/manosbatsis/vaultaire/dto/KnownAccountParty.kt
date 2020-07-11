package com.github.manosbatsis.vaultaire.dto

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty

/**
 * Convenient "participant" type that combines a known Corda Account ID
 * with a corresponding  anonymous party.
 */
data class AccountIdAndParty(
        /** The account name, maps to [com.r3.corda.lib.accounts.contracts.states.AccountInfo.identifier] */
        var identifier: UniqueIdentifier,
        /** The account party */
        var party: AnonymousParty
){
        fun hasMatchingIdentifier(other: AccountIdAndParty?): Boolean = identifier == other?.identifier

}

/**
 * Convenient "participant" type that combines a known Corda Account name
 * with a corresponding  anonymous party.
 */
data class AccountNameAndParty(
        /** The account ID, maps to [com.r3.corda.lib.accounts.contracts.states.AccountInfo.name] */
        var name: String,
        var party: AnonymousParty
)
