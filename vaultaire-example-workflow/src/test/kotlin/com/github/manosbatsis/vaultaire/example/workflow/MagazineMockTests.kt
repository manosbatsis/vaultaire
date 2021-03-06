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
package com.github.manosbatsis.vaultaire.example.workflow

import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.example.contract.BOOK_CONTRACT_PACKAGE
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineGenre
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineGenre.HISTORICAL
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineGenre.SCIENCE_FICTION
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineGenre.TECHNOLOGY
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineGenre.UNKNOWN
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineState
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoDto
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoLiteDto
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoService
import com.github.manosbatsis.vaultaire.service.dao.BasicStateService
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manosbatsis.vaultaire.service.node.NotFoundException
import com.r3.corda.lib.accounts.contracts.AccountInfoContract
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.ci.workflows.RequestKey
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.Vault
import net.corda.core.node.services.Vault.RelevancyStatus.RELEVANT
import net.corda.core.node.services.Vault.StateStatus.ALL
import net.corda.core.node.services.Vault.StateStatus.CONSUMED
import net.corda.core.node.services.Vault.StateStatus.UNCONSUMED
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp.Companion.findCordapp
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@Suppress(names = ["DEPRECATION"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // allow non-static @BeforeAll etc.
class MagazineMockTests {
    companion object {
        val logger = loggerFor<MagazineMockTests>()
    }

    // Works as long as the main and test package names are  in sync
    val cordappPackages = listOf(
            // Acounts
            AccountInfoContract::class.java.`package`.name,
            RequestKeyForAccount::class.java.`package`.name,
            RequestKey::class.java.`package`.name,
            // Vaultaire
            AccountParty::class.java.`package`.name,
            AccountInfoDto::class.java.`package`.name,
            // Partiture
            PartitureFlow::class.java.`package`.name,
            // Our coprdapp
            BOOK_CONTRACT_PACKAGE,
            this.javaClass.`package`.name)

    lateinit var network: MockNetwork

    lateinit var a: StartedMockNode
    lateinit var aAccountService: AccountInfoService
    lateinit var aPublisher: AccountInfo
    lateinit var aAuthor: AccountInfo

    lateinit var b: StartedMockNode
    lateinit var bAccountService: AccountInfoService
    lateinit var bPublisher: AccountInfo
    lateinit var bAuthor: AccountInfo


    @BeforeAll
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                networkSendManuallyPumped = false,
                notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, true)),
                cordappsForAllNodes = cordappPackages.map {
                    findCordapp(it)
                },
                threadPerNode = true))

        a = network.createPartyNode()
        b = network.createPartyNode()
        network.startNodes()

        aAccountService = AccountInfoService(a.services)
        aPublisher = flowWorksCorrectly(a, CreateAccount("aPublisher")).state.data
        aAuthor = flowWorksCorrectly(a, CreateAccount("aAuthor")).state.data


        bAccountService = AccountInfoService(b.services)
        bPublisher = flowWorksCorrectly(b, CreateAccount("bPublisher")).state.data
        bAuthor = flowWorksCorrectly(b, CreateAccount("bAuthor")).state.data

    }


    @AfterAll
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `Test @DefaultValue`() {
        assertEquals(1, MagazineStateDto().issues)
    }


    @Test
    fun `Test DSL conditions`() {
        val aPublisherDto = AccountInfoLiteDto.mapToDto(aPublisher)//aAccountService.toAccountInfoDto(aPublisher)
        val bAuthorDto = AccountInfoLiteDto.mapToDto(bAuthor)// aAccountService.toAccountInfoDto(bAuthor)
        val magazineTitle = "Test DSL conditions ${UUID.randomUUID()}"

        val magazineState = flowWorksCorrectly(a,
                CreateMagazineFlow(MagazineStateLiteDto(
                        publisher = aPublisherDto,//AccountService.toAccountParty(aPublisherDto),
                        author = bAuthorDto,
                        price = BigDecimal.valueOf(10),
                        published = Date(),
                        genre = MagazineContract.MagazineGenre.TECHNOLOGY,
                        issues = 3,
                        title = magazineTitle))).single()

        // Use the generated DSL to create query criteria
        val magazineStateQuery = magazineStateQuery {
            externalIds = listOfNotNull(aPublisherDto.identifier
                    /*, bAuthorDto.identifier
                    * TODO BUG! a StateMetadata is created for each, resulting in incorect counts,
                    *  i.e. totalStatesAvailable*/)
            status = Vault.StateStatus.UNCONSUMED // the default
            relevancyStatus = Vault.RelevancyStatus.ALL // the default
            and {
                fields.title `==` magazineTitle
                or {
                    fields.title notEqual magazineTitle
                    fields["title"] _notEqual magazineTitle
                    fields.title `!=` "false match"
                }
                or {
                    fields.description.isNull()
                    fields.description.notNull()
                }
            }
            orderBy {
                fields.title sort ASC
                fields.description sort ASC
            }
        }

        val criteria = magazineStateQuery.toCriteria()
        val sort = magazineStateQuery.toSort()
        // Test BasicStateService
        val stateBasicService = BasicStateService(a.services, MagazineState::class.java)
        testStateServiceQueryByForSingleResult(stateBasicService, criteria, sort, magazineState)

        // Test generated MagazineStateService
        val serviceHubMagazineStateService = MagazineStateService(b.services)
        testStateServiceQueryByForSingleResult(stateBasicService, criteria, sort, magazineState)

    }

    @Test
    fun `Test get or find by id`() {
        logger.info("AcountInfo for aPublisher: $aPublisher")
        logger.info("AcountInfo for bAuthor: $bAuthor")

        val aPublisherDto = AccountInfoLiteDto.mapToDto(aPublisher)
        val bAuthorDto = AccountInfoLiteDto.mapToDto(bAuthor)
        logger.info("AcountInfoDto for aPublisher: $aPublisherDto")
        logger.info("AcountInfoDto for bAuthor: $bAuthorDto")

        val identifier = UniqueIdentifier(id = UUID.randomUUID(), externalId = UUID.randomUUID().toString())

        val magazine = flowWorksCorrectly(a,
                CreateMagazineFlow(MagazineStateLiteDto(
                        publisher = aPublisherDto,
                        author = bAuthorDto,
                        price = BigDecimal.valueOf(82),
                        genre = MagazineContract.MagazineGenre.HISTORICAL,
                        issues = 24,
                        title = "Vault diaries, Volume 29",
                        linearId = identifier))).single()
        logger.info("Expected identifier: $identifier, actual: ${magazine.linearId}")
        assertEquals(identifier.id, magazine.linearId.id)
        assertEquals(identifier.externalId, magazine.linearId.externalId)
        assertEquals(aPublisherDto.identifier!!, magazine.publisher!!.identifier)
        assertEquals(bAuthorDto.identifier!!, magazine.author!!.identifier)
        mapOf(bAuthorDto to b.services, aPublisherDto to a.services).forEach {
            val accountInfoDto = it.key
            val serviceHub = it.value
            val stateService = MagazineStateService(serviceHub)
            val manualQueryResult = stateService.queryBy(
                    contractStateType = MagazineState::class.java,
                    criteria = QueryCriteria.VaultQueryCriteria(
                            //externalIds = listOf(accountInfoDto.identifier!!),
                            contractStateTypes = setOf(MagazineState::class.java))
                            .and(LinearStateQueryCriteria(
                                    linearId = listOf(identifier),
                                    relevancyStatus = RELEVANT,
                                    contractStateTypes = setOf(MagazineState::class.java))),
                    pageNumber = 1,
                    pageSize = 1).states.singleOrNull()?.state?.data
            // Query manually
            assertNotNull(manualQueryResult)
            assertEquals(identifier, manualQueryResult!!.linearId)

            // Get by linear ID
            assertNotNull(stateService.getByLinearId(identifier))
            assertNotNull(stateService.getByLinearId(identifier.toString()))
            assertNotNull(stateService.getByLinearId(identifier.id))
            assertNotNull(stateService.getByLinearId(identifier.id.toString()))

            // Find by linear ID
            assertNotNull(stateService.findByExternalId(identifier.externalId!!))
            assertNotNull(stateService.findByLinearId(identifier))
            assertNotNull(stateService.findByLinearId(identifier.toString()))
            assertNotNull(stateService.findByLinearId(identifier.id))
            assertNotNull(stateService.findByLinearId(identifier.id.toString()))

            // Get/find by external ID
            assertNotNull(stateService.getByExternalId(identifier.externalId!!))
            assertNotNull(stateService.findByExternalId(identifier.externalId!!))

            // Ensure a StateNotFoundException is thrown when no match is found in getXxxx methods
            val random = UUID.randomUUID().toString()
            assertThrows<NotFoundException> {
                stateService.getByLinearId(random)
            }
            assertThrows<NotFoundException> {
                stateService.getByExternalId(random)
            }


        }
    }

    @Test
    fun `Test find with state status cases`() {
        val aAuthorDto = AccountInfoLiteDto.mapToDto(aAuthor)
        val bPublisherDto = AccountInfoLiteDto.mapToDto(bPublisher)

        logger.info("aAuthorDto: $aAuthorDto")
        logger.info("bPublisherDto: $bPublisherDto")
        val stateService = MagazineStateService(a.services)


        logger.info("Author creates draft...")
        // Author creates draft
        logger.info("Step 1 DTO")
        val magazineDto = MagazineStateLiteDto(
                publisher = bPublisherDto,
                author = aAuthorDto,
                price = BigDecimal.valueOf(87),
                genre = MagazineContract.MagazineGenre.HISTORICAL,
                issues = 1,
                title = "Vault diaries")
        logger.info("Step 2 Flow")
        val createMagazineFlow = CreateMagazineFlow(magazineDto)
        logger.info("Step 3 Start Flow")
        val createdStates: List<MagazineState> = flowWorksCorrectly(a,
                createMagazineFlow)

        assertTrue(createdStates.isNotEmpty())
        val createdState = createdStates.first()
        assertNotNull(createdState)

        logger.info("Author created draft")
        // Update title
        logger.info("Author updates title and issues...")
        val updatedTitle = "${createdState.title} UPDATED"
        var updatedState: MagazineState = flowWorksCorrectly(a,
                UpdateMagazineFlow(MagazineStateLiteDto.mapToDto(
                        createdState.copy(
                                title = updatedTitle,
                                issues = 2),
                        stateService))).single()
        assertEquals(createdState.linearId, updatedState.linearId)

        // Update
        logger.info("Author updates title and issues again...")
        val updatedTitle2 = "${createdState.title} UPDATED2"
        val updatedState2: MagazineState = flowWorksCorrectly(a,
                UpdateMagazineFlow(MagazineStateLiteDto.mapToDto(createdState.copy(
                        title = updatedTitle2,
                        issues = 3
                ), stateService))).single()
        assertEquals(createdState.linearId, updatedState2.linearId)
        //
        logger.info("Publisher side: " +
                "Query for the consumed on the counter party node...")
        testFindWithStatusesAndService(MagazineStateService(a.services), bPublisherDto, createdState)
        testFindWithStatusesAndService(MagazineStateService(b.services), bPublisherDto, createdState)


    }

    private fun testFindWithStatusesAndService(extendedService: MagazineStateService, bPublisherDto: AccountInfoLiteDto, createdState: MagazineState) {
        logger.info("testFindWithStatusesAndService all")
        val all = extendedService.buildQuery{
            and {  }
        }

        assertTrue(extendedService.queryBy(all.toCriteria()).states.isNotEmpty())
        logger.info("testFindWithStatusesAndService consumed")

        val consumed = extendedService.buildQuery {
            externalIds = listOfNotNull(bPublisherDto.identifier)
            status = CONSUMED
            and {
                fields.price `==` createdState.price
                fields.genre `==` createdState.genre
                fields.author `==` createdState.author.name
                //if(createdState.publisher != null) fields.publisher `==` createdState.author.name+"1"
                if(createdState.genre != null) fields.genre `in` listOf(MagazineGenre.FANTACY, HISTORICAL, SCIENCE_FICTION, TECHNOLOGY, UNKNOWN)
                or {
                    fields.issues `==` 1
                    fields.issues `==` 2
                }
                fields.title `like` "%${createdState.title}%"
            }
        }
        val criteria = consumed.toCriteria()
        val results = extendedService.queryBy(criteria)
        // Get/find by external ID
        assertEquals(2, results.totalStatesAvailable.toInt())
        assertTrue(results.states.first().state.data.issues < 3)
        assertTrue(results.states[1].state.data.issues < 3)

        logger.info("testFindWithStatusesAndService unconsumed")
        val unconsumedCriteria = extendedService.buildQuery {
            externalIds = listOfNotNull(bPublisherDto.identifier)
            status = UNCONSUMED
            and {
                fields.price `==` createdState.price
                fields.genre `==` createdState.genre
                fields.author `==` createdState.author.name
                fields.issues `in` listOf(1, 2, 3)
                fields.title `like` "%${createdState.title}%"
            }
        }

        val unconsumedResults = extendedService.queryBy(unconsumedCriteria.toCriteria())

        assertEquals(1, unconsumedResults.totalStatesAvailable.toInt())
        assertTrue(unconsumedResults.states.single().state.data.issues == 3)

        logger.info("testFindWithStatusesAndService default")
        val defaultCriteria = extendedService.buildQuery {
            externalIds = listOfNotNull(bPublisherDto.identifier)
            and {
                fields.price `==` createdState.price
                fields.genre `==` createdState.genre
                fields.author `==` createdState.author.name
                fields.issues `in` listOf(1, 2, 3)
                fields.title `like` "%${createdState.title}%"
            }
        }

        val defaultResults = extendedService.queryBy(defaultCriteria.toCriteria())

        assertEquals(1, defaultResults.totalStatesAvailable.toInt())
        assertTrue(defaultResults.states.single().state.data.issues == 3)

        logger.info("testFindWithStatusesAndService all")
        val allCriteria = extendedService.buildQuery {
            status = ALL
            externalIds = listOfNotNull(bPublisherDto.identifier)
            and {
                fields.price `==` createdState.price
                fields.genre `==` createdState.genre
                fields.author `==` createdState.author.name
                fields.issues `in` listOf(1, 2, 3)
                fields.title `like` "%${createdState.title}%"
            }
        }

        val allResults = extendedService.queryBy(allCriteria.toCriteria())

        assertEquals(3, allResults.totalStatesAvailable.toInt())
    }

    private fun testStateServiceQueryByForSingleResult(
            stateService: StateService<MagazineState>,
            criteria: QueryCriteria,
            sort: Sort,
            magazineState: MagazineContract.MagazineState) {

        val magazineSearchPage = stateService.queryBy(
                criteria, 1, 10, sort
        )
        assertEquals(UNCONSUMED, magazineSearchPage.stateTypes,
        "Result states must be unconsumed1")
        assertEquals(1, magazineSearchPage.states.size, "Number of states must be 1")
        assertEquals(1, magazineSearchPage.totalStatesAvailable,
                "Total states available must be 1")
        val magazineSearchResult = magazineSearchPage.states.single().state.data
        assertEquals(magazineState.title, magazineSearchResult.title)
        assertEquals(1, stateService.countBy(criteria),
                "Result of countBy must be 1")
    }


    inline fun <reified OUT> flowWorksCorrectly(node: StartedMockNode, flow: FlowLogic<OUT>): OUT {
        val result = node.startFlow(flow).getOrThrow()
        // Ask nodes to process any queued up inbound messages
        network.waitQuiescent()
        return result
    }
}
