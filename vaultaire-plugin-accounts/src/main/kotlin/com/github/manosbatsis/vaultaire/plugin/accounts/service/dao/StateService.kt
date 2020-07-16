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
import com.github.manosbatsis.vaultaire.dto.AccountInfoDto
import com.github.manosbatsis.vaultaire.dto.AccountParty
import com.github.manosbatsis.vaultaire.service.dao.BasicStateService
import com.github.manosbatsis.vaultaire.service.dao.ExtendedStateService
import com.github.manosbatsis.vaultaire.service.dao.StateService
import com.github.manosbatsis.vaultaire.util.Fields
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.schemas.StatePersistable
import java.security.PublicKey


/**
 * Short-lived helper, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
interface AccountsAwareStateService<T : ContractState>: StateService<T>, AccountsAwareStateServiceDelegate<T> {

    @Suspendable
    fun toAccountInfoDtoOrNull(
            accountInfo: AccountInfo?,
            stateService: AccountsAwareStateService<*>? = null
    ): AccountInfoDto? = if(accountInfo != null) AccountInfoDto(
            name = accountInfo.name,
            host = accountInfo.host.name,
            identifier = accountInfo.identifier.id)
    else null

    @Suspendable
    fun toAccountInfoDtoOrNull(
            accountIdAndParty: AccountParty?
    ): AccountInfoDto? = if(accountIdAndParty != null) {
        val accountInfo = toAccountInfoDtoOrNull(findStoredAccountOrNull(accountIdAndParty.identifier)?.state?.data)
        if(accountInfo != null) AccountInfoDto(
                name = accountInfo.name,
                host = accountInfo.host,
                identifier = accountInfo.identifier)
        else null
    }
    else null

    @Suspendable
    fun toAccountInfoDto(
            accountIdAndParty: AccountParty
    ): AccountInfoDto = toAccountInfoDtoOrNull(accountIdAndParty)
            ?: error("Could not map input to AccountInfoDto")

    @Suspendable
    fun toAccountInfoDto(
            accountInfo: AccountInfo
    ): AccountInfoDto = toAccountInfoDtoOrNull(accountInfo)
            ?: error("Could not map input to AccountInfoDto")


    @Suspendable
    fun toAccountInfoDtoOrNull(
            anonymousParty: AnonymousParty?
    ): AccountInfoDto? = toAccountInfoDtoOrNull(anonymousParty?.owningKey)

    @Suspendable
    fun toAccountInfoDtoOrNull(
            owningKey: PublicKey?
    ): AccountInfoDto? = if(owningKey != null) {
        toAccountInfoDtoOrNull(findStoredAccountOrNull(owningKey)?.state?.data)
    }
    else null


    @Suspendable
    fun findAccountInfo(
            accountInfoDto: AccountInfoDto?
    ): AccountInfo? =
            when{
                // Return null input as is
                accountInfoDto == null -> null
                // Build instance otherwise, try by id first...
                accountInfoDto.identifier != null ->{
                    findStoredAccount(accountInfoDto.identifier!!).state.data
                }
                // ... name and host otherwise
                accountInfoDto.name != null && accountInfoDto.host != null -> {
                    findStoredAccount(accountInfoDto.name!!, accountInfoDto.host!!).state.data
                }
                else -> throw IllegalArgumentException("Invalid AccountInfoDto, must include either an id or name and host")
            }

    @Suspendable
    fun getAccountInfo(
            accountInfoDto: AccountInfoDto?
    ): AccountInfo = findAccountInfo(accountInfoDto)
            ?:throw IllegalArgumentException("No stored AccountInfo could be matched to the given AccountInfoDto")

    @Suspendable
    fun toAbstractPartyOrNull(
            accountInfoDto: AccountInfoDto?,
            default: AbstractParty? = null
    ): AbstractParty? {
        val accountInfo = findAccountInfo(accountInfoDto)
        return when(accountInfo) {
            // Return null input as is
            null -> default
            else -> createPublicKey(accountInfo)
        }
    }

    @Suspendable
    fun toAbstractParty(
            accountInfoDto: AccountInfoDto?,
            default: AbstractParty? = null
    ): AbstractParty? =
            toAbstractPartyOrNull(accountInfoDto, default)
                    ?:throw IllegalArgumentException("No party could be matched to the given AccountInfoDto")

    @Suspendable
    fun toPublicKeyOrNull(
            accountInfoDto: AccountInfoDto?,
            default: PublicKey? = null
    ): PublicKey? = toAbstractPartyOrNull(accountInfoDto)?.owningKey
            ?: default

    @Suspendable
    fun toPublicKey(
            accountInfoDto: AccountInfoDto?,
            default: PublicKey? = null
    ): PublicKey? = toPublicKeyOrNull(accountInfoDto)
            ?:throw IllegalArgumentException("No public key could be matched to the given AccountInfoDto")

    // TODO: needs cleanup
    @Suspendable
    fun toAccountPartyOrNull(
            accountInfoDto: AccountInfoDto?,
            default: AccountParty? = null,
            ignoreMatching: Boolean  = false
    ): AccountParty? {
        println("toAccountPartyOrNull, accountInfoDto: $accountInfoDto ")
        return when {
            // Return null input as is
            accountInfoDto == null -> default
            // Reuse available if IDs match
            //!ignoreMatching && accountInfoDto.hasMatchingIdentifierAndName(default) -> default
            // Build instance otherwise, try by id first...
            accountInfoDto.identifier != null && accountInfoDto.host != null -> {
                val accountInfo = findAccountOrNull(accountInfoDto.identifier!!, accountInfoDto.host!!)
                if (accountInfo != null) AccountParty(accountInfo.identifier.id, accountInfo.name, createPublicKey(accountInfo))
                else null
            }
            accountInfoDto.identifier != null -> {
                val accountInfo = findStoredAccountOrNull(accountInfoDto.identifier!!)?.state?.data
                if (accountInfo != null) AccountParty(accountInfo.identifier.id, accountInfo.name, createPublicKey(accountInfo))
                else null
            }
            else -> throw IllegalArgumentException("Failed converting from the given $accountInfoDto , must include an id and, optionally, a host")

        }
    }

    @Suspendable
    fun toAccountParty(
            accountInfoDto: AccountInfoDto?,
            default: AccountParty? = null,
            ignoreMatching: Boolean  = false
    ): AccountParty =
            toAccountPartyOrNull(accountInfoDto, default, ignoreMatching)
                    ?: throw IllegalArgumentException("Failed converting from the given $accountInfoDto to AccountParty")

}


/**
 * Basic [StateService] implementation, used for vault operations on a specific [ContractState] type
 * @param T the [ContractState] type
 */
open class BasicAccountsAwareStateService<T: ContractState>(
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
abstract class ExtendedAccountsAwareStateService<T: ContractState, P : StatePersistable, out F: Fields<P>, Q: VaultQueryCriteriaCondition<P, F>>(
        delegate: AccountsAwareStateServiceDelegate<T>
) : BasicAccountsAwareStateService<T>(delegate), ExtendedStateService<T, P, F, Q> {

    /** The fields of the target [StatePersistable] type `P` */
    override lateinit var criteriaConditionsType: Class<Q>
}
