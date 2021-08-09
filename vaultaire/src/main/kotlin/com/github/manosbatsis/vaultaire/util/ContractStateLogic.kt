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
