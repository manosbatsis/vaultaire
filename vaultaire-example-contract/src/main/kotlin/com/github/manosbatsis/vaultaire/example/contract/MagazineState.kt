package com.github.manosbatsis.vaultaire.example.contract

import com.github.manosbatsis.kotlin.utils.api.DefaultValue
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.example.contract.support.PublicationContractState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.math.BigDecimal
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@BelongsToContract(MagazineContract::class)
data class MagazineState(override val publisher: AccountParty?,
                         override val author: AccountParty,
                         val price: BigDecimal,
                         val genre: MagazineContract.MagazineGenre,
                         val issues: Int = 1,
                         val title: String,
                         @DefaultValue("Date()")
                         val published: Date,
                         @DefaultValue("UniqueIdentifier()")
                         override val linearId: UniqueIdentifier = UniqueIdentifier()) : PublicationContractState<AccountParty> {
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
                var genre: MagazineContract.MagazineGenre = MagazineContract.MagazineGenre.UNKNOWN,
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