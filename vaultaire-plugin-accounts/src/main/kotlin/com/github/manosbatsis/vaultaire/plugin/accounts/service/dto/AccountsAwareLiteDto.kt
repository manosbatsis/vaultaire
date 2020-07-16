package com.github.manosbatsis.vaultaire.plugin.accounts.service.dto

import com.github.manosbatsis.vaultaire.dto.VaultaireBaseLiteDto
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateService
import net.corda.core.contracts.ContractState


/**
 * Modeled after [com.github.manosbatsis.kotlin.utils.api.Dto]
 * only bringing a [AccountsAwareStateService] in-context for
 * additional conversion or other utility functions.
 */
interface AccountsAwareLiteDto<T : ContractState>: VaultaireBaseLiteDto<T, AccountsAwareStateService<T>>
