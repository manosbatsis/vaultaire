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
package com.github.manosbatsis.vaultaire.plugin.accounts.service.dao

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.vaultaire.dsl.query.VaultQueryCriteriaCondition
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoDto
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoLiteDto
import com.github.manosbatsis.vaultaire.service.dao.BasicStateService
import com.github.manosbatsis.vaultaire.service.dao.ExtendedStateService
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manosbatsis.vaultaire.util.Fields
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.schemas.StatePersistable
import net.corda.core.utilities.contextLogger
import java.security.PublicKey


/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
interface AccountsAwareStateService<T : ContractState> :
        StateService<T>,
        AccountsAwareStateServiceDelegate<T> {

    companion object {
        private val logger = contextLogger()
    }

    @Suspendable
    fun toAccountInfoDtoOrNull(
            accountIdAndParty: AccountParty?
    ): AccountInfoDto? {
        return if (accountIdAndParty != null) {
            val accountInfo = findStoredAccountOrNull(accountIdAndParty.identifier)
            if (accountInfo != null) with(accountInfo!!.state!!.data) {
                AccountInfoDto(
                        name = name,
                        host = host,
                        identifier = identifier)
            }
            else null
        } else null
    }

    @Suspendable
    fun toAccountInfoLiteDtoOrNull(
            accountIdAndParty: AccountParty?
    ): AccountInfoLiteDto? {
        return if (accountIdAndParty != null) {
            val accountInfo = findStoredAccountOrNull(accountIdAndParty.identifier)
            if (accountInfo != null) with(accountInfo!!.state!!.data) {
                AccountInfoLiteDto(
                        name = name,
                        host = host.name,
                        identifier = identifier.id)
            }
            else null
        } else null
    }

    @Suspendable
    fun toAccountInfoDto(
            accountIdAndParty: AccountParty
    ): AccountInfoDto {
        return toAccountInfoDtoOrNull(accountIdAndParty)
                ?: error("Could not map input to AccountInfoDto")
    }

    @Suspendable
    fun toAccountInfoLiteDto(
            accountIdAndParty: AccountParty
    ): AccountInfoLiteDto {
        return toAccountInfoLiteDtoOrNull(accountIdAndParty)
                ?: error("Could not map input to AccountInfoDto")
    }

    @Suspendable
    fun toAccountInfoDtoOrNull(
            anonymousParty: AnonymousParty?
    ): AccountInfoDto? {
        return toAccountInfoDtoOrNull(anonymousParty?.owningKey)
    }


    @Suspendable
    fun toAccountInfoLiteDtoOrNull(
            anonymousParty: AnonymousParty?
    ): AccountInfoLiteDto? {
        return toAccountInfoLiteDtoOrNull(anonymousParty?.owningKey)
    }

    @Suspendable
    fun toAccountInfoDtoOrNull(
            owningKey: PublicKey?
    ): AccountInfoDto? {
        return if (owningKey != null) {
            AccountInfoDto.mapToDto(
                    findStoredAccountOrNull(owningKey)!!.state!!.data)
        } else null
    }

    @Suspendable
    fun toAccountInfoLiteDtoOrNull(
            owningKey: PublicKey?
    ): AccountInfoLiteDto? {
        return if (owningKey != null) {
            AccountInfoLiteDto.mapToDto(
                    findStoredAccountOrNull(owningKey)!!.state!!.data)
        } else null
    }


    @Suspendable
    fun findAccountInfo(
            accountInfoDto: AccountInfoDto?
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
            else -> throw IllegalArgumentException("Invalid AccountInfoDto, must include either an id or name and host")
        }
    }

    @Suspendable
    fun getAccountInfo(
            accountInfoDto: AccountInfoDto?
    ): AccountInfo {
        return findAccountInfo(accountInfoDto)
                ?: throw IllegalArgumentException("No stored AccountInfo could be matched to the given AccountInfoDto")
    }

    @Suspendable
    fun toAbstractPartyOrNull(
            accountInfoDto: AccountInfoDto?,
            default: AbstractParty? = null
    ): AbstractParty? {
        val accountInfo = findAccountInfo(accountInfoDto)
        if (accountInfo == null) return default
        return createPublicKey(accountInfo)
    }

    @Suspendable
    fun toAbstractParty(
            accountInfoDto: AccountInfoDto?,
            default: AbstractParty? = null
    ): AbstractParty? {
        return toAbstractPartyOrNull(accountInfoDto, default)
                ?: throw IllegalArgumentException("No party could be matched to the given AccountInfoDto")
    }

    @Suspendable
    fun toPublicKeyOrNull(
            accountInfoDto: AccountInfoDto?,
            default: PublicKey? = null
    ): PublicKey? {
        return toAbstractPartyOrNull(accountInfoDto)?.owningKey
                ?: default
    }

    @Suspendable
    fun toPublicKey(
            accountInfoDto: AccountInfoDto?,
            default: PublicKey? = null
    ): PublicKey? {
        return toPublicKeyOrNull(accountInfoDto)
                ?: throw IllegalArgumentException("No public key could be matched to the given AccountInfoDto")
    }
