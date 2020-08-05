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
import com.github.manosbatsis.vaultaire.example.contract.BookContract
import com.github.manosbatsis.vaultaire.example.contract.BookContract.BookState
import com.github.manosbatsis.vaultaire.example.contract.BookContract.Genre.TECHNOLOGY
import com.github.manosbatsis.vaultaire.example.contract.BookStateDto
import com.github.manosbatsis.vaultaire.example.contract.BookStateService
import com.github.manosbatsis.vaultaire.example.contract.PersistentBookStateConditions
import com.github.manosbatsis.vaultaire.example.contract.bookStateQuery
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoDto
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoService
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.accountInfoQuery
import com.github.manosbatsis.vaultaire.service.dao.BasicStateService
import com.github.manosbatsis.vaultaire.service.node.NotFoundException
import com.r3.corda.lib.accounts.contracts.AccountInfoContract
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.ci.workflows.RequestKey
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@Suppress("DEPRECATION")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // allow non-static @BeforeAll etc.
class BookMockTests {
    companion object {
        val logger = loggerFor<BookMockTests>()
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
    lateinit var b: StartedMockNode

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
    }

    @AfterAll
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `Test @DefaultValue`() {
        assertEquals(1, BookStateDto().editions)
    }

    @Test
    fun `Test AccountInfo`() {
        // Create account
        val accountUuid = UUID.randomUUID()
        val accountName = "vaultaire"
        flowWorksCorrectly(a, CreateAccount(accountName))
        flowWorksCorrectly(a, CreateAccount("foobar"))

        // Query for account
        val accountInfoService = AccountInfoService(a.services)
        val results = accountInfoService.queryBy(
                accountInfoQuery {
                    and {
                        fields.name `==` accountName
                    }
                }
        )
        // Validate query results
        assertEquals(1, results.states.size)
    }

    @Test
    fun `Test DSL conditions`() {
        val bookTitle = "A book on Corda"
        val bookState = BookContract.BookState(
                publisher = a.info.legalIdentities.first(),
                author = b.info.legalIdentities.first(),
                price = BigDecimal.valueOf(10),
                genre = BookContract.Genre.TECHNOLOGY,
                editions = 3,
                title = bookTitle)


        val state = flowWorksCorrectly(a,
                CreateBookFlow(BookStateDto(
                        author = b.info.legalIdentities.first(),
                        price = BigDecimal.valueOf(10),
                        genre = TECHNOLOGY,
                        editions = 3,
                        title = bookTitle)))
        flowWorksCorrectly(a,
                CreateBookFlow(BookStateDto(
                        author = b.info.legalIdentities.first(),
                        price = BigDecimal.valueOf(20),
                        genre = BookContract.Genre.TECHNOLOGY,
                        editions = 1,
                        title = "$bookTitle 2")))
        print("bTx == $state\n")
        // Check book state is stored in the vault.
        // Simple query.
        val bYo = b.services.vaultService.queryBy<BookContract.BookState>()
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
        val stateBasicService = BasicStateService(b.services, BookContract.BookState::class.java)
        testStateServiceQueryBy(stateBasicService, bookStateQuery, bookState)

        // Test manually coded subclass of BasicStateService
        val bookBasicStateService = CustomBasicBookStateService(b.services)
        testStateServiceQueryBy(bookBasicStateService, bookStateQuery, bookState)

        // Test generated BookStateService
        val serviceHubBookStateService = BookStateService(b.services)
        testStateServiceQueryBy(bookBasicStateService, bookStateQuery, bookState)

        // Test manually coded subclass of BookStateService
        val myBookStateService = MyExtendedBookStateService(b.services)
        testStateServiceQueryBy(myBookStateService, bookStateQuery, bookState)
    }

