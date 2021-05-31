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
package com.github.manosbatsis.vaultaire.plugin.accounts.service.dto

import com.github.manosbatsis.vaultaire.dto.VaultaireBaseStateClientDto
import com.github.manosbatsis.vaultaire.dto.VaultaireModelClientDto
import com.github.manosbatsis.vaultaire.plugin.accounts.service.dao.AccountsAwareStateService
import com.github.manosbatsis.vaultaire.plugin.accounts.service.node.AccountsAwareNodeService
import net.corda.core.contracts.ContractState


/** Accounts-aware alternative to [VaultaireStateClientDto]. */
interface VaultaireAccountsAwareStateClientDto<T : ContractState> : VaultaireBaseStateClientDto<T, AccountsAwareStateService<T>>


/** Accounts-aware alternative to [VaultaireModelClientDto]. */
interface AccountsAwareVaultaireModelClientDto<T : Any>: VaultaireModelClientDto<T, AccountsAwareNodeService>