/*
    fun  toParty(
            partyName: CordaX500Name?,
            stateService: S,
            propertyName: String = "unknown"
    ): Party = if(partyName != null) stateService.wellKnownPartyFromX500Name(partyName)
                    ?: throw DtoInsufficientMappingException("Name ${partyName} not found for property: $propertyName")
            else throw DtoInsufficientMappingException("Required property: $propertyName was null")

 */

    // TODO: generate from/to Lite
    @Suspendable
    fun toAccountPartyOrNull(
            accountInfoLiteDto: AccountInfoLiteDto?,
            default: AccountParty? = null,
            ignoreMatching: Boolean = false,
            propertyName: String = "unknown"
    ): AccountParty? {
        val accountInfoDto = if (accountInfoLiteDto == null) {
            null
        } else {
            val host = if (accountInfoLiteDto.host == null) {
                null
            } else {
                wellKnownPartyFromX500Name(accountInfoLiteDto.host!!)
            }
            AccountInfoDto(
                    accountInfoLiteDto.name,
                    host,
                    accountInfoLiteDto.identifier?.let { UniqueIdentifier(null, it) }
            )
        }
        val accountParty = toAccountPartyOrNull(
                accountInfoDto,
                default,
                ignoreMatching,
                propertyName)
        return accountParty
    }

    // TODO: needs cleanup
    @Suspendable
    fun toAccountPartyOrNull(
            accountInfoDto: AccountInfoDto?,
            default: AccountParty? = null,
            ignoreMatching: Boolean = false,
            propertyName: String = "unknown"
    ): AccountParty? {

        // Return null input as is
        return if (accountInfoDto == null) {
            default
        }
        // Reuse available if IDs match
        //!ignoreMatching && accountInfoDto.hasMatchingIdentifierAndName(default) -> default
        // Build instance otherwise, try by id first...
        else if (accountInfoDto.identifier != null && accountInfoDto.host != null) {
            val accountInfo = findAccountOrNull(accountInfoDto.identifier!!.id, accountInfoDto.host!!.name)
            if (accountInfo != null) {
                val anonymousParty = createPublicKey(accountInfo)
                AccountParty(accountInfo.identifier.id, accountInfo.name, anonymousParty)
            } else null
        } else if (accountInfoDto.identifier != null) {
            val accountInfo = findStoredAccountOrNull(accountInfoDto.identifier!!.id)?.state?.data
            if (accountInfo != null) {
                val anonymousParty = createPublicKey(accountInfo)
                AccountParty(accountInfo.identifier.id, accountInfo.name, anonymousParty)
            } else null
        } else throw IllegalArgumentException("Failed converting property to AccountParty, name: $propertyName, " +
                "value: $accountInfoDto to AccountParty")

    }

    @Suspendable
    fun toAccountParty(
            accountInfoDto: AccountInfoDto?,
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
            accountInfoDto: AccountInfoLiteDto?,
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
 * Basic [StateService] implementation, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
open class BasicAccountsAwareStateService<T : ContractState>(
        delegate: AccountsAwareStateServiceDelegate<T>
) : BasicStateService<T>(delegate),
        AccountsAwareStateServiceDelegate<T> by delegate,
        AccountsAwareStateService<T> {


}

/**
 * Extends [BasicStateService] to provide a [StateService] aware of the target
 * [ContractState] type's [StatePersistable] and [Fields].
 *
 * Subclassed by Vaultaire's annotation processing to generate service components.
 */
abstract class ExtendedAccountsAwareStateService<T : ContractState, P : StatePersistable, out F : Fields<P>, Q : VaultQueryCriteriaCondition<P, F>>(
        delegate: AccountsAwareStateServiceDelegate<T>
) : BasicAccountsAwareStateService<T>(delegate), ExtendedStateService<T, P, F, Q> {

    /** The fields of the target [StatePersistable] type `P` */
    override lateinit var criteriaConditionsType: Class<Q>
}
