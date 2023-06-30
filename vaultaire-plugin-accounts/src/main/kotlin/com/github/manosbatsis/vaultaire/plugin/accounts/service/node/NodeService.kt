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
package com.github.manosbatsis.vaultaire.plugin.accounts.service.node

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.corda.rpc.poolboy.PoolBoyConnection
import com.github.manosbatsis.corda.rpc.poolboy.connection.NodeRpcConnection
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoStateClientDto
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoStateDto
import com.github.manosbatsis.vaultaire.service.node.BasicNodeService
import com.github.manosbatsis.vaultaire.service.node.NodeService
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub
import java.security.PublicKey
import java.util.*


/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
interface AccountsAwareNodeService : NodeService, AccountsAwareNodeServiceDelegate {

    @Suspendable
    fun toAccountInfoDtoOrNull(
            accountIdAndParty: AccountParty?
    ): AccountInfoStateDto? {
        return if (accountIdAndParty != null) {
            val accountInfo = findStoredAccountOrNull(accountIdAndParty.identifier)
            if (accountInfo != null) with(accountInfo!!.state!!.data) {
                AccountInfoStateDto(
                        name = name,
                        host = host,
                        identifier = identifier)
            }
            else null
        } else null
    }

    @Suspendable
    fun toAccountInfoClientDtoOrNull(
            accountIdAndParty: AccountParty?
    ): AccountInfoStateClientDto? {
        return if (accountIdAndParty != null) {
            val accountInfo = findStoredAccountOrNull(accountIdAndParty.identifier)
            if (accountInfo != null) with(accountInfo!!.state!!.data) {
                AccountInfoStateClientDto(
                        name = name,
                        host = host.name,
                        identifier = identifier.id,
                        externalId = identifier.externalId)
            }
            else null
        } else null
    }

    @Suspendable
    fun toAccountInfoDto(
            accountIdAndParty: AccountParty
    ): AccountInfoStateDto {
        return toAccountInfoDtoOrNull(accountIdAndParty)
                ?: error("Could not map input to AccountInfoStateDto")
    }

    @Suspendable
    fun toAccountInfoClientDto(
            accountIdAndParty: AccountParty
    ): AccountInfoStateClientDto {
        return toAccountInfoClientDtoOrNull(accountIdAndParty)
                ?: error("Could not map input to AccountInfoStateDto")
    }

    @Suspendable
    fun toAccountInfoDtoOrNull(
            anonymousParty: AnonymousParty?
    ): AccountInfoStateDto? {
        return toAccountInfoDtoOrNull(anonymousParty?.owningKey)
    }


    @Suspendable
    fun toAccountInfoClientDtoOrNull(
            anonymousParty: AnonymousParty?
    ): AccountInfoStateClientDto? {
        return toAccountInfoClientDtoOrNull(anonymousParty?.owningKey)
    }

    @Suspendable
    fun toAccountInfoDtoOrNull(
            owningKey: PublicKey?
    ): AccountInfoStateDto? {
        return if (owningKey != null) {
            AccountInfoStateDto.from(
                    findStoredAccountOrNull(owningKey)!!.state!!.data)
        } else null
    }

    @Suspendable
    fun toAccountInfoClientDtoOrNull(
            owningKey: PublicKey?
    ): AccountInfoStateClientDto? {
        return if (owningKey != null) {
            AccountInfoStateClientDto.from(
                    findStoredAccountOrNull(owningKey)!!.state!!.data)
        } else null
    }


    @Suspendable
    fun findAccountInfo(
            accountInfoDto: AccountInfoStateDto?
    ): AccountInfo? {
        return when {
            // Return null input as is
            accountInfoDto == null -> null
            // Build instance otherwise, try by id first...
            accountInfoDto.identifier != null -> {
                findStoredAccount(accountInfoDto.identifier!!.id).state.data
            }
            // ... name and host otherwise
            accountInfoDto.name != null && accountInfoDto.host != null -> {
                findStoredAccount(accountInfoDto.name!!, accountInfoDto.host!!).state.data
            }
            else -> throw IllegalArgumentException("Invalid AccountInfoStateDto, must include either an id or name and host")
        }
    }

    @Suspendable
    fun getAccountInfo(
            accountInfoDto: AccountInfoStateDto?
    ): AccountInfo {
        return findAccountInfo(accountInfoDto)
                ?: throw IllegalArgumentException("No stored AccountInfo could be matched to the given AccountInfoStateDto")
    }

    @Suspendable
    fun toAbstractPartyOrNull(
            accountInfoDto: AccountInfoStateDto?,
            default: AbstractParty? = null
    ): AbstractParty? {
        val accountInfo = findAccountInfo(accountInfoDto)
        if (accountInfo == null) return default
        return createPublicKey(accountInfo)
    }

    @Suspendable
    fun toAbstractParty(
            accountInfoDto: AccountInfoStateDto?,
            default: AbstractParty? = null
    ): AbstractParty? {
        return toAbstractPartyOrNull(accountInfoDto, default)
                ?: throw IllegalArgumentException("No party could be matched to the given AccountInfoStateDto")
    }

    @Suspendable
    fun toPublicKeyOrNull(
            accountInfoDto: AccountInfoStateDto?,
            default: PublicKey? = null
    ): PublicKey? {
        return toAbstractPartyOrNull(accountInfoDto)?.owningKey
                ?: default
    }

