package com.github.manosbatsis.vaultaire.example.contract

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.manosbatsis.kotlin.utils.api.DefaultValue
import com.github.manosbatsis.vaultaire.annotation.PersistenrPropertyMappingMode
import com.github.manosbatsis.vaultaire.annotation.VaultaireStateType
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.example.contract.support.PublicationContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.math.BigDecimal
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@VaultaireStateType(
        belongsTo = NewsPaperContract::class,
        persistentMappingModes = [
            PersistenrPropertyMappingMode.NATIVE,
            PersistenrPropertyMappingMode.STRINGIFY,
            PersistenrPropertyMappingMode.EXPANDED
        ]
)
interface NewsPaper: PublicationContractState<AccountParty> {
    override val publisher: AccountParty?
    override val author: AccountParty
    val price: BigDecimal
    val editions: Int
    val title: String
    val published: Date
    val alternativeTitle: String?
    //val linearId: UniqueIdentifier

    //override val participants get() = listOfNotNull(publisher?.party, author.party)

    override fun supportedSchemas() = listOf(NewsPaperSchemaV1)

    override fun generateMappedObject(schema: MappedSchema) = NewsPaperSchemaV1.PersistentNewsPaperState(
            id = linearId.id.toString(),
            externalId = linearId.externalId,
            publisher = publisher?.name,
            author = author.name,
            price = price,
            title = title,
            published = published)

    object NewsPaperSchema

    object NewsPaperSchemaV1 : MappedSchema(NewsPaperSchema.javaClass, 1, listOf(NewsPaperSchemaV1.PersistentNewsPaperState::class.java)) {

        @Entity
        @Table(name = "newsPapers")
        class PersistentNewsPaperState(
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
                @Column(name = "title")
                var title: String,
                @Column(name = "published")
                var published: Date = Date()
        ) : PersistentState()
    }
}