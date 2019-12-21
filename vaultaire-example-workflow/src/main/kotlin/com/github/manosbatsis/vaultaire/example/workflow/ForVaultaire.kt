package com.github.manosbatsis.vaultaire.example.workflow

import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateForDependency
import com.github.manosbatsis.vaultaire.example.contract.BookContract
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken


@VaultaireGenerateForDependency(name = "fungibleTokenConditions",
        persistentStateType = PersistentFungibleToken::class,
        contractStateType = FungibleToken::class)
class Dummy1

@VaultaireGenerateForDependency(name = "magazineConditions",
        persistentStateType = BookContract.MagazineState.MagazineSchemaV1.PersistentMagazineState::class,
        contractStateType = BookContract.MagazineState::class)
class Dummy2

