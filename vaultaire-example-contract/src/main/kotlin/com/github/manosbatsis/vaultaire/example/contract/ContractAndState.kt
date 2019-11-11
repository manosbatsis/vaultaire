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

import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerate
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.math.BigDecimal
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

// Contract and state.
val BOOK_CONTRACT_PACKAGE = BookContract::class.java.`package`.name
val BOOK_CONTRACT_ID = BookContract::class.java.canonicalName

class BookContract : Contract {

    // Command.
    class Send : TypeOnlyCommandData()

    // Contract code.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commands.requireSingleCommand<Send>()
        "There can be no inputs when creating books." using (tx.inputs.isEmpty())
        "There must be one output book" using (tx.outputs.size == 1)
        val yo = tx.outputsOfType<BookState>().single()
        "Cannot publish your own book!" using (yo.author != yo.publisher)
        "The book must be signed by the publisher." using (command.signers.contains(yo.publisher.owningKey))
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

    // State.
    data class BookState(val publisher: Party,
                         val author: Party,
                         val price: BigDecimal,
                         val genre: Genre,
                         val editions: Int = 1,
                         val title: String = "Uknown",
                         val published: Date = Date(),
                         override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
        override val participants get() = listOf(publisher, author)

        override fun supportedSchemas() = listOf(BookSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) = BookSchemaV1.PersistentBookState(
                linearId.id.toString(),
                linearId.externalId,
                publisher.name.toString(),
                author.name.toString(),
                price,
                genre,
                editions,
                title,
                published)

        object BookSchema

        object BookSchemaV1 : MappedSchema(BookSchema.javaClass, 1, listOf(BookSchemaV1.PersistentBookState::class.java)) {

            @VaultaireGenerate(/*name = "bookConditions", */contractStateType = BookState::class)
            @Entity
            @Table(name = "books")
            class PersistentBookState(
                    @Column(name = "linearId")
                    var id: String = "",
                    @Column(name = "externalId")
                    var externalId: String? = "",
                    @Column(name = "publisher")
                    var publisher: String = "",
                    @Column(name = "author")
                    var author: String = "",
                    @Column(name = "price")
                    var price: BigDecimal = BigDecimal.ZERO,
                    @Column(name = "GENRE")
                    var genre: Genre = Genre.UNKNOWN,
                    @Column(name = "edition_count")
                    var editions: Int = 1,
                    @Column(name = "title")
                    var title: String = "",
                    @Column(name = "published")
                    var published: Date = Date(),
                    @Column(name = "description", length = 500)
                    var description: String? = null
            ) : PersistentState()
        }
    }

    // State.
    data class MagazineState(val publisher: Party,
                             val author: Party,
                             val price: BigDecimal,
                             val genre: Genre,
                             val issues: Int = 1,
                             val title: String = "Uknown",
                             val published: Date = Date(),
                             override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
        override val participants get() = listOf(publisher, author)

        override fun supportedSchemas() = listOf(MagazineSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) = MagazineSchemaV1.PersistentMagazineState(
                linearId.id.toString(),
                linearId.externalId,
                publisher.name.toString(),
                author.name.toString(),
                price,
                genre,
                issues,
                title,
                published)

        object MagazineSchema

        object MagazineSchemaV1 : MappedSchema(MagazineSchema.javaClass, 1, listOf(MagazineSchemaV1.PersistentMagazineState::class.java)) {

            @Entity
            @Table(name = "magazines")
            class PersistentMagazineState(
                    @Column(name = "linearId")
                    var id: String = "",
                    @Column(name = "externalId")
                    var externalId: String? = "",
                    @Column(name = "publisher")
                    var publisher: String = "",
                    @Column(name = "author")
                    var author: String = "",
                    @Column(name = "price")
                    var price: BigDecimal = BigDecimal.ZERO,
                    @Column(name = "GENRE")
                    var genre: Genre = Genre.UNKNOWN,
                    @Column(name = "edition_count")
                    var issues: Int = 1,
                    @Column(name = "title")
                    var title: String = "",
                    @Column(name = "published")
                    var published: Date = Date(),
                    @Column(name = "description", length = 500)
                    var description: String? = null
            ) : PersistentState()
        }
    }
}