    @Suspendable
    fun toPublicKey(
            accountInfoDto: AccountInfoStateDto?,
            default: PublicKey? = null
    ): PublicKey? {
        return toPublicKeyOrNull(accountInfoDto)
                ?: throw IllegalArgumentException("No public key could be matched to the given AccountInfoStateDto")
    }

    @Suspendable
    fun toAccountPartyOrNull(accountInfo: AccountInfo?): AccountParty? {
        return if(accountInfo != null) {
            val anonymousParty = createPublicKey(accountInfo)
            AccountParty(accountInfo.identifier.id, accountInfo.name, anonymousParty, accountInfo.identifier.externalId)
        } else null
    }


    @Suspendable
    fun toAccountPartyOrNull(
            accountInfoClientDto: AccountInfoStateClientDto?,
            default: AccountParty? = null,
            ignoreMatching: Boolean = false,
            propertyName: String = "unknown"
    ): AccountParty? = toAccountPartyOrNull(
            name = accountInfoClientDto?.name,
            host = accountInfoClientDto?.host,
            identifier = accountInfoClientDto?.identifier,
            externalId = accountInfoClientDto?.externalId,
            default = default,
            ignoreMatching = ignoreMatching,
            propertyName = propertyName
    )

    // TODO: generate from/to Lite
    @Suspendable
    fun toAccountPartyOrNull(
            name: String? = null,
            host: Party? = null,
            identifier: UUID? = null,
            externalId: String? = null,
            default: AccountParty? = null,
            ignoreMatching: Boolean = false,
            propertyName: String = "unknown"
    ): AccountParty? = toAccountPartyOrNull(
            name = name,
            host = host?.name,
            identifier = identifier,
            externalId = externalId,
            default = default,
            ignoreMatching = ignoreMatching,
            propertyName = propertyName
    )

    @Suspendable
    fun toAccountPartyOrNull(
            name: String? = null,
            host: CordaX500Name? = null,
            identifier: UUID? = null,
            externalId: String? = null,
            default: AccountParty? = null,
            ignoreMatching: Boolean = false,
            propertyName: String = "unknown"
    ): AccountParty? {

        return when {
            host != null && identifier != null -> toAccountPartyOrNull(findAccountOrNull(identifier, host))
            host != null && name != null -> toAccountPartyOrNull(findAccountOrNull(name, host))
            host == null && identifier != null -> toAccountPartyOrNull(findStoredAccountOrNull(identifier)?.state?.data)
            else -> null
        }
    }

    // TODO: needs cleanup
    @Suspendable
    fun toAccountPartyOrNull(
            accountInfoStateDto: AccountInfoStateDto?,
            default: AccountParty? = null,
            ignoreMatching: Boolean = false,
            propertyName: String = "unknown"
    ): AccountParty? = toAccountPartyOrNull(
            name = accountInfoStateDto?.name,
            host = accountInfoStateDto?.host?.name,
            identifier = accountInfoStateDto?.identifier?.id,
            externalId = accountInfoStateDto?.identifier?.externalId,
            default = default,
            ignoreMatching = ignoreMatching,
            propertyName = propertyName
    )

    @Suspendable
    fun toAccountParty(
            accountInfoDto: AccountInfoStateDto?,
            default: AccountParty? = null,
            ignoreMatching: Boolean = false,
            propertyName: String = "unknown"
    ): AccountParty {
        return toAccountPartyOrNull(accountInfoDto, default, ignoreMatching, propertyName)
                ?: throw IllegalArgumentException("Failed converting property to AccountParty, name: $propertyName, " +
                        "value: $accountInfoDto to AccountParty")
    }

    @Suspendable
    fun toAccountParty(
            accountInfoDto: AccountInfoStateClientDto?,
            default: AccountParty? = null,
            ignoreMatching: Boolean = false,
            propertyName: String = "unknown"
    ): AccountParty {
        val account = toAccountPartyOrNull(accountInfoDto, default, ignoreMatching, propertyName)
        return account
                ?: throw IllegalArgumentException("Failed converting property to AccountParty, name: $propertyName, " +
                        "value: $accountInfoDto to AccountParty")
    }

}

/**
 * Basic [AccountsAwareNodeService] implementation
 */
open class BasicAccountsAwareNodeService(
        delegate: AccountsAwareNodeServiceDelegate
) : BasicNodeService(delegate), AccountsAwareNodeServiceDelegate by delegate {

    /** [PoolBoyConnection]-based constructor */
    constructor(
            poolBoy: PoolBoyConnection
    ) : this(AccountsAwareNodeServicePoolBoyDelegate(poolBoy))

    /** [NodeRpcConnection]-based constructor */
    @Deprecated(message = "RPC-based services should use the Pool Boy constructor instead")
    constructor(
            nodeRpcConnection: NodeRpcConnection
    ) : this(AccountsAwareNodeServiceRpcConnectionDelegate(nodeRpcConnection))

    /** [CordaRPCOps]-based constructor */
    @Deprecated(message = "RPC-based services should use the Pool Boy constructor instead")
    constructor(
            rpcOps: CordaRPCOps
    ) : this(AccountsAwareNodeServiceRpcDelegate(rpcOps))

    /** [ServiceHub]-based constructor */
    constructor(
            serviceHub: ServiceHub
    ) : this(serviceHub.cordaService(AccountsAwareNodeCordaServiceDelegate::class.java))
}
