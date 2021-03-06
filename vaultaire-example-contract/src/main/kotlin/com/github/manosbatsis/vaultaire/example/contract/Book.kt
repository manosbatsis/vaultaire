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

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.manosbatsis.kotlin.utils.api.DefaultValue
import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerate
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDto
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.example.contract.BookContract.Commands.Create
import com.github.manosbatsis.vaultaire.example.contract.BookContract.Commands.Delete
import com.github.manosbatsis.vaultaire.example.contract.BookContract.Commands.Update
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
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
val BOOK_CONTRACT_PACKAGE = BookContract::class.java.`package`.name
val BOOK_CONTRACT_ID = BookContract::class.java.canonicalName

class BookContract : Contract {

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
        "There can be no inputs when creating books." using (tx.inputs.isEmpty())
        "There must be one output book" using (tx.outputs.size == 1)
        val yo = tx.outputsOfType<BookState>().single()
        "Cannot publish your own book!" using (yo.author != yo.publisher)
        "The book must be signed by the publisher." using (command.signers.contains(yo.publisher!!.owningKey))
        //"The book must be signed by the author." using (command.signers.contains(yo.author.owningKey))
    }

    fun verifyUpdate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Update>()
        "There must be one input book." using (tx.inputs.size == 1)
        "There must be one output book" using (tx.outputs.size == 1)
        val yo = tx.outputsOfType<BookState>().single()
        "Cannot publish your own book!" using (yo.author != yo.publisher)
        "The book must be signed by the publisher." using (command.signers.contains(yo.publisher!!.owningKey))
        //"The book must be signed by the author." using (command.signers.contains(yo.author.owningKey))
    }

    fun verifyDelete(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Delete>()
        "There must be one input book." using (tx.inputs.size == 1)
        "There must no output book" using (tx.outputs.isEmpty())
        val yo = tx.outputsOfType<BookState>().single()
        "Cannot delete your own book!" using (yo.author != yo.publisher)
        "The book deletion must be signed by the publisher." using (command.signers.contains(yo.publisher!!.owningKey))
        //"The book must be signed by the author." using (command.signers.contains(yo.author.owningKey))
    }


    @CordaSerializable
    enum class Genre {
        UNKNOWN,
        TECHNOLOGY,
        SCIENCE_FICTION,
        FANTACY,
        HISTORICAL
    }

    // States.
    @VaultaireGenerateDto(
            copyAnnotationPackages = ["com.fasterxml.jackson.annotation"],
            // Default is [VaultaireDtoStrategyKeys.DEFAULT]
            strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
    data class PrivateBookDraftState(
            val author: AccountParty,
            val publisher: AccountParty?,
            override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

        companion object {
            val test = "test"
        }

        enum class TestEnum

        override val participants: List<AbstractParty> =
                listOfNotNull(author, publisher)
                        .map { it.party }

        override fun supportedSchemas() = listOf(PrivateBookDraftSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) =
                PrivateBookDraftSchemaV1.PersistentPrivateBookDraftState(
                        linearId.id.toString(),
                        linearId.externalId,
                        author.name.toString(),
                        publisher?.name?.toString())

        object PrivateBookDraftSchema

        object PrivateBookDraftSchemaV1 : MappedSchema(PrivateBookDraftSchema.javaClass, 1,
                listOf(PrivateBookDraftSchemaV1.PersistentPrivateBookDraftState::class.java)) {

            @VaultaireGenerate(/*name = "bookConditions", */contractStateType = PrivateBookDraftState::class)
            @Entity
            @Table(name = "books")
            class PersistentPrivateBookDraftState(
                    @Column(name = "linear_id")
                    var id: String = "",
                    @Column(name = "external_id")
                    var externalId: String? = "",
                    var author: String = "",
                    var publisher: String?
            ) : PersistentState()
        }
    }

    // States.
    @VaultaireGenerateDto(
            copyAnnotationPackages = ["com.fasterxml.jackson.annotation"],
            // Default is [VaultaireDtoStrategyKeys.DEFAULT]
            strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
    data class BookState(
            val publisher: Party?,
            val author: Party,
            val price: BigDecimal,
            val genre: Genre,
            @DefaultValue("1")
            val editions: Int = 1,
            val title: String = "Uknown",
            val published: Date = Date(),
            @field:JsonProperty("alias")
            val alternativeTitle: String? = null,
            override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

        companion object {
            val test = "test"
        }

        enum class TestEnum

        override val participants: List<AbstractParty> = listOfNotNull(publisher, author)

        override fun supportedSchemas() = listOf(BookSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) = BookSchemaV1.PersistentBookState(
                linearId.id.toString(),
                linearId.externalId,
                publisher!!.name.toString(),
                author.name.toString(),
                price,
                genre,
                editions,
                title,
                alternativeTitle,
                published)

        object BookSchema

        object BookSchemaV1 : MappedSchema(BookSchema.javaClass, 1, listOf(BookSchemaV1.PersistentBookState::class.java)) {

            @VaultaireGenerate(/*name = "bookConditions", */contractStateType = BookState::class)
            @Entity
            @Table(name = "books")
            class PersistentBookState(
                    @Column(name = "linear_id")
                    var id: String = "",
                    @Column(name = "external_id")
                    var externalId: String? = "",
                    @Column(name = "publisher")
                    var publisher: String = "",
                    @Column(name = "author")
                    var author: String = "",
                    @Column(name = "price")
                    var price: BigDecimal = BigDecimal.ZERO,
                    @Column(name = "genre")
                    var genre: Genre = Genre.UNKNOWN,
                    @Column(name = "edition_count")
                    var editions: Int = 1,
                    @Column(name = "title")
                    var title: String = "",
                    @Column(name = "alt__title")
                    var alternativeTitle: String? = null,
                    @Column(name = "published")
                    var published: Date = Date(),
                    @Column(name = "description", length = 500)
                    var description: String? = null
            ) : PersistentState()
        }
    }
}


