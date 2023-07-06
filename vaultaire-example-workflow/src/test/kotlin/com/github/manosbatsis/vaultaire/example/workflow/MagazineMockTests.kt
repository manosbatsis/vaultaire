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

import com.github.manosbatsis.corda.testacles.mocknetwork.NodeHandles
import com.github.manosbatsis.corda.testacles.mocknetwork.config.MockNetworkConfig
import com.github.manosbatsis.corda.testacles.mocknetwork.jupiter.MockNetworkExtension
import com.github.manosbatsis.corda.testacles.mocknetwork.jupiter.MockNetworkExtensionConfig
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineGenre
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineGenre.*
import com.github.manosbatsis.vaultaire.example.contract.MagazineState
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoStateClientDto
import com.github.manosbatsis.vaultaire.service.dao.BasicStateService
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manosbatsis.vaultaire.service.node.NotFoundException
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.Vault.RelevancyStatus.RELEVANT
import net.corda.core.node.services.Vault.StateStatus.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockNetworkExtension::class)
class MagazineMockTests {

    companion object {
        val logger = loggerFor<MagazineMockTests>()

        // Marks the field as config for the MockNetworkExtension
        @MockNetworkExtensionConfig
        @JvmStatic
        val mockNetworkConfig: MockNetworkConfig = TestConfig.mockNetworkConfig(
            ALICE_NAME, BOB_NAME
        )
    }



    @Test
    fun `Test @DefaultValue`() {
        assertEquals(1, MagazineStateDto().issues)
    }


    @Test
    fun `Test DSL conditions`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!
        
        val aPublisherDto = nodeHandles.getAccountDto("aPublisher", ALICE_NAME) 
        val bAuthorDto = nodeHandles.getAccountDto("bAuthor", BOB_NAME)
        val magazineTitle = "Test DSL conditions ${UUID.randomUUID()}"

        val magazineState = nodeA.startFlow(
                CreateMagazineFlow(MagazineStateClientDto(
                        publisher = aPublisherDto,//AccountService.toAccountParty(aPublisherDto),
                        author = bAuthorDto,
                        price = BigDecimal.valueOf(10),
                        published = Date(),
                        genre = MagazineContract.MagazineGenre.TECHNOLOGY,
                        issues = 3,
                        title = magazineTitle))).getOrThrow().single()

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
        val stateBasicService = BasicStateService(nodeA.services, MagazineState::class.java)
        testStateServiceQueryByForSingleResult(stateBasicService, criteria, sort, magazineState)

