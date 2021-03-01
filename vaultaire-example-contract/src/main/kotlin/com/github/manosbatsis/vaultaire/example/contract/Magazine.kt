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
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.Commands.Create
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.Commands.Delete
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.Commands.Update
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.math.BigDecimal
import java.security.PublicKey
import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

// Contract and state.
val MAGAZINE_CONTRACT_PACKAGE = MagazineContract::class.java.`package`.name
val MAGAZINE_CONTRACT_ID = MagazineContract::class.java.canonicalName

class MagazineContract : Contract {

    /**
     * Contract commands
     */
    interface Commands : CommandData {
        /** Create the initial state */
        class Create : TypeOnlyCommandData(), Commands

        /** Create the updated state */
        class Update : TypeOnlyCommandData(), Commands

        /** Delete the state */
        class Delete : TypeOnlyCommandData(), Commands
    }

    /**
     * Verify transactions
     */
    override fun verify(tx: LedgerTransaction) {
        // Ensure only one of this contract's commands is present
        val command = tx.commands.requireSingleCommand<Commands>()
        // Forward to command-specific verification
        val signers = command.signers.toSet()
        when (command.value) {
            is Create -> verifyCreate(tx, signers)
            is Update -> verifyUpdate(tx, signers)
            is Delete -> verifyDelete(tx, signers)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Create>()
        "There can be no inputs when creating magazines." using (tx.inputs.isEmpty())
        "There must be one output magazine" using (tx.outputs.size == 1)
        val yo = tx.outputsOfType<MagazineState>().single()
        "Cannot publish your own magazine!" using (yo.author != yo.publisher)
        "The magazine must be signed by the publisher." using (command.signers.contains(yo.publisher!!.party.owningKey))
        //"The magazine must be signed by the author." using (command.signers.contains(yo.author.owningKey))
    }

    fun verifyUpdate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Update>()
        "There must be one input magazine." using (tx.inputs.size == 1)
        "There must be one output magazine" using (tx.outputs.size == 1)
        val yo = tx.outputsOfType<MagazineState>().single()
        "Cannot publish your own magazine!" using (yo.author != yo.publisher)
        "The magazine must be signed by the publisher." using (command.signers.contains(yo.publisher!!.party.owningKey))
        //"The magazine must be signed by the author." using (command.signers.contains(yo.author.owningKey))
    }

    fun verifyDelete(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Delete>()
        "There must be one input magazine." using (tx.inputs.size == 1)
        "There must no output magazine" using (tx.outputs.isEmpty())
        val yo = tx.outputsOfType<MagazineState>().single()
        "Cannot delete your own magazine!" using (yo.author != yo.publisher)
        "The magazine deletion must be signed by the publisher." using (command.signers.contains(yo.publisher!!.party.owningKey))
        //"The magazine must be signed by the author." using (command.signers.contains(yo.author.owningKey))
    }


    @CordaSerializable
    enum class MagazineGenre {
        UNKNOWN,
        TECHNOLOGY,
        SCIENCE_FICTION,
        FANTACY,
        HISTORICAL
    }

    data class MagazineState(val publisher: AccountParty?,
                             val author: AccountParty,
                             val price: BigDecimal,
                             val genre: MagazineGenre,
                             val issues: Int = 1,
                             val title: String,
                             @DefaultValue("Date()")
                             val published: Date,
                             @DefaultValue("UniqueIdentifier()")
                             override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
        override val participants get() = listOfNotNull(publisher?.party, author.party)

        override fun supportedSchemas() = listOf(MagazineSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) = MagazineSchemaV1.PersistentMagazineState(
                id = linearId.id.toString(),
                externalId = linearId.externalId,
                publisher = publisher?.name,
                author = author.name,
                price = price,
                genre = genre,
                issues = issues,
                title = title,
                published = published)

        object MagazineSchema

        object MagazineSchemaV1 : MappedSchema(MagazineSchema.javaClass, 1, listOf(MagazineSchemaV1.PersistentMagazineState::class.java)) {

            @Entity
            @Table(name = "magazines")
            class PersistentMagazineState(
                    @Column(name = "linear_id")
                    var id: String,
                    @Column(name = "external_id")
                    var externalId: String? = "",
                    @Column(name = "publisher")
                    var publisher: String?,
                    @Column(name = "author")
                    var author: String,
                    @Column(name = "price")
                    var price: BigDecimal = BigDecimal.ZERO,
                    @Column(name = "GENRE")
                    var genre: MagazineGenre = MagazineGenre.UNKNOWN,
                    @Column(name = "edition_count")
                    var issues: Int = 1,
                    @Column(name = "title")
                    var title: String,
                    @Column(name = "published")
                    var published: Date = Date(),
                    @Column(name = "description", length = 500)
                    var description: String? = null
            ) : PersistentState()
        }
    }
}


