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
package com.github.manosbatsis.vaultaire.example.contract

import com.github.manosbatsis.kotlin.utils.api.DefaultValue
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.example.contract.support.AbstractPublicationContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.math.BigDecimal
import java.security.PublicKey
import java.util.*

// Contract and state.
val MAGAZINE_CONTRACT_PACKAGE = MagazineContract::class.java.`package`.name
val MAGAZINE_CONTRACT_ID = MagazineContract::class.java.canonicalName

class MagazineContract : AbstractPublicationContract<AccountParty, MagazineState>(MagazineState::class.java) {



    @CordaSerializable
    enum class MagazineGenre {
        UNKNOWN,
        TECHNOLOGY,
        SCIENCE_FICTION,
        FANTACY,
        HISTORICAL
    }

    data class MagazineModel(val publisher: AccountParty?,
                             val author: AccountParty,
                             val price: BigDecimal,
                             val genre: MagazineGenre,
                             val issues: Int = 1,
                             val title: String,
                             @DefaultValue("Date()")
                             val published: Date,
                             @DefaultValue("UniqueIdentifier()")
                             val linearId: UniqueIdentifier = UniqueIdentifier())


    override fun owningKey(party: AccountParty?): PublicKey? {
        return party?.party?.owningKey
    }

}


