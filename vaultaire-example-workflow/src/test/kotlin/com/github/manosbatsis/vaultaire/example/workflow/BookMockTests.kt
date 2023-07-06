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
import com.github.manosbatsis.vaultaire.example.contract.*
import com.github.manosbatsis.vaultaire.example.contract.BookContract.BookState
import com.github.manosbatsis.vaultaire.example.contract.BookContract.BookState.BookSchemaV1.PersistentBookState
import com.github.manosbatsis.vaultaire.example.contract.BookContract.Genre.TECHNOLOGY
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoService
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.accountInfoQuery
import com.github.manosbatsis.vaultaire.plugin.rsql.support.SimpleRsqlArgumentsConverter
import com.github.manosbatsis.vaultaire.plugin.rsql.withRsql
import com.github.manosbatsis.vaultaire.service.dao.BasicStateService
import com.github.manosbatsis.vaultaire.service.node.NotFoundException
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.node.StartedMockNode
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@ExtendWith(MockNetworkExtension::class)
class BookMockTests {
    companion object {
        val logger = loggerFor<BookMockTests>()

        // Marks the field as config for the MockNetworkExtension
        @MockNetworkExtensionConfig
        @JvmStatic
        val mockNetworkConfig: MockNetworkConfig = TestConfig.mockNetworkConfig(
            ALICE_NAME, BOB_NAME
        )
    }

    @Test
    fun `Test @DefaultValue`() {
        assertEquals(1, BookStateDto().editions)
    }

    @Test
    fun `Test AccountInfo`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!
        // Create account
        val accountUuid = UUID.randomUUID()
        val accountName = "vaultaire"
        nodeA.startFlow(CreateAccount(accountName)).getOrThrow()
        nodeA.startFlow(CreateAccount("foobar")).getOrThrow()

