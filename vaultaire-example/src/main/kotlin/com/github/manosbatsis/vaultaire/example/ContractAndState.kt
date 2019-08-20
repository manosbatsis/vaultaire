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
package com.github.manosbatsis.vaultaire.example

import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerate
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.LedgerTransaction
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

    // State.
    data class BookState(val publisher: Party,
                         val author: Party,
                         val title: String = "Uknown",
                         val published: Date = Date()) : ContractState, QueryableState {
        override val participants get() = listOf(publisher, author)

        override fun supportedSchemas() = listOf(BookSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) = BookSchemaV1.PersistentBookState(
                publisher.name.toString(),
                author.name.toString(),
                title,
                published)

        object BookSchema

        object BookSchemaV1 : MappedSchema(BookSchema.javaClass, 1, listOf(PersistentBookState::class.java)) {

            @VaultaireGenerate(/*name = "bookConditions", */constractStateType = BookState::class)
            @Entity
            @Table(name = "books")
            class PersistentBookState(
                    @Column(name = "publisher")
                    var publisher: String = "",
                    @Column(name = "author")
                    var author: String = "",
                    @Column(name = "title")
                    var title: String = "",
                    @Column(name = "published")
                    var published: Date = Date()
            ) : PersistentState()
        }
    }
}


