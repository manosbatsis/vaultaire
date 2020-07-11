package com.github.manosbatsis.vaultaire.plugin.accounts.service.dto

import com.github.manosbatsis.vaultaire.dto.AccountIdAndParty
import com.github.manosbatsis.vaultaire.dto.AccountInfoDto
import com.github.manosbatsis.vaultaire.dto.AccountNameAndParty
import com.github.manosbatsis.vaultaire.dto.VaultaireLiteDto
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateService
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey

/**
 * Modeled after [com.github.manosbatsis.kotlin.utils.api.Dto]
 * only bringing a [AccountsAwareStateService] in-context for
 * additional conversion or other utility functions.
 */
interface VaultaireAccountsAwareLiteDto<T : ContractState>: VaultaireLiteDto<T, AccountsAwareStateService<T>> {

    fun toAccountInfoDtoOrNull(
            accountInfo: AccountInfo?,
            stateService: AccountsAwareStateService<T>? = null
    ): AccountInfoDto? = if(accountInfo != null) AccountInfoDto(
            name = accountInfo.name,
            host = accountInfo.host.name,
            identifier = accountInfo.identifier)
    else null

    fun toAccountInfoDtoOrNull(
            accountIdAndParty: AccountIdAndParty?,
            stateService: AccountsAwareStateService<T>
    ): AccountInfoDto? = if(accountIdAndParty != null) {
        toAccountInfoDtoOrNull(stateService.findStoredAccount(accountIdAndParty.identifier)?.state?.data)
    }
    else null

    fun toAccountInfoDtoOrNull(
            accountNameAndParty: AccountNameAndParty?,
            stateService: AccountsAwareStateService<T>
    ): AccountInfoDto? = toAccountInfoDtoOrNull(accountNameAndParty?.party, stateService)

    fun toAccountInfoDtoOrNull(
            anonymousParty: AnonymousParty?,
            stateService: AccountsAwareStateService<T>
    ): AccountInfoDto? = toAccountInfoDtoOrNull(anonymousParty?.owningKey, stateService)

    fun toAccountInfoDtoOrNull(
            owningKey: PublicKey?,
            stateService: AccountsAwareStateService<T>
    ): AccountInfoDto? = if(owningKey != null) {
        toAccountInfoDtoOrNull(stateService.findStoredAccount(owningKey)?.state?.data)
    }
    else null


    fun findAccountInfo(
            accountInfoDto: AccountInfoDto?,
            stateService: AccountsAwareStateService<T>
    ): AccountInfo? =
            when{
                // Return null input as is
                accountInfoDto == null -> null
                // Build instance otherwise, try by id first...
                accountInfoDto.identifier != null ->{
                    stateService.getStoredAccount(accountInfoDto.identifier!!).state.data
                }
                // ... name and host otherwise
                accountInfoDto.name != null && accountInfoDto.host != null -> {
                    stateService.getStoredAccount(accountInfoDto.name!!, accountInfoDto.host!!).state.data
                }
                else -> throw IllegalArgumentException("Invalid AccountInfoDto, must include either an id or name and host")
            }

    fun getAccountInfo(
            accountInfoDto: AccountInfoDto?,
            stateService: AccountsAwareStateService<T>
    ): AccountInfo = findAccountInfo(accountInfoDto, stateService)
            ?:throw IllegalArgumentException("No stored AccountInfo could be matched to the given AccountInfoDto")

    fun toAbstractPartyOrNull(
            accountInfoDto: AccountInfoDto?,
            stateService: AccountsAwareStateService<T>,
            default: AbstractParty? = null
    ): AbstractParty? {
        val accountInfo = findAccountInfo(accountInfoDto, stateService)
        return when(accountInfo) {
            // Return null input as is
            null -> default
            else -> stateService.createPublicKey(accountInfo)
        }
    }

    fun toAbstractParty(
            accountInfoDto: AccountInfoDto?,
            stateService: AccountsAwareStateService<T>,
            default: AbstractParty? = null
    ): AbstractParty? =
            toAbstractPartyOrNull(accountInfoDto, stateService, default)
                    ?:throw IllegalArgumentException("No party could be matched to the given AccountInfoDto")

    fun toPublicKeyOrNull(
            accountInfoDto: AccountInfoDto?,
            stateService: AccountsAwareStateService<T>,
            default: PublicKey? = null
    ): PublicKey? = toAbstractPartyOrNull(accountInfoDto, stateService)?.owningKey
            ?: default

    fun toPublicKey(
            accountInfoDto: AccountInfoDto?,
            stateService: AccountsAwareStateService<T>,
            default: PublicKey? = null
    ): PublicKey? = toPublicKeyOrNull(accountInfoDto, stateService)
            ?:throw IllegalArgumentException("No public key could be matched to the given AccountInfoDto")

    fun toAccountIdAndPartyOrNull(
            accountInfoDto: AccountInfoDto?,
            stateService: AccountsAwareStateService<T>,
            default: AccountIdAndParty? = null,
            ignoreMatching: Boolean  = false
    ): AccountIdAndParty? =
            when{
                // Return null input as is
                accountInfoDto == null -> null
                // Reuse available if IDs match
                !ignoreMatching && accountInfoDto.hasMatchingIdentifier(default) -> default
                // Build instance otherwise, try by id first...
                accountInfoDto.identifier != null ->{
                    val accountInfo = stateService
                            .getStoredAccount(accountInfoDto.identifier!!).state.data
                    AccountIdAndParty(accountInfo.identifier, stateService.createPublicKey(accountInfo))
                }
                // ... name and host otherwise
                accountInfoDto.name != null && accountInfoDto.host != null -> {
                    val accountInfo = stateService
                            .getStoredAccount(accountInfoDto.name!!, accountInfoDto.host!!).state.data
                    AccountIdAndParty(accountInfo.identifier, stateService.createPublicKey(accountInfo))
                }
                else -> throw IllegalArgumentException("Invalid AccountInfoDto, must include either an id or name and host")

            }

    fun toAccountIdAndParty(
            accountInfoDto: AccountInfoDto?,
            stateService: AccountsAwareStateService<T>,
            default: AccountIdAndParty? = null,
            ignoreMatching: Boolean  = false
    ): AccountIdAndParty =
            toAccountIdAndPartyOrNull(accountInfoDto, stateService, default)
            ?: throw IllegalArgumentException("Failed converting from the given AccountInfoDto to AccountIdAndParty")


}
