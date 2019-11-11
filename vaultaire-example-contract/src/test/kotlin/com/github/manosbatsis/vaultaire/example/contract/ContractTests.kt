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

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test
import java.math.BigDecimal


class BookContractTests {
    // Works as long as the main and test package names are  in sync
    val cordappPackages = listOf(this.javaClass.`package`.name)
    private val ledgerServices = MockServices(cordappPackages)
    private val alice = TestIdentity(CordaX500Name("Alice", "New York", "US"))
    private val bob = TestIdentity(CordaX500Name("Bob", "Tokyo", "JP"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    @Test
    fun bookTransactionMustBeWellFormed() {
        val bookState = BookContract.BookState(alice.party, bob.party, BigDecimal.TEN, BookContract.Genre.TECHNOLOGY)
        // Tests.
        ledgerServices.ledger {
            // Input state present.
            transaction {
                input(BOOK_CONTRACT_ID, bookState)
                command(alice.publicKey, BookContract.Send())
                output(BOOK_CONTRACT_ID, bookState)
                this.failsWith("There can be no inputs when creating books.")
            }
            // Wrong command.
            transaction {
                output(BOOK_CONTRACT_ID, bookState)
                command(alice.publicKey, DummyCommandData)
                this.failsWith("")
            }
            // Command signed by wrong key.
            transaction {
                output(BOOK_CONTRACT_ID, bookState)
                command(miniCorp.publicKey, BookContract.Send())
                this.failsWith("The book must be signed by the publisher.")
            }
            // Sending to yourself is not allowed.
            transaction {
                output(BOOK_CONTRACT_ID, BookContract.BookState(alice.party, alice.party, BigDecimal.TEN, BookContract.Genre.TECHNOLOGY))
                command(alice.publicKey, BookContract.Send())
                this.failsWith("Cannot publish your own book!")
            }
            transaction {
                output(BOOK_CONTRACT_ID, bookState)
                command(alice.publicKey, BookContract.Send())
                this.verifies()
            }
        }
    }
}
