package com.github.manosbatsis.vaultaire.example.workflow

import com.github.manosbatsis.corda.testacles.mocknetwork.NodeHandles
import com.github.manosbatsis.corda.testacles.mocknetwork.config.CordaX500Names
import com.github.manosbatsis.corda.testacles.mocknetwork.config.MockNetworkConfig
import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.example.contract.BOOK_CONTRACT_PACKAGE
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoStateClientDto
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoStateDto
import com.r3.corda.lib.accounts.contracts.AccountInfoContract
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.ci.workflows.RequestKey
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters

fun NodeHandles.getAccount(accountName: String, nodeName: CordaX500Name): AccountInfo {
    val node = getNode(nodeName)
    val accountsFuture = node.startFlow(AccountInfoByName(accountName))
        .getOrThrow()
    return accountsFuture.firstOrNull()?.state?.data
        ?: node.startFlow(CreateAccount(accountName))
            .getOrThrow().state.data
}

fun NodeHandles.getAccountDto(accountName: String, nodeName: CordaX500Name): AccountInfoStateClientDto {
    return getAccount(accountName, nodeName).let { AccountInfoStateClientDto.from(it) }
}

object TestConfig{
    fun mockNetworkConfig(vararg nodes: CordaX500Name) = MockNetworkConfig(
        // The nodes to build, one of
        // names: List<MockNodeParameters>, CordaX500Names, OrgNames or
        // numberOfNodes: Int
        names = CordaX500Names(nodes.toList()),
        // Optional, used *only* for the current
        // Gradle module, if a cordapp.
        cordappProjectPackage = CustomBasicBookStateService::class.java.`package`.name,
        // Optional; package names are used to pickup
        // cordapp or cordaCompile dependencies
        cordappPackages = listOf(
            // Acounts
            AccountInfoContract::class.java.`package`.name,
            RequestKeyForAccount::class.java.`package`.name,
            RequestKey::class.java.`package`.name,
            // Vaultaire
            AccountParty::class.java.`package`.name,
            AccountInfoStateDto::class.java.`package`.name,
            // Partiture
            PartitureFlow::class.java.`package`.name,
            // Our coprdapp
            BOOK_CONTRACT_PACKAGE,
            this.javaClass.`package`.name
        ),
        // Optional, default
        threadPerNode = true,
        // Optional, default
        networkParameters = testNetworkParameters(
            minimumPlatformVersion = 10
        ),
        clearEnv = true
    )
}