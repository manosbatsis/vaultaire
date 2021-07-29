package com.github.manosbatsis.vaultaire.util

import com.github.manosbatsis.vaultaire.dto.AccountParty
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey


interface ContractStateLogic {
    fun toAbstractParty(entry: Any?): AbstractParty?{
        return when(entry){
            null -> null
            is AbstractParty -> entry
            is AccountParty -> toAbstractParty(entry)
            is PublicKey -> toAbstractParty(entry)
            else -> error("Could not convert input type to participant: ${entry.javaClass.canonicalName}")
        }
    }

    fun toAbstractParty(accountParty: AccountParty?) = accountParty?.party
    fun toAbstractParty(publicKey: PublicKey?) = publicKey?.let{AnonymousParty(it)}

    fun toAbstractParties(entries: Collection<*>?): List<AbstractParty> =
            entries?.mapNotNull(::toAbstractParty)
                    ?: emptyList()

    fun toParticipants(vararg entry: Any?): List<AbstractParty> =
            entry.filterNotNull().partition { it is Collection<*> }.run {
                first.mapNotNull(::toAbstractParty) + second.map { toAbstractParties(it as Collection<*>) }.flatten()
            }
}
