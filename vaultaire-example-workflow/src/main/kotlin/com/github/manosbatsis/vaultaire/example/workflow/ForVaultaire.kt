package com.github.manosbatsis.vaultaire.example.workflow

import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateForDependency
import com.github.manosbatsis.vaultaire.example.contract.BookContract
import com.r3.corda.lib.accounts.contracts.internal.schemas.PersistentAccountInfo
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken


@VaultaireGenerateForDependency(name = "accountInfoConditions",
        persistentStateType = PersistentAccountInfo::class,
        contractStateType = AccountInfo::class)
class Dummy10

@VaultaireGenerateForDependency(name = "fungibleTokenConditions",
        persistentStateType = PersistentFungibleToken::class,
        contractStateType = FungibleToken::class)
@VaultaireGenerateDtoForDependency(
        persistentStateType = PersistentFungibleToken::class,
        contractStateType = FungibleToken::class,
        strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
class Dummy1

@VaultaireGenerateForDependency(name = "magazineConditions",
        persistentStateType = BookContract.MagazineState.MagazineSchemaV1.PersistentMagazineState::class,
        contractStateType = BookContract.MagazineState::class)
@VaultaireGenerateDtoForDependency(
        persistentStateType = BookContract.MagazineState.MagazineSchemaV1.PersistentMagazineState::class,
        contractStateType = BookContract.MagazineState::class,
        strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
class Dummy2

