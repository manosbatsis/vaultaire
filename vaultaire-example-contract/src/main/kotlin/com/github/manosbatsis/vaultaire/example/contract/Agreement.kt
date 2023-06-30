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
val AGREEMENT_CONTRACT_PACKAGE = AgreementContract::class.java.`package`.name
val AGREEMENT_CONTRACT_ID = AgreementContract::class.java.canonicalName

class AgreementContract : Contract {


    // States.
    @VaultaireStateDto(
            copyAnnotationPackages = ["com.fasterxml.jackson.annotation"],
            // Default is [VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO]
            strategies = [VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO, VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO])
    data class AgreementState(
            val publisher: Contractee,
            val author: Party,
            override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

        companion object {
            val test = "test"
        }

        enum class TestEnum

        override val participants: List<AbstractParty> = listOfNotNull(publisher.account , author)

        override fun supportedSchemas() = listOf(AgreementSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) = AgreementSchemaV1.PersistentAgreementState(
                linearId.id.toString(),
                linearId.externalId,
                publisher!!.name.toString(),
                author.name.toString()
        )

        object AgreementSchema

        object AgreementSchemaV1 : MappedSchema(AgreementSchema.javaClass, 1, listOf(AgreementSchemaV1.PersistentAgreementState::class.java)) {

            @VaultaireStateUtils(/*name = "agreementConditions", */contractStateType = AgreementState::class)
            @Entity
            @Table(name = "agreements")
            class PersistentAgreementState(
                    @Column(name = "linear_id")
                    var id: String = "",
                    @Column(name = "external_id")
                    var externalId: String? = "",
                    @Column(name = "publisher")
                    var publisher: String = "",
                    @Column(name = "author")
                    var author: String = ""
            ) : PersistentState()
        }
    }

    override fun verify(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }

}


