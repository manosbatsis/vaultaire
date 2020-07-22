package com.github.manosbatsis.vaultaire.dto

import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable
import java.util.UUID

/**
 * Convenient "participant" type that combines a known Corda Account ID
 * with a corresponding  anonymous party.
 */
@CordaSerializable
data class AccountParty(
        /**
         * The account name, maps to
         * [com.r3.corda.lib.accounts.contracts.states.AccountInfo.identifier]
         */
        var identifier: UUID,
        /**
         * The account ID, maps to
         * [com.r3.corda.lib.accounts.contracts.states.AccountInfo.name]
         * */
        var name: String,
        /** The account party */
        var party: AnonymousParty
){
        fun hasMatchingIdentifier(other: AccountParty?): Boolean = identifier == other?.identifier

}
