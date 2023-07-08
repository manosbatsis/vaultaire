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
import com.github.manosbatsis.vaultaire.annotation.VaultaireStateDto
import com.github.manosbatsis.vaultaire.annotation.VaultaireStateUtils
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.example.contract.support.AbstractPublicationContract
import com.github.manosbatsis.vaultaire.example.contract.support.PublicationContractState
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.math.BigDecimal
import java.security.PublicKey
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

// Contract and state.
val BOOK_CONTRACT_PACKAGE = BookContract::class.java.`package`.name
val BOOK_CONTRACT_ID = BookContract::class.java.canonicalName

class BookContract : AbstractPublicationContract<Party, BookContract.BookState>(BookContract.BookState::class.java) {


    @CordaSerializable
    enum class Genre {
        UNKNOWN,
        TECHNOLOGY,
        SCIENCE_FICTION,
        FANTACY,
        HISTORICAL
    }

    /* TODO
    @VaultaireStateDto(
            copyAnnotationPackages = ["com.fasterxml.jackson.annotation"],
            // Default is [VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO]
            strategies = [VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO, VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO])

     */
    data class PrivateBookDraftState(
            override val author: AccountParty,
            override val publisher: AccountParty?,
            override val linearId: UniqueIdentifier = UniqueIdentifier()) : PublicationContractState<AccountParty> {

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

            @VaultaireStateUtils(/*name = "bookConditions", */contractStateType = PrivateBookDraftState::class)
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
    @VaultaireStateDto(
            copyAnnotationPackages = ["com.fasterxml.jackson.annotation"],
            // Default is [VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO]
            strategies = [VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO, VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO])
    data class BookState(
            override val publisher: Party?,
            override val author: Party,
            val price: BigDecimal,
            val genre: Genre,
            @DefaultValue("1")
            val editions: Int = 1,
            val title: String = "Uknown",
            val published: Date = Date(),
            val soldOut: Date? = null,
            @field:JsonProperty("alias")
            val alternativeTitle: String? = null,
            override val linearId: UniqueIdentifier = UniqueIdentifier()) : PublicationContractState<Party> {

        companion object {
            val test = "test"
        }

        enum class TestEnum

        override val participants: List<AbstractParty> = listOfNotNull(publisher, author)

        override fun supportedSchemas() = listOf(BookSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) = BookSchemaV1.PersistentBookState(
            id = linearId.id.toString(),
            externalId = linearId.externalId,
            publisher = publisher!!.name.toString(),
            author = author.name.toString(),
            price = price,
            genre = genre,
            editions = editions,
            title = title,
            alternativeTitle = alternativeTitle,
            published = published,
            soldOut = soldOut
        )

        object BookSchema

        object BookSchemaV1 : MappedSchema(BookSchema.javaClass, 1, listOf(BookSchemaV1.PersistentBookState::class.java)) {

            @VaultaireStateUtils(/*name = "bookConditions", */contractStateType = BookState::class)
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
                    var published: Date,
                    @Column(name = "sold_out")
                    val soldOut: Date?,
                    @Column(name = "description", length = 500)
                    var description: String? = null
            ) : PersistentState()
        }
    }

    override fun owningKey(party: Party?): PublicKey? {
        return party?.owningKey
    }

}


