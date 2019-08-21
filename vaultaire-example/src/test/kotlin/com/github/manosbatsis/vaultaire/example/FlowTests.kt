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

import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.vaultaire.dao.BasicStateService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
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
import kotlin.test.assertEquals


@Suppress("DEPRECATION")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // allow non-static @BeforeAll etc.
class FlowTests {

    // Works as long as the main and test package names are  in sync
    val cordappPackages = listOf(BOOK_CONTRACT_PACKAGE, this.javaClass.`package`.name, "com.github.manosbatsis.partiture.flow")
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @BeforeAll
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, true)),
                cordappsForAllNodes = cordappPackages.map {
                    findCordapp(it)
                }))

        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @AfterAll
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `Test generated conditions DSL `() {
        val bookTitle = "A bok on Corda"
        val bookState = BookContract.BookState(
                publisher = a.info.legalIdentities.first(),
                author = b.info.legalIdentities.first(),
                title = bookTitle)

        val stx = flowWorksCorrectly(
                CreateBookFlow(BookMessage(
                        author = b.info.legalIdentities.first(),
                        title = bookTitle)))

        // Check book transaction is stored in the storage service.
        val bTx = b.services.validatedTransactions.getTransaction(stx.id)
        assertEquals(bTx, stx)
        print("bTx == $stx\n")
        // Check book state is stored in the vault.
        b.transaction {
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
                        fields.title `!=` "false match"
                        //fields.title notEqual bookTitle
                        fields["title"] _notEqual bookTitle
                    }
                }
                orderBy {
                    fields.title sort ASC
                }
            }


            // Test BasicStateService
            val stateBasicService = BasicStateService(b.services, BookContract.BookState::class.java)
            testStateService(stateBasicService, bookStateQuery, bookState)

            // Test manually coded subclass of BasicStateService
            val bookBasicStateService = CustomBasicBookStateService(b.services)
            testStateService(bookBasicStateService, bookStateQuery, bookState)

            // Test generated BookStateService
            val serviceHubBookStateService = BookStateService(b.services)
            testStateService(bookBasicStateService, bookStateQuery, bookState)

            // Test manually coded subclass of BookStateService
            val myBookStateService = MyExtendedBookStateService(b.services)
            testStateService(myBookStateService, bookStateQuery, bookState)

        }
    }

    private fun testStateService(stateService: BasicStateService<BookContract.BookState>, bookStateQuery: PersistentBookStateConditions, bookState: BookContract.BookState) {
        var bookSearchResult = stateService.queryBy(
                bookStateQuery.toCriteria(), 1, 1, bookStateQuery.toSort()
        ).states.single().state.data
        assertEquals(bookState.title, bookSearchResult.title)
        assertEquals(1, stateService.countBy(bookStateQuery.toCriteria()))
        print("$bookSearchResult == $bookState\n")
    }


    inline fun <reified OUT> flowWorksCorrectly(flow: PartitureFlow<*, OUT>): OUT {
        val future = a.startFlow(flow)
        // Ask nodes to process any queued up inbound messages
        network.runNetwork()
        return  future.getOrThrow()

    }
}