    @Test
    fun `Test DSL aggregates`() {
        val bookTitle = "Corda Foundation"
        val bookState = BookContract.BookState(
                publisher = a.info.legalIdentities.first(),
                author = b.info.legalIdentities.first(),
                price = BigDecimal.valueOf(8),
                genre = BookContract.Genre.SCIENCE_FICTION,
                editions = 3,
                title = bookTitle)


        val state = flowWorksCorrectly(a,
                CreateBookFlow(BookStateDto(
                        author = bookState.author,
                        price = bookState.price,
                        genre = bookState.genre,
                        editions = bookState.editions,
                        title = bookState.title)))
        flowWorksCorrectly(a,
                CreateBookFlow(BookStateDto(
                        author = bookState.author,
                        price = BigDecimal.valueOf(10),
                        genre = BookContract.Genre.SCIENCE_FICTION,
                        editions = 2,
                        title = "Forward the Corda Foundation")))
        flowWorksCorrectly(a,
                CreateBookFlow(BookStateDto(
                        author = bookState.author,
                        price = BigDecimal.valueOf(12),
                        genre = BookContract.Genre.SCIENCE_FICTION,
                        editions = 2,
                        title = "Corda Foundation and Earth")))

        print("state == $state\n")
        // Check book state is stored in the vault.
        // Simple query.
        val bYo = b.services.vaultService.queryBy<BookContract.BookState>()
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
        val stateService = BasicStateService(b.services, BookContract.BookState::class.java)

        // Ensure three matching records
        var bookSearchPage = stateService.queryBy(
                bookStateQuery.toCriteria(true), 1, 10, bookStateQuery.toSort()
        )
        assertEquals(3, bookSearchPage.totalStatesAvailable)

        // Validate aggregate results
        bookSearchPage = stateService.queryBy(
                bookStateQuery.toCriteria(false), 1, 10, bookStateQuery.toSort()
        )
        bookSearchPage.otherResults.forEachIndexed { index, element ->
            println("testStateServiceAggregates, aggregate $index: $element")
        }
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

        val extendedService = BookStateService(b.services)
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
    fun `Test get or find by id`() {

        val identifier = UniqueIdentifier(id = UUID.randomUUID(), externalId = UUID.randomUUID().toString())

        flowWorksCorrectly(a,
                CreateBookFlow(BookStateDto(
                        author = b.info.legalIdentities.first(),
                        price = BigDecimal.valueOf(82),
                        genre = BookContract.Genre.HISTORICAL,
                        editions = 24,
                        title = "Vault diaries, Volume 29",
                        linearId = identifier)))
        val stateService = BasicStateService(b.services, BookContract.BookState::class.java)

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
    fun `Test find consumed`() {
        // Create a state
        val createdState: BookState = flowWorksCorrectly(a,
                CreateBookFlow(BookStateDto(
                        author = b.info.legalIdentities.first(),
                        price = BigDecimal.valueOf(87),
                        genre = BookContract.Genre.HISTORICAL,
                        editions = 1,
                        title = "Vault diaries"))).single()
        // Update title
        val updatedTitle = "${createdState.title} UPDATED"
        val updatedState: BookState = flowWorksCorrectly(a,
                UpdateBookFlow(BookStateDto.mapToDto(createdState.copy(
                        title = updatedTitle,
                        editions = 2
                )))).single()
        assertEquals(createdState.linearId, updatedState.linearId)

        // Update title and editions
        val updatedTitle2 = "${createdState.title} UPDATED2"
        val updatedState2: BookState = flowWorksCorrectly(a,
                UpdateBookFlow(BookStateDto.mapToDto(createdState.copy(
                        title = updatedTitle2,
                        editions = 3
                )))).single()
        assertEquals(createdState.linearId, updatedState2.linearId)
        // Query for the consumed
        val extendedService = BookStateService(a.services)
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
        println("Test find consumed, criteria: ${criteria}")
        val results = extendedService.queryBy(criteria)
        println("Test find consumed, results: $results")
        println("Test find consumed, results.states: ${results.states}")
        // Get/find by external ID
        assertEquals(2, results.totalStatesAvailable.toInt())
        assertTrue(results.states.first().state.data.editions < 3)
        assertTrue(results.states[1].state.data.editions < 3)
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


    inline fun <reified OUT> flowWorksCorrectly(node: StartedMockNode, flow: FlowLogic<OUT>): OUT {
        val result = node.startFlow(flow).getOrThrow()
        // Ask nodes to process any queued up inbound messages
        network.waitQuiescent()
        return result
    }
}