        // Query for account
        val accountInfoService = AccountInfoService(nodeA.services)
        val results = accountInfoService.queryBy(
                accountInfoQuery {
                    and {
                        fields.name `==` accountName
                    }
                }.toCriteria()
        )
        // Validate query results
        assertEquals(1, results.states.size)
    }

    @Test
    fun `Test DSL conditions`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!
        val bookTitle = "A book on Corda"
        val bookState = BookContract.BookState(
                publisher = nodeA.info.legalIdentities.first(),
                author = nodeB.info.legalIdentities.first(),
                price = BigDecimal.valueOf(10),
                genre = BookContract.Genre.TECHNOLOGY,
                editions = 3,
                title = bookTitle)


        val state = nodeA.startFlow(
                CreateBookFlow(BookStateDto(
                        author = nodeB.info.legalIdentities.first(),
                        price = BigDecimal.valueOf(10),
                        genre = TECHNOLOGY,
                        editions = 3,
                        title = bookTitle))).getOrThrow()
        nodeA.startFlow(
                CreateBookFlow(BookStateDto(
                        author = nodeB.info.legalIdentities.first(),
                        price = BigDecimal.valueOf(20),
                        genre = BookContract.Genre.TECHNOLOGY,
                        editions = 1,
                        title = "$bookTitle 2"))).getOrThrow()
        print("bTx == $state\n")
        // Check book state is stored in the vault.
        // Simple query.
        val bYo = nodeB.services.vaultService.queryBy<BookContract.BookState>()
                .states.map { it.state.data }.single { it.title == bookTitle }

        // Verify record
        assertEquals(bYo.publisher, bookState.publisher)
        assertEquals(bYo.author, bookState.author)
        assertEquals(bYo.title, bookState.title)


        // Use the generated DSL to create query criteria
        val bookStateQuery = bookStateQuery {
            status = Vault.StateStatus.UNCONSUMED // the default
            relevancyStatus = Vault.RelevancyStatus.ALL // the default
            and {
                fields.title `==` bookTitle
                or {
                    fields.title notEqual bookTitle
                    fields["title"] _notEqual bookTitle
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

        // Test BasicStateService
        val stateBasicService = BasicStateService(nodeB.services, BookContract.BookState::class.java)
        testStateServiceQueryBy(stateBasicService, bookStateQuery, bookState)

        // Test manually coded subclass of BasicStateService
        val bookBasicStateService = CustomBasicBookStateService(nodeB.services)
        testStateServiceQueryBy(bookBasicStateService, bookStateQuery, bookState)

        // Test generated BookStateService
        val serviceHubBookStateService = BookStateService(nodeB.services)
        testStateServiceQueryBy(bookBasicStateService, bookStateQuery, bookState)

        // Test manually coded subclass of BookStateService
        val myBookStateService = MyExtendedBookStateService(nodeB.services)
        testStateServiceQueryBy(myBookStateService, bookStateQuery, bookState)
    }

    @Test
    fun `Test DSL aggregates`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!
        val bookTitle = "Corda Foundation"
        val bookState = BookContract.BookState(
                publisher = nodeA.info.legalIdentities.first(),
                author = nodeB.info.legalIdentities.first(),
                price = BigDecimal.valueOf(8),
                genre = BookContract.Genre.SCIENCE_FICTION,
                editions = 3,
                title = bookTitle)


        val state = nodeA.startFlow(
                CreateBookFlow(BookStateDto(
                        author = bookState.author,
                        price = bookState.price,
                        genre = bookState.genre,
                        editions = bookState.editions,
                        title = bookState.title))).getOrThrow()
        nodeA.startFlow(
                CreateBookFlow(BookStateDto(
                        author = bookState.author,
                        price = BigDecimal.valueOf(10),
                        genre = BookContract.Genre.SCIENCE_FICTION,
                        editions = 2,
                        title = "Forward the Corda Foundation"))).getOrThrow()
        nodeA.startFlow(
                CreateBookFlow(BookStateDto(
                        author = bookState.author,
                        price = BigDecimal.valueOf(12),
                        genre = BookContract.Genre.SCIENCE_FICTION,
                        editions = 2,
                        title = "Corda Foundation and Earth"))).getOrThrow()

        print("state == $state\n")
        // Check book state is stored in the vault.
        // Simple query.
        val bYo = nodeB.services.vaultService.queryBy<BookContract.BookState>()
                .states.map { it.state.data }.single { it.title == bookTitle }

        // Verify record
        assertEquals(bYo.publisher, bookState.publisher)
        assertEquals(bYo.author, bookState.author)
        assertEquals(bYo.title, bookState.title)


        // Use the generated DSL to create query criteria
        val bookStateQuery = bookStateQuery {
            status = Vault.StateStatus.UNCONSUMED // the default
            relevancyStatus = Vault.RelevancyStatus.ALL // the default
            and {
                fields.title `like` "%Corda Foundation%"
                fields.genre `==` BookContract.Genre.SCIENCE_FICTION
            }
            aggregate {
                // add some aggregates
                fields.externalId.count()
                fields.id.count()
                fields.editions.sum()
                fields.price.min()
                fields.price.avg()
                fields.price.max()
            }
        }

        // Test BasicStateService
        val stateService = BasicStateService(nodeB.services, BookContract.BookState::class.java)

        // Ensure three matching records
        var bookSearchPage = stateService.queryBy(
                bookStateQuery.toCriteria(true), 1, 10, bookStateQuery.toSort()
        )
        assertEquals(3, bookSearchPage.totalStatesAvailable)

        // Validate aggregate results
        bookSearchPage = stateService.queryBy(
                bookStateQuery.toCriteria(false), 1, 10, bookStateQuery.toSort()
        )
        // Must be five aggregates
        assertEquals(6, bookSearchPage.otherResults.size)
        // Must zero external IDs
        assertEquals(0.toLong(), bookSearchPage.otherResults[0])
        // Must three linear IDs
        assertEquals(3.toLong(), bookSearchPage.otherResults[1])
        // Must be seven editions
        assertEquals(7.toLong(), bookSearchPage.otherResults[2])
        // Minimum price must be eight
        assertEquals(0, BigDecimal(8).compareTo(bookSearchPage.otherResults[3] as BigDecimal))
        // Average price must be ten
        assertEquals(0, BigDecimal.TEN.compareTo(BigDecimal(bookSearchPage.otherResults[4] as Double)))
        // Minimum price must be 12
        assertEquals(0, BigDecimal(12).compareTo(bookSearchPage.otherResults[5] as BigDecimal))

        val extendedService = BookStateService(nodeB.services)
        val querySpec = extendedService.buildQuery {
            status = Vault.StateStatus.UNCONSUMED // the default
            relevancyStatus = Vault.RelevancyStatus.ALL // the default
            timeRecorded gt Instant.now().minus(1, ChronoUnit.DAYS)
            and {
                fields.title `like` "%Corda Foundation%"
                fields.genre `==` BookContract.Genre.SCIENCE_FICTION
            }
            orderBy {
                // Sort by standard attribute alias, same as
                // Sort.VaultStateAttribute.RECORDED_TIME sort ASC
                recordedTime sort ASC
                // Sort by custom field
                fields.title sort DESC
            }
        }
        extendedService.queryBy(querySpec, 1, 1)
    }

    @Test
    fun `Test get or find by id`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!

        val identifier = UniqueIdentifier(id = UUID.randomUUID(), externalId = UUID.randomUUID().toString())
        val bookState = nodeA.startFlow(
                CreateBookFlow(BookStateDto(
                        author = nodeB.info.legalIdentities.first(),
                        price = BigDecimal.valueOf(82),
                        genre = BookContract.Genre.HISTORICAL,
                        editions = 24,
                        title = "Vault diaries, Volume 29",
                        linearId = identifier))).getOrThrow()

        assertEquals(identifier, bookState.single().linearId)

        val stateService = BasicStateService(nodeB.services, BookContract.BookState::class.java)
        TimeUnit.SECONDS.sleep(2L)
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

    @Test
    fun `Test find consumed`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!
        // Create a state
        val createdState: BookState = nodeA.startFlow(
                CreateBookFlow(BookStateDto(
                        author = nodeB.info.legalIdentities.first(),
                        price = BigDecimal.valueOf(87),
                        genre = BookContract.Genre.HISTORICAL,
                        editions = 1,
                        title = "Vault diaries"))).getOrThrow().single()
        // Update title
        val updatedTitle = "${createdState.title} UPDATED"
        val updatedState: BookState = nodeA.startFlow(
                UpdateBookFlow(BookStateDto.from(createdState.copy(
                        title = updatedTitle,
                        editions = 2
                )))).getOrThrow().single()
        assertEquals(createdState.linearId, updatedState.linearId)

        // Update title and editions
        val updatedTitle2 = "${createdState.title} UPDATED2"
        val updatedState2: BookState = nodeA.startFlow(
                UpdateBookFlow(BookStateDto.from(createdState.copy(
                        title = updatedTitle2,
                        editions = 3
                )))).getOrThrow().single()
        assertEquals(createdState.linearId, updatedState2.linearId)
        // Query for the consumed
        val extendedService = BookStateService(nodeA.services)
        val querySpec = extendedService.buildQuery {
            status = Vault.StateStatus.CONSUMED
            and {
                fields.price `==` createdState.price
                fields.genre `==` createdState.genre
                fields.author `==` updatedState.author.name.toString()
                or {
                    fields.editions `==` createdState.editions
                    fields.editions `==` updatedState.editions
                }
                fields.title `like` "%${createdState.title}%"
            }
        }
        val criteria = querySpec.toCriteria()
        val results = extendedService.queryBy(criteria, 1, 10)
        // Get/find by external ID
        assertEquals(2, results.totalStatesAvailable.toInt())
        assertTrue(results.states.first().state.data.editions < 3)
        assertTrue(results.states[1].state.data.editions < 3)
    }

    @Test
    fun `Test RSQL`(nodeHandles: NodeHandles) {
        val nodeA = nodeHandles[ALICE_NAME]!!
        val nodeB = nodeHandles[BOB_NAME]!!
        // Create a state
        val createdState1: BookState = nodeA.startFlow(
            CreateBookFlow(BookStateDto(
                author = nodeB.info.legalIdentities.first(),
                price = BigDecimal.valueOf(70),
                genre = BookContract.Genre.HISTORICAL,
                editions = 1,
                title = "RSQL1",
                alternativeTitle = "RSQL One"))).getOrThrow().single()
        val createdState2: BookState = nodeA.startFlow(
            CreateBookFlow(BookStateDto(
                author = nodeB.info.legalIdentities.first(),
                price = BigDecimal.valueOf(80),
                genre = BookContract.Genre.HISTORICAL,
                editions = 1,
                title = "RSQL2",
                alternativeTitle = "RSQL Two"))).getOrThrow().single()
        val createdState3: BookState = nodeA.startFlow(
            CreateBookFlow(BookStateDto(
                author = nodeB.info.legalIdentities.first(),
                price = BigDecimal.valueOf(90),
                genre = BookContract.Genre.HISTORICAL,
                editions = 1,
                title = "RSQL3"))).getOrThrow().single()

        val extendedService = BookStateService(nodeA.services)

        val converterFactory = SimpleRsqlArgumentsConverter
            .Factory<PersistentBookState, PersistentBookStateFields>()

        // Test (not) equals
        extendedService.buildQuery {}
            .withRsql("title==RSQL1;title!=RSQL2", converterFactory)
            .toCriteria()
            .also {
                assertEquals(1, extendedService.queryBy(it, 1, 10)
                    .totalStatesAvailable.toInt())
            }
        // Test (not) lile
        extendedService.buildQuery {}
            .withRsql("title=like=RSQL*;title=unlike=RSQL2*", converterFactory)
            .toCriteria()
            .also {
                assertEquals(2, extendedService.queryBy(it, 1, 10)
                    .totalStatesAvailable.toInt())
            }
        // Test greater/less than
        extendedService.buildQuery {}
            .withRsql("title=like=RSQL*;price>70;price<=90", converterFactory)
            .toCriteria()
            .also {
                assertEquals(2, extendedService.queryBy(it, 1, 10)
                    .totalStatesAvailable.toInt())
            }
        // Test null
        extendedService.buildQuery {}
            .withRsql("title=like=RSQL*;alternativeTitle=null=true", converterFactory)
            .toCriteria()
            .also {
                assertEquals(1, extendedService.queryBy(it, 1, 10)
                    .totalStatesAvailable.toInt())
            }
        // Test not null
        extendedService.buildQuery {}
            .withRsql("title=like=RSQL*;alternativeTitle=null=false", converterFactory)
            .toCriteria()
            .also {
                assertEquals(2, extendedService.queryBy(it, 1, 10)
                    .totalStatesAvailable.toInt())
            }
        // Test (not) in
        extendedService.buildQuery {}
            .withRsql("price=in=(70,80,90);price=out=(20,80)", converterFactory)
            .toCriteria()
            .also {
                assertEquals(2, extendedService.queryBy(it, 1, 10)
                    .totalStatesAvailable.toInt())
            }
    }

    private fun testStateServiceQueryBy(stateService: BasicStateService<BookContract.BookState>, bookStateQuery: PersistentBookStateConditions, bookState: BookContract.BookState) {
        val bookSearchPage = stateService.queryBy(
                bookStateQuery.toCriteria(), 1, 10, bookStateQuery.toSort()
        )
        val bookSearchResult = bookSearchPage.states.single().state.data
        assertEquals(bookState.title, bookSearchResult.title)
        assertEquals(1, stateService.countBy(bookStateQuery.toCriteria()))
        print("$bookSearchResult == $bookState\n")
    }
}
