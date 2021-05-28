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

import com.github.manosbatsis.kotlin.utils.api.DefaultValue
import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.annotation.VaultaireFlowInputForDependency
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateForDependency
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineState
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineState.MagazineSchemaV1.PersistentMagazineState
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.UniqueIdentifier
import java.util.*


@VaultaireFlowInputForDependency(
        baseType = MagazineContract.MagazineModel::class)
data class MagazineModelMixin(
        @DefaultValue("1")
        var issues: Int,
        @DefaultValue("Date()")
        val published: Date,
        @DefaultValue("UniqueIdentifier()")
        val linearId: UniqueIdentifier,
        val customMixinField: Map<String, String> = emptyMap()
)

@VaultaireGenerateForDependency(name = "fungibleTokenConditions",
        persistentStateType = PersistentFungibleToken::class,
        contractStateType = FungibleToken::class)
@VaultaireGenerateDtoForDependency(
        persistentStateType = PersistentFungibleToken::class,
        contractStateType = FungibleToken::class,
        strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
class FungibleTokenMixin

@VaultaireGenerateForDependency(name = "magazineConditions",
        persistentStateType = PersistentMagazineState::class,
        contractStateType = MagazineState::class)
@VaultaireGenerateDtoForDependency(
        persistentStateType = PersistentMagazineState::class,
        contractStateType = MagazineState::class,
        strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
data class MagazineMixin(
        @DefaultValue("1")
        var issues: Int,
        @DefaultValue("Date()")
        val published: Date,
        @DefaultValue("UniqueIdentifier()")
        val linearId: UniqueIdentifier,
        val customMixinField: Map<String, String> = emptyMap()
)

/*TODO: move to module, have it generated
@VaultaireGenerateForDependency(name = "accountInfoConditions",
        persistentStateType = PersistentAccountInfo::class,
        contractStateType = AccountInfo::class)
@VaultaireGenerateDtoForDependency(
        persistentStateType = PersistentAccountInfo::class,
        contractStateType = AccountInfo::class,
        strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
class AccountInfoMixin */