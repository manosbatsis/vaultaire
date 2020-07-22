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

import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoService
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.accountInfoQuery
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.utilities.detailedLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@TestInstance(TestInstance.Lifecycle.PER_CLASS) // allow non-static @BeforeAll etc.
class NodeDriverTests: SimpleDriverNodes() {
    companion object{
        val logger = detailedLogger()
    }


    //@BeforeAll
    fun prepare(){
    }

    

    @Test
    fun `Test node driver`(){
        withDriverNodes {
            val createdAccounts = mutableMapOf<String, List<AccountInfo>>()
            nodeHandles().entries.forEachIndexed{ index, entry ->
                logger.info("Loaded node ${entry.key},  handle: ${entry.value}")

                val handle = entry.value

                val accountInfoService = AccountInfoService(handle.rpc)
                val hostAccounts = mutableListOf<AccountInfo>()
                for (userInndex in 0..2) {
                    val accountInfo = accountInfoService.createAccount("Account${userInndex}_${entry.key}")
                            .state.data
                    hostAccounts.add(accountInfo)
                    logger.info("Created account ${accountInfo.name}, "
                            + "linearId: ${accountInfo.linearId}"
                            + "identifier: ${accountInfo.identifier}")
                }
                createdAccounts.put(entry.key, hostAccounts)

            }
            val nodeA = nodeHandles("partyA") ?: error("Couldn't match node handle")
            val nodeB = nodeHandles("partyB") ?: error("Couldn't match node handle")
            val nodeAService = AccountInfoService(nodeA.rpc)
            createdAccounts.mapKeys {

                logger.info("Find account in  ${it.key}")
                val targetAccount = it.value.first()
                val foundAccount = nodeAService
                        .findAccountOrNull(targetAccount.identifier.id, targetAccount.host.name)
                assertNotNull(foundAccount)
            }

            // Query for account
            val results = nodeAService.queryBy(
                    accountInfoQuery {
                        and {
                            fields.name `==` "Account1_partyA"
                        }
                    }
            )
            // Validate query results
            assertEquals(1, results.states.size)
            assertEquals(createdAccounts["partyA"]?.size?.toLong(),
                    nodeAService.findHostedAccounts().totalStatesAvailable)
        }
    }

}
