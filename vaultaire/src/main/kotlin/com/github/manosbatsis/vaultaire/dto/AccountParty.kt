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
package com.github.manosbatsis.vaultaire.dto

import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable
import java.util.UUID

/**
 * Convenient "participant" type that combines a known Corda Account ID
 * with a corresponding anonymous party.
 */
@CordaSerializable
data class AccountParty(
        /**
         * The account ID, maps to
         * [com.r3.corda.lib.accounts.contracts.states.AccountInfo.identifier.id]
         */
        var identifier: UUID,
        /**
         * The account name, maps to
         * [com.r3.corda.lib.accounts.contracts.states.AccountInfo.name]
         * */
        var name: String,
        /** The account party */
        var party: AnonymousParty,
        /**
         * The account external ID, maps to
         * [com.r3.corda.lib.accounts.contracts.states.AccountInfo.identifier.externalId]
         */
        var externalId: String? = null
) {
    fun hasMatchingIdentifier(other: AccountParty?): Boolean = identifier == other?.identifier

}
