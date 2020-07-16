package com.github.manosbatsis.vaultaire.dto


import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import java.util.*


/**
 * REST-friendly DTO equivalent to a Corda [com.r3.corda.lib.accounts.contracts.states.AccountInfo].
 * Also used in (generated) "light" DTOs of [net.corda.core.contracts.ContractState]s to
 * map Corda Account-based participants.
 */
@CordaSerializable
data class AccountInfoDto(
        /** Maps to [com.r3.corda.lib.accounts.contracts.states.AccountInfo.name] */
        var name: String? = null,
        /** Maps to [com.r3.corda.lib.accounts.contracts.states.AccountInfo.host] */
        var host: CordaX500Name? = null,
        /** Maps to [com.r3.corda.lib.accounts.contracts.states.AccountInfo.identifier] */
        var identifier: UUID? = null
){

        fun hasMatchingIdentifierAndName(other: AccountParty?): Boolean =
                identifier != null && identifier!! == other?.identifier
                        && name != null && name == other.name


}