        // Test generated MagazineStateService
        val serviceHubMagazineStateService = MagazineStateService(nodeB.services)
        testStateServiceQueryByForSingleResult(stateBasicService, criteria, sort, magazineState)

    }


    @Test
    fun `Test conversions for DTOs and Views`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!

        val aPublisherDto = nodeHandles.getAccountDto("aPublisher", ALICE_NAME) 
        val bAuthorDto = nodeHandles.getAccountDto("bAuthor", BOB_NAME)

        val stateService = MagazineStateService(nodeB.services)

        // Test DTO > State > DTO...
        val fullDto = MagazineStateClientDto(
                publisher = aPublisherDto,
                author = bAuthorDto,
                price = BigDecimal.valueOf(82),
                genre = HISTORICAL,
                issues = 24,
                title = "Vault diaries, Volume 29")
        // ... with full AccountInfoStateClientDtos
        val contractState = fullDto.toTargetType(stateService)
        assertEquals(fullDto, MagazineStateClientDto.from(contractState, stateService))
        val magazine1 = nodeA.startFlow(CreateMagazineFlow(fullDto)).getOrThrow().single()
        assertEquals(fullDto, MagazineStateClientDto.from(magazine1, stateService))
        // ... with id+host in AccountInfoStateClientDtos
        val fullDto2 = fullDto.copy(
                publisher = AccountInfoStateClientDto(
                        identifier = aPublisherDto.identifier,
                        host = aPublisherDto.host
                )
        )
        val contractState2 = fullDto2.toTargetType(stateService)
        assertEquals(fullDto, MagazineStateClientDto.from(contractState2, stateService))
        val magazine2 = nodeA.startFlow(CreateMagazineFlow(fullDto2)).getOrThrow().single()
        assertEquals(fullDto, MagazineStateClientDto.from(magazine2, stateService))

        // ... with name+host in AccountInfoStateClientDtos
        val fullDto3 = fullDto.copy(
                publisher = AccountInfoStateClientDto(
                        name = aPublisherDto.name,
                        host = aPublisherDto.host
                )
        )
        val contractState3 = fullDto3.toTargetType(stateService)
        // java.lang.AssertionError: Expected
        // <MagazineStateClientDto(
        // publisher=AccountInfoStateClientDto(name=aPublisher, host=O=Mock Company 1, L=London, C=GB, identifier=3dee8eb3-7b5a-44ba-9405-60e35b866018, externalId=null), author=AccountInfoStateClientDto(name=bAuthor, host=O=Mock Company 2, L=London, C=GB, identifier=08aa9d70-bada-433b-ab01-e47e038bdeac, externalId=null), price=82, genre=HISTORICAL, issues=24, title=Vault diaries, Volume 29, published=Mon Aug 09 05:28:16 EEST 2021, linearId=5e0f8046-bc66-477e-b69d-10a9b9633895, customMixinField=null)>,
        // publisher=null, author=AccountInfoStateClientDto(name=bAuthor, host=O=Mock Company 2, L=London, C=GB, identifier=08aa9d70-bada-433b-ab01-e47e038bdeac, externalId=null), price=82, genre=HISTORICAL, issues=24, title=Vault diaries, Volume 29, published=Mon Aug 09 05:28:16 EEST 2021, linearId=5e0f8046-bc66-477e-b69d-10a9b9633895, customMixinField=null)>.
        assertEquals(fullDto, MagazineStateClientDto.from(contractState3, stateService))
        val magazine3 = nodeA.startFlow(CreateMagazineFlow(fullDto3)).getOrThrow().single()
        assertEquals(fullDto, MagazineStateClientDto.from(magazine3, stateService))

        // Test DTO > View > DTO > State
        val updatedIssues = contractState.issues + 1
        val updatedPublished = Date()
        val addIssueView = MagazineStateAddIssueView.from(fullDto)
        assertEquals(contractState.issues, addIssueView.issues)
        assertEquals(contractState.published, addIssueView.published)

        addIssueView.issues = updatedIssues
        addIssueView.published = updatedPublished
        val patchedFullDto = addIssueView.toPatched(fullDto)
        assertEquals(patchedFullDto.issues, updatedIssues)
        assertEquals(patchedFullDto.published, updatedPublished)

        val toTargetFullDto = addIssueView.toTargetType()
        assertEquals(toTargetFullDto.issues, updatedIssues)
        assertEquals(toTargetFullDto.published, updatedPublished)

        val patchedContractState = patchedFullDto.toPatched(contractState, stateService)
        assertEquals(patchedContractState.issues, updatedIssues)
        assertEquals(patchedContractState.published, updatedPublished)

    }

    @Test
    fun `Test get or find by id`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!

        val aPublisherDto = nodeHandles.getAccountDto("aPublisher", ALICE_NAME) 
        val bAuthorDto = nodeHandles.getAccountDto("bAuthor", BOB_NAME)

        val identifier = UniqueIdentifier(id = UUID.randomUUID(), externalId = UUID.randomUUID().toString())

        val magazine = nodeA.startFlow(
                CreateMagazineFlow(MagazineStateClientDto(
                        publisher = aPublisherDto,
                        author = bAuthorDto,
                        price = BigDecimal.valueOf(82),
                        genre = MagazineContract.MagazineGenre.HISTORICAL,
                        issues = 24,
                        title = "Vault diaries, Volume 29",
                        linearId = identifier))).getOrThrow().single()
        logger.info("Expected identifier: $identifier, actual: ${magazine.linearId}")
        assertEquals(identifier.id, magazine.linearId.id)
        assertEquals(identifier.externalId, magazine.linearId.externalId)
        assertEquals(aPublisherDto.identifier!!, magazine.publisher!!.identifier)
        assertEquals(bAuthorDto.identifier!!, magazine.author!!.identifier)
        mapOf(bAuthorDto to nodeB.services, aPublisherDto to nodeA.services).forEach {
            val accountInfoDto = it.key
            val serviceHub = it.value
            val stateService = MagazineStateService(serviceHub)
            val manualQueryResult = stateService.queryBy(
                    contractStateType = MagazineState::class.java,
                    criteria = QueryCriteria.VaultQueryCriteria(
                            externalIds = listOf(accountInfoDto.identifier!!),
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
    fun `Test find with state status cases`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!

        val aAuthorDto = nodeHandles.getAccountDto("aAuthor", ALICE_NAME) 
        val bPublisherDto = nodeHandles.getAccountDto("bPublisherDto", BOB_NAME)

        logger.info("aAuthorDto: $aAuthorDto")
        logger.info("bPublisherDto: $bPublisherDto")
        val stateService = MagazineStateService(nodeA.services)


        logger.info("Author creates draft...")
        // Author creates draft
        logger.info("Step 1 DTO")
        val magazineDto = MagazineStateClientDto(
                publisher = bPublisherDto,
                author = aAuthorDto,
                price = BigDecimal.valueOf(87),
                genre = MagazineContract.MagazineGenre.HISTORICAL,
                issues = 1,
                title = "Vault diaries")
        logger.info("Step 2 Flow")
        val createMagazineFlow = CreateMagazineFlow(magazineDto)
        logger.info("Step 3 Start Flow")
        val createdStates: List<MagazineState> = nodeA.startFlow(createMagazineFlow).getOrThrow()

        assertTrue(createdStates.isNotEmpty())
        val createdState = createdStates.first()
        assertNotNull(createdState)

        logger.info("Author created draft")
        // Update title
        logger.info("Author updates title and issues...")
        val updatedTitle = "${createdState.title} UPDATED"
        var updatedState: MagazineState = nodeA.startFlow(
                UpdateMagazineFlow(MagazineStateClientDto.from(
                        createdState.copy(
                                title = updatedTitle,
                                issues = 2),
                        stateService))).getOrThrow().single()
        assertEquals(createdState.linearId, updatedState.linearId)

        // Update
        logger.info("Author updates title and issues again...")
        val updatedTitle2 = "${createdState.title} UPDATED2"
        val updatedState2: MagazineState = nodeA.startFlow(
                UpdateMagazineFlow(MagazineStateClientDto.from(createdState.copy(
                        title = updatedTitle2,
                        issues = 3
                ), stateService))).getOrThrow().single()
        assertEquals(createdState.linearId, updatedState2.linearId)
        //
        logger.info("Publisher side: " +
                "Query for the consumed on the counter party node...")
        testFindWithStatusesAndService(MagazineStateService(nodeA.services), bPublisherDto, createdState)
        testFindWithStatusesAndService(MagazineStateService(nodeB.services), bPublisherDto, createdState)


    }

    private fun testFindWithStatusesAndService(extendedService: MagazineStateService, bPublisherDto: AccountInfoStateClientDto, createdState: MagazineState) {
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
                fields.authorName `==` createdState.author.name
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
        val results = extendedService.queryBy(criteria, 1, 10)
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
                fields.authorName `==` createdState.author.name
                fields.issues `in` listOf(1, 2, 3)
                fields.title `like` "%${createdState.title}%"
            }
        }

        val unconsumedResults = extendedService.queryBy(unconsumedCriteria.toCriteria(), 1, 10)

        assertEquals(1, unconsumedResults.totalStatesAvailable.toInt())
        assertTrue(unconsumedResults.states.single().state.data.issues == 3)

        logger.info("testFindWithStatusesAndService default")
        val defaultCriteria = extendedService.buildQuery {
            externalIds = listOfNotNull(bPublisherDto.identifier)
            and {
                fields.price `==` createdState.price
                fields.genre `==` createdState.genre
                fields.authorName `==` createdState.author.name
                fields.issues `in` listOf(1, 2, 3)
                fields.title `like` "%${createdState.title}%"
            }
        }

        val defaultResults = extendedService.queryBy(defaultCriteria.toCriteria(), 1, 10)

        assertEquals(1, defaultResults.totalStatesAvailable.toInt())
        assertTrue(defaultResults.states.single().state.data.issues == 3)

        logger.info("testFindWithStatusesAndService all")
        val allCriteria = extendedService.buildQuery {
            status = ALL
            externalIds = listOfNotNull(bPublisherDto.identifier)
            and {
                fields.price `==` createdState.price
                fields.genre `==` createdState.genre
                fields.authorName `==` createdState.author.name
                fields.issues `in` listOf(1, 2, 3)
                fields.title `like` "%${createdState.title}%"
            }
        }

        val allResults = extendedService.queryBy(allCriteria.toCriteria(), 1, 10)

        assertEquals(3, allResults.totalStatesAvailable.toInt())
    }

    private fun testStateServiceQueryByForSingleResult(
            stateService: StateService<MagazineState>,
            criteria: QueryCriteria,
            sort: Sort,
            magazineState: MagazineState) {

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
}
