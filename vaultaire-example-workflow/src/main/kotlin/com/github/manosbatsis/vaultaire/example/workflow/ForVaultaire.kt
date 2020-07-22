package com.github.manosbatsis.vaultaire.example.workflow

import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateForDependency
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineState
import com.github.manosbatsis.vaultaire.example.contract.MagazineContract.MagazineState.MagazineSchemaV1.PersistentMagazineState
import com.github.manotbatsis.kotlin.utils.api.DefaultValue
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.UniqueIdentifier
import java.util.Date


@VaultaireGenerateForDependency(name = "fungibleTokenConditions",
        persistentStateType = PersistentFungibleToken::class,
        contractStateType = FungibleToken::class)
@VaultaireGenerateDtoForDependency(
        persistentStateType = PersistentFungibleToken::class,
        contractStateType = FungibleToken::class,
        strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
class Dummy1

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
        val linearId: UniqueIdentifier
)

